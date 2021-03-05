/*[INCLUDE-IF (JAVA_SPEC_VERSION >= 16) ]*/
/*******************************************************************************
 * Copyright (c) 2021, 2021 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/
package jdk.internal.foreign.abi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.ref.SoftReference;
import java.lang.invoke.MethodHandles.Lookup;

import java.util.Objects;
import java.util.Optional;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.ValueLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.LibraryLookup;
import static jdk.incubator.foreign.LibraryLookup.Symbol;

/**
 * The counterpart in OpenJDK is replaced with this class that wrap up a method handle
 * enabling the native code to the ffi_call via the libffi interface at runtime.
 */
public class ProgrammableInvoker {
	
	private final MethodType funcMethodType;
	private final FunctionDescriptor funcDescriptor;
	private final Addressable functionAddr;
	//private final MemorySegment returnValueMemSegReference;
	private long cifNativeThunkAddr;
	private long argTypesAddr;
	private Class<?>[] argLayoutClasses;
	private Class<?> retLayoutClass;
	private int argCount;
	
	final static Lookup lookup = MethodHandles.lookup();
	private final Set<MemoryAddress> memAddrReferences = new HashSet<MemoryAddress>();
	
	/* The ffi_cif and the corresponding argument types are shared in multiple downcalls or across threads */
	private static final ConcurrentHashMap<String, Long> cachedCifNativeThunkAddr = new ConcurrentHashMap<String, Long>();
	private static final ConcurrentHashMap<List<MemoryLayout>, Long> cachedArgTypes = new ConcurrentHashMap<List<MemoryLayout>, Long>();
	
	private static synchronized native void resolveRequiredFieldsAndClasses();
	private native void initCifNativeThunkData(Class<?>[] argLayoutClasses, Class<?> retLayoutClass, boolean newArgTypes);
	private native long invokeNative(long functionAddr, long calloutThunk, long[] argValues);
	
	private static final class PrivateClassLock {
		PrivateClassLock() {}
	}
	private static final Object privateClassLock = new PrivateClassLock();
	
	static {
		/* Resolve the required fields (specifically their offset in the jcl constant pool of VM)
		 * which can be shared in multiple calls or across threads given the generated macros
		 * in the vmconstantpool.xml depend on their offsets to access the corresponding fields.
		 * In addition, resolve the MemoryAddress class which is used to identify its ffi_type.
		 * 
		 * Note: the value of these fields varies with different instance.
		 */
		resolveRequiredFieldsAndClasses();
	}
	
	ProgrammableInvoker(Addressable downcallSymbol, MethodType functionMethodType, FunctionDescriptor functionDescriptor) {
		/* As explained in the Spec of LibraryLookup, the downcall must hold a strong reference to
		 * the native library symbol to prevent the underlying native library from being unloaded
		 * during the native calls.
		 */
		functionAddr = downcallSymbol;
		funcMethodType = functionMethodType;
		funcDescriptor = functionDescriptor;
		cifNativeThunkAddr = 0;
		argTypesAddr = 0;
		argCount = 0;
		generateAdapter();
	}
	
	private void generateAdapter() {
		Optional<MemoryLayout> funcDescReturnLayout = funcDescriptor.returnLayout();
		MemoryLayout actualDescReturnLayout = funcDescReturnLayout.isPresent() ? funcDescReturnLayout.get() : null; // set to null for void
		Class<?> actualReturnType = UnboxingPrimitiveClassType(funcMethodType.returnType());
		retLayoutClass = MapLayoutToClass(actualDescReturnLayout, actualReturnType);
		
		Class<?>[] argTypes = funcMethodType.parameterArray();
		List<MemoryLayout> argLayouts = funcDescriptor.argumentLayouts();
		argCount = argLayouts.size();
		argLayoutClasses = new Class<?>[argCount];
		MemoryLayout[] argLayoutArray = argLayouts.toArray(new MemoryLayout[argCount]);
		for (int argIndex = 0; argIndex < argCount; argIndex++) {
			Class<?> actualArgType = UnboxingPrimitiveClassType(argTypes[argIndex]);
			argLayoutClasses[argIndex] =  MapLayoutToClass(argLayoutArray[argIndex], actualArgType);
		}
		
		synchronized(privateClassLock) {
			System.out.println("\n\n************** generateAdapter: SYNLOCK BEGIN********************");
			
			/* Each function descriptor is created with an unique hashcode */
			String funcDescHashPlusReturnType = funcDescriptor.hashCode() + "_" + actualReturnType; //$NON-NLS-1$
			System.out.println("*** generateAdapter: funcDescriptor = " + funcDescriptor  //$NON-NLS-1$
					+ ", funcDescHashPlusReturnType = " + funcDescHashPlusReturnType //$NON-NLS-1$
					+ ", functionAddr = " + Long.toHexString(functionAddr.address().toRawLongValue())); //$NON-NLS-1$
			
			/* If a ffi_cif for a given function descriptor exists, then the corresponding argument types
			 * were already created to set up ffi_cif, in which case there is no need to check the argument types.
			 * If not the case, then we need to check whether the same argument types exists in the cache to
			 * avoid duplicate allocation for the same argument layouts.
			 * 
			 * Note: different return types should be treated as different in order to generate different prep_cif
			 * given that the return value is specially converted against its type in the native code on various platforms;
			 * otherwise, an int type will end up with a boolean value on return due to the same C_INT layout.
			 */
			if (cachedCifNativeThunkAddr.containsKey(funcDescHashPlusReturnType)) {
				cifNativeThunkAddr = cachedCifNativeThunkAddr.get(funcDescHashPlusReturnType).longValue();
				argTypesAddr = cachedArgTypes.get(argLayouts).longValue();
				System.out.println("generateAdapter: funcDescriptor exists: cifNativeThunkAddr = " + Long.toHexString(cifNativeThunkAddr)
									+ ", funcDescHashPlusReturnType = " + funcDescHashPlusReturnType);
				System.out.println("generateAdapter: argLayouts exists: argTypesAddr = " + Long.toHexString(argTypesAddr));
			} else {
				boolean newArgTypes = cachedArgTypes.containsKey(argLayouts) ? false : true;
				if (!newArgTypes) {
					argTypesAddr = cachedArgTypes.get(argLayouts).longValue();
					System.out.println("generateAdapter: argLayouts exists: argTypesAddr = " + Long.toHexString(argTypesAddr));
				}
				
				initCifNativeThunkData(argLayoutClasses, retLayoutClass, newArgTypes);
				
				/* Cache the address of cif and argTypes after setting up via the out-of-line native code */
				if (newArgTypes) {
					System.out.println("generateAdapter: argLayouts DOES NOT exists: SET argTypesAddr = " + Long.toHexString(argTypesAddr));
					cachedArgTypes.put(argLayouts, Long.valueOf(argTypesAddr));
				}
				System.out.println("generateAdapter: funcDescriptor DOES NOT exists: SET cifNativeThunkAddr = " + Long.toHexString(cifNativeThunkAddr)
									+ ", funcDescHashPlusReturnType = " + funcDescHashPlusReturnType);
				cachedCifNativeThunkAddr.put(funcDescHashPlusReturnType, Long.valueOf(cifNativeThunkAddr));
			}
			System.out.println("****************** generateAdapter: SYNLOCK END ********************");
		}
	}
	
	@SuppressWarnings("javadoc")
	public static MethodHandle getBoundMethodHandle(Addressable downcallSymbol, MethodType functionMethodType, FunctionDescriptor funcDesc) {
		checkIfValidType(functionMethodType, funcDesc.argumentLayouts().size());
		ProgrammableInvoker nativeInvoker = new ProgrammableInvoker(downcallSymbol, functionMethodType, funcDesc);
		try {
			MethodHandle boundHandle = lookup.bind(nativeInvoker, "runNativeMethod", MethodType.methodType(Object.class, Object[].class));
			
			/* Replace the bound handle with the specified types of the native function */
			boundHandle = permuteMH(boundHandle, functionMethodType);
			return boundHandle;
		} catch (NoSuchMethodException | IllegalAccessException | NullPointerException | WrongMethodTypeException e) {
			throw new InternalError(e);
		}
	}
	
	/* To be updated after the struct layout is implemented */
	private static void checkIfValidType(MethodType targetMethodType, int argLayoutCount) {
		Class<?> retType = targetMethodType.returnType();
		if (!validateArgRetTypeClass(retType) && (retType != void.class)) {
			throw new IllegalArgumentException("The return type is neither primitive/void nor MemoryAddress");  //$NON-NLS-1$
		}

		Class<?>[] argTypes = targetMethodType.parameterArray();
		int argTypeCount = argTypes.length;
		if (argTypeCount != argLayoutCount) {
			throw new IllegalArgumentException("The arity (" + argTypeCount //$NON-NLS-1$
												+ ") of the argument types is inconsistent with the arity ("  //$NON-NLS-1$
												+ argLayoutCount + ") of the argument layouts");  //$NON-NLS-1$
		}
		
		for (int argIndex = 0; argIndex < argTypeCount; argIndex++) {
			if (!validateArgRetTypeClass(argTypes[argIndex])) {
				throw new IllegalArgumentException("The passed-in argument types at index " + argIndex + " is neither primitive nor MemoryAddress"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	private static boolean validateArgRetTypeClass(Class<?> targetType) {
		if (!targetType.isPrimitive()
		&& (targetType != Boolean.class)
		&& (targetType != Byte.class)
		&& (targetType != Character.class)
		&& (targetType != Short.class)
		&& (targetType != Integer.class)
		&& (targetType != Long.class)
		&& (targetType != Float.class)
		&& (targetType != Double.class)
		&& (targetType != MemoryAddress.class)
		) {
			return false;
		}
		return true;
	}
	
	/* Collect and convert the passed-in arguments to an Object array for the underlying native call */
	private static MethodHandle permuteMH(MethodHandle targetHandle, MethodType nativeMethodType) throws NullPointerException, WrongMethodTypeException {
		MethodHandle resultHandle = targetHandle.asCollector(0, Object[].class, nativeMethodType.parameterCount());
		System.out.println("resultHandle1 = " + resultHandle);
		resultHandle = resultHandle.asType(nativeMethodType);
		System.out.println("resultHandle2 = " + resultHandle);
		return resultHandle;
	}
	
	@SuppressWarnings("boxing")
	Object runNativeMethod(Object[] args) {
		System.out.println("\n\n\n ******* calling runNativeMethod... ********* ");
		System.out.println("runNativeMethod: functionAddr = " + Long.toHexString(functionAddr.address().toRawLongValue()));
		System.out.println("runNativeMethod: cifNativeThunkAddr = " + Long.toHexString(cifNativeThunkAddr));
		
		long newArgs[] = new long[argCount];
		for (int argIndex = 0; argIndex < argCount; argIndex++) {
			System.out.println("args[" + argIndex + "] = " + args[argIndex]);
			System.out.println("argLayoutClasses[" + argIndex + "] = " + argLayoutClasses[argIndex]);
			newArgs[argIndex] = convertArgToLongValue(argLayoutClasses[argIndex], args[argIndex]);
			System.out.println("newArgs[" + argIndex + "] = " + newArgs[argIndex]);
		}
		
		long returnVal = invokeNative(functionAddr.address().toRawLongValue(), cifNativeThunkAddr, newArgs);
		System.out.println("runNativeMethod: returnVal = " + returnVal);
		
		/* Process the return value depending on the return type */
		Object processedReturnVal = processReturnVal(returnVal);
		return processedReturnVal;
	}
	
	private static Class<?> UnboxingPrimitiveClassType(Class<?> targetType) {
		Class<?> clazz = targetType;
		
		if (targetType == Boolean.class) {
			clazz = boolean.class;
		} else if (targetType == Byte.class) {
			clazz = byte.class;
		} else if (targetType == Character.class) {
			clazz = char.class;
		} else if (targetType == Short.class) {
			clazz = short.class;
		} else if (targetType == Integer.class) {
			clazz = int.class;
		} else if (targetType == Long.class) {
			clazz = long.class;
		} else if (targetType == Float.class) {
			clazz = float.class;
		} else if (targetType == Double.class) {
			clazz = double.class;
		}
		
		return clazz;
	}
	
	private static Class<?> MapLayoutToClass(MemoryLayout TypeLayout, Class<?> targetType) {
		if (TypeLayout != null) {
			if (!ValueLayout.class.isInstance(TypeLayout)) {
				throw new IllegalArgumentException("ValueLayout is expected: layout = " + TypeLayout); //$NON-NLS-1$
			}
			if (!TypeLayout.hasSize()) {
				throw new IllegalArgumentException("The layout's size is expected: layout = " + TypeLayout); //$NON-NLS-1$
			}
			
			validateLayoutSizeWithType(TypeLayout, targetType);
			
			if(!TypeLayout.toString().contains("[abi/kind=")) { //$NON-NLS-1$
				throw new IllegalArgumentException("The layout's ABI Class is undefined: layout = " + TypeLayout); //$NON-NLS-1$
			}
		}
		
		validateLayoutType(TypeLayout, targetType);
		return targetType;
	}
	
	private static void validateLayoutSizeWithType(MemoryLayout TypeLayout, Class<?> targetType) {
		String layoutSize = TypeLayout.bitSize() + "_bits"; //$NON-NLS-1$
		boolean mismatchedSize = false;
		
		switch (layoutSize) {
		case "8_bits": //$NON-NLS-1$
			/* the 8-bits layout in Java only matches with byte in C */
			if (targetType != byte.class) {
				mismatchedSize = true;
			}
			break;
		case "16_bits": //$NON-NLS-1$
			/* The 16-bits layout is shared by char and short
			 * given the char size is 16 bits in Java.
			 */
			if ((targetType != char.class) && (targetType != short.class) ) {
				mismatchedSize = true;
			}
			break;
		case "32_bits": //$NON-NLS-1$
			/* The 32-bits layout is shared by boolean, int and float
			 * given the boolean type is treated as int in Java.
			 */
			if ((targetType != boolean.class)
			&& (targetType != int.class)
			&& (targetType != float.class)
			) {
				mismatchedSize = true;
			}
			break;
		case "64_bits": //$NON-NLS-1$
			/* The 32-bits layout is shared by long, double and the MemoryAddress class
			 * given the corresponding pointer size is 32 bits in C.
			 */
			if ((targetType != long.class)
			&& (targetType != double.class)
			&& (targetType != MemoryAddress.class)
			) {
				mismatchedSize = true;
			}
			break;
		default:
			mismatchedSize = true;
			break;
		}
		
		if (mismatchedSize) {
			throw new IllegalArgumentException("Mismatched size between the layout and the type: layout = " //$NON-NLS-1$
												+ TypeLayout + ", type = " + targetType);  //$NON-NLS-1$
		}
	}
	
	private static void validateLayoutType(MemoryLayout TypeLayout, Class<?> targetType) {
		String layoutType = "VOID"; //$NON-NLS-1$
		boolean mismatchType = false;
		
		if (TypeLayout != null) {
			String[] layoutString = TypeLayout.toString().split("="); //$NON-NLS-1$
			layoutString = layoutString[1].split("]"); //$NON-NLS-1$
			layoutType = layoutString[0];
		}
		
		switch (layoutType) {
		case "CHAR": //$NON-NLS-1$
			/* the CHAR layout (8bits) in Java only matches with byte in C */
			break;
		case "SHORT": //$NON-NLS-1$
			/* the SHORT layout (16bits) in Java only matches char and short in C */
			break;
		case "INT": //$NON-NLS-1$
			/* the INT layout (32bits) in Java only matches boolean and int in C */
			if ((targetType != boolean.class) && (targetType != int.class)) {
				mismatchType = true;
			}
			break;
		case "FLOAT": //$NON-NLS-1$
			/* the FLOAT layout (32bits) in Java only matches float in C */
			if (targetType != float.class) {
				mismatchType = true;
			}
			break;
		case "LONG": //$NON-NLS-1$
		case "LONG_LONG": //$NON-NLS-1$
			/* the LONG/LONG_LONG layout (64bits) in Java only matches long in C */
			if (targetType != long.class) {
				mismatchType = true;
			}
			break;
		case "POINTER": //$NON-NLS-1$
			/* the POINTER layout (64bits) in Java only matches MemoryAddress */
			if (targetType != MemoryAddress.class) {
				mismatchType = true;
			}
			break;
		case "DOUBLE": //$NON-NLS-1$
			/* the DOUBLE layout (64bits) in Java only matches double in C */
			if (targetType != double.class) {
				mismatchType = true;
			}
			break;
		case "VOID": //$NON-NLS-1$
			if (targetType != void.class) {
				mismatchType = true;
			}
			break;
		default:
			mismatchType = true;
			break;
		}
		
		if (mismatchType) {
			throw new IllegalArgumentException("Mismatch between the layout and the type: layout = "  //$NON-NLS-1$
												+ ((TypeLayout == null) ? layoutType : TypeLayout)
												+ ", type = " + targetType);  //$NON-NLS-1$
		}
		
		System.out.println("validateLayoutType: layoutType = " + layoutType + ", targetType = " + targetType); //$NON-NLS-1$
	}
	
	@SuppressWarnings("boxing")
	private static long convertArgToLongValue(Class<?> argLayoutClass, Object argValue) {
		if (argLayoutClass == boolean.class) {
			boolean boolValue = ((Boolean)argValue).booleanValue();
			long tmpValue = boolValue ? 1 : 0;
			return tmpValue;
		} else if (argLayoutClass == char.class) {
			return ((Character)argValue).charValue();
		} else if (argLayoutClass == float.class) {
			float tmpValue = Float.valueOf(argValue.toString());
			return Float.floatToIntBits(tmpValue);
		} else if (argLayoutClass == double.class) {
			double tmpValue = Double.valueOf(argValue.toString());
			return Double.doubleToLongBits(tmpValue);
		} else if (argLayoutClass == MemoryAddress.class) {
			return ((MemoryAddress)argValue).toRawLongValue();
		}
		return Long.valueOf(argValue.toString());
	}
	
	@SuppressWarnings("boxing")
	private Object processReturnVal(long retValue) {
		System.out.println("###processReturnVal: retValue = " + retValue + " ###\n\n");
		System.out.println("\n###########################################\n");
		
		if (retLayoutClass == boolean.class) {
			boolean tmpValue = (retValue == 1) ? true : false;
			return tmpValue;
		} else if (retLayoutClass == char.class) {
			return (char)retValue;
		} else if (retLayoutClass == byte.class) {
			return (byte)retValue;
		} else if (retLayoutClass == short.class) {
			return (short)retValue;
		} else if (retLayoutClass == int.class) {
			return (int)retValue;
		} else if (retLayoutClass == float.class) {
			return Float.intBitsToFloat((int)retValue);
		} else if (retLayoutClass == double.class) {
			return Double.longBitsToDouble(retValue);
		} else if (retLayoutClass == MemoryAddress.class) {
			MemoryAddress memAddr = MemoryAddress.ofLong(retValue);
			memAddrReferences.add(memAddr);
			return memAddr;
		}
		return retValue;
	}
}

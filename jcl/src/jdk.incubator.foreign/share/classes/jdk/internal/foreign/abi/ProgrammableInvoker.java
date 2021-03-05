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
import java.lang.invoke.MethodHandles.Lookup;

import java.util.Objects;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.VaList;

/**
 * The counterpart in OpenJDK is replaced with this class that wrap up a method handle
 * enabling the native code to the ffi_call via the libffi interface at runtime.
 */
public class ProgrammableInvoker {
	
	private final MethodType funcMethodType;
	private final FunctionDescriptor funcDescriptor;
	private final long functionAddr;
	private long cifNativeThunkAddr;
	private long argTypesAddr;
	private Class<?>[] argLayoutClasses;
	private Class<?> retLayoutClass;
	private int argCount;
	
	final static Lookup lookup = MethodHandles.lookup();
	
	/* The ffi_cif and the corresponding argument types are shared in multiple downcalls or across threads */
	private static final ConcurrentHashMap<FunctionDescriptor, Long> cachedCifNativeThunkAddr = new ConcurrentHashMap<FunctionDescriptor, Long>();
	private static final ConcurrentHashMap<List<MemoryLayout>, Long> cachedArgTypes = new ConcurrentHashMap<List<MemoryLayout>, Long>();
	
	private static synchronized native void initInstanceFieldOffset();
	private native void initCifNativeThunkData(Class<?>[] argLayoutClasses, Class<?> retLayoutClass, boolean newArgTypes, boolean varArgsExist);
	private native long invokeNative(long functionAddr, long calloutThunk, long[] argValues);
	
	private static final class PrivateThreadLock {
		PrivateThreadLock() {}
	}
	private static Object lock = new PrivateThreadLock();
	
	static {
		/* Resolve the required fields (specifically their offset in the jcl constant pool of VM)
		 * which can be shared in multiple calls or across threads given the generated macros
		 * in the vmconstantpool.xml depend on their offsets to access the corresponding fields.
		 * Note: the value of these fields varies with different instance.
		 */
		initInstanceFieldOffset();
	}
	
	ProgrammableInvoker(long functionAddress, MethodType functionMethodType, FunctionDescriptor functionDescriptor) {
		functionAddr = functionAddress;
		funcMethodType = functionMethodType;
		funcDescriptor = functionDescriptor;
		cifNativeThunkAddr = 0;
		argTypesAddr = 0;
		argCount = 0;
		generateAdapter();
	}
	
	private void generateAdapter() {
		retLayoutClass = void.class;
		Optional<MemoryLayout> funcDescReturnLayout = funcDescriptor.returnLayout();
		if (funcDescReturnLayout.isPresent()) {
			retLayoutClass = convertLayoutToClass(funcDescReturnLayout.get(), funcMethodType.returnType());
		}
		
		List<MemoryLayout> argLayouts = funcDescriptor.argumentLayouts();
		argCount = argLayouts.size();
		argLayoutClasses = new Class<?>[argCount];
		MemoryLayout[] argLayoutArray = argLayouts.toArray(new MemoryLayout[argCount]);
		Class<?>[] argTypes = funcMethodType.parameterArray();
		for (int argIndex = 0; argIndex < argCount; argIndex++) {
			System.out.println("argTypes[" + argIndex + "] = " + argTypes[argIndex]);
			argLayoutClasses[argIndex] =  convertLayoutToClass(argLayoutArray[argIndex], argTypes[argIndex]);
		}
		
		synchronized(lock) {
			System.out.println("\n\n************** generateAdapter: BEGIN ********************");
			System.out.println("generateAdapter: funcDescriptor = " + funcDescriptor);
			System.out.println("generateAdapter: argTypesAddr = " + argTypesAddr);
			
			/* If a ffi_cif for a given function descriptor exists, then the corresponding argument types
			 * were already created to set up ffi_cif, in which case there is no need to check the argument types.
			 * If not the case, then we need to check whether the same argument types exists in the cache to
			 * avoid duplicate allocation for the same argument layouts.
			 */
			if (cachedCifNativeThunkAddr.containsKey(funcDescriptor)) {
				cifNativeThunkAddr = cachedCifNativeThunkAddr.get(funcDescriptor).longValue();
				argTypesAddr = cachedArgTypes.get(argLayouts).longValue();
				System.out.println("generateAdapter: funcDescriptor exists: cifNativeThunkAddr = " + Long.toHexString(cifNativeThunkAddr));
				System.out.println("generateAdapter: argLayouts exists: argTypesAddr = " + Long.toHexString(argTypesAddr));
			} else {
				boolean newArgTypes = cachedArgTypes.containsKey(argLayouts) ? false : true;
				if (!newArgTypes) {
					argTypesAddr = cachedArgTypes.get(argLayouts).longValue();
					System.out.println("generateAdapter: argLayouts exists: argTypesAddr = " + Long.toHexString(argTypesAddr));
				}
				
				boolean varArgsExist = (argLayoutClasses[argCount - 1] == VaList.class) ? true : false;
				System.out.println("generateAdapter: argLayoutClasses[" + (argCount - 1) + "] = " + argLayoutClasses[argCount - 1]);
				System.out.println("generateAdapter: varArgsExist = " + varArgsExist);
				initCifNativeThunkData(argLayoutClasses, retLayoutClass, newArgTypes, varArgsExist);
				
				/* Cache the address of cif and argTypes after setting up via the out-of-line native code */
				if (newArgTypes) {
					System.out.println("generateAdapter: argLayouts DOES NOT exists: SET argTypesAddr = " + Long.toHexString(argTypesAddr));
					cachedArgTypes.put(argLayouts, Long.valueOf(argTypesAddr));
				}
				System.out.println("generateAdapter: funcDescriptor DOES NOT exists: SET cifNativeThunkAddr = " + Long.toHexString(cifNativeThunkAddr));
				cachedCifNativeThunkAddr.put(funcDescriptor, Long.valueOf(cifNativeThunkAddr));
			}
			System.out.println("****************** generateAdapter: END ********************");
		}
	}
	
	@SuppressWarnings("javadoc")
	public static MethodHandle getBoundMethodHandle(Addressable functionAddr, MethodType functionMethodType, FunctionDescriptor funcDesc) {
		Class<?>[] argTypes = functionMethodType.parameterArray();
		for (int argIndex = 0; argIndex < argTypes.length; argIndex++) {
			System.out.println("getBoundMethodHandle: argTypes[" + argIndex + "] = " + argTypes[argIndex]);
		}
		checkIfValidType(functionMethodType);
		ProgrammableInvoker nativeInvoker = new ProgrammableInvoker(functionAddr.address().toRawLongValue(), functionMethodType, funcDesc);
		try {
			MethodHandle boundHandle = lookup.bind(nativeInvoker, "runNativeMethod", MethodType.methodType(Object.class, Object[].class));
			
			/* Replace the bound handle with the specified types of the native function */
			boundHandle = permuteMH(boundHandle, functionMethodType);
			return boundHandle;
		} catch (NoSuchMethodException | IllegalAccessException | NullPointerException | WrongMethodTypeException e) {
			throw new InternalError(e);
		}
	}
	
	private static void checkIfValidType(MethodType targetMethodType) {
		/* Throw InternalError if the newType contains non-primitives. */
		Class<?> retType = targetMethodType.returnType();
		if (!checkPrimitiveTypeClass(retType) && (retType != void.class) && (retType != MemoryAddress.class)) {
			throw new InternalError("The return type is neither primitive nor MemoryAddress");  //$NON-NLS-1$
		}

		Class<?>[] argTypes = targetMethodType.parameterArray();
		for (int argIndex = 0; argIndex < argTypes.length; argIndex++) {
			if (!checkPrimitiveTypeClass(argTypes[argIndex])) {
				if ((argTypes[argIndex] == VaList.class) && (argIndex < (argTypes.length - 1))) {
					throw new InternalError("The varargs must be at the end of the passed-in arguments"); //$NON-NLS-1$
				}
				if ((argTypes[argIndex] != VaList.class) && (argTypes[argIndex] != MemoryAddress.class)) {
					throw new InternalError("The passed-in arguments contain a non-primitive type which is neither VaList nor MemoryAddress"); //$NON-NLS-1$
				}
			}
		}
	}
	
	private static boolean checkPrimitiveTypeClass(Class<?> targetType) {
		if (!targetType.isPrimitive()
		&& (targetType != Boolean.class)
		&& (targetType != Byte.class)
		&& (targetType != Character.class)
		&& (targetType != Short.class)
		&& (targetType != Integer.class)
		&& (targetType != Long.class)
		&& (targetType != Float.class)
		&& (targetType != Double.class)
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
		System.out.println("calling runNativeMethod...");
		System.out.println("runNativeMethod: functionAddr = " + Long.toHexString(functionAddr));
		System.out.println("runNativeMethod: cifNativeThunkAddr = " + Long.toHexString(cifNativeThunkAddr));
		
		long newArgs[] = new long[argCount];
		for (int argIndex = 0; argIndex < argCount; argIndex++) {
			System.out.println("args[" + argIndex + "] = " + args[argIndex]);
			System.out.println("argLayoutClasses[" + argIndex + "] = " + argLayoutClasses[argIndex]);
			newArgs[argIndex] = MapArgValueToLong(argLayoutClasses[argIndex], args[argIndex]);
			System.out.println("newArgs[" + argIndex + "] = " + newArgs[argIndex]);
		}
		
		long returnVal = invokeNative(functionAddr, cifNativeThunkAddr, newArgs);
		System.out.println("runNativeMethod: returnVal = " + returnVal);
		
		/* Process the return value depending on the return type */
		Object processedReturnVal = processReturnVal(returnVal);
		return processedReturnVal;
	}
	
	private static Class<?> convertLayoutToClass(MemoryLayout TypeLayout, Class<?> targetType) {
		String[] layoutString = TypeLayout.toString().split("="); //$NON-NLS-1$
		layoutString = layoutString[1].split("]"); //$NON-NLS-1$
		String layoutType = TypeLayout.bitSize() + "_" + layoutString[0]; //$NON-NLS-1$
		boolean mismatchType = false;
		
		Class<?> clazz = null;
		switch (layoutType) {
		case "8_CHAR": //$NON-NLS-1$
			/* The char size is 8 bits in C while it is 16 bits in Java.
			 * Thus, 8 bits (in C) is only intended for byte in Java.
			 */
			if ((targetType == byte.class) || (targetType == Byte.class)) {
				clazz = byte.class;
			} else {
				mismatchType = true;
			}
			break;
		case "16_SHORT": //$NON-NLS-1$
			/* The char size is 16 bits in Java */
			if ((targetType == char.class) || (targetType == Character.class)) {
				clazz = char.class;
			} else if ((targetType == short.class) || (targetType == Short.class)) {
				clazz = short.class;
			} else {
				mismatchType = true;
			}
			break;
		case "32_INT": //$NON-NLS-1$
			/* The boolean size is 32 bits in Java.*/
			if ((targetType == boolean.class) || (targetType == Boolean.class)) {
				clazz = boolean.class;
			} else if ((targetType == int.class) || (targetType == Integer.class)) {
				clazz = int.class;
			} else {
				mismatchType = true;
			}
			break;
		case "64_LONG": //$NON-NLS-1$
		case "64_LONG_LONG": //$NON-NLS-1$
			if ((targetType == long.class) || (targetType == Long.class)) {
				clazz = long.class;
			} else {
				mismatchType = true;
			}
			break;
		case "64_POINTER": //$NON-NLS-1$
			/* Both MemoryAddress and CLinker.VaList are literally pointers to a given memory location */
			if ((targetType == MemoryAddress.class) || (targetType == VaList.class)) {
				clazz = targetType;
			} else {
				mismatchType = true;
			}
			break;
		case "32_FLOAT": //$NON-NLS-1$
			if ((targetType == float.class) || (targetType == Float.class)) {
				clazz = float.class;
			} else {
				mismatchType = true;
			}
			break;
		case "64_DOUBLE": //$NON-NLS-1$
			if ((targetType == double.class) || (targetType == Double.class)) {
				clazz = double.class;
			} else {
				mismatchType = true;
			}
			break;
		default:
			break;
		}
		
		if (mismatchType) {
			throw new InternalError("Mismatch between the layout and the type: layout = " + TypeLayout + ", type = " + targetType);  //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		System.out.println("layoutType = " + layoutType + ", targetType = " + targetType + ": clazz = " + clazz); //$NON-NLS-1$
		return clazz;
	}
	
	@SuppressWarnings("boxing")
	private static long MapArgValueToLong(Class<?> argLayoutClass, Object argValue) {
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
		System.out.println("processReturnVal: retValue = " + retValue);
		
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
		}
		return retValue;
	}
}

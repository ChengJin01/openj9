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

import java.util.Optional;
import java.util.List;
import java.util.HashMap;
import java.util.OptionalLong;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import static java.lang.invoke.MethodType.methodType;
import java.lang.invoke.WrongMethodTypeException;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ValueLayout;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.LibraryLookup;
import static jdk.incubator.foreign.LibraryLookup.Symbol;
import jdk.incubator.foreign.CLinker.TypeKind;
import static jdk.incubator.foreign.CLinker.TypeKind.*;

/**
 * The counterpart in OpenJDK is replaced with this class that wrap up a method handle
 * enabling the native code to the ffi_call via the libffi interface at runtime.
 */
public class ProgrammableInvoker {
	
	private final MethodType funcMethodType;
	private final FunctionDescriptor funcDescriptor;
	private final Addressable functionAddr;
	private long cifNativeThunkAddr;
	private long argTypesAddr;
	private List<MemoryLayout> argLayouts;
	private MemoryLayout[] argLayoutArray;
	private MemoryLayout realReturnLayout;
	
	final static Lookup lookup = MethodHandles.lookup();
	
	/* The prep_cif and the corresponding argument layouts are cached & shared in multiple downcalls/threads */
	private static final HashMap<String, Long> cachedCifNativeThunkAddr = new HashMap<String, Long>();
	private static final HashMap<List<MemoryLayout>, Long> cachedArgLayouts = new HashMap<List<MemoryLayout>, Long>();

	/* Filters that convert the primitive types & objects (wrapped up by the primitive classes wrapper) to long */
	private static MethodHandle booleanToLongFilter = null;
	private static MethodHandle charToLongFilter = null;
	private static MethodHandle byteToLongFilter = null;
	private static MethodHandle shortToLongFilter = null;
	private static MethodHandle intToLongFilter = null;
	private static MethodHandle floatToLongFilter = null;
	private static MethodHandle doubleToLongFilter = null;
	private static MethodHandle memAddrToLongFilter = null;
	private static MethodHandle memSegmtToLongFilter = null;
	
	private static MethodHandle booleanObjToLongFilter = null;
	private static MethodHandle charObjToLongFilter = null;
	private static MethodHandle byteObjToLongFilter = null;
	private static MethodHandle shortObjToLongFilter = null;
	private static MethodHandle intObjToLongFilter = null;
	private static MethodHandle longObjToLongFilter = null;
	private static MethodHandle floatObjToLongFilter = null;
	private static MethodHandle doubleObjToLongFilter = null;
	
	private static synchronized native void resolveRequiredFields();
	private native void initCifNativeThunkData(String[] argLayouts, String retLayout, boolean newArgTypes);
	private native long invokeNative(long functionAddress, long calloutThunk, long[] argValues);
	
	private static final class PrivateClassLock {
		PrivateClassLock() {}
	}
	private static final Object privateClassLock = new PrivateClassLock();
	
	static {
		try {
			/* Set up the filters of the primitive types & objects */
			booleanToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "booleanToLong", methodType(long.class, boolean.class)); //$NON-NLS-1$
			charToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "charToLong", methodType(long.class, char.class)); //$NON-NLS-1$
			byteToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "byteToLong", methodType(long.class, byte.class)); //$NON-NLS-1$
			shortToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "shortToLong", methodType(long.class, short.class)); //$NON-NLS-1$
			intToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "intToLong", methodType(long.class, int.class)); //$NON-NLS-1$
			floatToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "floatToLong", methodType(long.class, float.class)); //$NON-NLS-1$
			doubleToLongFilter = lookup.findStatic(Double.class, "doubleToLongBits", methodType(long.class, double.class)); //$NON-NLS-1$
			memAddrToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "memAddrToLong", methodType(long.class, MemoryAddress.class)); //$NON-NLS-1$
			memSegmtToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "memSegmtToLong", methodType(long.class, MemorySegment.class)); //$NON-NLS-1$

			booleanObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "booleanObjToLong", methodType(long.class, Boolean.class)); //$NON-NLS-1$
			charObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "charObjToLong", methodType(long.class, Character.class)); //$NON-NLS-1$
			byteObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "byteObjToLong", methodType(long.class, Byte.class)); //$NON-NLS-1$
			shortObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "shortObjToLong", methodType(long.class, Short.class)); //$NON-NLS-1$
			intObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "intObjToLong", methodType(long.class, Integer.class)); //$NON-NLS-1$
			longObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToLong", methodType(long.class, Long.class)); //$NON-NLS-1$
			floatObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "floatObjToLong", methodType(long.class, Float.class)); //$NON-NLS-1$
			doubleObjToLongFilter = lookup.findStatic(ProgrammableInvoker.class, "doubleObjToLong", methodType(long.class, Double.class)); //$NON-NLS-1$
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		/* Resolve the required fields (specifically their offset in the jcl constant pool of VM)
		 * which can be shared in multiple calls or across threads given the generated macros
		 * in the vmconstantpool.xml depend on their offsets to access the corresponding fields.
		 * Note: the value of these fields varies with different instances.
		 */
		resolveRequiredFields();
	}
	
	/* Intended for booleanToLongFilter that converts boolean to long */
	private static final long booleanToLong(boolean argValue) {
		return (argValue ? 1 : 0);
	}
	
	/* Intended for charToLongFilter that converts char to long */
	private static final long charToLong(char argValue) {
		return argValue;
	}
	
	/* Intended for byteToLongFilter that converts byte to long */
	private static final long byteToLong(byte argValue) {
		return argValue;
	}
	
	/* Intended for shortToLongFilter that converts short to long given
	 * short won't be casted to long automatically in filterArguments() 
	 */
	private static final long shortToLong(short argValue) {
		return argValue;
	}
	
	/* Intended for intToLongFilter that converts int to long given
	 * int won't be casted to long automatically in filterArguments() 
	 */
	private static final long intToLong(int argValue) {
		return argValue;
	}
	
	/* Intended for floatToLongFilter that converts the int value from Float.floatToIntBits()
	 * to long given int won't be casted to long automatically in filterArguments() 
	 */
	private static final long floatToLong(float argValue) {
		return Float.floatToIntBits(argValue);
	}
	
	/* Intended for memAddrToLongFilter that converts the memory address to long */
	private static final long memAddrToLong(MemoryAddress argValue) {
		return argValue.toRawLongValue();
	}
	
	/* Intended for memAddrToLongFilter that converts the memory segment to long */
	private static final long memSegmtToLong(MemorySegment argValue) {
		return argValue.address().toRawLongValue();
	}
	
	/* Intended for booleanObjToLongFilter that converts the Boolean object to long */
	private static final long booleanObjToLong(Boolean argValue) {
		return (argValue.booleanValue() ? 1 : 0);
	}
	
	/* Intended for charObjToLongFilter that converts the Character object to long */
	private static final long charObjToLong(Character argValue) {
		return argValue.charValue();
	}
	
	/* Intended for byteObjToLongFilter that converts the Byte object to long */
	private static final long byteObjToLong(Byte argValue) {
		return argValue.longValue();
	}
	
	/* Intended for shortObjToLongFilter that converts the Short object to long */
	private static final long shortObjToLong(Short argValue) {
		return argValue.longValue();
	}
	
	/* Intended for intObjToLongFilter that converts the Integer object to long */
	private static final long intObjToLong(Integer argValue) {
		return argValue.longValue();
	}
	
	/* Intended for longObjToLongFilter that converts the Long object to long */
	private static final long longObjToLong(Long argValue) {
		return argValue.longValue();
	}
	
	/* Intended for floatObjToLongFilter that converts the Float object to long */
	private static final long floatObjToLong(Float argValue) {
		return Float.floatToIntBits(argValue.floatValue());
	}
	
	/* Intended for doubleObjToLongFilter that converts the Double object to long */
	private static final long doubleObjToLong(Double argValue) {
		return Double.doubleToLongBits(argValue.doubleValue());
	}
	
	ProgrammableInvoker(Addressable downcallAddr, MethodType functionMethodType, FunctionDescriptor functionDescriptor) {
		checkIfValidLayoutAndType(functionMethodType, functionDescriptor);
		
		/* As explained in the Spec of LibraryLookup, the downcall must hold a strong reference to
		 * the native library symbol to prevent the underlying native library from being unloaded
		 * during the native calls.
		 * 
		 * Note: the passed-in addressable parameter can be either LibraryLookup.Symbol or MemoryAddress.
		 */
		functionAddr = downcallAddr;
		funcMethodType = functionMethodType;
		funcDescriptor = functionDescriptor;
		cifNativeThunkAddr = 0;
		argTypesAddr = 0;
		generateAdapter();
	}
	
	/* Map the layouts of return type & argument types to the underlying prep_cif */
	private void generateAdapter() {
		/* Set the void layout string intended for the underlying native code
		 * as the corresponding layout doesn't exist in the Spec.
		 * Note: 'V' stands for the void type and 0 means zero byte.
		 */
		String retLayoutStr = "0V"; //$NON-NLS-1$
		if (realReturnLayout != null) {
			retLayoutStr = realReturnLayout.byteSize() + preprocessLayoutString(realReturnLayout);
		}
		
		int argLayoutCount = argLayoutArray.length;
		String[] argLayoutStrs = new String[argLayoutCount];
		for (int argIndex = 0; argIndex < argLayoutCount; argIndex++) {
			MemoryLayout argLayout = argLayoutArray[argIndex];
			/* Prefix the size of layout to the layout string to be parsed in native */
			argLayoutStrs[argIndex] = argLayout.byteSize() + preprocessLayoutString(argLayout);
		}
		
		synchronized(privateClassLock) {
			/* If a prep_cif for a given function descriptor exists, then the corresponding return & argument layouts
			 * were already set up for this prep_cif, in which case there is no need to check the layouts.
			 * If not the case, check at first whether the same return & argument layouts exist in the cache
			 * in case of duplicate memory allocation for the same layouts.
			 * 
			 * Note: an int method (e.g. '(int,int)int') and a method function (e.g.'(boolean,boolean)boolean') share
			 * the same C_INT layouts (e.g. 'b32[abi/kind=INT]b32[abi/kind=INT])b32[abi/kind=INT') but the underlying C
			 * functions might be different from each other as follows:
			 * e.g. 
			 * int f1(int intArg1, int intArg2){ (the layouts are C_INT)
			 *     int sum = intArg1 + intArg2;
			 *     return sum;
			 * } 
			 * and 
			 * int f2(int boolArg1, bool boolArg2) (the layouts are C_INT)
			 * {  
			 *     int boolSum = boolArg1 || boolArg2;
			 *     return boolSum;
			 * }
			 * 
			 * To avoid overriding the prep_cif that is already cached for the same C_INT layouts, the hashcode of
			 * the function descriptor plus the corresponding function address is used to determine whether the int
			 * function and the boolean function come from the same C function or not; otherwise, a new prep_cif intended
			 * for the boolean function should be created in the native code.
			 */
			long functionAddrLongValue = functionAddr.address().toRawLongValue();
			String funcDescHashPlusFuncAddr = funcDescriptor.hashCode() + "@" + functionAddrLongValue; //$NON-NLS-1$
			
			if (cachedCifNativeThunkAddr.containsKey(funcDescHashPlusFuncAddr)) {
				cifNativeThunkAddr = cachedCifNativeThunkAddr.get(funcDescHashPlusFuncAddr).longValue();
				argTypesAddr = cachedArgLayouts.get(argLayouts).longValue();
			} else {
				boolean newArgTypes = cachedArgLayouts.containsKey(argLayouts) ? false : true;
				if (!newArgTypes) {
					argTypesAddr = cachedArgLayouts.get(argLayouts).longValue();
				}
				
				/* Prepare the prep_cif for the native function specified by the arguments/return layouts */
				initCifNativeThunkData(argLayoutStrs, retLayoutStr, newArgTypes);
				
				/* Cache the address of prep_cif and argTypes after setting up via the out-of-line native code */
				if (newArgTypes) {
					cachedArgLayouts.put(argLayouts, Long.valueOf(argTypesAddr));
				}
				cachedCifNativeThunkAddr.put(funcDescHashPlusFuncAddr, Long.valueOf(cifNativeThunkAddr));
			}
		}
	}
	
	/* Preprocess the layout string to remove all description for easier handling in native.
	 * e.g. a nested struct layout string such as
	 * [
	 *   [
	 *    b32(elem1)[abi/kind=INT,layout/name=elem1]
	 *    b32(elem2)[abi/kind=INT,layout/name=elem2]
	 *   ](Struct1_II)[layout/name=Struct1_II]
	 *   [
	 *    b32(elem1)[abi/kind=INT,layout/name=elem1]
	 *    b32(elem2)[abi/kind=INT,layout/name=elem2]
	 *    ](Struct2_II)[layout/name=Struct2_II]
	 *   ](nested_struct)[layout/name=nested_struct]
	 * 
	 * ends up with
	 * 
	 *   [
	 *     [
	 *      I  (C_INT)
	 *      I  (C_INT)
	 *     ]
	 *     [
	 *      I  (C_INT)
	 *      I  (C_INT)
	 *     ]
	 *   ]
	 */
	private static String preprocessLayoutString(MemoryLayout targetLayout) {
		String resultLayoutString = prefixCountOfStructElements(targetLayout);
		
		/* Remove the layout name in parentheses if exits. e.g. "(c_int)" */
		resultLayoutString = resultLayoutString.replaceAll("\\(.*?\\)", "") //$NON-NLS-1$ //$NON-NLS-2$
					/* Remove the primitive layout name in square brackets if exits. e.g. ",layout/name=C_INT" */
					.replaceAll("\\,layout/name=.*?\\]", "\\]") //$NON-NLS-1$ //$NON-NLS-2$
					/* Remove the struct layout name in square brackets if exits. e.g. "[layout/name=struct_II]" */
					.replaceAll("\\[layout/name=.*?\\]", "") //$NON-NLS-1$ //$NON-NLS-2$
					/* Remove all the primitives' size in the layout string.
					 * e.g. remove "b32" for little-endianness and "B" for big-endianness */
					.replaceAll("b\\d+", "").replaceAll("B\\d+", "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					/* Remove all the padding bits in the layout string. e.g. remove "x16" */
					.replaceAll("x\\d+", "") //$NON-NLS-1$ //$NON-NLS-2$
					/* Convert all primitives to a single letter for easier handling in native */
					.replaceAll("\\[abi/kind=CHAR\\]", "C") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=SHORT\\]", "S") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=INT\\]", "I") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=LONG\\]", "L") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=LONG_LONG\\]", "L") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=FLOAT\\]", "F") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=DOUBLE\\]", "D") //$NON-NLS-1$ //$NON-NLS-2$
					.replaceAll("\\[abi/kind=POINTER\\]", "P"); //$NON-NLS-1$ //$NON-NLS-2$
		return resultLayoutString;
	}
	
	/* Recursively prefix the count of struct elements (namely the count of ffi_types in a struct) 
	 * to the front of struct layout string.
	 */
	private static String prefixCountOfStructElements(MemoryLayout targetLayout) {
		String resultLayoutString = targetLayout.toString();
		
		MemoryLayout resultLayout = targetLayout;
		boolean isSequenceLayout = false;
		long seqElementCount = 0;
		if (SequenceLayout.class.isInstance(targetLayout)) {
			resultLayout = ((SequenceLayout)targetLayout).elementLayout();
			seqElementCount = ((SequenceLayout)targetLayout).elementCount().getAsLong();
			isSequenceLayout = true;
		}
		
		if (GroupLayout.class.isInstance(resultLayout)) {
			List<MemoryLayout> layoutElements = ((GroupLayout)resultLayout).memberLayouts();
			int structElementCount = layoutElements.size();
			String elementLayoutStrs = ""; //$NON-NLS-1$
			int paddingElementCount = 0;
			for (int elementIndex = 0; elementIndex < structElementCount; elementIndex++) {
				MemoryLayout structElement = layoutElements.get(elementIndex);
				String structElementStr = structElement.toString().toLowerCase();
				/* Skip the padding element (e.g. x16) if exists */
				if (structElementStr.startsWith("x")) { //$NON-NLS-1$
					paddingElementCount += 1;
				} else {
					elementLayoutStrs += prefixCountOfStructElements(structElement);
				}
			}
			/* Exclude all the padding elements in the struct layout string
			 * and prefix "#" to identify the start of this layout string.
			 */
			resultLayoutString = "#" + (layoutElements.size() - paddingElementCount)
									+ "[" + elementLayoutStrs + "]"; //$NON-NLS-1$ //$NON-NLS-2$

			if (isSequenceLayout) {
				/* Prefix the count of sequence element to the front of the SequenceLayout string */
				resultLayoutString = seqElementCount + ":" + resultLayoutString;
			}
		} else if (isSequenceLayout) {
			/* Remove "[" and "]" of the SequenceLayout string in the case of primitive elements */
			resultLayoutString = resultLayoutString.substring(1, resultLayoutString.length() - 1);
		}
		
		return resultLayoutString;
	}
	
	/* The method is ultimately invoked by Clinker on the specific platforms to generate the requested
	 * method handle to the underlying C function.
	 */
	@SuppressWarnings("javadoc")
	public static MethodHandle getBoundMethodHandle(Addressable downcallAddr, MethodType functionMethodType, FunctionDescriptor funcDesc) {
		ProgrammableInvoker nativeInvoker = new ProgrammableInvoker(downcallAddr, functionMethodType, funcDesc);
		try {
			MethodHandle boundHandle = lookup.bind(nativeInvoker, "runNativeMethod", methodType(Object.class, long[].class));
			
			/* Replace the original handle with the specified types of the C function */
			boundHandle = permuteMH(boundHandle, functionMethodType);
			return boundHandle;
		} catch (ReflectiveOperationException e) {
			throw new InternalError(e);
		}
	}
	
	/* Collect and convert the passed-in arguments to an Object array for the underlying native call */
	private static MethodHandle permuteMH(MethodHandle targetHandle, MethodType nativeMethodType) throws NullPointerException, WrongMethodTypeException {
		Class<?>[] argTypeClasses = nativeMethodType.parameterArray();
		int nativeArgCount = argTypeClasses.length;
		MethodHandle resultHandle = targetHandle.asCollector(long[].class, nativeArgCount);

		/* Convert the values of the primitive types to long via filterArguments() prior to the native call */
		MethodHandle[] argFilters = new MethodHandle[nativeArgCount];
		for (int argIndex = 0; argIndex < nativeArgCount; argIndex++) {
			argFilters[argIndex] = getArgumentFilter(argTypeClasses[argIndex]);
		}
		resultHandle = filterArguments(resultHandle, 0, argFilters).asType(nativeMethodType);
		return resultHandle;
	}
	
	/* Obtain the filter that converts the passed-in argument to long against its type */
	private static MethodHandle getArgumentFilter(Class<?> argTypeClass) {
		Class<?> realArgTypeClass = unboxingPrimitiveClassType(argTypeClass);
		MethodHandle filterMH = null;
		
		if (realArgTypeClass == boolean.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? booleanToLongFilter : booleanObjToLongFilter;
		} else if (realArgTypeClass == char.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? charToLongFilter : charObjToLongFilter;
		} else if (realArgTypeClass == byte.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? byteToLongFilter : byteObjToLongFilter;
		} else if (realArgTypeClass == short.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? shortToLongFilter : shortObjToLongFilter;
		} else if (realArgTypeClass == int.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? intToLongFilter : intObjToLongFilter;
		} else if (realArgTypeClass == long.class) {
			/* Set the filter to null in the case of long by default as there is no conversion for long */
			filterMH = (argTypeClass == realArgTypeClass) ? null : longObjToLongFilter;
		} else if (realArgTypeClass == float.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? floatToLongFilter : floatObjToLongFilter;
		} else if (realArgTypeClass == double.class) {
			filterMH = (argTypeClass == realArgTypeClass) ? doubleToLongFilter : doubleObjToLongFilter;
		} else if (realArgTypeClass == MemoryAddress.class) {
			filterMH = memAddrToLongFilter;
		} else if (realArgTypeClass == MemorySegment.class) {
			filterMH = memSegmtToLongFilter;
		}
		
		return filterMH;
	}
	
	/* The method (bound by the method handle to the native code) intends to invoke the C function via the inlined code */
	Object runNativeMethod(long[] args) {
		long returnVal = invokeNative(functionAddr.address().toRawLongValue(), cifNativeThunkAddr, args);
		
		/* Process the return value depending on the return type */
		Object processedReturnVal = processReturnVal(returnVal);
		return processedReturnVal;
	}
	
	/* Convert the returned long value from the C function against the specified return type at Java level */
	@SuppressWarnings("boxing")
	private Object processReturnVal(long retValue) {
		Class<?> realReturnType = unboxingPrimitiveClassType(funcMethodType.returnType());
		if (realReturnType == boolean.class) {
			boolean tmpValue = (retValue == 1) ? true : false;
			return tmpValue;
		} else if (realReturnType == char.class) {
			return (char)retValue;
		} else if (realReturnType == byte.class) {
			return (byte)retValue;
		} else if (realReturnType == short.class) {
			return (short)retValue;
		} else if (realReturnType == int.class) {
			return (int)retValue;
		} else if (realReturnType == float.class) {
			return Float.intBitsToFloat((int)retValue);
		} else if (realReturnType == double.class) {
			return Double.longBitsToDouble(retValue);
		} else if (realReturnType == MemoryAddress.class) {
			return MemoryAddress.ofLong(retValue);
		} else if (realReturnType == MemorySegment.class) {
			MemoryAddress memSegmtAddr = MemoryAddress.ofLong(retValue);
			return memSegmtAddr.asSegmentRestricted(realReturnLayout.byteSize());
		}
		return retValue;
	}
	
	/* Verify whether the specified layout and the corresponding type are valid and match each other.
	 * Note: will update after the struct layout (phase 2 & 3) is fully implemented.
	 */
	private void checkIfValidLayoutAndType(MethodType targetMethodType, FunctionDescriptor funcDesc) {
		Class<?> retType = targetMethodType.returnType();
		if (!validateArgRetTypeClass(retType) && (retType != void.class)) {
			throw new IllegalArgumentException("The return type must be primitive/void, MemoryAddress or MemoryAddress" //$NON-NLS-1$
												+ ": retType = " + retType);  //$NON-NLS-1$
		}
		
		Optional<MemoryLayout> returnLayout = funcDesc.returnLayout();
		realReturnLayout = returnLayout.isPresent() ? returnLayout.get() : null; // set to null for void
		validateLayoutAgainstType(realReturnLayout, targetMethodType.returnType());

		Class<?>[] argTypes = targetMethodType.parameterArray();
		int argTypeCount = argTypes.length;
		argLayouts = funcDesc.argumentLayouts();
		int argLayoutCount = argLayouts.size();
		if (argTypeCount != argLayoutCount) {
			throw new IllegalArgumentException("The arity (" + argTypeCount //$NON-NLS-1$
												+ ") of the argument types is inconsistent with the arity ("  //$NON-NLS-1$
												+ argLayoutCount + ") of the argument layouts");  //$NON-NLS-1$
		}
		
		argLayoutArray = argLayouts.toArray(new MemoryLayout[argLayoutCount]);
		for (int argIndex = 0; argIndex < argLayoutCount; argIndex++) {
			if (!validateArgRetTypeClass(argTypes[argIndex])) {
				throw new IllegalArgumentException("The passed-in argument type at index " + argIndex + " is neither primitive nor MemoryAddress/MemoryAddress"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			validateLayoutAgainstType(argLayoutArray[argIndex], argTypes[argIndex]);
		}
	}
	
	/* Verify whether the specified type is primitive, MemoryAddress (for pointer) or MemorySegment (for struct) */
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
		&& (targetType != MemorySegment.class)
		) {
			return false;
		}
		return true;
	}
	
	/* Check the validity of the layout against the corresponding type */
	private static void validateLayoutAgainstType(MemoryLayout targetLayout, Class<?> targetType) {
		Class<?>  realType = unboxingPrimitiveClassType(targetType);
		boolean isPrimitiveLayout = false;
		
		if (targetLayout != null) {
			if (!targetLayout.hasSize()) {
				throw new IllegalArgumentException("The layout's size is expected: layout = " + targetLayout); //$NON-NLS-1$
			} else if (targetLayout.bitSize() <= 0) {
				throw new IllegalArgumentException("The layout's size must be greater than zero: layout = " + targetLayout); //$NON-NLS-1$
			}
		}
		
		/* The struct (specified by GroupLayout) for MemorySegment corresponds to GroupLayout in terms of layout */
		if (targetType == MemorySegment.class) {
			if (!GroupLayout.class.isInstance(targetLayout)) {
				throw new IllegalArgumentException("GroupLayout is expected: layout = " + targetLayout); //$NON-NLS-1$
			}
		/* Check the void layout (null for void) and the void type */
		} else if (((targetType == void.class) && (targetLayout != null))
		|| ((targetType != void.class) && (targetLayout == null))
		) {
			throw new IllegalArgumentException("Mismatch between the layout and the type: layout = "  //$NON-NLS-1$
												+ ((targetLayout == null) ? "VOID" : targetLayout) //$NON-NLS-1$
												+ ", type = " + targetType);  //$NON-NLS-1$
		/* Check the primitive type and MemoryAddress against the ValueLayout */
		} else if (targetType != void.class) {
			if (!ValueLayout.class.isInstance(targetLayout)) {
				throw new IllegalArgumentException("ValueLayout is expected: layout = " + targetLayout); //$NON-NLS-1$
			}
			/* Check the size and kind of the ValueLayout for the primitive types and MemoryAddress */
			validateValueLayoutSize(targetLayout, realType);
			validateValueLayoutKind(targetLayout, realType);
		}
	}
	
	/* Unbox the primitive class type to the primitive type if it occurs in the method types */
	private static Class<?> unboxingPrimitiveClassType(Class<?> targetType) {
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
	
	/* Check the size of the specified primitive layout to determine whether it matches the specified type */
	private static void validateValueLayoutSize(MemoryLayout TypeLayout, Class<?> targetType) {
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
												+ TypeLayout + ", type = " + targetType.getSimpleName());  //$NON-NLS-1$
		}
	}
	
	/* Check the kind (type) of the specified primitive layout to determine whether it matches the specified type */
	private static void validateValueLayoutKind(MemoryLayout targetLayout, Class<?> targetType) {
		boolean mismatchType = false;
		
		if(!targetLayout.toString().contains("[abi/kind=")) { //$NON-NLS-1$
			throw new IllegalArgumentException("The layout's ABI Class is undefined: layout = " + targetLayout); //$NON-NLS-1$
		}
		
		/* Extract the kind from the specified layout with the ATTR_NAME "abi/kind".
		 * e.g. b32[abi/kind=INT]
		 */
		TypeKind kind = (TypeKind)targetLayout.attribute(TypeKind.ATTR_NAME).orElse(null);
		switch (kind) {
		case CHAR:
			/* the CHAR layout (8bits) in Java only matches with byte in C */
			break;
		case SHORT:
			/* the SHORT layout (16bits) in Java only matches char and short in C */
			break;
		case INT:
			/* the INT layout (32bits) in Java only matches boolean and int in C */
			if ((targetType != boolean.class) && (targetType != int.class)) {
				mismatchType = true;
			}
			break;
		case LONG:
		case LONG_LONG:
			/* the LONG/LONG_LONG layout (64bits) in Java only matches long in C */
			if (targetType != long.class) {
				mismatchType = true;
			}
			break;
		case FLOAT:
			/* the FLOAT layout (32bits) in Java only matches float in C */
			if (targetType != float.class) {
				mismatchType = true;
			}
			break;
		case DOUBLE:
			/* the DOUBLE layout (64bits) in Java only matches double in C */
			if (targetType != double.class) {
				mismatchType = true;
			}
			break;
		case POINTER:
			/* the POINTER layout (64bits) in Java only matches MemoryAddress */
			if (targetType != MemoryAddress.class) {
				mismatchType = true;
			}
			break;
		default:
			mismatchType = true;
			break;
		}
		
		if (mismatchType) {
			throw new IllegalArgumentException("Mismatch between the layout and the type: layout = " + targetLayout //$NON-NLS-1$
												+ ", type = " + targetType);  //$NON-NLS-1$
		}
	}
}

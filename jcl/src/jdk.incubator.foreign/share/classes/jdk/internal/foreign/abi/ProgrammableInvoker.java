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
	private MemoryLayout[] argLayoutArray;
	private MemoryLayout realReturnLayout;
	private MethodHandle longObjToMemSegmtRetFilter;

	final static Lookup lookup = MethodHandles.lookup();

	/* The prep_cif and the corresponding argument layouts are cached & shared in multiple downcalls/threads */
	private static final HashMap<Integer, Long> cachedCifNativeThunkAddr = new HashMap<Integer, Long>();
	private static final HashMap<Integer, Long> cachedArgLayouts = new HashMap<Integer, Long>();

	/* Argument filters that convert the primitive types or MemoryAddress to long */
	private static MethodHandle booleanToLongArgFilter = null;
	private static MethodHandle charToLongArgFilter = null;
	private static MethodHandle byteToLongArgFilter = null;
	private static MethodHandle shortToLongArgFilter = null;
	private static MethodHandle intToLongArgFilter = null;
	private static MethodHandle floatToLongArgFilter = null;
	private static MethodHandle doubleToLongArgFilter = null;
	private static MethodHandle memAddrToLongArgFilter = null;
	private static MethodHandle memSegmtToLongArgFilter = null;

	/* Return value filters that convert the Long object to the primitive types or MemoryAddress */
	private static MethodHandle longObjToVoidRetFilter = null;
	private static MethodHandle longObjToBooleanRetFilter = null;
	private static MethodHandle longObjToCharRetFilter = null;
	private static MethodHandle longObjToByteRetFilter = null;
	private static MethodHandle longObjToShortRetFilter = null;
	private static MethodHandle longObjToIntRetFilter = null;
	private static MethodHandle longObjToLongRetFilter = null;
	private static MethodHandle longObjToFloatRetFilter = null;
	private static MethodHandle longObjToDoubleRetFilter = null;
	private static MethodHandle longObjToMemAddrRetFilter = null;

	private static synchronized native void resolveRequiredFields();
	private native void initCifNativeThunkData(String[] argLayouts, String retLayout, boolean newArgTypes);
	private native long invokeNative(long functionAddress, long calloutThunk, long[] argValues);

	private static final class PrivateClassLock {
		PrivateClassLock() {}
	}
	private static final Object privateClassLock = new PrivateClassLock();

	static {
		try {
			/* Set up the argument filters for the primitive types and MemoryAddress */
			booleanToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "booleanToLongArg", methodType(long.class, boolean.class)); //$NON-NLS-1$
			charToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "charToLongArg", methodType(long.class, char.class)); //$NON-NLS-1$
			byteToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "byteToLongArg", methodType(long.class, byte.class)); //$NON-NLS-1$
			shortToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "shortToLongArg", methodType(long.class, short.class)); //$NON-NLS-1$
			intToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "intToLongArg", methodType(long.class, int.class)); //$NON-NLS-1$
			floatToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "floatToLongArg", methodType(long.class, float.class)); //$NON-NLS-1$
			doubleToLongArgFilter = lookup.findStatic(Double.class, "doubleToLongBits", methodType(long.class, double.class)); //$NON-NLS-1$
			memAddrToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "memAddrToLongArg", methodType(long.class, MemoryAddress.class)); //$NON-NLS-1$
			memSegmtToLongArgFilter = lookup.findStatic(ProgrammableInvoker.class, "memSegmtToLongArg", methodType(long.class, MemorySegment.class)); //$NON-NLS-1$

			/* Set up the return value filters for the primitive types and MemoryAddress */
			longObjToVoidRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToVoidRet", methodType(void.class, Object.class)); //$NON-NLS-1$
			longObjToBooleanRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToBooleanRet", methodType(boolean.class, Object.class)); //$NON-NLS-1$
			longObjToCharRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToCharRet", methodType(char.class, Object.class)); //$NON-NLS-1$
			longObjToByteRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToByteRet", methodType(byte.class, Object.class)); //$NON-NLS-1$
			longObjToShortRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToShortRet", methodType(short.class, Object.class)); //$NON-NLS-1$
			longObjToIntRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToIntRet", methodType(int.class, Object.class)); //$NON-NLS-1$
			longObjToLongRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToLongRet", methodType(long.class, Object.class)); //$NON-NLS-1$
			longObjToFloatRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToFloatRet", methodType(float.class, Object.class)); //$NON-NLS-1$
			longObjToDoubleRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToDoubleRet", methodType(double.class, Object.class)); //$NON-NLS-1$
			longObjToMemAddrRetFilter = lookup.findStatic(ProgrammableInvoker.class, "longObjToMemAddrRet", methodType(MemoryAddress.class, Object.class)); //$NON-NLS-1$
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

	/* Intended for booleanToLongArgFilter that converts boolean to long */
	private static final long booleanToLongArg(boolean argValue) {
		return (argValue ? 1 : 0);
	}

	/* Intended for charToLongArgFilter that converts char to long */
	private static final long charToLongArg(char argValue) {
		return argValue;
	}

	/* Intended for byteToLongArgFilter that converts byte to long */
	private static final long byteToLongArg(byte argValue) {
		return argValue;
	}

	/* Intended for shortToLongArgFilter that converts short to long given
	 * short won't be casted to long automatically in filterArguments()
	 */
	private static final long shortToLongArg(short argValue) {
		return argValue;
	}

	/* Intended for intToLongArgFilter that converts int to long given
	 * int won't be casted to long automatically in filterArguments()
	 */
	private static final long intToLongArg(int argValue) {
		return argValue;
	}

	/* Intended for floatToLongArgFilter that converts the int value from Float.floatToIntBits()
	 * to long given int won't be casted to long automatically in filterArguments()
	 */
	private static final long floatToLongArg(float argValue) {
		return Float.floatToIntBits(argValue);
	}

	/* Intended for memAddrToLongArgFilter that converts the memory address to long */
	private static final long memAddrToLongArg(MemoryAddress argValue) {
		return argValue.toRawLongValue();
	}

	/* Intended for memSegmtToLongArgFilter that converts the memory segment to long */
	private static final long memSegmtToLongArg(MemorySegment argValue) {
		return argValue.address().toRawLongValue();
	}

	/* Intended for longObjToVoidRetFilter that converts the Long object to void */
	private static final void longObjToVoidRet(Object retValue) {
		return;
	}

	/* Intended for longObjToBooleanRetFilter that converts the Long object to boolean */
	private static final boolean longObjToBooleanRet(Object retValue) {
		boolean resultValue = (((Long)retValue).intValue() == 1) ? true : false;
		return resultValue;
	}

	/* Intended for longObjToCharRetFilter that converts the Long object to char */
	private static final char longObjToCharRet(Object retValue) {
		return (char)(((Long)retValue).shortValue());
	}

	/* Intended for longObjToByteRetFilter that converts the Long object to byte */
	private static final byte longObjToByteRet(Object retValue) {
		return ((Long)retValue).byteValue();
	}

	/* Intended for longObjToShortRetFilter that converts the Long object to short */
	private static final short longObjToShortRet(Object retValue) {
		return ((Long)retValue).shortValue();
	}

	/* Intended for longObjToIntRetFilter that converts the Long object to int */
	private static final int longObjToIntRet(Object retValue) {
		return ((Long)retValue).intValue();
	}

	/* Intended for longObjToLongRetFilter that converts the Long object to long */
	private static final long longObjToLongRet(Object retValue) {
		return ((Long)retValue).longValue();
	}

	/* Intended for longObjToFloatRetFilter that converts the Long object to float with Float.floatToIntBits() */
	private static final float longObjToFloatRet(Object retValue) {
		int tmpValue = ((Long)retValue).intValue();
		return Float.intBitsToFloat(tmpValue);
	}

	/* Intended for longObjToFloatRetFilter that converts the Long object to double with Double.longBitsToDouble() */
	private static final double longObjToDoubleRet(Object retValue) {
		long tmpValue = ((Long)retValue).longValue();
		return Double.longBitsToDouble(tmpValue);
	}

	/* Intended for longObjToMemAddrRetFilter that converts the Long object to the memory address */
	private static final MemoryAddress longObjToMemAddrRet(Object retValue) {
		long tmpValue = ((Long)retValue).longValue();
		return MemoryAddress.ofLong(tmpValue);
	}

	/* Intended for longObjToMemSegmtRetFilter that converts the Long object to the memory address */
	private final MemorySegment longObjToMemSegmtRet(Object retValue) {
		long tmpValue = ((Long)retValue).longValue();
		MemoryAddress memSegmtAddr = MemoryAddress.ofLong(tmpValue);
		return memSegmtAddr.asSegmentRestricted(realReturnLayout.byteSize());
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
		longObjToMemSegmtRetFilter = null;
		/* Create the filter for the returned memory segment as the size of the memory segment
		 * is only determined by the corresponding layout size in bytes at runtime.
		 */
		if (funcMethodType.returnType() == MemorySegment.class) {
			try {
				longObjToMemSegmtRetFilter = lookup.bind(this, "longObjToMemSegmtRet", methodType(MemorySegment.class, Object.class));
			} catch (ReflectiveOperationException e) {
				throw new InternalError(e);
			}
		}
		generateAdapter();
	}

	/* Map the layouts of return type & argument types to the underlying prep_cif */
	private void generateAdapter() {
		int argLayoutCount = argLayoutArray.length;
		String[] argLayoutStrs = new String[argLayoutCount];
		String argLayoutStrsLine = "|"; //$NON-NLS-1$
		for (int argIndex = 0; argIndex < argLayoutCount; argIndex++) {
			MemoryLayout argLayout = argLayoutArray[argIndex];
			/* Prefix the size of layout to the layout string to be parsed in native */
			argLayoutStrs[argIndex] = argLayout.byteSize() + preprocessLayoutString(argLayout, true);
			argLayoutStrsLine += argLayoutStrs[argIndex] + "|"; //$NON-NLS-1$
		}
		argLayoutStrsLine = "(" + argLayoutStrsLine + ")"; //$NON-NLS-1$ //$NON-NLS-2$

		/* Set the void layout string intended for the underlying native code
		 * as the corresponding layout doesn't exist in the Spec.
		 * Note: 'V' stands for the void type and 0 means zero byte.
		 */
		String retLayoutStr = "0V"; //$NON-NLS-1$
		if (realReturnLayout != null) {
			retLayoutStr = realReturnLayout.byteSize() + preprocessLayoutString(realReturnLayout, true);
		}

		synchronized(privateClassLock) {
			/* If a prep_cif for a given function descriptor exists, then the corresponding return & argument layouts
			 * were already set up for this prep_cif, in which case there is no need to check the layouts.
			 * If not the case, check at first whether the same return & argument layouts exist in the cache
			 * in case of duplicate memory allocation for the same layouts.
			 *
			 * Note:
			 * 1) C_LONG and C_LONG_LONG should be treated as the same layout in the cache.
			 * 2) the same layout kind with or without the layout name should be treated as the same layout.
			 * e.g.  C_INT without the layout name = b32[abi/kind=INT]
			 *  and  C_INT with the layout name = b32(int)[abi/kind=INT,layout/name=int]
			 */
			String argRetLayoutStrsLine = argLayoutStrsLine + retLayoutStr;
			Integer argRetLayoutStrLineHash = Integer.valueOf(argRetLayoutStrsLine.hashCode());
			Integer argLayoutStrsLineHash = Integer.valueOf(argLayoutStrsLine.hashCode());
			if (cachedCifNativeThunkAddr.containsKey(argRetLayoutStrLineHash)) {
				cifNativeThunkAddr = cachedCifNativeThunkAddr.get(argRetLayoutStrLineHash).longValue();
				argTypesAddr = cachedArgLayouts.get(argLayoutStrsLineHash).longValue();
			} else {
				boolean newArgTypes = cachedArgLayouts.containsKey(argLayoutStrsLineHash) ? false : true;
				if (!newArgTypes) {
					argTypesAddr = cachedArgLayouts.get(argLayoutStrsLineHash).longValue();
				}

				/* Prepare the prep_cif for the native function specified by the arguments/return layouts */
				initCifNativeThunkData(argLayoutStrs, retLayoutStr, newArgTypes);

				/* Cache the address of prep_cif and argTypes after setting up via the out-of-line native code */
				if (newArgTypes) {
					cachedArgLayouts.put(argLayoutStrsLineHash, Long.valueOf(argTypesAddr));
				}
				cachedCifNativeThunkAddr.put(argRetLayoutStrLineHash, Long.valueOf(cifNativeThunkAddr));
			}
		}
	}

	/* Preprocess the layout to generate a concise layout string with all kind symbols
	 * extracted from the layout to simplify parsing the layout string in native.
	 * e.g. a struct layout string with nested struct is as follows:
	 * [
	 *   [
	 *    b32(elem1)[abi/kind=INT,layout/name=elem1]
	 *    b32(elem2)[abi/kind=INT,layout/name=elem2]
	 *   ](Struct1_II)[layout/name=Struct1_II]
	 *   [
	 *    b32(elem1)[abi/kind=INT,layout/name=elem1]
	 *    b32(elem2)[abi/kind=INT,layout/name=elem2]
	 *   ](Struct2_II)[layout/name=Struct2_II]
	 * ](nested_struct)[layout/name=nested_struct]
	 *
	 * ends up with "16#2[#2[II]#2[II]]" as follows:
	 *
	 *   16#2[  (16 is the byte size of the layout and 2 is the count of the struct elements
	 *        #2[ 2 is the count of the int elements
	 *           I  (INT)
	 *           I  (INT)
	 *          ]
	 *        #2[ 2 is the count of the int elements
	 *           I  (INT)
	 *           I  (INT)
	 *          ]
	 *        ]
	 *  where "#" denotes the start of struct.
	 */
	private static String preprocessLayoutString(MemoryLayout targetLayout, boolean enablePadding) {
		String targetLayoutString = "";
		
		/* Directly obtain the kind symbol of the primitive layout */
		if (ValueLayout.class.isInstance(targetLayout)) {
			targetLayoutString = getPrimitiveKindSymbol((ValueLayout)targetLayout);
		} else if (SequenceLayout.class.isInstance(targetLayout)) { // Intended for nested arrays
			SequenceLayout arrayLayout = (SequenceLayout)targetLayout;
			MemoryLayout elementLayout = arrayLayout.elementLayout();
			long elementCount = arrayLayout.elementCount().getAsLong();
			/* Ignore any padding in the nested array given only a padding in
			 * the outermost struct is required by ffi_call in native.
			 */
			targetLayoutString = elementCount + ":" + preprocessLayoutString(elementLayout, false);
		} else if (GroupLayout.class.isInstance(targetLayout)) { // Intended for the nested structs
			GroupLayout structLayout = (GroupLayout)targetLayout;
			List<MemoryLayout> elementLayoutList = structLayout.memberLayouts();
			int structElementCount = elementLayoutList.size();
			String elementLayoutStrs = ""; //$NON-NLS-1$
			
			int nestedStructCount = 0;
			for (int elemIndex = 0; elemIndex < structElementCount; elemIndex++) {
				MemoryLayout structElement = elementLayoutList.get(elemIndex);
				if (structElement.isPadding()) {
					nestedStructCount += 1;
				}
				elementLayoutStrs += preprocessLayoutString(structElement, false);
			}
			
			/* Prefix "#" to denote the start of this layout string */
			targetLayoutString = "#";
			
			/* Only count in the tailing padding element in bytes (which will be allocated for ffi_type in native)
			 * and exclude any padding element in the middle of struct if exits.
			 */
			int paddingBits = getPaddingBitsOfStruct(targetLayout);
			structElementCount -= nestedStructCount; // exclude all paddings before counting in the required padding
			if (enablePadding && (paddingBits > 0)) {
				if (24 == paddingBits) { // 3 bytes (C_CHAR)
					structElementCount += 3;
				} else { // 1 byte (C_CHAR), 2 bytes (C_SHORT) or 4 bytes (C_INT)
					structElementCount += 1;
				}
			}
			targetLayoutString += structElementCount + "[" + elementLayoutStrs; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			/* Ignore any padding in the nested struct given only the padding in
			 * the outermost struct is required by ffi_call in native.
			 */
			if (enablePadding && (paddingBits > 0)) {
				targetLayoutString += "x" + paddingBits;
			}
			targetLayoutString += "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return targetLayoutString;
	}
	
	/* Get the padding bits for a struct with nested structs/arrays by traversing its elements to see
	 * whether a nested struct/array exists in the middle of the struct.
	 */
	private static int getPaddingBitsOfStruct(MemoryLayout targetLayout) {
		int paddingBits = 0;
		
		if (GroupLayout.class.isInstance(targetLayout)) {
			GroupLayout structLayout = (GroupLayout)targetLayout;
			List<MemoryLayout> elementLayoutList = structLayout.memberLayouts();
			int structElementCount = elementLayoutList.size();
			
			/* Check whether a nested struct/array occurs in the middle of struct
			 * so as to determine whether a padding is required for ffi_call.
			 */
			int nestedStructIndex = -1;
			for (int elemIndex = structElementCount - 1; elemIndex >= 0; elemIndex--) {
				MemoryLayout structElement = elementLayoutList.get(elemIndex);
				if (GroupLayout.class.isInstance(structElement)
				|| SequenceLayout.class.isInstance(structElement)
				) {
					nestedStructIndex = elemIndex;
					break;
				}
			}
			
			if (nestedStructIndex >= 0) {
				MemoryLayout lastElement = elementLayoutList.get(structElementCount - 1);
				if (lastElement.isPadding()) {
					long tempPaddingBits = lastElement.bitSize();
					/* The padding bits must be 8 bits (1 byte), 16 bits (2 bytes), 24 bits (3 bytes)
					 * or 32 bits (4 bytes) as requested by ffi_call.
					 */
					if ((tempPaddingBits != 8) && (tempPaddingBits != 16) && (tempPaddingBits != 24) && (tempPaddingBits != 32)) {
						throw new IllegalArgumentException("The padding bits is invalid: " + paddingBits);  //$NON-NLS-1$
					}
					paddingBits = (int)tempPaddingBits;
				} else if (nestedStructIndex > 0) {
					/* The padding is mandatory in the struct layout if a nested struct/arrary occurs in the middle
					 * of the struct, which includes at least the following cases. e.g.
					 * 1) char, [char, char] which are 3 bytes
					 * 2) short, [short, short] which are 6 bytes
					 * 3) int, [int, int] which are 12 bytes
					 */
					if ((targetLayout.byteSize() % 4 != 0)
					|| (((targetLayout.byteSize() / 4) % 2) != 0)
					) {
						throw new IllegalArgumentException("A tailing padding of the struct layout is mandatory");  //$NON-NLS-1$
					}
				}
			}
		}
		
		return paddingBits;
	}
	
	/* Map the specified primitive layout's kind to the symbol for primitive type in VM Spec */
	private static String getPrimitiveKindSymbol(ValueLayout targetLayout) {
		/* Extract the kind from the specified layout with the ATTR_NAME "abi/kind".
		 * e.g. b32[abi/kind=INT]
		 */
		TypeKind kind = (TypeKind)targetLayout.attribute(TypeKind.ATTR_NAME)
				.orElseThrow(() -> new IllegalArgumentException("The layout's ABI class is empty")); //$NON-NLS-1$
		String kindSymbol = "";

		switch (kind) {
		case CHAR:
			kindSymbol = "C";
			break;
		case SHORT:
			kindSymbol = "S";
			break;
		case INT:
			kindSymbol = "I";
			break;
		case LONG:
		case LONG_LONG: // A 8-byte long type on 64bit Windows as specified in the Spec.
			/* Map the long layout to 'J' so as to keep consistent with the existing VM Spec */
			kindSymbol = "J";
			break;
		case FLOAT:
			kindSymbol = "F";
			break;
		case DOUBLE:
			kindSymbol = "D";
			break;
		case POINTER:
			kindSymbol = "P";
			break;
		default:
			throw new IllegalArgumentException("The layout's ABI Class is undefined: layout = " + targetLayout); //$NON-NLS-1$
		}
		
		return kindSymbol;
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
			boundHandle = nativeInvoker.permuteMH(boundHandle, functionMethodType);
			return boundHandle;
		} catch (ReflectiveOperationException e) {
			throw new InternalError(e);
		}
	}

	/* Collect and convert the passed-in arguments to an Object array for the underlying native call */
	private MethodHandle permuteMH(MethodHandle targetHandle, MethodType nativeMethodType) throws NullPointerException, WrongMethodTypeException {
		Class<?>[] argTypeClasses = nativeMethodType.parameterArray();
		int nativeArgCount = argTypeClasses.length;
		MethodHandle resultHandle = targetHandle.asCollector(long[].class, nativeArgCount);

		/* Convert the argument values to long via filterArguments() prior to the native call */
		MethodHandle[] argFilters = new MethodHandle[nativeArgCount];
		for (int argIndex = 0; argIndex < nativeArgCount; argIndex++) {
			argFilters[argIndex] = getArgumentFilter(argTypeClasses[argIndex]);
		}
		resultHandle = filterArguments(resultHandle, 0, argFilters);

		/* Convert the return value to the specified type via filterReturnValue() after the native call */
		MethodHandle retFilter = getReturnValFilter(nativeMethodType.returnType());
		resultHandle = filterReturnValue(resultHandle, retFilter);
		return resultHandle;
	}

	/* Obtain the filter that converts the passed-in argument to long against its type */
	private static MethodHandle getArgumentFilter(Class<?> argTypeClass) {
		/* Set the filter to null in the case of long by default as there is no conversion for long */
		MethodHandle filterMH = null;

		if (argTypeClass == boolean.class) {
			filterMH = booleanToLongArgFilter;
		} else if (argTypeClass == char.class) {
			filterMH = charToLongArgFilter;
		} else if (argTypeClass == byte.class) {
			filterMH = byteToLongArgFilter;
		} else if (argTypeClass == short.class) {
			filterMH = shortToLongArgFilter;
		} else if (argTypeClass == int.class) {
			filterMH = intToLongArgFilter;
		} else if (argTypeClass == float.class) {
			filterMH = floatToLongArgFilter;
		} else if (argTypeClass == double.class) {
			filterMH = doubleToLongArgFilter;
		} else if (argTypeClass == MemoryAddress.class) {
			filterMH = memAddrToLongArgFilter;
		} else if (argTypeClass == MemorySegment.class) {
			filterMH = memSegmtToLongArgFilter;
		}

		return filterMH;
	}

	/* The return value filter that converts the returned long value from the C function to the specified return type at Java level */
	private MethodHandle getReturnValFilter(Class<?> returnType) {
		MethodHandle filterMH = longObjToLongRetFilter;

		if (returnType == void.class) {
			filterMH = longObjToVoidRetFilter;
		} else if (returnType == boolean.class) {
			filterMH = longObjToBooleanRetFilter;
		} else if (returnType == char.class) {
			filterMH = longObjToCharRetFilter;
		} else if (returnType == byte.class) {
			filterMH = longObjToByteRetFilter;
		} else if (returnType == short.class) {
			filterMH = longObjToShortRetFilter;
		} else if (returnType == int.class) {
			filterMH = longObjToIntRetFilter;
		} else if (returnType == float.class) {
			filterMH = longObjToFloatRetFilter;
		} else if (returnType == double.class) {
			filterMH = longObjToDoubleRetFilter;
		} else if (returnType == MemoryAddress.class) {
			filterMH = longObjToMemAddrRetFilter;
		} else if (returnType == MemorySegment.class) {
			filterMH = longObjToMemSegmtRetFilter;
		}

		return filterMH;
	}

	/* The method (bound by the method handle to the native code) intends to invoke the C function via the inlined code */
	Object runNativeMethod(long[] args) {
		long returnVal = invokeNative(functionAddr.address().toRawLongValue(), cifNativeThunkAddr, args);
		return Long.valueOf(returnVal);
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
		List<MemoryLayout> argLayouts = funcDesc.argumentLayouts();
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
		&& (targetType != MemoryAddress.class)
		&& (targetType != MemorySegment.class)
		) {
			return false;
		}
		return true;
	}

	/* Check the validity of the layout against the corresponding type */
	private static void validateLayoutAgainstType(MemoryLayout targetLayout, Class<?> targetType) {
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
			validateValueLayoutSize(targetLayout, targetType);
			validateValueLayoutKind(targetLayout, targetType);
		}
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
		boolean kindAttrFound = false;
		List<String> layoutAttrList = targetLayout.attributes().toList();
		for (String attrStr : layoutAttrList) {
			if (attrStr.equalsIgnoreCase("abi/kind")) { //$NON-NLS-1$
				kindAttrFound = true;
				break;
			}
		}
		if (!kindAttrFound) {
			throw new IllegalArgumentException("The layout's ABI Class is undefined: layout = " + targetLayout); //$NON-NLS-1$
		}

		/* Extract the kind from the specified layout with the ATTR_NAME "abi/kind".
		 * e.g. b32[abi/kind=INT]
		 */
		TypeKind kind = (TypeKind)targetLayout.attribute(TypeKind.ATTR_NAME)
				.orElseThrow(() -> new IllegalArgumentException("The layout's ABI class is empty")); //$NON-NLS-1$
		boolean mismatchType = false;

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

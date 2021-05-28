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

#if !defined(LAYOUTFFITYPEHELPERS_HPP_)
#define LAYOUTFFITYPEHELPERS_HPP_

#include "ut_j9vm.h"
#include "vm_internal.h"
#include "ffi.h"

#define J9VM_LAYOUT_STRING_ON_STACK_LIMIT 128

class LayoutFFITypeHelpers
{
#if JAVA_SPEC_VERSION >= 16
	/* Data members */
private:
	J9VMThread *const _currentThread;
	J9JavaVM *const _vm;

	/* Function members */
public:
	LayoutFFITypeHelpers(J9VMThread *currentThread)
			: _currentThread(currentThread)
			, _vm(_currentThread->javaVM)
	{ };

	/**
	 * @brief Convert the preceding integer of a layout string from string to UDATA,
	 * and put layout at the position after the integer
	 *
	 * @param layout[in] A pointer to a c string describing the types of the struct elements. For example:
	 * 		If layout is "16#2[#2[II]#2[II]]", it returns 16 in byte
	 *
	 * Note: it is also used to obtain the array count e.g. 5:I for an int array with 5 elements.
	 * @return The preceding integer converted from string to UDATA
	 */
	static VMINLINE UDATA
	getIntFromLayout(char **layout)
	{
		char *currentLayout = *layout;
		UDATA sumBytes = 0;
		while ('\0' != *currentLayout) {
			char symb = *currentLayout;
			if ((symb >= '0') && (symb <= '9')) {
				sumBytes = (sumBytes * 10) + (symb - '0');
			} else {
				break;
			}
			currentLayout += 1;
		}
		*layout = currentLayout;
		return sumBytes;
	}

	/**
	 * @brief obtain the ffi_type from the layout symbol (the preceding letter of the layout type. e.g. I for INT)
	 *
	 * @param layoutSymb[in] The layout symbol describing the type of the layout
	 * @return The pointer to the ffi_type corresponding to the layout symbol
	 */
	static VMINLINE ffi_type*
	getPrimitiveFFIType(char layoutSymb)
	{
		ffi_type* typeFFI = NULL;

		switch (layoutSymb) {
		case 'V': /* VOID */
			typeFFI = &ffi_type_void;
			break;
		case 'C': /* C_CHAR */
			typeFFI = &ffi_type_sint8;
			break;
		case 'S': /* C_SHORT */
			typeFFI = &ffi_type_sint16;
			break;
		case 'I': /* C_INT */
			typeFFI = &ffi_type_sint32;
			break;
		case 'J': /* 8 bytes for either C_LONG or C_LONG_LONG(maps to long long specific to Windows/AIX 64-bit) */
			typeFFI = &ffi_type_sint64;
			break;
		case 'F': /* C_FLOAT */
			typeFFI = &ffi_type_float;
			break;
		case 'D': /* C_DOUBLE */
			typeFFI = &ffi_type_double;
			break;
		case 'P': /* C_POINTER */
			typeFFI = &ffi_type_pointer;
			break;
		default:
			Assert_VM_unreachable();
		}

		return typeFFI;
	}

	/**
	 * @brief Create a FFI type from a layout string object for both primitive and struct
	 *
	 * @param typeFFI[in] The custom FFI type to be created
	 * @param layoutStringObject[in] An object containing the layout string describing the field types.
	 * @return The size of the layout, extracted from the layout string, UDATA_MAX if a failure occurs
	 */
	VMINLINE UDATA
	getLayoutFFIType(ffi_type** typeFFI, j9object_t layoutStringObject)
	{
		PORT_ACCESS_FROM_JAVAVM(_vm);
		*typeFFI = NULL;
		UDATA layoutSize = 0;
		char layoutBuffer[J9VM_LAYOUT_STRING_ON_STACK_LIMIT];
		char *layout = copyStringToUTF8WithMemAlloc(_currentThread, layoutStringObject,
				J9_STR_NULL_TERMINATE_RESULT, "", 0, layoutBuffer, sizeof(layoutBuffer), NULL);
		/* Preserve the original pointer for j9mem_free_memory() as
		 * subsequent calls will modify the contents of this pointer.
		 */
		char *layoutTemp = layout;
		if (NULL == layout) {
			goto done;
		}

		/* Check the byte size of the layout's size which is prefixed to the layout string */
		layoutSize = getIntFromLayout(&layoutTemp);
		if (layoutSize >= UDATA_MAX) {
			return layoutSize;
		}
		printf("\ngetLayoutFFIType: layout = %s", layout);
		//printf("\ngetLayoutFFIType: layoutSize = %d, layoutTemp = %s", (int)layoutSize, layoutTemp);

		/* There are 2 kinds of layout: ValueLayout for primitive and GroupLayout for struct.
		 * e.g.
		 * 1) int: 4I  (4 represents the int layout's size in bytes)
		 * 2) struct:
		 *    struct struct_III{
		 *                       int elem1;
		 *                       int elem2;
		 *                       int elem3;
		 *                     };
		 *    with the layout (12#3[III]) as follows:
		 *    12#3[   (12 represents the struct layout's size in bytes while 3 represents the counts of elements)
		 *         I  (C_INT)
		 *         I  (C_INT)
		 *         I  (C_INT)
		 *        ]
		 *    where the header of the struct layout is "12#3".
		 * Thus, a struct layout string starts with the layout's size, a '#' as separator
		 * plus the count of struct elements as the header.
		 *
		 * Note: the layout string is preprocessed and prefixed with the header in
		 * ProgrammableInvoker.preprocessLayoutString() for easier handling in native.
		 */
		if ('#' == *layoutTemp) {
			*typeFFI = getStructFFIType(&layoutTemp);
		} else {
			*typeFFI = getPrimitiveFFIType(*layoutTemp);
		}

		if (layout != layoutBuffer) {
			j9mem_free_memory(layout);
		}

done:
		return layoutSize;
	}

	/**
	 * @brief Create an array of elements for a construct FFI type
	 *
	 * @param layout[in] A pointer to a c string describing the types of the struct elements
	 * @return An array of ffi_type* which are the elements of the struct
	 */
	ffi_type**
	getStructFFITypeElements(char **layout);

	/**
	 * @brief Create a struct FFI type
	 * @param layout[in] A pointer to a c string describing the types of the struct elements.
	 * For example:
	 * 1) "35#7[ (the padding bits (omitted here) are specified initially for each short-sized primitives to align with the longest one)
	 *         C  (C_CHAR)
	 *         S  (C_SHORT)
	 *         I  (C_INT)
	 *         L  (C_LONG)
	 *         F  (C_FLOAT)
	 *         D  (C_DOUBLE)
	 *         P  (C_POINTER)
	 *     ]" is a struct with primitives & pointer (byte, short, int, float, long, double, pointer)
	 *
	 * 2) "16#2[
	 *        #2[
	 *           I  (C_INT)
	 *           I  (C_INT)
	 *          ]
	 *        #2[
	 *           I  (C_INT)
	 *           I  (C_INT)
	 *          ]
	 *        ]" is a struct with two nested structs
	 *
	 * 3) "12#2[
	 *        [4:   (an int array where ":" is the sign for array)
	 *           I  (C_INT)
	 *        ]
	 *        [2:  (a strcut arrays where ":" is the sign for array)
	 *          #2[
	 *             I  (C_INT)
	 *             I  (C_INT)
	 *            ] (Point)
	 *        ]
	 *       ]" is a struct with 2 arrays: int[4] and Point[2], where Point is a struct with 2 ints
	 *
	 * Note:
	 * 1) All the descriptions in the layout string are removed with preprocessLayoutString()
	 *    in ProgrammableInvoker in advance for easier parsing the layout string.
	 * 2) A struct pointer is treated as a generic pointer (C_POINTER in C corresponds to MemoryAddress in Java)
	 *    which is the same as a primitive pointer given there is no difference in terms of the layout string.
	 *
	 * @return The ffi_type of type FFI_TYPE_STRUCT
	 */
	ffi_type*
	getStructFFIType(char **layout);

	/**
	 * @brief Create an array FFI type describing an array as a field in a struct
	 *
	 * @param layout[in] A c string describing the type of the array elements
	 * @param nElements[in] The number of elements in the array
	 * @return A ffi_type* describing the array
	 */
	ffi_type*
	getArrayFFIType(char **layout, UDATA nElements);

	/**
	 * @brief Free a struct FFI type
	 *
	 * @param ffi[in] A pointer to a ffi type
	 */
	void
	freeStructFFIType(ffi_type *ffi);

	/**
	 * @brief Free the elements of a struct FFI type
	 *
	 * @param elements[in] The elements of a struct FFI type
	 */
	void
	freeStructFFITypeElements(ffi_type **elements);

	/* Encode a signature string to a struct representing the signature to be used
	 * to handle the argument or return value in the upcall.
	 */
	static void
	encodeUpcallSignature(char *cSignature, J9UpcallSigType *sigType)
	{
		if ((*cSignature >= '0') && (*cSignature <= '9')) {
			sigType->sizeInByte = getIntFromLayout(&cSignature);
			cSignature += 1; // Skip over '#' to the signature

			if ('[' == *cSignature) { // The start of a struct signature string
				sigType->type = encodeOuterStruct(cSignature, sigType->sizeInByte);
			} else {
				sigType->type = encodeUpcallPrimitive(cSignature);
			}
		} else {
			Assert_VM_unreachable();
		}
	}

	/* Encode a primitive signature string to a predefined type which
	 * is set in a struct representing the signature in the upcall.
	 */
	static U_8
	encodeUpcallPrimitive(char *cSignature)
	{
		U_8 primSigType = 0;

		switch (*cSignature) {
		case 'V': // The void type on return
			primSigType = J9_FFI_UPCALL_SIG_TYPE_VOID;
			break;
		case 'C': // C_CHAR in 1 byte
			primSigType = J9_FFI_UPCALL_SIG_TYPE_CHAR;
			break;
		case 'S': // C_SHORT in 2 bytes
			primSigType = J9_FFI_UPCALL_SIG_TYPE_SHORT;
			break;
		case 'I': // C_INT in 4 bytes
			primSigType = J9_FFI_UPCALL_SIG_TYPE_INT32;
			break;
		case 'J': // C_LONG or C_LONG_LONG(Windows 64bit) in 8 bytes
			primSigType = J9_FFI_UPCALL_SIG_TYPE_INT64;
			break;
		case 'F': // C_FLOAT in 4 bytes
			primSigType = J9_FFI_UPCALL_SIG_TYPE_FLOAT;
			break;
		case 'D': // C_DOUBLE in 8 bytes
			primSigType = J9_FFI_UPCALL_SIG_TYPE_DOUBLE;
			break;
		case 'P': // C_POINTER in 8 bytes
			primSigType = J9_FFI_UPCALL_SIG_TYPE_POINTER;
			break;
		default:
			Assert_VM_unreachable();
			break;
		}

		return primSigType;
	}

	/* This wrapper function invokes parseStruct() to determine
	 * the AGGREGATE subtype of the specified struct.
	 */
	static U_8
	encodeOuterStruct(char *structSig, UDATA sizeInByte)
	{
		bool isAllSP = true;
		bool isAllDP = true;
		U_8 structSigType = 0;
		U_8 first16ByteComposTypes[J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH] = {0};
		UDATA curIndex = 0;

		/* Analyze the specified native signature to fill up a 16-byte composition type
		 * array so as to determine the aggregate subtype.
		 */
		parseStruct(structSig, &isAllSP, &isAllDP, first16ByteComposTypes, &curIndex);

		if (isAllSP) {
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_ALL_SP;
		} else if (isAllDP) {
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_ALL_DP;
		} else if (sizeInByte > J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH) {
			/* AGGREGATE_OTHER (mix of different types without pure float/double) is
			 * intended for the native signature greater than 16 bytes in size
			 */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_OTHER;
		} else {
			/* Analyze the 16-byte composition type array to determine the aggregate subtype
			 * of the native signature which is equal to or less than 16 bytes in size).
			 */
			structSigType = getStructSigTypeFrom16ByteComposTypes(first16ByteComposTypes);
		}

		return structSigType;
	}

	/* This function is invoked recursively to parse each element in a struct signature
	 * which is used to determine the AGGREGATE subtype of the struct.
	 *
	 * To help analyze the aggregate subtype, we use a 16-byte composition type array
	 * in which each element represents a 1-byte of composition types:
	 *  0 ---- undefined
	 * 'E' --- this 1-byte cell is filled with padding
	 * 'M' --- this 1-byte cell is part of any integer byte without pure float/double
	 * 'F' --- this 1-byte cell is part of a single-precision floating point
	 * 'D' --- this 1-byte cell is part of a double-precision floating point
	 *
	 * Note:
	 * This array is unused if the native signature is bigger than 16 bytes in size.
	 */
	static void
	parseStruct(char *currentStructSig, bool *isAllSP, bool *isAllDP, U_8 *first16ByteComposTypes, UDATA *currentIndex)
	{
		U_8 structSigType = 0;
		UDATA arrayLength = 0;
		UDATA paddingBytes = 0;

		while ('\0' != *currentStructSig) {
			switch (*currentStructSig) {
			case 'C': // C_CHAR in 1 byte
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_M, 1, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case 'S': // C_SHORT in 2 bytes
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_M, 2, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case 'I': // C_INT in 4 bytes
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_M, 4, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case 'J': // C_LONG or C_LONG_LONG(Windows 64bit) in 8 bytes
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_M, 8, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case 'P': // C_POINTER in 8 bytes
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_M, 8, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case 'F': // C_FLOAT in 4 bytes
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_F, 4, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case 'D': // C_DOUBLE in 8 bytes
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_D, 8, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			case '(': // The start of the padding bytes explicitly specified by users
				currentStructSig += 1; // Skip over '(' to the padding bytes explicitly specified in java
				paddingBytes = getIntFromLayout(&currentStructSig);
				setByteCellforPrimitive(isAllSP, isAllDP, first16ByteComposTypes, currentIndex, J9_FFI_UPCALL_COMPOSITION_TYPE_E, paddingBytes, 0);
				currentStructSig += 1; // Skip over ')' to the next element of struct
				break;
			case '[': // The start of a struct signature
			{
				setByteCellforStruct(currentStructSig, isAllSP, isAllDP, first16ByteComposTypes, currentIndex, arrayLength);
				arrayLength = 0; // Reset for the next array if exists
				break;
			}
			case ']': // The end of a struct signature
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			{
				/* Get the array count followed by the array type */
				arrayLength = getIntFromLayout(&currentStructSig);
				currentStructSig += 1; // Skip over ':' to the array type
				break;
			}
			default:
				Assert_VM_unreachable();
				break;
			}
			currentStructSig += 1;
		}
	}

	/* Fill in a U_8[16] array with the specified composition type for primitives and set the sign
	 * for the float/double type which helps determine the AGGREGATE subtype of struct.
	 */
	static void
	setByteCellforPrimitive(bool *isAllSP, bool *isAllDP, U_8 *first16ByteComposTypes, UDATA *currentIndex, U_8 composType, UDATA composTypeSize, UDATA arrayLength)
	{
		UDATA arrLen = (arrayLength > 0) ? arrayLength : 1; // Set 1 for non-array by default

		switch (composType) {
		case J9_FFI_UPCALL_COMPOSITION_TYPE_E: // Part of padding bytes
		case J9_FFI_UPCALL_COMPOSITION_TYPE_M: // Part of any integer byte
			/* It is neither  ALL_SP nor ALL_DP in the case of padding or any integer type */
			*isAllSP = false;
			*isAllDP = false;
			break;
		case J9_FFI_UPCALL_COMPOSITION_TYPE_F: // Part of a single-precision floating point
			*isAllDP = false;
			break;
		case J9_FFI_UPCALL_COMPOSITION_TYPE_D: // Part of a double-precision floating point
			*isAllSP = false;
			break;
		default:
			Assert_VM_unreachable();
			break;
		}

		/* Only set the 16-byte composition type array with the first 16 bytes of the native signature */
		while ((currentIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH) && (arrLen > 0)) {
			for (UDATA typeSize = composTypeSize; typeSize > 0; typeSize--) {
				if (currentIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH) {
					first16ByteComposTypes[currentIndex] = composType;
					currentIndex += 1;
				}
			}
			arrLen -= 1;
		}
	}

	/* Fill in the 16-byte composition type array with the specified composition type for struct and copy
	 * the filled bytes of struct to the rest of the array in the case of a nested struct array.
	 */
	static void
	setByteCellforStruct(char *currentStructSig, bool *isAllSP, bool *isAllDP, U_8 *first16ByteComposTypes, UDATA *currentIndex, UDATA arrayLength)
	{
		UDATA arrLen = (arrayLength > 0) ? arrayLength : 1; // Set 1 for non-array by default
		UDATA *startIndex = currentIndex; // The start of the struct bytes to be filled in the composition type array
		UDATA composTypesSize = 0;
		currentStructSig += 1; // Skip over '[' to the 1st element type of struct
		parseStruct(currentStructSig, isAllSP, isAllDP, first16ByteComposTypes, currentIndex);
		composTypesSize = currentIndex - startIndex; // The length of the filled bytes of struct in the composition type array
		arrLen -= 1;

		/* Copy the filled bytes of struct to the rest of the array based on the length
		 * of the struct array till it reaches the end of the composition type array.
		 */
		while ((currentIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH) && (arrLen > 0)) {
			for (UDATA offset = 0; offset < composTypesSize; offset++) {
				if (currentIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH) {
					first16ByteComposTypes[currentIndex] = first16ByteComposTypes[startIndex + offset];
					currentIndex += 1;
				}
			}
			arrLen -= 1;
		}
	}

	/* Check the merged composition types of both the first 8 bytes and the next 8 bytes
	 * of the 16-byte composition type array so as to determine the aggregate subtype of
	 * a struct equal to or less than 16 bytes in size).
	 */
	static U_8
	getStructSigTypeFrom16ByteComposTypes(U_8 *first16ByteComposTypes)
	{
		U_8 structSigType = 0;
		U_8 first8ByteComposType = getComposTypeFrom8Bytes(first16ByteComposTypes, 0);
		U_8 second8ByteComposType = getComposTypeFrom8Bytes(first16ByteComposTypes, 8);
		U_8 composType = first8ByteComposType | second8ByteComposType;

		if ((J9_FFI_UPCALL_COMPOSITION_TYPE_F_E == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_D == second8ByteComposType)
		) {
			/* The aggregate subtype is set for the struct {float, padding, double} */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_SP_DP;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_F == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_D == second8ByteComposType)
		) {
			/* The aggregate subtype is set for the struct {float, float, double} */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_SP_SP_DP;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_D == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_F_E == second8ByteComposType)
		) {
			/* The aggregate subtype is set for the struct {double, float, padding} */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_DP_SP;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_D == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_F == second8ByteComposType)
		) {
			/* The aggregate subtype is set for the struct {double, float, float} */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_DP_SP_SP;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_M == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_F == second8ByteComposType)
		) {
			/* The aggregate subtype is set for structs starting with the mix of any integer type/float(the first 8 bytes)
			 * followed by two floats(the next 8 bytes).
			 * e.g struct {int, float, float} or stuct {float, int, float}.
			 */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MISC_SP;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_M == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_D == second8ByteComposType)
		) {
			/* The aggregate subtype is set for a struct starting with the mix of any integer type/float(the first 8 bytes)
			 * followed by a double(the next 8 bytes).
			 * e.g struct {int, float, double}, struct {float, int, double}, or struct{long, double}
			 */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MISC_DP;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_F == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_M == second8ByteComposType)
		) {
			/* The aggregate subtype is set for a struct starting with two floats(the first 8 bytes)
			 * followed by the mix of any integer type/float(the next 8 bytes).
			 * e.g struct {float, float, float, int}, or struct {float, float, long}
			 */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_SP_MISC;
		} else if (((J9_FFI_UPCALL_COMPOSITION_TYPE_D == first8ByteComposType)
		&& (J9_FFI_UPCALL_COMPOSITION_TYPE_M == second8ByteComposType)
		) {
			/* The aggregate subtype is set for a struct starting with a double(the first 8 bytes)
			 * followed by the mix of any integer type/float(the next 8 bytes).
			 * e.g struct {double, float, int}, or struct {double, long}
			 */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_DP_MISC;
		} else {
			/* The aggregate subtype is set for a struct mixed with any integer type/float
			 * with pure float/double on the first/second 8 bytes.
			 * e.g struct {short a[3], char b}, or struct {int float, int, float}
			 */
			structSigType = J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MISC;
		}

		return structSigType;
	}

	/* Merge 8 bytes of the 16-byte composition type array from the specified index to determine the composition types */
	static U_8
	getComposTypeFrom8Bytes(U_8 *first16ByteComposTypes, UDATA arrayIndex)
	{
		U_8 composType = 0;
		U_8 low4ByteComposType = 0;
		U_8 high4ByteComposType = 0;

		/* The specified index to the U_8[16] composition type array must be 0 or 8 */
		Assert_VM_true(arrayIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH);
		Assert_VM_true(0 == (arrayIndex % J9_FFI_UPCALL_COMPOSITION_TYPE_DWORD_SIZE));

		/* Merge the low 4 bytes of 8 bytes from the specified index to the composition type array */
		low4ByteComposType = getComposTypeFrom4Bytes(first16ByteComposTypes, arrayIndex);
		/* The first byte and the low 4 bytes of 8 bytes can't be padding bytes.
		 * e.g. a struct {padding, int, ...} is invalid if specified in java code.
		 */
		Assert_VM_true(J9_FFI_UPCALL_COMPOSITION_TYPE_E != low4ByteComposType);
		Assert_VM_true(J9_FFI_UPCALL_COMPOSITION_TYPE_E != first16ByteComposTypes[arrayIndex]);

		/* Merge the high 4 bytes of 8 bytes from the specified index to the composition type array */
		high4ByteComposType = getComposTypeFrom4Bytes(first16ByteComposTypes, arrayIndex + J9_FFI_UPCALL_COMPOSITION_TYPE_WORD_SIZE);

		composType = low4ByteComposType | high4ByteComposType;
		/* 'D' and 'E' can't coexist in 8 bytes */
		Assert_VM_true(!J9_ARE_ALL_BITS_SET(composType, J9_FFI_UPCALL_COMPOSITION_TYPE_D_E));

		switch (composType) {
		case J9_FFI_UPCALL_COMPOSITION_TYPE_F: // 8 bytes for single-precision floating point
		case J9_FFI_UPCALL_COMPOSITION_TYPE_F_E: // 8 bytes for single-precision floating point (4 bytes) plus the padding bytes
		case J9_FFI_UPCALL_COMPOSITION_TYPE_D: // 8 bytes for a double-precision floating point
			break;
		default:
			composType = J9_FFI_UPCALL_COMPOSITION_TYPE_M; // 8 bytes for the mix of integer types without pure float/double
			break;
		}

		return composType;
	}

	/* Merge 4 bytes of the 16-byte composition type array from the specified index to determine the composition types */
	static U_8
	getComposTypeFrom4Bytes(U_8 *first16ByteComposTypes, UDATA arrayIndex)
	{
		U_8 composType = 0;

		/* The specified index to the 16-byte composition type array must be one of 0, 4, 8 and 12 */
		Assert_VM_true(arrayIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_ARRAY_LENGTH);
		Assert_VM_true(0 == (arrayIndex % J9_FFI_UPCALL_COMPOSITION_TYPE_WORD_SIZE));

		for (UATA byteIndex = 0; byteIndex < J9_FFI_UPCALL_COMPOSITION_TYPE_WORD_SIZE; byteIndex++) {
			composType |= first16ByteComposTypes[arrayIndex + byteIndex];
		}

		switch (composType) {
		case J9_FFI_UPCALL_COMPOSITION_TYPE_E: // 4 padding bytes
		case J9_FFI_UPCALL_COMPOSITION_TYPE_F: // 4 bytes for single-precision floating point
		case J9_FFI_UPCALL_COMPOSITION_TYPE_D: // 4 bytes of a double-precision floating point
			break;
		default:
			/* It is impossible that 'F' or 'D' partially exits in 4 bytes */
			Assert_VM_true(J9_ARE_NO_BITS_SET(composType, (J9_FFI_UPCALL_COMPOSITION_TYPE_F | J9_FFI_UPCALL_COMPOSITION_TYPE_D)));
			composType = J9_FFI_UPCALL_COMPOSITION_TYPE_M; // 4 bytes for the mix of integer types
			break;
		}

		return composType;
	}

#endif /* JAVA_SPEC_VERSION >= 16 */
};

#endif /* LAYOUTFFITYPEHELPERS_HPP_ */

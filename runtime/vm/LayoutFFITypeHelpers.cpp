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

#include "LayoutFFITypeHelpers.hpp"

#if JAVA_SPEC_VERSION >= 16

extern "C" void
freeAllStructFFITypes(J9VMThread *currentThread, void **argFFITypes)
{
	ffi_type **argTypes = (ffi_type **)argFFITypes;
	if (NULL != argTypes) {
		LayoutFFITypeHelpers ffiTypeHelpers(currentThread);
		J9JavaVM * vm = currentThread->javaVM;
		PORT_ACCESS_FROM_JAVAVM(vm);

		for (U_32 argIndex = 0; argTypes[argIndex] != NULL; argIndex++) {
			ffiTypeHelpers.freeStructFFIType(argTypes[argIndex]);
		}
		j9mem_free_memory(argTypes);
		argTypes = NULL;
	}
}

ffi_type*
LayoutFFITypeHelpers::getArrayFFIType(char **layout, UDATA nElements) {
	ffi_type *typeFFI = NULL;
	ffi_type *elementType = NULL;
	PORT_ACCESS_FROM_JAVAVM(_vm);

	/* A struct starts with "#" plus the count of struct elements,
	 * followed by the type of struct elements.
	 * e.g. the preprocessed layout of
	 * struct int_struct{
	 *                   int elem1,
	 *                   int elem2
	 *                  }int_struct[2]
	 *     is #2[II]
	 */
	if ('#' == **layout) {
		elementType = getStructFFIType(layout);
		if (NULL == elementType) {
			goto doneGetArrayFFIType;
		}
	} else {
		elementType = getPrimitiveFFIType(**layout);
	}

	if (elementType != NULL) {
		typeFFI = (ffi_type *)j9mem_allocate_memory(sizeof(ffi_type), OMRMEM_CATEGORY_VM);
		if (NULL == typeFFI) {
			freeStructFFIType(elementType);
			goto doneGetArrayFFIType;
		}

		typeFFI->size = 0;
		typeFFI->alignment = 0;
		typeFFI->type = FFI_TYPE_STRUCT;
		typeFFI->elements = (ffi_type **) j9mem_allocate_memory(sizeof(ffi_type*) * (nElements + 1), OMRMEM_CATEGORY_VM);
		if (NULL == typeFFI->elements) {
			freeStructFFIType(elementType);
			j9mem_free_memory(typeFFI);
			typeFFI = NULL;
			goto doneGetArrayFFIType;
		}

		for (U_32 elemIndex = 0; elemIndex < nElements; elemIndex++) {
			typeFFI->elements[elemIndex] = elementType;
		}
		typeFFI->elements[nElements] = NULL;
	}
doneGetArrayFFIType:
	return typeFFI;
}

ffi_type*
LayoutFFITypeHelpers::getStructFFIType(char **layout)
{
	ffi_type *typeFFI = NULL;
	/* The struct layout is incremented for the '[' case in getStructFFITypeElements(), in which case
	 * the remaining layout string is then traversed when getStructFFITypeElements() is called recursively.
	 */
	(*layout)++; // skip over the '#' case
	ffi_type **structElements = getStructFFITypeElements(layout);
	PORT_ACCESS_FROM_JAVAVM(_vm);
	typeFFI = (ffi_type *)j9mem_allocate_memory(sizeof(ffi_type), OMRMEM_CATEGORY_VM);
	if (NULL == typeFFI) {
		setNativeOutOfMemoryError(_currentThread, 0, 0);
		goto doneGetStructFFIType;
	}
	typeFFI->size = 0;
	typeFFI->alignment = 0;
	typeFFI->type = FFI_TYPE_STRUCT;
	typeFFI->elements = structElements;

doneGetStructFFIType:
	return typeFFI;
}

ffi_type**
LayoutFFITypeHelpers::getStructFFITypeElements(char **layout)
{
	PORT_ACCESS_FROM_JAVAVM(_vm);
	
	char *currentLayout = *layout;
	ffi_type **elements = NULL;
	UDATA nElements = 0;
	UDATA elementCount = getIntFromLayout(&currentLayout);

	elements = (ffi_type **) j9mem_allocate_memory(sizeof(ffi_type *) * (elementCount + 1), OMRMEM_CATEGORY_VM);
	if (NULL == elements) {
		goto doneGetStructFFITypeElements;
	}
	elements[elementCount] = NULL;
	currentLayout++; // Skip over the '[' case to the struct elements

	while (*currentLayout != '\0') {
		char symb = *currentLayout;
		switch (symb) {
		case '#':
		{
			/* Start of nested struct e.g. #5[...] */
			ffi_type *result = getStructFFIType(&currentLayout);
			if (NULL == result) {
				freeStructFFITypeElements(elements);
				elements = NULL;
				goto doneGetStructFFITypeElements;
			}
			elements[nElements] = result;
			nElements++;
			break;
		}
		case ']': /* End of struct */
			*layout = currentLayout;
			/* The last element of struct needs to be NULL for FFI_TYPE_STRUCT */
			elements[nElements] = NULL;
			goto doneGetStructFFITypeElements;
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
			/* Get the count of the array elements, followed by the elements's type.
			 * e.g.
			 * 1) "5:I" for an int array with 5 int elements (int[5])
			 * 2) "5:#2[II]" for a struct array with 5 struct elements (each stuct contains 2 integers).
			 */
			UDATA nArray = getIntFromLayout(&currentLayout);
			currentLayout++; // Skip over the separator of an array (":") to the elements
			ffi_type *result = getArrayFFIType(&currentLayout, nArray);
			if (NULL == result) {
				freeStructFFITypeElements(elements);
				elements = NULL;
				goto doneGetStructFFITypeElements;
			}
			elements[nElements] = result;
			nElements++;
			break;
		}
		default:
			elements[nElements] = getPrimitiveFFIType(*currentLayout);
			nElements++;
		}
		currentLayout++;
	}

doneGetStructFFITypeElements:
	return elements;
}

void
LayoutFFITypeHelpers::freeStructFFIType(ffi_type *ffiType)
{
	if ((NULL != ffiType) && (FFI_TYPE_STRUCT == ffiType->type)) {
		if (NULL != ffiType->elements) {
			PORT_ACCESS_FROM_JAVAVM(_vm);
			freeStructFFITypeElements(ffiType->elements);
			ffiType->elements = NULL;
			j9mem_free_memory(ffiType);
			ffiType = NULL;
		}
	}
}

void
LayoutFFITypeHelpers::freeStructFFITypeElements(ffi_type **elements)
{
	if (NULL != elements) {
		PORT_ACCESS_FROM_JAVAVM(_vm);
		for (U_32 elemIndex = 0; elements[elemIndex] != NULL; elemIndex++) {
			freeStructFFIType(elements[elemIndex]);
		}
		j9mem_free_memory(elements);
	}
}

#endif /* JAVA_SPEC_VERSION >= 16 */

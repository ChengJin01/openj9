/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corp. and others
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

#include "OutOfLineINL.hpp"

#include "BytecodeAction.hpp"
#include "UnsafeAPI.hpp"
#include "j9vmnls.h"

#ifdef J9VM_OPT_PANAMA
#include "FFITypeHelpers.hpp"
#endif /* J9VM_OPT_PANAMA */

extern "C" {

#ifdef J9VM_OPT_PANAMA
/* jdk.internal.foreign.abi.ProgrammableInvoker: private native void initCifNativeThunkData(String[] argLayoutStrings); */
VM_BytecodeAction
OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_initCifNativeThunkData(J9VMThread *currentThread, J9Method *method)
{
	VM_BytecodeAction rc = EXECUTE_BYTECODE;
	J9JavaVM *vm = currentThread->javaVM;
	ffi_type **args = NULL;
	ffi_cif *cif = NULL;

	printf("\ncalling OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_initCifNativeThunkData ....");

	j9object_t nativeInvoker = *(j9object_t*)(currentThread->sp + 1);
	cif = (ffi_cif *)JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_CIFNATIVETHUNKDATAREF(currentThread, nativeInvoker);
	Assert_VM_Null(cif);

	j9object_t methodType = JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_METHODTYPE(currentThread, nativeInvoker);
	J9Class *returnTypeClass = J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, J9VMJAVALANGINVOKEMETHODTYPE_RTYPE(currentThread, methodType));
	j9object_t argTypesObject = J9VMJAVALANGINVOKEMETHODTYPE_PTYPES(currentThread, methodType);

	FFITypeHelpers FFIHelpers = FFITypeHelpers(currentThread);
	j9object_t argLayoutStringsObject = *(j9object_t*)currentThread->sp;
	bool layoutStringsExist = false;
	if (NULL != argLayoutStringsObject) {
		layoutStringsExist = true;
	}
	
	PORT_ACCESS_FROM_JAVAVM(vm);

	/* args[0] stores the ffi_type of the return argument of the method.
	 * The remaining args array stores the ffi_type of the input arguments.
	 * typeCount is the number of input arguments, plus one for the return argument.
	 */
	U_32 typeCount = J9INDEXABLEOBJECT_SIZE(currentThread, argTypesObject) + 1;
	args = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * typeCount, OMRMEM_CATEGORY_VM);
	if (NULL == args) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setNativeOutOfMemoryError(currentThread, 0, 0);
		goto done;
	}

	/* Zero out the memory because if an error occurs before all the entries in this array
	 * are initialized then the error handling code at freeAllMemoryThenExit will attempt
	 * to free all the pointers, some of which will not be initialized.
	 */
	memset(args, 0, sizeof(ffi_type *) * typeCount);

	/* In the common case we expect that layoutStringsExist will be NULL,
	 * which is why args[0] is written first then layoutStringsExist is
	 * checked after that.
	 */
	args[0] = (ffi_type *)FFIHelpers.getFFIType(returnTypeClass);
	if (layoutStringsExist) {
		j9object_t retlayoutStringObject = J9JAVAARRAYOFOBJECT_LOAD(currentThread, argLayoutStringsObject, 0);
		if (NULL != retlayoutStringObject) {
			UDATA structSize = FFIHelpers.getCustomFFIType(&(args[0]), retlayoutStringObject);
			if ((NULL == args[0]) || (NULL == args[0]->elements) || (0 == structSize)) {
				rc = GOTO_THROW_CURRENT_EXCEPTION;
				setNativeOutOfMemoryError(currentThread, 0, 0);
				goto freeAllMemoryThenExit;
			}
		}
	}

	if (layoutStringsExist) {
		for (U_8 i = 0; i < typeCount-1; i++) {
			j9object_t layoutStringObject = J9JAVAARRAYOFOBJECT_LOAD(currentThread, argLayoutStringsObject, i+1);
			if (NULL == layoutStringObject) {
				args[i+1] = (ffi_type *)FFIHelpers.getFFIType(J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, J9JAVAARRAYOFOBJECT_LOAD(currentThread, argTypesObject, i)));
			} else {
				/**
				 * Pointers are converted to longs before the callout so the ffi type for pointers is sint64.
				 * Structs are also converted to longs before the callout, and are identified by the non-null layout string of the argument.
				 * The struct ffi type is created after the struct layout string is parsed in getCustomFFIType(ffi_type**, j9object_t).
				 */
				UDATA structSize = FFIHelpers.getCustomFFIType(&(args[i+1]), layoutStringObject);
				if (UDATA_MAX == structSize) {
					rc = GOTO_THROW_CURRENT_EXCEPTION;
					setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
					goto freeAllMemoryThenExit;
				}
			}
		}
	} else {
		for (U_8 i = 0; i < typeCount-1; i++) {
			args[i+1] = (ffi_type *)FFIHelpers.getFFIType(J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, J9JAVAARRAYOFOBJECT_LOAD(currentThread, argTypesObject, i)));
		}
	}

	/* Set typeCount-1 in that ffi_prep_cif() expects the number of input arguments of the method */
	if (FFI_OK != ffi_prep_cif(cif, FFI_DEFAULT_ABI, typeCount-1, args[0], &(args[1]))) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
	}

freeAllMemoryThenExit:
	for (U_32 i = 0; i < typeCount; i++) {
		FFIHelpers.freeStructFFIType(args[i]);
	}
	j9mem_free_memory(args);

done:
	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 2);
	return rc;
}

/* jdk.internal.foreign.abi.ProgrammableInvoker: private native void getCifNativeThunkRefSize(); */
VM_BytecodeAction
OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_getCifNativeThunkRefSize(J9VMThread *currentThread, J9Method *method)
{
	/*
	JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_J9NATIVETHUNKDATAREF(currentThread, sizeof(ffi_cif));
	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 1);
	*/
	return EXECUTE_BYTECODE;
}
#endif /* J9VM_OPT_PANAMA */

}

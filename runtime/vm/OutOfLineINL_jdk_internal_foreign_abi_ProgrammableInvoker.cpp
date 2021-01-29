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

#include "OutOfLineINL.hpp"

#include "BytecodeAction.hpp"
#include "UnsafeAPI.hpp"
#include "j9vmnls.h"
#include "omrlinkedlist.h"

#if JAVA_SPEC_VERSION >= 16
#include "FFITypeHelpers.hpp"
#endif /* JAVA_SPEC_VERSION >= 16 */

extern "C" {

#if JAVA_SPEC_VERSION >= 16
/* jdk.internal.foreign.abi.ProgrammableInvoker: private native void initCifNativeThunkData(String[] argLayoutStrings); */
VM_BytecodeAction
OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_initCifNativeThunkData(J9VMThread *currentThread, J9Method *method)
{
	VM_BytecodeAction rc = EXECUTE_BYTECODE;
	J9JavaVM *vm = currentThread->javaVM;
	FFITypeHelpers FFIHelpers = FFITypeHelpers(currentThread);
	ffi_cif *cif = NULL;
	ffi_type *returnType = NULL;
	ffi_type **argTypes = NULL;
	J9CifArgumentTypes *cifArgTypesNode = NULL;
	bool isNewArgTypes = false;

	PORT_ACCESS_FROM_JAVAVM(vm);

	printf("\n**********calling OutOfLineINL_initCifNativeThunkData***BEGIN*******\n");

	j9object_t nativeInvoker = *(j9object_t*)(currentThread->sp + 1);
	j9object_t methodType = J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_METHODTYPE(currentThread, nativeInvoker);
	printf("\nOutOfLineINL_initCifNativeThunkData: methodType = %p\n", methodType);
	Assert_VM_notNull(methodType);

	J9Class *returnTypeClass = J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, J9VMJAVALANGINVOKEMETHODTYPE_RTYPE(currentThread, methodType));
	j9object_t argTypesObject = J9VMJAVALANGINVOKEMETHODTYPE_PTYPES(currentThread, methodType);
	U_32 typeCount = J9INDEXABLEOBJECT_SIZE(currentThread, argTypesObject);
	j9object_t argLayoutStringsObject = *(j9object_t*)currentThread->sp;
	bool layoutStringsExist = false;
	if (NULL != argLayoutStringsObject) {
		layoutStringsExist = true;
	}

	if (NULL == vm->cifNativeCalloutDataCache) {
		vm->cifNativeCalloutDataCache = pool_new(sizeof(ffi_cif), 0, 0, 0, J9_GET_CALLSITE(), OMRMEM_CATEGORY_VM, POOL_FOR_PORT(PORTLIB));
		if (NULL == vm->cifNativeCalloutDataCache) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto _exit;
		}
		printf("\ninitCifNativeThunkData: create a new cifNativeCalloutDataCache.....\n");
	}

	/* In the common case we expect that layoutStringsExist will be NULL,
	 * which is why argTypes[0] is written first then layoutStringsExist is
	 * checked after that.
	 */
	returnType = (ffi_type *)FFIHelpers.getFFIType(returnTypeClass);
	if (layoutStringsExist) {
		/*
		j9object_t retlayoutStringObject = J9JAVAARRAYOFOBJECT_LOAD(currentThread, argLayoutStringsObject, 0);
		if (NULL != retlayoutStringObject) {
			UDATA structSize = FFIHelpers.getCustomFFIType(&(argTypes[0]), retlayoutStringObject);
			if ((NULL == argTypes[0]) || (NULL == argTypes[0]->elements) || (0 == structSize)) {
				rc = GOTO_THROW_CURRENT_EXCEPTION;
				setNativeOutOfMemoryError(currentThread, 0, 0);
				goto freeAllMemoryThenExit;
			}
		}
		*/
	}

	argTypes = J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_ARGTYPESADDR(currentThread, nativeInvoker);
	if (NULL == argTypes) {
		argTypes = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * typeCount, OMRMEM_CATEGORY_VM);
		if (NULL == argTypes) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto _exit;
		}
		memset(argTypes, 0, sizeof(ffi_type *) * typeCount);
		isNewArgTypes = true;
		printf("\ninitCifNativeThunkData: argTypes doesn't exist: create a new argTypes\n");

		if (layoutStringsExist) {
			/*
			for (U_8 i = 0; i < typeCount; i++) {
				j9object_t layoutStringObject = J9JAVAARRAYOFOBJECT_LOAD(currentThread, argLayoutStringsObject, i);
				if (NULL == layoutStringObject) {
					argTypes[i] = (ffi_type *)FFIHelpers.getFFIType(J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, J9JAVAARRAYOFOBJECT_LOAD(currentThread, argTypesObject, i)));
				} else {
					// Pointers are converted to longs before the callout so the ffi type for pointers is sint64.
					// Structs are also converted to longs before the callout, and are identified by the non-null layout string of the argument.
					// The struct ffi type is created after the struct layout string is parsed in getCustomFFIType(ffi_type**, j9object_t).
					UDATA structSize = FFIHelpers.getCustomFFIType(&(argTypes[i]), layoutStringObject);
					if (UDATA_MAX == structSize) {
						rc = GOTO_THROW_CURRENT_EXCEPTION;
						setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
						goto freeAllMemoryThenExit;
					}
				}
			}
			*/
		} else {
			for (U_8 i = 0; i < typeCount; i++) {
				argTypes[i] = (ffi_type *)FFIHelpers.getFFIType(J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, J9JAVAARRAYOFOBJECT_LOAD(currentThread, argTypesObject, i)));
			}
		}
	}

	omrthread_monitor_enter(vm->cifNativeCalloutDataCacheMutex);
	cif = (ffi_cif *)pool_newElement(vm->cifNativeCalloutDataCache);
	omrthread_monitor_exit(vm->cifNativeCalloutDataCacheMutex);
	if (NULL == cif) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setNativeOutOfMemoryError(currentThread, 0, 0);
		goto freeAllMemoryThenExit;
	}

	if (FFI_OK != ffi_prep_cif(cif, FFI_DEFAULT_ABI, typeCount, returnType, &(argTypes[0]))) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
		printf("\n**********calling OutOfLineINL_initCifNativeThunkData***NOK: GOTO_THROW_CURRENT_EXCEPTION\n");
		goto freeAllMemoryThenExit;
	}

	if (isNewArgTypes) {
		cifArgTypesNode = (J9CifArgumentTypes *)j9mem_allocate_memory(sizeof(J9CifArgumentTypes), OMRMEM_CATEGORY_VM);
		if (NULL == cifArgTypesNode) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto freeAllMemoryThenExit;
		}
		cifArgTypesNode->argumentTypes = argTypes;

		omrthread_monitor_enter(vm->cifArgumentTypesMutex);
		J9_LINKED_LIST_ADD_LAST(vm->cifArgumentTypesListHead, cifArgTypesNode);
		omrthread_monitor_exit(vm->cifArgumentTypesMutex);
		J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_ARGTYPESADDR(currentThread, nativeInvoker, argTypes);
		printf("\ninitCifNativeThunkData: set argTypes back to java code after ffi_prep_cif: cifArgTypesNode = %p\n", argTypes);
	}

	J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_CIFNATIVETHUNKADDR(currentThread, nativeInvoker, cif);
	printf("\n**********calling OutOfLineINL_initCifNativeThunkData***END OK*******\n");

_exit:
	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 2);
	return rc;

freeAllMemoryThenExit:
	if (isNewArgTypes) {
		for (U_32 i = 0; i < typeCount; i++) {
			FFIHelpers.freeStructFFIType(argTypes[i]);
		}
		j9mem_free_memory(argTypes);
		argTypes = NULL;
	}

	if (NULL != cif) {
		omrthread_monitor_enter(vm->cifNativeCalloutDataCacheMutex);
		pool_removeElement(vm->cifNativeCalloutDataCache, cif);
		omrthread_monitor_exit(vm->cifNativeCalloutDataCacheMutex);
	}
	goto _exit;
}

#endif /* JAVA_SPEC_VERSION >= 16 */

}

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

#include "VMHelpers.hpp"
#include "BytecodeAction.hpp"
#include "UnsafeAPI.hpp"
#include "j9vmnls.h"
#include "omrlinkedlist.h"
#include "OutOfLineINL.hpp"
#include "FFITypeHelpers.hpp"
#include "AtomicSupport.hpp"

extern "C" {

#if JAVA_SPEC_VERSION >= 16
/* jdk.internal.foreign.abi.ProgrammableInvoker: private static synchronized native void resolveRequiredFields(); */
VM_BytecodeAction
OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_resolveRequiredFields(J9VMThread *currentThread, J9Method *method)
{
	VM_BytecodeAction rc = EXECUTE_BYTECODE;
	J9JavaVM *vm = currentThread->javaVM;
	J9ConstantPool *jclConstantPool = (J9ConstantPool *)vm->jclConstantPool;
	const int cpEntryNum = 2;
	U_16 cpIndex[cpEntryNum] = {
				J9VMCONSTANTPOOL_JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_CIFNATIVETHUNKADDR,
				J9VMCONSTANTPOOL_JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_ARGTYPESADDR
			};

	printf("\n**********calling OutOfLineINL_resolveRequiredFields***BEGIN*******\n");

	for (int i = 0; i < cpEntryNum; i++) {
		J9RAMFieldRef *cpFieldRef = ((J9RAMFieldRef*)jclConstantPool) + cpIndex[i];
		UDATA const flags = cpFieldRef->flags;
		UDATA const valueOffset = cpFieldRef->valueOffset;

		if (!VM_VMHelpers::instanceFieldRefIsResolved(flags, valueOffset)) {
			resolveInstanceFieldRef(currentThread, NULL, jclConstantPool, cpIndex[i], J9_RESOLVE_FLAG_NO_THROW_ON_FAIL | J9_RESOLVE_FLAG_JCL_CONSTANT_POOL, NULL);
			if (VM_VMHelpers::exceptionPending(currentThread)) {
				rc = GOTO_THROW_CURRENT_EXCEPTION;
				goto done;
			}
			printf("\nOutOfLineINL_resolveRequiredFields: exit from resolveInstanceFieldRef: cpIndex[%d] = %d, valueOffset = %d", i, (int)cpIndex[i], (int)cpFieldRef->valueOffset);
		}
	}

done:
	printf("\n**********calling OutOfLineINL_resolveRequiredFields***END*******\n");
	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 0);
	return rc;
}

/* jdk.internal.foreign.abi.ProgrammableInvoker: private native void initCifNativeThunkData(String[] argLayouts, String retLayout, boolean newArgTypes); */
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

	PORT_ACCESS_FROM_JAVAVM(vm);

	printf("\n**********calling OutOfLineINL_initCifNativeThunkData***BEGIN*******\n");

	bool newArgTypes = (bool)(*(U_32*)currentThread->sp);
	j9object_t retLayoutStrObject = J9_JNI_UNWRAP_REFERENCE(currentThread->sp + 1);
	j9object_t argLayoutStrsObject = J9_JNI_UNWRAP_REFERENCE(currentThread->sp + 2);
	j9object_t nativeInvoker = J9_JNI_UNWRAP_REFERENCE(currentThread->sp + 3);
	U_32 argTypesCount = J9INDEXABLEOBJECT_SIZE(currentThread, argLayoutStrsObject);
	printf("\ninitCifNativeThunkData: argTypesCount = %d\n", (int)argTypesCount);
	returnType = (ffi_type *)FFIHelpers.mapLayoutToPrimitiveFFIType(retLayoutStrObject);

	if (!newArgTypes) {
		argTypes = (ffi_type **)(UDATA)J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_ARGTYPESADDR(currentThread, nativeInvoker);
		printf("\nOutOfLineINL_initCifNativeThunkData: argTypes EXISTS: old argTypes = %p\n", argTypes);
	} else {
		argTypes = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * argTypesCount, OMRMEM_CATEGORY_VM);
		if (NULL == argTypes) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto done;
		}
		memset(argTypes, 0, sizeof(ffi_type *) * argTypesCount);
		printf("\ninitCifNativeThunkData: argTypes DOESN'T EXISTS: create a new argTypes = %p\n", argTypes);

		for (U_8 i = 0; i < argTypesCount; i++) {
			argTypes[i] = (ffi_type *)FFIHelpers.mapLayoutToPrimitiveFFIType(J9JAVAARRAYOFOBJECT_LOAD(currentThread, argLayoutStrsObject, i));
		}
	}
	printf("\nOutOfLineINL_initCifNativeThunkData: argTypes = %p\n", argTypes);

	if (NULL == vm->cifNativeCalloutDataCache) {
		vm->cifNativeCalloutDataCache = pool_new(sizeof(ffi_cif), 0, 0, 0, J9_GET_CALLSITE(), OMRMEM_CATEGORY_VM, POOL_FOR_PORT(PORTLIB));
		if (NULL == vm->cifNativeCalloutDataCache) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto freeAllMemoryThenExit;
		}
		printf("\ninitCifNativeThunkData: create a new cifNativeCalloutDataCache.....\n");
	}

	omrthread_monitor_enter(vm->cifNativeCalloutDataCacheMutex);
	cif = (ffi_cif *)pool_newElement(vm->cifNativeCalloutDataCache);
	omrthread_monitor_exit(vm->cifNativeCalloutDataCacheMutex);
	if (NULL == cif) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setNativeOutOfMemoryError(currentThread, 0, 0);
		goto freeAllMemoryThenExit;
	}

	if (FFI_OK != ffi_prep_cif(cif, FFI_DEFAULT_ABI, argTypesCount, returnType, &(argTypes[0]))) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
		printf("\n**********calling OutOfLineINL_initCifNativeThunkData: ffi_prep_cif NOK!!!: GOTO_THROW_CURRENT_EXCEPTION\n");
		goto freeAllMemoryThenExit;
	}

	if (newArgTypes) {
		cifArgTypesNode = (J9CifArgumentTypes *)j9mem_allocate_memory(sizeof(J9CifArgumentTypes), OMRMEM_CATEGORY_VM);
		if (NULL == cifArgTypesNode) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto freeAllMemoryThenExit;
		}

		cifArgTypesNode->argumentTypes = (void **)argTypes;
		omrthread_monitor_enter(vm->cifArgumentTypesMutex);
		J9_LINKED_LIST_ADD_LAST(vm->cifArgumentTypesListHead, cifArgTypesNode);
		omrthread_monitor_exit(vm->cifArgumentTypesMutex);

		VM_AtomicSupport::writeBarrier();
		printf("\ninitCifNativeThunkData: set argTypes back to java code after ffi_prep_cif: argTypesLongValue = %p\n", argTypes);
		J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_ARGTYPESADDR(currentThread, nativeInvoker, (intptr_t)argTypes);
		printf("\ninitCifNativeThunkData: returnType = %p\n", returnType);
	}

	VM_AtomicSupport::writeBarrier();
	printf("\ninitCifNativeThunkData: set argTypes back to java code after ffi_prep_cif: cifLongValue = %p\n", cif);
	J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_CIFNATIVETHUNKADDR(currentThread, nativeInvoker, (intptr_t)cif);

done:
	printf("\n**********calling OutOfLineINL_initCifNativeThunkData***END OK*******\n");
	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 4);
	return rc;

freeAllMemoryThenExit:
	if (newArgTypes) {
		j9mem_free_memory(argTypes);
		argTypes = NULL;
	}
	goto done;
}

#endif /* JAVA_SPEC_VERSION >= 16 */

}

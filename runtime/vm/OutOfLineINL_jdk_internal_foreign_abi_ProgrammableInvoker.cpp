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
#include "OutOfLineINL.hpp"
#include "LayoutFFITypeHelpers.hpp"
#include "AtomicSupport.hpp"

extern "C" {

#if JAVA_SPEC_VERSION >= 16

typedef struct stru_Int_Int stru_Int_Int;
//typedef struct stru_Int_NestedStruct stru_Int_NestedStruct;

struct stru_Float_Float {
	float elem1;
	float elem2;
};

/*
struct stru_NestedStruct_Float {
	stru_Float_Float elem1;
	float elem2;
};

static float addFloatAndFloatsFromNestedStruct(float arg1, stru_NestedStruct_Float arg2)
{
	float floatSum = arg1 + arg2.elem2 + arg2.elem1.elem1 + arg2.elem1.elem2;
	return floatSum;
}
*/


typedef struct stru_Float_NestedStruct stru_Float_NestedStruct;

struct stru_Float_NestedStruct {
	float elem1;
	stru_Float_Float elem2;
};

static float addFloatAndFloatsFromNestedStruct_reverseOrder(float arg1, stru_Float_NestedStruct arg2)
{
	float floatSum = arg1 + arg2.elem1 + arg2.elem2.elem1 + arg2.elem2.elem2;
	return floatSum;
}


/* jdk.internal.foreign.abi.ProgrammableInvoker: private static synchronized native void resolveRequiredFields(); */
VM_BytecodeAction
OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_resolveRequiredFields(J9VMThread *currentThread, J9Method *method)
{
	VM_BytecodeAction rc = EXECUTE_BYTECODE;
	J9JavaVM *vm = currentThread->javaVM;
	J9ConstantPool *jclConstantPool = (J9ConstantPool *)vm->jclConstantPool;
	const int cpEntryNum = 2;
	PORT_ACCESS_FROM_JAVAVM(vm);

	U_16 cpIndex[cpEntryNum] = {
				J9VMCONSTANTPOOL_JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_CIFNATIVETHUNKADDR,
				J9VMCONSTANTPOOL_JDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_ARGTYPESADDR
			};

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
		}
	}

done:
/*
{
	  float returnValue = 0;
	  float *returnStorageTest = &returnValue;
	  ffi_cif cif_temp;
	  UDATA structElemNum = 2;
	  UDATA nestedStructElemNum = 2;
	  UDATA argNum = 2;
	  ffi_type **cls_struct_fields1 = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (structElemNum + 1), OMRMEM_CATEGORY_VM);
	  ffi_type **cls_struct_fields2 = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (nestedStructElemNum + 1), OMRMEM_CATEGORY_VM);
	  ffi_type **dbl_arg_types = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (argNum + 1), OMRMEM_CATEGORY_VM);
	  void **args_db = (void **)j9mem_allocate_memory(sizeof(void *) * (argNum + 1), OMRMEM_CATEGORY_VM);
	  ffi_type cls_struct_type1, cls_struct_type2;
	  ffi_type *retType = &ffi_type_float;
	  float arg1;
	  float *arg2 = (float *)j9mem_allocate_memory(sizeof(stru_NestedStruct_Float), OMRMEM_CATEGORY_VM);

	  cls_struct_fields2[0] = &ffi_type_float;
	  cls_struct_fields2[1] = &ffi_type_float;
	  cls_struct_fields2[2] = NULL;

	  cls_struct_type2.size = 0;
	  cls_struct_type2.alignment = 0;
	  cls_struct_type2.type = FFI_TYPE_STRUCT;
	  cls_struct_type2.elements = cls_struct_fields2;

	  cls_struct_fields1[0] = &cls_struct_type2;
	  cls_struct_fields1[1] = &ffi_type_float;
	  cls_struct_fields1[2] = NULL;

	  cls_struct_type1.size = 0;
	  cls_struct_type1.alignment = 0;
	  cls_struct_type1.type = FFI_TYPE_STRUCT;
	  cls_struct_type1.elements = cls_struct_fields1;

	  dbl_arg_types[0] = &ffi_type_float;
	  dbl_arg_types[1] = &cls_struct_type1;
	  dbl_arg_types[2] = NULL;

	 ffi_prep_cif(&cif_temp, FFI_DEFAULT_ABI, 2, retType, dbl_arg_types);
	 arg1 = 37.88;
	 arg2[0] = 31.22;
	 arg2[1] = 33.44;
	 arg2[2] = 35.66;
	 args_db[0] = &arg1;
	 args_db[1] = arg2;
	 args_db[2] = NULL;

	 printf("\nOutOfLineINL: calling ffi_call: ... ");
	ffi_call(&cif_temp, FFI_FN(addFloatAndFloatsFromNestedStruct), returnStorageTest, args_db);
	printf("\nffi_call1: addFloatAndFloatsFromNestedStruct: returnStorageTest = %f, returnValue = %f\n", *returnStorageTest, returnValue);
	j9mem_free_memory(cls_struct_fields1);
	j9mem_free_memory(cls_struct_fields2);
	j9mem_free_memory(dbl_arg_types);
	j9mem_free_memory(args_db);
}
*/

{
	  float returnValue = 0;
	  float *returnStorageTest = &returnValue;
	  ffi_cif cif_temp;
	  UDATA structElemNum = 2;
	  UDATA nestedStructElemNum = 2;
	  UDATA argNum = 2;
	  ffi_type **cls_struct_fields1 = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (structElemNum + 1), OMRMEM_CATEGORY_VM);
	  ffi_type **cls_struct_fields2 = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (nestedStructElemNum + 1), OMRMEM_CATEGORY_VM);
	  ffi_type **dbl_arg_types = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (argNum + 1), OMRMEM_CATEGORY_VM);
	  void **args_db = (void **)j9mem_allocate_memory(sizeof(void *) * (argNum + 1), OMRMEM_CATEGORY_VM);
	  ffi_type cls_struct_type1, cls_struct_type2;
	  ffi_type *retType = &ffi_type_float;
	  float arg1;
	  float *arg2 = (float *)j9mem_allocate_memory(sizeof(stru_Float_NestedStruct), OMRMEM_CATEGORY_VM);

	  cls_struct_fields2[0] = &ffi_type_float;
	  cls_struct_fields2[1] = &ffi_type_float;
	  cls_struct_fields2[2] = NULL;

	  cls_struct_type2.size = 0;
	  cls_struct_type2.alignment = 0;
	  cls_struct_type2.type = FFI_TYPE_STRUCT;
	  cls_struct_type2.elements = cls_struct_fields2;

	  cls_struct_fields1[0] = &ffi_type_float;
	  cls_struct_fields1[1] = &cls_struct_type2;
	  cls_struct_fields1[2] = NULL;

	  cls_struct_type1.size = 0;
	  cls_struct_type1.alignment = 0;
	  cls_struct_type1.type = FFI_TYPE_STRUCT;
	  cls_struct_type1.elements = cls_struct_fields1;

	  dbl_arg_types[0] = &ffi_type_float;
	  dbl_arg_types[1] = &cls_struct_type1;
	  dbl_arg_types[2] = NULL;

	 ffi_prep_cif(&cif_temp, FFI_DEFAULT_ABI, 2, retType, dbl_arg_types);
	 arg1 = 37.88;
	 arg2[0] = 31.22;
	 arg2[1] = 33.44;
	 arg2[2] = 35.66;
	 args_db[0] = &arg1;
	 args_db[1] = arg2;
	 args_db[2] = NULL;

	 printf("\nOutOfLineINL: calling ffi_call: ... ");
	ffi_call(&cif_temp, FFI_FN(addFloatAndFloatsFromNestedStruct_reverseOrder), returnStorageTest, args_db);
	printf("\nffi_call1: addFloatAndFloatsFromNestedStruct_reverseOrder: returnStorageTest = %f, returnValue = %f\n", *returnStorageTest, returnValue);
	j9mem_free_memory(cls_struct_fields1);
	j9mem_free_memory(cls_struct_fields2);
	j9mem_free_memory(dbl_arg_types);
	j9mem_free_memory(args_db);
}

	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 0);
	return rc;
}

/**
 * jdk.internal.foreign.abi.ProgrammableInvoker: private native void initCifNativeThunkData(String[] argLayouts, String retLayout, boolean newArgTypes);
 *
 * @brief Prepare the prep_cif for the native function specified by the arguments/return layouts
 * @param argLayouts[in] A c string array describing the argument layouts
 * @param retLayout[in] A c string describing the return layouts
 * @param newArgTypes[in] a flag determining whether to create a new ffi_type array for arguments
 * @return void
 */
VM_BytecodeAction
OutOfLineINL_jdk_internal_foreign_abi_ProgrammableInvoker_initCifNativeThunkData(J9VMThread *currentThread, J9Method *method)
{
	VM_BytecodeAction rc = EXECUTE_BYTECODE;
	J9JavaVM *vm = currentThread->javaVM;
	LayoutFFITypeHelpers ffiTypeHelpers(currentThread);
	ffi_cif *cif = NULL;
	ffi_type *returnType = NULL;
	ffi_type **argTypes = NULL;

	bool newArgTypes = (bool)(*(U_32*)currentThread->sp);
	j9object_t retLayoutStrObject = J9_JNI_UNWRAP_REFERENCE(currentThread->sp + 1);
	j9object_t argLayoutStrsObject = J9_JNI_UNWRAP_REFERENCE(currentThread->sp + 2);
	j9object_t nativeInvoker = J9_JNI_UNWRAP_REFERENCE(currentThread->sp + 3);
	U_32 argTypesCount = J9INDEXABLEOBJECT_SIZE(currentThread, argLayoutStrsObject);
	UDATA returnLayoutSize = 0;

	PORT_ACCESS_FROM_JAVAVM(vm);

	/* Set up the ffi_type of the return layout in the case of primitive or struct */
	returnLayoutSize = ffiTypeHelpers.getLayoutFFIType(&returnType, retLayoutStrObject);
	if (returnLayoutSize >= UDATA_MAX) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
		goto done;
	/* Only intended for strut as the primitive's ffi_type is non-null */
	} else if ((NULL == returnType)
	|| ((FFI_TYPE_STRUCT == returnType->type) && (NULL == returnType->elements))
	) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setNativeOutOfMemoryError(currentThread, 0, 0);
		goto done;
	}

	if (!newArgTypes) {
		argTypes = (ffi_type **)(UDATA)J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_ARGTYPESADDR(currentThread, nativeInvoker);
	} else {
		argTypes = (ffi_type **)j9mem_allocate_memory(sizeof(ffi_type *) * (argTypesCount + 1), OMRMEM_CATEGORY_VM);
		if (NULL == argTypes) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto freeAllMemoryThenExit;
		}
		argTypes[argTypesCount] = NULL;

		for (U_32 argIndex = 0; argIndex < argTypesCount; argIndex++) {
			j9object_t argLayoutStrObject = J9JAVAARRAYOFOBJECT_LOAD(currentThread, argLayoutStrsObject, argIndex);
			/* Set up the ffi_type of the argument layout in the case of primitive or struct */
			UDATA argLayoutSize = ffiTypeHelpers.getLayoutFFIType(&argTypes[argIndex], argLayoutStrObject);
			if (argLayoutSize >= UDATA_MAX) {
				rc = GOTO_THROW_CURRENT_EXCEPTION;
				setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
				goto freeAllMemoryThenExit;
			/* Only intended for strut as the primitive's ffi_type is non-null */
			} else if ((NULL == argTypes[argIndex])
			|| ((FFI_TYPE_STRUCT == argTypes[argIndex]->type) && (NULL == argTypes[argIndex]->elements))
			) {
				rc = GOTO_THROW_CURRENT_EXCEPTION;
				setNativeOutOfMemoryError(currentThread, 0, 0);
				goto freeAllMemoryThenExit;
			}
		}
	}

	if (NULL == vm->cifNativeCalloutDataCache) {
		vm->cifNativeCalloutDataCache = pool_new(sizeof(ffi_cif), 0, 0, 0, J9_GET_CALLSITE(), OMRMEM_CATEGORY_VM, POOL_FOR_PORT(PORTLIB));
		if (NULL == vm->cifNativeCalloutDataCache) {
			rc = GOTO_THROW_CURRENT_EXCEPTION;
			setNativeOutOfMemoryError(currentThread, 0, 0);
			goto freeAllMemoryThenExit;
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

	if (FFI_OK != ffi_prep_cif(cif, FFI_DEFAULT_ABI, argTypesCount, returnType, &(argTypes[0]))) {
		rc = GOTO_THROW_CURRENT_EXCEPTION;
		setCurrentException(currentThread, J9VMCONSTANTPOOL_JAVALANGINTERNALERROR, NULL);
		goto freeAllMemoryThenExit;
	}

	if (newArgTypes) {
		VM_AtomicSupport::writeBarrier();
		J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_ARGTYPESADDR(currentThread, nativeInvoker, (intptr_t)argTypes);
	}

	VM_AtomicSupport::writeBarrier();
	J9VMJDKINTERNALFOREIGNABIPROGRAMMABLEINVOKER_SET_CIFNATIVETHUNKADDR(currentThread, nativeInvoker, (intptr_t)cif);

done:
	VM_OutOfLineINL_Helpers::returnVoid(currentThread, 4);
	return rc;

freeAllMemoryThenExit:
	if (newArgTypes && (NULL != argTypes)) {
		for (U_32 argIndex = 0; argTypes[argIndex] != NULL; argIndex++) {
			ffiTypeHelpers.freeStructFFIType(argTypes[argIndex]);
		}
		j9mem_free_memory(argTypes);
		argTypes = NULL;
	}
	ffiTypeHelpers.freeStructFFIType(returnType);
	goto done;
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

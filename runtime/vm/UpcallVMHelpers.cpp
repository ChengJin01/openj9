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

#include "j9accessbarrier.h"
#include "j9protos.h"
#include "j9vmnls.h"
#include "ut_j9vm.h"
#include "util_api.h"
#include "vm_api.h"
#include "vm_internal.h"
#include "j9cp.h"
#include "jni.h"
#include "stackwalk.h"
#include "rommeth.h"
#include "j2sever.h"
#include "objhelp.h"
#include "VMHelpers.hpp"
#include "VMAccess.hpp"

extern "C" {

#if JAVA_SPEC_VERSION >= 16
static U_8 getReturnTypeFromMetaData(J9UpcallMetaData *data);
static void icallVMprJavaUpcallImpl(J9UpcallMetaData *data, J9VMThread currentThread, void *argsListPointer);

void
icallVMprJavaUpcall0(J9UpcallMetaData *data, void *argsListPointer)
{
	icallVMprJavaUpcallImpl(data, currentThread, argsListPointer);
}

I_32
icallVMprJavaUpcall1(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread currentThread = vm->internalVMFunctions->currenVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, currentThread, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (I_32)currentThread->returnValue;
}

I_64
icallVMprJavaUpcallJ(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread currentThread = vm->internalVMFunctions->currenVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, currentThread, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (I_64)currentThread->returnValue;
}

float
icallVMprJavaUpcallF(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread currentThread = vm->internalVMFunctions->currenVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, currentThread, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (float)currentThread->returnValue;
}

double
icallVMprJavaUpcallD(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread currentThread = vm->internalVMFunctions->currenVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, currentThread, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (double)currentThread->returnValue;
}

U_8 *
icallVMprJavaUpcallStruct(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread currentThread = vm->internalVMFunctions->currenVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, currentThread, argsListPointer);
	/* returnStorage is not the address of _currentThread->returnValue any more
	 * given it stores the address of struct allocated previously.
	 */
	_currentThread->returnValue = (UDATA)returnStorage;
	return (U_8 *)currentThread->returnValue;
}

static U_8
getReturnTypeFromMetaData(J9UpcallMetaData *data)
{
	J9JavaVM *vm = data->vm;
	J9InternalVMFunctions const *vmFuncs = vm->internalVMFunctions;
	J9VMThread currentThread = vmFuncs->currenVMThread(vm);
	jobject_t mhMetaData = J9_JNI_UNWRAP_REFERENCE(data->mhMetaData);
	j9object_t targetHandle = J9VMCONSTANTPOOL_JDKINTERNALFOREIGNABIUPCALLMHMETADATA_CALLEEMH(currentThread, mhMetaData);
	j9object_t methodType = J9VMJAVALANGINVOKEMETHODHANDLE_TYPE(currentThread, targetHandle);
	j9object_t retType = J9VMJAVALANGINVOKEMETHODTYPE_RTYPE(currentThread, methodType);
	J9Class *retClass = J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, retType);
	J9UpcallNativeSignature *nativeSig = data->nativeFuncSignature;
	J9UpcallSigType *retSigType = nativeSig->sigArray[nativeSig->numSigs - 1]; // The last element is for the return type
	U_8 returnType = 0;

	if (retClass == vm->voidReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_VOID
		returnType = J9NtcVoid;
	} else if (retClass == vm->booleanReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_INT32 (4 bytes)
		returnType = J9NtcBoolean;
	} else if (retClass == vm->byteReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_CHAR (1 byte)
		returnType = J9NtcByte;
	} else if (retClass == vm->charReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_SHORT (2 bytes)
		returnType = J9NtcChar;
	} else if (retClass == vm->shortReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_SHORT (2 bytes)
		returnType = J9NtcShort;
	} else if (retClass == vm->intReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_INT32 (4 bytes)
		returnType = J9NtcInt;
	} else if (retClass == vm->longReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_INT64 (8 bytes)
		returnType = J9NtcLong;
	} else if (retClass == vm->floatReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_FLOAT (4 bytes)
		returnType = J9NtcFloat;
	} else if (retClass == vm->doubleReflectClass) { // With the signature type as J9_FFI_UPCALL_SIG_TYPE_DOUBLE (8 bytes)
		returnType = J9NtcDouble;
	} else if (J9_FFI_UPCALL_SIG_TYPE_POINTER == retSigType->type) {
		returnType = J9NtcPointer;
	} else if (J9_FFI_UPCALL_SIG_TYPE_STRUCT <= retSigType->type) { // Aggregate subtype >= J9_FFI_UPCALL_SIG_TYPE_STRUCT
		returnType = J9NtcStruct;
	} else {
		Assert_VM_unreachable();
	}

	return returnType;
}

static void
icallVMprJavaUpcallImpl(J9UpcallMetaData *data, J9VMThread currentThread, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	j9object_t mhMetaDataObj = J9_JNI_UNWRAP_REFERENCE(data->mhMetaData);
	j9object_t methodHandle = J9VMCONSTANTPOOL_JDKINTERNALFOREIGNABIUPCALLMHMETADATA_CALLEEMH(currentThread, mhMetaDataObj);
	j9object_t methodType = J9VMJAVALANGINVOKEMETHODHANDLE_TYPE(currentThread, methodHandle);
	//UDATA paramSlots = VM_VMHelpers::methodTypeParameterSlotCount(methodType);
	J9VMEntryLocalStorage newELS;

	PORT_ACCESS_FROM_JAVAVM(vm);

	if (buildCallInStackFrame(currentThread, &newELS, true, false)) {
		/*
		for (UDATA i = 0; i < paramSlots; i++) {
			currentThread->sp -= 1;
			*currentThread->sp = (UDATA)*argsListPointer;
			argsListPointer += 1;
		}
		*/

		for (UDATA i = 0; i < paramSlots; i++) {

		}




		currentThread->returnValue = J9_BCLOOP_N2I_TRANSITION;
		currentThread->returnValue2 = (UDATA)mhMetaDataObj;
		c_cInterpreter(currentThread);
		restoreCallInFrame(currentThread);
	}
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

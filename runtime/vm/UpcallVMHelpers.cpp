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

extern void c_cInterpreter(J9VMThread *currentThread);
extern bool buildCallInStackFrameHelper(J9VMThread *currentThread, J9VMEntryLocalStorage *newELS);
extern void restoreCallInFrameHelper(J9VMThread *currentThread);

static U_8 getReturnTypeFromMetaData(J9UpcallMetaData *data);
static void icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer);

/**
 * @brief the function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and ignores the return value for the void return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return void
 */
void
icallVMprJavaUpcall0(J9UpcallMetaData *data, void *argsListPointer)
{
	icallVMprJavaUpcallImpl(data, argsListPointer);
}

/**
 * @brief the function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns an I_32 value for the byte/char/short/int return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return an I_32 value
 */
I_32
icallVMprJavaUpcall1(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (I_32)currentThread->returnValue;
}

/**
 * @brief the function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns an I_64 value for the long/pointer return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return an I_64 value
 */
I_64
icallVMprJavaUpcallJ(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (I_64)currentThread->returnValue;
}

/**
 * @brief the function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns a float value for the float return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a float
 */
float
icallVMprJavaUpcallF(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (float)currentThread->returnValue;
}

/**
 * @brief the function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns a double value for the double return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a double
 */
double
icallVMprJavaUpcallD(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
	return (double)currentThread->returnValue;
}

/**
 * @brief the function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns a U_8 pointer for the struct return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a U_8 pointer
 */
U_8 *
icallVMprJavaUpcallStruct(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	/* returnStorage is not the address of currentThread->returnValue any more
	 * given it stores the address of struct allocated previously.
	 */
	currentThread->returnValue = (UDATA)returnStorage;
	return (U_8 *)currentThread->returnValue;
}

static U_8
getReturnTypeFromMetaData(J9UpcallMetaData *data)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	j9object_t mhMetaData = J9_JNI_UNWRAP_REFERENCE(data->mhMetaData);
	j9object_t targetHandle = J9VMJDKINTERNALFOREIGNABIUPCALLMHMETADATA_CALLEEMH(currentThread, mhMetaData);
	j9object_t methodType = J9VMJAVALANGINVOKEMETHODHANDLE_TYPE(currentThread, targetHandle);
	j9object_t retType = J9VMJAVALANGINVOKEMETHODTYPE_RTYPE(currentThread, methodType);
	J9Class *retClass = J9VM_J9CLASS_FROM_HEAPCLASS(currentThread, retType);
	J9UpcallNativeSignature *nativeSig = data->nativeFuncSignature;
	J9UpcallSigType *sigArray = nativeSig->sigArray;
	U_8 retSigType = sigArray[nativeSig->numSigs - 1].type; // The last element is for the return type
	U_8 returnType = 0;

	if ((retSigType >= J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MIN)
	&& (retSigType <= J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MAX)
	) {
		returnType = J9NtcStruct;
	} else {
		switch (retSigType) {
		case J9_FFI_UPCALL_SIG_TYPE_VOID:
			returnType = J9NtcVoid;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_CHAR:
			returnType = J9NtcByte;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_SHORT:
			returnType = (retClass == vm->charReflectClass) ? J9NtcChar : J9NtcShort;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_INT32:
			returnType = (retClass == vm->booleanReflectClass) ? J9NtcBoolean : J9NtcInt;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_INT64:
			returnType = J9NtcLong;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_FLOAT:
			returnType = J9NtcFloat;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_DOUBLE:
			returnType = J9NtcDouble;
			break;
		case J9_FFI_UPCALL_SIG_TYPE_POINTER:
			returnType = J9NtcPointer;
			break;
		default:
			Assert_VM_unreachable();
			break;
		}
	}

	return returnType;
}

static void
icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9InternalVMFunctions *vmFuncs = vm->internalVMFunctions;
	J9VMThread *currentThread = vmFuncs->currentVMThread(vm);
	j9object_t mhMetaData = J9_JNI_UNWRAP_REFERENCE(data->mhMetaData);
	J9VMEntryLocalStorage newELS;

	if (buildCallInStackFrameHelper(currentThread, &newELS)) {
		UDATA *sp = currentThread->sp;
		J9UpcallNativeSignature *nativeSig = data->nativeFuncSignature;
		J9UpcallSigType *sigArray = nativeSig->sigArray;
		UDATA paramCount = nativeSig->numSigs - 1; // The last element is for the return type

		for (UDATA argIndex = 0; argIndex < paramCount; argIndex++) {
			U_8 argSigType = sigArray[argIndex].type;

			if ((argSigType >= J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MIN)
			&& (argSigType <= J9_FFI_UPCALL_SIG_TYPE_STRUCT_AGGREGATE_MAX)
			) {
				//this will be a subclass of MemorySegment
				//j9object_t memorySegment = allocJavaRepresentationOfAggregate(paramSig, sizeOfType);

				// assign memorySegment to data -- essentialy memorySegment will be assigned the value of argListPointer
				//assignMemorySegmentToData(memorySegment, argListPointer, sizeOfType);

				j9object_t memorySegment = NULL;
				*(j9object_t*)sp = memorySegment; //place memorySegment on the stack
				sp -= 1;
			} else {
				switch (argSigType) {
				case J9_FFI_UPCALL_SIG_TYPE_CHAR:
				case J9_FFI_UPCALL_SIG_TYPE_SHORT:
				case J9_FFI_UPCALL_SIG_TYPE_INT32:
				case J9_FFI_UPCALL_SIG_TYPE_FLOAT:
					*(U_32*)sp = *(U_32*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
					sp -= 1;
					break;
				case J9_FFI_UPCALL_SIG_TYPE_INT64:
				case J9_FFI_UPCALL_SIG_TYPE_DOUBLE:
				case J9_FFI_UPCALL_SIG_TYPE_POINTER:
					*(U_64*)sp = *(U_64*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
					sp -= 2;
					break;
				default:
					Assert_VM_unreachable();
					break;
				}
			}
		}

		currentThread->returnValue = J9_BCLOOP_N2I_TRANSITION;
		currentThread->returnValue2 = (UDATA)mhMetaData;
		c_cInterpreter(currentThread);
		restoreCallInFrameHelper(currentThread);
	}
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

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
#include "VMHelpers.hpp"
#include "VMAccess.hpp"
#include "ObjectAllocationAPI.hpp"
#include "AtomicSupport.hpp"

extern "C" {

#if JAVA_SPEC_VERSION >= 16

extern void c_cInterpreter(J9VMThread *currentThread);
extern bool buildCallInStackFrameHelper(J9VMThread *currentThread, J9VMEntryLocalStorage *newELS);
extern void restoreCallInFrameHelper(J9VMThread *currentThread);

static void JNICALL icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer);
static j9object_t createMemAddressObject(J9UpcallMetaData *data, I_64 offset);
static long getNativePtrFromMemAddressObject(J9UpcallMetaData *data, j9object_t memAddrObject);

/**
 * @brief The function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and ignores the return value for the void return value.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return void
 */
void JNICALL
icallVMprJavaUpcall0(J9UpcallMetaData *data, void *argsListPointer)
{
	icallVMprJavaUpcallImpl(data, argsListPointer);
}

/**
 * @brief The function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns an I_32 value for the byte/char/short/int return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return an I_32 value
 */
I_32 JNICALL
icallVMprJavaUpcall1(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	return (I_32)currentThread->returnValue;
}

/**
 * @brief The function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns an I_64 value for the long/pointer return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return an I_64 value
 */
I_64 JNICALL
icallVMprJavaUpcallJ(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	return (I_64)currentThread->returnValue;
}

/**
 * @brief The function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns a float value for the float return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a float
 */
float JNICALL
icallVMprJavaUpcallF(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	J9FloatPatternInfo floatPatternInfo;
	icallVMprJavaUpcallImpl(data, argsListPointer);
	/* The value returned from the upcall method is literally the single precision (32bit) IEEE 754 floating-point
	 * representation which must be converted to a real float value before returning back to the native
	 * function in the downcall.
	 */
	floatPatternInfo.intValue = (I_32)currentThread->returnValue;
	return floatPatternInfo.floatValue;
}

/**
 * @brief The function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns a double value for the double return type.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a double
 */
double JNICALL
icallVMprJavaUpcallD(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	J9DoublePatternInfo doublePatternInfo;
	icallVMprJavaUpcallImpl(data, argsListPointer);
	/* The value returned from the upcall method is literally the double precision (64bit) IEEE 754 floating-point
	 * representation which must be converted to a real double value before returning back to the native
	 * function in the downcall.
	 */
	doublePatternInfo.longIntValue = (I_64)currentThread->returnValue;
	return doublePatternInfo.doubleValue;
}

/**
 * @brief The function calls into the interpreter to the requested java method in the upcall by invoking
 * icallVMprJavaUpcallImpl() and returns a U_8 pointer to the struct return value.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a U_8 pointer
 */
U_8 * JNICALL
icallVMprJavaUpcallStruct(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	icallVMprJavaUpcallImpl(data, argsListPointer);
	return (U_8 *)currentThread->returnValue;
}

/**
 * @brief Determine the predefined return type against the return signature type
 * stored in the native signature array of the upcall metadata.
 *
 * @param data the pointer to J9UpcallMetaData
 * @return a U_8 value for the return type
 */
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
	U_8 retSigType = sigArray[nativeSig->numSigs - 1].type & J9_FFI_UPCALL_SIG_TYPE_MASK; // The last element is for the return type
	U_8 returnType = 0;

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
	case J9_FFI_UPCALL_SIG_TYPE_STRUCT:
		returnType = J9NtcStruct;
		break;
	default:
		Assert_VM_unreachable();
		break;
	}

	return returnType;
}

/**
 * @brief The common helper function calls into the interpreter to the requested java method
 * in the upcall by invoking the OpenJDK MH.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return void
 */
static void JNICALL
icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	J9InternalVMFunctions *vmFuncs = vm->internalVMFunctions;
	J9VMThread *currentThread = vmFuncs->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue); // Store return value from the requested java method
	U_8 returnType = getReturnTypeFromMetaData(data);
	J9VMEntryLocalStorage newELS;

	VM_VMAccess::inlineEnterVMFromJNI(currentThread);
	if (buildCallInStackFrameHelper(currentThread, &newELS)) {
		J9UpcallNativeSignature *nativeSig = data->nativeFuncSignature;
		J9UpcallSigType *sigArray = nativeSig->sigArray;
		UDATA paramCount = nativeSig->numSigs - 1; // The last element is for the return type
		j9object_t mhMetaData = J9_JNI_UNWRAP_REFERENCE(data->mhMetaData);
		j9object_t calleeHandle = J9VMJDKINTERNALFOREIGNABIUPCALLMHMETADATA_CALLEEMH(currentThread, mhMetaData);

		/* The arguments list of the upcall method handle on the java stack consist of
		 * 1) the target method handle
		 * 2) the method arguments
		 * 3) the appendix (which is set via MethodHandleResolver.linkCallerMethod())
		 */
		*(j9object_t*)--currentThread->sp = calleeHandle;

		for (UDATA argIndex = 0; argIndex < paramCount; argIndex++) {
			U_8 argSigType = sigArray[argIndex].type & J9_FFI_UPCALL_SIG_TYPE_MASK;

			switch (argSigType) {
			case J9_FFI_UPCALL_SIG_TYPE_CHAR:
			case J9_FFI_UPCALL_SIG_TYPE_SHORT:
			case J9_FFI_UPCALL_SIG_TYPE_INT32:
			case J9_FFI_UPCALL_SIG_TYPE_FLOAT:
				*(I_32*)--currentThread->sp = *(I_32*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
				break;
			case J9_FFI_UPCALL_SIG_TYPE_POINTER:
			{
				I_64 offset = *(I_64*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
				j9object_t memoryAddrObject = createMemAddressObject(data, offset);
				*(j9object_t*)--currentThread->sp = memoryAddrObject;
				break;
			}
			case J9_FFI_UPCALL_SIG_TYPE_INT64:
			case J9_FFI_UPCALL_SIG_TYPE_DOUBLE:
				currentThread->sp -= 2;
				*(I_64*)currentThread->sp = *(I_64*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
				break;
			case J9_FFI_UPCALL_SIG_TYPE_STRUCT:
			{
				//this will be a subclass of MemorySegment
				//j9object_t memorySegment = allocJavaRepresentationOfAggregate(paramSig, sizeOfType);

				// assign memorySegment to data -- essentialy memorySegment will be assigned the value of argListPointer
				//assignMemorySegmentToData(memorySegment, argListPointer, sizeOfType);

				j9object_t memorySegment = NULL;
				*(j9object_t*)--currentThread->sp = memorySegment; //place memorySegment on the stack
				break;
			}
			default:
				Assert_VM_unreachable();
				break;
			}
		}

		/* Pass mhMetaData as argument to native2InterpreterTransition() in interpreter
		 * as we need to set the invoke cache array (MemberName and appendix) before
		 * invoking the target handle.
		 */
		currentThread->returnValue = J9_BCLOOP_N2I_TRANSITION;
		currentThread->returnValue2 = (UDATA)data;
		c_cInterpreter(currentThread);
		restoreCallInFrameHelper(currentThread);
	}
	VM_VMAccess::inlineExitVMToJNI(currentThread);

	switch (returnType) {
	case J9NtcPointer:
	{
		j9object_t memoryAddrObject = (j9object_t)currentThread->returnValue;
		currentThread->returnValue = (UDATA)getNativePtrFromMemAddressObject(data, memoryAddrObject);
		break;
	}
	case J9NtcStruct:
		/* returnStorage is no longer the address of currentThread->returnValue
		 * given it stores the address of struct allocated at Java level.
		 */
		currentThread->returnValue = (UDATA)returnStorage;
		break;
	default:
		/* The returned value conversion is only intended for a few primitives returned from the upcall method
		 * given the code is shared by downcall and upcall, in which case part of conversion in
		 * upcall is entirely distinct from downcall.
		 * Note: the conversions on float and double are ignored here as they will be further
		 * converted in icallVMprJavaUpcallF() and icallVMprJavaUpcallD().
		 */
		VM_VMHelpers::convertJNIReturnValue(returnType, returnStorage);
		break;
	}
}

/**
 * @brief Wrap up a MemoryAddress object with the specified offset in the native memory.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param offset the native address to the value in the memory
 * @return a j9object_t value for MemoryAddress
 */
static j9object_t
createMemAddressObject(J9UpcallMetaData *data, I_64 offset)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	J9Class *memAddrClass = J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL(vm);
	MM_ObjectAllocationAPI objectAllocate(currentThread);
	j9object_t memoryAddrObject = NULL;

	Assert_VM_true(NULL != memAddrClass);

	/* To wrap up a MemoryAddress Object as an argument on the java stack, an object is directly allocated
	 * on the heap with the passed-in native address(offset) set to this object.
	 */
	memoryAddrObject = objectAllocate.inlineAllocateObject(currentThread, memAddrClass, true, false);
	if (NULL == memoryAddrObject) {
		memoryAddrObject = vm->memoryManagerFunctions->J9AllocateObject(currentThread, memAddrClass, J9_GC_ALLOCATE_OBJECT_NON_INSTRUMENTABLE);
		if (J9_UNEXPECTED(NULL == memoryAddrObject)) {
			setHeapOutOfMemoryError(currentThread);
			goto done;
		}
	}
	VM_AtomicSupport::writeBarrier();
	J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_SET_OFFSET(currentThread, memoryAddrObject, offset);
done:
	return memoryAddrObject;
}

/**
 * @brief Get the native pointer to the value from the genereated MemoryAddress object.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param memoryAddrObject a MemoryAddress object
 * @return the pointer value (native address) in the memory
 */
static long getNativePtrFromMemAddressObject(J9UpcallMetaData *data, j9object_t memAddrObject)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	j9object_t segmtObject = J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_SEGMENT(currentThread, memAddrObject);

	 /* The memory address is generated by MemorySegment.address() that invokes MemoryAddressImpl(this segment, 0L)
	  * in which the passed-in offset is zero and the actual native pointer of the memory segment is stored in
	  * NativeMemorySegmentImpl.min.
	  */
	long nativePtrValue = J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_MIN(currentThread, segmtObject);

	return nativePtrValue;
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

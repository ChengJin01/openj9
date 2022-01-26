/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corp. and others
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

#include "j9.h"
#include "j9protos.h"
#include "j9vmnls.h"
#include "ut_j9vm.h"
#include "vm_internal.h"
#include "VMAccess.hpp"

extern "C" {

#if JAVA_SPEC_VERSION >= 16

extern void c_cInterpreter(J9VMThread *currentThread);
extern bool buildCallInStackFrameHelper(J9VMThread *currentThread, J9VMEntryLocalStorage *newELS);
extern void restoreCallInFrameHelper(J9VMThread *currentThread);

static void JNICALL icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer);
static void convertUpcallReturnValue(J9UpcallMetaData *data, U_8 returnType, UDATA *returnStorage);
static long getNativeAddrFromMemAddressObject(J9UpcallMetaData *data, j9object_t memAddrObject);
static long getNativeAddrFromMemSegmentObject(J9UpcallMetaData *data, j9object_t memAddrObject);

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
 * icallVMprJavaUpcallImpl() and returns an I_32 value for the boolean/byte/char/short/int return type.
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
 * icallVMprJavaUpcallImpl() and returns a U_8 pointer to the returned struct.
 *
 * @param data the pointer to J9UpcallMetaData
 * @param argsListPointer the pointer to the argument list
 * @return a U_8 pointer to the returned struct
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
	/* The last element is for the return type */
	U_8 retSigType = sigArray[nativeSig->numSigs - 1].type & J9_FFI_UPCALL_SIG_TYPE_MASK;
	U_8 returnType = 0;

	switch (retSigType) {
	case J9_FFI_UPCALL_SIG_TYPE_VOID:
		returnType = J9NtcVoid;
		break;
	case J9_FFI_UPCALL_SIG_TYPE_CHAR:
		returnType = (retClass == vm->booleanReflectClass) ? J9NtcBoolean : J9NtcByte;
		break;
	case J9_FFI_UPCALL_SIG_TYPE_SHORT:
		returnType = (retClass == vm->charReflectClass) ? J9NtcChar : J9NtcShort;
		break;
	case J9_FFI_UPCALL_SIG_TYPE_INT32:
		returnType = J9NtcInt;
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
 * @param data The pointer to J9UpcallMetaData
 * @param argsListPointer The pointer to the argument list
 * @return void
 */
static void JNICALL
icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	const J9InternalVMFunctions *vmFuncs = vm->internalVMFunctions;
	J9VMThread *currentThread = vmFuncs->currentVMThread(vm);
	UDATA *returnStorage = &(currentThread->returnValue);
	U_8 returnType = getReturnTypeFromMetaData(data);
	J9UpcallNativeSignature *nativeSig = data->nativeFuncSignature;
	J9UpcallSigType *sigArray = nativeSig->sigArray;
	UDATA lastParamIdx = nativeSig->numSigs - 1; /* The last element is for the return type */
	J9VMEntryLocalStorage newELS = {0};
	data->argsListPtr = argsListPointer;
	*returnStorage = 0;

	VM_VMAccess::inlineEnterVMFromJNI(currentThread);
	if (buildCallInStackFrameHelper(currentThread, &newELS)) {
		/* Call into the interpreter at native2InterpreterTransition() with the upcall specific data
		 * as argument so as to set the invoke cache array (MemberName and appendix) before invoking
		 * the target method handle during the upcall.
		 *
		 * Note: the setting the arguments of the target method handle on the stack is delayed to
		 * native2InterpreterTransition() prior to the handle's invocation so as to prevent GC from
		 * updating the reference to the upcall metadata in java; otherwise, part of argument objects
		 * (e.g. the reference to the target method handle) will become obsolete.
		 */
		currentThread->returnValue = J9_BCLOOP_N2I_TRANSITION;
		currentThread->returnValue2 = (UDATA)data;
		c_cInterpreter(currentThread);
		restoreCallInFrameHelper(currentThread);
	}
	VM_VMAccess::inlineExitVMToJNI(currentThread);

	printf("\nicallVMprJavaUpcallImpl_0: *returnStorage = 0x%lx, returnTypeByteSize = %d", *returnStorage, sigArray[lastParamIdx].sizeInByte);
	convertUpcallReturnValue(data, returnType, returnStorage);
	printf("\nicallVMprJavaUpcallImpl_1: *returnStorage = 0x%lx\n", *returnStorage);
}

/**
 * @brief Converts the type of the return value to the return type intended for JEP389/419 upcall
 *
 * @param data The pointer to J9UpcallMetaData
 * @param returnType[in] The type of the return value
 * @param returnStorage[in] The pointer to the return value
 */
static void
convertUpcallReturnValue(J9UpcallMetaData *data, U_8 returnType, UDATA *returnStorage)
{
	switch (returnType) {
	case J9NtcBoolean: /* Fall through */
	case J9NtcByte:    /* Fall through */
	case J9NtcChar:    /* Fall through */
	case J9NtcShort:   /* Fall through */
	case J9NtcInt:     /* Fall through */
	case J9NtcFloat:
	{
#if !defined(J9VM_ENV_LITTLE_ENDIAN)
		/* Right shift the returned value from the upcall method by 4 bytes(32 bits) for the signature type
		 * less than or equal to 4 bytes in size given the actual value is placed on the higher 4 bytes
		 * on the Big-Endian(BE) platforms.
		 */
		*returnStorage = *returnStorage >> J9_FFI_UPCALL_SIG_TYPE_32_BIT;
#endif /* J9VM_ENV_LITTLE_ENDIAN */
		break;
	}
	case J9NtcPointer:
	{
		j9object_t memAddrObject = (j9object_t)*returnStorage;
		*returnStorage = (UDATA)getNativeAddrFromMemAddressObject(data, memAddrObject);
		break;
	}
	case J9NtcStruct:
	{
		j9object_t memSegmtObject = (j9object_t)*returnStorage;
		*returnStorage = (UDATA)getNativeAddrFromMemSegmentObject(data, memSegmtObject);
		break;
	}
	default:
		break;
	}
}

/**
 * @brief Get the native address to the requested value from a MemoryAddress object.
 *
 * @param data The pointer to J9UpcallMetaData
 * @param memAddrObject The specified MemoryAddress object
 * @return the native address to the value in the memory
 *
 * Note:
 * There are two cases for the calculation of the native memory address (offset) as follows:
 * 1) if the offset is generated via createMemAddressObject() in native and passed over into java,
 *    then the offset is the requested native address value;
 * 2) MemorySegment.address() is invoked upon return in java, which means:
 * Java 17: address = segment.min() as specified in MemoryAddressImpl (offset is set to zero)
 * Java 18: address = offset which is indirectly set by segment.min() via NativeMemorySegmentImpl.address()
 */
static long getNativeAddrFromMemAddressObject(J9UpcallMetaData *data, j9object_t memAddrObject)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	long offset = J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_OFFSET(currentThread, memAddrObject);
	long nativePtrValue = offset;
#if JAVA_SPEC_VERSION <= 17
	j9object_t segmtObject = J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_SEGMENT(currentThread, memAddrObject);
	/* The offset is set to zero in AbstractMemorySegmentImpl.address() in OpenJDK */
	if (NULL != segmtObject) {
		nativePtrValue = J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_MIN(currentThread, segmtObject);
	}
#endif /* JAVA_SPEC_VERSION <= 17 */

	Assert_VM_true(0 != nativePtrValue);
	return nativePtrValue;
}

/**
 * @brief Get the native address to the requested struct from a MemorySegment object.
 *
 * @param data The pointer to J9UpcallMetaData
 * @param memSegmtObject The specified MemorySegment object
 * @return the native address to the requested struct
 */
static long getNativeAddrFromMemSegmentObject(J9UpcallMetaData *data, j9object_t memSegmtObject)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	long nativePtrValue = J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_MIN(currentThread, memSegmtObject);

	Assert_VM_true(0 != nativePtrValue);
	return nativePtrValue;
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

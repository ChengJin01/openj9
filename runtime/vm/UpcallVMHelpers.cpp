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
#include "objhelp.h"
#include "ut_j9vm.h"
#include "vm_internal.h"
#include "AtomicSupport.hpp"
#include "ObjectAllocationAPI.hpp"
#include "VMAccess.hpp"

extern "C" {

#if JAVA_SPEC_VERSION >= 16

extern void c_cInterpreter(J9VMThread *currentThread);
extern bool buildCallInStackFrameHelper(J9VMThread *currentThread, J9VMEntryLocalStorage *newELS, bool returnsObject);
extern void restoreCallInFrameHelper(J9VMThread *currentThread);

static UDATA JNICALL icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer);
static J9VMThread * getCurrentThread(J9UpcallMetaData *data, bool *isAllocated);
static void convertUpcallReturnValue(J9UpcallMetaData *data, U_8 returnType, UDATA *returnStorage);
static j9object_t createMemAddressObject(J9UpcallMetaData *data, I_64 offset, bool isAllocated);
static j9object_t createMemSegmentObject(J9UpcallMetaData *data, I_64 offset, U_32 sigTypeSize, bool isAllocated);
#if JAVA_SPEC_VERSION >= 19
static j9object_t createMemorySessionObject(J9UpcallMetaData *data, bool isAllocated);
#else /* JAVA_SPEC_VERSION >= 19 */
static j9object_t createResourceScopeObject(J9UpcallMetaData *data, bool isAllocated);
#endif /* JAVA_SPEC_VERSION >= 19 */
static I_64 getNativeAddrFromMemAddressObject(J9UpcallMetaData *data, j9object_t memAddrObject);
static I_64 getNativeAddrFromMemSegmentObject(J9UpcallMetaData *data, j9object_t memAddrObject);

/**
 * @brief Call into the interpreter via icallVMprJavaUpcallImpl to invoke the upcall
 * specific method handle and ignore the return value in the case of void.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return void
 */
void JNICALL
icallVMprJavaUpcall0(J9UpcallMetaData *data, void *argsListPointer)
{
	icallVMprJavaUpcallImpl(data, argsListPointer);
}

/**
 * @brief Call into the interpreter via icallVMprJavaUpcallImpl to invoke the upcall
 * specific method handle and return an I_32 value in the case of byte/char/short/int.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return an I_32 value
 */
I_32 JNICALL
icallVMprJavaUpcall1(J9UpcallMetaData *data, void *argsListPointer)
{
	UDATA returnValue = icallVMprJavaUpcallImpl(data, argsListPointer);
	return (I_32)returnValue;
}

/**
 * @brief Call into the interpreter via icallVMprJavaUpcallImpl to invoke the upcall
 * specific method handle and return an I_64 value in the case of long/pointer.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return an I_64 value
 */
I_64 JNICALL
icallVMprJavaUpcallJ(J9UpcallMetaData *data, void *argsListPointer)
{
	UDATA returnValue = icallVMprJavaUpcallImpl(data, argsListPointer);
	return (I_64)returnValue;
}

/**
 * @brief Call into the interpreter via icallVMprJavaUpcallImpl to invoke the upcall
 * specific method handle and return a float value as specified in the return type.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return a float
 */
float JNICALL
icallVMprJavaUpcallF(J9UpcallMetaData *data, void *argsListPointer)
{
	J9FloatPatternInfo floatPatternInfo;
	UDATA returnValue = icallVMprJavaUpcallImpl(data, argsListPointer);
	/* The value returned from the upcall method is literally the single precision (32bit) IEEE 754 floating-point
	 * representation which must be converted to a real float value before returning back to the native
	 * function in the downcall.
	 */
	floatPatternInfo.intValue = (I_32)returnValue;
	return floatPatternInfo.floatValue;
}

/**
 * @brief Call into the interpreter via icallVMprJavaUpcallImpl to invoke the upcall
 * specific method handle and return a double value as specified in the return type.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return a double
 */
double JNICALL
icallVMprJavaUpcallD(J9UpcallMetaData *data, void *argsListPointer)
{
	J9DoublePatternInfo doublePatternInfo;
	UDATA returnValue = icallVMprJavaUpcallImpl(data, argsListPointer);
	/* The value returned from the upcall method is literally the double precision (64bit) IEEE 754 floating-point
	 * representation which must be converted to a real double value before returning back to the native
	 * function in the downcall.
	 */
	doublePatternInfo.longIntValue = (I_64)returnValue;
	return doublePatternInfo.doubleValue;
}

/**
 * @brief Call into the interpreter via icallVMprJavaUpcallImpl to invoke the upcall
 * specific method handle and return a U_8 pointer to the requested struct.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return a U_8 pointer
 */
U_8 * JNICALL
icallVMprJavaUpcallStruct(J9UpcallMetaData *data, void *argsListPointer)
{
	UDATA returnValue = icallVMprJavaUpcallImpl(data, argsListPointer);
	return (U_8 *)returnValue;
}

/**
 * @brief Determine the predefined return type against the return signature type
 * stored in the native signature array of the upcall metadata.
 *
 * @param data a pointer to J9UpcallMetaData
 * @return a U_8 value for the return type
 */
static U_8
getReturnTypeFromMetaData(J9UpcallMetaData *data)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	j9object_t methodType = J9VMOPENJ9INTERNALFOREIGNABIUPCALLMHMETADATA_CALLEETYPE(currentThread,
			J9_JNI_UNWRAP_REFERENCE(data->mhMetaData));
	J9Class *retClass = J9VM_J9CLASS_FROM_HEAPCLASS(currentThread,
			J9VMJAVALANGINVOKEMETHODTYPE_RTYPE(currentThread, methodType));
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
 * @brief The common helper function that calls into the interpreter
 * via icallVMprJavaUpcallImpl to invoke the OpenJDK MH in the upcall
 *
 * @param data a pointer to J9UpcallMetaData
 * @param argsListPointer a pointer to the argument list
 * @return the expected value against the specified return type
 */
static UDATA JNICALL
icallVMprJavaUpcallImpl(J9UpcallMetaData *data, void *argsListPointer)
{
	J9JavaVM *vm = data->vm;
	const J9InternalVMFunctions *vmFuncs = vm->internalVMFunctions;
	J9UpcallNativeSignature *nativeSig = data->nativeFuncSignature;
	J9UpcallSigType *sigArray = nativeSig->sigArray;
	I_32 paramCount = (I_32)(nativeSig->numSigs - 1); /* The last element is for the return type */
	U_8 returnType = 0;
	bool returnsObject = false;
	J9VMEntryLocalStorage newELS = {0};
	J9VMThread *currentThread = NULL;
	bool isAllocated = false;
	bool throwOOM = false;
	J9Method* thrLiterals = NULL;
	UDATA returnStorage = 0;

	/* Determine whether to use the current thread or create a new one
	 * when there is no java thread attached to the native thread
	 * created directly in native.
	 */
	currentThread = getCurrentThread(data, &isAllocated);
	if (NULL == currentThread) {
		/* The OOM exception set in getCurrentThread() will be thrown in the interpreter
		 * after returning from the native function in downcall.
		 */
		goto doneAndExit;
	}

	returnType = getReturnTypeFromMetaData(data);
	returnsObject = ((J9NtcPointer == returnType) || (J9NtcStruct == returnType)) ? true : false;

	VM_VMAccess::inlineEnterVMFromJNI(currentThread);
	if (buildCallInStackFrameHelper(currentThread, &newELS, returnsObject)) {

		/* Save the current executing method and restore later after the memory allocation
		 * for pointer/struct in the argument list.
		 */
		thrLiterals = currentThread->literals;

		/* The argument list of the upcall method handle on the stack includes the target method handle,
		 * the method arguments and the appendix which is set via MethodHandleResolver.upcallLinkCallerMethod().
		 *
		 * Note: push the target method handle on the special frame so as to avoid updating the address
		 * on the stack by GC when allocating memory for the next pointer/struct of the argument list.
		 */
		j9object_t calleeHandle = J9VMOPENJ9INTERNALFOREIGNABIUPCALLMHMETADATA_CALLEEMH(currentThread, J9_JNI_UNWRAP_REFERENCE(data->mhMetaData));
		PUSH_OBJECT_IN_SPECIAL_FRAME(currentThread, calleeHandle);

		for (I_32 argIndex = 0; argIndex < paramCount; argIndex++) {
			U_8 argSigType = sigArray[argIndex].type & J9_FFI_UPCALL_SIG_TYPE_MASK;

			switch (argSigType) {
			case J9_FFI_UPCALL_SIG_TYPE_CHAR:  /* Fall through */
			case J9_FFI_UPCALL_SIG_TYPE_SHORT: /* Fall through */
			case J9_FFI_UPCALL_SIG_TYPE_INT32: /* Fall through */
			case J9_FFI_UPCALL_SIG_TYPE_FLOAT:
			{
				/* Convert the argument value to 64 bits prior to the 32-bit conversion to get the actual value
				 * in the case of boolean/byte/char/short/int regardless of the endianness on platforms.
				 */
				I_64 argValue = *(I_64*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
#if !defined(J9VM_ENV_LITTLE_ENDIAN)
				/* Right shift the 64-bit float argument by 4 bytes(32 bits) given the actual value
				 * is placed on the higher 4 bytes on the Big-Endian(BE) platforms.
				 */
				if (J9_FFI_UPCALL_SIG_TYPE_FLOAT == argSigType) {
					argValue = argValue >> J9_FFI_UPCALL_SIG_TYPE_32_BIT;
				}
#endif /* J9VM_ENV_LITTLE_ENDIAN */
				*(I_32*)--currentThread->sp = (I_32)argValue;
				break;
			}
			case J9_FFI_UPCALL_SIG_TYPE_INT64: /* Fall through */
			case J9_FFI_UPCALL_SIG_TYPE_DOUBLE:
				currentThread->sp -= 2;
				*(I_64*)currentThread->sp = *(I_64*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
				break;
			case J9_FFI_UPCALL_SIG_TYPE_POINTER:
			{
				I_64 offset = *(I_64*)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
				j9object_t memAddrObject = createMemAddressObject(data, offset, isAllocated);
				if (NULL == memAddrObject) {
					/* The OOM exception set in createMemAddressObject() will be thrown in the interpreter
					 * after returning from the native function in downcall.
					 */
					throwOOM = true;
					goto done;
				}
				 /* Push the object on the special frame so as to avoid updating the address on the stack
				  * by GC when allocating memory for the next pointer/struct of the argument list.
				  */
				PUSH_OBJECT_IN_SPECIAL_FRAME(currentThread, memAddrObject);
				break;
			}
			case J9_FFI_UPCALL_SIG_TYPE_STRUCT:
			{
				I_64 offset = (I_64)(intptr_t)vmFuncs->getArgPointer(nativeSig, argsListPointer, argIndex);
				j9object_t memSegmtObject = createMemSegmentObject(data, offset, sigArray[argIndex].sizeInByte, isAllocated);
				if (NULL == memSegmtObject) {
					/* The OOM exception set in createMemSegmentObject() will be thrown in the interpreter
					 * after returning from the native function in downcall.
					 */
					throwOOM = true;
					goto done;
				}
				 /* Push the object on the special frame so as to avoid updating the address on the stack
				  * by GC when allocating memory for the next pointer/struct of the argument list.
				  */
				PUSH_OBJECT_IN_SPECIAL_FRAME(currentThread, memSegmtObject);
				break;
			}
			default:
				Assert_VM_unreachable();
				break;
			}
		}
		/* Only restore the literals and keep the unchanged arguments on the stack which
		 * are passed over to native2InterpreterTransition() in the interpreter so as to
		 * invoke the upcall method handle.
		 */
		currentThread->literals = thrLiterals;

		/* Place mhMetaData as the return value to native2InterpreterTransition() when calling into
		 * the interpreter so as to set the invoke cache array (MemberName and appendix)
		 * before invoking the target handle in upcall.
		 */
		currentThread->returnValue = J9_BCLOOP_N2I_TRANSITION;
		currentThread->returnValue2 = (UDATA)data;
		c_cInterpreter(currentThread);

done:
		restoreCallInFrameHelper(currentThread);
	}
	VM_VMAccess::inlineExitVMToJNI(currentThread);

	if (!throwOOM) {
		returnStorage = currentThread->returnValue;
		convertUpcallReturnValue(data, returnType, &returnStorage);
	}

	/* Release the J9VMThread if allocated locally for the native thread */
	if (isAllocated) {
		threadCleanup(currentThread, false);
		currentThread = NULL;
	}

doneAndExit:
	return returnStorage;
}

/**
 * @brief Get a J9VMThread whether it is the current thread or a newly created thread if doesn't exist
 *
 * Note:
 * The function is to handle the situation when there is no Java thread for the current native thread
 * which is directly created in native code without a Java thread attached to it.
 *
 * @param vm a pointer to J9UpcallMetaData
 * @param isAllocated a pointer to a flag indicating
 *                    whether the current thread is allocated locally
 * @return a pointer to J9VMThread
 */
static J9VMThread *
getCurrentThread(J9UpcallMetaData *data, bool *isAllocated)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	omrthread_t osThread = NULL;

	if (NULL == currentThread) {
		if (omrthread_attach_ex(&osThread, J9THREAD_ATTR_DEFAULT)) {
			goto done;
		}
		Assert_VM_true(NULL != osThread);

		if (NULL == (currentThread = allocateVMThread(vm, osThread,
				J9_PRIVATE_FLAGS_ATTACHED_THREAD, vm->defaultMemorySpace, NULL))) {
			goto done;
		}
		currentThread->gpProtected = TRUE;
		*isAllocated = true;

		/* Determine the thread's remaining OS stack */
		initializeCurrentOSStackFree(currentThread, osThread, vm->defaultOSStackSize);

		threadAboutToStart(currentThread);
	}

done:
	return currentThread;
}

/**
 * @brief Converts the type of the return value to the return type intended for JEP389/419 upcall
 *
 * @param data a pointer to J9UpcallMetaData
 * @param returnType the type for the return value
 * @param returnStorage a pointer to the return value
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
	default: /* J9NtcVoid */
		break;
	}
}

/**
 * @brief Generate an object of the MemoryAddress's subclass on the heap
 * with the specified native address to the value.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param offset the native address to the value
 * @param isAllocated a flag indicating whether the current thread is allocated locally
 * @return a MemoryAddress object
 */
static j9object_t
createMemAddressObject(J9UpcallMetaData *data, I_64 offset, bool isAllocated)
{
	J9JavaVM * vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	MM_ObjectAllocationAPI objectAllocate(currentThread);
	J9Class *memAddrClass = J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL(vm);
	j9object_t memAddrObject = NULL;

	/* To wrap up an object of the MemoryAddress's subclass as an argument on the java stack,
	 * this object is directly allocated on the heap with the passed-in native address(offset)
	 * set to this object.
	 */
	memAddrObject = objectAllocate.inlineAllocateObject(currentThread, memAddrClass, true, false);
	if (NULL == memAddrObject) {
		memAddrObject = vm->memoryManagerFunctions->J9AllocateObject(currentThread, memAddrClass, J9_GC_ALLOCATE_OBJECT_NON_INSTRUMENTABLE);
		if ((NULL == memAddrObject) && (!isAllocated)) {
			setHeapOutOfMemoryError(currentThread);
			goto done;
		}
	}

#if JAVA_SPEC_VERSION <= 17
	J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_SET_SEGMENT(currentThread, memAddrObject, NULL);
#endif /* JAVA_SPEC_VERSION <= 17 */
	J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_SET_OFFSET(currentThread, memAddrObject, offset);

done:
	return memAddrObject;
}

/**
 * @brief Generate an object of the MemorySegment's subclass on the heap with the specified
 * native address to the requested struct.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param offset the native address to the requested struct
 * @param sigTypeSize the byte size of the requested struct
 * @param isAllocated a flag indicating whether the current thread is allocated locally
 * @return a MemorySegment object
 */
static j9object_t
createMemSegmentObject(J9UpcallMetaData *data, I_64 offset, U_32 sigTypeSize, bool isAllocated)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	MM_ObjectAllocationAPI objectAllocate(currentThread);
	j9object_t scopeOrSessionObject = NULL;
	j9object_t memSegmtObject = NULL;
	J9Class *memSegmtClass = J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL(vm);

#if JAVA_SPEC_VERSION >= 19
	scopeOrSessionObject = createMemorySessionObject(data, isAllocated);
#else /* JAVA_SPEC_VERSION >= 19 */
	scopeOrSessionObject = createResourceScopeObject(data, isAllocated);
#endif /* JAVA_SPEC_VERSION >= 19 */
	if (NULL == scopeOrSessionObject) {
		/* The OOM exception will be thrown when returning back to the interpreter */
		goto done;
	}

	/* To wrap up an object of the MemorySegment's subclass as an argument on the java stack,
	 * this object is directly allocated on the heap with the passed-in native address(offset)
	 * set to this object.
	 */
	memSegmtObject = objectAllocate.inlineAllocateObject(currentThread, memSegmtClass, true, false);
	if (NULL == memSegmtObject) {
		PUSH_OBJECT_IN_SPECIAL_FRAME(currentThread, scopeOrSessionObject);
		memSegmtObject = vm->memoryManagerFunctions->J9AllocateObject(currentThread, memSegmtClass, J9_GC_ALLOCATE_OBJECT_NON_INSTRUMENTABLE);
		scopeOrSessionObject = POP_OBJECT_IN_SPECIAL_FRAME(currentThread);
		if ((NULL == memSegmtObject) && (!isAllocated)) {
			setHeapOutOfMemoryError(currentThread);
			goto done;
		}
	}

	J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_SET_MIN(currentThread, memSegmtObject, offset);
	J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_SET_LENGTH(currentThread, memSegmtObject, sigTypeSize);
#if JAVA_SPEC_VERSION >= 19
	J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_SET_SESSION(currentThread, memSegmtObject, scopeOrSessionObject);
#else /* JAVA_SPEC_VERSION >= 19 */
	J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_SET_SCOPE(currentThread, memSegmtObject, scopeOrSessionObject);
#endif /* JAVA_SPEC_VERSION >= 19 */

done:
	return memSegmtObject;
}

#if JAVA_SPEC_VERSION >= 19
/**
 * @brief Generate an object of the MemorySession's subclass on the heap with the current thread
 * as the owner thread for the MemorySession object.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param isAllocated a flag indicating whether the current thread is allocated locally
 * @return a MemorySession object
 */
static j9object_t
createMemorySessionObject(J9UpcallMetaData *data, bool isAllocated)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	MM_ObjectAllocationAPI objectAllocate(currentThread);
	j9object_t sessionObject = NULL;
	J9Class *sessionClass = J9VMJDKINTERNALFOREIGNCONFINEDSESSION(vm);

	/* The object of the MemorySession's subclass is set as a field value of the MemorySegment object
	 * created in native, which is validated in OpenJDK before returning the native memory address from
	 * MemorySegment.address() in java.
	 */
	sessionObject = objectAllocate.inlineAllocateObject(currentThread, sessionClass, true, false);
	if (NULL == sessionObject) {
		sessionObject = vm->memoryManagerFunctions->J9AllocateObject(currentThread, sessionClass, J9_GC_ALLOCATE_OBJECT_NON_INSTRUMENTABLE);
		if ((NULL == sessionObject) && (!isAllocated)) {
			setHeapOutOfMemoryError(currentThread);
			goto done;
		}
	}

	J9VMJDKINTERNALFOREIGNCONFINEDSESSION_SET_OWNER(currentThread, sessionObject, currentThread->threadObject);
done:
	return sessionObject;
}
#else /* JAVA_SPEC_VERSION >= 19 */
/**
 * @brief Generate an object of the ResourceScope's subclass on the heap with the current thread
 * as the owner thread for the ResourceScope object.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param isAllocated a flag indicating whether the current thread is allocated locally
 * @return a ResourceScope object
 */
static j9object_t
createResourceScopeObject(J9UpcallMetaData *data, bool isAllocated)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	MM_ObjectAllocationAPI objectAllocate(currentThread);
	j9object_t scopeObject = NULL;
	J9Class *scopeClass = J9VMJDKINTERNALFOREIGNCONFINEDSCOPE(vm);

	/* The object of the ResourceScope's subclass is set as a field value of the MemorySegment object
	 * created in native, which is validated in OpenJDK before returning the native memory address from
	 * MemorySegment.address() in java.
	 */
	scopeObject = objectAllocate.inlineAllocateObject(currentThread, scopeClass, true, false);
	if (NULL == scopeObject) {
		scopeObject = vm->memoryManagerFunctions->J9AllocateObject(currentThread, scopeClass, J9_GC_ALLOCATE_OBJECT_NON_INSTRUMENTABLE);
		if ((NULL == scopeObject) && (!isAllocated)) {
			setHeapOutOfMemoryError(currentThread);
			goto done;
		}
	}

	J9VMJDKINTERNALFOREIGNCONFINEDSCOPE_SET_OWNER(currentThread, scopeObject, currentThread->threadObject);
done:
	return scopeObject;
}
#endif /* JAVA_SPEC_VERSION >= 19 */

/**
 * @brief Get the native address to the requested value from a MemoryAddress object.
 *
 * @param data a pointer to J9UpcallMetaData
 * @param memAddrObject the specified MemoryAddress object
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
static I_64
getNativeAddrFromMemAddressObject(J9UpcallMetaData *data, j9object_t memAddrObject)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	I_64 offset = J9VMJDKINTERNALFOREIGNMEMORYADDRESSIMPL_OFFSET(currentThread, memAddrObject);
	I_64 nativePtrValue = offset;
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
 * @param data a pointer to J9UpcallMetaData
 * @param memSegmtObject the specified MemorySegment object
 * @return the native address to the requested struct
 */
static I_64
getNativeAddrFromMemSegmentObject(J9UpcallMetaData *data, j9object_t memSegmtObject)
{
	J9JavaVM *vm = data->vm;
	J9VMThread *currentThread = vm->internalVMFunctions->currentVMThread(vm);
	I_64 nativePtrValue = J9VMJDKINTERNALFOREIGNNATIVEMEMORYSEGMENTIMPL_MIN(currentThread, memSegmtObject);

	Assert_VM_true(0 != nativePtrValue);
	return nativePtrValue;
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

/*[INCLUDE-IF (JAVA_SPEC_VERSION >= 16) ]*/
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
package jdk.internal.foreign.abi;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.Cleaner;
import java.util.Objects;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.MemoryAddress;
//import java.nicl.*;
//import java.nicl.metadata.NativeType;
//import java.nicl.types.*;
//import java.nicl.types.Pointer;
//import jdk.internal.nicl.types.*;
import java.lang.reflect.Type;
//import com.ibm.oti.vm.VMLangInvokeAccess;

public class ProgrammableInvoker {
	
	private MemorySegment cifNativeThunkMemSgmt;
	private long cifNativeThunkDataRef;
	private static long cifNativeThunkSize;
	private MethodType methodType;
	private FunctionDescriptor funcDesc;
	private long functionAddr;
	
	private native void getCifNativeThunkRefSize();
	private native void initCifNativeThunkData(String[] argLayoutStrings);
	private native void invokeNative(long functionAddr, long calloutThunk, Object returnVal, Object args[]);
	
	ProgrammableInvoker(long functionAddr, MethodType methodType, FunctionDescriptor funcDesc) {
		this.functionAddr = functionAddr;
		this.methodType = methodType;
		this.funcDesc = funcDesc;
	}
	
	public static MethodHandle getBoundMethodHandle(Addressable functionAddr, MethodType methodType, FunctionDescriptor funcDesc) {
		MethodHandle boundHandle = null;

		checkIfPrimitiveType(methodType);
		ProgrammableInvoker nativeInvoker = new ProgrammableInvoker(functionAddr.address().toRawLongValue(), methodType, funcDesc);
		nativeInvoker.generateAdapter(funcDesc);
		try {
			boundHandle = MethodHandles.lookup().bind(nativeInvoker, "runNativeMethod", methodType);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new InternalError(e.toString());
		}

		//perform perumations of the boundHandle for args/ret
		//boundHandle = permuteMH(boundHandle);
		return boundHandle;
	}
	
	private void generateAdapter(FunctionDescriptor funcDesc) {
		funcDesc = Objects.requireNonNull(funcDesc);
		
		/* The size of the ffi_cif reference is fixed on a given platform */
		if (cifNativeThunkSize == 0) {
			getCifNativeThunkRefSize();
		}
		
		if (!funcDesc.equals(this.funcDesc)) {
			if (cifNativeThunkMemSgmt != null) {
				cifNativeThunkMemSgmt.close();
			}
			cifNativeThunkMemSgmt = MemorySegment.allocateNative(cifNativeThunkSize);
			/* Register the memory segment of ffi_cif to ensure it is released implicitly */
			cifNativeThunkMemSgmt = cifNativeThunkMemSgmt.registerCleaner(Cleaner.create());
			cifNativeThunkDataRef = cifNativeThunkMemSgmt.address().toRawLongValue();
			
			initCifNativeThunkData(null);
		}
	}
	
	private static void checkIfPrimitiveType(MethodType newType) {
		/* Throw InternalError if the newType contains non-primitives. */
		Class<?> retType = newType.returnType();
		if (!retType.isPrimitive()) {
			throw new InternalError("newType has non-primitive return type, only primtives are supported");  //$NON-NLS-1$
		}

		Class<?>[] argTypes = newType.parameterArray();
		for (int argIndex = 0; argIndex < argTypes.length; argIndex++) {
			if (!argTypes[argIndex].isPrimitive()) {
				throw new InternalError("newType has non-primitive argument type, only primitives are supported"); //$NON-NLS-1$
			}
		}
	}
	
	Object runNativeMethod(Object returnVal, Object args[]) {
		invokeNative(functionAddr, cifNativeThunkDataRef, returnVal, args);
		
		return null;
		//will be to proces the returnVal depend on the type
		//Object processedReturnVal = processReturnVal(returnVal);
		//return processedReturnVal;
	}
	
	/*
	public static MethodHandle getBoundMethodHandle(Addressable addr, MethodType mt, FunctionDescriptor cDesc) {
		
		System.out.println("OpenJ9_ProgrammableInvoker: caling getBoundMethodHandle..."); //$NON-NLS-1$
		new Exception().printStackTrace();
		MethodHandle mh = null;
		int len = methodType.parameterCount();

		Class<?> rtype = methodType.returnType();
		MethodHandle returnFilter = null;
		boolean isStructReturn = false;
		boolean isPointerReturn = false;
		if (Pointer.class.isAssignableFrom(rtype)) {
			rtype = long.class;
			returnFilter = MethodHandles.insertArguments(getPtrReturn, 0, genericReturnType);
			isPointerReturn = true;
		} else if (Util.isCStruct(rtype)) {
			NativeType nativeType = rtype.getAnnotation(java.nicl.metadata.NativeType.class);
			String retLayoutString = Util.sizeof(rtype) + nativeType.layout();
			addLayoutString(retLayoutString, 0);
			returnFilter = MethodHandles.insertArguments(getStructReturn, 0, NativeLibrary.createLayout(rtype));
			rtype = long.class;
			isStructReturn = true;
		}

		Class<?> ptypes[] = new Class<?>[len];
		MethodHandle filters[] = new MethodHandle[len];
		boolean isFilteredArg = false;
		for (int i=0; i<len; i++) {
			Class<?> ptype = methodType.parameterType(i);
			if (Pointer.class.isAssignableFrom(ptype)) {
				ptypes[i] = long.class;
				filters[i] = getPtrAddr;
				isFilteredArg = true;
			} else if (Util.isCStruct(ptype)) {
				ptypes[i] = long.class;
				filters[i] = getStructAddr;
				isFilteredArg = true;
				NativeType nativeType = ptype.getAnnotation(java.nicl.metadata.NativeType.class);
				String argLayoutString = Util.sizeof(ptype) + nativeType.layout();
				addLayoutString(argLayoutString, i+1);
			} else {
				ptypes[i] = ptype;
				filters[i] = null;
			}
		}

		MethodHandle mh;
		MethodType mt = MethodType.methodType(rtype, ptypes);
		VMLangInvokeAccess access = com.ibm.oti.vm.VM.getVMLangInvokeAccess();
		long nativeAddress = symbol.getAddress().addr(new PointerTokenImpl());
		if (null != returnFilter) {
			mh = MethodHandles.filterReturnValue(access.generateNativeMethodHandle(methodName, mt, nativeAddress, layoutStrings), returnFilter);
		} else {
			mh = access.generateNativeMethodHandle(methodName, mt, nativeAddress, layoutStrings);
		}

		if (isFilteredArg) {
			mh = MethodHandles.filterArguments(mh, 0, filters);
		}
		*/
}

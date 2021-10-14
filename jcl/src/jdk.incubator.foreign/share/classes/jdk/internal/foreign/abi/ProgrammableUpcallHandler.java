/*[INCLUDE-IF JAVA_SPEC_VERSION >= 16]*/
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

import java.util.Optional;
import java.util.OptionalLong;
import java.util.List;
import java.util.HashMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import static java.lang.invoke.MethodType.methodType;
import java.lang.invoke.WrongMethodTypeException;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ValueLayout;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.CLinker.TypeKind;
import static jdk.incubator.foreign.CLinker.TypeKind.*;

/**
 * The counterpart in OpenJDK is replaced with this class that wrap up
 * a upcall handler enabling the native call to the java code at runtime.
 */
public class ProgrammableUpcallHandler implements UpcallHandler {

	private MemoryLayout[] argLayoutArray;
	private MemoryLayout realReturnLayout;
	//private final Addressable functionAddr;
	private final long thunkAddr;
	private UpcallMHMetaData metaData;

	/* The address of the generated native thunk is cached & shared in multiple upcalls/threads */
	private static final HashMap<Integer, Long> cachedHandleHashToThunkAddr = new HashMap<>();

	private static final class PrivateUpcallClassLock {
		PrivateUpcallClassLock() {}
	}
	private static final Object privateUpcallClassLock = new PrivateUpcallClassLock();

	static final Lookup lookup = MethodHandles.lookup();

	/**
	 * The method is ultimately invoked by Clinker on a given platform to generate a thunk
	 * in native for the upcall handler intended for the requested java method at runtime.
	 *
	 * @param target The upcall method handle to the requested java method
	 * @param mt The MethodType of the requested java method
	 * @param cDesc The FunctionDescriptor of the requested java method
	 */
	public ProgrammableUpcallHandler(MethodHandle target, MethodType mt, FunctionDescriptor cDesc) {
		List<MemoryLayout> argLayouts = cDesc.argumentLayouts();
		argLayoutArray = argLayouts.toArray(new MemoryLayout[argLayouts.size()]);
		Optional<MemoryLayout> returnLayout = cDesc.returnLayout();
		realReturnLayout = returnLayout.orElse(null); // Set to null for void

		/* A method handle created by lookup.findvirtual() holds the defining class as the 1st
		 * argument in its method type, which needs to be removed before validating against
		 * the argument layouts.
		 */
		MethodType tempTargetMethodType = mt;
		Class<?>[] paramArray1 = tempTargetMethodType.parameterArray();
		int paramArrayLength = paramArray1.length;
		if (!TypeLayoutCheckHelper.validateArgRetTypeClass(paramArray1[0])) {
			Class<?>[] paramArray2 = new Class<?>[paramArrayLength - 1];
			System.arraycopy(paramArray1, 1, paramArray2, 0, paramArray2.length);
			tempTargetMethodType = methodType(mt.returnType(), paramArray2);
		}
		TypeLayoutCheckHelper.checkIfValidLayoutAndType(tempTargetMethodType, argLayoutArray, realReturnLayout);
		thunkAddr = getUpcallThunkAddr(target);
	}

	/**
	 * Returns the address of the generated thunk at runtime.
	 *
	 * @return the thunk address
	 */
	public long entryPoint() {
		return thunkAddr;
	}

	/* Check whether the thunk address of the requested java method exists in the cache;
	 * otherwise, call the native to request the JIT to generate a upcall thunk for this
	 * java method.
	 */
	private long getUpcallThunkAddr(MethodHandle target) {
		int argLayoutCount = argLayoutArray.length;
		/* The last element of the native signature array is for the return type */
		String[] nativeSignatureStrs = new String[argLayoutCount + 1];
		for (int argIndex = 0; argIndex < argLayoutCount; argIndex++) {
			MemoryLayout argLayout = argLayoutArray[argIndex];
			nativeSignatureStrs[argIndex] = LayoutStrPreprocessor.getSimplifiedLayoutString(argLayout, false);
		}

		/* Set the void layout string intended for the underlying native code
		 * as the corresponding layout doesn't exist in the Spec.
		 * Note: 'V' stands for the void type.
		 */
		if (realReturnLayout == null) {
			nativeSignatureStrs[argLayoutCount] = "0#V"; //$NON-NLS-1$
		} else {
			nativeSignatureStrs[argLayoutCount] = LayoutStrPreprocessor.getSimplifiedLayoutString(realReturnLayout, false);
		}

		synchronized(privateUpcallClassLock) {
			Integer targetHandleHash = Integer.valueOf(target.hashCode());
			Long nativeThunkAddr = cachedHandleHashToThunkAddr.get(targetHandleHash);
			long addr = 0;
			if (nativeThunkAddr != null) {
				addr = nativeThunkAddr.longValue();
			} else {
				metaData = new UpcallMHMetaData(this, target);
				addr = allocateUpcallStub(metaData, nativeSignatureStrs);
				System.out.println("getUpcallThunkAddr: addr = " + Long.toHexString(addr)); //$NON-NLS-1$
				cachedHandleHashToThunkAddr.put(targetHandleHash, Long.valueOf(addr));
			}
			return addr;
		}
	}

	/* This native requests the JIT to generate a upcall thunk of the specified java method
	 * by invoking createUpcallThunk() and returns the requested thunk address.
	 */
	private native long allocateUpcallStub(UpcallMHMetaData mhMetaData, String[] cSignatureStrs);

}

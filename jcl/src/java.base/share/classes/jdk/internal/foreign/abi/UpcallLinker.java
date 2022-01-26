/*[INCLUDE-IF JAVA_SPEC_VERSION >= 19]*/
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
package jdk.internal.foreign.abi;

import java.util.HashMap;
import java.util.List;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

/**
 * The counterpart in OpenJDK is replaced with this class that wrap up
 * an upcall handler enabling the native call to the java code at runtime.
 */
public final class UpcallLinker { // UpcallHandler is removed in Java 18

	private final MemoryLayout[] argLayoutArray;
	private final MemoryLayout realReturnLayout;
	private final long thunkAddr;
	private UpcallMHMetaData metaData;

	/* The generated thunk address are cached & shared in multiple upcalls/threads within the same session */
	private static final HashMap<Integer, Long> cachedUpcallThunkAddr = new HashMap<>();

	private static final class PrivateUpcallClassLock {
		PrivateUpcallClassLock() {}
	}
	private static final Object privateUpcallClassLock = new PrivateUpcallClassLock();

	/* The constructor creates an upcall handler specific to the requested java method
	 * by generating a native thunk in upcall on a given platform.
	 */
	UpcallLinker(MethodHandle target, MethodType mt, FunctionDescriptor cDesc, MemorySession session) {
		List<MemoryLayout> argLayouts = cDesc.argumentLayouts();
		argLayoutArray = argLayouts.toArray(new MemoryLayout[argLayouts.size()]);
		realReturnLayout = cDesc.returnLayout().orElse(null); // Set to null for void
		thunkAddr = getUpcallThunkAddr(target, session);
	}

	/**
	 * Returns the address of the generated thunk at runtime.
	 *
	 * @return the thunk address
	 */
	public long entryPoint() {
		return thunkAddr;
	}

	/**
	 * The method invoked via Clinker generates a native thunk to create
	 * a native symbol that holds an entry point to the native function
	 * intended for the requested java method in upcall.
	 *
	 * @param target The upcall method handle to the requested java method
	 * @param mt The MethodType of the upcall method handle
	 * @param cDesc The FunctionDescriptor of the upcall method handle
	 * @param session The MemorySession of the upcall method handle
	 * @return the native symbol
	 */
	public static MemorySegment make(MethodHandle target, MethodType mt, FunctionDescriptor cDesc, MemorySession session) {
		UpcallLinker upcallHandler = new UpcallLinker(target, mt, cDesc, session);
		return UpcallStubs.makeUpcall(upcallHandler.entryPoint(), session);
	}

	/* Check whether the thunk address of the requested java method exists in the cache;
	 * otherwise, call the native to request the JIT to generate an upcall thunk for this
	 * java method.
	 */
	private long getUpcallThunkAddr(MethodHandle target, MemorySession session) {
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

		long addr = 0;
		synchronized(privateUpcallClassLock) {
			/* The generated thunks are shared across upcalls or threads
			 * only when the target method handles are identical within
			 * the same session given a thunk plus the associated metadata
			 * is released automatically in native via freeUpcallStub()
			 * in OpenJDK when the corresponding session is terminated.
			 */
			String targetHashScopeStr = target.hashCode() + "#" + session.toString(); //$NON-NLS-1$
			Integer upcallThunkAddrKey = Integer.valueOf(targetHashScopeStr.hashCode());
			Long upcallThunkAddr = cachedUpcallThunkAddr.get(upcallThunkAddrKey);
			if (upcallThunkAddr != null) {
				addr = upcallThunkAddr.longValue();
			} else {
				/* The thunk must be created for each upcall handler given the UpcallMHMetaData object uniquely bound to the thunk
				 * is only alive for a memory session specified in java, which means the upcall handler and its UpcallMHMetaData
				 * object will be cleaned up automatically once their session is closed.
				 */
				metaData = new UpcallMHMetaData(target);
				addr = allocateUpcallStub(metaData, nativeSignatureStrs);
				cachedUpcallThunkAddr.put(upcallThunkAddrKey, Long.valueOf(addr));
			}
		}

		return addr;
	}

	/* This native requests the JIT to generate an upcall thunk of the specified java method
	 * by invoking createUpcallThunk() and returns the requested thunk address.
	 */
	private native long allocateUpcallStub(UpcallMHMetaData mhMetaData, String[] cSignatureStrs);

}

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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.UpcallHanderResolver;
import static java.lang.invoke.MethodHandles.privateLookupIn;
import static java.lang.invoke.MethodType.methodType;
import java.lang.invoke.WrongMethodTypeException;

/**
 * The meta data consists of the callee MH and a cache of 2 elements for MH resolution,
 * which are used to generate a upcall handler to the requested java method.
 */
class UpcallMHMetaData {
	/* The upcall handler's class is treated as the caller class in MH resolution.
	 * see sendResolveUpcallInvokeHandle() in callin.cpp for details.
	 */
	ProgrammableUpcallHandler upcallHander;
	private MethodHandle calleeMH;
	private String invokeName;
	/* MemberName and appendix (resolved by MethodHandleResolver.linkCallerMethod()) are stored in this array */
	private Object[] invokeCache;
	/* This is the current thread used by a RescourceScope object which is one of arguments
	 * passed over to a MemorySegment object created in native.
	 */
	private Thread scopeOwnerThread;

	private static final Lookup callerLookup = MethodHandles.lookup();
	private static final MethodHandle MH_UpcallLinkCallerMethod;

	private static synchronized native void resolveUpcallDataFields();

	static {
		/* Resolve the fields (offset in the JCL constant pool of VM) specific to the meta data plus the fields
		 * of MemoryAddressImpl and NativeMemorySegmentImpl given the generated macros from vmconstantpool.xml
		 * depend on their offsets to access the corresponding fields in the process of the upcall.
		 */
		resolveUpcallDataFields();
		try {
			Lookup mhPrivLookupIn = privateLookupIn(UpcallHanderResolver.class, callerLookup);
			MH_UpcallLinkCallerMethod = mhPrivLookupIn.findStatic(UpcallHanderResolver.class, "upcallLinkCallerMethod", methodType(Class.class, int.class, Class.class, String.class, String.class)); //$NON-NLS-1$
		} catch (IllegalAccessException | NoSuchMethodException e) {
			throw new InternalError(e);
		}
	}

	UpcallMHMetaData(ProgrammableUpcallHandler upcallHander, MethodHandle calleeMH) {
		this.upcallHander = upcallHander;
		this.calleeMH = calleeMH;
		invokeName = "invokeExact"; //$NON-NLS-1$
		scopeOwnerThread = Thread.currentThread();
		/* Invoke toMethodDescriptorString() to cache the method descriptor which will
		 * be used in the MH resolution.
		 * See resolveUpcallInvokeHandle() in resolvesupport.cpp for details.
		 */
		try {
			String descString = calleeMH.type().toMethodDescriptorString();
			invokeCache = (Object[]) MH_UpcallLinkCallerMethod.invokeExact(upcallHander.getClass(),
					MethodHandleInfo.REF_invokeVirtual, MethodHandle.class, invokeName, descString);
			System.out.println("invokeCache[0] = " + invokeCache[0]); //$NON-NLS-1$
			System.out.println("invokeCache[1] = " + invokeCache[1]); //$NON-NLS-1$
		} catch (Throwable e) {
			throw new InternalError(e);
		}
	}
}

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

	private static synchronized native void resolveUpcallDataFields();

	static {
		/* Resolve the fields (offset in the JCL constant pool of VM) specific to the meta data plus the fields
		 * of MemoryAddressImpl and NativeMemorySegmentImpl given the generated macros from vmconstantpool.xml
		 * depend on their offsets to access the corresponding fields in the process of the upcall.
		 */
		resolveUpcallDataFields();
	}

	UpcallMHMetaData(ProgrammableUpcallHandler upcallHander, MethodHandle calleeMH) {
		this.upcallHander = upcallHander;
		this.calleeMH = calleeMH;
		invokeName = "invokeExact"; //$NON-NLS-1$
		/* Invoke toMethodDescriptorString() to cache the method descriptor which will
		 * be used in the MH resolution.
		 * See resolveUpcallInvokeHandle() in resolvesupport.cpp for details.
		 */
		calleeMH.type().toMethodDescriptorString();
	}
}

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

#include "jni.h"
#include "jcl.h"
#include "jclglob.h"
#include "jclprots.h"
#include "jcl_internal.h"

extern "C" {

#if JAVA_SPEC_VERSION >= 16
void JNICALL
Java_jdk_internal_foreign_abi_UpcallStubs_registerNatives(JNIEnv *env, jclass clazz)
{
}

/**
 * A memory segment is associated with a native scope owned by a thread, in which case the memory
 * segment in the scope will be automatically released in OpenJDK by invoking this native if the
 * owner thread of the native scope is terminated.
 * (see src/jdk.incubator.foreign/share/classes/jdk/internal/foreign/abi/UpcallStubs.java in OpenJDK)
 *
 * In our implementation, a thunk block is allocated to store all thunks in which case the whole block
 * is released only when the VM exits. Thus, this native does nothing to the thunk memory to avoid
 * regenerating & releasing thunk every time given it is cached at Java level and will be reused
 * next time.
 */
jboolean JNICALL
Java_jdk_internal_foreign_abi_UpcallStubs_freeUpcallStub0(JNIEnv *env, jobject receiver, jlong address)
{
	return true;
}
#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

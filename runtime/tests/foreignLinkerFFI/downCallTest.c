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

/*
 * Created by jincheng@ca.ibm.com
 *
 * This file contains the native code used by j9vm.test.classloader.LazyClassLoaderInitTest in j9vm_test.
 */

#include <stdio.h>

/**
 * Define an addition operation for the int type within a FFI DownCall.
 *
 * @param arg1 the 1st argument to add.
 * @param arg2 the 2nd argument to add.
 * @return the result of adding two passed-in arguments.
 */
int test2Int(int arg1, int arg2) {
	int sum = arg1 + arg2;
	return sum;
}

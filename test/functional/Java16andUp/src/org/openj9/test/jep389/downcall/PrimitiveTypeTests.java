package org.openj9.test.jep389.downcall;

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

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.*;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator)
 * 
 */
@Test(groups = { "level.sanity" })
public class PrimitiveTypeTests {
	@Test
	public void test_addTwoInts() throws Throwable {
        var lib = LibraryLookup.ofPath(Path.of("/home/jincheng/X86_64_OPENJ9/Panama_FFI_support_2021/tests/jep389add.so"));
        var sym = lib.lookup("jep389add").get();
        MemoryAddress sym_addr = sym.address();
        
        var mt1 = MethodType.methodType(int.class, int.class, int.class);
        var fd1 = FunctionDescriptor.of(C_INT, C_INT, C_INT);
        var mh1 = CLinker.getInstance().downcallHandle(sym_addr, mt1,fd1);
        int sum = (int)mh1.invokeExact(2, 3);
        System.out.println("sum1 = " + sum);
	}
	
	@Test
	public void test_addTwoIntegers() throws Throwable {

        var lib = LibraryLookup.ofPath(Path.of("/home/jincheng/X86_64_OPENJ9/Panama_FFI_support_2021/tests/jep389add.so"));
        var sym = lib.lookup("jep389add").get();
        MemoryAddress sym_addr = sym.address();
        
        var mt1 = MethodType.methodType(int.class, int.class, int.class);
        var fd1 = FunctionDescriptor.of(C_INT, C_INT, C_INT);
        var mh1 = CLinker.getInstance().downcallHandle(sym_addr, mt1,fd1);
        int sum = (int)mh1.invokeExact(2, 3);
        System.out.println("sum1 = " + sum);
        
        Integer arg1 = 5;
        Integer arg2 = 12;
        Integer sum3 = arg1 + arg2;
        int temp = (int)sum3;
        System.out.println("temp = " + temp);
        /* ----------------------- */
        var mt2 = MethodType.methodType(Integer.class, Integer.class, Integer.class);
        var fd2 = FunctionDescriptor.of(C_INT, C_INT, C_INT);
        var mh2 = CLinker.getInstance().downcallHandle(sym_addr, mt2,fd2);
        sum = (int)mh2.invokeExact(5, 6);
        System.out.println("sum2 = " + sum);
	}
}

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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.*;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.NativeScope;
import static jdk.incubator.foreign.LibraryLookup.Symbol;
import static jdk.incubator.foreign.CLinker.VaList.Builder;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) DownCall for primitive types,
 * which verifies multiple downcalls with the same or different layouts or argument/return types.
 */
@Test(groups = { "level.sanity" })
public class MultiCallTests {
	LibraryLookup nativeLib = LibraryLookup.ofLibrary("clinkerffitests");
	CLinker clinker = CLinker.getInstance();
	
	@Test
	public void test_twoCallsWithSameFuncDescriptor() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, int.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Int").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		int result = (int)mh.invokeExact(112, 123);
		Assert.assertEquals(result, 235);
		
		mh = clinker.downcallHandle(functionSymbol, mt, fd);
		result = (int)mh.invokeExact(235, 439);
		Assert.assertEquals(result, 674);
	}
	
	@Test
	public void test_twoCallsWithDiffFuncDescriptor() throws Throwable {
		MethodType mt1 = MethodType.methodType(int.class, int.class, int.class);
		FunctionDescriptor fd1 = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol1 = nativeLib.lookup("testAdd2Int").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol1, mt1, fd1);
		int result = (int)mh.invokeExact(112, 123);
		Assert.assertEquals(result, 235);
		
		MethodType mt2 = MethodType.methodType(int.class, int.class, int.class, int.class);
		FunctionDescriptor fd2 = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT);
		Symbol functionSymbol2 = nativeLib.lookup("testAdd3Int").get();
		mh = clinker.downcallHandle(functionSymbol2, mt2, fd2);
		result = (int)mh.invokeExact(112, 123, 235);
		Assert.assertEquals(result, 470);
	}
	
	@Test
	public void test_twoCallsWithDiffReturnType() throws Throwable {
		MethodType mt1 = MethodType.methodType(int.class, int.class, int.class);
		FunctionDescriptor fd1 = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol1 = nativeLib.lookup("testAdd2Int").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol1, mt1, fd1);
		int result = (int)mh.invokeExact(112, 123);
		Assert.assertEquals(result, 235);
		
		MethodType mt2 = MethodType.methodType(void.class, int.class, int.class);
		FunctionDescriptor fd2 = FunctionDescriptor.ofVoid(C_INT, C_INT);
		Symbol functionSymbol2 = nativeLib.lookup("testAdd2IntReturnVoid").get();
		mh = clinker.downcallHandle(functionSymbol2, mt2, fd2);
		mh.invokeExact(454, 398);
	}
	
	@Test
	public void test_multiCallsWithMixedFuncDescriptors() throws Throwable {
		MethodType mt1 = MethodType.methodType(int.class, int.class, int.class);
		FunctionDescriptor fd1 = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol1 = nativeLib.lookup("testAdd2Int").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol1, mt1, fd1);
		int result = (int)mh.invokeExact(112, 123);
		Assert.assertEquals(result, 235);
		
		MethodType mt2 = MethodType.methodType(int.class, int.class, int.class, int.class);
		FunctionDescriptor fd2 = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT);
		Symbol functionSymbol2 = nativeLib.lookup("testAdd3Int").get();
		mh = clinker.downcallHandle(functionSymbol2, mt2, fd2);
		result = (int)mh.invokeExact(112, 123, 235);
		Assert.assertEquals(result, 470);
		
		MethodType mt3 = MethodType.methodType(void.class, int.class, int.class);
		FunctionDescriptor fd3 = FunctionDescriptor.ofVoid(C_INT, C_INT);
		Symbol functionSymbol3 = nativeLib.lookup("testAdd2IntReturnVoid").get();
		mh = clinker.downcallHandle(functionSymbol3, mt3, fd3);
		mh.invokeExact(454, 398);
		
		MethodType mt4 = MethodType.methodType(Integer.class, int.class, int.class);
		mh = clinker.downcallHandle(functionSymbol1, mt4, fd1);
		Integer result2 = (Integer)mh.invokeExact(212, 223);
		Assert.assertEquals(result2.intValue(), 435);
		
		MethodType mt5 = MethodType.methodType(int.class, Integer.class, int.class);
		mh = clinker.downcallHandle(functionSymbol1, mt5, fd1);
		result = (int)mh.invokeExact((Integer)312, 323);
		Assert.assertEquals(result, 635);
		
		MethodType mt6 = MethodType.methodType(int.class, int.class, Integer.class, Integer.class);
		mh = clinker.downcallHandle(functionSymbol2, mt6, fd2);
		result = (int)mh.invokeExact(312, (Integer)323, (Integer)335);
		Assert.assertEquals(result, 970);
		
		mh = clinker.downcallHandle(functionSymbol3, mt3, fd3);
		mh.invokeExact(539, 672);
		
	}

}

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
import java.lang.invoke.MethodType;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.*;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ValueLayout;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.NativeScope;
import static jdk.incubator.foreign.LibraryLookup.Symbol;
import static jdk.incubator.foreign.CLinker.VaList.Builder;
import jdk.incubator.foreign.GroupLayout;
import  jdk.incubator.foreign.MemoryLayout.PathElement;
import java.lang.invoke.VarHandle;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) DownCall for primitive types,
 * which covers generic tests, tests with the void type, the MemoryAddress type, and the vararg list.
 */
@Test(groups = { "level.sanity" })
public class StructTests {
	private static boolean isWinOS = System.getProperty("os.name").toLowerCase().contains("win") ? true : false;
	private static ValueLayout longLayout = isWinOS ? C_LONG_LONG : C_LONG;
	private static LibraryLookup nativeLib = LibraryLookup.ofLibrary("clinkerffitests");
	private static LibraryLookup defaultLib = LibraryLookup.ofDefault();
	private static CLinker clinker = CLinker.getInstance();
	
	@Test
	public void test_addBoolAndBoolsFromStructWithXor() throws Throwable  {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, MemorySegment.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, structLayout);
		Symbol functionSymbol = nativeLib.lookup("addBoolAndBoolsFromStructWithXor").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment structSegmt = MemorySegment.allocateNative(structLayout);
		boolHandle1.set(structSegmt, 0);
		boolHandle2.set(structSegmt, 1);
		boolean result = (boolean)mh.invokeExact(false, structSegmt);
		Assert.assertEquals(result, true);
		structSegmt.close();
	}
	
	@Test
	public void test_addBoolFromPointerAndBoolsFromStructWithXor() throws Throwable  {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		
		MethodType mt = MethodType.methodType(boolean.class, MemoryAddress.class, MemorySegment.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_POINTER, structLayout);
		Symbol functionSymbol = nativeLib.lookup("addBoolFromPointerAndBoolsFromStructWithXor").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		
		MemorySegment booleanSegmt = MemorySegment.allocateNative(C_INT);
		MemoryAccess.setInt(booleanSegmt, 1);
		MemorySegment structSegmt = MemorySegment.allocateNative(structLayout);
		boolHandle1.set(structSegmt, 0);
		boolHandle2.set(structSegmt, 1);
		boolean result = (boolean)mh.invokeExact(booleanSegmt.address(), structSegmt);
		Assert.assertEquals(result, false);
		booleanSegmt.close();
		structSegmt.close();
	}
	
	@Test
	public void test_addBoolAndBoolsFromStructPointerWithXor() throws Throwable  {		
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addBoolAndBoolsFromStructPointerWithXor").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		
		MemorySegment structSegmt = MemorySegment.allocateNative(structLayout);
		boolHandle1.set(structSegmt, 1);
		boolHandle2.set(structSegmt, 0);
		boolean result = (boolean)mh.invokeExact(false, structSegmt.address());
		Assert.assertEquals(result, true);
		structSegmt.close();
	}
	
	@Test
	public void test_add2BoolStructsWithXor() throws Throwable  {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		
		MethodType mt = MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class);
		FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout);
		Symbol functionSymbol = nativeLib.lookup("add2BoolStructsWithXor").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment structSegmt1 = MemorySegment.allocateNative(structLayout);
		boolHandle1.set(structSegmt1, 1);
		boolHandle1.set(structSegmt1, 0);
		MemorySegment structSegmt2 = MemorySegment.allocateNative(structLayout);
		boolHandle2.set(structSegmt2, 1);
		boolHandle2.set(structSegmt2, 1);

		MemorySegment result = (MemorySegment)mh.invokeExact(structSegmt1, structSegmt2);
		Assert.assertEquals(boolHandle1.get(result), 0);
		Assert.assertEquals(boolHandle2.get(result), 1);
		structSegmt1.close();
		structSegmt2.close();
		result.close();
	}
	
	@Test
	public void test_addBoolAndBoolsFromNestedStructWithXor() throws Throwable  {
		GroupLayout nestedstructLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedstructLayout.withName("struct_elem1"), C_INT.withName("elem2"));
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, MemorySegment.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, structLayout);
		Symbol functionSymbol = nativeLib.lookup("addBoolAndBoolsFromNestedStructWithXor").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment structSegmt = MemorySegment.allocateNative(structLayout.bitSize());
		MemoryAccess.setIntAtOffset(structSegmt, 0, 1);
		MemoryAccess.setIntAtOffset(structSegmt, 4, 0);
		MemoryAccess.setIntAtOffset(structSegmt, 8, 1);
		boolean result = (boolean)mh.invokeExact(true, structSegmt);
		Assert.assertEquals(result, true);
		structSegmt.close();
	}
}

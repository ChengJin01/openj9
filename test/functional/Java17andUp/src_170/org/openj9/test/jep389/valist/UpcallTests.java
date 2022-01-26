/*******************************************************************************
 * Copyright (c) 2022, 2022 IBM Corp. and others
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
package org.openj9.test.jep389.valist;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.*;
import static jdk.incubator.foreign.CLinker.VaList.Builder;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.ValueLayout;

import org.openj9.test.jep389.upcall.UpcallMethodHandles;
import static org.openj9.test.jep389.upcall.UpcallMethodHandles.*;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) for the vararg list in upcall.
 */
@Test(groups = { "level.sanity" })
public class UpcallTests {
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isAixOS = osName.contains("aix");
	private static boolean isWinOS = osName.contains("win");
	/* long long is 64 bits on AIX/ppc64, which is the same as Windows */
	private static ValueLayout longLayout = (isWinOS || isAixOS) ? C_LONG_LONG : C_LONG;
	private static CLinker clinker = CLinker.getInstance();

	static {
		System.loadLibrary("clinkerffitests");
	}
	private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();

	@Test(enabled=false)
	public void test_addIntsWithVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addIntsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 700)
					.vargFromInt(C_INT, 800)
					.vargFromInt(C_INT, 900)
					.vargFromInt(C_INT, 1000), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addIntsFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST), scope);

			int result = (int)mh.invoke(4, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 3400);
		}
	}

	@Test(enabled=false)
	public void test_addLongsFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addLongsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromLong(longLayout, 700000L)
					.vargFromLong(longLayout, 800000L)
					.vargFromLong(longLayout, 900000L)
					.vargFromLong(longLayout, 1000000L), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addLongsFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST), scope);

			long result = (long)mh.invoke(4, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 3400000L);
		}
	}

	@Test(enabled=false)
	public void test_addDoublesFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromDouble(C_DOUBLE, 111150.1001D)
					.vargFromDouble(C_DOUBLE, 111160.2002D)
					.vargFromDouble(C_DOUBLE, 111170.1001D)
					.vargFromDouble(C_DOUBLE, 111180.2002D), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addDoublesFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST), scope);

			double result = (double)mh.invoke(4, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 444660.6006D, 0.0001D);
		}
	}

	@Test(enabled=false)
	public void test_addMixedArgsFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addMixedArgsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 700)
					.vargFromLong(longLayout, 800000L)
					.vargFromDouble(C_DOUBLE, 160.2002D), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addMixedArgsFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_VA_LIST), scope);

			double result = (double)mh.invoke(vaList, upcallFuncAddr);
			Assert.assertEquals(result, 800860.2002D, 0.0001D);
		}
	}

	@Test(enabled=false)
	public void test_addIntsByPtrFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addIntsByPtrFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
			MemorySegment intSegmt1 = allocator.allocate(C_INT, 700);
			MemorySegment intSegmt2 = allocator.allocate(C_INT, 800);
			MemorySegment intSegmt3 = allocator.allocate(C_INT, 900);

			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromAddress(C_POINTER, intSegmt1.address())
					.vargFromAddress(C_POINTER, intSegmt2.address())
					.vargFromAddress(C_POINTER, intSegmt3.address()), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addIntsByPtrFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST), scope);

			int result = (int)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 2400);
		}
	}

	@Test(enabled=false)
	public void test_addLongsByPtrFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addLongsByPtrFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
			MemorySegment longSegmt1 = allocator.allocate(longLayout, 700000L);
			MemorySegment longSegmt2 = allocator.allocate(longLayout, 800000L);
			MemorySegment longSegmt3 = allocator.allocate(longLayout, 900000L);

			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromAddress(C_POINTER, longSegmt1.address())
					.vargFromAddress(C_POINTER, longSegmt2.address())
					.vargFromAddress(C_POINTER, longSegmt3.address()), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addLongsByPtrFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST), scope);

			long result = (long)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 2400000L);
		}
	}

	@Test(enabled=false)
	public void test_addDoublesByPtrFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesByPtrFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
			MemorySegment doubleSegmt1 = allocator.allocate(C_DOUBLE, 150.1001D);
			MemorySegment doubleSegmt2 = allocator.allocate(C_DOUBLE, 160.2002D);
			MemorySegment doubleSegmt3 = allocator.allocate(C_DOUBLE, 170.1001D);

			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromAddress(C_POINTER, doubleSegmt1.address())
					.vargFromAddress(C_POINTER, doubleSegmt2.address())
					.vargFromAddress(C_POINTER, doubleSegmt3.address()), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addDoublesByPtrFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST), scope);

			double result = (double)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 480.4004D, 0.0001D);
		}
	}

	@Test(enabled=false)
	public void test_addIntsByStructFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addIntsByStructFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt1, 1122333);
			intHandle2.set(structSegmt1, 4455666);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt2, 2244668);
			intHandle2.set(structSegmt2, 1133557);

			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addIntsByStructFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST), scope);

			int result = (int)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 8956224);
		}
	}

	@Test(enabled=false)
	public void test_addLongsByStructFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addLongsByStructFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			longHandle1.set(structSegmt1, 1122334455L);
			longHandle2.set(structSegmt1, 6677889911L);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			longHandle1.set(structSegmt2, 2233445566L);
			longHandle2.set(structSegmt2, 7788991122L);

			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addLongsByStructFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST), scope);

			long result = (long)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 17822661054L);
		}
	}

	@Test(enabled=false)
	public void test_addDoublesByStructFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesByStructFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt1, 11150.1001D);
			doubleHandle2.set(structSegmt1, 11160.2002D);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt2, 11170.1001D);
			doubleHandle2.set(structSegmt2, 11180.2002D);

			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(UpcallMethodHandles.MH_addDoublesByStructFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST), scope);

			double result = (double)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 44660.6006D, 0.0001D);
		}
	}
}

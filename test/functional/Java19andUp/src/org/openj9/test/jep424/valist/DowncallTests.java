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
package org.openj9.test.jep424.valist;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import java.lang.foreign.Addressable;
import java.lang.foreign.Linker;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.VaList;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.VaList.Builder;

/**
 * Test cases for JEP 424: Foreign Linker API (Preview) for the vararg list in downcall.
 */
@Test(groups = { "level.sanity" })
public class DowncallTests {
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isAixOS = osName.contains("aix");
	private static boolean isWinOS = osName.contains("win");
	private static Linker linker = Linker.nativeLinker();

	static {
		System.loadLibrary("clinkerffitests");
	}
	private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();
	private static final SymbolLookup defaultLibLookup = linker.defaultLookup();

	@Test
	public void test_addIntsFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addIntsFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(JAVA_INT, 700)
					.addVarg(JAVA_INT, 800)
					.addVarg(JAVA_INT, 900)
					.addVarg(JAVA_INT, 1000), session);
			int result = (int)mh.invoke(4, vaList);
			Assert.assertEquals(result, 3400);
		}
	}

	@Test
	public void test_addLongsFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addLongsFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(JAVA_LONG, 700000L)
					.addVarg(JAVA_LONG, 800000L)
					.addVarg(JAVA_LONG, 900000L)
					.addVarg(JAVA_LONG, 1000000L), session);
			long result = (long)mh.invoke(4, vaList);
			Assert.assertEquals(result, 3400000L);
		}
	}

	@Test
	public void test_addDoublesFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_DOUBLE, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(JAVA_DOUBLE, 150.1001D)
					.addVarg(JAVA_DOUBLE, 160.2002D)
					.addVarg(JAVA_DOUBLE, 170.1001D)
					.addVarg(JAVA_DOUBLE, 180.2002D), session);
			double result = (double)mh.invoke(4, vaList);
			Assert.assertEquals(result, 660.6006D, 0.0001D);
		}
	}

	@Test
	public void test_addMixedArgsFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addMixedArgsFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(JAVA_INT, 700)
					.addVarg(JAVA_LONG, 800000L)
					.addVarg(JAVA_DOUBLE, 160.2002D), session);
			double result = (double)mh.invoke(vaList);
			Assert.assertEquals(result, 800860.2002D, 0.0001D);
		}
	}

	@Test
	public void test_addIntsByPtrFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addIntsByPtrFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			SegmentAllocator allocator = SegmentAllocator.newNativeArena(session);
			MemorySegment intSegmt1 = allocator.allocate(JAVA_INT, 700);
			MemorySegment intSegmt2 = allocator.allocate(JAVA_INT, 800);
			MemorySegment intSegmt3 = allocator.allocate(JAVA_INT, 900);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(ADDRESS, intSegmt1.address())
					.addVarg(ADDRESS, intSegmt2.address())
					.addVarg(ADDRESS, intSegmt3.address()), session);
			int result = (int)mh.invoke(3, vaList);
			Assert.assertEquals(result, 2400);
		}
	}

	@Test
	public void test_addLongsByPtrFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addLongsByPtrFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			SegmentAllocator allocator = SegmentAllocator.newNativeArena(session);
			MemorySegment longSegmt1 = allocator.allocate(JAVA_LONG, 700000L);
			MemorySegment longSegmt2 = allocator.allocate(JAVA_LONG, 800000L);
			MemorySegment longSegmt3 = allocator.allocate(JAVA_LONG, 900000L);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(ADDRESS, longSegmt1.address())
					.addVarg(ADDRESS, longSegmt2.address())
					.addVarg(ADDRESS, longSegmt3.address()), session);
			long result = (long)mh.invoke(3, vaList);
			Assert.assertEquals(result, 2400000L);
		}
	}

	@Test
	public void test_addDoublesByPtrFromVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesByPtrFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_DOUBLE, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			SegmentAllocator allocator = SegmentAllocator.newNativeArena(session);
			MemorySegment doubleSegmt1 = allocator.allocate(JAVA_DOUBLE, 111150.1001D);
			MemorySegment doubleSegmt2 = allocator.allocate(JAVA_DOUBLE, 111160.2002D);
			MemorySegment doubleSegmt3 = allocator.allocate(JAVA_DOUBLE, 111170.1001D);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(ADDRESS, doubleSegmt1.address())
					.addVarg(ADDRESS, doubleSegmt2.address())
					.addVarg(ADDRESS, doubleSegmt3.address()), session);
			double result = (double)mh.invoke(3, vaList);
			Assert.assertEquals(result, 333480.4004D, 0.0001D);
		}
	}

	@Test
	public void test_addIntsByStructFromVaList() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addIntsByStructFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			SegmentAllocator allocator = SegmentAllocator.newNativeArena(session);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt1, 1122333);
			intHandle2.set(structSegmt1, 4455666);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt2, 2244668);
			intHandle2.set(structSegmt2, 1133557);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(structLayout, structSegmt1)
					.addVarg(structLayout, structSegmt2), session);
			int result = (int)mh.invoke(2, vaList);
			Assert.assertEquals(result, 8956224);
		}
	}

	@Test
	public void test_addLongsByStructFromVaList() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(JAVA_LONG.withName("elem1"), JAVA_LONG.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addLongsByStructFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			SegmentAllocator allocator = SegmentAllocator.newNativeArena(session);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			longHandle1.set(structSegmt1, 1122334455L);
			longHandle2.set(structSegmt1, 6677889911L);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			longHandle1.set(structSegmt2, 2233445566L);
			longHandle2.set(structSegmt2, 7788991122L);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(structLayout, structSegmt1)
					.addVarg(structLayout, structSegmt2), session);
			long result = (long)mh.invoke(2, vaList);
			Assert.assertEquals(result, 17822661054L);
		}
	}

	@Test
	public void test_addDoublesByStructFromVaList() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(JAVA_DOUBLE.withName("elem1"), JAVA_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesByStructFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_DOUBLE, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			SegmentAllocator allocator = SegmentAllocator.newNativeArena(session);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt1, 11150.1001D);
			doubleHandle2.set(structSegmt1, 11160.2002D);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt2, 11170.1001D);
			doubleHandle2.set(structSegmt2, 11180.2002D);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(structLayout, structSegmt1)
					.addVarg(structLayout, structSegmt2), session);
			double result = (double)mh.invoke(2, vaList);
			Assert.assertEquals(result, 44660.6006D, 0.0001D);
		}
	}

	@Test
	public void test_vprintfFromDefaultLibFromVaList() throws Throwable {
		/* 1) Disable the test on Windows given a misaligned access exception coming from
		 * java.base/java.lang.invoke.MemoryAccessVarHandleBase triggered by CLinker.toCString()
		 * is also captured on OpenJDK/Hotspot.
		 * 2) Disable the test on AIX as Valist is not yet implemented in OpenJDK.
		 */
		if (!isWinOS && !isAixOS) {
			Addressable functionSymbol = defaultLibLookup.lookup("vprintf").get();
			FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
			MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

			try (MemorySession session = MemorySession.openConfined()) {
				SegmentAllocator nativeAllocator = SegmentAllocator.newNativeArena(session);
				MemorySegment formatSegmt = nativeAllocator.allocateUtf8String("%d * %d = %d\n");
				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(JAVA_INT, 7)
						.addVarg(JAVA_INT, 8)
						.addVarg(JAVA_INT, 56), session);
				mh.invoke(formatSegmt, vaList);
			}
		}
	}
}

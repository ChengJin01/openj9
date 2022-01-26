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
package org.openj9.test.jep424.valist;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.Addressable;
import java.lang.foreign.Linker;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.VaList;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.VaList.Builder;

import org.openj9.test.jep424.upcall.UpcallMethodHandles;
import static org.openj9.test.jep424.upcall.UpcallMethodHandles.*;

/**
 * Test cases for JEP 424: Foreign Linker API (Preview) for the vararg list in downcall & upcall.
 */
@Test(groups = { "level.sanity" })
public class VaListTests {
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
	public void test_addIntsWithVaList() throws Throwable {
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
	public void test_addLongsWithVaList() throws Throwable {
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
	public void test_addDoublesWithVaList() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesFromVaList").get();
		FunctionDescriptor fd = FunctionDescriptor.of(JAVA_DOUBLE, JAVA_INT, ADDRESS);
		MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

		try (MemorySession session = MemorySession.openConfined()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.addVarg(JAVA_DOUBLE, 150.1001D)
					.addVarg(JAVA_DOUBLE, 160.2002D)
					.addVarg(JAVA_DOUBLE, 170.1001D)
					.addVarg(JAVA_DOUBLE, 180.2002D), session);
			double result = (double)mh.invoke(4, vaList);
			Assert.assertEquals(result, 660.6006D);
		}
	}

	@Test
	public void test_vprintfFromDefaultLibWithVaList() throws Throwable {
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

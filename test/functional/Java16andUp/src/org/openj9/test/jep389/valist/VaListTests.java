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
package org.openj9.test.jep389.valist;

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
import org.openj9.test.jep389.upcall.UpCallMethodHandles;
import static org.openj9.test.jep389.upcall.UpCallMethodHandles.*;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) DownCall & UpCall for the vararg list.
 */
@Test(groups = { "level.sanity" })
public class VaListTests {
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isAixOS = osName.contains("aix");
	private static boolean isWinOS = osName.contains("win");
	/* long long is 64 bits on AIX/ppc64, which is the same as Windows */
	private static ValueLayout longLayout = (isWinOS || isAixOS) ? C_LONG_LONG : C_LONG;
	
	private static LibraryLookup nativeLib = LibraryLookup.ofLibrary("clinkerffitests");
	private static LibraryLookup defaultLib = LibraryLookup.ofDefault();
	private static CLinker clinker = CLinker.getInstance();

	@Test
	public void test_addIntsWithVaList() throws Throwable {
		Symbol functionSymbol = nativeLib.lookup("addIntsFromVaList").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST);
		NativeScope nativeScope = NativeScope.unboundedScope();
		VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 700)
				.vargFromInt(C_INT, 800)
				.vargFromInt(C_INT, 900)
				.vargFromInt(C_INT, 1000), nativeScope);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		int result = (int)mh.invoke(4, vaList);
		Assert.assertEquals(result, 3400);
	}
	
	@Test
	public void test_addLongsWithVaList() throws Throwable {
		Symbol functionSymbol = nativeLib.lookup("addLongsFromVaList").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST);
		NativeScope nativeScope = NativeScope.unboundedScope();
		VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromLong(longLayout, 700000L)
				.vargFromLong(longLayout, 800000L)
				.vargFromLong(longLayout, 900000L)
				.vargFromLong(longLayout, 1000000L), nativeScope);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		long result = (long)mh.invoke(4, vaList);
		Assert.assertEquals(result, 3400000L);
	}
	
	@Test
	public void test_addDoublesWithVaList() throws Throwable {
		Symbol functionSymbol = nativeLib.lookup("addDoublesFromVaList").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST);
		NativeScope nativeScope = NativeScope.unboundedScope();
		VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromDouble(C_DOUBLE, 150.1001D)
				.vargFromDouble(C_DOUBLE, 160.2002D)
				.vargFromDouble(C_DOUBLE, 170.1001D)
				.vargFromDouble(C_DOUBLE, 180.2002D), nativeScope);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		double result = (double)mh.invoke(4, vaList);
		Assert.assertEquals(result, 660.6006D);
	}
	
	@Test
	public void test_vprintfFromDefaultLibWithVaList() throws Throwable {
		/* Disable the test on Windows given a misaligned access exception coming from
		 * java.base/java.lang.invoke.MemoryAccessVarHandleBase triggered by CLinker.toCString()
		 * is also captured on OpenJDK/Hotspot.
		 */
		if (!isWinOS) {
			Symbol functionSymbol = defaultLib.lookup("vprintf").get();
			MethodType mt = MethodType.methodType(int.class, MemoryAddress.class, VaList.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_POINTER, C_VA_LIST);
			NativeScope nativeScope = NativeScope.unboundedScope();
			MemorySegment formatMemSegment = CLinker.toCString("%d * %d = %d\n", nativeScope);
			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 7)
					.vargFromInt(C_INT, 8)
					.vargFromInt(C_INT, 56), nativeScope);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
			mh.invoke(formatMemSegment.address(), vaList);
		}
	}
	
	@Test
	public void test_vprintfFromDefaultLibWithVaList_fromMemAddr() throws Throwable {
		/* Disable the test on Windows given a misaligned access exception coming from
		 * java.base/java.lang.invoke.MemoryAccessVarHandleBase triggered by CLinker.toCString()
		 * is also captured on OpenJDK/Hotspot.
		 */
		if (!isWinOS) {
			Symbol functionSymbol = defaultLib.lookup("vprintf").get();
			MemoryAddress memAddr = functionSymbol.address();
			MethodType mt = MethodType.methodType(int.class, MemoryAddress.class, VaList.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_POINTER, C_VA_LIST);
			NativeScope nativeScope = NativeScope.unboundedScope();
			MemorySegment formatMemSegment = CLinker.toCString("%d * %d = %d\n", nativeScope);
			VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 7)
					.vargFromInt(C_INT, 8)
					.vargFromInt(C_INT, 56), nativeScope);
			MethodHandle mh = clinker.downcallHandle(memAddr, mt, fd);
			mh.invoke(formatMemSegment.address(), vaList);
		}
	}
	
	@Test
	public void test_addIntsWithVaListByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addIntsFromVaListByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		
		NativeScope nativeScope = NativeScope.unboundedScope();
		VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 700)
				.vargFromInt(C_INT, 800)
				.vargFromInt(C_INT, 900)
				.vargFromInt(C_INT, 1000), nativeScope);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addIntsFromVaList,
				FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST));
		
		int result = (int)mh.invoke(4, vaList, upcallFunc.address());
		Assert.assertEquals(result, 3400);
	}
	
	@Test
	public void test_addLongsFromVaListByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addLongsFromVaListByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		
		NativeScope nativeScope = NativeScope.unboundedScope();
		VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromLong(longLayout, 700000L)
				.vargFromLong(longLayout, 800000L)
				.vargFromLong(longLayout, 900000L)
				.vargFromLong(longLayout, 1000000L), nativeScope);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addLongsFromVaList,
				FunctionDescriptor.of(longLayout, C_INT, C_VA_LIST));
		
		long result = (long)mh.invoke(4, vaList, upcallFunc.address());
		Assert.assertEquals(result, 3400000L);
	}
	
	@Test
	public void test_addDoublesFromVaListByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addDoublesFromVaListByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		
		NativeScope nativeScope = NativeScope.unboundedScope();
		VaList vaList = CLinker.VaList.make(vaListBuilder -> vaListBuilder.vargFromDouble(C_DOUBLE, 150.1001D)
				.vargFromDouble(C_DOUBLE, 160.2002D)
				.vargFromDouble(C_DOUBLE, 170.1001D)
				.vargFromDouble(C_DOUBLE, 180.2002D), nativeScope);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addDoublesFromVaList,
				FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST));
		
		double result = (double)mh.invoke(4, vaList, upcallFunc.address());
		Assert.assertEquals(result, 660.6006D);
	}
}

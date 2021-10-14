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
package org.openj9.test.jep389.upcall;

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
import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) Upcall for primitive types,
 * which covers generic tests, tests with the void type, the MemoryAddress type, and the vararg list.
 */
@Test(groups = { "level.sanity" })
public class UpcallMHWithPrimitiveTests2 {
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isAixOS = osName.contains("aix");
	private static boolean isWinOS = osName.contains("win");
	/* long long is 64 bits on AIX/ppc64, which is the same as Windows */
	private static ValueLayout longLayout = (isWinOS || isAixOS) ? C_LONG_LONG : C_LONG;
	private UpcallMethodHandles2 upcallMH = new UpcallMethodHandles2();
	private static CLinker clinker = CLinker.getInstance();
	private static ResourceScope resourceScope = ResourceScope.newImplicitScope();

	static {
		System.loadLibrary("clinkerffitests");
	}
	private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();
	private static final SymbolLookup defaultLibLookup = (!isAixOS) ? CLinker.systemLookup() : null;

	@Test
	public void test_addTwoBoolsWithOrByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, boolean.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2BoolsWithOrByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			System.out.println("upcallMH.MH_add2BoolsWithOr.type = " + upcallMH.MH_add2BoolsWithOr.type());
			MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2BoolsWithOr,
					FunctionDescriptor.of(C_INT, C_INT, C_INT), scope);
			boolean result = (boolean)mh.invokeExact(true, false, upcallFuncAddr);
			Assert.assertEquals(result, true);
		}
	}
/*
	@Test
	public void test_addBoolAndBoolFromPointerWithOrByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addBoolAndBoolFromPointerWithOrByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addBoolAndBoolFromPointerWithOr,
				FunctionDescriptor.of(C_INT, C_INT, C_POINTER));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment intSegmt = allocator.allocate(C_INT);
			MemoryAccess.setInt(intSegmt, 1);
			boolean result = (boolean)mh.invokeExact(false, intSegmt.address(), upcallFuncAddr);
			Assert.assertEquals(result, true);
		}
	}

	@Test
	public void test_generateNewCharByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(char.class, char.class, char.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("createNewCharFrom2CharsByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_createNewCharFrom2Chars,
				FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT));
		char result = (char)mh.invokeExact('B', 'D', upcallFuncAddr);
		Assert.assertEquals(result, 'C');
	}

	@Test
	public void test_generateNewCharFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(char.class, MemoryAddress.class, char.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("createNewCharFromCharAndCharFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_createNewCharFromCharAndCharFromPointer,
				FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment shortSegmt = allocator.allocate(C_SHORT);
			MemoryAccess.setChar(shortSegmt, 'B');
			char result = (char)mh.invokeExact(shortSegmt.address(), 'D', upcallFuncAddr);
			Assert.assertEquals(result, 'C');
		}
	}

	@Test
	public void test_addTwoBytesByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(byte.class, byte.class, byte.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_CHAR, C_CHAR, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2BytesByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2Bytes,
				FunctionDescriptor.of(C_CHAR, C_CHAR, C_CHAR));
		byte result = (byte)mh.invokeExact((byte)6, (byte)3, upcallFuncAddr);
		Assert.assertEquals(result, (byte)9);
	}

	@Test
	public void test_addByteAndByteFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(byte.class, byte.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_CHAR, C_POINTER, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addByteAndByteFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addByteAndByteFromPointer,
				FunctionDescriptor.of(C_CHAR, C_CHAR, C_POINTER));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment charSegmt = allocator.allocate(C_CHAR);
			MemoryAccess.setByte(charSegmt, (byte)7);
			byte result = (byte)mh.invokeExact((byte)8, charSegmt.address(), upcallFuncAddr);
			Assert.assertEquals(result, (byte)15);
		}
	}

	@Test
	public void test_addTwoShortsByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(short.class, short.class, short.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2ShortsByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2Shorts,
				FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT));
		short result = (short)mh.invokeExact((short)11, (short)22, upcallFuncAddr);
		Assert.assertEquals(result, (short)33);
	}

	@Test
	public void test_addShortAndShortFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(short.class, MemoryAddress.class, short.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addShortAndShortFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addShortAndShortFromPointer,
				FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment shortSegmt = allocator.allocate(C_SHORT);
			MemoryAccess.setShort(shortSegmt, (short)22);
			short result = (short)mh.invokeExact(shortSegmt.address(), (short)33, upcallFuncAddr);
			Assert.assertEquals(result, (short)55);
		}
	}

	@Test
	public void test_addTwoIntsByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2IntsByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2Ints,
				FunctionDescriptor.of(C_INT, C_INT, C_INT));
		int result = (int)mh.invokeExact(112, 123, upcallFuncAddr);
		Assert.assertEquals(result, 235);
	}

	@Test
	public void test_addIntAndIntFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addIntAndIntFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addIntAndIntFromPointer,
				FunctionDescriptor.of(C_INT, C_INT, C_POINTER));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment intSegmt = allocator.allocate(C_INT);
			MemoryAccess.setInt(intSegmt, 215);
			int result = (int)mh.invokeExact(321, intSegmt.address(), upcallFuncAddr);
			Assert.assertEquals(result, 536);
		}
	}

	@Test
	public void test_add3IntsByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add3IntsByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add3Ints,
				FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));
		int result = (int)mh.invokeExact(112, 123, 124, upcallFuncAddr);
		Assert.assertEquals(result, 359);
	}

	@Test
	public void test_addIntAndCharByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, char.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_SHORT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addIntAndCharByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addIntAndChar,
				FunctionDescriptor.of(C_INT, C_INT, C_SHORT));
		int result = (int)mh.invokeExact(58, 'A', upcallFuncAddr);
		Assert.assertEquals(result, 123);
	}

	@Test
	public void test_addTwoIntsReturnVoidByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(void.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_INT, C_INT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2IntsReturnVoidByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2IntsReturnVoid,
				FunctionDescriptor.ofVoid(C_INT, C_INT));
		mh.invokeExact(454, 398, upcallFuncAddr);
	}

	@Test
	public void test_addTwoLongsByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(long.class, long.class, long.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, longLayout, longLayout, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2LongsByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2Longs,
				FunctionDescriptor.of(longLayout, longLayout, longLayout));
		long result = (long)mh.invokeExact(333222L, 111555L, upcallFuncAddr);
		Assert.assertEquals(result, 444777L);
	}

	@Test
	public void test_addLongAndLongFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(long.class, MemoryAddress.class, long.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_POINTER, longLayout, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addLongAndLongFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addLongAndLongFromPointer,
				FunctionDescriptor.of(longLayout, C_POINTER, longLayout));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment longSegmt = allocator.allocate(longLayout);
			MemoryAccess.setLong(longSegmt, 57424L);
			long result = (long)mh.invokeExact(longSegmt.address(), 698235L, upcallFuncAddr);
			Assert.assertEquals(result, 755659L);
		}
	}

	@Test
	public void test_addTwoFloatsByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(float.class, float.class, float.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_FLOAT, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2FloatsByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2Floats,
				FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_FLOAT));
		float result = (float)mh.invokeExact(15.74f, 16.79f, upcallFuncAddr);
		Assert.assertEquals(result, 32.53f, 0.01f);
	}

	@Test
	public void test_addFloatAndFloatFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(float.class, float.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_POINTER, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addFloatAndFloatFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addFloatAndFloatFromPointer,
				FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_POINTER));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment floatSegmt = allocator.allocate(C_FLOAT);
			MemoryAccess.setFloat(floatSegmt, 6.79f);
			float result = (float)mh.invokeExact(5.74f, floatSegmt.address(), upcallFuncAddr);
			Assert.assertEquals(result, 12.53f, 0.01f);
		}
	}

	@Test
	public void test_add2DoublesByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(double.class, double.class, double.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_DOUBLE, C_DOUBLE, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("add2DoublesByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_add2Doubles,
				FunctionDescriptor.of(C_DOUBLE, C_DOUBLE, C_DOUBLE));
		double result = (double)mh.invokeExact(159.748d, 262.795d, upcallFuncAddr);
		Assert.assertEquals(result, 422.543d, 0.001d);
	}

	@Test
	public void test_addDoubleAndDoubleFromPointerByUpcallMH_2() throws Throwable {
		MethodType mt = MethodType.methodType(double.class, MemoryAddress.class, double.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_DOUBLE, C_POINTER);
		Addressable functionSymbol = nativeLibLookup.lookup("addDoubleAndDoubleFromPointerByUpcallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_addDoubleAndDoubleFromPointer,
				FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_DOUBLE));

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment doubleSegmt = allocator.allocate(C_DOUBLE);
			MemoryAccess.setDouble(doubleSegmt, 1159.748d);
			double result = (double)mh.invokeExact(doubleSegmt.address(), 1262.795d, upcallFuncAddr);
			Assert.assertEquals(result, 2422.543d, 0.001d);
		}
	}

	@Test
	public void test_qsortByUpcallMH_2() throws Throwable {
		// The default library loading is not yet implemented on AIX
		if (!isAixOS) {
			int expectedArray[] = {11, 12, 13, 14, 15, 16, 17};
			int expectedArrayLength = expectedArray.length;

			MethodType mt = MethodType.methodType(void.class, MemoryAddress.class, int.class, int.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_INT, C_POINTER);
			Addressable functionSymbol = defaultLibLookup.lookup("qsort").get();
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(upcallMH.MH_compare,
					FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
				MemorySegment arraySegmt =  allocator.allocateArray(C_INT, new int[]{17, 14, 13, 16, 15, 12, 11});
				mh.invokeExact(arraySegmt.address(), 7, 4, upcallFuncAddr);
				int[] sortedArray = arraySegmt.toIntArray();
				for (int index = 0; index < expectedArrayLength; index++) {
					Assert.assertEquals(sortedArray[index], expectedArray[index]);
				}
			}
		}
	}
	*/
}

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
import static jdk.incubator.foreign.CLinker.VaList.Builder;
import jdk.incubator.foreign.LibraryLookup;
import static jdk.incubator.foreign.LibraryLookup.Symbol;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ValueLayout;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.NativeScope;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) UpCall for primitive types,
 * which covers generic tests, tests with the void type, the MemoryAddress type, and the vararg list.
 */
@Test(groups = { "level.sanity" })
public class UpCallMHWithPrimitiveTests {
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isAixOS = osName.contains("aix");
	private static boolean isWinOS = osName.contains("win");
	/* long long is 64 bits on AIX/ppc64, which is the same as Windows */
	private static ValueLayout longLayout = (isWinOS || isAixOS) ? C_LONG_LONG : C_LONG;

	private static LibraryLookup nativeLib = LibraryLookup.ofLibrary("clinkerffitests");
	private static LibraryLookup defaultLib = LibraryLookup.ofDefault();
	private static CLinker clinker = CLinker.getInstance();

	/*
	@Test
	public void test_addTwoBoolsWithOrByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, boolean.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2BoolsWithOrByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2BoolsWithOr,
				FunctionDescriptor.of(C_INT, C_INT, C_INT));

		boolean result = (boolean)mh.invokeExact(true, false, upcallFunc.address());
		Assert.assertEquals(result, true);
	}

	@Test
	public void test_addBoolAndBoolFromPointerWithOrByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addBoolAndBoolFromPointerWithOrByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment intSegmt = MemorySegment.allocateNative(C_INT);
		MemoryAccess.setInt(intSegmt, 1);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addBoolAndBoolFromPointerWithOr,
				FunctionDescriptor.of(C_INT, C_INT, C_POINTER));

		boolean result = (boolean)mh.invokeExact(false, intSegmt.address(), upcallFunc.address());
		Assert.assertEquals(result, true);
		intSegmt.close();
	}
	*/

	@Test
	public void test_generateNewCharByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(char.class, char.class, char.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("createNewCharFrom2CharsByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_createNewCharFrom2Chars,
				FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT));

		char result = (char)mh.invokeExact('B', 'D', upcallFunc.address());
		Assert.assertEquals(result, 'C');
	}

	@Test
	public void test_generateNewCharFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(char.class, MemoryAddress.class, char.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("createNewCharFromCharAndCharFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment shortSegmt = MemorySegment.allocateNative(C_SHORT);
		MemoryAccess.setChar(shortSegmt, 'B');
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_createNewCharFromCharAndCharFromPointer,
				FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT));

		char result = (char)mh.invokeExact(shortSegmt.address(), 'D', upcallFunc.address());
		Assert.assertEquals(result, 'C');
		shortSegmt.close();
	}

	@Test
	public void test_addTwoBytesByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(byte.class, byte.class, byte.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_CHAR, C_CHAR, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2BytesByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2Bytes,
				FunctionDescriptor.of(C_CHAR, C_CHAR, C_CHAR));

		byte result = (byte)mh.invokeExact((byte)6, (byte)3, upcallFunc.address());
		Assert.assertEquals(result, (byte)9);
	}

	@Test
	public void test_addByteAndByteFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(byte.class, byte.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_CHAR, C_POINTER, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addByteAndByteFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment charSegmt = MemorySegment.allocateNative(C_CHAR);
		MemoryAccess.setByte(charSegmt, (byte)7);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addByteAndByteFromPointer,
				FunctionDescriptor.of(C_CHAR, C_CHAR, C_POINTER));

		byte result = (byte)mh.invokeExact((byte)8, charSegmt.address(), upcallFunc.address());
		Assert.assertEquals(result, (byte)15);
		charSegmt.close();
	}

	@Test
	public void test_addTwoShortsByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(short.class, short.class, short.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2ShortsByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2Shorts,
				FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT));

		short result = (short)mh.invokeExact((short)11, (short)22, upcallFunc.address());
		Assert.assertEquals(result, (short)33);
	}

	@Test
	public void test_addShortAndShortFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(short.class, MemoryAddress.class, short.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addShortAndShortFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment shortSegmt = MemorySegment.allocateNative(C_SHORT);
		MemoryAccess.setShort(shortSegmt, (short)22);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addShortAndShortFromPointer,
				FunctionDescriptor.of(C_SHORT, C_POINTER, C_SHORT));

		short result = (short)mh.invokeExact(shortSegmt.address(), (short)33, upcallFunc.address());
		Assert.assertEquals(result, (short)55);
		shortSegmt.close();
	}

	@Test
	public void test_addTwoIntsByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2IntsByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2Ints,
				FunctionDescriptor.of(C_INT, C_INT, C_INT));

		int result = (int)mh.invokeExact(112, 123, upcallFunc.address());
		Assert.assertEquals(result, 235);
	}

	@Test
	public void test_addIntAndIntFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addIntAndIntFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment intSegmt = MemorySegment.allocateNative(C_INT);
		MemoryAccess.setInt(intSegmt, 215);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addIntAndIntFromPointer,
				FunctionDescriptor.of(C_INT, C_INT, C_POINTER));

		int result = (int)mh.invokeExact(321, intSegmt.address(), upcallFunc.address());
		Assert.assertEquals(result, 536);
		intSegmt.close();
	}

	@Test
	public void test_add3IntsByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add3IntsByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add3Ints,
				FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));

		int result = (int)mh.invokeExact(112, 123, 124, upcallFunc.address());
		Assert.assertEquals(result, 359);
	}

	@Test
	public void test_addIntAndCharByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, char.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_SHORT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addIntAndCharByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addIntAndChar,
				FunctionDescriptor.of(C_INT, C_INT, C_SHORT));

		int result = (int)mh.invokeExact(58, 'A', upcallFunc.address());
		Assert.assertEquals(result, 123);
	}

	@Test
	public void test_addTwoIntsReturnVoidByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(void.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_INT, C_INT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2IntsReturnVoidByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2IntsReturnVoid,
				FunctionDescriptor.ofVoid(C_INT, C_INT));

		mh.invokeExact(454, 398, upcallFunc.address());
	}

	@Test
	public void test_addTwoLongsByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(long.class, long.class, long.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, longLayout, longLayout, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2LongsByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2Longs,
				FunctionDescriptor.of(longLayout, longLayout, longLayout));

		long result = (long)mh.invokeExact(333222L, 111555L, upcallFunc.address());
		Assert.assertEquals(result, 444777L);
	}

	@Test
	public void test_addLongAndLongFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(long.class, MemoryAddress.class, long.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_POINTER, longLayout, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addLongAndLongFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment longSegmt = MemorySegment.allocateNative(longLayout);
		MemoryAccess.setLong(longSegmt, 57424L);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addLongAndLongFromPointer,
				FunctionDescriptor.of(longLayout, C_POINTER, longLayout));

		long result = (long)mh.invokeExact(longSegmt.address(), 698235L, upcallFunc.address());
		Assert.assertEquals(result, 755659L);
		longSegmt.close();
	}

	@Test
	public void test_addTwoFloatsByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(float.class, float.class, float.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_FLOAT, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2FloatsByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2Floats,
				FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_FLOAT));

		float result = (float)mh.invokeExact(15.74f, 16.79f, upcallFunc.address());
		Assert.assertEquals(result, 32.53f, 0.01f);
	}

	@Test
	public void test_addFloatAndFloatFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(float.class, float.class, MemoryAddress.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_POINTER, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addFloatAndFloatFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemorySegment floatSegmt = MemorySegment.allocateNative(C_FLOAT);
		MemoryAccess.setFloat(floatSegmt, 6.79f);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addFloatAndFloatFromPointer,
				FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_POINTER));

		float result = (float)mh.invokeExact(5.74f, floatSegmt.address(), upcallFunc.address());
		Assert.assertEquals(result, 12.53f, 0.01f);
		floatSegmt.close();
	}

	@Test
	public void test_add2DoublesByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(double.class, double.class, double.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_DOUBLE, C_DOUBLE, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("add2DoublesByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_add2Doubles,
				FunctionDescriptor.of(C_DOUBLE, C_DOUBLE, C_DOUBLE));

		double result = (double)mh.invokeExact(159.748d, 262.795d, upcallFunc.address());
		Assert.assertEquals(result, 422.543d, 0.001d);
	}

	@Test
	public void test_addDoubleAndDoubleFromPointerByUpCallMH() throws Throwable {
		MethodType mt = MethodType.methodType(double.class, MemoryAddress.class, double.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_DOUBLE, C_POINTER);
		Symbol functionSymbol = nativeLib.lookup("addDoubleAndDoubleFromPointerByUpCallMH").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment doubleSegmt = MemorySegment.allocateNative(C_DOUBLE);
		MemoryAccess.setDouble(doubleSegmt, 1159.748d);
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_addDoubleAndDoubleFromPointer,
				FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_DOUBLE));

		double result = (double)mh.invokeExact(doubleSegmt.address(), 1262.795d, upcallFunc.address());
		Assert.assertEquals(result, 2422.543d, 0.001d);
		doubleSegmt.close();
	}

	@Test
	public void test_qsortByUpCallMH() throws Throwable {
		int expectedArray[] = {11, 12, 13, 14, 15, 16, 17};
		int expectedArrayLength = expectedArray.length;

		MethodType mt = MethodType.methodType(void.class, MemoryAddress.class, int.class, int.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_INT, C_POINTER);
		Symbol functionSymbol = defaultLib.lookup("qsort").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		MemorySegment arraySegmt =  NativeScope.boundedScope(28).allocateArray(C_INT, new int[]{17, 14, 13, 16, 15, 12, 11});
		MemorySegment upcallFunc = clinker.upcallStub(UpCallMethodHandles.MH_compare,
				FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));

		mh.invokeExact(arraySegmt.address(), 7, 4, upcallFunc.address());
		int[] sortedArray = arraySegmt.toIntArray();
		for (int index = 0; index < expectedArrayLength; index++) {
			Assert.assertEquals(sortedArray[index], expectedArray[index]);
		}
	}
}

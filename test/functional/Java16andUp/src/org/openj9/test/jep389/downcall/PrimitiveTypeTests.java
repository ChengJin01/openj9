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

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) DownCall for primitive types,
 * which covers generic tests, tests with the void type, the pointer type, and the vararg list.
 */
@Test(groups = { "level.sanity" })
public class PrimitiveTypeTests {
	private static boolean isWinOS = System.getProperty("os.name").toLowerCase().contains("win") ? true : false;
	private static ValueLayout longLayout = isWinOS ? C_LONG_LONG : C_LONG;
	private static LibraryLookup nativeLib = LibraryLookup.ofLibrary("clinkerffitests");
	private static LibraryLookup defaultLib = LibraryLookup.ofDefault();
	private static CLinker clinker = CLinker.getInstance();
	
	@Test
	public void test_addTwoBoolsWithOr() throws Throwable {
		MethodType mt = MethodType.methodType(boolean.class, boolean.class, boolean.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2BoolWithOr").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		boolean result = (boolean)mh.invokeExact(true, false);
		Assert.assertEquals(result, true);
	}
	
	@Test
	public void test_addTwoBoolObjectsWithOr() throws Throwable {
		MethodType mt = MethodType.methodType(Boolean.class, Boolean.class, Boolean.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2BoolWithOr").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Boolean result = (Boolean)mh.invokeExact((Boolean)false, (Boolean)true);
		Assert.assertEquals(result.booleanValue(), true);
	}
	
	@Test
	public void test_generateNewChar() throws Throwable {
		MethodType mt = MethodType.methodType(char.class, char.class, char.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT);
		Symbol functionSymbol = nativeLib.lookup("testGenNewChar").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		char result = (char)mh.invokeExact('B', 'D');
		Assert.assertEquals(result, 'C');
	}
	
	@Test
	public void test_generateNewCharObject() throws Throwable {
		MethodType mt = MethodType.methodType(Character.class, Character.class, Character.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT);
		Symbol functionSymbol = nativeLib.lookup("testGenNewChar").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Character result = (Character)mh.invokeExact((Character)'F', (Character)'C');
		Assert.assertEquals(result.charValue(), 'D');
	}
	
	@Test
	public void test_addTwoBytes() throws Throwable {
		MethodType mt = MethodType.methodType(byte.class, byte.class, byte.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_CHAR, C_CHAR);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Byte").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		byte result = (byte)mh.invokeExact((byte)6, (byte)3);
		Assert.assertEquals(result, (byte)9);
	}
	
	@Test
	public void test_addTwoByteObjects() throws Throwable {
		MethodType mt = MethodType.methodType(Byte.class, Byte.class, Byte.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_CHAR, C_CHAR);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Byte").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Byte result = (Byte)mh.invokeExact(Byte.valueOf((byte)8), Byte.valueOf((byte)5));
		Assert.assertEquals(result.byteValue(), (byte)13);
	}
	
	@Test
	public void test_addTwoShorts() throws Throwable {
		MethodType mt = MethodType.methodType(short.class, short.class, short.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Short").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		short result = (short)mh.invokeExact((short)24, (short)32);
		Assert.assertEquals(result, (short)56);
	}
	
	@Test
	public void test_addTwoShortObjects() throws Throwable {
		MethodType mt = MethodType.methodType(Short.class, Short.class, Short.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_SHORT, C_SHORT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Short").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Short result = (Short)mh.invokeExact(Short.valueOf((short)56), Short.valueOf((short)42));
		Assert.assertEquals(result.shortValue(), (short)98);
	}
	
	@Test
	public void test_addTwoInts() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, int.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Int").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		int result = (int)mh.invokeExact(112, 123);
		Assert.assertEquals(result, 235);
	}
	
	@Test
	public void test_addTwoIntObjects() throws Throwable {
		MethodType mt = MethodType.methodType(Integer.class,Integer.class, Integer.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_INT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Int").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Integer result = (Integer)mh.invokeExact((Integer)234, (Integer)245);
		Assert.assertEquals(result.intValue(), 479);
	}
	
	@Test
	public void test_addIntAndChar() throws Throwable {
		MethodType mt = MethodType.methodType(int.class, int.class, char.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_SHORT);
		Symbol functionSymbol = nativeLib.lookup("testAddIntAndChar").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		int result = (int)mh.invokeExact(58, 'A');
		Assert.assertEquals(result, 123);
	}
	
	@Test
	public void test_addIntObjectAndCharObject() throws Throwable {
		MethodType mt = MethodType.methodType(Integer.class, Integer.class, Character.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_SHORT);
		Symbol functionSymbol = nativeLib.lookup("testAddIntAndChar").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Integer result = (Integer)mh.invokeExact((Integer)276, (Character)'E');
		Assert.assertEquals(result.intValue(), 345);
	}
	
	@Test
	public void test_addTwoLongs() throws Throwable {
		MethodType mt = MethodType.methodType(long.class, long.class, long.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, longLayout, longLayout);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Long").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		long result = (long)mh.invokeExact(57424L, 698235L);
		Assert.assertEquals(result, 755659L);
	}
	
	@Test
	public void test_addTwoLongObjects() throws Throwable {
		MethodType mt = MethodType.methodType(Long.class, Long.class, Long.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, longLayout, longLayout);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Long").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Long result = (Long)mh.invokeExact((Long)257423L, (Long)235726L);
		Assert.assertEquals(result.longValue(), 493149L);
	}
	
	@Test
	public void test_addTwoFloats() throws Throwable {
		MethodType mt = MethodType.methodType(float.class, float.class, float.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_FLOAT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Float").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		float result = (float)mh.invokeExact(5.74f, 6.79f);
		Assert.assertEquals(result, 12.53f, 0.01f);
	}
	
	@Test
	public void test_addTwoFloatObjects() throws Throwable {
		MethodType mt = MethodType.methodType(Float.class, Float.class, Float.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_FLOAT, C_FLOAT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Float").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Float result = (Float)mh.invokeExact((Float)15.03f, (Float)16.09f);
		Assert.assertEquals(result.floatValue(), 31.12f, 0.01f);
	}
	
	@Test
	public void test_addTwoDoubles() throws Throwable {
		MethodType mt = MethodType.methodType(double.class, double.class, double.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_DOUBLE, C_DOUBLE);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Double").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		double result = (double)mh.invokeExact(159.748d, 262.795d);
		Assert.assertEquals(result, 422.543d, 0.001d);
	}
	
	@Test
	public void test_addTwoDoubleObjects() throws Throwable {
		MethodType mt = MethodType.methodType(Double.class, Double.class, Double.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_DOUBLE, C_DOUBLE);
		Symbol functionSymbol = nativeLib.lookup("testAdd2Double").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		Double result = (Double)mh.invokeExact((Double)259.748d, (Double)362.797d);
		Assert.assertEquals(result.doubleValue(), 622.545d, 0.001d);
	}
	
	@Test
	public void test_addTwoIntsReturnVoid() throws Throwable {
		MethodType mt = MethodType.methodType(void.class, int.class, int.class);
		FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_INT, C_INT);
		Symbol functionSymbol = nativeLib.lookup("testAdd2IntReturnVoid").get();
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		mh.invokeExact(454, 398);
	}
	
	@Test
	public void test_strlenFromDefaultLibWithMemAddr() throws Throwable {
		Symbol strlenSymbol = defaultLib.lookup("strlen").get();
		MethodType mt = MethodType.methodType(long.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(strlenSymbol, mt, fd);
		MemorySegment funcMemSegment = CLinker.toCString("JEP389 DOWNCALL TEST SUITES");
		long strLength = (long)mh.invokeExact(funcMemSegment.address());
		Assert.assertEquals(strLength, 27);
	}
	
	@Test
	public void test_memoryAllocFreeFromDefaultLib() throws Throwable {
		Symbol allocSymbol = defaultLib.lookup("malloc").get();
		MethodType allocMethodType = MethodType.methodType(MemoryAddress.class, long.class);
		FunctionDescriptor allocFuncDesc = FunctionDescriptor.of(C_POINTER, longLayout);
		MethodHandle allocHandle = clinker.downcallHandle(allocSymbol, allocMethodType, allocFuncDesc);
		MemoryAddress allocMemAddr = (MemoryAddress)allocHandle.invokeExact(10L);
		long allocMemAddrValue = allocMemAddr.toRawLongValue();
		
		MemorySegment memSeg = MemorySegment.ofNativeRestricted();
		MemoryAccess.setIntAtOffset(memSeg, allocMemAddrValue, 15);
		Assert.assertEquals(MemoryAccess.getIntAtOffset(memSeg, allocMemAddrValue), 15);
		
		Symbol freeSymbol = defaultLib.lookup("free").get();
		MethodType freeMethodType = MethodType.methodType(void.class, MemoryAddress.class);
		FunctionDescriptor freeFuncDesc = FunctionDescriptor.ofVoid(C_POINTER);
		MethodHandle freeHandle = clinker.downcallHandle(freeSymbol, freeMethodType, freeFuncDesc);
		freeHandle.invokeExact(allocMemAddr);
	}
	
	@Test
	public void test_memoryAllocFreeFromCLinkerMethod() throws Throwable {
		MemoryAddress allocMemAddr = CLinker.allocateMemoryRestricted(10L);
		long allocMemAddrValue = allocMemAddr.toRawLongValue();
		
		MemorySegment memSeg = MemorySegment.ofNativeRestricted();
		MemoryAccess.setIntAtOffset(memSeg, allocMemAddrValue, 49);
		Assert.assertEquals(MemoryAccess.getIntAtOffset(memSeg, allocMemAddrValue), 49);
		
		CLinker.freeMemoryRestricted(allocMemAddr);
	}
	
	@Test
	public void test_printfFromDefaultLibWithMemAddr() throws Throwable {
		Symbol functionSymbol = defaultLib.lookup("printf").get();
		MethodType mt = MethodType.methodType(int.class, MemoryAddress.class, int.class, int.class, int.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT, C_INT);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
		MemorySegment formatMemSegment = CLinker.toCString("\n%d + %d = %d\n");
		mh.invoke(formatMemSegment.address(), 15, 27, 42);
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
												.vargFromInt(C_INT, 8).vargFromInt(C_INT, 56), nativeScope);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);
			mh.invoke(formatMemSegment.address(), vaList);
		}
	}
}

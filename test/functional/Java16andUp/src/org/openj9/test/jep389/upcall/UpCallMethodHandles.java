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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodType.methodType;

import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.*;
import static jdk.incubator.foreign.LibraryLookup.Symbol;
import jdk.incubator.foreign.CLinker.VaList;
import static jdk.incubator.foreign.CLinker.VaList.Builder;
import jdk.incubator.foreign.FunctionDescriptor;

import jdk.incubator.foreign.ValueLayout;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.NativeScope;

/**
 * The helper class that contains all upcall method handles with primitive types or struct
 * as arguments.
 */
public class UpCallMethodHandles {
	static final Lookup lookup = MethodHandles.lookup();

	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isAixOS = osName.contains("aix");
	private static boolean isWinOS = osName.contains("win");
	/* long long is 64 bits on AIX/ppc64, which is the same as Windows */
	private static ValueLayout longLayout = (isWinOS || isAixOS) ? C_LONG_LONG : C_LONG;

	static final MethodType MT_Bool_Bool_MemSegmt = methodType(boolean.class, boolean.class, MemorySegment.class);
	static final MethodType MT_Byte_Byte_MemSegmt = methodType(byte.class, byte.class, MemorySegment.class);
	static final MethodType MT_Char_Char_MemSegmt = methodType(char.class, char.class, MemorySegment.class);
	static final MethodType MT_Short_Short_MemSegmt = methodType(short.class, short.class, MemorySegment.class);
	static final MethodType MT_Int_Int_MemSegmt = methodType(int.class, int.class, MemorySegment.class);
	static final MethodType MT_Long_Long_MemSegmt = methodType(long.class, long.class, MemorySegment.class);
	static final MethodType MT_Long_Int_MemSegmt = methodType(long.class, int.class, MemorySegment.class);
	static final MethodType MT_Float_Float_MemSegmt = methodType(float.class, float.class, MemorySegment.class);
	static final MethodType MT_Double_Double_MemSegmt = methodType(double.class, double.class, MemorySegment.class);

	static final MethodType MT_MemAddr_MemAddr_MemSegmt = methodType(MemoryAddress.class, MemoryAddress.class, MemorySegment.class);
	static final MethodType MT_MemSegmt_MemSegmt_MemSegmt = methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class);

	public static final MethodHandle MH_add2BoolsWithOr;
	public static final MethodHandle MH_addBoolAndBoolFromPointerWithOr;
	public static final MethodHandle MH_createNewCharFrom2Chars;
	public static final MethodHandle MH_createNewCharFromCharAndCharFromPointer;
	public static final MethodHandle MH_add2Bytes;
	public static final MethodHandle MH_addByteAndByteFromPointer;
	public static final MethodHandle MH_add2Shorts;
	public static final MethodHandle MH_addShortAndShortFromPointer;
	public static final MethodHandle MH_add2Ints;
	public static final MethodHandle MH_addIntAndIntFromPointer;
	public static final MethodHandle MH_add3Ints;
	public static final MethodHandle MH_addIntsFromVaList;
	public static final MethodHandle MH_addIntAndChar;
	public static final MethodHandle MH_add2IntsReturnVoid;
	public static final MethodHandle MH_add2Longs;
	public static final MethodHandle MH_addLongAndLongFromPointer;
	public static final MethodHandle MH_addLongsFromVaList;
	public static final MethodHandle MH_add2Floats;
	public static final MethodHandle MH_addFloatAndFloatFromPointer;
	public static final MethodHandle MH_add2Doubles;
	public static final MethodHandle MH_addDoubleAndDoubleFromPointer;
	public static final MethodHandle MH_addDoublesFromVaList;
	public static final MethodHandle MH_compare;

	public static final MethodHandle MH_addBoolAndBoolsFromStructWithXor;
	public static final MethodHandle MH_addBoolFromPointerAndBoolsFromStructWithXor;
	public static final MethodHandle MH_addBoolFromPointerAndBoolsFromStructWithXor_returnBoolPointer;
	public static final MethodHandle MH_addBoolAndBoolsFromStructPointerWithXor;
	public static final MethodHandle MH_addBoolAndBoolsFromNestedStructWithXor;
	public static final MethodHandle MH_addBoolAndBoolsFromNestedStructWithXor_reverseOrder;
	public static final MethodHandle MH_addBoolAndBoolsFromStructWithNestedBoolArray;
	public static final MethodHandle MH_addBoolAndBoolsFromStructWithNestedBoolArray_reverseOrder;
	public static final MethodHandle MH_addBoolAndBoolsFromStructWithNestedStructArray;
	public static final MethodHandle MH_addBoolAndBoolsFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2BoolStructsWithXor_returnStruct;
	public static final MethodHandle MH_add2BoolStructsWithXor_returnStructPointer;
	public static final MethodHandle MH_add3BoolStructsWithXor_returnStruct;

	public static final MethodHandle MH_addByteAndBytesFromStruct;
	public static final MethodHandle MH_addByteFromPointerAndBytesFromStruct;
	public static final MethodHandle MH_addByteFromPointerAndBytesFromStruct_returnBytePointer;
	public static final MethodHandle MH_addByteAndBytesFromStructPointer;
	public static final MethodHandle MH_addByteAndBytesFromNestedStruct;
	public static final MethodHandle MH_addByteAndBytesFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addByteAndBytesFromStructWithNestedByteArray;
	public static final MethodHandle MH_addByteAndBytesFromStructWithNestedByteArray_reverseOrder;
	public static final MethodHandle MH_addByteAndBytesFromStructWithNestedStructArray;
	public static final MethodHandle MH_addByteAndBytesFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2ByteStructs_returnStruct;
	public static final MethodHandle MH_add2ByteStructs_returnStructPointer;
	public static final MethodHandle MH_add3ByteStructs_returnStruct;

	public static final MethodHandle MH_addCharAndCharsFromStruct;
	public static final MethodHandle MH_addCharFromPointerAndCharsFromStruct;
	public static final MethodHandle MH_addCharFromPointerAndCharsFromStruct_returnCharPointer;
	public static final MethodHandle MH_addCharAndCharsFromStructPointer;
	public static final MethodHandle MH_addCharAndCharsFromNestedStruct;
	public static final MethodHandle MH_addCharAndCharsFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addCharAndCharsFromStructWithNestedCharArray;
	public static final MethodHandle MH_addCharAndCharsFromStructWithNestedCharArray_reverseOrder;
	public static final MethodHandle MH_addCharAndCharsFromStructWithNestedStructArray;
	public static final MethodHandle MH_addCharAndCharsFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2CharStructs_returnStruct;
	public static final MethodHandle MH_add2CharStructs_returnStructPointer;
	public static final MethodHandle MH_add3CharStructs_returnStruct;

	public static final MethodHandle MH_addShortAndShortsFromStruct;
	public static final MethodHandle MH_addShortFromPointerAndShortsFromStruct;
	public static final MethodHandle MH_addShortFromPointerAndShortsFromStruct_returnShortPointer;
	public static final MethodHandle MH_addShortAndShortsFromStructPointer;
	public static final MethodHandle MH_addShortAndShortsFromNestedStruct;
	public static final MethodHandle MH_addShortAndShortsFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addShortAndShortsFromStructWithNestedShortArray;
	public static final MethodHandle MH_addShortAndShortsFromStructWithNestedShortArray_reverseOrder;
	public static final MethodHandle MH_addShortAndShortsFromStructWithNestedStructArray;
	public static final MethodHandle MH_addShortAndShortsFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2ShortStructs_returnStruct;
	public static final MethodHandle MH_add2ShortStructs_returnStructPointer;
	public static final MethodHandle MH_add3ShortStructs_returnStruct;

	public static final MethodHandle MH_addIntAndIntsFromStruct;
	public static final MethodHandle MH_addIntAndIntShortFromStruct;
	public static final MethodHandle MH_addIntAndShortIntFromStruct;
	public static final MethodHandle MH_addIntFromPointerAndIntsFromStruct;
	public static final MethodHandle MH_addIntFromPointerAndIntsFromStruct_returnIntPointer;
	public static final MethodHandle MH_addIntAndIntsFromStructPointer;
	public static final MethodHandle MH_addIntAndIntsFromNestedStruct;
	public static final MethodHandle MH_addIntAndIntsFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addIntAndIntsFromStructWithNestedIntArray;
	public static final MethodHandle MH_addIntAndIntsFromStructWithNestedIntArray_reverseOrder;
	public static final MethodHandle MH_addIntAndIntsFromStructWithNestedStructArray;
	public static final MethodHandle MH_addIntAndIntsFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2IntStructs_returnStruct;
	public static final MethodHandle MH_add2IntStructs_returnStructPointer;
	public static final MethodHandle MH_add3IntStructs_returnStruct;

	public static final MethodHandle MH_addLongAndLongsFromStruct;
	public static final MethodHandle MH_addIntAndIntLongFromStruct;
	public static final MethodHandle MH_addIntAndLongIntFromStruct;
	public static final MethodHandle MH_addLongFromPointerAndLongsFromStruct;
	public static final MethodHandle MH_addLongFromPointerAndLongsFromStruct_returnLongPointer;
	public static final MethodHandle MH_addLongAndLongsFromStructPointer;
	public static final MethodHandle MH_addLongAndLongsFromNestedStruct;
	public static final MethodHandle MH_addLongAndLongsFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addLongAndLongsFromStructWithNestedLongArray;
	public static final MethodHandle MH_addLongAndLongsFromStructWithNestedLongArray_reverseOrder;
	public static final MethodHandle MH_addLongAndLongsFromStructWithNestedStructArray;
	public static final MethodHandle MH_addLongAndLongsFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2LongStructs_returnStruct;
	public static final MethodHandle MH_add2LongStructs_returnStructPointer;
	public static final MethodHandle MH_add3LongStructs_returnStruct;

	public static final MethodHandle MH_addFloatAndFloatsFromStruct;
	public static final MethodHandle MH_addFloatFromPointerAndFloatsFromStruct;
	public static final MethodHandle MH_addFloatFromPointerAndFloatsFromStruct_returnFloatPointer;
	public static final MethodHandle MH_addFloatAndFloatsFromStructPointer;
	public static final MethodHandle MH_addFloatAndFloatsFromNestedStruct;
	public static final MethodHandle MH_addFloatAndFloatsFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addFloatAndFloatsFromStructWithNestedFloatArray;
	public static final MethodHandle MH_addFloatAndFloatsFromStructWithNestedFloatArray_reverseOrder;
	public static final MethodHandle MH_addFloatAndFloatsFromStructWithNestedStructArray;
	public static final MethodHandle MH_addFloatAndFloatsFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2FloatStructs_returnStruct;
	public static final MethodHandle MH_add2FloatStructs_returnStructPointer;
	public static final MethodHandle MH_add3FloatStructs_returnStruct;

	public static final MethodHandle MH_addDoubleAndDoublesFromStruct;
	public static final MethodHandle MH_addDoubleAndFloatDoubleFromStruct;
	public static final MethodHandle MH_addDoubleAndIntDoubleFromStruct;
	public static final MethodHandle MH_addDoubleAndDoubleFloatFromStruct;
	public static final MethodHandle MH_addDoubleAndDoubleIntFromStruct;
	public static final MethodHandle MH_addDoubleFromPointerAndDoublesFromStruct;
	public static final MethodHandle MH_addDoubleFromPointerAndDoublesFromStruct_returnDoublePointer;
	public static final MethodHandle MH_addDoubleAndDoublesFromStructPointer;
	public static final MethodHandle MH_addDoubleAndDoublesFromNestedStruct;
	public static final MethodHandle MH_addDoubleAndDoublesFromNestedStruct_reverseOrder;
	public static final MethodHandle MH_addDoubleAndDoublesFromStructWithNestedDoubleArray;
	public static final MethodHandle MH_addDoubleAndDoublesFromStructWithNestedDoubleArray_reverseOrder;
	public static final MethodHandle MH_addDoubleAndDoublesFromStructWithNestedStructArray;
	public static final MethodHandle MH_addDoubleAndDoublesFromStructWithNestedStructArray_reverseOrder;
	public static final MethodHandle MH_add2DoubleStructs_returnStruct;
	public static final MethodHandle MH_add2DoubleStructs_returnStructPointer;
	public static final MethodHandle MH_add3DoubleStructs_returnStruct;

	static {
		try {
			MH_add2BoolsWithOr = lookup.findStatic(UpCallMethodHandles.class, "add2BoolsWithOr", methodType(boolean.class, boolean.class, boolean.class)); //$NON-NLS-1$
			MH_addBoolAndBoolFromPointerWithOr = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolFromPointerWithOr", methodType(boolean.class, boolean.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_createNewCharFrom2Chars = lookup.findStatic(UpCallMethodHandles.class, "createNewCharFrom2Chars", methodType(char.class, char.class, char.class)); //$NON-NLS-1$
			MH_createNewCharFromCharAndCharFromPointer = lookup.findStatic(UpCallMethodHandles.class, "createNewCharFromCharAndCharFromPointer", methodType(char.class, MemoryAddress.class, char.class)); //$NON-NLS-1$
			MH_add2Bytes = lookup.findStatic(UpCallMethodHandles.class, "add2Bytes", methodType(byte.class, byte.class, byte.class)); //$NON-NLS-1$
			MH_addByteAndByteFromPointer = lookup.findStatic(UpCallMethodHandles.class, "addByteAndByteFromPointer", methodType(byte.class, byte.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_add2Shorts = lookup.findStatic(UpCallMethodHandles.class, "add2Shorts", methodType(short.class, short.class, short.class)); //$NON-NLS-1$
			MH_addShortAndShortFromPointer = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortFromPointer", methodType(short.class, MemoryAddress.class, short.class)); //$NON-NLS-1$
			MH_add2Ints = lookup.findStatic(UpCallMethodHandles.class, "add2Ints", methodType(int.class, int.class, int.class)); //$NON-NLS-1$
			MH_addIntAndIntFromPointer = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntFromPointer", methodType(int.class, int.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_add3Ints = lookup.findStatic(UpCallMethodHandles.class, "add3Ints", methodType(int.class, int.class, int.class, int.class)); //$NON-NLS-1$
			MH_addIntsFromVaList = lookup.findStatic(UpCallMethodHandles.class, "addIntsFromVaList", methodType(int.class, int.class, VaList.class)); //$NON-NLS-1$
			MH_addIntAndChar = lookup.findStatic(UpCallMethodHandles.class, "addIntAndChar", methodType(int.class, int.class, char.class)); //$NON-NLS-1$
			MH_add2IntsReturnVoid = lookup.findStatic(UpCallMethodHandles.class, "add2IntsReturnVoid", methodType(void.class, int.class, int.class)); //$NON-NLS-1$
			MH_add2Longs = lookup.findStatic(UpCallMethodHandles.class, "add2Longs", methodType(long.class, long.class, long.class)); //$NON-NLS-1$
			MH_addLongAndLongFromPointer = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongFromPointer", methodType(long.class, MemoryAddress.class, long.class)); //$NON-NLS-1$
			MH_addLongsFromVaList = lookup.findStatic(UpCallMethodHandles.class, "addLongsFromVaList", methodType(long.class, int.class, VaList.class)); //$NON-NLS-1$
			MH_add2Floats = lookup.findStatic(UpCallMethodHandles.class, "add2Floats", methodType(float.class, float.class, float.class)); //$NON-NLS-1$
			MH_addFloatAndFloatFromPointer = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatFromPointer", methodType(float.class, float.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_add2Doubles = lookup.findStatic(UpCallMethodHandles.class, "add2Doubles", methodType(double.class, double.class, double.class)); //$NON-NLS-1$
			MH_addDoubleAndDoubleFromPointer = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoubleFromPointer", methodType(double.class, MemoryAddress.class, double.class)); //$NON-NLS-1$
			MH_addDoublesFromVaList = lookup.findStatic(UpCallMethodHandles.class, "addDoublesFromVaList", methodType(double.class, int.class, VaList.class)); //$NON-NLS-1$
			MH_compare = lookup.findStatic(UpCallMethodHandles.class, "compare", methodType(int.class, MemoryAddress.class, MemoryAddress.class)); //$NON-NLS-1$

			MH_addBoolAndBoolsFromStructWithXor = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromStructWithXor", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_addBoolFromPointerAndBoolsFromStructWithXor = lookup.findStatic(UpCallMethodHandles.class, "addBoolFromPointerAndBoolsFromStructWithXor", methodType(boolean.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addBoolFromPointerAndBoolsFromStructWithXor_returnBoolPointer = lookup.findStatic(UpCallMethodHandles.class, "addBoolFromPointerAndBoolsFromStructWithXor_returnBoolPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addBoolAndBoolsFromStructPointerWithXor = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromStructPointerWithXor", methodType(boolean.class, boolean.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addBoolAndBoolsFromNestedStructWithXor = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromNestedStructWithXor", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_addBoolAndBoolsFromNestedStructWithXor_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromNestedStructWithXor_reverseOrder", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_addBoolAndBoolsFromStructWithNestedBoolArray = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromStructWithNestedBoolArray", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_addBoolAndBoolsFromStructWithNestedBoolArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromStructWithNestedBoolArray_reverseOrder", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_addBoolAndBoolsFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromStructWithNestedStructArray", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_addBoolAndBoolsFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addBoolAndBoolsFromStructWithNestedStructArray_reverseOrder", MT_Bool_Bool_MemSegmt); //$NON-NLS-1$
			MH_add2BoolStructsWithXor_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2BoolStructsWithXor_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2BoolStructsWithXor_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2BoolStructsWithXor_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3BoolStructsWithXor_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3BoolStructsWithXor_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addByteAndBytesFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromStruct", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_addByteFromPointerAndBytesFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addByteFromPointerAndBytesFromStruct", methodType(byte.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addByteFromPointerAndBytesFromStruct_returnBytePointer = lookup.findStatic(UpCallMethodHandles.class, "addByteFromPointerAndBytesFromStruct_returnBytePointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addByteAndBytesFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromStructPointer", methodType(byte.class, byte.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addByteAndBytesFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromNestedStruct", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_addByteAndBytesFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromNestedStruct_reverseOrder", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_addByteAndBytesFromStructWithNestedByteArray = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromStructWithNestedByteArray", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_addByteAndBytesFromStructWithNestedByteArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromStructWithNestedByteArray_reverseOrder", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_addByteAndBytesFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromStructWithNestedStructArray", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_addByteAndBytesFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addByteAndBytesFromStructWithNestedStructArray_reverseOrder", MT_Byte_Byte_MemSegmt); //$NON-NLS-1$
			MH_add2ByteStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2ByteStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2ByteStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2ByteStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3ByteStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3ByteStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addCharAndCharsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromStruct", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_addCharFromPointerAndCharsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addCharFromPointerAndCharsFromStruct", methodType(char.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addCharFromPointerAndCharsFromStruct_returnCharPointer = lookup.findStatic(UpCallMethodHandles.class, "addCharFromPointerAndCharsFromStruct_returnCharPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addCharAndCharsFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromStructPointer", methodType(char.class, char.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addCharAndCharsFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromNestedStruct", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_addCharAndCharsFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromNestedStruct_reverseOrder", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_addCharAndCharsFromStructWithNestedCharArray = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromStructWithNestedCharArray", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_addCharAndCharsFromStructWithNestedCharArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromStructWithNestedCharArray_reverseOrder", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_addCharAndCharsFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromStructWithNestedStructArray", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_addCharAndCharsFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addCharAndCharsFromStructWithNestedStructArray_reverseOrder", MT_Char_Char_MemSegmt); //$NON-NLS-1$
			MH_add2CharStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2CharStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2CharStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2CharStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3CharStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3CharStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addShortAndShortsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromStruct", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_addShortFromPointerAndShortsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addShortFromPointerAndShortsFromStruct", methodType(short.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addShortFromPointerAndShortsFromStruct_returnShortPointer = lookup.findStatic(UpCallMethodHandles.class, "addShortFromPointerAndShortsFromStruct_returnShortPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addShortAndShortsFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromStructPointer", methodType(short.class, short.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addShortAndShortsFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromNestedStruct", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_addShortAndShortsFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromNestedStruct_reverseOrder", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_addShortAndShortsFromStructWithNestedShortArray = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromStructWithNestedShortArray", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_addShortAndShortsFromStructWithNestedShortArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromStructWithNestedShortArray_reverseOrder", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_addShortAndShortsFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromStructWithNestedStructArray", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_addShortAndShortsFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addShortAndShortsFromStructWithNestedStructArray_reverseOrder", MT_Short_Short_MemSegmt); //$NON-NLS-1$
			MH_add2ShortStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2ShortStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2ShortStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2ShortStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3ShortStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3ShortStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addIntAndIntsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromStruct", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntShortFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntShortFromStruct", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndShortIntFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntAndShortIntFromStruct", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntFromPointerAndIntsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntFromPointerAndIntsFromStruct", methodType(int.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addIntFromPointerAndIntsFromStruct_returnIntPointer = lookup.findStatic(UpCallMethodHandles.class, "addIntFromPointerAndIntsFromStruct_returnIntPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntsFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromStructPointer", methodType(int.class, int.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addIntAndIntsFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromNestedStruct", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntsFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromNestedStruct_reverseOrder", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntsFromStructWithNestedIntArray = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromStructWithNestedIntArray", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntsFromStructWithNestedIntArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromStructWithNestedIntArray_reverseOrder", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntsFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromStructWithNestedStructArray", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntsFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntsFromStructWithNestedStructArray_reverseOrder", MT_Int_Int_MemSegmt); //$NON-NLS-1$
			MH_add2IntStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2IntStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2IntStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2IntStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3IntStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3IntStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addLongAndLongsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromStruct", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_addIntAndIntLongFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntAndIntLongFromStruct", MT_Long_Int_MemSegmt); //$NON-NLS-1$
			MH_addIntAndLongIntFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addIntAndLongIntFromStruct", MT_Long_Int_MemSegmt); //$NON-NLS-1$
			MH_addLongFromPointerAndLongsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addLongFromPointerAndLongsFromStruct", methodType(long.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addLongFromPointerAndLongsFromStruct_returnLongPointer = lookup.findStatic(UpCallMethodHandles.class, "addLongFromPointerAndLongsFromStruct_returnLongPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addLongAndLongsFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromStructPointer", methodType(long.class, long.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addLongAndLongsFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromNestedStruct", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_addLongAndLongsFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromNestedStruct_reverseOrder", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_addLongAndLongsFromStructWithNestedLongArray = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromStructWithNestedLongArray", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_addLongAndLongsFromStructWithNestedLongArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromStructWithNestedLongArray_reverseOrder", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_addLongAndLongsFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromStructWithNestedStructArray", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_addLongAndLongsFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addLongAndLongsFromStructWithNestedStructArray_reverseOrder", MT_Long_Long_MemSegmt); //$NON-NLS-1$
			MH_add2LongStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2LongStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2LongStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2LongStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3LongStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3LongStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addFloatAndFloatsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromStruct", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_addFloatFromPointerAndFloatsFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addFloatFromPointerAndFloatsFromStruct", methodType(float.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addFloatFromPointerAndFloatsFromStruct_returnFloatPointer = lookup.findStatic(UpCallMethodHandles.class, "addFloatFromPointerAndFloatsFromStruct_returnFloatPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addFloatAndFloatsFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromStructPointer", methodType(float.class, float.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addFloatAndFloatsFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromNestedStruct", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_addFloatAndFloatsFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromNestedStruct_reverseOrder", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_addFloatAndFloatsFromStructWithNestedFloatArray = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromStructWithNestedFloatArray", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_addFloatAndFloatsFromStructWithNestedFloatArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromStructWithNestedFloatArray_reverseOrder", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_addFloatAndFloatsFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromStructWithNestedStructArray", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_addFloatAndFloatsFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addFloatAndFloatsFromStructWithNestedStructArray_reverseOrder", MT_Float_Float_MemSegmt); //$NON-NLS-1$
			MH_add2FloatStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2FloatStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2FloatStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2FloatStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3FloatStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3FloatStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$

			MH_addDoubleAndDoublesFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromStruct", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndFloatDoubleFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndFloatDoubleFromStruct", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndIntDoubleFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndIntDoubleFromStruct", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoubleFloatFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoubleFloatFromStruct", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoubleIntFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoubleIntFromStruct", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleFromPointerAndDoublesFromStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleFromPointerAndDoublesFromStruct", methodType(double.class, MemoryAddress.class, MemorySegment.class)); //$NON-NLS-1$
			MH_addDoubleFromPointerAndDoublesFromStruct_returnDoublePointer = lookup.findStatic(UpCallMethodHandles.class, "addDoubleFromPointerAndDoublesFromStruct_returnDoublePointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromStructPointer = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromStructPointer", methodType(double.class, double.class, MemoryAddress.class)); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromNestedStruct = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromNestedStruct", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromNestedStruct_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromNestedStruct_reverseOrder", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromStructWithNestedDoubleArray = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromStructWithNestedDoubleArray", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromStructWithNestedDoubleArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromStructWithNestedDoubleArray_reverseOrder", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromStructWithNestedStructArray = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromStructWithNestedStructArray", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_addDoubleAndDoublesFromStructWithNestedStructArray_reverseOrder = lookup.findStatic(UpCallMethodHandles.class, "addDoubleAndDoublesFromStructWithNestedStructArray_reverseOrder", MT_Double_Double_MemSegmt); //$NON-NLS-1$
			MH_add2DoubleStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add2DoubleStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
			MH_add2DoubleStructs_returnStructPointer = lookup.findStatic(UpCallMethodHandles.class, "add2DoubleStructs_returnStructPointer", MT_MemAddr_MemAddr_MemSegmt); //$NON-NLS-1$
			MH_add3DoubleStructs_returnStruct = lookup.findStatic(UpCallMethodHandles.class, "add3DoubleStructs_returnStruct", MT_MemSegmt_MemSegmt_MemSegmt); //$NON-NLS-1$
		} catch (IllegalAccessException | NoSuchMethodException e) {
			throw new InternalError(e);
		}
	}
	public static boolean integerToBool(int value) {
		return (value != 0);
	}

	public static int boolToInteger(boolean value) {
		return (value == true) ? 1 : 0;
	}

	public static int compare(MemoryAddress argAddr1, MemoryAddress argAddr2) {
		int value1 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), argAddr1.toRawLongValue());
		int value2 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), argAddr2.toRawLongValue());
		return (value1 - value2);
	}

	public static boolean add2BoolsWithOr(boolean boolArg1, boolean boolArg2) {
		boolean result = boolArg1 || boolArg2;
		return result;
	}

	public static boolean addBoolAndBoolFromPointerWithOr(boolean boolArg1, MemoryAddress boolArg2Addr) {
		int boolArg2 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), boolArg2Addr.toRawLongValue());
		boolean result = boolArg1 || integerToBool(boolArg2);
		return result;
	}

	public static char createNewCharFrom2Chars(char charArg1, char charArg2) {
		int diff = (charArg2 >= charArg1) ? (charArg2 - charArg1) : (charArg1 - charArg2);
		diff = (diff > 5) ? 5 : diff;
		char result = (char)(diff + 'A');
		return result;
	}

	public static char createNewCharFromCharAndCharFromPointer(MemoryAddress charArg1Addr, char charArg2) {
		short charArg1 = MemoryAccess.getShortAtOffset(MemorySegment.ofNativeRestricted(), charArg1Addr.toRawLongValue());
		int diff = (charArg2 >= charArg1) ? (charArg2 - charArg1) : (charArg1 - charArg2);
		diff = (diff > 5) ? 5 : diff;
		char result = (char)(diff + 'A');
		return result;
	}

	public static byte add2Bytes(byte byteArg1, byte byteArg2) {
		byte byteSum = (byte)(byteArg1 + byteArg2);
		return byteSum;
	}

	public static byte addByteAndByteFromPointer(byte byteArg1, MemoryAddress byteArg2Addr) {
		byte byteArg2 = MemoryAccess.getByteAtOffset(MemorySegment.ofNativeRestricted(), byteArg2Addr.toRawLongValue());
		byte byteSum = (byte)(byteArg1 + byteArg2);
		return byteSum;
	}

	public static short add2Shorts(short shortArg1, short shortArg2) {
		short shortSum = (short)(shortArg1 + shortArg2);
		return shortSum;
	}

	public static short addShortAndShortFromPointer(MemoryAddress shortArg1Addr, short shortArg2) {
		short shortArg1 = MemoryAccess.getShortAtOffset(MemorySegment.ofNativeRestricted(), shortArg1Addr.toRawLongValue());
		short shortSum = (short)(shortArg1 + shortArg2);
		return shortSum;
	}

	public static int add2Ints(int intArg1, int intArg2) {
		int intSum = intArg1 + intArg2;
		return intSum;
	}

	public static int addIntAndIntFromPointer(int intArg1, MemoryAddress intArg2Addr) {
		int intSum = intArg1 + MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), intArg2Addr.toRawLongValue());;
		return intSum;
	}

	public static int add3Ints(int intArg1, int intArg2, int intArg3) {
		int intSum = intArg1 + intArg2 + intArg3;
		return intSum;
	}

	public static int addIntsFromVaList(int intCount, VaList intVaList) {
		int intSum = 0;
		while (intCount > 0) {
			intSum += intVaList.vargAsInt(C_INT);
			intCount--;
		}
		return intSum;
	}

	public static int addIntAndChar(int intArg, char charArg) {
		int sum = intArg + charArg;
		return sum;
	}

	public static void add2IntsReturnVoid(int intArg1, int intArg2) {
		int intSum = intArg1 + intArg2;
		System.out.println("add2IntsReturnVoid: intSum = " + intSum + "\n");
	}

	public static long add2Longs(long longArg1, long longArg2) {
		long longSum = longArg1 + longArg2;
		return longSum;
	}

	public static long addLongAndLongFromPointer(MemoryAddress longArg1Addr, long longArg2) {
		long longArg1 = MemoryAccess.getLongAtOffset(MemorySegment.ofNativeRestricted(), longArg1Addr.toRawLongValue());
		long longSum = longArg1 + longArg2;
		return longSum;
	}

	public static long addLongsFromVaList(int longCount, VaList longVaList) {
		long longSum = 0;
		while (longCount > 0) {
			longSum += longVaList.vargAsLong(longLayout);
			longCount--;
		}
		return longSum;
	}

	public static float add2Floats(float floatArg1, float floatArg2) {
		float floatSum = floatArg1 + floatArg2;
		return floatSum;
	}

	public static float addFloatAndFloatFromPointer(float floatArg1, MemoryAddress floatArg2Addr) {
		float floatArg2 = MemoryAccess.getFloatAtOffset(MemorySegment.ofNativeRestricted(), floatArg2Addr.toRawLongValue());
		float floatSum = floatArg1 + floatArg2;
		return floatSum;
	}

	public static double add2Doubles(double doubleArg1, double doubleArg2) {
		double doubleSum = doubleArg1 + doubleArg2;
		return doubleSum;
	}

	public static double addDoubleAndDoubleFromPointer(MemoryAddress doubleArg1Addr, double doubleArg2) {
		double doubleArg1 = MemoryAccess.getDoubleAtOffset(MemorySegment.ofNativeRestricted(), doubleArg1Addr.toRawLongValue());
		double doubleSum = doubleArg1 + doubleArg2;
		return doubleSum;
	}

	public static double addDoublesFromVaList(int doubleCount, VaList doubleVaList) {
		double doubleSum = 0;
		while (doubleCount > 0) {
			doubleSum += doubleVaList.vargAsDouble(C_DOUBLE);
			doubleCount--;
		}
		return doubleSum;
	}

	public static boolean addBoolAndBoolsFromStructWithXor(boolean arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		boolean boolSum = arg1 ^ (boolean)elemHandle1.get(arg2) ^ (boolean)elemHandle2.get(arg2);
		return boolSum;
	}

	public static boolean addBoolFromPointerAndBoolsFromStructWithXor(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		int arg1 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		boolean boolSum = integerToBool(arg1) ^ (boolean)elemHandle1.get(arg2) ^ (boolean)elemHandle2.get(arg2);
		return boolSum;
	}

	public static MemoryAddress addBoolFromPointerAndBoolsFromStructWithXor_returnBoolPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		int arg1 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		boolean boolSum = integerToBool(arg1) ^ (boolean)elemHandle1.get(arg2) ^ (boolean)elemHandle2.get(arg2);
		MemoryAccess.setIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), boolToInteger(boolSum));
		return arg1Addr;
	}

	public static boolean addBoolAndBoolsFromStructPointerWithXor(boolean arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		boolean boolSum = arg1 ^ (boolean)boolHandle1.get(arg2) ^ (boolean)boolHandle2.get(arg2);
		return boolSum;
	}

	public static boolean addBoolAndBoolsFromNestedStructWithXor(boolean arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"), C_INT.withName("elem2"));

		boolean nestedStructElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 0));
		boolean nestedStructElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 4));
		boolean structElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 8));
		boolean boolSum = arg1 ^ nestedStructElem1 ^ nestedStructElem2 ^ structElem2;
		return boolSum;
	}

	public static boolean addBoolAndBoolsFromNestedStructWithXor_reverseOrder(boolean arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), nestedStructLayout.withName("struct_elem2"));

		boolean structElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 0));
		boolean nestedStructElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 4));
		boolean nestedStructElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 8));
		boolean boolSum = arg1 ^ structElem1 ^ nestedStructElem1 ^ nestedStructElem2;
		return boolSum;
	}

	public static boolean addBoolAndBoolsFromStructWithNestedBoolArray(boolean arg1, MemorySegment arg2) {
		SequenceLayout intArray = MemoryLayout.ofSequence(2, C_INT);
		GroupLayout structLayout = MemoryLayout.ofStruct(intArray.withName("array_elem1"), C_INT.withName("elem2"));

		boolean nestedBoolArrayElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 0));
		boolean nestedBoolArrayElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 4));
		boolean structElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 8));

		boolean boolSum = arg1 ^ nestedBoolArrayElem1 ^ nestedBoolArrayElem2 ^ structElem2;
		return boolSum;
	}

	public static boolean addBoolAndBoolsFromStructWithNestedBoolArray_reverseOrder(boolean arg1, MemorySegment arg2) {
		SequenceLayout intArray = MemoryLayout.ofSequence(2, C_INT);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), intArray.withName("array_elem2"));

		boolean structElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 0));
		boolean nestedBoolArrayElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 4));
		boolean nestedBoolArrayElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 8));

		boolean boolSum = arg1 ^ structElem1 ^ nestedBoolArrayElem1 ^ nestedBoolArrayElem2;
		return boolSum;
	}

	public static boolean addBoolAndBoolsFromStructWithNestedStructArray(boolean arg1, MemorySegment arg2) {
		GroupLayout intStruct = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, intStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"), C_INT.withName("elem2"));

		boolean nestedStructArrayElem1_Elem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 0));
		boolean nestedStructArrayElem1_Elem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 4));
		boolean nestedStructArrayElem2_Elem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 8));
		boolean nestedStructArrayElem2_Elem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 12));
		boolean structElem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 16));

		boolean boolSum = arg1 ^ structElem2
				^ nestedStructArrayElem1_Elem1 ^ nestedStructArrayElem1_Elem2
				^ nestedStructArrayElem2_Elem1 ^ nestedStructArrayElem2_Elem2;
		return boolSum;
	}

	public static boolean addBoolAndBoolsFromStructWithNestedStructArray_reverseOrder(boolean arg1, MemorySegment arg2) {
		GroupLayout intStruct = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, intStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), structArray.withName("struct_array_elem2"));

		boolean structElem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 0));
		boolean nestedStructArrayElem1_Elem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 4));
		boolean nestedStructArrayElem1_Elem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 8));
		boolean nestedStructArrayElem2_Elem1 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 12));
		boolean nestedStructArrayElem2_Elem2 = integerToBool(MemoryAccess.getIntAtOffset(arg2, 16));

		boolean boolSum = arg1 ^ structElem1
				^ nestedStructArrayElem1_Elem1 ^ nestedStructArrayElem1_Elem2
				^ nestedStructArrayElem2_Elem1 ^ nestedStructArrayElem2_Elem2;
		return boolSum;
	}

	public static MemorySegment add2BoolStructsWithXor_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		MemorySegment boolStructSegmt = MemorySegment.allocateNative(structLayout);
		boolean boolStruct_Elem1 = (boolean)boolHandle1.get(arg1) ^ (boolean)boolHandle1.get(arg2);
		boolean boolStruct_Elem2 = (boolean)boolHandle2.get(arg1) ^ (boolean)boolHandle2.get(arg2);
		boolHandle1.set(boolStructSegmt, boolToInteger(boolStruct_Elem1));
		boolHandle2.set(boolStructSegmt, boolToInteger(boolStruct_Elem2));
		return boolStructSegmt;
	}

	public static MemoryAddress add2BoolStructsWithXor_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		boolean boolStruct_Elem1 = (boolean)boolHandle1.get(arg1) ^ (boolean)boolHandle1.get(arg2);
		boolean boolStruct_Elem2 = (boolean)boolHandle2.get(arg1) ^ (boolean)boolHandle2.get(arg2);
		boolHandle1.set(arg1, boolToInteger(boolStruct_Elem1));
		boolHandle2.set(arg1, boolToInteger(boolStruct_Elem2));
		return arg1Addr;
	}

	public static MemorySegment add3BoolStructsWithXor_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"), C_INT.withName("elem3"));
		VarHandle boolHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle boolHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		VarHandle boolHandle3 = structLayout.varHandle(int.class, PathElement.groupElement("elem3"));

		MemorySegment boolStructSegmt = MemorySegment.allocateNative(structLayout);
		boolean boolStruct_Elem1 = (boolean)boolHandle1.get(arg1) ^ (boolean)boolHandle1.get(arg2);
		boolean boolStruct_Elem2 = (boolean)boolHandle2.get(arg1) ^ (boolean)boolHandle2.get(arg2);
		boolean boolStruct_Elem3 = (boolean)boolHandle3.get(arg1) ^ (boolean)boolHandle3.get(arg2);
		boolHandle1.set(boolStructSegmt, boolToInteger(boolStruct_Elem1));
		boolHandle2.set(boolStructSegmt, boolToInteger(boolStruct_Elem2));
		boolHandle3.set(boolStructSegmt, boolToInteger(boolStruct_Elem3));
		return boolStructSegmt;
	}

	public static byte addByteAndBytesFromStruct(byte arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

		byte byteSum = (byte)(arg1 + (byte)byteHandle1.get(arg2) + (byte)byteHandle2.get(arg2));
		return byteSum;
	}

	public static byte addByteFromPointerAndBytesFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

		byte arg1 = MemoryAccess.getByteAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		byte byteSum = (byte)(arg1 + (byte)byteHandle1.get(arg2) + (byte)byteHandle2.get(arg2));
		return byteSum;
	}

	public static MemoryAddress addByteFromPointerAndBytesFromStruct_returnBytePointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

		byte arg1 = MemoryAccess.getByteAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		byte byteSum = (byte)(arg1 + (byte)byteHandle1.get(arg2) + (byte)byteHandle2.get(arg2));
		MemoryAccess.setByteAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), byteSum);
		return arg1Addr;
	}

	public static byte addByteAndBytesFromStructPointer(byte arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		byte byteSum = (byte)(arg1 + (byte)byteHandle1.get(arg2) + (byte)byteHandle2.get(arg2));
		return byteSum;
	}

	public static byte addByteAndBytesFromNestedStruct(byte arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"),
				C_CHAR.withName("elem2"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize()));

		byte nestedStructElem1 = MemoryAccess.getByteAtOffset(arg2, 0);
		byte nestedStructElem2 = MemoryAccess.getByteAtOffset(arg2, 1);
		byte structElem2 = MemoryAccess.getByteAtOffset(arg2, 2);

		byte byteSum = (byte)(arg1 + nestedStructElem1 + nestedStructElem2 + structElem2);
		return byteSum;
	}

	public static byte addByteAndBytesFromNestedStruct_reverseOrder(byte arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"),
				nestedStructLayout.withName("struct_elem2"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize()));

		byte structElem1 = MemoryAccess.getByteAtOffset(arg2, 0);
		byte nestedStructElem1 = MemoryAccess.getByteAtOffset(arg2, 1);
		byte nestedStructElem2 = MemoryAccess.getByteAtOffset(arg2, 2);

		byte byteSum = (byte)(arg1 + structElem1 + nestedStructElem1 + nestedStructElem2);
		return byteSum;
	}

	public static byte addByteAndBytesFromStructWithNestedByteArray(byte arg1, MemorySegment arg2) {
		SequenceLayout byteArray = MemoryLayout.ofSequence(2, C_CHAR);
		GroupLayout structLayout = MemoryLayout.ofStruct(byteArray.withName("array_elem1"),
				C_CHAR.withName("elem2"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize()));

		byte nestedByteArrayElem1 = MemoryAccess.getByteAtOffset(arg2, 0);
		byte nestedByteArrayElem2 = MemoryAccess.getByteAtOffset(arg2, 1);
		byte structElem2 = MemoryAccess.getByteAtOffset(arg2, 2);

		byte byteSum = (byte)(arg1 + nestedByteArrayElem1 + nestedByteArrayElem2 + structElem2);
		return byteSum;
	}

	public static byte addByteAndBytesFromStructWithNestedByteArray_reverseOrder(byte arg1, MemorySegment arg2) {
		SequenceLayout byteArray = MemoryLayout.ofSequence(2, C_CHAR);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"),
				byteArray.withName("array_elem2"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize()));

		byte structElem1 = MemoryAccess.getByteAtOffset(arg2, 0);
		byte nestedByteArrayElem1 = MemoryAccess.getByteAtOffset(arg2, 1);
		byte nestedByteArrayElem2 = MemoryAccess.getByteAtOffset(arg2, 2);

		byte byteSum = (byte)(arg1 + structElem1 + nestedByteArrayElem1 + nestedByteArrayElem2);
		return byteSum;
	}

	public static byte addByteAndBytesFromStructWithNestedStructArray(byte arg1, MemorySegment arg2) {
		GroupLayout byteStruct = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, byteStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"),
				C_CHAR.withName("elem2"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize() * 3));

		byte nestedStructArrayElem1_Elem1 = MemoryAccess.getByteAtOffset(arg2, 0);
		byte nestedStructArrayElem1_Elem2 = MemoryAccess.getByteAtOffset(arg2, 1);
		byte nestedStructArrayElem2_Elem1 = MemoryAccess.getByteAtOffset(arg2, 2);
		byte nestedStructArrayElem2_Elem2 = MemoryAccess.getByteAtOffset(arg2, 3);
		byte structElem2 = MemoryAccess.getByteAtOffset(arg2, 4);

		byte byteSum = (byte)(arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2);
		return byteSum;
	}

	public static byte addByteAndBytesFromStructWithNestedStructArray_reverseOrder(byte arg1, MemorySegment arg2) {
		GroupLayout byteStruct = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, byteStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"),
				structArray.withName("struct_array_elem2"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize() * 3));

		byte structElem1 = MemoryAccess.getByteAtOffset(arg2, 0);
		byte nestedStructArrayElem1_Elem1 = MemoryAccess.getByteAtOffset(arg2, 1);
		byte nestedStructArrayElem1_Elem2 = MemoryAccess.getByteAtOffset(arg2, 2);
		byte nestedStructArrayElem2_Elem1 = MemoryAccess.getByteAtOffset(arg2, 3);
		byte nestedStructArrayElem2_Elem2 = MemoryAccess.getByteAtOffset(arg2, 4);

		byte byteSum = (byte)(arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2);
		return byteSum;
	}

	public static MemorySegment add2ByteStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

		MemorySegment byteStructSegmt = MemorySegment.allocateNative(structLayout);
		byte byteStruct_Elem1 = (byte)((byte)byteHandle1.get(arg1) + (byte)byteHandle1.get(arg2));
		byte byteStruct_Elem2 = (byte)((byte)byteHandle2.get(arg1) + (byte)byteHandle2.get(arg2));
		byteHandle1.set(byteStructSegmt, byteStruct_Elem1);
		byteHandle2.set(byteStructSegmt, byteStruct_Elem2);
		return byteStructSegmt;
	}

	public static MemoryAddress add2ByteStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		byte byteStruct_Elem1 = (byte)((byte)byteHandle1.get(arg1) + (byte)byteHandle1.get(arg2));
		byte byteStruct_Elem2 = (byte)((byte)byteHandle2.get(arg1) + (byte)byteHandle2.get(arg2));
		byteHandle1.set(arg1, byteStruct_Elem1);
		byteHandle2.set(arg1, byteStruct_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3ByteStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"),
				C_CHAR.withName("elem3"), MemoryLayout.ofPaddingBits(C_CHAR.bitSize()));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));
		VarHandle byteHandle3 = structLayout.varHandle(byte.class, PathElement.groupElement("elem3"));

		MemorySegment byteStructSegmt = MemorySegment.allocateNative(structLayout);
		byte byteStruct_Elem1 = (byte)((byte)byteHandle1.get(arg1) + (byte)byteHandle1.get(arg2));
		byte byteStruct_Elem2 = (byte)((byte)byteHandle2.get(arg1) + (byte)byteHandle2.get(arg2));
		byte byteStruct_Elem3 = (byte)((byte)byteHandle3.get(arg1) + (byte)byteHandle3.get(arg2));
		byteHandle1.set(byteStructSegmt, byteStruct_Elem1);
		byteHandle2.set(byteStructSegmt, byteStruct_Elem2);
		byteHandle3.set(byteStructSegmt, byteStruct_Elem3);
		return byteStructSegmt;
	}

	public static char addCharAndCharsFromStruct(char arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));

		char result = (char)(arg1 + (char)charHandle1.get(arg2) + (char)charHandle2.get(arg2) - 2 * 'A');
		return result;
	}

	public static char addCharFromPointerAndCharsFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));

		char arg1 = MemoryAccess.getCharAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		char result = (char)(arg1 + (char)charHandle1.get(arg2) + (char)charHandle2.get(arg2) - 2 * 'A');
		return result;
	}

	public static MemoryAddress addCharFromPointerAndCharsFromStruct_returnCharPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));

		char arg1 = MemoryAccess.getCharAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		char result = (char)(arg1 + (char)charHandle1.get(arg2) + (char)charHandle2.get(arg2) - 2 * 'A');
		MemoryAccess.setCharAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), result);
		return arg1Addr;
	}

	public static char addCharAndCharsFromStructPointer(char arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		char result = (char)(arg1 + (char)charHandle1.get(arg2) + (char)charHandle2.get(arg2) - 2 * 'A');
		return result;
	}

	public static char addCharAndCharsFromNestedStruct(char arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"),
				C_SHORT.withName("elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		char nestedStructElem1 = MemoryAccess.getCharAtOffset(arg2, 0);
		char nestedStructElem2 = MemoryAccess.getCharAtOffset(arg2, 2);
		char structElem2 = MemoryAccess.getCharAtOffset(arg2, 4);

		char result = (char)(arg1 + nestedStructElem1 + nestedStructElem2 + structElem2 - 3 * 'A');
		return result;
	}

	public static char addCharAndCharsFromNestedStruct_reverseOrder(char arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"),
				nestedStructLayout.withName("struct_elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		char structElem1 = MemoryAccess.getCharAtOffset(arg2, 0);
		char nestedStructElem1 = MemoryAccess.getCharAtOffset(arg2, 2);
		char nestedStructElem2 = MemoryAccess.getCharAtOffset(arg2, 4);

		char result = (char)(arg1 + structElem1 + nestedStructElem1 + nestedStructElem2 - 3 * 'A');
		return result;
	}

	public static char addCharAndCharsFromStructWithNestedCharArray(char arg1, MemorySegment arg2) {
		SequenceLayout charArray = MemoryLayout.ofSequence(2, C_SHORT);
		GroupLayout structLayout = MemoryLayout.ofStruct(charArray.withName("array_elem1"),
				C_SHORT.withName("elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		char nestedCharArrayElem1 = MemoryAccess.getCharAtOffset(arg2, 0);
		char nestedCharArrayElem2 = MemoryAccess.getCharAtOffset(arg2, 2);
		char structElem2 = MemoryAccess.getCharAtOffset(arg2, 4);

		char result = (char)(arg1 + nestedCharArrayElem1 + nestedCharArrayElem2 + structElem2 - 3 * 'A');
		return result;
	}

	public static char addCharAndCharsFromStructWithNestedCharArray_reverseOrder(char arg1, MemorySegment arg2) {
		SequenceLayout charArray = MemoryLayout.ofSequence(2, C_SHORT);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"),
				charArray.withName("array_elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		char structElem1 = MemoryAccess.getCharAtOffset(arg2, 0);
		char nestedCharArrayElem1 = MemoryAccess.getCharAtOffset(arg2, 2);
		char nestedCharArrayElem2 = MemoryAccess.getCharAtOffset(arg2, 4);

		char result = (char)(arg1 + structElem1 + nestedCharArrayElem1 + nestedCharArrayElem2 - 3 * 'A');
		return result;
	}

	public static char addCharAndCharsFromStructWithNestedStructArray(char arg1, MemorySegment arg2) {
		GroupLayout charStruct = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, charStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"), C_SHORT.withName("elem2"));

		char nestedStructArrayElem1_Elem1 = MemoryAccess.getCharAtOffset(arg2, 0);
		char nestedStructArrayElem1_Elem2 = MemoryAccess.getCharAtOffset(arg2, 2);
		char nestedStructArrayElem2_Elem1 = MemoryAccess.getCharAtOffset(arg2, 4);
		char nestedStructArrayElem2_Elem2 = MemoryAccess.getCharAtOffset(arg2, 6);
		char structElem2 = MemoryAccess.getCharAtOffset(arg2, 8);

		char result = (char)(arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2 - 5 * 'A');
		return result;
	}

	public static char addCharAndCharsFromStructWithNestedStructArray_reverseOrder(char arg1, MemorySegment arg2) {
		GroupLayout charStruct = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, charStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"),
				structArray.withName("struct_array_elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		char structElem1 = MemoryAccess.getCharAtOffset(arg2, 0);
		char nestedStructArrayElem1_Elem1 = MemoryAccess.getCharAtOffset(arg2, 2);
		char nestedStructArrayElem1_Elem2 = MemoryAccess.getCharAtOffset(arg2, 4);
		char nestedStructArrayElem2_Elem1 = MemoryAccess.getCharAtOffset(arg2, 6);
		char nestedStructArrayElem2_Elem2 = MemoryAccess.getCharAtOffset(arg2, 8);

		char result = (char)(arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2 - 5 * 'A');
		return result;
	}

	public static MemorySegment add2CharStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));

		MemorySegment charStructSegmt = MemorySegment.allocateNative(structLayout);
		char charStruct_Elem1 = (char)((char)charHandle1.get(arg1) + (char)charHandle1.get(arg2) - 'A');
		char charStruct_Elem2 = (char)((char)charHandle2.get(arg1) + (char)charHandle2.get(arg2) - 'A');
		charHandle1.set(charStructSegmt, charStruct_Elem1);
		charHandle2.set(charStructSegmt, charStruct_Elem2);
		return charStructSegmt;
	}

	public static MemoryAddress add2CharStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		char charStruct_Elem1 = (char)((char)charHandle1.get(arg1) + (char)charHandle1.get(arg2) - 'A');
		char charStruct_Elem2 = (char)((char)charHandle2.get(arg1) + (char)charHandle2.get(arg2) - 'A');
		charHandle1.set(arg1, charStruct_Elem1);
		charHandle2.set(arg1, charStruct_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3CharStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"),
				C_SHORT.withName("elem3"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));
		VarHandle charHandle1 = structLayout.varHandle(char.class, PathElement.groupElement("elem1"));
		VarHandle charHandle2 = structLayout.varHandle(char.class, PathElement.groupElement("elem2"));
		VarHandle charHandle3 = structLayout.varHandle(char.class, PathElement.groupElement("elem3"));

		MemorySegment charStructSegmt = MemorySegment.allocateNative(structLayout);
		char charStruct_Elem1 = (char)((char)charHandle1.get(arg1) + (char)charHandle1.get(arg2) - 'A');
		char charStruct_Elem2 = (char)((char)charHandle2.get(arg1) + (char)charHandle2.get(arg2) - 'A');
		char charStruct_Elem3 = (char)((char)charHandle3.get(arg1) + (char)charHandle3.get(arg2) - 'A');
		charHandle1.set(charStructSegmt, charStruct_Elem1);
		charHandle2.set(charStructSegmt, charStruct_Elem2);
		charHandle3.set(charStructSegmt, charStruct_Elem3);
		return charStructSegmt;
	}

	public static short addShortAndShortsFromStruct(short arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		short shortSum = (short)(arg1 + (short)shortHandle1.get(arg2) + (short)shortHandle2.get(arg2));
		return shortSum;
	}

	public static short addShortFromPointerAndShortsFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		short arg1 = MemoryAccess.getShortAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		short shortSum = (short)(arg1 + (short)shortHandle1.get(arg2) + (short)shortHandle2.get(arg2));
		return shortSum;
	}

	public static MemoryAddress addShortFromPointerAndShortsFromStruct_returnShortPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		short arg1 = MemoryAccess.getShortAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		short shortSum = (short)(arg1 + (short)shortHandle1.get(arg2) + (short)shortHandle2.get(arg2));
		MemoryAccess.setIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), shortSum);
		return arg1Addr;
	}

	public static short addShortAndShortsFromStructPointer(short arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		short shortSum = (short)(arg1 + (short)shortHandle1.get(arg2) + (short)shortHandle2.get(arg2));
		return shortSum;
	}

	public static short addShortAndShortsFromNestedStruct(short arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"),
				C_SHORT.withName("elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		short nestedStructElem1 = MemoryAccess.getShortAtOffset(arg2, 0);
		short nestedStructElem2 = MemoryAccess.getShortAtOffset(arg2, 2);
		short structElem2 = MemoryAccess.getShortAtOffset(arg2, 4);

		short shortSum = (short)(arg1 + nestedStructElem1 + nestedStructElem2 + structElem2);
		return shortSum;
	}

	public static short addShortAndShortsFromNestedStruct_reverseOrder(short arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"),
				nestedStructLayout.withName("struct_elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		short structElem1 = MemoryAccess.getShortAtOffset(arg2, 0);
		short nestedStructElem1 = MemoryAccess.getShortAtOffset(arg2, 2);
		short nestedStructElem2 = MemoryAccess.getShortAtOffset(arg2, 4);

		short shortSum = (short)(arg1 + structElem1 + nestedStructElem1 + nestedStructElem2);
		return shortSum;
	}

	public static short addShortAndShortsFromStructWithNestedShortArray(short arg1, MemorySegment arg2) {
		SequenceLayout shortArray = MemoryLayout.ofSequence(2, C_SHORT);
		GroupLayout structLayout = MemoryLayout.ofStruct(shortArray.withName("array_elem1"),
				C_SHORT.withName("elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		short nestedShortArrayElem1 = MemoryAccess.getShortAtOffset(arg2, 0);
		short nestedShortArrayElem2 = MemoryAccess.getShortAtOffset(arg2, 2);
		short structElem2 = MemoryAccess.getShortAtOffset(arg2, 4);

		short shortSum = (short)(arg1 + nestedShortArrayElem1 + nestedShortArrayElem2 + structElem2);
		return shortSum;
	}

	public static short addShortAndShortsFromStructWithNestedShortArray_reverseOrder(short arg1, MemorySegment arg2) {
		SequenceLayout shortArray = MemoryLayout.ofSequence(2, C_SHORT);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"),
				shortArray.withName("array_elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));

		short structElem1 = MemoryAccess.getShortAtOffset(arg2, 0);
		short nestedShortArrayElem1 = MemoryAccess.getShortAtOffset(arg2, 2);
		short nestedShortArrayElem2 = MemoryAccess.getShortAtOffset(arg2, 4);

		short shortSum = (short)(arg1 + structElem1 + nestedShortArrayElem1 + nestedShortArrayElem2);
		return shortSum;
	}

	public static short addShortAndShortsFromStructWithNestedStructArray(short arg1, MemorySegment arg2) {
		GroupLayout shortStruct = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, shortStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struc_array_elem1"), C_SHORT.withName("elem2"));

		short nestedStructArrayElem1_Elem1 = MemoryAccess.getShortAtOffset(arg2, 0);
		short nestedStructArrayElem1_Elem2 = MemoryAccess.getShortAtOffset(arg2, 2);
		short nestedStructArrayElem2_Elem1 = MemoryAccess.getShortAtOffset(arg2, 4);
		short nestedStructArrayElem2_Elem2 = MemoryAccess.getShortAtOffset(arg2, 6);
		short structElem2 = MemoryAccess.getShortAtOffset(arg2, 8);

		short shortSum = (short)(arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2);
		return shortSum;
	}

	public static short addShortAndShortsFromStructWithNestedStructArray_reverseOrder(short arg1, MemorySegment arg2) {
		GroupLayout shortStruct = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, shortStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), structArray.withName("struc_array_elem2"));

		short structElem1 = MemoryAccess.getShortAtOffset(arg2, 0);
		short nestedStructArrayElem1_Elem1 = MemoryAccess.getShortAtOffset(arg2, 2);
		short nestedStructArrayElem1_Elem2 = MemoryAccess.getShortAtOffset(arg2, 4);
		short nestedStructArrayElem2_Elem1 = MemoryAccess.getShortAtOffset(arg2, 6);
		short nestedStructArrayElem2_Elem2 = MemoryAccess.getShortAtOffset(arg2, 8);

		short shortSum = (short)(arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2);
		return shortSum;
	}

	public static MemorySegment add2ShortStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		MemorySegment shortStructSegmt = MemorySegment.allocateNative(structLayout);
		short shortStruct_Elem1 = (short)((short)shortHandle1.get(arg1) + (short)shortHandle1.get(arg2));
		short shortStruct_Elem2 = (short)((short)shortHandle2.get(arg1) + (short)shortHandle2.get(arg2));
		shortHandle1.set(shortStructSegmt, shortStruct_Elem1);
		shortHandle2.set(shortStructSegmt, shortStruct_Elem2);
		return shortStructSegmt;
	}

	public static MemoryAddress add2ShortStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		short shortStruct_Elem1 = (short)((short)shortHandle1.get(arg1) + (short)shortHandle1.get(arg2));
		short shortStruct_Elem2 = (short)((short)shortHandle2.get(arg1) + (short)shortHandle2.get(arg2));
		shortHandle1.set(arg1, shortStruct_Elem1);
		shortHandle2.set(arg1, shortStruct_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3ShortStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"),
				C_SHORT.withName("elem3"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));
		VarHandle shortHandle3 = structLayout.varHandle(short.class, PathElement.groupElement("elem3"));

		MemorySegment shortStructSegmt = MemorySegment.allocateNative(structLayout);
		short shortStruct_Elem1 = (short)((short)shortHandle1.get(arg1) + (short)shortHandle1.get(arg2));
		short shortStruct_Elem2 = (short)((short)shortHandle2.get(arg1) + (short)shortHandle2.get(arg2));
		short shortStruct_Elem3 = (short)((short)shortHandle3.get(arg1) + (short)shortHandle3.get(arg2));
		shortHandle1.set(shortStructSegmt, shortStruct_Elem1);
		shortHandle2.set(shortStructSegmt, shortStruct_Elem2);
		shortHandle3.set(shortStructSegmt, shortStruct_Elem3);
		return shortStructSegmt;
	}

	public static int addIntAndIntsFromStruct(int arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		int intSum = arg1 + (int)intHandle1.get(arg2) + (int)intHandle2.get(arg2);
		return intSum;
	}

	public static int addIntAndIntShortFromStruct(int arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"),
				C_SHORT.withName("elem2"), MemoryLayout.ofPaddingBits(C_SHORT.bitSize()));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		int intSum = arg1 + (int)elemHandle1.get(arg2) + (short)elemHandle2.get(arg2);
		return intSum;
	}

	public static int addIntAndShortIntFromStruct(int arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_SHORT.withName("elem1"),
				MemoryLayout.ofPaddingBits(C_SHORT.bitSize()), C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		int intSum = arg1 + (short)elemHandle1.get(arg2) + (int)elemHandle2.get(arg2);
		return intSum;
	}

	public static int addIntFromPointerAndIntsFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		int arg1 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		int intSum = arg1 + (int)intHandle1.get(arg2) + (int)intHandle2.get(arg2);
		return intSum;
	}

	public static MemoryAddress addIntFromPointerAndIntsFromStruct_returnIntPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		int arg1 = MemoryAccess.getIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		int intSum = arg1 + (int)intHandle1.get(arg2) + (int)intHandle2.get(arg2);
		MemoryAccess.setIntAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), intSum);
		return arg1Addr;
	}

	public static int addIntAndIntsFromStructPointer(int arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		int intSum = arg1 + (int)intHandle1.get(arg2) + (int)intHandle2.get(arg2);
		return intSum;
	}

	public static int addIntAndIntsFromNestedStruct(int arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"), C_INT.withName("elem2"));

		int nestedStructElem1 = MemoryAccess.getIntAtOffset(arg2, 0);
		int nestedStructElem2 = MemoryAccess.getIntAtOffset(arg2, 4);
		int structElem2 = MemoryAccess.getIntAtOffset(arg2, 8);

		int intSum = arg1 + nestedStructElem1 + nestedStructElem2 + structElem2;
		return intSum;
	}

	public static int addIntAndIntsFromNestedStruct_reverseOrder(int arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), nestedStructLayout.withName("struct_elem2"));

		int structElem1 = MemoryAccess.getIntAtOffset(arg2, 0);
		int nestedStructElem1 = MemoryAccess.getIntAtOffset(arg2, 4);
		int nestedStructElem2 = MemoryAccess.getIntAtOffset(arg2, 8);

		int intSum = arg1 + structElem1 + nestedStructElem1 + nestedStructElem2;
		return intSum;
	}

	public static int addIntAndIntsFromStructWithNestedIntArray(int arg1, MemorySegment arg2) {
		SequenceLayout intArray = MemoryLayout.ofSequence(2, C_INT);
		GroupLayout structLayout = MemoryLayout.ofStruct(intArray.withName("array_elem1"), C_INT.withName("elem2"));

		int nestedIntArrayElem1 = MemoryAccess.getIntAtOffset(arg2, 0);
		int nestedIntArrayElem2 = MemoryAccess.getIntAtOffset(arg2, 4);
		int structElem2 = MemoryAccess.getIntAtOffset(arg2, 8);

		int intSum = arg1 + nestedIntArrayElem1 + nestedIntArrayElem2 + structElem2;
		return intSum;
	}

	public static int addIntAndIntsFromStructWithNestedIntArray_reverseOrder(int arg1, MemorySegment arg2) {
		SequenceLayout intArray = MemoryLayout.ofSequence(2, C_INT);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), intArray.withName("array_elem2"));

		int structElem1 = MemoryAccess.getIntAtOffset(arg2, 0);
		int nestedIntArrayElem1 = MemoryAccess.getIntAtOffset(arg2, 4);
		int nestedIntArrayElem2 = MemoryAccess.getIntAtOffset(arg2, 8);

		int intSum = arg1 + structElem1 + nestedIntArrayElem1 + nestedIntArrayElem2;
		return intSum;
	}

	public static int addIntAndIntsFromStructWithNestedStructArray(int arg1, MemorySegment arg2) {
		GroupLayout intStruct = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, intStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"), C_INT.withName("elem2"));

		int nestedStructArrayElem1_Elem1 = MemoryAccess.getIntAtOffset(arg2, 0);
		int nestedStructArrayElem1_Elem2 = MemoryAccess.getIntAtOffset(arg2, 4);
		int nestedStructArrayElem2_Elem1 = MemoryAccess.getIntAtOffset(arg2, 8);
		int nestedStructArrayElem2_Elem2 = MemoryAccess.getIntAtOffset(arg2, 12);
		int structElem2 = MemoryAccess.getIntAtOffset(arg2, 16);

		int intSum = arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return intSum;
	}

	public static int addIntAndIntsFromStructWithNestedStructArray_reverseOrder(int arg1, MemorySegment arg2) {
		GroupLayout intStruct = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, intStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), structArray.withName("struct_array_elem2"));

		int structElem1 = MemoryAccess.getIntAtOffset(arg2, 0);
		int nestedStructArrayElem1_Elem1 = MemoryAccess.getIntAtOffset(arg2, 4);
		int nestedStructArrayElem1_Elem2 = MemoryAccess.getIntAtOffset(arg2, 8);
		int nestedStructArrayElem2_Elem1 = MemoryAccess.getIntAtOffset(arg2, 12);
		int nestedStructArrayElem2_Elem2 = MemoryAccess.getIntAtOffset(arg2, 16);

		int intSum = arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return intSum;
	}

	public static MemorySegment add2IntStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		MemorySegment intStructSegmt = MemorySegment.allocateNative(structLayout);
		int intStruct_Elem1 = (int)intHandle1.get(arg1) + (int)intHandle1.get(arg2);
		int intStruct_Elem2 = (int)intHandle2.get(arg1) + (int)intHandle2.get(arg2);
		intHandle1.set(intStructSegmt, intStruct_Elem1);
		intHandle2.set(intStructSegmt, intStruct_Elem2);
		return intStructSegmt;
	}

	public static MemoryAddress add2IntStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		int intSum_Elem1 = (int)intHandle1.get(arg1) + (int)intHandle1.get(arg2);
		int intSum_Elem2 = (int)intHandle2.get(arg1) + (int)intHandle2.get(arg2);
		intHandle1.set(arg1, intSum_Elem1);
		intHandle2.set(arg1, intSum_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3IntStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"), C_INT.withName("elem2"), C_INT.withName("elem3"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		VarHandle intHandle3 = structLayout.varHandle(int.class, PathElement.groupElement("elem3"));

		MemorySegment intStructSegmt = MemorySegment.allocateNative(structLayout);
		int intStruct_Elem1 = (int)intHandle1.get(arg1) + (int)intHandle1.get(arg2);
		int intStruct_Elem2 = (int)intHandle2.get(arg1) + (int)intHandle2.get(arg2);
		int intStruct_Elem3 = (int)intHandle3.get(arg1) + (int)intHandle3.get(arg2);
		intHandle1.set(intStructSegmt, intStruct_Elem1);
		intHandle2.set(intStructSegmt, intStruct_Elem2);
		intHandle3.set(intStructSegmt, intStruct_Elem3);
		return intStructSegmt;
	}

	public static long addLongAndLongsFromStruct(long arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		long longSum = arg1 + (long)longHandle1.get(arg2) + (long)longHandle2.get(arg2);
		return longSum;
	}

	public static long addIntAndIntLongFromStruct(int arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"),
				MemoryLayout.ofPaddingBits(C_INT.bitSize()), longLayout.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		long longSum = arg1 + (int)elemHandle1.get(arg2) + (long)elemHandle2.get(arg2);
		return longSum;
	}

	public static long addIntAndLongIntFromStruct(int arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"),
				C_INT.withName("elem2"), MemoryLayout.ofPaddingBits(C_INT.bitSize()));
		VarHandle elemHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		long longSum = arg1 + (long)elemHandle1.get(arg2) + (int)elemHandle2.get(arg2);
		return longSum;
	}

	public static long addLongFromPointerAndLongsFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		long arg1 = MemoryAccess.getLongAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		long longSum = arg1 + (long)longHandle1.get(arg2) + (long)longHandle2.get(arg2);
		return longSum;
	}

	public static MemoryAddress addLongFromPointerAndLongsFromStruct_returnLongPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		long arg1 = MemoryAccess.getLongAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		long longSum = arg1 + (long)longHandle1.get(arg2) + (long)longHandle2.get(arg2);
		MemoryAccess.setLongAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), longSum);
		return arg1Addr;
	}

	public static long addLongAndLongsFromStructPointer(long arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		long longSum = arg1 + (long)longHandle1.get(arg2) + (long)longHandle2.get(arg2);
		return longSum;
	}

	public static long addLongAndLongsFromNestedStruct(long arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), nestedStructLayout.withName("struct_elem2"));

		long nestedStructElem1 = MemoryAccess.getLongAtOffset(arg2, 0);
		long nestedStructElem2 = MemoryAccess.getLongAtOffset(arg2, 8);
		long structElem2 = MemoryAccess.getLongAtOffset(arg2, 16);

		long longSum = arg1 + nestedStructElem1 + nestedStructElem2 + structElem2;
		return longSum;
	}

	public static long addLongAndLongsFromNestedStruct_reverseOrder(long arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), nestedStructLayout.withName("struct_elem2"));

		long structElem1 = MemoryAccess.getLongAtOffset(arg2, 0);
		long nestedStructElem1 = MemoryAccess.getLongAtOffset(arg2, 8);
		long nestedStructElem2 = MemoryAccess.getLongAtOffset(arg2, 16);

		long longSum = arg1 + structElem1 + nestedStructElem1 + nestedStructElem2;
		return longSum;
	}

	public static long addLongAndLongsFromStructWithNestedLongArray(long arg1, MemorySegment arg2) {
		SequenceLayout longArray = MemoryLayout.ofSequence(2, longLayout);
		GroupLayout structLayout = MemoryLayout.ofStruct(longArray.withName("array_elem1"), longLayout.withName("elem2"));

		long nestedLongrrayElem1 = MemoryAccess.getLongAtOffset(arg2, 0);
		long nestedLongrrayElem2 = MemoryAccess.getLongAtOffset(arg2, 8);
		long structElem2 = MemoryAccess.getLongAtOffset(arg2, 16);

		long longSum = arg1 + nestedLongrrayElem1 + nestedLongrrayElem2 + structElem2;
		return longSum;
	}

	public static long addLongAndLongsFromStructWithNestedLongArray_reverseOrder(long arg1, MemorySegment arg2) {
		SequenceLayout longArray = MemoryLayout.ofSequence(2, longLayout);
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longArray.withName("array_elem2"));

		long structElem1 = MemoryAccess.getLongAtOffset(arg2, 0);
		long nestedLongrrayElem1 = MemoryAccess.getLongAtOffset(arg2, 8);
		long nestedLongrrayElem2 = MemoryAccess.getLongAtOffset(arg2, 16);

		long longSum = arg1 + structElem1 + nestedLongrrayElem1 + nestedLongrrayElem2;
		return longSum;
	}

	public static long addLongAndLongsFromStructWithNestedStructArray(long arg1, MemorySegment arg2) {
		GroupLayout longStruct = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, longStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"), longLayout.withName("elem2"));

		long nestedStructArrayElem1_Elem1 = MemoryAccess.getLongAtOffset(arg2, 0);
		long nestedStructArrayElem1_Elem2 = MemoryAccess.getLongAtOffset(arg2, 8);
		long nestedStructArrayElem2_Elem1 = MemoryAccess.getLongAtOffset(arg2, 16);
		long nestedStructArrayElem2_Elem2 = MemoryAccess.getLongAtOffset(arg2, 24);
		long structElem2 = MemoryAccess.getLongAtOffset(arg2, 32);

		long longSum = arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return longSum;
	}

	public static long addLongAndLongsFromStructWithNestedStructArray_reverseOrder(long arg1, MemorySegment arg2) {
		GroupLayout longStruct = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, longStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), structArray.withName("struct_array_elem2"));

		long structElem1 = MemoryAccess.getLongAtOffset(arg2, 0);
		long nestedStructArrayElem1_Elem1 = MemoryAccess.getLongAtOffset(arg2, 8);
		long nestedStructArrayElem1_Elem2 = MemoryAccess.getLongAtOffset(arg2, 16);
		long nestedStructArrayElem2_Elem1 = MemoryAccess.getLongAtOffset(arg2, 24);
		long nestedStructArrayElem2_Elem2 = MemoryAccess.getLongAtOffset(arg2, 32);

		long longSum = arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return longSum;
	}

	public static MemorySegment add2LongStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		MemorySegment longStructSegmt = MemorySegment.allocateNative(structLayout);
		long longStruct_Elem1 = (long)longHandle1.get(arg1) + (long)longHandle1.get(arg2);
		long longStruct_Elem2 = (long)longHandle2.get(arg1) + (long)longHandle2.get(arg2);
		longHandle1.set(longStructSegmt, longStruct_Elem1);
		longHandle2.set(longStructSegmt, longStruct_Elem2);
		return longStructSegmt;
	}

	public static MemoryAddress add2LongStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		long longSum_Elem1 = (long)longHandle1.get(arg1) + (long)longHandle1.get(arg2);
		long longSum_Elem2 = (long)longHandle2.get(arg1) + (long)longHandle2.get(arg2);
		longHandle1.set(arg1, longSum_Elem1);
		longHandle2.set(arg1, longSum_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3LongStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(longLayout.withName("elem1"), longLayout.withName("elem2"), longLayout.withName("elem3"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));
		VarHandle longHandle3 = structLayout.varHandle(long.class, PathElement.groupElement("elem3"));

		MemorySegment longStructSegmt = MemorySegment.allocateNative(structLayout);
		long longStruct_Elem1 = (long)longHandle1.get(arg1) + (long)longHandle1.get(arg2);
		long longStruct_Elem2 = (long)longHandle2.get(arg1) + (long)longHandle2.get(arg2);
		long longStruct_Elem3 = (long)longHandle3.get(arg1) + (long)longHandle3.get(arg2);
		longHandle1.set(longStructSegmt, longStruct_Elem1);
		longHandle2.set(longStructSegmt, longStruct_Elem2);
		longHandle3.set(longStructSegmt, longStruct_Elem3);
		return longStructSegmt;
	}

	public static float addFloatAndFloatsFromStruct(float arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		float floatSum = arg1 + (float)floatHandle1.get(arg2) + (float)floatHandle2.get(arg2);
		return floatSum;
	}

	public static float addFloatFromPointerAndFloatsFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		float arg1 = MemoryAccess.getFloatAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		float floatSum = arg1 + (float)floatHandle1.get(arg2) + (float)floatHandle2.get(arg2);
		return floatSum;
	}

	public static MemoryAddress addFloatFromPointerAndFloatsFromStruct_returnFloatPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		float arg1 = MemoryAccess.getFloatAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		float floatSum = arg1 + (float)floatHandle1.get(arg2) + (float)floatHandle2.get(arg2);
		MemoryAccess.setFloatAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), floatSum);
		return arg1Addr;
	}

	public static float addFloatAndFloatsFromStructPointer(float arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		float floatSum = arg1 + (float)floatHandle1.get(arg2) + (float)floatHandle2.get(arg2);
		return floatSum;
	}

	public static float addFloatAndFloatsFromNestedStruct(float arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"), C_FLOAT.withName("elem2"));

		float nestedStructElem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
		float nestedStructElem2 = MemoryAccess.getFloatAtOffset(arg2, 4);
		float structElem2 = MemoryAccess.getFloatAtOffset(arg2, 8);

		float floatSum = arg1 + nestedStructElem1 + nestedStructElem2 + structElem2;
		return floatSum;
	}

	public static float addFloatAndFloatsFromNestedStruct_reverseOrder(float arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), nestedStructLayout.withName("struct_elem2"));

		float structElem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
		float nestedStructElem1 = MemoryAccess.getFloatAtOffset(arg2, 4);
		float nestedStructElem2 = MemoryAccess.getFloatAtOffset(arg2, 8);

		float floatSum = arg1 + structElem1 + nestedStructElem1 + nestedStructElem2;
		return floatSum;
	}

	public static float addFloatAndFloatsFromStructWithNestedFloatArray(float arg1, MemorySegment arg2) {
		SequenceLayout floatArray = MemoryLayout.ofSequence(2, C_FLOAT);
		GroupLayout structLayout = MemoryLayout.ofStruct(floatArray.withName("array_elem1"), C_FLOAT.withName("elem2"));

		float nestedFloatArrayElem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
		float nestedFloatArrayElem2 = MemoryAccess.getFloatAtOffset(arg2, 4);
		float structElem2 = MemoryAccess.getFloatAtOffset(arg2, 8);

		float floatSum = arg1 + nestedFloatArrayElem1 + nestedFloatArrayElem2 + structElem2;
		return floatSum;
	}

	public static float addFloatAndFloatsFromStructWithNestedFloatArray_reverseOrder(float arg1, MemorySegment arg2) {
		SequenceLayout floatArray = MemoryLayout.ofSequence(2, C_FLOAT);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), floatArray.withName("array_elem2"));

		float structElem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
		float nestedFloatArrayElem1 = MemoryAccess.getFloatAtOffset(arg2, 4);
		float nestedFloatArrayElem2 = MemoryAccess.getFloatAtOffset(arg2, 8);

		float floatSum = arg1 + structElem1 + nestedFloatArrayElem1 + nestedFloatArrayElem2;
		return floatSum;
	}

	public static float addFloatAndFloatsFromStructWithNestedStructArray(float arg1, MemorySegment arg2) {
		GroupLayout floatStruct = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, floatStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"), C_FLOAT.withName("elem2"));

		float nestedStructArrayElem1_Elem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
		float nestedStructArrayElem1_Elem2 = MemoryAccess.getFloatAtOffset(arg2, 4);
		float nestedStructArrayElem2_Elem1 = MemoryAccess.getFloatAtOffset(arg2, 8);
		float nestedStructArrayElem2_Elem2 = MemoryAccess.getFloatAtOffset(arg2, 12);
		float structElem2 = MemoryAccess.getFloatAtOffset(arg2, 16);

		float floatSum = arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return floatSum;
	}

	public static float addFloatAndFloatsFromStructWithNestedStructArray_reverseOrder(float arg1, MemorySegment arg2) {
		GroupLayout floatStruct = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, floatStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), structArray.withName("struct_array_elem2"));

		float structElem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
		float nestedStructArrayElem1_Elem1 = MemoryAccess.getFloatAtOffset(arg2, 4);
		float nestedStructArrayElem1_Elem2 = MemoryAccess.getFloatAtOffset(arg2, 8);
		float nestedStructArrayElem2_Elem1 = MemoryAccess.getFloatAtOffset(arg2, 12);
		float nestedStructArrayElem2_Elem2 = MemoryAccess.getFloatAtOffset(arg2, 16);

		float floatSum = arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return floatSum;
	}

	public static MemorySegment add2FloatStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		MemorySegment floatStructSegmt = MemorySegment.allocateNative(structLayout);
		float floatStruct_Elem1 = (float)floatHandle1.get(arg1) + (float)floatHandle1.get(arg2);
		float floatStruct_Elem2 = (float)floatHandle2.get(arg1) + (float)floatHandle2.get(arg2);
		floatHandle1.set(floatStructSegmt, floatStruct_Elem1);
		floatHandle2.set(floatStructSegmt, floatStruct_Elem2);
		return floatStructSegmt;
	}

	public static MemoryAddress add2FloatStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		float floatSum_Elem1 = (float)floatHandle1.get(arg1) + (float)floatHandle1.get(arg2);
		float floatSum_Elem2 = (float)floatHandle2.get(arg1) + (float)floatHandle2.get(arg2);
		floatHandle1.set(arg1, floatSum_Elem1);
		floatHandle2.set(arg1, floatSum_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3FloatStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"),  C_FLOAT.withName("elem3"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));
		VarHandle floatHandle3 = structLayout.varHandle(float.class, PathElement.groupElement("elem3"));

		MemorySegment floatStructSegmt = MemorySegment.allocateNative(structLayout);
		float floatStruct_Elem1 = (float)floatHandle1.get(arg1) + (float)floatHandle1.get(arg2);
		float floatStruct_Elem2 = (float)floatHandle2.get(arg1) + (float)floatHandle2.get(arg2);
		float floatStruct_Elem3 = (float)floatHandle3.get(arg1) + (float)floatHandle3.get(arg2);
		floatHandle1.set(floatStructSegmt, floatStruct_Elem1);
		floatHandle2.set(floatStructSegmt, floatStruct_Elem2);
		floatHandle3.set(floatStructSegmt, floatStruct_Elem3);
		return floatStructSegmt;
	}

	public static double addDoubleAndDoublesFromStruct(double arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		double doubleSum = arg1 + (double)doubleHandle1.get(arg2) + (double)doubleHandle2.get(arg2);
		return doubleSum;
	}

	public static double addDoubleAndFloatDoubleFromStruct(double arg1, MemorySegment arg2) {
		float structElem1 = 0;
		double structElem2 = 0;

		/* The size of [float, double] on AIX/PPC 64-bit is 12 bytes without padding by default
		 * while the same struct is 16 bytes with padding on other platforms.
		 */
		if (isAixOS) {
			structElem1 = MemoryAccess.getFloatAtOffset(arg2, 0);
			structElem2 = MemoryAccess.getDoubleAtOffset(arg2, 4);
		} else {
			GroupLayout structLayout = MemoryLayout.ofStruct(C_FLOAT.withName("elem1"),
					MemoryLayout.ofPaddingBits(C_FLOAT.bitSize()), C_DOUBLE.withName("elem2"));
			VarHandle elemHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
			VarHandle elemHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));
			structElem1 = (float)elemHandle1.get(arg2);
			structElem2 = (double)elemHandle2.get(arg2);
		}

		double doubleSum = arg1 + structElem1 + structElem2;
		return doubleSum;
	}

	public static double addDoubleAndIntDoubleFromStruct(double arg1, MemorySegment arg2) {
		int structElem1 = 0;
		double structElem2 = 0;

		/* The size of [int, double] on AIX/PPC 64-bit is 12 bytes without padding by default
		 * while the same struct is 16 bytes with padding on other platforms.
		 */
		if (isAixOS) {
			structElem1 = MemoryAccess.getIntAtOffset(arg2, 0);
			structElem2 = MemoryAccess.getDoubleAtOffset(arg2, 4);
		} else {
			GroupLayout structLayout = MemoryLayout.ofStruct(C_INT.withName("elem1"),
					MemoryLayout.ofPaddingBits(C_INT.bitSize()), C_DOUBLE.withName("elem2"));
			VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
			VarHandle elemHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));
			structElem1 = (int)elemHandle1.get(arg2);
			structElem2 = (double)elemHandle2.get(arg2);
		}

		double doubleSum = arg1 + structElem1 + structElem2;
		return doubleSum;
	}

	public static double addDoubleAndDoubleFloatFromStruct(double arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		double doubleSum = arg1 + (double)elemHandle1.get(arg2) + (float)elemHandle2.get(arg2);
		return doubleSum;
	}

	public static double addDoubleAndDoubleIntFromStruct(double arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		double doubleSum = arg1 + (double)elemHandle1.get(arg2) + (int)elemHandle2.get(arg2);
		return doubleSum;
	}

	public static double addDoubleFromPointerAndDoublesFromStruct(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		double arg1 = MemoryAccess.getDoubleAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		double doubleSum = arg1 + (double)doubleHandle1.get(arg2) + (double)doubleHandle2.get(arg2);
		return doubleSum;
	}

	public static MemoryAddress addDoubleFromPointerAndDoublesFromStruct_returnDoublePointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		double arg1 = MemoryAccess.getDoubleAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue());
		double doubleSum = arg1 + (double)doubleHandle1.get(arg2) + (double)doubleHandle2.get(arg2);
		MemoryAccess.setDoubleAtOffset(MemorySegment.ofNativeRestricted(), arg1Addr.toRawLongValue(), doubleSum);
		return arg1Addr;
	}

	public static double addDoubleAndDoublesFromStructPointer(double arg1, MemoryAddress arg2Addr) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		MemorySegment arg2 = arg2Addr.asSegmentRestricted(structLayout.byteSize());
		double doubleSum = arg1 + (double)doubleHandle1.get(arg2) + (double)doubleHandle2.get(arg2);
		return doubleSum;
	}

	public static double addDoubleAndDoublesFromNestedStruct(double arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(nestedStructLayout.withName("struct_elem1"), C_DOUBLE.withName("elem2"));

		double nestedStructElem1 = MemoryAccess.getDoubleAtOffset(arg2, 0);
		double nestedStructElem2 = MemoryAccess.getDoubleAtOffset(arg2, 8);
		double structElem2 = MemoryAccess.getDoubleAtOffset(arg2, 16);

		double doubleSum = arg1 + nestedStructElem1 + nestedStructElem2 + structElem2;
		return doubleSum;
	}

	public static double addDoubleAndDoublesFromNestedStruct_reverseOrder(double arg1, MemorySegment arg2) {
		GroupLayout nestedStructLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), nestedStructLayout.withName("struct_elem2"));

		double structElem1 = MemoryAccess.getDoubleAtOffset(arg2, 0);
		double nestedStructElem1 = MemoryAccess.getDoubleAtOffset(arg2, 8);
		double nestedStructElem2 = MemoryAccess.getDoubleAtOffset(arg2, 16);

		double doubleSum = arg1 + structElem1 + nestedStructElem1 + nestedStructElem2;
		return doubleSum;
	}

	public static double addDoubleAndDoublesFromStructWithNestedDoubleArray(double arg1, MemorySegment arg2) {
		SequenceLayout doubleArray = MemoryLayout.ofSequence(2, C_DOUBLE);
		GroupLayout structLayout = MemoryLayout.ofStruct(doubleArray.withName("array_elem1"), C_DOUBLE.withName("elem2"));

		double nestedDoubleArrayElem1 = MemoryAccess.getDoubleAtOffset(arg2, 0);
		double nestedDoubleArrayElem2 = MemoryAccess.getDoubleAtOffset(arg2, 8);
		double structElem2 = MemoryAccess.getDoubleAtOffset(arg2, 16);

		double doubleSum = arg1 + nestedDoubleArrayElem1 + nestedDoubleArrayElem2 + structElem2;
		return doubleSum;
	}

	public static double addDoubleAndDoublesFromStructWithNestedDoubleArray_reverseOrder(double arg1, MemorySegment arg2) {
		SequenceLayout doubleArray = MemoryLayout.ofSequence(2, C_DOUBLE);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), doubleArray.withName("array_elem2"));

		double structElem1 = MemoryAccess.getDoubleAtOffset(arg2, 0);
		double nestedDoubleArrayElem1 = MemoryAccess.getDoubleAtOffset(arg2, 8);
		double nestedDoubleArrayElem2 = MemoryAccess.getDoubleAtOffset(arg2, 16);

		double doubleSum = arg1 + structElem1 + nestedDoubleArrayElem1 + nestedDoubleArrayElem2;
		return doubleSum;
	}

	public static double addDoubleAndDoublesFromStructWithNestedStructArray(double arg1, MemorySegment arg2) {
		GroupLayout doubleStruct = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, doubleStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(structArray.withName("struct_array_elem1"), C_DOUBLE.withName("elem2"));

		double nestedStructArrayElem1_Elem1 = MemoryAccess.getDoubleAtOffset(arg2, 0);
		double nestedStructArrayElem1_Elem2 = MemoryAccess.getDoubleAtOffset(arg2, 8);
		double nestedStructArrayElem2_Elem1 = MemoryAccess.getDoubleAtOffset(arg2, 16);
		double nestedStructArrayElem2_Elem2 = MemoryAccess.getDoubleAtOffset(arg2, 24);
		double structElem2 = MemoryAccess.getDoubleAtOffset(arg2, 32);

		double doubleSum = arg1 + structElem2
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return doubleSum;
	}

	public static double addDoubleAndDoublesFromStructWithNestedStructArray_reverseOrder(double arg1, MemorySegment arg2) {
		GroupLayout doubleStruct = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		SequenceLayout structArray = MemoryLayout.ofSequence(2, doubleStruct);
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), structArray.withName("struct_array_elem2"));

		double structElem1 = MemoryAccess.getDoubleAtOffset(arg2, 0);
		double nestedStructArrayElem1_Elem1 = MemoryAccess.getDoubleAtOffset(arg2, 8);
		double nestedStructArrayElem1_Elem2 = MemoryAccess.getDoubleAtOffset(arg2, 16);
		double nestedStructArrayElem2_Elem1 = MemoryAccess.getDoubleAtOffset(arg2, 24);
		double nestedStructArrayElem2_Elem2 = MemoryAccess.getDoubleAtOffset(arg2, 32);

		double doubleSum = arg1 + structElem1
				+ nestedStructArrayElem1_Elem1 + nestedStructArrayElem1_Elem2
				+ nestedStructArrayElem2_Elem1 + nestedStructArrayElem2_Elem2;
		return doubleSum;
	}

	public static MemorySegment add2DoubleStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		MemorySegment doubleStructSegmt = MemorySegment.allocateNative(structLayout);
		double doubleStruct_Elem1 = (double)doubleHandle1.get(arg1) + (double)doubleHandle1.get(arg2);
		double doubleStruct_Elem2 = (double)doubleHandle2.get(arg1) + (double)doubleHandle2.get(arg2);
		doubleHandle1.set(doubleStructSegmt, doubleStruct_Elem1);
		doubleHandle2.set(doubleStructSegmt, doubleStruct_Elem2);
		return doubleStructSegmt;
	}

	public static MemoryAddress add2DoubleStructs_returnStructPointer(MemoryAddress arg1Addr, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		MemorySegment arg1 = arg1Addr.asSegmentRestricted(structLayout.byteSize());
		double doubleSum_Elem1 = (double)doubleHandle1.get(arg1) + (double)doubleHandle1.get(arg2);
		double doubleSum_Elem2 = (double)doubleHandle2.get(arg1) + (double)doubleHandle2.get(arg2);
		doubleHandle1.set(arg1, doubleSum_Elem1);
		doubleHandle2.set(arg1, doubleSum_Elem2);
		return arg1Addr;
	}

	public static MemorySegment add3DoubleStructs_returnStruct(MemorySegment arg1, MemorySegment arg2) {
		GroupLayout structLayout = MemoryLayout.ofStruct(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"), C_DOUBLE.withName("elem3"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));
		VarHandle doubleHandle3 = structLayout.varHandle(double.class, PathElement.groupElement("elem3"));

		MemorySegment doubleStructSegmt = MemorySegment.allocateNative(structLayout);
		double doubleStruct_Elem1 = (double)doubleHandle1.get(arg1) + (double)doubleHandle1.get(arg2);
		double doubleStruct_Elem2 = (double)doubleHandle2.get(arg1) + (double)doubleHandle2.get(arg2);
		double doubleStruct_Elem3 = (double)doubleHandle3.get(arg1) + (double)doubleHandle3.get(arg2);
		doubleHandle1.set(doubleStructSegmt, doubleStruct_Elem1);
		doubleHandle2.set(doubleStructSegmt, doubleStruct_Elem2);
		doubleHandle3.set(doubleStructSegmt, doubleStruct_Elem3);
		return doubleStructSegmt;
	}

}

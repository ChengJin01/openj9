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

/**
 * This file contains the native code used by the test cases in
 * org.openj9.test.jep389.upcall via a Clinker FFI UpCall.
 *
 * Created by jincheng@ca.ibm.com
 */

#include <stdio.h>
#include <stdbool.h>
#include <stdarg.h>
#include "downcall.h"

/**
 * Add two booleans with the OR (||) operator by invoking an upcall method handle.
 *
 * @param boolArg1 the 1st boolean
 * @param boolArg2 the 2nd boolean
 * @param upcallMH an upcall method handle
 * @return the result returned from the upcall method handle
 */
int
add2BoolsWithOrByUpCallMH(int boolArg1, int boolArg2, int (*upcallMH)(int, int))
{
	int result = (*upcallMH)(boolArg1, boolArg2);
	return result;
}

/**
 * Add two booleans with the OR (||) operator (the 2nd one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param boolArg1 a boolean
 * @param boolArg2 a pointer to boolean
 * @param upcallMH an upcall method handle
 * @return the result returned from the upcall method handle
 */
int
addBoolAndBoolFromPointerWithOrByUpCallMH(int boolArg1, int *boolArg2, int (*upcallMH)(int, int *))
{
	int result = (*upcallMH)(boolArg1, boolArg2);
	return result;
}

/**
 * Generate a new char by manipulating two chars via an upcall method handle.
 *
 * @param charArg1 the 1st char
 * @param charArg2 the 2nd char
 * @param upcallMH an upcall method handle
 * @return the resulting char returned from the upcall method handle
 */
short
createNewCharFrom2CharsByUpCallMH(short charArg1, short charArg2, short (*upcallMH)(short, short))
{
	short result = (*upcallMH)(charArg1, charArg2);
	return result;
}

/**
 * Generate a new char by manipulating two chars
 * (the 1st one dereferenced from a pointer) via invoking
 * an upcall method handle.
 *
 * @param charArg1 a pointer to char
 * @param charArg2 the 2nd char
 * @param upcallMH an upcall method handle
 * @return the resulting char returned from the upcall method handle
 */
short
createNewCharFromCharAndCharFromPointerByUpCallMH(short *charArg1, short charArg2, short (*upcallMH)(short *, short))
{
	short result = (*upcallMH)(charArg1, charArg2);
	return result;
}

/**
 * Add two bytes by invoking an upcall method handle.
 *
 * Note: the passed-in arguments are byte given the byte size
 * in Java is the same size as the char in C code.
 *
 * @param byteArg1 the 1st byte to add
 * @param byteArg2 the 2nd byte to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
add2BytesByUpCallMH(char byteArg1, char byteArg2, char (*upcallMH)(char, char))
{
	char byteSum = (*upcallMH)(byteArg1, byteArg2);
	return byteSum;
}

/**
 * Add two bytes (the 2nd one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * Note: the passed-in arguments are byte given the byte size
 * in Java is the same size as the char in C code.
 *
 * @param byteArg1 a byte to add
 * @param byteArg2 a pointer to byte in char size
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndByteFromPointerByUpCallMH(char byteArg1, char *byteArg2, char (*upcallMH)(char, char *))
{
	char byteSum = (*upcallMH)(byteArg1, byteArg2);
	return byteSum;
}

/**
 * Add two short integers by invoking an upcall method handle.
 *
 * @param shortArg1 the 1st short integer to add
 * @param shortArg2 the 2nd short integer to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
add2ShortsByUpCallMH(short shortArg1, short shortArg2, short (*upcallMH)(short, short))
{
	short shortSum = (*upcallMH)(shortArg1, shortArg2);
	return shortSum;
}

/**
 * Add two short integers (the 1st one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param shortArg1 a pointer to short integer
 * @param shortArg2 a short integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortFromPointerByUpCallMH(short *shortArg1, short shortArg2, short (*upcallMH)(short *, short))
{
	short shortSum = (*upcallMH)(shortArg1, shortArg2);
	return shortSum;
}

/**
 * Add two integers by invoking an upcall method handle.
 *
 * @param intArg1 an integer to add
 * @param intArg2 an integer to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
add2IntsByUpCallMH(int intArg1, int intArg2, int (*upcallMH)(int, int))
{
	int intSum = (*upcallMH)(intArg1, intArg2);
	return intSum;
}

/**
 * Add two integers (the 2nd one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param intArg1 an integer to add
 * @param intArg2 a pointer to integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntFromPointerByUpCallMH(int intArg1, int *intArg2,  int (*upcallMH)(int, int *))
{
	int intSum = (*upcallMH)(intArg1, intArg2);
	return intSum;
}

/**
 * Add three integers by invoking an upcall method handle.
 *
 * @param intArg1 an integer to add
 * @param intArg2 an integer to add
 * @param intArg3 an integer to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
add3IntsByUpCallMH(int intArg1, int intArg2, int intArg3, int (*upcallMH)(int, int, int))
{
	int intSum = (*upcallMH)(intArg1, intArg2, intArg3);
	return intSum;
}

/**
 * Add integers from the va_list with the specified count
 * by invoking an upcall method handle.
 *
 * @param intCount the count of the integers
 * @param intArgList the integer va_list
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntsFromVaListByUpCallMH(int intCount, va_list intVaList, int (*upcallMH)(int, va_list))
{
	int intSum = (*upcallMH)(intCount, intVaList);
	return intSum;
}

/**
 * Add an integer and a char by invoking an upcall method handle.
 *
 * @param intArg the integer to add
 * @param charArg the char to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndCharByUpCallMH(int intArg, char charArg,  int (*upcallMH)(int, char))
{
	int sum = (*upcallMH)(intArg, charArg);
	return sum;
}

/**
 * Add two integers without return value by invoking an upcall method handle.
 *
 * @param intArg1 the 1st integer to add
 * @param intArg2 the 2nd integer to add
 * @param upcallMH an upcall method handle
 * @return void
 */
void
add2IntsReturnVoidByUpCallMH(int intArg1, int intArg2, int (*upcallMH)(int, int))
{
	(*upcallMH)(intArg1, intArg2);
}

/**
 * Add two long integers by invoking an upcall method handle.
 *
 * @param longArg1 the 1st long integer to add
 * @param longArg2 the 2nd long integer to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
add2LongsByUpCallMH(LONG longArg1, LONG longArg2, LONG (*upcallMH)(LONG, LONG))
{
	LONG longSum = (*upcallMH)(longArg1, longArg2);
	return longSum;
}

/**
 * Add two long integers (the 1st one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param longArg1 a pointer to long integer
 * @param longArg2 a long integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongFromPointerByUpCallMH(LONG *longArg1, LONG longArg2, LONG (*upcallMH)(LONG *, LONG))
{
	LONG longSum = (*upcallMH)(longArg1, longArg2);
	return longSum;
}

/**
 * Add long integers from the va_list with the specified count
 * by invoking an upcall method handle.
 *
 * @param longCount the count of the long integers
 * @param longArgList the long va_list
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongsFromVaListByUpCallMH(int longCount, va_list longVaList, LONG (*upcallMH)(int, va_list))
{
	LONG longSum = (*upcallMH)(longCount, longVaList);
	return longSum;
}

/**
 * Add two floats by invoking an upcall method handle.
 *
 * @param floatArg1 the 1st float to add
 * @param floatArg2 the 2nd float to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
add2FloatsByUpCallMH(float floatArg1, float floatArg2, float (*upcallMH)(float, float))
{
	float floatSum = (*upcallMH)(floatArg1, floatArg2);
	return floatSum;
}

/**
 * Add two floats (the 2nd one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param floatArg1 a float
 * @param floatArg2 a pointer to float
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatFromPointerByUpCallMH(float floatArg1, float *floatArg2, float (*upcallMH)(float, float *))
{
	float floatSum = (*upcallMH)(floatArg1, floatArg2);
	return floatSum;
}

/**
 * Add two doubles by invoking an upcall method handle.
 *
 * @param doubleArg1 the 1st double to add
 * @param doubleArg2 the 2nd double to add
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
add2DoublesByUpCallMH(double doubleArg1, double doubleArg2, double (*upcallMH)(double, double))
{
	double doubleSum = (*upcallMH)(doubleArg1, doubleArg2);
	return doubleSum;
}

/**
 * Add two doubles (the 1st one dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param doubleArg1 a pointer to double
 * @param doubleArg2 a double
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoubleFromPointerByUpCallMH(double *doubleArg1, double doubleArg2, double (*upcallMH)(double *, double))
{
	double doubleSum = (*upcallMH)(doubleArg1, doubleArg2);
	return doubleSum;
}

/**
 * Add doubles from the va_list with the specified count
 * by invoking an upcall method handle.
 *
 * @param doubleCount the count of the double arguments
 * @param doubleArgList the double va_list
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoublesFromVaListByUpCallMH(int doubleCount, va_list doubleVaList, double (*upcallMH)(int, va_list))
{
	double doubleSum = (*upcallMH)(doubleCount, doubleVaList);
	return doubleSum;
}

/**
 * Add a boolean and all boolean elements of a struct with the XOR (^) operator
 * by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with two booleans
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromStructWithXorByUpCallMH(int arg1, stru_Bool_Bool arg2, int (*upcallMH)(int, stru_Bool_Bool))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean (dereferenced from a pointer) and all boolean elements of
 * a struct with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a pointer to boolean
 * @param arg2 a struct with two booleans
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolFromPointerAndBoolsFromStructWithXorByUpCallMH(int *arg1, stru_Bool_Bool arg2, int (*upcallMH)(int *, stru_Bool_Bool))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Get a pointer to boolean by adding a boolean (dereferenced from a pointer) and all boolean elements
 * of a struct with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a pointer to boolean
 * @param arg2 a struct with two booleans
 * @param upcallMH an upcall method handle
 * @return a pointer to the XOR result of booleans returned from the upcall method handle
 */
int *
addBoolFromPointerAndBoolsFromStructWithXor_returnBoolPointerByUpCallMH(int *arg1, stru_Bool_Bool arg2, int * (*upcallMH)(int *, stru_Bool_Bool))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add a boolean and two booleans of a struct (dereferenced from a pointer)
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a pointer to struct with two booleans
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromStructPointerWithXorByUpCallMH(int arg1, stru_Bool_Bool *arg2, int (*upcallMH)(int, stru_Bool_Bool *))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean and all booleans of a struct with a nested struct and a boolean
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with a nested struct and a boolean
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromNestedStructWithXorByUpCallMH(int arg1, stru_NestedStruct_Bool arg2, int (*upcallMH)(int, stru_NestedStruct_Bool))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean and all booleans of a struct with a boolean and a nested struct (in reverse order)
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with a boolean and a nested struct
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromNestedStructWithXor_reverseOrderByUpCallMH(int arg1, stru_Bool_NestedStruct arg2, int (*upcallMH)(int, stru_Bool_NestedStruct))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean and all booleans of a struct with a nested array and a boolean
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with a nested array and a boolean
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromStructWithNestedBoolArrayByUpCallMH(int arg1, stru_NestedBoolArray_Bool arg2, int (*upcallMH)(int, stru_NestedBoolArray_Bool))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean and all booleans of a struct with a boolean and a nested array (in reverse order)
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with a boolean and a nested array
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromStructWithNestedBoolArray_reverseOrderByUpCallMH(int arg1, stru_Bool_NestedBoolArray arg2, int (*upcallMH)(int, stru_Bool_NestedBoolArray))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean and all booleans of a struct with a nested struct array and a boolean
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with a nested struct array and a boolean
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromStructWithNestedStructArrayByUpCallMH(int arg1, stru_NestedStruArray_Bool arg2, int (*upcallMH)(int, stru_NestedStruArray_Bool))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Add a boolean and all booleans of a struct with a boolean and a nested struct array
 * (in reverse order) with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a boolean
 * @param arg2 a struct with a boolean and a nested struct array
 * @param upcallMH an upcall method handle
 * @return the XOR result of booleans returned from the upcall method handle
 */
int
addBoolAndBoolsFromStructWithNestedStructArray_reverseOrderByUpCallMH(int arg1, stru_Bool_NestedStruArray arg2, int (*upcallMH)(int, stru_Bool_NestedStruArray))
{
	int boolSum = (*upcallMH)(arg1, arg2);
	return boolSum;
}

/**
 * Get a new struct by adding each boolean element of two structs
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two booleans
 * @param arg2 the 2nd struct with two booleans
 * @param upcallMH an upcall method handle
 * @return a struct with two booleans returned from the upcall method handle
 */
stru_Bool_Bool
add2BoolStructsWithXor_returnStructByUpCallMH(stru_Bool_Bool arg1, stru_Bool_Bool arg2, stru_Bool_Bool (*upcallMH)(stru_Bool_Bool, stru_Bool_Bool))
{
	stru_Bool_Bool boolStruct = (*upcallMH)(arg1, arg2);
	return boolStruct;
}

/**
 * Get a pointer to struct by adding each boolean element of two structs
 * with the XOR (^) operator by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two booleans
 * @param arg2 the 2nd struct with two booleans
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two booleans returned from the upcall method handle
 */
stru_Bool_Bool *
add2BoolStructsWithXor_returnStructPointerByUpCallMH(stru_Bool_Bool *arg1, stru_Bool_Bool arg2, stru_Bool_Bool * (*upcallMH)(stru_Bool_Bool *, stru_Bool_Bool))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Get a new struct by adding each boolean element of two structs with
 * three boolean elements by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three booleans
 * @param arg2 the 2nd struct with three booleans
 * @param upcallMH an upcall method handle
 * @return a struct with three booleans returned from the upcall method handle
 */
stru_Bool_Bool_Bool
add3BoolStructsWithXor_returnStructByUpCallMH(stru_Bool_Bool_Bool arg1, stru_Bool_Bool_Bool arg2, stru_Bool_Bool_Bool (*upcallMH)(stru_Bool_Bool_Bool, stru_Bool_Bool_Bool))
{
	stru_Bool_Bool_Bool boolStruct = (*upcallMH)(arg1, arg2);
	return boolStruct;
}

/**
 * Add a byte and two bytes of a struct by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with two bytes
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromStructByUpCallMH(char arg1, stru_Byte_Byte arg2, char (*upcallMH)(char, stru_Byte_Byte))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte (dereferenced from a pointer) and two bytes
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to byte
 * @param arg2 a struct with two bytes
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteFromPointerAndBytesFromStructByUpCallMH(char *arg1, stru_Byte_Byte arg2, char (*upcallMH)(char *, stru_Byte_Byte))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Get a pointer to byte by adding a byte (dereferenced from a pointer)
 * and two bytes of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to byte
 * @param arg2 a struct with two bytes
 * @param upcallMH an upcall method handle
 * @return a pointer to the sum returned from the upcall method handle
 */
char *
addByteFromPointerAndBytesFromStruct_returnBytePointerByUpCallMH(char *arg1, stru_Byte_Byte arg2, char * (*upcallMH)(char *, stru_Byte_Byte))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add a byte and two bytes of a struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a pointer to struct with two bytes
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromStructPointerByUpCallMH(char arg1, stru_Byte_Byte *arg2, char (*upcallMH)(char, stru_Byte_Byte *))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte and all bytes of a struct with a nested struct
 * and a byte by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with a nested struct and a byte
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromNestedStructByUpCallMH(char arg1, stru_NestedStruct_Byte arg2, char (*upcallMH)(char, stru_NestedStruct_Byte))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte and all bytes of a struct with a byte and a nested struct
 * (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with a byte and a nested struct
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromNestedStruct_reverseOrderByUpCallMH(char arg1, stru_Byte_NestedStruct arg2, char (*upcallMH)(char, stru_Byte_NestedStruct))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte and all byte elements of a struct with a nested byte array
 * and a byte by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with a nested byte array and a byte
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromStructWithNestedByteArrayByUpCallMH(char arg1, stru_NestedByteArray_Byte arg2, char (*upcallMH)(char, stru_NestedByteArray_Byte))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte and all byte elements of a struct with a byte and a nested byte array
 * (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with a byte and a nested byte array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromStructWithNestedByteArray_reverseOrderByUpCallMH(char arg1, stru_Byte_NestedByteArray arg2, char (*upcallMH)(char, stru_Byte_NestedByteArray))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte and all byte elements of a struct with a nested struct array
 * and a byte by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with a nested struct array and a byte
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromStructWithNestedStructArrayByUpCallMH(char arg1, stru_NestedStruArray_Byte arg2, char (*upcallMH)(char, stru_NestedStruArray_Byte))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Add a byte and all byte elements of a struct with a byte and a nested
 * struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a byte
 * @param arg2 a struct with a byte and a nested byte array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
char
addByteAndBytesFromStructWithNestedStructArray_reverseOrderByUpCallMH(char arg1, stru_Byte_NestedStruArray arg2, char (*upcallMH)(char, stru_Byte_NestedStruArray))
{
	char byteSum = (*upcallMH)(arg1, arg2);
	return byteSum;
}

/**
 * Get a new struct by adding each byte element of two structs with
 * two byte elements by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two bytes
 * @param arg2 the 2nd struct with two bytes
 * @param upcallMH an upcall method handle
 * @return a struct with two bytes returned from the upcall method handle
 */
stru_Byte_Byte
add2ByteStructs_returnStructByUpCallMH(stru_Byte_Byte arg1, stru_Byte_Byte arg2, stru_Byte_Byte (*upcallMH)(stru_Byte_Byte, stru_Byte_Byte))
{
	stru_Byte_Byte byteStruct = (*upcallMH)(arg1, arg2);
	return byteStruct;
}

/**
 * Get a pointer to struct by adding each byte element of two structs
 * with two byte elements by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two bytes
 * @param arg2 the 2nd struct with two bytes
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two bytes returned from the upcall method handle
 */
stru_Byte_Byte *
add2ByteStructs_returnStructPointerByUpCallMH(stru_Byte_Byte *arg1, stru_Byte_Byte arg2, stru_Byte_Byte * (*upcallMH)(stru_Byte_Byte *, stru_Byte_Byte))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Get a new struct by adding each byte element of two structs with
 * three byte elements by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three bytes
 * @param arg2 the 2nd struct with three bytes
 * @param upcallMH an upcall method handle
 * @return a struct with three bytes returned from the upcall method handle
 */
stru_Byte_Byte_Byte
add3ByteStructs_returnStructByUpCallMH(stru_Byte_Byte_Byte arg1, stru_Byte_Byte_Byte arg2, stru_Byte_Byte_Byte (*upcallMH)(stru_Byte_Byte_Byte, stru_Byte_Byte_Byte))
{
	stru_Byte_Byte_Byte byteStruct = (*upcallMH)(arg1, arg2);
	return byteStruct;
}

/**
 * Generate a new char by adding a char and two chars of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with two chars
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromStructByUpCallMH(short arg1, stru_Char_Char arg2, short (*upcallMH)(short, stru_Char_Char))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}


/**
 * Generate a new char by adding a char (dereferenced from a pointer)
 * and two chars of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to char
 * @param arg2 a struct with two chars
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharFromPointerAndCharsFromStructByUpCallMH(short *arg1, stru_Char_Char arg2, short (*upcallMH)(short *, stru_Char_Char))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Get a pointer to char by adding a char (dereferenced from a pointer)
 * and two chars of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to char
 * @param arg2 a struct with two chars
 * @param upcallMH an upcall method handle
 * @return a pointer to a new char returned from the upcall method handle
 */
short *
addCharFromPointerAndCharsFromStruct_returnCharPointerByUpCallMH(short *arg1, stru_Char_Char arg2, short * (*upcallMH)(short *, stru_Char_Char))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Generate a new char by adding a char and two chars of struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a pointer to struct with two chars
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromStructPointerByUpCallMH(short arg1, stru_Char_Char *arg2, short (*upcallMH)(short, stru_Char_Char *))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Generate a new char by adding a char and all char elements of a struct
 * with a nested struct and a char by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with a nested struct
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromNestedStructByUpCallMH(short arg1, stru_NestedStruct_Char arg2, short (*upcallMH)(short, stru_NestedStruct_Char))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Generate a new char by adding a char and all char elements of a struct with a char
 * and a nested struct (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with a char and a nested struct
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromNestedStruct_reverseOrderByUpCallMH(short arg1, stru_Char_NestedStruct arg2, short (*upcallMH)(short, stru_Char_NestedStruct))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Generate a new char by adding a char and all char elements of a struct with
 * a nested char array and a char by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with a nested char array and a char
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromStructWithNestedCharArrayByUpCallMH(short arg1, stru_NestedCharArray_Char arg2, short (*upcallMH)(short, stru_NestedCharArray_Char))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Generate a new char by adding a char and all char elements of a struct with a char
 * and a nested char array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with a char and a nested char array
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromStructWithNestedCharArray_reverseOrderByUpCallMH(short arg1, stru_Char_NestedCharArray arg2, short (*upcallMH)(short, stru_Char_NestedCharArray))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Generate a new char by adding a char and all char elements of a struct with
 * a nested struct array and a char by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with a nested char array and a char
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromStructWithNestedStructArrayByUpCallMH(short arg1, stru_NestedStruArray_Char arg2, short (*upcallMH)(short, stru_NestedStruArray_Char))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Generate a new char by adding a char and all char elements of a struct with a char
 * and a nested struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a char
 * @param arg2 a struct with a char and a nested char array
 * @param upcallMH an upcall method handle
 * @return a new char returned from the upcall method handle
 */
short
addCharAndCharsFromStructWithNestedStructArray_reverseOrderByUpCallMH(short arg1, stru_Char_NestedStruArray arg2, short (*upcallMH)(short, stru_Char_NestedStruArray))
{
	short result = (*upcallMH)(arg1, arg2);
	return result;
}

/**
 * Create a new struct by adding each char element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two chars
 * @param arg2 the 2nd struct with two chars
 * @param upcallMH an upcall method handle
 * @return a new struct of with two chars returned from the upcall method handle
 */
stru_Char_Char
add2CharStructs_returnStructByUpCallMH(stru_Char_Char arg1, stru_Char_Char arg2, stru_Char_Char (*upcallMH)(stru_Char_Char, stru_Char_Char))
{
	stru_Char_Char charStruct = (*upcallMH)(arg1, arg2);
	return charStruct;
}

/**
 * Get a pointer to a struct by adding each element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two chars
 * @param arg2 the 2nd struct with two chars
 * @param upcallMH an upcall method handle
 * @return a pointer to a struct of with two chars returned from the upcall method handle
 */
stru_Char_Char *
add2CharStructs_returnStructPointerByUpCallMH(stru_Char_Char *arg1, stru_Char_Char arg2, stru_Char_Char * (*upcallMH)(stru_Char_Char *, stru_Char_Char))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Create a new struct by adding each char element of two structs
 * with three chars by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three chars
 * @param arg2 the 2nd struct with three chars
 * @param upcallMH an upcall method handle
 * @return a new struct of with three chars returned from the upcall method handle
 */
stru_Char_Char_Char
add3CharStructs_returnStructByUpCallMH(stru_Char_Char_Char arg1, stru_Char_Char_Char arg2, stru_Char_Char_Char (*upcallMH)(stru_Char_Char_Char, stru_Char_Char_Char))
{
	stru_Char_Char_Char charStruct = (*upcallMH)(arg1, arg2);
	return charStruct;
}

/**
 * Add a short and two shorts of a struct by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with two shorts
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromStructByUpCallMH(short arg1, stru_Short_Short arg2, short (*upcallMH)(short, stru_Short_Short))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short (dereferenced from a pointer) and two shorts of
 * a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to short
 * @param arg2 a struct with two shorts
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortFromPointerAndShortsFromStructByUpCallMH(short *arg1, stru_Short_Short arg2, short (*upcallMH)(short *, stru_Short_Short))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short (dereferenced from a pointer) and two shorts
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to short
 * @param arg2 a struct with two shorts
 * @param upcallMH an upcall method handle
 * @return a pointer to the sum returned from the upcall method handle
 */
short *
addShortFromPointerAndShortsFromStruct_returnShortPointerByUpCallMH(short *arg1, stru_Short_Short arg2, short * (*upcallMH)(short *, stru_Short_Short))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add a short and two shorts of a struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a pointer to struct with two shorts
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromStructPointerByUpCallMH(short arg1, stru_Short_Short *arg2, short (*upcallMH)(short, stru_Short_Short *))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short and all short elements of a struct with a nested struct
 * and a short by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with a nested struct and a short
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromNestedStructByUpCallMH(short arg1, stru_NestedStruct_Short arg2, short (*upcallMH)(short, stru_NestedStruct_Short))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short and all short elements of a struct with a short and a nested struct
 * (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with a short and a nested struct
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromNestedStruct_reverseOrderByUpCallMH(short arg1, stru_Short_NestedStruct arg2, short (*upcallMH)(short, stru_Short_NestedStruct))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short and all short elements of a struct with a nested short array
 * and a short by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with a nested short array and a short
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromStructWithNestedShortArrayByUpCallMH(short arg1, stru_NestedShortArray_Short arg2, short (*upcallMH)(short, stru_NestedShortArray_Short))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short and all short elements of a struct with a short and a nested
 * short array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with a short and a nested short array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromStructWithNestedShortArray_reverseOrderByUpCallMH(short arg1, stru_Short_NestedShortArray arg2, short (*upcallMH)(short, stru_Short_NestedShortArray))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short and all short elements of a struct with a nested struct
 * array and a short by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with a nested short array and a short
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromStructWithNestedStructArrayByUpCallMH(short arg1, stru_NestedStruArray_Short arg2, short (*upcallMH)(short, stru_NestedStruArray_Short))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Add a short and all short elements of a struct with a short and a nested
 * struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a short
 * @param arg2 a struct with a short and a nested short array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
short
addShortAndShortsFromStructWithNestedStructArray_reverseOrderByUpCallMH(short arg1, stru_Short_NestedStruArray arg2, short (*upcallMH)(short, stru_Short_NestedStruArray))
{
	short shortSum = (*upcallMH)(arg1, arg2);
	return shortSum;
}

/**
 * Get a new struct by adding each short element of two structs
 * with two short elements by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two shorts
 * @param arg2 the 2nd struct with two shorts
 * @param upcallMH an upcall method handle
 * @return a struct with two shorts returned from the upcall method handle
 */
stru_Short_Short
add2ShortStructs_returnStructByUpCallMH(stru_Short_Short arg1, stru_Short_Short arg2, stru_Short_Short (*upcallMH)(stru_Short_Short, stru_Short_Short))
{
	stru_Short_Short shortStruct = (*upcallMH)(arg1, arg2);
	return shortStruct;
}

/**
 * Get a pointer to struct by adding each short element of two structs
 * with two short elements by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two shorts
 * @param arg2 the 2nd struct with two shorts
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two shorts from the upcall method handle
 */
stru_Short_Short *
add2ShortStructs_returnStructPointerByUpCallMH(stru_Short_Short *arg1, stru_Short_Short arg2, stru_Short_Short * (*upcallMH)(stru_Short_Short *, stru_Short_Short))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Get a new struct by adding each short element of two structs with
 * three short elements by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three shorts
 * @param arg2 the 2nd struct with three shorts
 * @param upcallMH an upcall method handle
 * @return a struct with three shorts returned from the upcall method handle
 */
stru_Short_Short_Short
add3ShortStructs_returnStructByUpCallMH(stru_Short_Short_Short arg1, stru_Short_Short_Short arg2, stru_Short_Short_Short (*upcallMH)(stru_Short_Short_Short, stru_Short_Short_Short))
{
	stru_Short_Short_Short shortStruct = (*upcallMH)(arg1, arg2);
	return shortStruct;
}

/**
 * Add an integer and two integers of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with two integers
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromStructByUpCallMH(int arg1, stru_Int_Int arg2, int (*upcallMH)(int, stru_Int_Int))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all elements (integer & short) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with an integer and a short
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntShortFromStructByUpCallMH(int arg1, stru_Int_Short arg2, int (*upcallMH)(int, stru_Int_Short))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all elements (short & integer) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with a short and an integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndShortIntFromStructByUpCallMH(int arg1, stru_Short_Int arg2, int (*upcallMH)(int, stru_Short_Int))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer (dereferenced from a pointer) and two integers
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to integer
 * @param arg2 a struct with two integers
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntFromPointerAndIntsFromStructByUpCallMH(int *arg1, stru_Int_Int arg2,  int (*upcallMH)(int *, stru_Int_Int))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer (dereferenced from a pointer) and two integers
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to integer
 * @param arg2 a struct with two integers
 * @param upcallMH an upcall method handle
 * @return a pointer to the sum returned from the upcall method handle
 */
int *
addIntFromPointerAndIntsFromStruct_returnIntPointerByUpCallMH(int *arg1, stru_Int_Int arg2, int *(*upcallMH)(int *, stru_Int_Int))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add an integer and two integers of a struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a pointer to struct with two integers
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromStructPointerByUpCallMH(int arg1, stru_Int_Int *arg2, int (*upcallMH)(int, stru_Int_Int *))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all integer elements of a struct with a nested struct
 * and an integer by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with a nested struct and an integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromNestedStructByUpCallMH(int arg1, stru_NestedStruct_Int arg2, int (*upcallMH)(int, stru_NestedStruct_Int))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all integer elements of a struct with an integer and
 * a nested struct (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with an integer and a nested struct
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromNestedStruct_reverseOrderByUpCallMH(int arg1, stru_Int_NestedStruct arg2, int (*upcallMH)(int, stru_Int_NestedStruct))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all integer elements of a struct with a nested integer array
 * and an integer by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with a nested integer array and an integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromStructWithNestedIntArrayByUpCallMH(int arg1, stru_NestedIntArray_Int arg2, int (*upcallMH)(int, stru_NestedIntArray_Int))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all integer elements of a struct with an integer and a
 * nested integer array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with an integer and a nested integer array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromStructWithNestedIntArray_reverseOrderByUpCallMH(int arg1, stru_Int_NestedIntArray arg2, int (*upcallMH)(int, stru_Int_NestedIntArray))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all integer elements of a struct with a nested struct array
 * and an integer by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with a nested integer array and an integer
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromStructWithNestedStructArrayByUpCallMH(int arg1, stru_NestedStruArray_Int arg2, int (*upcallMH)(int, stru_NestedStruArray_Int))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Add an integer and all integer elements of a struct with an integer and a nested
 * struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 an integer
 * @param arg2 a struct with an integer and a nested integer array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
int
addIntAndIntsFromStructWithNestedStructArray_reverseOrderByUpCallMH(int arg1, stru_Int_NestedStruArray arg2, int (*upcallMH)(int, stru_Int_NestedStruArray))
{
	int intSum = (*upcallMH)(arg1, arg2);
	return intSum;
}

/**
 * Get a new struct by adding each integer element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two integers
 * @param arg2 the 2nd struct with two integers
 * @param upcallMH an upcall method handle
 * @return a struct with two integers returned from the upcall method handle
 */
stru_Int_Int
add2IntStructs_returnStructByUpCallMH(stru_Int_Int arg1, stru_Int_Int arg2, stru_Int_Int (*upcallMH)(stru_Int_Int, stru_Int_Int))
{
	stru_Int_Int intStruct = (*upcallMH)(arg1, arg2);
	return intStruct;
}

/**
 * Get a pointer to struct by adding each integer element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two integers
 * @param arg2 the 2nd struct with two integers
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two integers returned from the upcall method handle
 */
stru_Int_Int *
add2IntStructs_returnStructPointerByUpCallMH(stru_Int_Int *arg1, stru_Int_Int arg2, stru_Int_Int * (*upcallMH)(stru_Int_Int *, stru_Int_Int))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Get a new struct by adding each integer element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three integers
 * @param arg2 the 2nd struct with three integers
 * @param upcallMH an upcall method handle
 * @return a struct with three integers returned from the upcall method handle
 */
stru_Int_Int_Int
add3IntStructs_returnStructByUpCallMH(stru_Int_Int_Int arg1, stru_Int_Int_Int arg2, stru_Int_Int_Int (*upcallMH)(stru_Int_Int_Int, stru_Int_Int_Int))
{
	stru_Int_Int_Int intStruct = (*upcallMH)(arg1, arg2);
	return intStruct;
}

/**
 * Add a long and two longs of a struct by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with two longs
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromStructByUpCallMH(LONG arg1, stru_Long_Long arg2, LONG (*upcallMH)(LONG, stru_Long_Long))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add an integer and all elements (int & long) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 an int
 * @param arg2 a struct with an integer and a long
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addIntAndIntLongFromStructByUpCallMH(int arg1, stru_Int_Long arg2, LONG (*upcallMH)(int, stru_Int_Long))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add an integer and all elements (long & int) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 an int
 * @param arg2 a struct with a long and an int
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addIntAndLongIntFromStructByUpCallMH(int arg1, stru_Long_Int arg2, LONG (*upcallMH)(int, stru_Long_Int))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long (dereferenced from a pointer) and two longs
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to long
 * @param arg2 a struct with two longs
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongFromPointerAndLongsFromStructByUpCallMH(LONG *arg1, stru_Long_Long arg2, LONG (*upcallMH)(LONG *, stru_Long_Long))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long (dereferenced from a pointer) and two longs
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to long
 * @param arg2 a struct with two longs
 * @param upcallMH an upcall method handle
 * @return a pointer to the sum returned from the upcall method handle
 */
LONG *
addLongFromPointerAndLongsFromStruct_returnLongPointerByUpCallMH(LONG *arg1, stru_Long_Long arg2, LONG * (*upcallMH)(LONG *, stru_Long_Long))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add a long and two longs of a struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a pointer to struct with two longs
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromStructPointerByUpCallMH(LONG arg1, stru_Long_Long *arg2, LONG (*upcallMH)(LONG, stru_Long_Long *))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long and all long elements of a struct with a nested struct
 * and a long by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with a nested struct and long
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromNestedStructByUpCallMH(LONG arg1, stru_NestedStruct_Long arg2, LONG (*upcallMH)(LONG, stru_NestedStruct_Long))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long and all long elements of a struct with a long and a nested
 * struct (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with a long and a nested struct
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromNestedStruct_reverseOrderByUpCallMH(LONG arg1, stru_Long_NestedStruct arg2, LONG (*upcallMH)(LONG, stru_Long_NestedStruct))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long and all long elements of a struct with a nested long
 * array and a long by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with a nested long array and a long
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromStructWithNestedLongArrayByUpCallMH(LONG arg1, stru_NestedLongArray_Long arg2, LONG (*upcallMH)(LONG, stru_NestedLongArray_Long))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long and all long elements of a struct with a long and a nested
 * long array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with a long and a nested long array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromStructWithNestedLongArray_reverseOrderByUpCallMH(LONG arg1, stru_Long_NestedLongArray arg2, LONG (*upcallMH)(LONG, stru_Long_NestedLongArray))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long and all long elements of a struct with a nested struct
 * array and a long by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with a nested long array and a long
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromStructWithNestedStructArrayByUpCallMH(LONG arg1, stru_NestedStruArray_Long arg2, LONG (*upcallMH)(LONG, stru_NestedStruArray_Long))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Add a long and all long elements of a struct with a long and a nested
 * struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a long
 * @param arg2 a struct with a long and a nested long array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
LONG
addLongAndLongsFromStructWithNestedStructArray_reverseOrderByUpCallMH(LONG arg1, stru_Long_NestedStruArray arg2, LONG (*upcallMH)(LONG, stru_Long_NestedStruArray))
{
	LONG longSum = (*upcallMH)(arg1, arg2);
	return longSum;
}

/**
 * Get a new struct by adding each long element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two longs
 * @param arg2 the 2nd struct with two longs
 * @param upcallMH an upcall method handle
 * @return a struct with two longs returned from the upcall method handle
 */
stru_Long_Long
add2LongStructs_returnStructByUpCallMH(stru_Long_Long arg1, stru_Long_Long arg2, stru_Long_Long (*upcallMH)(stru_Long_Long, stru_Long_Long))
{
	stru_Long_Long longStruct = (*upcallMH)(arg1, arg2);
	return longStruct;
}

/**
 * Get a pointer to struct by adding each long element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two longs
 * @param arg2 the 2nd struct with two longs
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two longs returned from the upcall method handle
 */
stru_Long_Long *
add2LongStructs_returnStructPointerByUpCallMH(stru_Long_Long *arg1, stru_Long_Long arg2, stru_Long_Long * (*upcallMH)(stru_Long_Long *, stru_Long_Long))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Get a new struct by adding each long element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three longs
 * @param arg2 the 2nd struct with three longs
 * @param upcallMH an upcall method handle
 * @return a struct with three longs returned from the upcall method handle
 */
stru_Long_Long_Long
add3LongStructs_returnStructByUpCallMH(stru_Long_Long_Long arg1, stru_Long_Long_Long arg2, stru_Long_Long_Long (*upcallMH)(stru_Long_Long_Long, stru_Long_Long_Long))
{
	stru_Long_Long_Long longStruct = (*upcallMH)(arg1, arg2);
	return longStruct;
}

/**
 * Add a float and two floats of a struct by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with two floats
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromStructByUpCallMH(float arg1, stru_Float_Float arg2, float (*upcallMH)(float, stru_Float_Float))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float (dereferenced from a pointer) and two floats
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to float
 * @param arg2 a struct with two floats
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatFromPointerAndFloatsFromStructByUpCallMH(float *arg1, stru_Float_Float arg2, float (*upcallMH)(float *, stru_Float_Float))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float (dereferenced from a pointer) and two floats
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to float
 * @param arg2 a struct with two floats
 * @param upcallMH an upcall method handle
 * @return a pointer to the sum returned from the upcall method handle
 */
float *
addFloatFromPointerAndFloatsFromStruct_returnFloatPointerByUpCallMH(float *arg1, stru_Float_Float arg2, float * (*upcallMH)(float *, stru_Float_Float))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add a float and two floats of a struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a pointer to struct with two floats
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromStructPointerByUpCallMH(float arg1, stru_Float_Float *arg2, float (*upcallMH)(float, stru_Float_Float *))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float and all float elements of a struct with a nested
 * struct and a float by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with a nested struct and a float
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromNestedStructByUpCallMH(float arg1, stru_NestedStruct_Float arg2, float (*upcallMH)(float, stru_NestedStruct_Float))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float and all float elements of a struct with a float and a nested struct
 * (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with a float and a nested struct
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromNestedStruct_reverseOrderByUpCallMH(float arg1, stru_Float_NestedStruct arg2, float (*upcallMH)(float, stru_Float_NestedStruct))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float and all float elements of a struct with a nested
 * float array and a float by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with a nested float array and a float
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromStructWithNestedFloatArrayByUpCallMH(float arg1, stru_NestedFloatArray_Float arg2, float (*upcallMH)(float, stru_NestedFloatArray_Float))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float and all float elements of a struct with a float and a nested
 * float array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with a float and a nested float array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromStructWithNestedFloatArray_reverseOrderByUpCallMH(float arg1, stru_Float_NestedFloatArray arg2, float (*upcallMH)(float, stru_Float_NestedFloatArray))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float and all float elements of a struct with a nested
 * struct array and a float by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with a nested float array and a float
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromStructWithNestedStructArrayByUpCallMH(float arg1, stru_NestedStruArray_Float arg2, float (*upcallMH)(float, stru_NestedStruArray_Float))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Add a float and all float elements of a struct with a float and a nested
 * struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a float
 * @param arg2 a struct with a float and a nested float array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
float
addFloatAndFloatsFromStructWithNestedStructArray_reverseOrderByUpCallMH(float arg1, stru_Float_NestedStruArray arg2, float (*upcallMH)(float, stru_Float_NestedStruArray))
{
	float floatSum = (*upcallMH)(arg1, arg2);
	return floatSum;
}

/**
 * Create a new struct by adding each float element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two floats
 * @param arg2 the 2nd struct with two floats
 * @param upcallMH an upcall method handle
 * @return a struct with two floats returned from the upcall method handle
 */
stru_Float_Float
add2FloatStructs_returnStructByUpCallMH(stru_Float_Float arg1, stru_Float_Float arg2, stru_Float_Float (*upcallMH)(stru_Float_Float, stru_Float_Float))
{
	stru_Float_Float floatStruct = (*upcallMH)(arg1, arg2);
	return floatStruct;
}

/**
 * Get a pointer to struct by adding each float element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two floats
 * @param arg2 the 2nd struct with two floats
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two floats returned from the upcall method handle
 */
stru_Float_Float *
add2FloatStructs_returnStructPointerByUpCallMH(stru_Float_Float *arg1, stru_Float_Float arg2, stru_Float_Float * (*upcallMH)(stru_Float_Float *, stru_Float_Float))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Create a new struct by adding each float element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three floats
 * @param arg2 the 2nd struct with three floats
 * @param upcallMH an upcall method handle
 * @return a struct with three floats returned from the upcall method handle
 */
stru_Float_Float_Float
add3FloatStructs_returnStructByUpCallMH(stru_Float_Float_Float arg1, stru_Float_Float_Float arg2, stru_Float_Float_Float (*upcallMH)(stru_Float_Float_Float, stru_Float_Float_Float))
{
	stru_Float_Float_Float floatStruct = (*upcallMH)(arg1, arg2);
	return floatStruct;
}

/**
 * Add a double and two doubles of a struct by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with two doubles
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromStructByUpCallMH(double arg1, stru_Double_Double arg2, double (*upcallMH)(double, stru_Double_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all elements (float & double) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a float and a double
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndFloatDoubleFromStructByUpCallMH(double arg1, stru_Float_Double arg2, double (*upcallMH)(double, stru_Float_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all elements (int & double) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with an int and a double
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndIntDoubleFromStructByUpCallMH(double arg1, stru_Int_Double arg2, double (*upcallMH)(double, stru_Int_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all elements (double & float) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a double and a float
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoubleFloatFromStructByUpCallMH(double arg1, stru_Double_Float arg2, double (*upcallMH)(double, stru_Double_Float))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all elements (double & int) of a struct
 * by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a double and an int
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoubleIntFromStructByUpCallMH(double arg1, stru_Double_Int arg2, double (*upcallMH)(double, stru_Double_Int))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double (dereferenced from a pointer) and two doubles
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to double
 * @param arg2 a struct with two doubles
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleFromPointerAndDoublesFromStructByUpCallMH(double *arg1, stru_Double_Double arg2, double (*upcallMH)(double *, stru_Double_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double (dereferenced from a pointer) and two doubles
 * of a struct by invoking an upcall method handle.
 *
 * @param arg1 a pointer to double
 * @param arg2 a struct with two doubles
 * @param upcallMH an upcall method handle
 * @return a pointer to the sum returned from the upcall method handle
 */
double *
addDoubleFromPointerAndDoublesFromStruct_returnDoublePointerByUpCallMH(double *arg1, stru_Double_Double arg2, double * (*upcallMH)(double *, stru_Double_Double))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Add a double and two doubles of a struct (dereferenced from a pointer)
 * by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a pointer to struct with two doubles
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromStructPointerByUpCallMH(double arg1, stru_Double_Double *arg2, double (*upcallMH)(double, stru_Double_Double *))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all doubles of a struct with a nested struct
 * and a double by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a nested struct and a double
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromNestedStructByUpCallMH(double arg1, stru_NestedStruct_Double arg2, double (*upcallMH)(double, stru_NestedStruct_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all doubles of a struct with a double and a nested struct
 * (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a double a nested struct
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromNestedStruct_reverseOrderByUpCallMH(double arg1, stru_Double_NestedStruct arg2, double (*upcallMH)(double, stru_Double_NestedStruct))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all double elements of a struct with a nested
 * double array and a double by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a nested double array and a double
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromStructWithNestedDoubleArrayByUpCallMH(double arg1, stru_NestedDoubleArray_Double arg2, double (*upcallMH)(double, stru_NestedDoubleArray_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all double elements of a struct with a double and a nested
 * double array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a double and a nested double array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromStructWithNestedDoubleArray_reverseOrderByUpCallMH(double arg1, stru_Double_NestedDoubleArray arg2, double (*upcallMH)(double, stru_Double_NestedDoubleArray))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all double elements of a struct with a nested struct array
 * and a double by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a nested double array and a double
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromStructWithNestedStructArrayByUpCallMH(double arg1, stru_NestedStruArray_Double arg2, double (*upcallMH)(double, stru_NestedStruArray_Double))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Add a double and all double elements of a struct with a double and a nested
 * struct array (in reverse order) by invoking an upcall method handle.
 *
 * @param arg1 a double
 * @param arg2 a struct with a double and a nested double array
 * @param upcallMH an upcall method handle
 * @return the sum returned from the upcall method handle
 */
double
addDoubleAndDoublesFromStructWithNestedStructArray_reverseOrderByUpCallMH(double arg1, stru_Double_NestedStruArray arg2, double (*upcallMH)(double, stru_Double_NestedStruArray))
{
	double doubleSum = (*upcallMH)(arg1, arg2);
	return doubleSum;
}

/**
 * Create a new struct by adding each double element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with two doubles
 * @param arg2 the 2nd struct with two doubles
 * @param upcallMH an upcall method handle
 * @return a struct with two doubles returned from the upcall method handle
 */
stru_Double_Double
add2DoubleStructs_returnStructByUpCallMH(stru_Double_Double arg1, stru_Double_Double arg2, stru_Double_Double (*upcallMH)(stru_Double_Double, stru_Double_Double))
{
	stru_Double_Double doubleStruct = (*upcallMH)(arg1, arg2);
	return doubleStruct;
}

/**
 * Get a pointer to struct by adding each double element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 a pointer to the 1st struct with two doubles
 * @param arg2 the 2nd struct with two doubles
 * @param upcallMH an upcall method handle
 * @return a pointer to struct with two doubles returned from the upcall method handle
 */
stru_Double_Double *
add2DoubleStructs_returnStructPointerByUpCallMH(stru_Double_Double *arg1, stru_Double_Double arg2, stru_Double_Double * (*upcallMH)(stru_Double_Double *, stru_Double_Double))
{
	arg1 = (*upcallMH)(arg1, arg2);
	return arg1;
}

/**
 * Create a new struct by adding each double element of two structs
 * by invoking an upcall method handle.
 *
 * @param arg1 the 1st struct with three doubles
 * @param arg2 the 2nd struct with three doubles
 * @param upcallMH an upcall method handle
 * @return a struct with three doubles returned from the upcall method handle
 */
stru_Double_Double_Double
add3DoubleStructs_returnStructByUpCallMH(stru_Double_Double_Double arg1, stru_Double_Double_Double arg2, stru_Double_Double_Double (*upcallMH)(stru_Double_Double_Double, stru_Double_Double_Double))
{
	stru_Double_Double_Double doubleStruct = (*upcallMH)(arg1, arg2);
	return doubleStruct;
}

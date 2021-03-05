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
 * This file contains the native code used by org.openj9.test.jep389.downcall.PrimitiveTypeTests
 * via a Clinker FFI DownCall.
 *
 * Created by jincheng@ca.ibm.com
 */

#include <stdio.h>
#include <stdbool.h>

/**
 * Add two integers.
 *
 * @param intArg1 the 1st integer to add
 * @param intArg2 the 2nd integer to add
 * @return the resulting integer number
 */
int testAdd2Int(int intArg1, int intArg2) {
	int result = intArg1 + intArg2;
	return result;
}

/**
 * Add three integers.
 *
 * @param intArg1 the 1st integer to add
 * @param intArg2 the 2nd integer to add
 * @param intArg3 the 3rd integer to add
 * @return the resulting integer number
 */
int testAdd3Int(int intArg1, int intArg2, int intArg3) {
	int result = intArg1 + intArg2 + intArg3;
	return result;
}

/**
 * Add an integer and a character.
 *
 * @param intArg the integer to add
 * @param charArg the character to add
 * @return the resulting integer number
 */
int testAddIntAndChar(int intArg, char charArg) {
	int result = intArg + charArg;
	return result;
}

/**
 * Add two integers without return value.
 *
 * @param intArg1 the 1st integer to add
 * @param intArg2 the 2nd integer to add
 * @return void
 */
void testAdd2IntReturnVoid(int intArg1, int intArg2) {
	int result = intArg1 + intArg2;
}

/**
 * Add two boolean numbers with the OR (||) operator.
 *
 * @param boolArg1 the 1st boolean number to add
 * @param boolArg2 the 2nd boolean number to add
 * @return the resulting boolean number
 */
bool testAdd2BoolWithOr(bool boolArg1, bool boolArg2) {
	bool result = (boolArg1 || boolArg2);
	return result;
}

/**
 * Generate a new character by manipulating two characters.
 *
 * @param charArg1 the 1st character to add
 * @param charArg2 the 2nd character to add
 * @return the resulting character
 */
char testGenNewChar(char charArg1, char charArg2) {
	int diff = (charArg2 >= charArg1) ? (charArg2 - charArg1) : (charArg1 - charArg2);
	diff = (diff > 5) ? 5 : diff;
	char result = diff + 'A';
	return result;
}

/**
 * Add two byte numbers.
 *
 * Note: the passed-in arguments are byte numbers given the byte size
 * in Java is the same size as the character in C code.
 *
 * @param byteArg1 the 1st byte number to add
 * @param byteArg2 the 2nd byte number to add
 * @return the resulting number
 */
char testAdd2Byte(char byteArg1, char byteArg2) {
	char result = byteArg1 + byteArg2;
	return result;
}

/**
 * Add two short integers.
 *
 * @param shortArg1 the 1st short integer to add
 * @param shortArg2 the 2nd short integer to add
 * @return the resulting short integer
 */
short testAdd2Short(short shortArg1, short shortArg2) {
	short result = shortArg1 + shortArg2;
	return result;
}

/**
 * Add two long integers.
 *
 * @param longArg1 the 1st long integer to add
 * @param longArg2 the 2nd long integer to add
 * @return the resulting long integer
 */
long testAdd2Long(long longArg1, long longArg2) {
	long result = longArg1 + longArg2;
	return result;
}

/**
 * Add two float numbers.
 *
 * @param floatArg1 the 1st float number to add
 * @param floatArg2 the 2nd float number to add
 * @return the resulting float number
 */
float testAdd2Float(float floatArg1, float floatArg2) {
	float result = floatArg1 + floatArg2;
	return result;
}

/**
 * Add two double numbers.
 *
 * @param doubleArg1 the 1st double number to add
 * @param doubleArg2 the 2nd double number to add
 * @return the resulting double number
 */
double testAdd2Double(double doubleArg1, double doubleArg2) {
	double result = doubleArg1 + doubleArg2;
	return result;
}

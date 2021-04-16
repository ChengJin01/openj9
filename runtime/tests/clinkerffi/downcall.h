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
 * This file contains the structs required in the native code used by org.openj9.test.jep389.downcall.StructTests
 * via a Clinker FFI DownCall.
 *
 * Note: The tailing padding bytes intended for alignment in a struct with nested structs
 * must be explicitly specified on Windows (especially with its elements less then 4 bytes)
 * to guarantee ffi_call locates the address of each element of the struct correctly.
 *
 * Created by jincheng@ca.ibm.com
 */

#if defined(WIN64)
/* the C_LONG_LONG layout maps to long long (8bytes) on Windows 64-bit */
typedef long long LONG;
#else /* WIN64 */
typedef long LONG;
#endif /* defined(WIN64) */

typedef struct stru_Bool_Bool stru_Bool_Bool;
typedef struct stru_NestedStruct_Bool stru_NestedStruct_Bool;
typedef struct stru_Bool_NestedStruct stru_Bool_NestedStruct;
typedef struct stru_NestedBoolArray_Bool stru_NestedBoolArray_Bool;
typedef struct stru_NestedStruArray_Bool stru_NestedStruArray_Bool;

typedef struct stru_Byte_Byte stru_Byte_Byte;
typedef struct stru_NestedStruct_Byte stru_NestedStruct_Byte;
typedef struct stru_Byte_NestedStruct stru_Byte_NestedStruct;
typedef struct stru_NestedByteArray_Byte stru_NestedByteArray_Byte;
typedef struct stru_NestedStruArray_Byte stru_NestedStruArray_Byte;

typedef struct stru_Char_Char stru_Char_Char;
typedef struct stru_NestedStruct_Char stru_NestedStruct_Char;
typedef struct stru_Char_NestedStruct stru_Char_NestedStruct;
typedef struct stru_NestedCharArray_Char stru_NestedCharArray_Char;
typedef struct stru_NestedStruArray_Char stru_NestedStruArray_Char;

typedef struct stru_Short_Short stru_Short_Short;
typedef struct stru_NestedStruct_Short stru_NestedStruct_Short;
typedef struct stru_Short_NestedStruct stru_Short_NestedStruct;
typedef struct stru_NestedShortArray_Short stru_NestedShortArray_Short;
typedef struct stru_NestedStruArray_Short stru_NestedStruArray_Short;

typedef struct stru_Int_Int stru_Int_Int;
typedef struct stru_Int_Short stru_Int_Short;
typedef struct stru_Short_Int stru_Short_Int;
typedef struct stru_NestedStruct_Int stru_NestedStruct_Int;
typedef struct stru_Int_NestedStruct stru_Int_NestedStruct;

typedef struct stru_NestedIntArray_Int stru_NestedIntArray_Int;
typedef struct stru_NestedStruArray_Int stru_NestedStruArray_Int;

typedef struct stru_Long_Long stru_Long_Long;
typedef struct stru_Int_Long stru_Int_Long;
typedef struct stru_Long_Int stru_Long_Int;
typedef struct stru_NestedStruct_Long stru_NestedStruct_Long;
typedef struct stru_Long_NestedStruct stru_Long_NestedStruct;
typedef struct stru_NestedLongArray_Long stru_NestedLongArray_Long;
typedef struct stru_NestedStruArray_Long stru_NestedStruArray_Long;

typedef struct stru_Float_Float stru_Float_Float;
typedef struct stru_NestedStruct_Float stru_NestedStruct_Float;
typedef struct stru_Float_NestedStruct stru_Float_NestedStruct;
typedef struct stru_NestedFloatArray_Float stru_NestedFloatArray_Float;
typedef struct stru_NestedStruArray_Float stru_NestedStruArray_Float;

typedef struct stru_Double_Double stru_Double_Double;
typedef struct stru_Double_Float stru_Double_Float;
typedef struct stru_Float_Double stru_Float_Double;
typedef struct stru_NestedStruct_Double stru_NestedStruct_Double;
typedef struct stru_Double_NestedStruct stru_Double_NestedStruct;
typedef struct stru_NestedDoubleArray_Double stru_NestedDoubleArray_Double;
typedef struct stru_NestedStruArray_Double stru_NestedStruArray_Double;

struct stru_Bool_Bool {
	int elem1;
	int elem2;
};

struct stru_NestedStruct_Bool {
	stru_Bool_Bool elem1;
	int elem2;
};

struct stru_Bool_NestedStruct {
	int elem1;
	stru_Bool_Bool elem2;
};

struct stru_NestedBoolArray_Bool {
	int elem1[2];
	int elem2;
};

struct stru_NestedStruArray_Bool {
	stru_Bool_Bool elem1[2];
	int elem2;
};

struct stru_Byte_Byte {
	char elem1;
	char elem2;
};

struct stru_NestedStruct_Byte {
	stru_Byte_Byte elem1;
	char elem2;
#if defined(WIN64)
	char padding[1];
#endif /* defined(WIN64) */
};

struct stru_Byte_NestedStruct {
	char elem1;
	stru_Byte_Byte elem2;
#if defined(WIN64)
	char padding[1];
#endif /* defined(WIN64) */
};

struct stru_NestedByteArray_Byte {
	char elem1[2];
	char elem2;
#if defined(WIN64)
	char padding[1];
#endif /* defined(WIN64) */
};

struct stru_NestedStruArray_Byte {
	stru_Byte_Byte elem1[2];
	char elem2;
#if defined(WIN64)
	char padding[3];
#endif /* defined(WIN64) */
};

struct stru_Char_Char {
	short elem1;
	short elem2;
};

struct stru_NestedStruct_Char {
	stru_Char_Char elem1;
	short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_Char_NestedStruct {
	short elem1;
	stru_Char_Char elem2;
#if defined(WIN64)
	short padding[1];
#endif /* defined(WIN64) */
};

struct stru_NestedCharArray_Char {
	short elem1[2];
	short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_NestedStruArray_Char {
	stru_Char_Char elem1[2];
	short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_Short_Short {
	short elem1;
	short elem2;
};

struct stru_NestedStruct_Short {
	stru_Short_Short elem1;
	short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_NestedShortArray_Short {
	short elem1[2];
	short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_NestedStruArray_Short {
	stru_Short_Short elem1[2];
	short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_Short_NestedStruct {
	short elem1;
	stru_Short_Short elem2;
#if defined(WIN64)
	char padding[2];
#endif /* defined(WIN64) */
};

struct stru_Int_Int {
	int elem1;
	int elem2;
};

struct stru_Int_Short {
	int elem1;
	short elem2;
};

struct stru_Short_Int {
	short elem1;
	int elem2;
};

struct stru_NestedStruct_Int {
	stru_Int_Int elem1;
	int elem2;
};

struct stru_Int_NestedStruct {
	int elem1;
	stru_Int_Int elem2;
};

struct stru_NestedIntArray_Int {
	int elem1[2];
	int elem2;
};

struct stru_NestedStruArray_Int {
	stru_Int_Int elem1[2];
	int elem2;
};

struct stru_Long_Long {
	LONG elem1;
	LONG elem2;
};

struct stru_Int_Long {
	int elem1;
	LONG elem2;
};

struct stru_Long_Int {
	int elem1;
	LONG elem2;
};

struct stru_NestedStruct_Long {
	stru_Long_Long elem1;
	LONG elem2;
};

struct stru_Long_NestedStruct {
	LONG elem1;
	stru_Long_Long elem2;
};

struct stru_NestedLongArray_Long {
	LONG elem1[2];
	LONG elem2;
};

struct stru_NestedStruArray_Long {
	stru_Long_Long elem1[2];
	LONG elem2;
};

struct stru_Float_Float {
	float elem1;
	float elem2;
};

struct stru_NestedStruct_Float {
	stru_Float_Float elem1;
	float elem2;
};

struct stru_Float_NestedStruct {
	float elem1;
	stru_Float_Float elem2;
	/* The padding bytes must be explicitly specified to correspond
	 * to the padding element (in the same size) being set up for ffi_call.
	 */
	char padding[4];
};

struct stru_NestedFloatArray_Float {
	float elem1[2];
	float elem2;
};

struct stru_NestedStruArray_Float {
	stru_Float_Float elem1[2];
	float elem2;
};

struct stru_Double_Double {
	double elem1;
	double elem2;
};

struct stru_Double_Float {
	double elem1;
	float elem2;
};

struct stru_Float_Double {
	float elem1;
	double elem2;
};

struct stru_NestedStruct_Double {
	stru_Double_Double elem1;
	double elem2;
};

struct stru_Double_NestedStruct {
	double elem1;
	stru_Double_Double elem2;
};

struct stru_NestedDoubleArray_Double {
	double elem1[2];
	double elem2;
};

struct stru_NestedStruArray_Double {
	stru_Double_Double elem1[2];
	double elem2;
};

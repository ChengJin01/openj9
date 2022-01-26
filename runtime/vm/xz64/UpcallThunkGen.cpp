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

#include "j9.h"
#include "ut_j9vm.h"


/**
 * @file: UpCallThunkGen.cpp
 * @brief: Service routines dealing with platform-ABI specifics for upcall
 *
 * Given an upcallMetaData, an upcall thunk/adaptor will be generated;
 * Given an upcallSignature, argListPtr, and argIndex, a pointer to that specific arg will be returned
 */

extern "C" {

#if JAVA_SPEC_VERSION >= 16


//#define ROUND_UP_SLOT(si)	(((si) + 7) / 8)

static I_32
generateLAYInstruction(I_8 *instructionPtr, I_32 R1, I_32 X2, I_32 B2, I_32 D2)
{
	// LAY R1,D2(X2,B2)
	I_32 instructionSize = 6;
	*((I_32 *)instructionPtr) = 0xE3000000 | ((R1 & 0xF) << 20) | ((X2 & 0xF) << 16) | ((B2 & 0xF) << 12) | (D2 & 0xFFF);
	instructionPtr[4] = ((D2 & 0xFF000) >> 12);
	instructionPtr[5] = 0x71;
	return instructionSize;
}

static I_32
generateBASR(I_8 *instructionPtr, I_32 R1, I_32 R2)
{
	// BASR R1,R2
	I_32 instructionSize = 2;
	I_32 opcode = 0x0D00;
	*((I_16 *)instructionPtr) = (opcode | (((I_16)R1) << 4) | (((I_16)R2)));
	return instructionSize;
}
static I_32
generateInsertImmediateInstruction(I_8 *instructionPtr, I_32 R1, I_32 I2, bool isHigherWordInsert)
{
	// IIHF R1, I2 or IILF R1, I2
	I_32 instructionSize = 6;
	I_32 opcode = isHigherWordInsert ? 0xC0080000 : 0xC0090000;
	*((I_32 *)instructionPtr) = (opcode | (R1 << 20));
	instructionPtr += 2;
	*((I_32 *)instructionPtr) = I2;
	return instructionSize;
}

static I_32
generateAddRegisterImmediateInstruction(I_8 *instructionPtr, I_32 R1, I_32 I2, bool is64BitAdd)
{
	// "AFI R1, I2" or "AGFI R1,I2"
	I_32 instructionSize = is64BitAdd ? 6 : 4;
	I_32 opcode = is64BitAdd ? 0xC2080000 : 0xC2090000;
	*((I_32 *)instructionPtr) = (opcode | (R1 << 20) | ((I2 & 0xFFFF0000) >> 16));
        instructionPtr[4] = (I_8)((0x0000FF00 & I2) >> 8);
	instructionPtr[5] = (I_8)(0x000000FF & I2);
	return instructionSize;
}

static I_32
generateLoadInstruction(I_8 *instructionPtr, I_32 R1, I_32 X2, I_32 B2, I_32 D2, bool is64BitLoad)
{
	// Load 32-bit data from memory. Instruction format: "L R1,D2(X2,B2)"
	I_32 instructionSize = 4;
	I_32 opcode = is64BitLoad ? 0xE3000000 : 0x58000000;
	*((I_32 *)instructionPtr) = (opcode | (R1 << 20) | (X2 << 16) | (B2 << 12) | (D2 & 0x00000FFF));

	// If loading 64-bit data, then add the following two bytes. Instruction format: "LG R1,D2(X2,B2)"
	if (is64BitLoad)
	{
		instructionPtr[4] = (I_8)(D2 & 0x000FFF00);
		instructionPtr[5] = 0x04;
		instructionSize = 6;
	}
	return instructionSize;
} 

static I_32
generateStoreInstruction(I_8 *instructionPtr, I_32 R1, I_32 X2, I_32 B2, I_32 D2, bool is64BitStore)
{
	// Store 32-bit data to memory. Instruction format: "ST R1, D2(X2,B2)"
	
	I_32 instructionSize = 4;
	I_32 opcode = is64BitStore ? 0xE3000000 : 0x50000000;
	*((I_32 *)instructionPtr) = (opcode | (R1 << 20) | (X2 << 16) | (B2 << 12) | (D2 & 0x00000FFF));

	if (is64BitStore)
	{
		// STG "R1, D2(X2,B2)"
		instructionPtr[4] = (I_8)((D2 & 0x000FF000) >> 12);
		instructionPtr[5] = 0x24;
		instructionSize = 6;
	}
	return instructionSize;
}

/**
 * @brief Generate straight sequence of instructions to copy back result
 * @param instrArray[in/out] A pointer to the thunk memory
 * @param currIdx[in/out] A pointer to the current instruction index
 * @param resSize[in] The size in byte to copy, guarantee to be not more than 64
 * @param paramOffset[in] Offset to the parameter area
 * @return none
 *
 * Details:
 *   a static routine to generate instructions to copy back the upcall result
 *   fixed registers are used
 *     load the hidden parameter into register number 4
 *     load the result in sequence in no more than 8 registers starting from register 5
 *     store these registers back into the memory designated by the hidden parameter
 *
 *     short-cut convenience in handling the residue
 */
static void
copyBackStraight(I_32 *instrArray, I_32 *currIdx, I_32 resSize, I_32 paramOffset)
{

}

/**
 * @brief Generate instruction loop to copy back result
 * @param instrArray[in/out] A pointer to the thunk memory
 * @param currIdx[in/out]    A pointer to the current instruction index
 * @param resSize[in] The size in byte to copy, guarantee to be more than 64
 * @param paramOffset[in] Offset to the parameter area
 * @return none
 *
 * Details:
 *   a static routine to generate instruction loop to copy back the upcall result
 *   fixed registers are used
 *     load the hidden parameter into register number 4
 *     set up the loop:  2 instructions
 *     loop itself: 11 instructions (4 load, 4 store, 2 addi, and branch)
 *     load the residue in sequence in no more than 4 registers starting from register 5
 *     store these registers back into the memory designated by the hidden parameter
 *
 *     short-cut convenience in handling the residue
 */
static void
copyBackLoop(I_32 *instrArray, I_32 *currIdx, I_32 resSize, I_32 paramOffset)
{
}

/**
 * @brief Generate the appropriate thunk/adaptor for a given J9UpcallMetaData
 * @param metaData[in/out] A pointer to the given J9UpcallMetaData
 * @return the address for this future upcall function handle, either the thunk or the thunk-descriptor
 *
 * Details:
 *   A thunk or adaptor is mainly composed of 4 parts of instructions to be counted separately:
 *   1) the eventual call to the upcallCommonDispatcher (fixed number of instructions)
 *   2) if needed, instructions to build a stack frame
 *   3) pushing in-register arguments back to the stack, either in newly-built frame or caller frame
 *   4) if needed, instructions to distribute  the java result back to the native side appropriately
 *
 *     1) and 3) are mandatory, while 2) and 4) depend on the particular signature under consideration.
 *     2) is most likely needed, since the caller frame might not contain the parameter area in most cases;
 *     4) implies needing 2), since this adaptor expects a return from java side before returning
 *        to the native caller.
 */
void *
createUpcallThunk(J9UpcallMetaData *metaData)
{
	J9JavaVM *vm = metaData->vm;
	J9InternalVMFunctions *vmFuncs = vm->internalVMFunctions;
	J9UpcallSigType *sigArray = metaData->nativeFuncSignature->sigArray;
	I_32 lastSigIdx = metaData->nativeFuncSignature->numSigs - 1; // The index of the return type in the signature array

	Assert_VM_true(lastSigIdx >= 0);
	copyBackLoop((I_32 *)metaData, (I_32 *)metaData, 5, 5);
	copyBackStraight((I_32 *)metaData, (I_32 *)metaData, 5, 5);

	I_32 numFourByteInstructions = 0;
	I_32 numSixByteInstructions = 0;

	// Before allocating the thunk memory we need to:
	// - Calculate the amount of instructions (instructionCount) needed to generate a call to the common dispatcher for the Java method.
	// - Calculate the amount of instructions (instructionCount) we will need to transfer parameters from registers + C stack to our stack
	// - Calculate the amount of additional space (if any) we will need to store the parameters + instructions to store the parameter
	// - Calculate the amount of instructions we will need to unpack Java's MemorySegment object into a struct before returning to the C native code.
	// - Calculate the amount of instructions we will need to build a stack frame (if it's needed based on the above additional space requirements).
	//
	// - Once we know how much space is needed, we can allocate the thunk and generate the necessary instructions to save parameters. Then we can call
	// the dispatcher. If the return type is a struct, then we'll need to also generate instructions to unpack the Java return value into the format required
	// by the C side.

	// Test the return type
	switch (sigArray[lastSigIdx].type) {
		case J9_FFI_UPCALL_SIG_TYPE_INT32:
			metaData->upCallCommonDispatcher = (void *)vmFuncs->native2InterpJavaUpcall1;
			break;
		default:
			printf("DCDCDCDC -- unsupported Return type\n");
			Assert_VM_unreachable();
	}

	I_32 numStackSlots = 0;
	I_32 gprArgCount = 0;

	// Loop through the arguments and count how many instructions you'll need to copy data from C stack to java stack, and how many
	// bytes of space you'll need to allocate in thunk and new stack frame to store the instructions + parameter info.
	for (I_32 i = 0; i < lastSigIdx; i++) {
		// Testing this argument
		switch (sigArray[i].type) {
			case J9_FFI_UPCALL_SIG_TYPE_INT32:
			{
				// That's one extra parameter, so one more space needed on the stack.
				numStackSlots++;
				gprArgCount++;
				if (gprArgCount <= 5)
				{
					numFourByteInstructions++;
				}
				break;
			}
			default:
			{
				Assert_VM_unreachable();
			}
		}
	}

	I_32 frameSize = 0;
	I_32 offsetToParamArea = 0;
	I_32 roundedCodeSize = 0;
	I_8 *thunkMem = NULL;

	// if we need stack space to store arguments or return values,
	// then figure out the size of the new frame that we need.
	if (numStackSlots > 0)
	{
		frameSize = 160 + ((numStackSlots +1) / 2) * 16;
		offsetToParamArea = 160;
	}

	// 8 byte align the thunk memory.
	// extra instructions Size
	I_32 extraInstructionsSize = numStackSlots <= 0 ? 46 : 64;
	roundedCodeSize = (((numFourByteInstructions * 4 + numSixByteInstructions * 6 + extraInstructionsSize) / 8) + 1) * 8;
	printf("DCDCDCDC --> numFourByteInstructions: %d, numSixByteInstructions: %d,  roundedCodeSize: %d\n", numFourByteInstructions, numSixByteInstructions, roundedCodeSize);
	metaData->thunkSize = roundedCodeSize + 8;
	thunkMem = (I_8 *)vmFuncs->allocateUpcallThunkMemory(metaData);
	if (NULL == thunkMem) {
		return NULL;
	}
	metaData->thunkAddress = (void *)thunkMem;

	//Generate the instruction sequence to copy data from C side to Java side.
	I_32 stackSlotIndex = 0;
	I_32 gprIndex = 2;
	I_32 C_SP_RegisterNum = 15;
	// First extend the stack frame
	if (numStackSlots > 0)
	{
	// save the original return address into memory
	// 6 bytes
	thunkMem += generateStoreInstruction(thunkMem, 14, 0, C_SP_RegisterNum, 112, true);
		// Store the current C stack pointer at the first byte of the new stack pointer.
		//
		thunkMem += generateStoreInstruction(thunkMem, C_SP_RegisterNum, 0, C_SP_RegisterNum, -frameSize, true);
		// Then update R15 with the new c stack pointer.
		// Now do an add to update the stack pointer
		thunkMem += generateAddRegisterImmediateInstruction(thunkMem, C_SP_RegisterNum, -frameSize, true);
	}
	
	// Now that the stack has expanded and the new stack pointer is in, we loop through the arguments and generate the actual instructions
	for (I_32 i = 0; i < lastSigIdx; i++) {
		switch(sigArray[i].type) {
			case J9_FFI_UPCALL_SIG_TYPE_INT32:
				// Only store the value onto the stack if it's not already in the caller's frame.
				if (gprIndex <= 6){
					thunkMem += generateStoreInstruction(thunkMem, gprIndex, 0, C_SP_RegisterNum, offsetToParamArea + (stackSlotIndex * 8), true); 
				}	
				gprIndex += 1;
				stackSlotIndex += 1;
				break;
			default:
                Assert_VM_unreachable();

		}
	}

	// Now jump to the common dispatcher helper routine.
	// to load the metadat structure, jump to previous stack frame, then from there jump another 160 bytes to find the first
	// parameter, and load that (use 64-bit loads in both).
	// For the second pointer, load C_SP + 160 to point to where the current list of parameters start.
	thunkMem += generateInsertImmediateInstruction(thunkMem, 2, (reinterpret_cast<uint64_t>(metaData) >> 32), true);
	thunkMem += generateInsertImmediateInstruction(thunkMem, 2, reinterpret_cast<uint64_t>(metaData), false);
	// Load the third register with argPointer (stackFrame + 160)
	thunkMem += generateLAYInstruction(thunkMem, 3, 0, C_SP_RegisterNum, offsetToParamArea);
	thunkMem += generateInsertImmediateInstruction(thunkMem, 1, reinterpret_cast<uint64_t>(metaData->upCallCommonDispatcher) >> 32, true);
	thunkMem += generateInsertImmediateInstruction(thunkMem, 1, reinterpret_cast<uint64_t>(metaData->upCallCommonDispatcher), false);
	// Now generate the branch
	thunkMem += generateBASR(thunkMem, 14, 1);

	// Upon returning restore the stack pointer.
	thunkMem += generateLAYInstruction(thunkMem, C_SP_RegisterNum, 0, C_SP_RegisterNum, frameSize);
	// Now load R14 with the return address.
	thunkMem += generateLoadInstruction(thunkMem, 14, 0, C_SP_RegisterNum, 112, true);


	// Then branch back
	thunkMem += generateBASR(thunkMem, 14, 14);

	// Finish up before returning
    vmFuncs->doneUpcallThunkGeneration(metaData, (void *)(metaData->thunkAddress));
	return (void *)(metaData->thunkAddress);
}

/**
 * @brief Calculate the requested argument in-stack memory address to return
 * @param nativeSig[in] A pointer to the J9UpcallNativeSignature
 * @param argListPtr[in] A pointer to the argument list prepared by the thunk
 * @param argIdx[in] The requested argument index
 * @return address in argument list for the requested argument
 *
 * Details:
 *   A quick walk-through of the argument list ahead of the requested one
 *   Calculating its address based on argListPtr
 */
void *
getArgPointer(J9UpcallNativeSignature *nativeSig, void *argListPtr, I_32 argIdx)
{
	J9UpcallSigType *sigArray = nativeSig->sigArray;
        I_32 lastSigIdx = nativeSig->numSigs - 1; // The index for the return type in the signature array
        I_32 stackSlotCount = 0;

	Assert_VM_true((argIdx >= 0) && (argIdx < lastSigIdx));

	// Test the return type
	switch (sigArray[lastSigIdx].type) {
		// If the return type is a struct, then the first entry in the stack is an address to a buffer
		// where the struct will be stored when we return to native caller. So we must skip over that
		// "hiddenParameter" in this function.
		case J9_FFI_UPCALL_SIG_TYPE_STRUCT:
			stackSlotCount += 1;
			break;
		default:
			break;
	}

	// Loop through the arguments until you reach the argument you want to return the pointer to.
	for (I_32 i = 0; i < argIdx; i++) {
		// Test the current argument
		switch(sigArray[i].type & J9_FFI_UPCALL_SIG_TYPE_MASK) {
			// These types each take up one stack slot in our thunk memory (8 byte aligned), so
			// add one to the stackSlotCounter
			case J9_FFI_UPCALL_SIG_TYPE_CHAR:    /* Fall through */
                        case J9_FFI_UPCALL_SIG_TYPE_SHORT:   /* Fall through */
                        case J9_FFI_UPCALL_SIG_TYPE_INT32:   /* Fall through */
                        case J9_FFI_UPCALL_SIG_TYPE_POINTER: /* Fall through */
                        case J9_FFI_UPCALL_SIG_TYPE_INT64:   /* Fall through */
                        case J9_FFI_UPCALL_SIG_TYPE_FLOAT:   /* Fall through */
                        case J9_FFI_UPCALL_SIG_TYPE_DOUBLE:
				stackSlotCount += 1;
				break;
			case J9_FFI_UPCALL_SIG_TYPE_STRUCT:
				// If the argument is a struct, then we store it in thunk memory sequentially (8 byte aligned).
				// So figure out size of struct and round it up to the nearest multiple of 8.
				//stackSlotCount += ROUND_UP_SLOT(sigArray[i].sizeInByte);
				break;
			default:
				Assert_VM_unreachable();
		}
	}
	printf("\n getArgPointer --- OK: argIdx = %d, argListPtr = %p, stackSlotCount = %d\n", argIdx, argListPtr, (int)stackSlotCount);
	return static_cast<void *>((static_cast<char *>(argListPtr) + (stackSlotCount * 8)));

}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

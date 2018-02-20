/*******************************************************************************
 * Copyright (c) 2018, 2018 IBM Corp. and others
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

package StackMap;
import java.util.Arrays;

public class StackMapMetaDataEntryTest1 {
	public static void CheckStackMapMetaDataEntry1(String args[]) {
		if ((1 != args.length) && (1 != args[0].length())) {
			return;
		}
		
		switch (args[0].charAt(0)) {
		case 'B':
			byte arr1[] = new byte[] {0xA, 0xB, 0xC, 0xD, 0xE};
			byte arr2[] = new byte[arr1.length];
			for (int index = 0; index < arr1.length; index++) {
				arr2[index] = arr1[index];
			}
			System.out.println(Arrays.toString(arr2));
			break;
		case 'Z':
			boolean arr3[] = new boolean[] {true, false, true, false, true};
			boolean arr4[] = new boolean[arr3.length];
			for (int index = 0; index < arr3.length; index++) {
				arr4[index] = arr3[index];
			}
			System.out.println(Arrays.toString(arr4));
			break;
		default:
			int arr5[] = new int[] {1, 2, 3, 4, 5};
			int arr6[] = new int[arr5.length];
			for (int index = 0; index < arr5.length; index++) {
				arr6[index] = arr5[index];
			}
			System.out.println(Arrays.toString(arr6));
			break;
		}
	}
	
	public static void main(String args[]) {
		CheckStackMapMetaDataEntry1(args);
	}
}

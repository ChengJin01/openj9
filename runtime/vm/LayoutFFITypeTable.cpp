/*******************************************************************************
 * Copyright IBM Corp. and others 2024
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
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 *******************************************************************************/

#include "j9.h"
#include "j9protos.h"

extern "C" {

#if JAVA_SPEC_VERSION >= 16

/**
 * @brief Return the hash value for an entry in the layout string table.
 *
 * @param entry the layout string entry
 * @param userData the data specified as userData when the hashtable was created,
 *                 in our case pointer to the port library
 * @returns the hash value
 */
static UDATA
layouStrFFITypeHashFn(void *entry, void *userData)
{
	J9LayoutStrFFITypeEntry *layoutStrEntry = (J9LayoutStrFFITypeEntry *)entry;

	return computeHashForUTF8(layoutStrEntry->layoutStr, layoutStrEntry->layoutStrLength);
}

/**
 * @brief Compare two layout string entries for equality.
 *
 * @param leftEntry the first entry to compare
 * @param rightEntry the second entry to compare
 * @param userData the data specified as userData when the hashtable was created,
 *                 in our case pointer to the port library
 * @returns 1 if the entries match, otherwise return 0
 */
static UDATA
layouStrFFITypeHashEqualFn(void *leftEntry, void *rightEntry, void *userData)
{
	J9LayoutStrFFITypeEntry *leftLayoutStrEntry = (J9LayoutStrFFITypeEntry *)leftEntry;
	J9LayoutStrFFITypeEntry *rightLayoutStrEntry = (J9LayoutStrFFITypeEntry *)rightEntry;

	return J9UTF8_DATA_EQUALS(leftLayoutStrEntry->layoutStr, leftLayoutStrEntry->layoutStrLength,
			rightLayoutStrEntry->layoutStr, rightLayoutStrEntry->layoutStrLength);
}

/**
 * @brief Delete an entry from the layout string hashtable.
 *
 * @param entry the entry to be deleted
 * @param userData the data specified as userData when the hashtable was created,
 *                 in our case pointer to the port library
 */
static UDATA
freeLayouStrFFIDoFn(void *entry, void *userData)
{
	PORT_ACCESS_FROM_PORT((J9PortLibrary *)userData);
	J9LayoutStrFFITypeEntry *layoutStrEntry = (J9LayoutStrFFITypeEntry *)entry;
	j9mem_free_memory(layoutStrEntry->layoutStr);
	j9mem_free_memory(layoutStrEntry);
	return TRUE;
}

J9HashTable *
createLayouStrFFITypeTable(J9JavaVM *vm)
{
	return hashTableNew(OMRPORT_FROM_J9PORT(vm->portLibrary), "Layout string table", 0,
			sizeof(J9LayoutStrFFITypeEntry), 0, 0, J9MEM_CATEGORY_VM_FFI, layouStrFFITypeHashFn, layouStrFFITypeHashEqualFn, NULL, NULL);
}

J9LayoutStrFFITypeEntry *
findLayouStrFFIType(J9HashTable *hashtable, J9LayoutStrFFITypeEntry *entry)
{
	return (J9LayoutStrFFITypeEntry *)hashTableFind(hashtable, entry);
}

J9LayoutStrFFITypeEntry *
addLayouStrFFIType(J9HashTable *hashtable, J9LayoutStrFFITypeEntry *entry)
{
	return (J9LayoutStrFFITypeEntry *)hashTableAdd(hashtable, entry);
}

void
cleanUpLayouStrFFITypeTable(J9JavaVM *vm)
{
	if (NULL != vm->layoutStrFFITypeTable) {
		hashTableForEachDo(vm->layoutStrFFITypeTable, (J9HashTableDoFn)freeLayouStrFFIDoFn, NULL);
		hashTableFree(vm->layoutStrFFITypeTable);
		vm->layoutStrFFITypeTable = NULL;
	}
}

#endif /* JAVA_SPEC_VERSION >= 16 */

} /* extern "C" */

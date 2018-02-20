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

#include "StackMapManagerImpl.hpp"
#include "ut_j9shr.h"
#include "j9shrnls.h"
#include "j9consts.h"
#include <string.h>

SH_StackMapManagerImpl::SH_StackMapManagerImpl()
{
}

SH_StackMapManagerImpl::~SH_StackMapManagerImpl()
{
}

/**
 * Return the number of bytes required to construct this SH_StackMapManager
 *
 * @return size in bytes
 */
UDATA
SH_StackMapManagerImpl::getRequiredConstrBytes(void)
{
	UDATA reqBytes = 0;

	reqBytes += sizeof(SH_StackMapManagerImpl);
	return reqBytes;
}

/**
 * Create a new instance of SH_StackMapManagerImpl
 *
 * @param [in] vm A Java VM
 * @param [in] cache_ The SH_SharedCache that will use this StackMapManager
 * @param [in] memForConstructor Memory in which to build the instance
 *
 * @return new SH_StackMapManager
 */	
SH_StackMapManagerImpl*
SH_StackMapManagerImpl::newInstance(J9JavaVM* vm, SH_SharedCache* cache_, SH_StackMapManagerImpl* memForConstructor)
{
	SH_StackMapManagerImpl* newSMM = (SH_StackMapManagerImpl*)memForConstructor;

	Trc_SHR_SMMI_newInstance_Entry(vm, cache_);

	new(newSMM) SH_StackMapManagerImpl();
	newSMM->initialize(vm, cache_, ((BlockPtr)memForConstructor + sizeof(SH_StackMapManagerImpl)));

	Trc_SHR_SMMI_newInstance_Exit(newSMM);

	return newSMM;
}

/* Initialize the SH_StackMapManager - should be called before startup */
void
SH_StackMapManagerImpl::initialize(J9JavaVM* vm, SH_SharedCache* cache_, BlockPtr memForConstructor)
{
	Trc_SHR_SMMI_initialize_Entry();

	_cache = cache_;
	_portlib = vm->portLibrary;
	_htMutex = NULL;
	_htMutexName = "smTableMutex";
	_dataTypesRepresented[0] = TYPE_STACKMAP;
	_dataTypesRepresented[1] = 0;

	_rrmHashTableName = J9_GET_CALLSITE();
	_rrmLookupFnName = "smTableLookup";
	_rrmAddFnName = "smTableAdd";
	_rrmRemoveFnName = "smTableRemove";
	
	_accessPermitted = true;	/* No mechanism to prevent access */

	notifyManagerInitialized(_cache->managers(), "TYPE_STACKMAP");

	Trc_SHR_SMMI_initialize_Exit();
}

UDATA
SH_StackMapManagerImpl::getKeyForItem(const ShcItem* cacheItem)
{
	return (UDATA)SMWROMMETHOD((StackMapWrapper*)ITEMDATA(cacheItem)) + TYPE_STACKMAP;
}

U_32
SH_StackMapManagerImpl::getHashTableEntriesFromCacheSize(UDATA cacheSizeBytes)
{
	return (U_32)((cacheSizeBytes/5000) + 100);
}

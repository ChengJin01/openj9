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
#if !defined(STACKMAP_MANAGER_HPP_INCLUDED)
#define STACKMAP_MANAGER_HPP_INCLUDED

#include "ROMClassResourceManager.hpp"

class SH_StackMapManager : public SH_ROMClassResourceManager
{
public:

class SH_StackMapResourceDescriptor : public SH_ResourceDescriptor
{
	public:
		typedef char* BlockPtr;

		SH_StackMapResourceDescriptor() :
			_dataStart(0), _dataSize(0)
		{
		}

		SH_StackMapResourceDescriptor(const U_8* dataStart, U_32 dataSize) :
			_dataStart(dataStart), 
			_dataSize(dataSize)
		{
		}

		~SH_StackMapResourceDescriptor()
		{
		}
		
		virtual U_32 getResourceLength() 
		{
			return _dataSize;
		}

		virtual U_32 getWrapperLength() 
		{
			return sizeof(StackMapWrapper);
		}

		virtual U_16 getResourceType() 
		{
			return TYPE_STACKMAP;
		}

		virtual U_32 getAlign() 
		{
			return SHC_WORDALIGN;
		}

		virtual const ShcItem* wrapperToItem(const void* wrapper) 
		{
			return (const ShcItem*)SMWITEM(wrapper);
		}

		virtual UDATA resourceLengthFromWrapper(const void* wrapper) 
		{
			return ((StackMapWrapper*)wrapper)->dataLength;
		}

		virtual const void* unWrap(const void* wrapper) 
		{
			return (const void*)SMWDATA(wrapper);
		}

		virtual void writeDataToCache(const ShcItem* newCacheItem, const void* resourceAddress) 
		{
			StackMapWrapper* smwInCache = (StackMapWrapper*)ITEMDATA(newCacheItem);

			smwInCache->dataLength = _dataSize;
			smwInCache->romMethodOffset = (J9SRP)((BlockPtr)resourceAddress - (BlockPtr)(smwInCache));
			memcpy(SMWDATA(smwInCache), (void *)_dataStart, _dataSize);
		}

		virtual UDATA generateKey(const void *resourceAddress)
		{
			return (UDATA)(resourceAddress) + TYPE_STACKMAP;
		}

private:
		/* Placement operator new (<new> is not included) */
		void* operator new(size_t size, void* memoryPtr) 
		{
			return memoryPtr;
		}

		const U_8* _dataStart;
		const U_32 _dataSize;
	};

};

#endif

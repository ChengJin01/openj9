/* -----------------------------------------------------------------------
   internal64.h - Copyright (c) 2021  Madhavan T. Venkataraman <75220914+madvenka786@users.noreply.github.com>
                  Copyright (c) 2014-2015  Richard Henderson <rth@twiddle.net>

   x86-64 Foreign Function Interface

   Permission is hereby granted, free of charge, to any person obtaining
   a copy of this software and associated documentation files (the
   ``Software''), to deal in the Software without restriction, including
   without limitation the rights to use, copy, modify, merge, publish,
   distribute, sublicense, and/or sell copies of the Software, and to
   permit persons to whom the Software is furnished to do so, subject to
   the following conditions:

   The above copyright notice and this permission notice shall be included
   in all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND,
   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
   NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
   HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
   DEALINGS IN THE SOFTWARE.
   ----------------------------------------------------------------------- */

/*
 * ===========================================================================
 * (c) Copyright IBM Corp. 2021, 2021 All Rights Reserved
 * ===========================================================================
 */

#define UNIX64_RET_VOID		0
#define UNIX64_RET_UINT8	1
#define UNIX64_RET_UINT16	2
#define UNIX64_RET_UINT32	3
#define UNIX64_RET_SINT8	4
#define UNIX64_RET_SINT16	5
#define UNIX64_RET_SINT32	6
#define UNIX64_RET_INT64	7
#define UNIX64_RET_XMM32	8
#define UNIX64_RET_XMM64	9
#define UNIX64_RET_X87		10
#define UNIX64_RET_X87_2	11
#define UNIX64_RET_ST_XMM0_RAX	12
#define UNIX64_RET_ST_RAX_XMM0	13
#define UNIX64_RET_ST_XMM0_XMM1	14
#define UNIX64_RET_ST_RAX_RDX	15

#define UNIX64_RET_LAST		15

#define UNIX64_FLAG_RET_IN_MEM	(1 << 10)
#define UNIX64_FLAG_XMM_ARGS	(1 << 11)
#define UNIX64_SIZE_SHIFT	12

#if defined(FFI_EXEC_STATIC_TRAMP)
/*
 * For the trampoline code table mapping, a mapping size of 4K (base page size)
 * is chosen.
 */
#define UNIX64_TRAMP_MAP_SHIFT	12
#define UNIX64_TRAMP_MAP_SIZE	(1 << UNIX64_TRAMP_MAP_SHIFT)
#ifdef ENDBR_PRESENT
#define UNIX64_TRAMP_SIZE	40
#else
#define UNIX64_TRAMP_SIZE	32
#endif
#endif

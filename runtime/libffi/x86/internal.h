/* -----------------------------------------------------------------------
   internal.h - Copyright (c) 2021  Madhavan T. Venkataraman <75220914+madvenka786@users.noreply.github.com>
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

#define X86_RET_FLOAT		0
#define X86_RET_DOUBLE		1
#define X86_RET_LDOUBLE		2
#define X86_RET_SINT8		3
#define X86_RET_SINT16		4
#define X86_RET_UINT8		5
#define X86_RET_UINT16		6
#define X86_RET_INT64		7
#define X86_RET_INT32		8
#define X86_RET_VOID		9
#define X86_RET_STRUCTPOP	10
#define X86_RET_STRUCTARG       11
#define X86_RET_STRUCT_1B	12
#define X86_RET_STRUCT_2B	13
#define X86_RET_UNUSED14	14
#define X86_RET_UNUSED15	15

#define X86_RET_TYPE_MASK	15
#define X86_RET_POP_SHIFT	4

#define R_EAX	0
#define R_EDX	1
#define R_ECX	2

#ifdef __PCC__
# define HAVE_FASTCALL 0
#else
# define HAVE_FASTCALL 1
#endif

#if defined(FFI_EXEC_STATIC_TRAMP)
/*
 * For the trampoline code table mapping, a mapping size of 4K (base page size)
 * is chosen.
 */
#define X86_TRAMP_MAP_SHIFT	12
#define X86_TRAMP_MAP_SIZE	(1 << X86_TRAMP_MAP_SHIFT)
#ifdef ENDBR_PRESENT
#define X86_TRAMP_SIZE		44
#else
#define X86_TRAMP_SIZE		40
#endif
#endif

/*
 * Copyright 2013 OldCurmudgeon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oldcurmudgeon.galois.math.huge;

import com.oldcurmudgeon.galois.math.sparse.SparseIterator;
import com.oldcurmudgeon.galois.math.sparse.Sparse;
import com.oldcurmudgeon.galois.math.sparse.SparseIterable;
import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.math.BigInteger;

/**
 * Defines a stream of bits to perform maths over.
 *
 * It should be possible to step through two streams of bits
 * at once and do the maths on them.
 *
 * Underneath, it should be possible to ask the stream to skip
 * uninteresting sequences of bits such as all zeros.
 *
 * Going forward I hope to perform the actual math using lambdas
 * and closures but for now we will merely iterate.
 *
 * T is the type of each chunk.
 * I is the type of the index.
 *
 * Currently only using BigInteger - later even more generic perhaps.
 * 
 * @author OldCurmudgeon
 */
public abstract class Bits<T extends Sparse<BigInteger, BigInteger>> implements SparseIterable<T, BigInteger> {
  // Bits in a byte.
  public static final BigInteger EIGHT = BigInteger.valueOf(8);

  @Override
  public abstract SparseIterator<T, BigInteger> iterator();

  public abstract SparseIterator<T, BigInteger> reverseIterator();
  // Length in bits.

  public abstract BigInteger length();

  protected abstract class BitsIterator implements SparseIterator<T, BigInteger> {
    // The next to return - populate in getNext please.
    T next = null;

    // hasNext is idempotent - we can call it as often as we like.
    @Override
    public boolean hasNext() {
      if (next == null) {
        getNext();
      }
      return next != null;
    }

    // Obtain the next Big.
    protected abstract void getNext();

    @Override
    public T next() {
      // Standard pattern for next.
      if (hasNext()) {
        T n = next;
        next = null;
        return n;
      } else {
        return null;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }

    // Return the current index.
    @Override
    public BigInteger index() {
      // Note use of next.index - not next().index.
      return hasNext() ? next.index() : null;
    }

    // Return its length.
    @Override
    public BigInteger length() {
      // Note use of next.index - not next().index.
      return hasNext() ? next.length() : null;
    }

  }

  @Override
  public String toString() {
    return Separator.separate("{", ",", "}", iterator()) + "=" + toString(16);
  }

  public String toString(int base) {
    StringBuilder sb = new StringBuilder();
    // NB - This is the bit index of the top bit.
    BigInteger index = length();
    // Start from the end and work backwards.
    for (SparseIterator<T, BigInteger> i = reverseIterator(); i.hasNext();) {
      // The value.
      T next = i.next();
      // How long is it?
      BigInteger itsLength = next.length();
      // Where is its top bit?
      BigInteger itsIndex = next.index().add(itsLength);
      // Roll out zeros to get us to that bit.
      index = padZeros(sb, base, index, itsIndex);
      // Emit the value.
      sb.append(next.value().toString(base));
      // Step down that many bits.
      index = index.subtract(itsLength);
    }
    padZeros(sb, base, index, BigInteger.ZERO);
    return sb.toString();
  }

  // Generate the right number of zeros depending on the base.
  private BigInteger padZeros(StringBuilder sb, int base, BigInteger index, BigInteger itsIndex) {
    // That may bits.
    BigInteger zeroBits = index.subtract(itsIndex);
    if ( !zeroBits.equals(BigInteger.ZERO) ) {
      // How many bits does a zero in this base represent?
      BigInteger bitsPerZero = BigInteger.valueOf(bitCount(base - 1));
      // Do the right number.
      while (index.subtract(bitsPerZero).compareTo(BigInteger.ZERO) > 0) {
        // Right number of zeros in that base.
        sb.append(BigInteger.ZERO.toString(base));
        // Step that far.
        index = index.subtract(bitsPerZero);
      }
    }
    // Tell them where we finished.
    return index;
  }
  
  private int bitCount ( int v ) {
    // Brian Kernighan's way
    int c;
    for ( c = 0; v != 0; c++ ) {
      v &= v - 1;
    }
    return c;
  }
  
  // Actual Bits processes that do things.
  // ToDo: Use ForkJoinPools for some of thois stuff.

  public static Bits xor(Bits a, Bits b) {
    // Ultimately use a lambda but for now I will use a loop and ops.
    return apply(a, b, Op.xor);
  }

  // Possible operations to perform on the bits.
  enum Op {
    xor {
      @Override
      public BigInteger op(BigInteger a, BigInteger b) {
        return a.xor(b);
      }

    },
    and {
      @Override
      public BigInteger op(BigInteger a, BigInteger b) {
        return a.and(b);
      }

    },
    or {
      @Override
      public BigInteger op(BigInteger a, BigInteger b) {
        return a.or(b);
      }

    };
    // Perform the op.

    abstract BigInteger op(BigInteger a, BigInteger b);

  }

  // Where to pull index, length, value and next from - a or b.
  private enum Next {
    A {
      @Override
      <T extends Sparse<BigInteger, BigInteger>> BigInteger index(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b) {
        return a.index();
      }

      @Override
      <T extends Sparse<BigInteger, BigInteger>> BigInteger length(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b) {
        return a.length();
      }

      @Override
      <T extends Sparse<BigInteger, BigInteger>> BigInteger value(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b) {
        return a.next().value();
      }

      @Override
      Next other() {
        return Next.B;
      }

    },
    B {
      @Override
      <T extends Sparse<BigInteger, BigInteger>> BigInteger index(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b) {
        return b.index();
      }

      @Override
      <T extends Sparse<BigInteger, BigInteger>> BigInteger length(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b) {
        return b.length();
      }

      @Override
      <T extends Sparse<BigInteger, BigInteger>> BigInteger value(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b) {
        return b.next().value();
      }

      @Override
      Next other() {
        return Next.A;
      }

    };

    // The index of the next value.
    abstract <T extends Sparse<BigInteger, BigInteger>> BigInteger index(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b);
    // Its length.

    abstract <T extends Sparse<BigInteger, BigInteger>> BigInteger length(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b);
    // The next value.

    abstract <T extends Sparse<BigInteger, BigInteger>> BigInteger value(SparseIterator<T, BigInteger> a, SparseIterator<T, BigInteger> b);

    // The other one.
    abstract Next other();

    // Determine next.
    static Next next(SparseIterator<?, BigInteger> a, SparseIterator<?, BigInteger> b) {
      // If they both exist compare them - if one exists return it - otherwise return null.
      BigInteger ai = a.index();
      BigInteger bi = b.index();
      if (ai == null && bi == null) {
        // Both null.
        return null;
      }
      if (ai == null) {
        // A null - return B.
        return B;
      }
      if (bi == null) {
        // B null - return A.
        return A;
      }
      // Neither null - return lowest index with priority A.
      return a.index().compareTo(b.index()) <= 0 ? A : B;
    }

  }

  // Applies the op to the bits.
  public static Bits apply(Bits<Big> a, Bits<Big> b, Op op) {
    // Accumulator.
    HugeBits applied = new HugeBits();
    // Start both iterators.
    SparseIterator<Big, BigInteger> ia = a.iterator();
    SparseIterator<Big, BigInteger> ib = b.iterator();
    // Which one is next - using the enum above to work itout.
    for (Next next = Next.next(ia, ib); next != null; next = Next.next(ia, ib)) {
      // Grab the index, length and value.
      BigInteger index = next.index(ia, ib);
      BigInteger length = next.length(ia, ib);
      BigInteger value = next.value(ia, ib);
      Next other = next.other();
      // Handle overlaps
      if (overlaps(index, length, other.index(ia, ib), other.length(ia, ib))) {
        // Work out the shift.
        //ToDo: What do we do if there is overlap between the values?
        // Perform the op.
        applied.add(new Big(index, op.op(value, other.value(ia, ib))));
      } else {
        // No counterparSt - add it directly.
        applied.add(new Big(index, value));
      }
    }
    return applied;
  }

  private static boolean overlaps(BigInteger aIndex, BigInteger aLength, BigInteger bIndex, BigInteger bLength) {
    // Any missing?
    if (aIndex == null || aLength == null || bIndex == null || bLength == null) {
      return false;
    }
    // Wholly below?
    if (aIndex.add(aLength).compareTo(bIndex) <= 0) {
      return false;
    }
    // Wholly above?
    if (bIndex.add(bLength).compareTo(aIndex) <= 0) {
      return false;
    }
    // There is some intersection.
    return true;
  }

  public static void main(String[] args) {
    // 2^10 = 4 * 2^8
    Big x = new Big(BigInteger.TEN, BigInteger.ONE);
    System.out.println("x = " + x);
    // 2^10 * 2^6 = 2^16
    Big y = new Big(BigInteger.TEN, BigInteger.valueOf(64));
    System.out.println("y = " + y);
    HugeBits z = new HugeBits(new Big(BigInteger.ONE), x);
    System.out.println("z = " + z);
    HugeBits a = new HugeBits(new Big(BigInteger.ZERO, BigInteger.valueOf(0xFEDCBA987654321L)));
    System.out.println("a = " + a);
    HugeBits b = new HugeBits(new Big(BigInteger.ZERO, BigInteger.valueOf(0x123456789ABCDEFL)));
    System.out.println("b = " + b);
    HugeBits c = new HugeBits(new Big(BigInteger.ONE), x);
    System.out.println("c = " + c);
    HugeBits d = new HugeBits(
            new Big(BigInteger.ZERO, BigInteger.ONE),
            new Big(BigInteger.ONE, BigInteger.ONE));
    System.out.println("d = " + d);
    System.out.println("a xor b = " + apply(a, b, Op.xor));
    System.out.println("a xor c = " + apply(a, c, Op.xor));
    System.out.println("b xor c = " + apply(b, c, Op.xor));
    System.out.println("d xor d = " + apply(d, d, Op.xor));
    HugeBits e = new HugeBits(
            new Big(BigInteger.ZERO, BigInteger.ONE),
            new Big(BigInteger.TEN, BigInteger.ONE));
    HugeBits f = new HugeBits(
            new Big(BigInteger.ZERO, BigInteger.ONE),
            new Big(BigInteger.valueOf(100), BigInteger.ONE));
    System.out.println("e(" + e + ") xor f(" + f + ") = " + apply(e, f, Op.xor));
    HugeBits g = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0})));
    System.out.println("g = " + g);
    HugeBits h = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0})),
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0})));
    System.out.println("g(" + g + ") xor h(" + h + ") = " + apply(g, h, Op.xor));
    HugeBits i = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{1, 0, 1, 1, 0, 0, 0, 1})));
    HugeBits j = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{1, 0, 0, 1, 0, 0, 1, 1})));
    System.out.println("i(" + i + ") xor j(" + j + ") = " + apply(i, j, Op.xor));

  }

}

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
 * @author OldCurmudgeon
 */
public abstract class Bits<T extends Sparse<BigInteger, BigInteger>> implements SparseIterable<T, BigInteger> {
  @Override
  public abstract SparseIterator<T, BigInteger> iterator();

  protected abstract class BitsIterator implements SparseIterator<T, BigInteger> {
    // The next to return - populate in getNext please.
    T next = null;

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
      return hasNext() ? next.index() : null;
    }

    // Return its length.
    @Override
    public BigInteger length() {
      return hasNext() ? next.length() : null;
    }
  }

  @Override
  public String toString() {
    return Separator.separate("{", ",", "}", iterator());
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
      public BigInteger op(BigInteger a, BigInteger b) {
        return a.xor(b);
      }
    },
    and {
      public BigInteger op(BigInteger a, BigInteger b) {
        return a.and(b);
      }
    },
    or {
      public BigInteger op(BigInteger a, BigInteger b) {
        return a.or(b);
      }
    };
    // Perform the op.

    abstract BigInteger op(BigInteger a, BigInteger b);
  }

  // Where to pull next from - a or b.
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
  private static Bits apply(Bits<Big> a, Bits<Big> b, Op op) {
    HugeBits applied = new HugeBits();
    SparseIterator<Big, BigInteger> ia = a.iterator();
    SparseIterator<Big, BigInteger> ib = b.iterator();
    // Which one is next.
    for (Next next = Next.next(ia, ib); next != null; next = Next.next(ia, ib)) {
      BigInteger index = next.index(ia, ib);
      BigInteger length = next.length(ia, ib);
      BigInteger value = next.value(ia, ib);
      Next other = next.other();
      if (overlaps(index, length, other.index(ia, ib), other.length(ia, ib))) {
        // Work out the shift.
        //xxx
        // Perform the op.
        applied.add(new Big(index, op.op(value, other.value(ia, ib))));
      } else {
        // No counterpart - add it directly.
        applied.add(new Big(index, value));
      }
    }
    return applied;
  }

  private static boolean overlaps(BigInteger aIndex, BigInteger aLength, BigInteger bIndex, BigInteger bLength) {
    // Any missing?
    if ( aIndex == null || aLength == null || bIndex == null || bLength == null ) {
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
    System.out.println("e("+e+") xor f("+f+") = " + apply(e, f, Op.xor));
    HugeBits g = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[] {0,0,1,0,0,0,1,0,0,1,0,0})));
    System.out.println("g = " + g);
    HugeBits h = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[] {0,0,1,0,0,0,0,0,0,1,0,0})),
            new Big(BigInteger.ZERO, new BigInteger(new byte[] {0,0,1,0,0,0,0,0,0,1,0,0})));
    System.out.println("g("+g+") xor h("+h+") = " + apply(g, h, Op.xor));
    
  }
}

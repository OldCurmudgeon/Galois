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
package com.oldcurmudgeon.galois.math;

import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.math.BigInteger;
import java.util.Iterator;

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
public abstract class Bits<T extends Indexed<BigInteger, BigInteger>> implements IndexedIterable<T, BigInteger> {
  @Override
  public abstract Iterator<T> iterator();

  protected abstract class BitsIterator implements IndexedIterator<T, BigInteger> {
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

    };
    // Perform the op.

    abstract BigInteger op(BigInteger a, BigInteger b);

  }

  // Where to pull next from - a or b.
  private enum Next {
    A {
      @Override
      <T extends Indexed<BigInteger, BigInteger>> BigInteger index(IndexedIterator<T, BigInteger> a, IndexedIterator<T, BigInteger> b) {
        return a.index();
      }

      @Override
      <T extends Indexed<BigInteger, BigInteger>> BigInteger value(IndexedIterator<T, BigInteger> a, IndexedIterator<T, BigInteger> b) {
        return a.next().value();
      }

      @Override
      Next other() {
        return Next.B;
      }

    },
    B {
      @Override
      <T extends Indexed<BigInteger, BigInteger>> BigInteger index(IndexedIterator<T, BigInteger> a, IndexedIterator<T, BigInteger> b) {
        return b.index();
      }

      @Override
      <T extends Indexed<BigInteger, BigInteger>> BigInteger value(IndexedIterator<T, BigInteger> a, IndexedIterator<T, BigInteger> b) {
        return b.next().value();
      }

      @Override
      Next other() {
        return Next.A;
      }

    };

    // The index of the next value.
    abstract <T extends Indexed<BigInteger, BigInteger>> BigInteger index(IndexedIterator<T, BigInteger> a, IndexedIterator<T, BigInteger> b);
    // The next value.

    abstract <T extends Indexed<BigInteger, BigInteger>> BigInteger value(IndexedIterator<T, BigInteger> a, IndexedIterator<T, BigInteger> b);

    abstract Next other();

    // Determine next.
    static Next next(Bits a, Bits b) {
      // If they both exist compare them - if one exists return it - otherwise return null.
      return a.i() != null ? b.i() != null ? a.i().compareTo(b.i()) < 0 ? A : B : A : null;
    }

  }

  private static Bits apply(Bits a, Bits b, Op op) {
    HugeBits applied = new HugeBits();
    // Which one is next.
    for (Next next = Next.next(a, b); next != null; next = Next.next(a, b)) {
      BigInteger index = next.index(a, b);
      BigInteger value = next.value(a, b);
      Next other = next.other();
      if (index.equals(other.index(a, b))) {
        // Perform the op.
        applied.add(new Big(index, op.op(value, other.value(a, b))));
      } else {
        // No counterpart - add it directly.
        applied.add(new Big(index, value));
      }
    }
    return applied;
  }

  public static void main(String[] args) {
    HugeBits a = new HugeBits(new Big(BigInteger.ZERO, BigInteger.valueOf(0xFEDCBA987654321L)));
    System.out.println("a = " + a);
    HugeBits b = new HugeBits(new Big(BigInteger.ZERO, BigInteger.valueOf(0x123456789ABCDEFL)));
    System.out.println("b = " + b);
    HugeBits c = new HugeBits(
            new Big(BigInteger.ZERO, BigInteger.ONE),
            new Big(BigInteger.TEN, BigInteger.ONE));
    System.out.println("c = " + c);
    System.out.println("a xor b = " + apply(a, b, Op.xor));
    System.out.println("a xor c = " + apply(a, c, Op.xor));
    System.out.println("b xor c = " + apply(b, c, Op.xor));
  }

}

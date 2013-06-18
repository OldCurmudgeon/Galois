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
 * T is the type of each part.
 * I is the type of the index.
 *
 * @author OldCurmudgeon
 */
public abstract class Bits implements IndexedIterator<BigInteger, BigInteger> {

  // The last returned - populated by next.
  BigInteger prev = null;
  // The next to return - populate in hasNext please.
  BigInteger next = null;
  // The index it is at - populate in hasNext please.
  BigInteger i = null;

  @Override
  public boolean hasNext() {
    if ( next == null ) {
      getNextAndSetIndex ();
    }
    return next != null;
  }

  // getNext must prime both next and i.
  protected abstract void getNextAndSetIndex();

  @Override
  public BigInteger next() {
    // Standard pattern for next.
    if (hasNext()) {
      prev = next;
      next = null;
      return prev;
    } else {
      return null;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public BigInteger i() {
    return i;
  }
  
  // Actual Bits processes that do things.
  // ToDo: Use ForkJoinPools for some of thois stuff.
  public static Bits xor (Bits a, Bits b) {
    // Ultimately use a lambda but for now I will use a loop and ops.
    // ToDo: Use an enum for the op.
    return iterateIgnoringZeros ( a, b, Op.xor);
  }
  
  enum Op {
    xor {
      public BigInteger op( BigInteger a, BigInteger b ) {
        return a.xor(b);
      }
    };
  }
  
  private static Bits iterateIgnoringZeros(Bits a, Bits b, Op op) {
    // Prime each one.
    BigInteger abi = a.next();
    BigInteger bbi = b.next();
    BigInteger ai = a.i();
    BigInteger bi = b.i();
    throw new UnsupportedOperationException("Not supported yet.");
  }
}

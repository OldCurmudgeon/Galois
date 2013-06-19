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
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Bits implementation using many BigIntegers.
 * 
 * @author OldCurmudgeon.
 */
public class HugeBits extends Bits {
  // The actual bits.
  private final TreeMap<BigInteger, BigInteger> bits = new TreeMap<>();
  // The iterator over them.
  private final Iterator<BigInteger> walker;

  // A packet to define a section of bits that fit into a BigInteger.
  public static class Big {
    final BigInteger index;
    final BigInteger value;

    public Big(BigInteger index, BigInteger value) {
      this.index = index;
      this.value = value;
    }
  }

  public HugeBits(Big... bigs) {
    for (Big i : bigs) {
      bits.put(i.index, i.value);
    }
    walker = bits.navigableKeySet().iterator();
  }

  // Does this interfere with the iterator?
  public void add ( Big big ) {
    bits.put(big.index, big.value);
  }
  
  @Override
  protected void getNextAndSetIndex() {
    if ( walker.hasNext() ) {
      // Next index.
      index = walker.next();
      // Next value.
      next = bits.get(index);
    } else {
      // Finished!
      index = null;
      next = null;
    }
  }
}

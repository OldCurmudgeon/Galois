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
public class HugeBits extends Bits<Big> {
  // The actual bits.
  private final TreeMap<BigInteger, Big> bits = new TreeMap<>();

  public HugeBits(Big... bigs) {
    for (Big i : bigs) {
      bits.put(i.index, i);
    }
  }

  // Does this interfere with the iterator?
  public void add ( Big big ) {
    bits.put(big.index, big);
  }
  
 @Override
  public SparseIterator<Big,BigInteger> iterator() {
    return new HugeBitsIterator(bits.values().iterator());
  }

  class HugeBitsIterator extends Bits.BitsIterator {
    private Iterator<Big> it;

    private HugeBitsIterator(Iterator<Big> it) {
      this.it = it;
    }


    @Override
    protected void getNext() {
      next = it.hasNext()?it.next():null;
    }

  }
}

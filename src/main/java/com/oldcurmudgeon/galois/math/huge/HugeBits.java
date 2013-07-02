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

import com.oldcurmudgeon.galois.math.sparse.Sparse;
import com.oldcurmudgeon.galois.math.sparse.SparseIterator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
      addWithoutNormalise(i);
    }
    // Make everything consistent.
    normalise();
  }

  // Does this interfere with the iterator?
  private void addWithoutNormalise(Big big) {
    // What do we do if two intersect?
    Big old = bits.get(big.index);
    if (old != null) {
      // Add them together - todo - is this correct?
      bits.put(big.index, new Big(big.index, big.value.add(old.value)));
    } else {
      // Just stick it in.
      bits.put(big.index, big);
    }
  }

  // Does this interfere with the iterator?
  public void add(Big big) {
    // Add it.
    addWithoutNormalise(big);
    // Make everything consistent.
    normalise();
  }

  /*
   */
  private void normalise() {
    TreeMap<BigInteger, Byte> bytes = new TreeMap<>();
    // Roll the whole lot out into bytes.
    for (Iterator<Map.Entry<BigInteger, Big>> big = bits.entrySet().iterator(); big.hasNext();) {
      Map.Entry<BigInteger, Big> it = big.next();
      BigInteger index = it.getKey();
      BigInteger value = it.getValue().value;
      byte[] itsBytes = value.toByteArray();
      for (int i = 0; i < itsBytes.length; i++) {
        BigInteger bi = BigInteger.valueOf(i);
        bytes.put(bi, itsBytes[i]);
      }
    }
    // Do nothing if empty.
    if (!bytes.isEmpty()) {
      // Unroll back out into a sequence of BigIntegers.
      bits.clear();
      // Start from the end.
      Map.Entry<BigInteger, Byte> lastEntry = bytes.lastEntry();
      BigInteger index = lastEntry.getKey();
      ArrayList<Byte> next = new ArrayList<>();
      // Walk it backwards.
      for (Map.Entry<BigInteger, Byte> entry : bytes.descendingMap().entrySet()) {
        //System.out.println("Entry " + entry.getKey() + " = " + entry.getValue());
        if (entry.getKey().equals(index)) {
          // Just append.
          next.add(entry.getValue());
          index.add(BigInteger.ONE);
        } else {
          add(index, next);
          next.clear();
        }
      }
      // And what's left.
      add(index, next);
    }
  }

  private void add(BigInteger index, ArrayList<Byte> next) {
    if (next.size() > 0) {
      // Make a new BigInteger.
      byte[] newBytes = new byte[next.size()];
      for (int i = 0; i < next.size(); i++) {
        newBytes[i] = next.get(i);
      }
      bits.put(index, new Big(index, new BigInteger(newBytes)));

    }
  }

  @Override
  public SparseIterator<Big, BigInteger> iterator() {
    return new HugeBitsIterator(bits.values().iterator());
  }

  @Override
  public SparseIterator<Big, BigInteger> reverseIterator() {
    return new HugeBitsIterator(bits.descendingMap().values().iterator());
  }

  @Override
  public BigInteger length() {
    Map.Entry<BigInteger, Big> lastEntry = bits.lastEntry();
    if (lastEntry == null) {
      return BigInteger.ZERO;
    }
    Big last = lastEntry.getValue();
    return last.index.add(last.length());
  }

  class HugeBitsIterator extends Bits.BitsIterator {
    private Iterator<Big> it;

    private HugeBitsIterator(Iterator<Big> it) {
      this.it = it;
    }

    @Override
    protected void getNext() {
      next = it.hasNext() ? it.next() : null;
    }

    public String toString() {
      return "[" + (next == null ? "" : next.toString()) + "]";
    }

  }
}

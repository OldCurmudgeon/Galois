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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
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

  private void normalise() {
    boolean repeat;
    do {
      // Keep doing it until nothing changes.
      repeat = false;
      // List of new ones to add.
      ArrayList<Big> add = new ArrayList<>();
      for (Iterator<Map.Entry<BigInteger, Big>> big = bits.entrySet().iterator(); big.hasNext();) {
        Map.Entry<BigInteger, Big> it = big.next();
        BigInteger value = it.getValue().value;
        boolean remove = false;
        // Discard all zeros.
        if (!value.equals(BigInteger.ZERO)) {
          // The index of the lowest byte.
          BigInteger index = it.getKey();
          // Inspect the bytes.
          byte[] bytes = value.toByteArray();
          // Cut out runs of zeros inside.
          boolean chopped;
          do {
            chopped = false;
            int f;
            int l = 0;
            for (f = 1; f < bytes.length - 1 && l == 0;) {
              if (bytes[f] == 0) {
                // Find the end of the range.
                for (l = 1; l < bytes.length - f - 1 && bytes[f + l] == 0;) {
                  // Work out the length.
                  l += 1;
                }
              } else {
                f += 1;
              }
            }
            if (l > 0) {
              // Make a new one.
              add.add(new Big(index.add(BigInteger.valueOf(bytes.length - f).multiply(EIGHT)), new BigInteger(Arrays.copyOfRange(bytes, 0, f))));
              // Remove it from the old.
              bytes = Arrays.copyOfRange(bytes, f + l, bytes.length);
              // Done some chopping.
              chopped = true;
            } else {
              // ToDo
            }
          } while (chopped);
          // Did we play around with the bytes?
          if (!add.isEmpty()) {
            // We hacked it around!
            remove = true;
            add.add(new Big(index, new BigInteger(bytes)));
          }
        } else {
          // Remove a zero.
          remove = true;
        }
        if (remove) {
          // Remove it.
          big.remove();
        }
      }
      if (!add.isEmpty()) {
        // Add my new ones.
        for (Big a : add) {
          addWithoutNormalise(a);
        }
        // Must repeat if we've added stuff.
        repeat = true;
      }
    } while (repeat);
  }

  /*
   * A normalise that always rebuilds.
   */
  private void rebuild() {
    TreeMap<BigInteger, Byte> bytes = new TreeMap<>();
    // Roll the whole lot out into bytes.
    for (Iterator<Map.Entry<BigInteger, Big>> big = bits.entrySet().iterator(); big.hasNext();) {
      Map.Entry<BigInteger, Big> it = big.next();
      BigInteger index = it.getKey();
      BigInteger value = it.getValue().value;
      byte[] itsBytes = value.toByteArray();
      for (int i = 0; i < itsBytes.length; i++) {
        BigInteger bi = index.add(BigInteger.valueOf(itsBytes.length - 1 - i));
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
        if (entry.getKey().equals(index) && entry.getValue() != 0) {
          // Just append.
          next.add(entry.getValue());
          index = index.subtract(BigInteger.ONE);
        } else {
          // Index has jumped or a zero byte.
          index = add(index, next);
          next.clear();
        }
      }
      // And what's left.
      add(index, next);
    }
  }

  private BigInteger add(BigInteger index, ArrayList<Byte> next) {
    if (next.size() > 0) {
      // Make a new BigInteger.
      byte[] newBytes = new byte[next.size()];
      for (int i = 0; i < next.size(); i++) {
        newBytes[i] = next.get(i);
      }
      bits.put(index, new Big(index, new BigInteger(newBytes)));
      return index.subtract(BigInteger.valueOf(next.size()));
    }
    return index;
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

    @Override
    public String toString() {
      return "[" + (next == null ? "" : next.toString()) + "]";
    }

  }

  public static void main(String[] args) {
    HugeBits i = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{1, 0, 1, 1, 0, 0, 0, 1})));
    HugeBits j = new HugeBits(
            new Big(BigInteger.ZERO, new BigInteger(new byte[]{1, 0, 0, 1, 0, 0, 1, 1})));
    System.out.println("i(" + i + ") xor j(" + j + ") = " + Bits.apply(i, j, Op.xor));

  }

}

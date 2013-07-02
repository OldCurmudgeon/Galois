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
  private static final BigInteger EIGHT = BigInteger.valueOf(8);

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
      repeat = false;
      // List of new ones to add.
      ArrayList<Big> add = new ArrayList<>();
      for (Iterator<Map.Entry<BigInteger, Big>> big = bits.entrySet().iterator(); big.hasNext();) {
        Map.Entry<BigInteger, Big> it = big.next();
        BigInteger index = it.getKey();
        BigInteger value = it.getValue().value;
        boolean remove = false;
        // Discard all zeros.
        if (!value.equals(BigInteger.ZERO)) {
          // Further remove head and trailing zero bytes.
          // Inspect the bytes.
          byte[] bytes = value.toByteArray();
          // Trim off the end.
          int trim;
          for (trim = 0; trim < bytes.length && bytes[trim] == 0;) {
            // Trim off the start.
            trim += 1;
          }
          // Slice off the end.
          int slice;
          for (slice = 0; slice < bytes.length - trim && bytes[bytes.length - slice - 1] == 0;) {
            // Slice off the end.
            slice += 1;
          }
          if (slice != 0) {
            // Step up the index.
            index = index.add(BigInteger.valueOf(slice));
          }
          if (trim != 0 || slice != 0) {
            // Cut it up.
            bytes = Arrays.copyOfRange(bytes, trim, trim + bytes.length - slice);
          }
          // Cut out runs of zeros inside.
          boolean chopped;
          do {
            chopped = false;
            int f;
            int l = 0;
            for (f = 1; f < bytes.length - 1 && l == 0; ) {
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
            }
          } while (chopped);
          // Did we play around with the bytes?
          if (trim != 0 || slice != 0 || !add.isEmpty()) {
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

  @Override
  public SparseIterator<Big, BigInteger> iterator() {
    return new HugeBitsIterator(bits.values().iterator());
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

  }
}

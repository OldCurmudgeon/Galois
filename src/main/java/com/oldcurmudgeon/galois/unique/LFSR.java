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
package com.oldcurmudgeon.galois.unique;

import com.oldcurmudgeon.galois.polynomial.FastPolynomial;
import com.oldcurmudgeon.galois.polynomial.GaloisPoly;
import com.oldcurmudgeon.toolbox.twiddlers.Strings;
import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Linear feedback shift register
 *
 * Taps can be found at:
 * See http://www.xilinx.com/support/documentation/application_notes/xapp052.pdf
 * See http://mathoverflow.net/questions/46961/how-are-taps-proven-to-work-for-lfsrs/46983#46983
 * See http://www.newwaveinstruments.com/resources/articles/m_sequence_linear_feedback_shift_register_lfsr.htm
 * See http://www.yikes.com/~ptolemy/lfsr_web/index.htm
 * See http://seanerikoconnor.freeservers.com/Mathematics/AbstractAlgebra/PrimitivePolynomials/overview.html
 * And on my flash.
 *
 * @author OldCurmudgeon
 */
public class LFSR implements Iterable<BigInteger> {
  // Bit pattern for taps.
  private final BigInteger taps;
  // Where to start (and end).
  private final BigInteger start;

  // The poly must be prime to span the full sequence.
  public LFSR(BigInteger primePoly, BigInteger start) {
    // Where to start from (and stop).
    this.start = start;
    // Knock off the 2^0 coefficient of the polynomial for the TAP.
    this.taps = primePoly.shiftRight(1);
  }

  public LFSR(GaloisPoly primePoly, BigInteger start) {
    this(primePoly.asBigInteger(), start);
  }

  public LFSR(GaloisPoly primePoly) {
    // Default to start at 1.
    this(primePoly.asBigInteger(), BigInteger.ONE);
  }

  public LFSR(int bits) {
    // Default to first found prime poly.
    this(new FastPolynomial().new PrimePolynomials(bits, true).iterator().next());
  }

  public LFSR(int bits, BigInteger start) {
    // Default to first prime poly.
    this(new FastPolynomial().new PrimePolynomials(bits, true).iterator().next(), start);
  }

  @Override
  public Iterator<BigInteger> iterator() {
    return new LFSRIterator(start);
  }

  private class LFSRIterator implements Iterator<BigInteger> {
    // The last one we returned.
    private BigInteger last = null;
    // The next one to return.
    private BigInteger next = null;

    public LFSRIterator(BigInteger start) {
      // Do not return the seed.
      last = start;
    }

    @Override
    public boolean hasNext() {
      if (next == null) {
        /*
         * Uses the Galois form.
         * 
         * Shift last right one.
         * 
         * If the bit shifted out was a 1 - xor with the tap mask.
         */
        boolean shiftedOutA1 = last.testBit(0);
        // Shift right.
        next = last.shiftRight(1);
        if (shiftedOutA1) {
          // Tap!
          next = next.xor(taps);
        }
        // Never give them `start` again.
        if (next.equals(start)) {
          // Could set a finished flag here too.
          next = null;
        }
      }
      return next != null;
    }

    @Override
    public BigInteger next() {
      // Remember this one.
      last = hasNext() ? next : null;
      // Don't deliver it again.
      next = null;
      return last;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String toString() {
      return LFSR.this.toString() + "[" + last.toString(16) + "-" + next.toString(16) + "]";
    }
  }

  @Override
  public String toString() {
    return "(" + taps.toString(32) + ")-" + start.toString(32);
  }

  public static void main(String args[]) {
    GaloisPoly.Log.LFSRValues.set(true);

    //test(12);
    //test(10);
    //for (int bits = 3; bits <= 7; bits++) {
    //  test(bits);
    //}
    // Maximal
    testPoly(new FastPolynomial().valueOf(2, 1, 0));
    testPoly(new FastPolynomial().valueOf(3, 2, 0));
    // 85
    //testPoly(new FastPolynomial().valueOf(8, 7, 3, 1, 0));
    //testPoly(new FastPolynomial().valueOf(8, 5, 4, 3, 2, 1, 0));
    //testPoly(new FastPolynomial().valueOf(8, 7, 6, 5, 4, 3, 0));
    // Maximal
    // x^8 + x^4 + x^3 + x^2 + 1
    //testPoly(new FastPolynomial().valueOf(8, 4, 3, 2, 0));
    // x^8 + x^7 + x^6 + x^5 + x^4 + x^2 + 1
    testPoly(new FastPolynomial().valueOf(8, 7, 6, 5, 4, 2, 0));
    // x^10 + x^7 + x^6 + x^5 + x^4 + x^3 + x^2 + x + 1
    testPoly(new FastPolynomial().valueOf(10, 7));
    // x^12 + x^10 + x^8 + x^7 + x^6 + x^5 + x^3 + x + 1
    //testPoly(new FastPolynomial().valueOf(12, 10, 8, 7, 6, 5, 3, 1, 0));
    // x^14 + x^5 + x^3 + x + 1
    //17,16,14,13,12,11,10,9,7,5,3,1,0
    GaloisPoly.Log.LFSRValues.set(false);
    testPoly(new FastPolynomial().valueOf(14, 5, 3, 1));
    //testPoly(new FastPolynomial().valueOf(17, 16, 14, 13, 12, 11, 10, 9, 7, 5, 3, 1));
    //testPoly(new FastPolynomial().valueOf(23, 5));
    testPoly(new FastPolynomial().valueOf(25, 3));
    testPoly(new FastPolynomial().valueOf(26, 11));
    //testPoly(new FastPolynomial().valueOf(63,1));
    //testPoly(new FastPolynomial().valueOf(95,77));
    //testPoly(new FastPolynomial().valueOf(399,271));

  }

  private static void test(int bits) {
    System.out.println("==== Bits " + bits + " ====");
    testPoly(new FastPolynomial().new PrimePolynomials(bits, true).iterator().next());
  }

  private static class Stats {
    Map<Integer, Integer> stats = new TreeMap<>();
    Map<Integer, Integer> odds = new TreeMap<>();
    Map<Integer, List<Integer>> oddSpots = new TreeMap<>();
    final int bits;
    int count = 0;

    public Stats(int bits) {
      this.bits = bits;
    }

    private void put(int key, int value) {
      stats.put(key, value);
    }
    // Too many bits to log spost.
    private static final int TooManyBits = 11;

    private void inc(BigInteger i, int which) {
      int bitCount = i.bitCount();
      inc(stats, bitCount);
      //One more.
      count += 1;
      if (i.testBit(0)) {
        // Odd!
        inc(odds, bitCount);
        // Only analyse odds if not too many bitts.
        if (bitCount < TooManyBits) {
          // Record it's spots.
          List<Integer> odd = oddSpots.get(bitCount);
          if (odd == null) {
            odd = new ArrayList<>();
            oddSpots.put(bitCount, odd);
          }
          odd.add(which);
        }
      }
    }

    private void log(int counted) {
      GaloisPoly.Log.LFSR.log("n\tcount(n)\to\tspots\tgaps\ttotal/nGaps");
      for (Integer n : stats.keySet()) {
        StringBuilder s = new StringBuilder();
        Separator tab = new Separator("\t");
        s.append(tab.sep()).append(n);
        // How many times a number of that many bits occurred.
        s.append(tab.sep()).append(stats.get(n));
        // How many times it was odd.
        Integer o = odds.get(n);
        s.append(tab.sep()).append(o != null ? o : "");
        // Only analyse odds if not too many bits.
        if (bits < TooManyBits) {
          // Where in the list the odds occurred.
          List<Integer> spots = oddSpots.get(n);
          if (spots != null) {
            s.append(tab.sep()).append(spots);
            if (spots.size() > 1) {
              // Work out the gaps between the spots - the run lengths.
              ArrayList<Integer> gaps = new ArrayList<>(spots.size() + 1);
              // First gap.
              gaps.add(spots.get(0));
              for (int i = 1; i < spots.size(); i++) {
                Integer gap = spots.get(i) - spots.get(i - 1);
                gaps.add(gap);
              }
              // Last gap.
              gaps.add(counted - spots.get(spots.size() - 1));
              s.append(tab.sep()).append(gaps);
              s.append(tab.sep()).append(counted / gaps.size());
            }
          }
        }
        GaloisPoly.Log.LFSR.log(s);
      }
    }

    private void inc(Map<Integer, Integer> stats, int n) {
      Integer soFar = stats.get(n);
      if (soFar == null) {
        soFar = new Integer(0);
      }
      soFar = soFar + 1;
      stats.put(n, soFar);
    }
  }
  static AtomicBoolean testing = new AtomicBoolean(false);

  public static void testPoly(GaloisPoly p) {
    if (!testing.getAndSet(true)) {
      try {
        int bits = p.degree().intValue();
        Stats stats = new Stats(bits);
        // For perfection.
        stats.put(0, 1);
        GaloisPoly.Log.LFSR.log("LFSR ", p, bits <= 8 ? p.isPrime() ? p.isPrimitive() ? " Primitive" : " Prime" : " Boring" : " Huge");
        LFSR lfsr = new LFSR(p);
        int count = 0;
        for (BigInteger i : lfsr) {
          GaloisPoly.Log.LFSRValues.log(Strings.pad(i.toString(2), Strings.zeros(bits))
                  + "\t" + i.bitCount());
          stats.inc(i, count);
          count += 1;
          if (count % 1000000 == 0) {
            System.out.println("Count: " + count);
          }
        }
        stats.log(count);
        GaloisPoly.Log.LFSR.log("Total ", count);

      } finally {
        testing.set(false);
      }

    }
  }
}

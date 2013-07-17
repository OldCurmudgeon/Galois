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

import com.oldcurmudgeon.galois.math.huge.Bits;
import com.oldcurmudgeon.galois.polynomial.FastPolynomial;
import com.oldcurmudgeon.galois.polynomial.GaloisPoly;
import com.oldcurmudgeon.toolbox.twiddlers.Strings;
import java.math.BigInteger;
import java.util.TreeMap;
import java.util.Iterator;
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
  private final BigInteger tapsMask;
  // Where to start (and end).
  private final BigInteger start;

  // The poly must be prime to span the full sequence.
  public LFSR(BigInteger primePoly, BigInteger start) {
    // Where to start from (and stop).
    this.start = start;
    // Knock off the 2^0 coefficient of the polynomial for the TAP.
    this.tapsMask = primePoly.shiftRight(1);
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
      next = start;
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
          next = next.xor(tapsMask);
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
    testPoly(new FastPolynomial().valueOf(8, 7, 3, 1, 0));
    testPoly(new FastPolynomial().valueOf(8, 5, 4, 3, 2, 1, 0));
    testPoly(new FastPolynomial().valueOf(8, 7, 6, 5, 4, 3, 0));
    // Maximal
    // x^8 + x^4 + x^3 + x^2 + 1
    testPoly(new FastPolynomial().valueOf(8, 4, 3, 2, 0));
    // x^8 + x^7 + x^6 + x^5 + x^4 + x^2 + 1
    testPoly(new FastPolynomial().valueOf(8, 7, 6, 5, 4, 2, 0));
    // x^12 + x^10 + x^8 + x^7 + x^6 + x^5 + x^3 + x + 1
    testPoly(new FastPolynomial().valueOf(12, 10, 8, 7, 6, 5, 3, 1, 0));
    // x^14 + x^5 + x^3 + x + 1
    testPoly(new FastPolynomial().valueOf(14, 5, 3, 1, 0));
  }
  
  private static void test(int bits) {
    System.out.println("==== Bits " + bits + " ====");
    testPoly(new FastPolynomial().new PrimePolynomials(bits, true).iterator().next());
  }
  
  private static class Stats {
    Map<Integer, Integer> stats = new TreeMap<>();
    Map<Integer, Integer> last = new TreeMap<>();
    int count = 0;
    
    private void put(int key, int value) {
      stats.put(key, value);
    }
    
    private void inc(int which, int bitCount) {
      Integer soFar = stats.get(bitCount);
      if (soFar == null) {
        soFar = new Integer(0);
      }
      soFar = soFar + 1;
      stats.put(bitCount, soFar);
      //One more.
      count += 1;
      // How far away are we from the last?
      Integer l = last.get(bitCount);
      if (l == null) {
        l = new Integer(0);
      }
    }
    
    private void log() {
      for (Integer n : stats.keySet()) {
        GaloisPoly.Log.LFSR.log("Count(", n, ")=", stats.get(n));
      }
    }
  }
  static AtomicBoolean testing = new AtomicBoolean(false);
  
  public static void testPoly(GaloisPoly p) {
    if (!testing.getAndSet(true)) {
      try {
        Stats stats = new Stats();
        // For perfection.
        stats.put(0, 1);
        int bits = p.degree().intValue();
        GaloisPoly.Log.LFSR.log("LFSR ", p, bits <= 8 ? p.isPrime() ? p.isPrimitive() ? " Primitive" : " Prime" : " Boring" : " Huge");
        LFSR lfsr = new LFSR(p);
        int count = 0;
        for (BigInteger i : lfsr) {
          GaloisPoly.Log.LFSRValues.log(Strings.pad(i.toString(2), Strings.zeros(bits))
                  + "\t" + i.bitCount());
          count += 1;
          stats.inc(count, i.bitCount());
        }
        stats.log();
        GaloisPoly.Log.LFSR.log("Total ", count);
        
      } finally {
        testing.set(false);
      }
      
    }
  }
}

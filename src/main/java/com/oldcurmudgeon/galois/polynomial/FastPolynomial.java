/*
 * Copyright 2013 OldCurmudgeon
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
package com.oldcurmudgeon.galois.polynomial;

import com.oldcurmudgeon.galois.math.PolyMath;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Fast implementation of PolyNomial.
 *
 * @author OldCurmudgeon
 */
public class FastPolynomial
        extends GaloisPoly<FastPolynomial>
        implements PolyMath<FastPolynomial>,
        Comparable<FastPolynomial> {
  /**
   * Using a BigInteger for speed.
   * Upside: Speed
   * Downside: Limited number of bits (but not that limited).
   *
   * ToDo: Use FastBitSet because it is mutable and may therefore be faster.
   */
  private final BigInteger degrees;
  // Base ones.
  static final FastPolynomial ZERO = new FastPolynomial();
  // Say it twice to avoid the `long` implementation.
  static final FastPolynomial ONE = new FastPolynomial().valueOf(0, 0);
  static final FastPolynomial X = new FastPolynomial().valueOf(1, 1);

  @Override
  public FastPolynomial zero() {
    return ZERO;
  }

  @Override
  public FastPolynomial one() {
    return ONE;
  }

  @Override
  public FastPolynomial x() {
    return X;
  }

  // Leave it empty.
  public FastPolynomial() {
    degrees = BigInteger.ZERO;
  }

  // Set my degrees from the clone.
  public FastPolynomial(FastPolynomial p) {
    degrees = p.degrees;
  }

  // Set my degrees from a long.
  public FastPolynomial(long l) {
    degrees = BigInteger.valueOf(l);
  }

  // Set my degrees from a long.
  public FastPolynomial(BigInteger bi) {
    degrees = bi;
  }

  @Override
  public FastPolynomial valueOf(int... powers) {
    BigInteger big = BigInteger.ZERO;
    for (int i : powers) {
      big = big.setBit(i);
    }
    // Fill in the rest down to 0 too.
    for ( int i = powers[powers.length-1] - 1; i >= 0; i-- ) {
      big = big.setBit(i);
    }
    return new FastPolynomial(big);
  }

  @Override
  public FastPolynomial valueOf(BigInteger... powers) {
    BigInteger big = BigInteger.ZERO;
    for (BigInteger i : powers) {
      if (i.compareTo(MAX) < 0) {
        big = big.setBit(i.intValue());
      } else {
        System.out.println("MAX Exceeded! " + i);
        throw new IllegalArgumentException("Power too big " + i);
      }
    }
    return new FastPolynomial(big);
  }

  /**
   * Constructs a polynomial using the bits from a BigInteger.
   */
  @Override
  public FastPolynomial valueOf(BigInteger big, long degree) {
    return new FastPolynomial(big.setBit((int) degree));
  }

  @Override
  public FastPolynomial multiply(FastPolynomial p) {
    BigInteger me = this.degrees;
    BigInteger it = p.degrees;
    BigInteger r = BigInteger.ZERO;
    for ( int i = 0; i < me.bitLength(); i++ ) {
      if ( me.testBit(i) ) {
        r = r.xor(it);
      }
      it = it.shiftLeft(1);
    }
    return new FastPolynomial(r);
  }

  @Override
  public FastPolynomial and(FastPolynomial p) {
    return new FastPolynomial(degrees.and(p.degrees));
  }

  @Override
  public FastPolynomial or(FastPolynomial p) {
    return new FastPolynomial(degrees.or(p.degrees));
  }

  @Override
  public FastPolynomial xor(FastPolynomial p) {
    return new FastPolynomial(degrees.xor(p.degrees));
  }

  private FastPolynomial shiftLeft(BigInteger i) {
    return new FastPolynomial(degrees.shiftLeft(i.intValue()));
  }

  @Override
  public FastPolynomial mod(FastPolynomial p) {
    BigInteger da = this.degree();
    BigInteger db = p.degree();
    BigInteger mod = degrees;
    for (BigInteger i = da.subtract(db); i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
      //if (mod.hasDegree(i.add(db))) {
      //  mod = mod.xor(that.shiftLeft(i));
      //}
      BigInteger d = i.add(db);
      if (mod.testBit(d.intValue())) {
        mod = mod.xor(p.degrees.shiftLeft(i.intValue()));
      }
    }
    return new FastPolynomial(mod);
  }

  @Override
  public FastPolynomial divide(FastPolynomial p) {
    BigInteger da = this.degree();
    BigInteger db = p.degree();
    FastPolynomial mod = new FastPolynomial(this);
    FastPolynomial div = new FastPolynomial();
    for (BigInteger i = da.subtract(db); i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
      if (mod.degrees.testBit(i.add(db).intValue())) {
        mod = mod.xor(p.shiftLeft(i));
        div = div.or(one().shiftLeft(i));
      }
    }
    return div;
  }

  @Override
  public FastPolynomial gcd(FastPolynomial p) {
    FastPolynomial gcd = new FastPolynomial(this);
    while (!p.degrees.equals(BigInteger.ZERO)) {
      FastPolynomial t = new FastPolynomial(p);
      p = gcd.mod(p);
      gcd = t;
    }
    return gcd;
  }

  @Override
  public boolean isEmpty() {
    return degrees.equals(BigInteger.ZERO);
  }

  @Override
  public int compareTo(FastPolynomial p) {
    return degrees.compareTo(p.degrees);
  }

  @Override
  public BigInteger degree() {
    return BigInteger.valueOf(degrees.bitLength() - 1);
  }

  @Override
  public BigInteger asBigInteger() {
    return degrees;
  }

  @Override
  public boolean equals(Object it) {
    return it != null
            && it instanceof FastPolynomial
            && ((FastPolynomial) it).degrees.equals(degrees);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 67 * hash + Objects.hashCode(this.degrees);
    return hash;
  }

  /**
   * Computes ( x^(2^p) - x ) mod f
   *
   * This function is useful for computing the reducibility of the polynomial
   *
   * ToDo: Move this to GaloisPoly
   */
  @Override
  protected FastPolynomial xToQtoIminusXmodF(final int i) {
    // compute (x^q^i mod f)
    BigInteger qToI = BQ.pow(i);
    FastPolynomial xToQtoImodF = X.modPow(qToI, this);
    // - x mod f
    FastPolynomial xToQtoIminusXmodF = xToQtoImodF.xor(X).mod(this);
    //System.out.println("Q2Q2I-X%F qToI=" + qToI + " xToQtoImodF=" + xToQtoImodF + " xToQtoIminusXmodF=" + xToQtoIminusXmodF);
    return xToQtoIminusXmodF;
  }

  /**
   * Computes x^e mod m.
   *
   * This algorithm requires at most this.degree() + m.degree() space.'
   *
   * http://en.wikipedia.org/wiki/Modular_exponentiation
   *
   * ToDo: Move this up into GaloisPoly
   */
  @Override
  public FastPolynomial modPow(BigInteger e, FastPolynomial m) {
    FastPolynomial result = ONE;
    FastPolynomial b = new FastPolynomial(this);

    while (!e.equals(BigInteger.ZERO)) {
      //System.out.println("modPow (" + e + "," + m + ") result=" + result + " b=" + b);
      if (e.testBit(0)) {
        result = result.multiply(b).mod(m);
      }
      e = e.shiftRight(1);
      b = b.multiply(b).mod(m);
    }

    //System.out.println("modPow (" + e + "," + m + ") result=" + result + " b=" + b);
    return result;
  }

  /**
   * Returns standard ascii representation of this polynomial in the form:
   *
   * e.g.: x^8 + x^4 + x^3 + x + 1
   */
  public String toPolynomialString() {
    StringBuilder str = new StringBuilder();
    for (int i = degrees.bitLength(); i >= 0; i--) {
      if (degrees.testBit(i)) {
        if (str.length() != 0) {
          str.append(" + ");
        }
        if (i == 0) {
          str.append("1");
        } else {
          str.append("x");
          if (i > 1) {
            str.append("^").append(i);
          }
        }
      }
    }
    return str.toString();
  }

  /**
   * Default toString override uses the ascii representation
   */
  @Override
  public String toString() {
    return toPolynomialString();
  }

  private static void test(FastPolynomial p) {
    System.out.println(p.degrees.toString(2) + "=" + p.toString());
  }

  public static void main(String[] args) {
    FastPolynomial p = new FastPolynomial();
    test(p);
    test(p.plus(ONE));
    test(p.multiply(X));
    test(p.plus(X));
    p = p.valueOf(8, 6, 4, 2, 1, 0);
    test(p);
  }

}

/*
 * Fast implementation of PolyNomial.
 * 
 * Most(all) functionality is(potentially) MUTABLE!!
 */
package com.oldcurmudgeon.galois.polynomial;

import java.math.BigInteger;

/**
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
  private BigInteger degrees = BigInteger.ZERO;
  // Base ones.
  static final FastPolynomial ZERO = new FastPolynomial();
  static final FastPolynomial ONE = new FastPolynomial(1);
  static final FastPolynomial X = new FastPolynomial(2);
  
  // Leave it empty.
  public FastPolynomial() {
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
  public FastPolynomial valueOf(int ... powers) {
    FastPolynomial p = new FastPolynomial();
    for ( int i : powers ) {
      degrees = degrees.setBit(i);
    }
    return p;
  }

 @Override
  public FastPolynomial multiply(FastPolynomial p) {
    degrees = degrees.multiply(p.degrees);
    return this;
  }

  @Override
  public FastPolynomial and(FastPolynomial p) {
    degrees = degrees.and(p.degrees);
    return this;
  }

  @Override
  public FastPolynomial or(FastPolynomial p) {
    degrees = degrees.or(p.degrees);
    return this;
  }

  @Override
  public FastPolynomial xor(FastPolynomial p) {
    degrees = degrees.xor(p.degrees);
    return this;
  }

  @Override
  public FastPolynomial mod(FastPolynomial p) {
    degrees = degrees.mod(p.degrees);
    return this;
  }

  @Override
  public FastPolynomial divide(FastPolynomial p) {
    degrees = degrees.divide(p.degrees);
    return this;
  }

  @Override
  public FastPolynomial gcd(FastPolynomial p) {
    degrees = degrees.gcd(p.degrees);
    return this;
  }

  @Override
  public int compareTo(FastPolynomial p) {
    return degrees.compareTo(p.degrees);
  }

   @Override
  public BigInteger degree() {
     return BigInteger.valueOf(degrees.bitLength());
  }
   
 /**
   * Returns standard ascii representation of this polynomial in the form:
   *
   * e.g.: x^8 + x^4 + x^3 + x + 1
   */
  public String toPolynomialString() {
    StringBuilder str = new StringBuilder();
    for ( int i = degrees.bitLength(); i >= 0; i-- ) {
      if ( degrees.testBit(i)) {
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
  
  private static void test ( FastPolynomial p ) {
    System.out.println(p.degrees.toString(2)+"="+p.toString());
  }
  
  public static void main(String[] args) {
    FastPolynomial p = new FastPolynomial();
    test(p);
    test(p.plus(ONE));
    test(p.multiply(X));
    test(p.plus(X));
    p = p.valueOf(8,6,4,2,1,0);
    test(p);
  }

}

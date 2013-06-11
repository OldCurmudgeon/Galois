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
  static final FastPolynomial ONE = new FastPolynomial(0);
  static final FastPolynomial X = new FastPolynomial(1);
  @Override
  public FastPolynomial x () {
    return X;
  }
  @Override
  public FastPolynomial zero () {
    return ZERO;
  }
  @Override
  public FastPolynomial one () {
    return ONE;
  }
  
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

  /**
   * Constructs a polynomial using the bits from a BigInteger.
   */
  @Override
  public FastPolynomial valueOf(BigInteger big, long degree) {
    degrees = big.setBit((int)degree);
    return this;
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
   * Computes ( x^(2^p) - x ) mod f
   *
   * This function is useful for computing the reducibility of the polynomial
   * 
   * ToDo: Move this to GaloisPoly
   */
  @Override
  protected FastPolynomial reduceExponent(final int p) {
    // compute (x^q^p mod f)
    BigInteger q_to_p = BQ.pow(p);
    FastPolynomial x_to_q_to_p = x().modPow(q_to_p, this);

    // subtract (x mod f)
    return x_to_q_to_p.xor(X).mod(this);
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

    while (e.bitCount() != 0) {
      if (e.testBit(0)) {
        result = result.multiply(b).mod(m);
      }
      e = e.shiftRight(1);
      b = b.multiply(b).mod(m);
    }

    return result;
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

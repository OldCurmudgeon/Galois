/*
 * Fast implementation of PolyNomial.
 * 
 * Most(all) functionality is(potentially) MUTABLE!!
 */
package com.oldcurmudgeon.galois.polynomial;

import java.math.BigInteger;
import java.util.Objects;

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
  private final BigInteger degrees;
  // Base ones.
  static final FastPolynomial ZERO = new FastPolynomial();
  // Avoids the `long` implementation.
  static final FastPolynomial ONE = new FastPolynomial().valueOf(0, 0);
  static final FastPolynomial X = new FastPolynomial().valueOf(1, 1);
  @Override
  public FastPolynomial zero () {
    return new FastPolynomial();
  }
  @Override
  public FastPolynomial one () {
    return new FastPolynomial(1);
  }
  @Override
  public FastPolynomial x () {
    return new FastPolynomial(2);
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
  public FastPolynomial valueOf(int ... powers) {
    BigInteger big = BigInteger.ZERO;
    for ( int i : powers ) {
      big = big.setBit(i);
    }
    return new FastPolynomial(big);
  }

  /**
   * Constructs a polynomial using the bits from a BigInteger.
   */
  @Override
  public FastPolynomial valueOf(BigInteger big, long degree) {
    return new FastPolynomial(big.setBit((int)degree));
  }

  @Override
  public FastPolynomial multiply(FastPolynomial p) {
    return new FastPolynomial(degrees.multiply(p.degrees));
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
    while(!p.degrees.equals(BigInteger.ZERO)) {
      FastPolynomial t = new FastPolynomial(p);
      p = gcd.mod(p);
      gcd = t;
    }
    return gcd;
  }

  @Override
  public int compareTo(FastPolynomial p) {
    return degrees.compareTo(p.degrees);
  }

   @Override
  public BigInteger degree() {
     return BigInteger.valueOf(degrees.bitLength()-1);
  }
   
  @Override
  public BigInteger asBigInteger() {
    return degrees;
  }

  @Override
  public boolean equals (Object it) {
    return it != null &&
            it instanceof FastPolynomial &&
            ((FastPolynomial) it).degrees.equals(degrees);
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

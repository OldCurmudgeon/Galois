package com.oldcurmudgeon.galois.polynomial;

import com.oldcurmudgeon.galois.math.PolyMath;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * https://code.google.com/p/rabinfingerprint/source/browse/trunk/src/org/bdwyer/galoisfield/Polynomial.java?r=4
 *
 * NB: All in Z[2]
 *
 * Galois field http://www.ee.unb.ca/tervo/ee4253/poly.shtml
 *
 * @author themadcreator
 */
public class Polynomial extends GaloisPoly<Polynomial> implements PolyMath<Polynomial>, Comparable<Polynomial> {
  /**
   * A (sorted) set of the degrees of the terms of the polynomial
   */
  private final TreeSet<BigInteger> degrees;
  /**
   * the polynomial ""
   */
  public static final Polynomial ZERO = new Polynomial();
  /**
   * the polynomial "x"
   */
  public static final Polynomial X = new Polynomial().valueOf(1,  1);
  /**
   * the polynomial "1"
   */
  public static final Polynomial ONE = new Polynomial().valueOf(0, 0);

  @Override
  public Polynomial x () {
    return X;
  }
  @Override
  public Polynomial zero () {
    return ZERO;
  }
  @Override
  public Polynomial one () {
    return ONE;
  }
  
  /**
   * Constructs a polynomial using the bits from a long.
   */
  public Polynomial valueOf(long l) {
    Set<BigInteger> dgrs = createDegreesCollection();
    int i = 0;
    while (l != 0) {
      if ((l & 1) == 1) {
        dgrs.add(BigInteger.valueOf(i));
      }
      i++;
      l >>= 1;
    }
    return new Polynomial(dgrs);
  }

  /**
   * Constructs a polynomial using the bits from a BigInteger.
   */
  @Override
  public Polynomial valueOf(BigInteger big, long degree) {
    Set<BigInteger> dgrs = createDegreesCollection();
    // NB: BigInteger uses Big Endian.
    byte[] bytes = big.toByteArray();

    for (int i = 0; i < degree; i++) {
      int aidx = bytes.length - 1 - (i / 8); // byte array index
      if (aidx >= 0) {
        int bidx = i % 8; // bit index
        byte b = bytes[aidx];
        if (((b >> bidx) & 1) == 1) {
          dgrs.add(BigInteger.valueOf(i));
        }
      }
    }
    dgrs.add(BigInteger.valueOf(degree));
    return new Polynomial(dgrs);
  }

  @Override
  public Polynomial valueOf(int... powers) {
    Set<BigInteger> dgrs = createDegreesCollection();
    for (int i : powers) {
      dgrs.add(BigInteger.valueOf(i));
    }
    return new Polynomial(dgrs);
  }

  @Override
  public Polynomial valueOf(BigInteger... powers) {
    Set<BigInteger> dgrs = createDegreesCollection();
    for (BigInteger i : powers) {
      dgrs.add(i);
    }
    return new Polynomial(dgrs);
  }

  public Polynomial() {
    degrees = createDegreesCollection();
  }

  protected Polynomial(Collection<BigInteger> degrees) {
    this();
    this.degrees.addAll(degrees);
  }

  public Polynomial(Polynomial p) {
    this(p.degrees);
  }

  protected static TreeSet<BigInteger> createDegreesCollection() {
    return new TreeSet<>(new ReverseComparator());
  }

  @Override
  public BigInteger degree() {
    if (degrees.isEmpty()) {
      return BigInteger.ONE.negate();
    }
    return degrees.first();
  }

  @Override
  public boolean isEmpty() {
    return degrees.isEmpty();
  }

  /**
   * Computes (this * that) in GF(2^k)
   */
  @Override
  public Polynomial multiply(Polynomial that) {
    Set<BigInteger> dgrs = createDegreesCollection();
    for (BigInteger pa : this.degrees) {
      for (BigInteger pb : that.degrees) {
        BigInteger sum = pa.add(pb);
        // xor the result
        if (dgrs.contains(sum)) {
          dgrs.remove(sum);
        } else {
          dgrs.add(sum);
        }
      }
    }
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this & that) in GF(2^k)
   */
  @Override
  public Polynomial and(Polynomial that) {
    Set<BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.retainAll(that.degrees);
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this | that) in GF(2^k)
   */
  @Override
  public Polynomial or(Polynomial that) {
    Set<BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.addAll(that.degrees);
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this ^ that) in GF(2^k)
   */
  @Override
  public Polynomial xor(Polynomial that) {
    Polynomial or = this.or(that);
    Polynomial and = this.and(that);
    Set<BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(or.degrees);
    dgrs.removeAll(and.degrees);
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this % that) in GF(2^k)
   */
  @Override
  public Polynomial mod(Polynomial that) {
    BigInteger da = this.degree();
    BigInteger db = that.degree();
    Polynomial mod = new Polynomial(this);
    for (BigInteger i = da.subtract(db); i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
      if (mod.hasDegree(i.add(db))) {
        mod = mod.xor(that.shiftLeft(i));
      }
    }
    return mod;
  }

  /**
   * Computes (this / that) in GF(2^k)
   * Probably not the most efficient.
   */
  @Override
  public Polynomial divide(Polynomial that) {
    BigInteger da = this.degree();
    BigInteger db = that.degree();
    Polynomial mod = new Polynomial(this);
    Polynomial div = new Polynomial();
    for (BigInteger i = da.subtract(db); i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
      if (mod.hasDegree(i.add(db))) {
        mod = mod.xor(that.shiftLeft(i));
        div = div.or(ONE.shiftLeft(i));
      }
    }
    return div;
  }

  /**
   * Computes (this <<shift) in GF(2^k)
   */
  public Polynomial shiftLeft(BigInteger shift) {
    Set<BigInteger> dgrs = createDegreesCollection();
    for (BigInteger degree : degrees) {
      dgrs.add(degree.add(shift));
    }
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this >> shift) in GF(2^k)
   */
  public Polynomial shiftRight(BigInteger shift) {
    Set<BigInteger> dgrs = createDegreesCollection();
    for (BigInteger degree : degrees) {
      BigInteger shifted = degree.subtract(shift);
      if (shifted.compareTo(BigInteger.ZERO) < 0) {
        continue;
      }
      dgrs.add(shifted);
    }
    return new Polynomial(dgrs);
  }

  /**
   * Tests if there exists a term with degree k
   */
  public boolean hasDegree(BigInteger k) {
    return degrees.contains(k);
  }

  /**
   * Sets the coefficient of the term with degree k to 1
   */
  public Polynomial setDegree(BigInteger k) {
    Set<BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.add(k);
    return new Polynomial(dgrs);
  }

  /**
   * Sets the coefficient of the term with degree k to 0
   */
  public Polynomial clearDegree(BigInteger k) {
    Set<BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.remove(k);
    return new Polynomial(dgrs);
  }

  /**
   * Toggles the coefficient of the term with degree k
   */
  public Polynomial toggleDegree(BigInteger k) {
    Set<BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    if (dgrs.contains(k)) {
      dgrs.remove(k);
    } else {
      dgrs.add(k);
    }
    return new Polynomial(dgrs);
  }

  /**
   * Computes x^e mod m.
   *
   * This algorithm requires at most this.degree() + m.degree() space.'
   *
   * http://en.wikipedia.org/wiki/Modular_exponentiation
   */
  @Override
  public Polynomial modPow(BigInteger e, Polynomial m) {

    Polynomial result = Polynomial.ONE;
    Polynomial b = new Polynomial(this);

    while (!e.equals(BigInteger.ZERO)) {
      //System.out.println("modPow ("+e + ","+m+") result="+result+" b="+b);
      if (e.testBit(0)) {
        result = result.multiply(b).mod(m);
      }
      e = e.shiftRight(1);
      b = b.multiply(b).mod(m);
    }

    //System.out.println("modPow ("+e + ","+m+") result="+result+" b="+b);
    return result;
  }

  /**
   * Computes the greatest common divisor between polynomials using Euclid's algorithm
   *
   * http://en.wikipedia.org/wiki/Euclids_algorithm
   */
  @Override
  public Polynomial gcd(Polynomial p) {
    Polynomial a = new Polynomial(this);
    while (!p.isEmpty()) {
      Polynomial t = new Polynomial(p);
      p = a.mod(p);
      a = t;
    }
    return a;
  }

  /**
   * Computes ( x^(2^p) - x ) mod f
   *
   * This function is useful for computing the reducibility of the polynomial
   * 
   * ToDo: Move this to GaloisPoly
   */
  @Override
  protected Polynomial xToQtoIminusXmodF(final int i) {
    BigInteger qToI = BQ.pow(i);
    Polynomial xToQtoImodF = X.modPow(qToI, this);
    Polynomial xToQtoIminusXmodF = xToQtoImodF.xor(X).mod(this);
    //System.out.println("Q2Q2I-X%F qToI="+qToI + " xToQtoImodF="+xToQtoImodF+" xToQtoIminusXmodF="+xToQtoIminusXmodF);
    return xToQtoIminusXmodF;
  }
  
  /**
   * Construct a BigInteger whose value represents this polynomial. This can
   * lose information if the degrees of the terms are larger than
   * Integer.MAX_VALUE;
   */
  @Override
  public BigInteger asBigInteger() {
    BigInteger b = BigInteger.ZERO;
    for (BigInteger degree : degrees) {
// technically accurate but slow as hell:
//                      BigInteger term = BigInteger.ONE;
//                      for ( BigInteger i = BigInteger.ONE; i.compareTo( degree ) >= 0; i = i.add( BigInteger.ONE ) ) {
//                              term = term.shiftLeft( 1 );
//                      }
      b = b.setBit((int) degree.longValue());
    }
    return b;
  }

  /**
   * Returns a string of hex characters representing this polynomial
   */
  public String toHexString() {
    return asBigInteger().toString(16).toUpperCase();
  }

  /**
   * Returns a string of digits presenting this polynomial
   */
  public String toDecimalString() {
    return asBigInteger().toString();
  }

  /**
   * Returns a string of binary digits presenting this polynomial
   */
  public String toBinaryString() {
    StringBuilder str = new StringBuilder();
    for (BigInteger deg = degree(); deg.compareTo(BigInteger.ZERO) >= 0; deg = deg.subtract(BigInteger.ONE)) {
      if (degrees.contains(deg)) {
        str.append("1");
      } else {
        str.append("0");
      }
    }
    return str.toString();
  }

  /**
   * Returns standard ascii representation of this polynomial in the form:
   *
   * e.g.: x^8 + x^4 + x^3 + x + 1
   */
  public String toPolynomialString() {
    StringBuilder str = new StringBuilder();
    for (BigInteger degree : degrees) {
      if (str.length() != 0) {
        str.append(" + ");
      }
      if (degree.compareTo(BigInteger.ZERO) == 0) {
        str.append("1");
      } else {
        str.append("x");
        if (degree.compareTo(BigInteger.ONE) > 0) {
          str.append("^").append(degree);
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

  /**
   * Compares this polynomial to the other
   */
  @Override
  public int compareTo(Polynomial o) {
    int cmp = degree().compareTo(o.degree());
    if (cmp != 0) {
      return cmp;
    }
    // get first degree difference
    Polynomial x = this.xor(o);
    if (x.isEmpty()) {
      return 0;
    }
    return this.hasDegree(x.degree()) ? 1 : 0;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof Polynomial)) {
      return false;
    }
    // All degrees are the same.
    return degrees.equals(((Polynomial) that).degrees);
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + Objects.hashCode(this.degrees);
    return hash;
  }

}

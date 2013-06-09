/*
 * Fast implementation of PolyNomial.
 * 
 * Most(all) functionality is(potentially) MUTABLE!!
 */
package com.oldcurmudgeon.galois.polynomial;

import com.oldcurmudgeon.toolbox.walkers.Iterables;
import java.math.BigInteger;
import java.util.Set;
import javolution.util.FastBitSet;
import javolution.util.Index;

/**
 * @author OldCurmudgeon
 */
public class FastPolynomial
        extends GaloisPoly<FastPolynomial>
        implements PolyMath<FastPolynomial>,
        Comparable<FastPolynomial> {
  /**
   * A set of the degrees of the terms of the polynomial
   */
  private final FastBitSet degrees = new FastBitSet();

  // Set my (empty) degrees) from the clone.
  public FastPolynomial(FastPolynomial clone) {
    degrees.or(clone.degrees);
  }

  @Override
  public FastPolynomial times(FastPolynomial o) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public FastPolynomial and(FastPolynomial o) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public FastPolynomial or(FastPolynomial o) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public FastPolynomial xor(FastPolynomial it) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public FastPolynomial mod(FastPolynomial o) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public FastPolynomial div(FastPolynomial o) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public FastPolynomial gcd(FastPolynomial o) {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int compareTo(FastPolynomial t) {
    // Identical -> zero.
    if (degrees.equals(t.degrees)) {
      return 0;
    }
    // Walk down the degrees.
    return Iterables.compare(degrees.iterator(), t.degrees.iterator());
  }

  public static void main(String[] args) {
  }
}

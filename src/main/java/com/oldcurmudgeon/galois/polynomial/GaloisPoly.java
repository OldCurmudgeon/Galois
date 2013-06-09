/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.oldcurmudgeon.galois.polynomial;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * @author OldCurmudgeon
 */
public abstract class GaloisPoly<T extends PolyMath<T>> implements PolyMath<T> {
  /**
   * number of elements in the finite field GF(2)
   */
  protected static final long Q = 2L;
  // A BigInteger version of it.
  protected static final BigInteger BQ = BigInteger.valueOf(Q);

  @Override
  public abstract T times(T o);

  @Override
  public abstract T and(T o);

  @Override
  public abstract T or(T o);

  @Override
  public abstract T mod(T o);

  @Override
  public abstract T div(T o);

  @Override
  public abstract T gcd(T o);

  @Override
  public abstract T xor(T o);

  /**
   * An enum representing the reducibility of the polynomial
   *
   * A polynomial p(x) in GF(2^k) is called irreducible over GF[2^k] if it is
   * non-constant and cannot be represented as the product of two or more
   * non-constant polynomials from GF(2^k).
   *
   * http://en.wikipedia.org/wiki/Irreducible_element
   */
  public static enum Reducibility {
    REDUCIBLE, IRREDUCIBLE
  };
  
  /**
   * a reverse comparator so that polynomials are printed out correctly
   */
  protected static final class ReverseComparator implements Comparator<BigInteger> {
    @Override
    public int compare(BigInteger o1, BigInteger o2) {
      return -1 * o1.compareTo(o2);
    }
  }

  // In a Galois field plus and minus are xor.
  @Override
  public T plus(T it) {
    // plus === minus === xor
   return xor(it);
  }

  // In a Galois field plus and minus are xor.
  @Override
  public T minus(T it) {
    // plus === minus === xor
   return xor(it);
  }

}

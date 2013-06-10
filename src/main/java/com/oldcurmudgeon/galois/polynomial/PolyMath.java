package com.oldcurmudgeon.galois.polynomial;

/**
 * What should be possible with a polynomial.
 * 
 * https://code.google.com/p/rabinfingerprint/source/browse/trunk/src/org/bdwyer/galoisfield/Arithmetic.java?r=4
 *
 * @originalauthor themadcreator
 * @adjustedby OldCurmudgeons
 */
public interface PolyMath<T> {
  public T plus(T o);

  public T minus(T o);

  public T multiply(T o);

  public T and(T o);

  public T or(T o);

  public T xor(T o);

  public T mod(T o);

  public T divide(T o);

  public T gcd(T o);

}

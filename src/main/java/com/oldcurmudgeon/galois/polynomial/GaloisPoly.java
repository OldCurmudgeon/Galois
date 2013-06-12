/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.galois.polynomial;

import com.oldcurmudgeon.toolbox.walkers.BitPattern;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author OldCurmudgeon
 */
public abstract class GaloisPoly<T extends GaloisPoly<T>> implements PolyMath<T> {
  /**
   * number of elements in the finite field GF(2)
   */
  protected static final long Q = 2L;
  // A BigInteger version of it.
  protected static final BigInteger BQ = BigInteger.valueOf(Q);

  @Override
  public abstract T xor(T o);

  @Override
  public abstract T and(T o);

  @Override
  public abstract T or(T o);

  @Override
  public abstract T multiply(T o);

  @Override
  public abstract T mod(T o);

  public abstract T modPow(BigInteger e, T m);

  @Override
  public abstract T divide(T o);

  @Override
  public abstract T gcd(T o);
  
  public abstract T x();
  public abstract T zero();
  public abstract T one();

  public abstract BigInteger asBigInteger();
  // What degree am I.
  public abstract BigInteger degree();
  
  // Should be static - but no way to do that in Java.
  public abstract GaloisPoly valueOf(int ... powers);

  /**
   * Constructs a polynomial using the bits from a BigInteger.
   */
  public abstract T valueOf(BigInteger big, long degree);

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

  /**
   * Tests the reducibility of the polynomial
   */
  public boolean isReducible() {
    return getReducibility() == Reducibility.REDUCIBLE;
  }

  /**
   * Computes ( x^(2^p) - x ) mod f
   *
   * This function is useful for computing the reducibility of the polynomial
   * 
   * ToDo: Move this to GaloisPoly
   */
  abstract T reduceExponent(final int p);
  
  /*
   * An irreducible polynomial of degree m, 
   * F(x) over GF(p) for prime p, is a primitive polynomial 
   * if the smallest positive integer n such that 
   * F(x) divides x^n - 1 is n = p^m − 1.
   * 
   * ToDo: Somehow retain primality.
   * 
   * By discovery: x^12 + x^3 + 1 is NOT primitive but IS prime.
   * 
   * http://theory.cs.uvic.ca/inf/neck/PolyInfo.html
   * 
   * A polynomial over GF(2) is primitive if it has order 2^n-1. 
   * For example, x2+x+1 has order 3 = 2^2-1 since (x^2+x+1)(x+1) = x^3+1. 
   * Thus x^2+x+1 is primitive. 
   * 
   * http://maths-people.anu.edu.au/~brent/pd/rpb199.pdf
   * 
   * A polynomial P(x) of degree r > 1 is primitive if P(x) is irreducible
   * and x^j != 1 mod P(x) for 0 <j <2^r - 1.
   * 
   * http://mathworld.wolfram.com/PrimitiveGaloisPoly.html
   * 
   * A polynomial of degree n over the finite field GF(2) 
   * (i.e., with coefficients either 0 or 1) is primitive if it has 
   * polynomial order 2^n-1
   * 
   * http://mathworld.wolfram.com/GaloisPolyOrder.html
   * 
   * In particular, the order of a polynomial P(x) with P(0)!=0 is the 
   * smallest integer e for which P(x) divides x^e+1 (Lidl and Niederreiter 1994).
   */
  public boolean isPrimitive() {
    try {
      return Primitivity.test(this);
    } catch (InterruptedException | ExecutionException ex) {
      // Rethrow it as a runtime exception.
      throw new RuntimeException(ex);
    }
  }

  private static class Primitivity {
    // The common pool.
    private static ForkJoinPool pool = new ForkJoinPool();
    // How many calculations per task.
    private static final BigInteger GRANULARITY = BigInteger.valueOf(1000);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    // Test failed.
    //volatile static boolean failed = false;

    // Tests primitivity of a GaloisPoly using the pool.
    private static boolean test(GaloisPoly p) throws InterruptedException, ExecutionException {
      // The required order o = 2^r - 1
      BigInteger o = BQ.pow(p.degree().intValue()).subtract(BigInteger.ONE);
      // Initially not failed.
      AtomicBoolean failed = new AtomicBoolean(false);
      // Build the task.
      Task task = new Task(p, BigInteger.ONE, o, failed);
      // Process it in the pool.
      pool.invoke(task);
      // Deliver the answer.
      return task.get();
    }

    private static class Task extends RecursiveTask<Boolean> {
      // The polynomial we are testing.
      final GaloisPoly it;
      // Where to start.
      final BigInteger start;
      // Where to stop.
      final BigInteger stop;
      // Has the whole test failed?
      final AtomicBoolean failed;

      public Task(GaloisPoly it, BigInteger start, BigInteger stop, AtomicBoolean failed) {
        this.it = it;
        this.start = start;
        this.stop = stop;
        this.failed = failed;
      }

      @Override
      protected Boolean compute() {
        // Do nothing if failed already.
        if (!failed.get()) {
          // Compute or fork?
          if (stop.subtract(start).compareTo(GRANULARITY) < 0) {
            return computeDirectly();
          } else {
            // Fork!
            BigInteger split = stop.subtract(start).divide(TWO).add(start);
            Task is1 = new Task(it, start, split, failed);
            Task is2 = new Task(it, split, stop, failed);
            is1.fork();
            // Join.
            return is2.compute().booleanValue() && is1.join().booleanValue();
          }
        }
        // Definitely not if failed.
        return false;
      }

      protected Boolean computeDirectly() {
        // Do it for myself.
        for (BigInteger e = start; e.compareTo(stop) < 0 && !failed.get(); e = e.add(BigInteger.ONE)) {
          // p = (x^e + 1)
          GaloisPoly p = it.valueOf(e.intValue(), 0);
          PolyMath mod = p.mod(it);
          if (true) {
            // We failed - but are we the first?
            if (failed.getAndSet(true) == false) {
              System.out.println("Reject " + it + " = (" + p + ")/(" + p.divide(it) + ")");
            }
          }
        }
        return !failed.get();
      }
    }
  }

  /**
   * Tests the reducibility of the polynomial
   */
  public Reducibility getReducibility() {
    return getReducibilityBenOr();
  }

  /**
   * BenOr Reducibility Test
   *
   * Tests and Constructions of Irreducible GaloisPolys over Finite Fields
   * (1997) Shuhong Gao, Daniel Panario
   *
   * http://citeseer.ist.psu.edu/cache/papers/cs/27167/http:zSzzSzwww.math.clemson.eduzSzfacultyzSzGaozSzpaperszSzGP97a.pdf/gao97tests.pdf
   */
  protected Reducibility getReducibilityBenOr() {
    final long degree = this.degree().longValue();
    for (int i = 1; i <= (int) (degree / 2); i++) {
      T b = reduceExponent(i);
      T g = gcd(b);
      if (!g.equals(one())) {
        return Reducibility.REDUCIBLE;
      }
    }

    return Reducibility.IRREDUCIBLE;
  }

  /**
   * Rabin's Reducibility Test
   *
   * This requires the distinct prime factors of the degree, so we don't use
   * it. But this could be faster for prime degree polynomials
   */
  protected Reducibility getReducibilityRabin(int[] factors) {
    int degree = (int) degree().longValue();
    for (int i = 0; i < factors.length; i++) {
      //int n_i = factors[i];
      T b = reduceExponent(i);
      T g = gcd(b);
      if (!g.equals(one())) {
        return Reducibility.REDUCIBLE;
      }
    }

    T g = reduceExponent(degree);
    if (!g.equals(zero())) {
      return Reducibility.REDUCIBLE;
    }

    return Reducibility.IRREDUCIBLE;
  }

  // Iterate over prime polynomials.
  public class PrimePolynomials implements Iterable<T> {
    // TODO: Use a FilteredIterator.
    private final int degree;
    private final boolean primitive;
    private final boolean reverse;

    public PrimePolynomials(int degree, boolean primitive, boolean reverse) {
      this.degree = degree;
      this.primitive = primitive;
      this.reverse = reverse;
    }

    public PrimePolynomials(int degree, boolean primitive) {
      this(degree, primitive, false);
    }

    public PrimePolynomials(int degree) {
      this(degree, false, false);
    }

    private class PrimeIterator implements Iterator<T> {
      // How many bits we are working on right now.
      int bits = 1;
      // The current pattern.
      Iterator<BigInteger> pattern = new BitPattern(bits, degree - 1, reverse).iterator();
      // Next one to deliver.
      T next = null;
      T last = null;
      // Have we finished?
      boolean finished = false;

      @Override
      public boolean hasNext() {

        while (next == null && !finished) {
          // Turn the wheels.
          if (!pattern.hasNext()) {
            // Exhausted! Step bits.
            bits += 1;
            // Note when we've finished.
            finished = bits >= degree;
            // Next bit pattern.
            if (!finished) {
              pattern = new BitPattern(bits, degree - 1, reverse).iterator();
            }
          }
          if (!finished && pattern.hasNext()) {
            // Roll a polynomial of base + this pattern.
            // i.e. 2^d + ... + 1
            T p = valueOf(pattern.next().multiply(TWO), degree).or(one());
            // Is it a prime poly?
            boolean ok = !p.isReducible();
            if (ok && primitive) {
              ok &= p.isPrimitive();
            }
            if (ok) {
              next = p;
            }
          }
        }
        return next != null;
      }

      @Override
      public T next() {
        T it = hasNext() ? next : null;
        next = null;
        last = it;
        return it;
      }

      @Override
      public void remove() {
        // To change body of generated methods, choose Tools | Templates.
        throw new UnsupportedOperationException("Not supported.");
      }

      @Override
      public String toString() {
        return next != null ? next.toString() : last != null ? last.toString() : "";
      }
    }

    @Override
    public Iterator<T> iterator() {
      return new PrimeIterator();
    }
  }
  
  public static final BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);

  public static void main(String[] args) {
    FastPolynomial q = new FastPolynomial().valueOf(4,1);
    FastPolynomial d = new FastPolynomial().valueOf(1,2);
    // Should be 0
    FastPolynomial mod = new FastPolynomial(q).mod(d);
    // Should be x + 1
    FastPolynomial div = new FastPolynomial(q).divide(d);
    // Big!
    //generatePrimitivePolys(95, 2, true);

    //generateMinimalPrimitivePolys(4, 5);
    //generateMinimalPrimitivePolys(12, 5);
    //generateMinimalPrimePolys(5);
    generatePrimitivePolysUpToDegree(13, Integer.MAX_VALUE, true);
    generatePrimitivePolysUpToDegree(13, Integer.MAX_VALUE, false);
    //generateMinimalPrimePolysUpToDegree(96);
    //generatePrimitivePolys(95, 1, true);
    //generatePrimitivePolys(95, 1, false);
    //generatePrimitivePolys(96, 1, false);
    //generatePrimitivePolys(255, 1, false);
    //generatePrimitivePolys(256, 1, false);
  }

  private static void generatePrimePolysUpToDegree(int d, int max, boolean minimal) {
    for (int degree = 2; degree < d; degree++) {
      generatePrimePolys(degree, max, minimal);
    }
  }

  private static void generatePrimePolys(int degree, int count, boolean minimal) {
    System.out.println("Degree: " + degree + (minimal ? " minimal" : " maximal"));
    int seen = 0;
    for (FastPolynomial p : new FastPolynomial().new PrimePolynomials(degree, false, minimal ? false : true)) {
      // Prime Polynomials!
      System.out.println("Prime poly: " + p);
      seen += 1;
      if (seen >= count) {
        // Stop after the 1st 10 for speed - one day enumerate all.
        System.out.println("...");
        break;
      }

    }
  }

  private static void generatePrimitivePolysUpToDegree(int d, int max, boolean minimal) {
    for (int degree = 2; degree < d; degree++) {
      generatePrimitivePolys(degree, max, minimal);
    }
  }

  private static void generatePrimitivePolys(int degree, int count, boolean minimal) {
    System.out.println("Degree: " + degree + (minimal ? " minimal" : " maximal"));
    int seen = 0;
    for (FastPolynomial p : new FastPolynomial().new PrimePolynomials(degree, true, minimal ? false : true)) {
      // Prime Polynomials!
      System.out.println("Primitive poly: " + p);
      seen += 1;
      if (seen >= count) {
        // Stop after the 1st 10 for speed - one day enumerate all.
        System.out.println("...");
        break;
      }

    }
  }
}

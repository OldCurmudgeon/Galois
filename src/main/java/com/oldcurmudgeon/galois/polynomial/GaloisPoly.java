/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.galois.polynomial;

import com.oldcurmudgeon.galois.math.PolyMath;
import com.oldcurmudgeon.toolbox.twiddlers.ProcessTimer;
import com.oldcurmudgeon.toolbox.walkers.BitPattern;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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
  // A BigInteger -1.
  protected static final BigInteger MINUS1 = BigInteger.valueOf(-1);
  // Big Max Int
  protected static final BigInteger MAX = BigInteger.valueOf(Integer.MAX_VALUE);
  // Big 2
  public static final BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);

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

  public abstract boolean isEmpty();

  public abstract BigInteger asBigInteger();
  // What degree am I.

  public abstract BigInteger degree();

  // Should be static - but no way to do that in Java.
  public abstract GaloisPoly valueOf(int... powers);

  public abstract GaloisPoly valueOf(BigInteger... powers);

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
      Task task = new Task(p, ONE, o, failed);
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
      // Rejects we've seen before.
      /*
       * Observation suggests that if a 2^e+1 is found 
       * to be a factor then it is a factor more than once.
       * 
       * We therefore record the factors used to reject 
       * and try them first.
       */
      Set<BigInteger> culprits = new ConcurrentSkipListSet<>();

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
        // Walk the culprits first.
        for (BigInteger e : culprits) {
          // Check previous culprits first.
          check(e);
          // Get out if a culprit rejected.
          if (failed.get()) {
            break;
          }
        }
        // Do the rest.
        for (BigInteger e = start; e.compareTo(stop) < 0 && !failed.get(); e = e.add(BigInteger.ONE)) {
          // Skip the already known culprits - we dealt with them in the previous loop.
          if (!culprits.contains(e)) {
            // Not already checked.
            check(e);
          }
        }
        // Stop now if we failed.
        return !failed.get();
      }

      protected void check(BigInteger e) {
        // p = (x^e + 1)
        GaloisPoly p = it.valueOf(e, BigInteger.ZERO);
        if (p.mod(it).isEmpty()) {
          // Found a new culprit.
          culprits.add(e);
          // We failed - but are we the first?
          if (failed.getAndSet(true) == false) {
            // Its only prime - not primitive.
            System.out.println("Prime: " + it + " = (" + p + ")/(" + p.divide(it) + ")");
          }
        }
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

  // Filter primes out of these.
  public class PolyIterator implements Iterator<BigInteger> {
    // Degree of the poly to generate.
    private int degree;
    // Start with no bits - will step up to 1 first time around..
    private int bits = 0;
    // Invert the bits?
    private final boolean reverse;
    // The current pattern.
    Iterator<BigInteger> pattern = null;
    // Next to deliver.
    BigInteger next = null;

    public PolyIterator(int degree, boolean reverse) {
      this.degree = degree;
      this.reverse = reverse;
    }

    public PolyIterator(int degree) {
      this(degree, false);
    }

    @Override
    public boolean hasNext() {
      // Next number of bits?
      if (next == null) {
        if (pattern == null || !pattern.hasNext()) {
          // Exhausted! Step bits.
          bits += 1;
          // Next bit pattern.
          if (bits < degree) {
            pattern = new BitPattern(bits, degree - 1, reverse).iterator();
          } else {
            // Finished.
            pattern = null;
          }
        }
        if (pattern != null && pattern.hasNext()) {
          next = pattern.next();
          //System.out.println("Next poly: " + next.toString(2));
        }
      }
      return next != null;
    }

    @Override
    public BigInteger next() {
      BigInteger it = hasNext() ? next : null;
      next = null;
      return it;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }
  }

  // Iterate over prime polynomials.
  public class PrimePolynomials implements Iterable<T> {
    // Degree of the poly to generate.
    private final int degree;
    // The poly iterator.
    final Iterator<BigInteger> polys;

    public PrimePolynomials(int degree, boolean reverse) {
      this.degree = degree;
      polys = new PolyIterator(degree, reverse);
    }

    public PrimePolynomials(int degree) {
      this(degree, false);
    }

    private class PrimeIterator implements Iterator<T> {
      // Next one to deliver.
      T next = null;
      // The flpped versions - which also are prime.
      Set<BigInteger> primeFutures = new HashSet<>();

      @Override
      public boolean hasNext() {
        // Need a new next?
        while (next == null && polys.hasNext()) {
          // Roll a polynomial of base + this pattern.
          BigInteger bitPattern = polys.next();
          if (bitPattern != null) {
            // i.e. 2^d + ... + 1
            T p = valueOf(bitPattern.multiply(TWO), degree).or(one());
            boolean prime = false;
            // Check first in the futures.
            if (primeFutures.contains(bitPattern)) {
              // We know it is prime.
              prime = true;
              // Done with it - it wont come around again.
              primeFutures.remove(bitPattern);
            } else {
              // Not in the future set.
              // New pattern - Is it a prime poly?
              prime = !p.isReducible();
              // Prime or primitive - record its reverse.
              if (prime) {
                // Keep track of the reverse-pattern ones because they are prime/primitive too.
                BigInteger reversePattern = reverse(bitPattern, degree - 1);
                // Don't bother if it is a palindrome.
                if (reversePattern.compareTo(bitPattern) != 0) {
                  primeFutures.add(reversePattern);
                }
              }
            }
            // Only deliver primitives.
            if (prime) {
              next = p;
              //System.out.println("Next prime: " + next);
            }
          }
        }
        return next != null;
      }

      @Override
      public T next() {
        T it = hasNext() ? next : null;
        next = null;
        return it;
      }

      @Override
      public void remove() {
        // To change body of generated methods, choose Tools | Templates.
        throw new UnsupportedOperationException("Not supported.");
      }

      @Override
      public String toString() {
        return hasNext() ? next.toString() : "";
      }
    }

    @Override
    public Iterator<T> iterator() {
      return new PrimeIterator();
    }
  }

  // Iterate over prime polynomials.
  public class PrimitivePolynomials implements Iterable<T> {
    // TODO: Use a FilteredIterator.
    // Degree of the poly to generate.
    private final int degree;
    // The poly iterator.
    final Iterator<T> primes;

    public PrimitivePolynomials(int degree, boolean reverse) {
      this.degree = degree;
      this.primes = new PrimePolynomials(degree, reverse).iterator();
    }

    public PrimitivePolynomials(int degree) {
      this(degree, false);
    }

    private class PrimitiveIterator implements Iterator<T> {
      // Next one to deliver.
      T next = null;
      // The flpped versions - which also are prime/primitive.
      Set<T> primitiveFutures = new HashSet<>();

      @Override
      public boolean hasNext() {
        // Need a new next?
        while (next == null && primes.hasNext()) {
          // Roll a polynomial of base + this pattern.
          T p = primes.next();
          // Is it primitive.
          boolean primitive;

          // Check first in the futures.
          if (primitiveFutures.contains(p)) {
            // It's the reverse of one we've already seen.
            primitive = true;
            // Done with it - it wont come around again.
            primitiveFutures.remove(p);
          } else {
            // Primitive too?
            primitive = p.isPrimitive();
            // Prime or primitive - record its reverse.
            if (primitive) {
              // Keep track of the reverse-pattern ones because they are prime/primitive too.
              BigInteger reversePattern = reverse(p.asBigInteger(), degree - 1);
              // Don't bother if it is a palindrome.
              if (reversePattern.compareTo(p.asBigInteger()) != 0) {
                // Either primitive pr prime.
                if (primitive) {
                  primitiveFutures.add(p);
                }
              }
            }
          }
          // Only deliver primitives.
          if (primitive) {
            next = p;
            //System.out.println("Next primitive: " + next);
          }

        }
        return next != null;
      }

      @Override
      public T next() {
        T it = hasNext() ? next : null;
        next = null;
        return it;
      }

      @Override
      public void remove() {
        // To change body of generated methods, choose Tools | Templates.
        throw new UnsupportedOperationException("Not supported.");
      }

      @Override
      public String toString() {
        return hasNext() ? next.toString() : "";
      }
    }

    @Override
    public Iterator<T> iterator() {
      return new PrimitiveIterator();
    }
  }

  /**
   * From bit twiddling:
   *
   * unsigned int v; // input bits to be reversed
   * unsigned int r = v; // r will be reversed bits of v; first get LSB of v
   * int s = sizeof(v) * CHAR_BIT - 1; // extra shift needed at end
   *
   * for (v >>= 1; v; v >>= 1)
   * {
   * r <<= 1;
   * r |= v & 1;
   * s--;
   * }
   * r <<= s; // shift when v's highest bits are zero
   *
   * @param bitPattern
   * @return the bit pattern reversed.
   */
  private static BigInteger reverse(BigInteger v, int bits) {
    BigInteger r = v;
    for (v = v.shiftRight(1); !v.equals(BigInteger.ZERO); v = v.shiftRight(1)) {
      r = r.shiftLeft(1).or(v.and(BigInteger.ONE));
      bits -= 1;
    }
    return r.shiftLeft(bits - 1);
  }

  public static void main(String[] args) {
    // x^2 + 1
    FastPolynomial p = new FastPolynomial().valueOf(2, 0);
    // x + 1
    FastPolynomial q = new FastPolynomial().valueOf(1, 0);
    // (x^2 + 1) + (x + 1) = x^2 + x
    System.out.println("(" + p + ") + (" + q + ") = " + p.plus(q) + "");
    // (x^2 + 1) - (x + 1) = x^2 + x
    System.out.println("(" + p + ") - (" + q + ") = " + p.minus(q) + "");
    // (x^2 + 1) * (x + 1) = x^3 + x^2 + x + 1
    System.out.println("(" + p + ") * (" + q + ") = " + p.multiply(q) + "");
    // (x^2 + 1) / (x + 1) = x + 1
    System.out.println("(" + p + ") / (" + q + ") = " + p.divide(q) + "");
    // (x^2 + 1) % (x + 1) =
    System.out.println("(" + p + ") % (" + q + ") = " + p.mod(q) + "");
    // (x^2 + 1) %^ (2,x + 1) =
    System.out.println("(" + p + ") %^ (2," + q + ") = " + p.modPow(TWO, q) + "");
    // Should be (* -> Primitive, = -> Prime)
    // 1000011 * x6 + x + 1
    // 1001001 = x6 + x3 + 1
    // 1100001 * x6 + x5 + 1 
    // 1010111 = x6 + x4 + x2 + x + 1
    // 1011011 * x6 + x4 + x3 + x + 1
    // 1100111 * x6 + x5 + x2 + x + 1
    // 1101101 * x6 + x5 + x3 + x2 + 1
    // 1110011 * x6 + x5 + x4 + x + 1
    // 1110101 = x6 + x5 + x4 + x2 + 1
    //generatePrimitivePolys(6, Integer.MAX_VALUE, true);
    // Big!
    //generatePrimitivePolys(95, 2, true);

    //generateMinimalPrimitivePolys(4, 5);
    //generateMinimalPrimitivePolys(12, 5);
    //generateMinimalPrimePolys(5);
    /* Should see at least:
     * Degree: 2 minimal
     * Primitive poly: x^2 + x + 1
     * Degree: 3 minimal
     * Primitive poly: x^3 + x + 1
     * Primitive poly: x^3 + x^2 + 1
     * Degree: 4 minimal
     * Primitive poly: x^4 + x + 1
     * Primitive poly: x^4 + x^3 + 1
     * Reject x^4 + x^3 + x^2 + x + 1 = (x^5 + 1)/(x + 1)
     * Degree: 5 minimal
     * Primitive poly: x^5 + x^2 + 1
     * Primitive poly: x^5 + x^3 + 1
     * Primitive poly: x^5 + x^3 + x^2 + x + 1
     */
    //generatePrimitivePolys(3, Integer.MAX_VALUE, true);
    ProcessTimer t = new ProcessTimer();
    generatePrimitivePolysUpToDegree(14, Integer.MAX_VALUE, true);
    //generatePrimitivePolys(95, 1, true);
    System.out.println("Took: " + t);
    //generatePrimitivePolysUpToDegree(13, Integer.MAX_VALUE, false);
    //generateMinimalPrimePolysUpToDegree(96);
    //generatePrimitivePolys(95, 1, false);
    //generatePrimitivePolys(96, 1, false);
    //generatePrimitivePolys(255, 1, false);
    //generatePrimitivePolys(256, 1, false);
  }

  private static void generatePrimitivePolysUpToDegree(int d, int max, boolean minimal) {
    for (int degree = 2; degree <= d; degree++) {
      generatePrimitivePolys(degree, max, minimal);
    }
  }

  private static void generatePrimitivePolys(int degree, int count, boolean minimal) {
    System.out.println("Degree: " + degree + (minimal ? " minimal" : " maximal"));
    int seen = 0;
    for (FastPolynomial p : new FastPolynomial().new PrimitivePolynomials(degree, minimal ? false : true)) {
      // Prime Polynomials!
      System.out.println("Primitive: " + p);
      seen += 1;
      if (seen >= count) {
        // Stop after the 1st 10 for speed - one day enumerate all.
        System.out.println("...");
        break;
      }

    }
  }
}

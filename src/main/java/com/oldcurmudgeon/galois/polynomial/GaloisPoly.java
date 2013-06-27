/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.galois.polynomial;

import com.oldcurmudgeon.galois.math.PolyMath;
import com.oldcurmudgeon.galois.math.Primes;
import com.oldcurmudgeon.toolbox.twiddlers.ProcessTimer;
import com.oldcurmudgeon.toolbox.walkers.BitPattern;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
  abstract T xToQtoIminusXmodF(final int i);

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
    return isPrimitive(Primitivity.dividends(this.degree().intValue()));
  }

  public boolean isPrimitive(Collection<Long> dividends) {
    return Primitivity.test(this, dividends);
  }

  private static class Primitivity {
    // Tests primitivity of a GaloisPoly using the pool.
    private static boolean test(GaloisPoly p, Collection<Long> dividends) {
      // Get the totient to see if there might be factors.
      boolean failed = false;
      // Walk each dividend.
      for (Long d : dividends) {
        // Check it.
        failed = check(p, BigInteger.valueOf(d));
        if (failed) {
          // Stop now if failed.
          break;
        }
      }
      // Deliver the answer.
      return !failed;
    }

    static boolean check(GaloisPoly it, BigInteger e) {
      boolean failed = false;
      // p = (x^e + 1)
      GaloisPoly p = it.valueOf(e, BigInteger.ZERO);
      if (p.mod(it).isEmpty()) {
        failed = true;
        // Its only prime - not primitive.
        Log.Primes.log("Prime: ", it, " divides ", p);
      }
      return failed;
    }

    // Could memoize these.
    private static Collection<Long> dividends(int d) {
      long twoToTheNMinus1 = Primes.twoToTheNMinus1(d);
      List<Long> factors = Primes.primeFactors(twoToTheNMinus1);
      Set<Long> dividends = new TreeSet<>();
      for (int i = 0; i < factors.size(); i++) {
        // Add that factor and all products of that factor with all other factors.
        addProducts(factors.get(i), factors, i + 1, dividends, twoToTheNMinus1);
      }
      return dividends;
    }

    private static void addProducts(Long f, List<Long> factors, int start, Set<Long> dividends, long limit) {
      // Stop at limit.
      if (f.compareTo(limit) < 0) {
        dividends.add(f);
        // And all multiples.
        for (int j = start; j < factors.size(); j++) {
          addProducts(f * factors.get(j), factors, j + 1, dividends, limit);
        }
      }
    }

  }

  /**
   * Tests the reducibility of the polynomial
   */
  public boolean isPrime() {
    return getReducibility() == Reducibility.IRREDUCIBLE;
  }

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
      T b = xToQtoIminusXmodF(i);
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
      T b = xToQtoIminusXmodF(i);
      T g = gcd(b);
      if (!g.equals(one())) {
        return Reducibility.REDUCIBLE;
      }
    }

    T g = xToQtoIminusXmodF(degree);
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
            boolean prime;
            // Check first in the futures.
            if (primeFutures.contains(bitPattern)) {
              // We know it is prime.
              prime = true;
              // Done with it - it wont come around again.
              primeFutures.remove(bitPattern);
            } else {
              // Not in the future set.
              // New pattern - Is it a prime poly?
              prime = p.isPrime();
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
        return next != null ? next.toString() : "";
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
    // Hang on to the possible dividends.
    final Collection<Long> dividends;
    // How many!
    int primeCount = 0;
    int primitiveCount = 0;

    public PrimitivePolynomials(int degree, boolean reverse) {
      this.degree = degree;
      dividends = Primitivity.dividends(degree);
      this.primes = new PrimePolynomials(degree, reverse).iterator();
    }

    public PrimitivePolynomials(int degree) {
      this(degree, false);
    }

    private class PrimitiveIterator implements Iterator<T> {
      // The last one we delivered.
      T prev = null;
      // Next one to deliver.
      T next = null;
      // The flpped versions - which also are prime/primitive.
      Set<T> primitiveFutures = new HashSet<>();

      @Override
      public boolean hasNext() {
        // Need a new next?
        while (next == null && primes.hasNext()) {
          // Copunt the primes.
          primeCount += 1;
          // Roll a prime polynomial.
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
            primitive = p.isPrimitive(dividends);
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
            primitiveCount += 1;
            //System.out.println("Next primitive: " + next);
          }

        }
        return next != null;
      }

      @Override
      public T next() {
        // Keep track of prev.
        prev = next;
        // Get the next.
        T it = hasNext() ? next : null;
        // Given that one now.
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
        return next != null ? next.toString() : "";
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

  // Rudimentary logging.
  enum Log {
    // By default - all log.
    // Construct with (false) not to log.
    Tests,
    Degrees,
    Primes,
    Primitives,
    Counts,
    Times;
    // Should we log this level.
    private final boolean log;

    Log() {
      this(true);
    }

    Log(boolean log) {
      this.log = log;
    }

    public void log(Object... l) {
      if (log) {
        StringBuilder s = new StringBuilder();
        for (Object o : l) {
          s.append(o.toString());
        }
        System.out.println(s.toString());
      }
    }

  }
  
  public static void main(String[] args) {
    // x^2 + 1
    FastPolynomial p = new FastPolynomial().valueOf(2, 0);
    // x + 1
    FastPolynomial q = new FastPolynomial().valueOf(1, 0);
    // (x^2 + 1) + (x + 1) = x^2 + x
    Log.Tests.log("(", p, ") + (", q, ") = ", p.plus(q));
    // (x^2 + 1) - (x + 1) = x^2 + x
    Log.Tests.log("(" + p + ") - (" + q + ") = " + p.minus(q), "");
    // (x^2 + 1) * (x^ + 1) = x^4 + 1
    Log.Tests.log("(" + p + ") * (" + p + ") = " + p.multiply(p), "");
    // (x^2 + 1) * (x + 1) = x^3 + x^2 + x + 1
    Log.Tests.log("(" + p + ") * (" + q + ") = " + p.multiply(q), "");
    // (x^2 + 1) / (x + 1) = x + 1
    Log.Tests.log("(" + p + ") / (" + q + ") = " + p.divide(q), "");
    // (x^2 + 1) % (x + 1) =
    Log.Tests.log("(" + p + ") % (" + q + ") = " + p.mod(q), "");
    // (x^2 + 1) %^ (2,x + 1) =
    Log.Tests.log("(" + p + ") %^ (2," + q + ") = " + p.modPow(TWO, q), "");
    // Test a specific poly.
    //testPoly(new FastPolynomial().valueOf(10, 4, 0));
    // Test a whole degree.
    //testDegree(6);
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
    // While testing I want all factors printed.
    //Primitivity.findAllFactors = true;
    //generatePrimitivePolys(4, Integer.MAX_VALUE, true);
    ProcessTimer t = new ProcessTimer();
    generatePrimitivePolysUpToDegree(14, Integer.MAX_VALUE, true);
    //generatePrimitivePolysUpToDegree(14, Integer.MAX_VALUE, true);
    //generatePrimitivePolys(95, 1, true);
    Log.Times.log("Took: ", t);
    //generatePrimitivePolysUpToDegree(13, Integer.MAX_VALUE, false);
    //generateMinimalPrimePolysUpToDegree(96);
    //generatePrimitivePolys(95, 1, false);
    //generatePrimitivePolys(96, 1, false);
    //generatePrimitivePolys(255, 1, false);
    //generatePrimitivePolys(256, 1, false);
  }

  private static void testPoly(FastPolynomial p) {
    Log.Tests.log("Poly: ", p, " Prime: ", p.isPrime(), " Primitive: ", p.isPrimitive());
  }

  private static void testDegree(int d) {
    generatePrimitivePolys(d, Integer.MAX_VALUE, true);
  }

  private static void generatePrimitivePolysUpToDegree(int d, int max, boolean minimal) {
    for (int degree = 2; degree <= d; degree++) {
      generatePrimitivePolys(degree, max, minimal);
    }
  }

  private static void generatePrimitivePolys(int degree, int count, boolean minimal) {
    long twoPowDegreeMinus1 = Primes.twoToTheNMinus1(degree);
    int seen = 0;
    FastPolynomial.PrimitivePolynomials primitivePolynomials = new FastPolynomial().new PrimitivePolynomials(degree, minimal ? false : true);
    Log.Degrees.log("Degree: ", degree //, (minimal ? " minimal" : " maximal")
            , " Factors of ", twoPowDegreeMinus1, ": ", Primes.mersenneFactors(degree)
            , " Dividends: ", primitivePolynomials.dividends);
    for (FastPolynomial p : primitivePolynomials) {
      // Prime Polynomials!
      Log.Primitives.log("Primitive: ", p);
      seen += 1;
      if (seen >= count) {
        // Stop after the 1st 10 for speed - one day enumerate all.
        Log.Primitives.log("...");
        break;
      }
    }
    Log.Counts.log("Degree: ", degree,
                   " Primes: ", primitivePolynomials.primeCount, 
                   " Primitives: ", primitivePolynomials.primitiveCount, 
                   " Möbius: ", Primes.möbius(degree), 
                   " Totient: ", Primes.totient(degree));
  }

}

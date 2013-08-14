package com.oldcurmudgeon.galois;

import com.oldcurmudgeon.galois.polynomial.FastPolynomial;
import com.oldcurmudgeon.galois.polynomial.GaloisPoly;
import com.oldcurmudgeon.galois.polynomial.Primitives;
import com.oldcurmudgeon.galois.unique.LFSR;
import java.math.BigInteger;

public class Test {
  // n = digits, k = weight, m = position.
  public static BigInteger combinadic(int n, int k, BigInteger m) {
    BigInteger out = BigInteger.ZERO;
    for (; n > 0; n--) {
      BigInteger y = nChooseK(n - 1, k);
      if (m.compareTo(y) >= 0) {
        m = m.subtract(y);
        out = out.setBit(n - 1);
        k -= 1;
      }
    }
    return out;
  }

  // Algorithm borrowed (and tweaked) from: http://stackoverflow.com/a/15302448/823393
  public static BigInteger nChooseK(int n, int k) {
    if (k > n) {
      return BigInteger.ZERO;
    }
    if (k <= 0 || k == n) {
      return BigInteger.ONE;
    }
    // ( n * ( nChooseK(n-1,k-1) ) ) / k;
    return BigInteger.valueOf(n).multiply(nChooseK(n - 1, k - 1)).divide(BigInteger.valueOf(k));
  }

  public void test() {
    int n, k;
    // Proves nChooseK is working.
    for (n = 1; n <= 15; n++) {
      System.out.print("n = " + n + " : ");
      for (k = 1; k <= 10; k++) {
        System.out.print(nChooseK(n, k) + ",");
      }
      System.out.println();
    }
    n = 95;
    k = 15;
    // There will be nChoosek(n,k) values.
    BigInteger limit = nChooseK(n, k);
    // And therefore I need an LFSR with that bit length.
    int bitLength = limit.bitLength();
    // Build me a big-enough lfsr.
    GaloisPoly poly = new FastPolynomial().valueOf(Primitives.get(bitLength));
    LFSR lfsr = new LFSR(poly);
    System.out.println("Limit:"+limit+" bits "+bitLength);
    // Test the first 1000.
    int count = 1000;
    for (BigInteger i : lfsr) {
      System.out.print(i.toString(2) + ",");
      // Since the LFSR has been chosen to be too big it can generated too big numbers.
      BigInteger comb = combinadic(n, k, i);
      // combinadic will fill all bits in this case.
      if (comb.bitCount() == k) {
        // Its a goodn.
        System.out.println(comb.toString(2));
        if (--count <= 0) {
          break;
        }
      } else {
        System.out.println("Rejected!");
      }
    }
  }

  public static void main(String args[]) {
    try {
      new Test().test();
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

}

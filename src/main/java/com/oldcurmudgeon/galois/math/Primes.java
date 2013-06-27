/*
 * Copyright 2013 OldCurmudgeon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oldcurmudgeon.galois.math;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Find factors of ints.
 *
 * @author OldCurmudgeon
 */
public class Primes {
  public static List<Long> primeFactors(long n) {
    List<Long> factors = new ArrayList<>();
    for (long i = 2; i <= n / i; i++) {
      while (n % i == 0) {
        factors.add(i);
        n /= i;
      }
    }
    if (n > 1) {
      factors.add(n);
    }
    return factors;
  }
  // No point in factoring these - note that these are the n of 2^n-1.
  static final Set<Integer> MersennePrimes = new HashSet<>(
          Arrays.asList(2, 3, 5, 7, 13, 17, 19, 31, 61, 89, 107, 127,
                        521, 607, 1279, 2203, 2281, 3217, 4253,
                        4423, 9689, 9941, 11213, 19937, 21701,
                        23209, 44497, 86243, 110503, 132049, 216091,
                        756839, 859433, 1257787, 1398269, 2976221,
                        3021377, 6972593, 13466917, 20996011,
                        24036583, 25964951, 30402457, 32582657,
                        37156667, 42643801, 43112609, 57885161));

  // Returns factors of 2^n-1
  public static List<Long> mersenneFactors(int n) {
    if (!MersennePrimes.contains(n)) {
      return primeFactors(twoToTheNMinus1(n));
    } else {
      // No factors of the primes.
      return Collections.EMPTY_LIST;
    }
  }

  public static long twoToTheNMinus1(int n) {
    return (long) Math.pow(2, n) - 1;
  }
  // 10,000 primes - from http://primes.utm.edu/lists/small/10000.txt
  private static final ArrayList<Integer> SomePrimes = new ArrayList<>();
  private static final String PrimesFileName = "10000Primes.txt";

  static {
    try (InputStream in = Primes.class.getResourceAsStream(PrimesFileName);
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr)) {
      for (String s; br.ready() && (s = br.readLine()) != null;) {
        // Split into commas.
        String[] ps = s.split(",");
        for (String p : ps) {
          if (p.length() > 0) {
            SomePrimes.add(Integer.valueOf(p));
          }
        }
      }
    } catch (IOException ex) {
      System.err.println("Loading pf primes failed.");
      ex.printStackTrace(System.err);
    }
    //System.out.println("Primes: "+SomePrimes.size());
  }

  // Returns a list of primes in the range requested.
  // NB - from and to need not be prime.
  public static List<Integer> primes(int from, int to) {
    int whereFrom = whereNextPrime(from, true);
    int whereTo = whereNextPrime(to, false);
    return SomePrimes.subList(whereFrom, whereTo);
  }

  public static int whereNextPrime(int from, boolean higher) {
    for (int i = 0; i < SomePrimes.size(); i++) {
      if (SomePrimes.get(i) > from) {
        return higher ? i - 1 : i;
      }
    }
    // Not sure what to do here.
    // We were given a from which is beyond my range.
    // Currently doing something that should throw an exception.
    return -1;
  }

  // Euler phi or totient.
  public static long totient(long n) {
    long t = n;
    // Only do unique ones.
    Set<Long> done = new HashSet<> ();
    for (Long f : primeFactors(n)) {
      if ( !done.contains(f)) {
        // t * ( 1 - (1/f) ) = (t * (f-1)) / f - Avoiding doubles.
        t = (t * (f - 1)) / f;
        done.add(f);
      }
    }
    return t;
  }

  public static long möbius(long d) {
    long m = Math.round(Math.floor(Math.pow(2, d)));
    for (Long f : primeFactors(d)) {
      // m -= 2^f
      if (f != d) {
        m -= Math.pow(2, f);
      }
    }
    m += 2;
    return m / d;
  }

  public static void main(String[] args) {
    int[] ints = new int[]{527, 1143, 1581, 2635, 2667, 7905, 8001};
    for (int i : ints) {
      System.out.println("Factors of " + i + "=" + primeFactors(i));
    }

    System.out.println("möbius(" + 10 + ") = " + möbius(10));

    System.out.println("totient(" + 33 + ") = " + totient(33) + " should be 20.");
    System.out.println("totient(" + 93 + ") = " + totient(93) + " should be 60.");
    System.out.println("totient(" + 341 + ") = " + totient(341) + " should be 300.");
    System.out.println("totient(" + 1023 + ") = " + totient(1023) + " should be 600.");
  }
}

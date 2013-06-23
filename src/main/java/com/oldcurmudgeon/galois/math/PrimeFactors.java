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

import java.util.ArrayList;
import java.util.List;

/**
 * Find prime factors of ints.
 * 
 * @author OldCurmudgeon
 */
public class PrimeFactors {
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

  public static void main(String[] args) {
    System.out.println("Primefactors of 44");
    for (Long i : primeFactors(44)) {
      System.out.println(i);
    }
    System.out.println("Primefactors of 3");
    for (Long i : primeFactors(3)) {
      System.out.println(i);
    }
    System.out.println("Primefactors of 32");
    for (Long i : primeFactors(32)) {
      System.out.println(i);
    }
  }
}

/*
 * Copyright 2013 OldCurmudgeon.
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

import java.math.BigInteger;

/**
 * Pascal's Triangle
 *
 * @author OldCurmudgeon
 */
public class PascalsTriangle {
  public static void main(String[] args) {
    final int n = 100;
    BigInteger[] row = new BigInteger[0];

    for (int i = 0; i < n; i++) {
      // Calculate next row
      row = nextRow(row);
      
      // Output row
      System.out.print(row.length + ":");
      System.out.print("["+BigInteger.valueOf(2).pow(row.length) + "]:");
      for (int j = 0; j < row.length; j++) {
        System.out.print(row[j] + ",");
      }
      System.out.println();
    }
  }

  public static BigInteger[] nextRow(BigInteger[] previous) {
    // Row is 1 element longer than previous row
    BigInteger[] row = new BigInteger[previous.length + 1];

    // First and last numbers in row are always 1
    row[0] = BigInteger.ONE;
    row[row.length - 1] = BigInteger.ONE;

    // The rest of the row can be 
    // calculated based on previous row
    for (int i = 1; i < row.length - 1; i++) {
      row[i] = previous[i - 1].add(previous[i]);
    }

    return row;
  }
}
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
 * Bits implementation using BigIntegers.
 * 
 * @author OldCurmudgeon.
 */
public class BigBits extends Bits {
  // The actual bits.
  private final BigInteger bits;
  
  public BigBits(BigInteger bits) {
    this.bits = bits;
  }

  @Override
  protected void getNextAndSetIndex() {
    // The bits to deliver.
    next = bits;
    // The power of the lowest bit.
    i = BigInteger.ZERO;
  }
  
}

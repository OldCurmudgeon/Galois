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
 * A packet to define a section of bits that fit into a BigInteger.
 * 
 * Both the object and the index are big.
 *
 * @author OldCurmudgeon.
 */
public class Big implements Indexed<BigInteger,BigInteger> {
  final BigInteger index;
  final BigInteger value;

  public Big(BigInteger index, BigInteger value) {
    this.index = index;
    this.value = value;
  }

  public Big( BigInteger value) {
    this.index = BigInteger.ZERO;
    this.value = value;
  }

  @Override
  public BigInteger index() {
    return index;
  }

  @Override
  public BigInteger value() {
    return value;
  }

  @Override
  public String toString() {
    return ("[" + index.toString() + "]:" + value.toString(16));
  }

}

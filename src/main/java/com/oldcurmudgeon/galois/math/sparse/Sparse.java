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
package com.oldcurmudgeon.galois.math.sparse;

/**
 * Something that has an index, a length and a value.
 * 
 * We can then string these together leaving gaps between
 * them to allow for sparse functions to perform.
 * 
 * Big implements Sparse<BigInteger,BigInteger> i.e. it is
 * a BigInteger with BigInteger offset and length.
 * 
 * Length will rarely get bigger than int. I use I here because
 * that makes the maths much easier at little cost.
 * 
 * @author OldCurmudgeon.
 */
public interface Sparse<T, I extends Number> {
  // Return the current bit index.
  public I index();
  // Return the bits length.
  public I length();
  // Return the current value.
  public T value();
}

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

import java.util.Iterator;

/**
 * Defines a stream of bits to perform maths over.
 * 
 * It should be possible to step through two streams of bits
 * at once and do the maths on them.
 * 
 * Underneath, it should be possible to ask the stream to skip
 * uninteresting sequences of bits such as all zeros.
 * 
 * Going forward I hope to perform the actual math using lambdas
 * and closures but for now we will merely iterate.
 * 
 * T is the type of each part.
 * 
 * @author OldCurmudgeon
 */
public class Bits<T extends Number, I extends Number> implements IndexedIterator<T,I> {

  @Override
  public boolean hasNext() {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public T next() {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void remove() {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public I getIndex() {
    // To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }

}

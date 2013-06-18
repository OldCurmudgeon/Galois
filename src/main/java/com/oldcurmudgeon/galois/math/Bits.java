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
 * I is the type of the index.
 *
 * @author OldCurmudgeon
 */
public abstract class Bits<T extends Number, I extends Number>
        implements IndexedIterator<T, I> {
  // The last returned - populated by next.
  T prev = null;
  // The next to return - populate in hasNext please.
  T next = null;
  // The indext it is at - populate in hasNext please.
  I i = null;

  // hasNext must prime both next and i.
  @Override
  public abstract boolean hasNext();

  @Override
  public T next() {
    // Standard pattern for next.
    if (hasNext()) {
      prev = next;
      next = null;
      return prev;
    } else {
      return null;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public I i() {
    return i;
  }
}

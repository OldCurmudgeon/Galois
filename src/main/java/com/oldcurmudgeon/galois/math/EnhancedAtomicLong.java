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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Add setIf method to set if new value compares to old.
 * 
 * @author OldCurmudgeon
 */
public class EnhancedAtomicLong extends AtomicLong {
  // Construct with a value.
  public EnhancedAtomicLong ( long value ) {
    super(value);
  }
  
  // Construct with a value.
  public EnhancedAtomicLong () {
    this(0);
  }
  
  public enum Op {
    gt {
      @Override
      boolean tst(long cur, long upd) {
        return upd > cur;
      }
    },
    ge {
      @Override
      boolean tst(long cur, long upd) {
        return upd >= cur;
      }
    },
    lt {
      @Override
      boolean tst(long cur, long upd) {
        return upd < cur;
      }
    },
    le {
      @Override
      boolean tst(long cur, long upd) {
        return upd <= cur;
      }
    },
    ne {
      @Override
      boolean tst(long cur, long upd) {
        return upd != cur;
      }
    };
    // NB: eq is compareAndSet - direct.
    // Do the test.
    abstract boolean tst(long cur, long upd);
  }

  public boolean setIf(Op op, long upd) {
    while (true) {
      long cur = get();

      if (op.tst(cur, upd)) {
        if (compareAndSet(cur, upd)) {
          return true;
        }
      } else {
        return false;
      }
    }
  }
}

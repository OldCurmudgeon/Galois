/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.galois.math;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * @author Expression project.property.user is undefined on line 11, column 14 in Templates/Classes/Class.java.
 */
public class HugeBits extends Bits {
  // The actual bits.
  private final TreeMap<BigInteger, BigInteger> bits = new TreeMap<>();
  // The iterator over them.
  private final Iterator<BigInteger> walker;

  public static class Big {
    final BigInteger index;
    final BigInteger value;

    public Big(BigInteger index, BigInteger value) {
      this.index = index;
      this.value = value;
    }
  }

  public HugeBits(Big... bigs) {
    for (Big i : bigs) {
      bits.put(i.index, i.value);
    }
    walker = bits.navigableKeySet().iterator();
  }

  @Override
  protected void getNextAndSetIndex() {
    // Next index.
    i = walker.next();
    // Next value.
    next = bits.get(i);
  }
}

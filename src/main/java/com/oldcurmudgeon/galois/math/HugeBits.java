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

  // A packet to define a section of bits that fit into a BigInteger.
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
    if ( walker.hasNext() ) {
      // Next index.
      i = walker.next();
      // Next value.
      next = bits.get(i);
    } else {
      // Finished!
      i = null;
      next = null;
    }
  }
}

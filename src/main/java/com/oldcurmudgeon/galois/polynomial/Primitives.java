/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.galois.polynomial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author Paul Caswell
 */
public class Primitives {
  // 213 Primitives - from http://www.jjj.de/mathdata/rand-primpoly.txt
  private static final ArrayList<int[]> SomePrimitives = new ArrayList<>();
  private static final String PrimitivesFileName = "213Primitives.txt";

  static {
    try (InputStream in = Primitives.class.getResourceAsStream(PrimitivesFileName);
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr)) {
      for (String s; br.ready() && (s = br.readLine()) != null;) {
        // Split into commas.
        String[] ps = s.split(",");
        int[] p = new int[ps.length];
        for ( int i = 0; i < ps.length; i++ ) {
          p[i] = Integer.valueOf(ps[i]);
        }
        SomePrimitives.add(p);
      }
    } catch (IOException ex) {
      System.err.println("Loading of primiyives failed.");
      ex.printStackTrace(System.err);
    }
    //System.out.println("SomePrimitives: "+SomePrimitives.size());
  }
  
  public static int[] get(int n) {
    return SomePrimitives.get(n);
  }
  
}

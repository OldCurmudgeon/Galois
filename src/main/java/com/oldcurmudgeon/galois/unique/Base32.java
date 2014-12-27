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
package com.oldcurmudgeon.galois.unique;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Parse and format base 32 numbers.
 *
 * Some of this may apply to almost any format but there's much that won't such
 * as allowing both uppercase and lowercase forms of each digit.
 *
 * See: http://en.wikipedia.org/wiki/Base32
 */
public class Base32 {

    public interface BigFormatter {

        public String format(BigInteger v);
    }

    public interface BigParser {

        public BigInteger parse(String s);

        public boolean good(String s);
    }

    /**
     * The character sets.
     */
    public enum Formatter implements BigFormatter, BigParser {

        /**
         * Mimics BigInteger
         */
        Ordinary(null),
        /**
         * Like Hex but up to V
         */
        Base32Hex("0123456789ABCDEFGHIJKLMNOPQRSTUV"),
        /**
         * Common alternative - avoids O/0, i/1 etc.
         */
        Base32("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"),
        /**
         * Avoids vowels (and therefore real words)
         */
        ZBase32("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"),
        /**
         * Avoids o/0 confusion.
         */
        Crockford("0123456789ABCDEFGHJKMNPQRSTVWXYZ", "O0", "o0", "L1", "l1", "I1", "i1");

        // The formatter/parser.
        public final Base32 format;

        Formatter(String charSet, String... extras) {
            this.format = new Base32(charSet, extras);
        }

        // Utilities.
        @Override
        public String format(BigInteger i) {
            return format.format(i);
        }

        @Override
        public BigInteger parse(String s) {
            return format.parse(s);
        }

        @Override
        public boolean good(String s) {
            return format.good(s);
        }

    }
    // Invalid character.
    private static final int Invalid = -1;
    // The radix.
    private static final int Radix = 32;
    // The bits per digit - could use (int) (Math.log(radix) / Math.log(2))
    private static final int BitsPerDigit = 5;
    // The bits per byte.
    private static final int BitsPerByte = 8;
    // Translation table for each code.
    private final char[] formatTable;
    // Translation table for each character.
    private final int[] parseTable;

    // Constructor - Probably should be private but why restrict the user.
    private Base32() {
        // Empty tables makes us match BigInteger format so no formatting/parsing is required.
        formatTable = null;
        parseTable = null;
    }

    // Constructor with character set and optional extras :).
    private Base32(String charSet, String... extras) {
        // Check the character set against the radix.
        if (charSet.length() != Radix) {
            throw new NumberFormatException("Invalid character set - must be 32 long " + charSet);
        }
        // Build the format table.
        formatTable = buildFormatTable(charSet);
        // And the parse table.
        parseTable = buildParseTable(charSet, extras);
    }

    // Build a format table from the character set.
    private char[] buildFormatTable(String characterSet) {
        if (characterSet != null) {
            // Start clear.
            char[] table = new char[Radix];
            // Put each character from the character set in.
            for (int i = 0; i < Radix; i++) {
                table[i] = characterSet.charAt(i);
            }
            return table;
        } else {
            // No formatting for a null charset.
            return null;
        }
    }

    private int[] buildParseTable(String characterSet, String... extras) {
        // Handle all characters up to and including 'z'.
        int[] table = new int['z' + 1];
        // By default invalid character.
        Arrays.fill(table, Invalid);
        // Lowercase and uppercase versions.
        String lc = characterSet.toLowerCase();
        String uc = characterSet.toUpperCase();
        // Walk through the character set.
        for (int i = 0; i < Radix; i++) {
            char l = lc.charAt(i);
            char u = uc.charAt(i);
            // Something wrong if we've already filled this one in.
            if (table[l] == Invalid && table[u] == Invalid) {
                // Put both lowercase and uppercase forms in the table.
                table[l] = i;
                table[u] = i;
            } else {
                // Failed.
                throw new NumberFormatException("Invalid character set - duplicate found at position " + i);
            }
        }
        // Add extras.
        for (String pair : extras) {
            // Each Must be length 2.
            if (pair.length() == 2) {
                // From
                int f = pair.charAt(1);
                // To
                int t = pair.charAt(0);
                // Something wrong if we've already filled this one in or we are copying from one that is not filled in.
                if (table[f] != Invalid && table[t] == Invalid) {
                    // EG "O0" means a capital oh should be treated as a zero.
                    table[t] = table[f];
                } else {
                    // Failed.
                    throw new NumberFormatException("Invalid character set extra - copying from " + f + " to " + t);
                }
            } else {
                // Failed.
                throw new NumberFormatException("Invalid extra \"" + pair + "\" - should be 2 characters wide.");
            }

        }
        return table;
    }

    // Format a BigInteger.
    private String format(BigInteger n) {
        // Get its raw Radix32 string - in uppercase.
        String formatted = n.toString(Radix).toUpperCase();
        // Further formatting through the format table?
        if (formatTable != null) {
            // Translate it.
            char[] translated = new char[formatted.length()];
            for (int i = 0; i < formatted.length(); i++) {
                // Use Character.digit to decode the digit value.
                int d = Character.digit(formatted.charAt(i), Radix);
                // Translate to that.
                translated[i] = formatTable[d];
            }
            formatted = new String(translated);
        }
        return formatted;
    }

    // Parse a string.
    private BigInteger parse(String s) {
        BigInteger big;
        // Pass it through the parse table if present.
        if (parseTable != null) {
            // Digits in the number.
            int digits = s.length();
            // Total bits (+1 to avoid sign bit).
            int bits = digits * BitsPerDigit + 1;
            // Number of bytes.
            int bytes = (bits + BitsPerByte - 1) / BitsPerByte;
            // Bias bits to slide to the right to get the bottom bit rightmost (+1 to avoid sign bit).
            int bias = (bytes * BitsPerByte) - bits + 1;
            // Make my array.
            byte[] parsed = new byte[bytes];
            // Walk the string.
            for (int i = 0, bit = bias; i < digits; i++, bit += BitsPerDigit) {
                // The character.
                char c = s.charAt(i);
                // Must be in the parse table.
                if (c < parseTable.length) {
                    // Roll in each digit value into the correct bits.
                    int n = parseTable[c];
                    // Special cases.
                    switch (n) {
                        case 0:
                            // Nothing to do.
                            break;

                        default:
                            // How far to shift it to line up with "bit"
                            int shift = (BitsPerByte - BitsPerDigit - (bit % BitsPerByte));
                            // Sorry about the name.
                            int bite = bit / BitsPerByte;
                            // +ve shift is left into this byte.
                            if (shift >= 0) {
                                // Slide left only.
                                parsed[bite] |= n << shift;
                            } else {
                                // Split across this byte and the next.
                                parsed[bite] |= n >>> -shift;
                                // Slide right.
                                parsed[bite + 1] |= n << (BitsPerByte + shift);
                            }
                            break;

                        case Invalid:
                            // Must be mapped to something.
                            throw new NumberFormatException("Invalid character '" + c + "' at position " + i);
                    }
                } else {
                    // Failed.
                    throw new NumberFormatException("Invalid character '" + c + "' at position " + i);
                }
            }
            // Grow the biginteger out of the byte array.
            big = new BigInteger(parsed);
        } else {
            // No parsing - it's ordinary.
            big = new BigInteger(s, Radix);
        }
        return big;
    }

    // Check a string.
    private boolean good(String s) {
        boolean good = true;
        // Check each character.
        for (int i = 0; i < s.length() && good; i++) {
            // The character.
            char c = s.charAt(i);
            if (parseTable != null) {
                if (c < parseTable.length) {
                    // Must be a valid character.
                    good = parseTable[c] != Invalid;
                } else {
                    // Out of range of the parse table.
                    good = false;
                }
            } else {
                // Use Character.digit - returns -1 if not valid.
                good = Character.digit(c, Radix) != Invalid;
            }
        }
        return good;
    }

    public static void main(String args[]) {
        Test.main(args);
    }

}

class Test {

    // For testing only.
    private static Random r = new Random();
    /*
     * A 95 bit number fits in a 12 byte binary with a bit to spare (sign bit).
     * A 95 bit number formats in base 32 to 19 digits exactly.
     *
     * Other numbers of this type:
     * 15 bits 2 bytes 3 digits
     * 55 bits 7 bytes 11 digits
     * 95 bits 12 bytes 19 digits
     * 135 bits 17 bytes 27 digits
     * 175 bits 22 bytes 35 digits
     * 215 bits 27 bytes 43 digits
     * 255 bits 32 bytes 51 digits
     */
    private static final int testBits = 95;

    public static void main(String args[]) {
        test(new BigInteger("10"));
        test(new BigInteger("32"));
        test(new BigInteger("100"));
        BigInteger big = BigInteger.valueOf(0);
        for (int i = 0; i < 1000; i++, big = big.add(BigInteger.ONE)) {
            test(big);
        }
        for (int i = 0; i < 1000; i++) {
            test(new BigInteger(testBits, r));
        }
        testCrockfords();
    }

    private static void test(BigInteger i) {
        for (Base32.Formatter f : Base32.Formatter.values()) {
            test(i, f);
        }
    }

    private static void test(BigInteger i, Base32.Formatter f) {
        test(i, f, f.format(i), f.name());
    }

    private static void test(BigInteger i, Base32.Formatter f, String formatted, String name) {
        BigInteger parsed = f.parse(formatted);
        boolean ok = parsed.equals(i) && f.good(formatted);
        //if (!ok) {
        // For debug - so we can trace the issue.
        BigInteger reParsed = f.parse(formatted);
        boolean good = f.good(formatted);
        System.out.println(i + " = " + f.format(i) + " in " + name + (ok ? " Ok" : " BAD!"));
        if (!ok) {
            System.out.println(reParsed + " != " + i);
        }
        //}
    }

    private static void testCrockfords() {
        /// Crockford uses extras.
        for (int i = 0; i < 100; i++) {
            BigInteger b = BigInteger.valueOf(i);
            String formatted = Base32.Formatter.Crockford.format(b)
                    .replace('0', 'O')
                    .replace('1', 'l');
            test(b, Base32.Formatter.Crockford, formatted, "Crockfords Test");
        }
        for (int i = 0; i < 1000; i++) {
            BigInteger b = new BigInteger(testBits, r);
            String formatted = Base32.Formatter.Crockford.format(b)
                    .replace('0', 'O')
                    .replace('1', 'l');
            test(b, Base32.Formatter.Crockford, formatted, "Crockfords Test");
        }
    }

}

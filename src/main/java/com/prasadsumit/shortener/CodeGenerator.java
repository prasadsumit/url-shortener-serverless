package com.prasadsumit.shortener;

import java.security.SecureRandom;

final class CodeGenerator {
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();
    private static final int LENGTH = 7;

    private CodeGenerator() {}

    // 62^7 is about 3.5 trillion combinations. Collisions are still handled
    // by the conditional write-in CreateHandler.
    static String next() {
        char[] out = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            out[i] = ALPHABET[RNG.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}

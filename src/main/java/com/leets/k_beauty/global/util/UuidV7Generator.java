package com.leets.k_beauty.global.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class UuidV7Generator {

    private UuidV7Generator() {
    }

    public static UUID generate() {
        long timestampMillis = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
        long randomA = ThreadLocalRandom.current().nextLong() & 0xFFFL;
        long randomB = ThreadLocalRandom.current().nextLong() & 0x3FFFFFFFFFFFFFFFL;

        long mostSignificantBits = (timestampMillis << 16) | 0x7000L | randomA;
        long leastSignificantBits = 0x8000000000000000L | randomB;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}

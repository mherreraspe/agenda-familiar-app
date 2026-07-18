package com.obusystem.agendafamiliar.agenda.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7 {
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private UuidV7() { }

    public static UUID nuevo() {
        long tiempo = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
        long msb = (tiempo << 16) | 0x7000L | ALEATORIO.nextInt(0x1000);
        long lsb = ALEATORIO.nextLong();
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}

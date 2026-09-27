package com.inacap.fyagobras.domain.user;

import java.security.SecureRandom;

public final class GeneradorPassword {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorPassword() {}

    public static String generar(int largo) {
        var sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
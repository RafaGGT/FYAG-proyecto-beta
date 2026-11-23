package com.inacap.fyagobras.domain.user;

import java.security.SecureRandom;

// Clase GeneradorPassword: implementa la funcionalidad del dominio.
public final class GeneradorPassword {
    // Alfabeto limitado para evitar caracteres ambiguos y facilitar su lectura.
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    // Generador criptográfico utilizado para producir contraseñas impredecibles.
    private static final SecureRandom RANDOM = new SecureRandom();

    // Impide crear instancias: esta clase solo expone utilidades estáticas.
    private GeneradorPassword() {}

    // Genera una contraseña aleatoria con la cantidad de caracteres indicada.
    public static String generar(int largo) {
        // Crea un StringBuilder con la capacidad inicial igual al largo solicitado.
        var sb = new StringBuilder(largo);
        // Repite la selección aleatoria hasta completar el largo solicitado.
        for (int i = 0; i < largo; i++) {
            // Selecciona un carácter aleatorio del alfabeto definido y lo agrega al StringBuilder.
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        // Convierte el StringBuilder a String y lo retorna como la contraseña generada.
        return sb.toString();
    }
}

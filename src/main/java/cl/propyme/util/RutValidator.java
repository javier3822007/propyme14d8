/*
 * ProPyme Transparente — Sistema de declaración tributaria 14D N°8
 * Copyright (C) 2026 Javier Ignacio Aguilar G. <javier.aguilar382@protonmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License v3 as published by the
 * Free Software Foundation. See the LICENSE file for the full text.
 *
 * Distributed WITHOUT ANY WARRANTY. See https://www.gnu.org/licenses/agpl-3.0.html
 */
package cl.propyme.util;

/**
 * RutValidator — Validador de RUT chileno con algoritmo Módulo 11.
 *
 * El RUT chileno tiene formato XXXXXXXX-Y donde Y es el dígito verificador
 * (puede ser un dígito 0-9 o la letra K).
 *
 * Algoritmo del DV (estándar oficial chileno):
 *   1. Tomar el cuerpo del RUT (sin el DV).
 *   2. Multiplicar cada dígito de derecha a izquierda por: 2,3,4,5,6,7,2,3,...
 *   3. Sumar todos los productos.
 *   4. Calcular: 11 - (suma mod 11).
 *   5. Si resultado = 11 → DV = "0".
 *      Si resultado = 10 → DV = "K".
 *      En cualquier otro caso → DV = número.
 *
 * Esta clase NO modifica el RUT — solo lo valida. La decisión de aceptarlo
 * o no queda en manos del caller (típicamente el usuario decide).
 */
public final class RutValidator {

    private RutValidator() {}

    /**
     * Valida si un RUT es correcto según el algoritmo Módulo 11.
     * Acepta formatos: "12345678-9", "12.345.678-9", "123456789" (sin guión),
     * "12345678-K" / "12345678-k".
     *
     * @param rut RUT en cualquier formato común
     * @return true si el RUT es válido, false si no
     */
    public static boolean esValido(String rut) {
        if (rut == null) return false;
        String limpio = limpiar(rut);
        if (limpio.length() < 2) return false;

        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = Character.toUpperCase(limpio.charAt(limpio.length() - 1));

        // El cuerpo debe ser solo dígitos
        if (!cuerpo.matches("\\d+")) return false;
        // El cuerpo debe tener al menos 1 dígito (RUTs reales tienen al menos 7)
        if (cuerpo.isEmpty()) return false;
        // El DV debe ser 0-9 o K
        if (!Character.isDigit(dv) && dv != 'K') return false;

        char dvCalculado = calcularDV(cuerpo);
        return dv == dvCalculado;
    }

    /**
     * Calcula el dígito verificador para un cuerpo de RUT dado.
     * @param cuerpo solo los dígitos del RUT, sin guión ni DV
     * @return el DV calculado ('0'-'9' o 'K')
     */
    public static char calcularDV(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        // Recorrer de derecha a izquierda
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            int digito = cuerpo.charAt(i) - '0';
            suma += digito * multiplicador;
            multiplicador++;
            if (multiplicador > 7) multiplicador = 2;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return '0';
        if (resto == 10) return 'K';
        return (char) ('0' + resto);
    }

    /**
     * Quita puntos, guiones y espacios del RUT, dejando solo dígitos y DV.
     * Ej: "12.345.678-9" → "123456789"
     */
    private static String limpiar(String rut) {
        return rut.trim()
                  .replace(".", "")
                  .replace("-", "")
                  .replace(" ", "");
    }
}

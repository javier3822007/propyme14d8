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
 * NotasLegales — Textos legales hardcodeados utilizados en documentos
 * impresos (Balance Tributario, Certificación F1947, etc.).
 *
 * ⚠ IMPORTANTE: Estos textos están hardcodeados por diseño.
 * Si la legislación tributaria chilena cambia, estos textos deben ser
 * revisados manualmente y actualizados aquí. La aplicación debe
 * recompilarse después de cualquier cambio.
 *
 * Última revisión: 2026-05 (Régimen Pro Pyme Transparente 14 D N°8 LIR
 * vigente según Ley N° 21.210 y modificaciones posteriores).
 */
public final class NotasLegales {

    private NotasLegales() {}

    /**
     * Nota informativa al pie del Balance Tributario para empresas
     * acogidas al Régimen 14 D N°8 LIR.
     *
     * ⚠ HARDCODEADA — Actualizar si cambia el régimen tributario.
     * Fuente: Art. 14 letra D N°8 inciso (ii) LIR.
     */
    public static final String NOTA_BALANCE_14D8 =
        "Este balance se presenta con carácter informativo. La empresa se " +
        "encuentra acogida al Régimen Pro Pyme Transparente del Art. 14 letra " +
        "D N°8 de la Ley sobre Impuesto a la Renta, por lo que está liberada " +
        "de la obligación de llevar contabilidad completa (Art. 14 D N°8 " +
        "inciso (ii) LIR).";

    /**
     * Referencia legal estándar al pie del Balance Tributario.
     * ⚠ HARDCODEADA — Actualizar solo si cambia el Código Tributario.
     */
    public static final String ARTICULO_100_CT =
        "Artículo 100 Código Tributario: Balance confeccionado con los " +
        "antecedentes aportados por el Contribuyente.";

    /**
     * Título estándar del Balance Tributario.
     * Convención chilena: "BALANCE TRIBUTARIO (a nivel 4)"
     */
    public static final String TITULO_BALANCE = "BALANCE TRIBUTARIO (a nivel 4)";
}

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
package cl.propyme.model;

public class GlobalConfig {
    private int anio;
    private double[] factores = new double[13]; // index 1-12

    // IGC table: 8 tramos, each has [desde, hasta, factor, rebaja]
    private double[][] igcTable = new double[8][4];

    public GlobalConfig() {
        for (int i = 1; i <= 12; i++) factores[i] = 1.0;
        // IGC por defecto — valores sentinela, deben reemplazarse con datos reales antes de usar
        igcTable = new double[][] {
            {0,  1,  0.00, 0},
            {2,  3,  0.04, 450632},
            {4,  5,  0.08, 1452037},
            {6,  7,  0.135,3746923},
            {8,  9,  0.23, 9296375},
            {10, 11, 0.304,14854171},
            {12, 13, 0.35, 19460633},
            {14, 1.0E15, 0.40, 32395445},
        };
    }

    public GlobalConfig(int anio) { this(); this.anio = anio; }

    public int getAnio() { return anio; }
    public void setAnio(int v) { this.anio = v; }
    public double getFactor(int mes) {
        if (mes < 1 || mes > 12) return 1.0;
        return factores[mes] > 0 ? factores[mes] : 1.0;
    }
    public void setFactor(int mes, double v) { if (mes>=1&&mes<=12) factores[mes]=v; }
    public double[] getFactores() { return factores; }
    public void setFactores(double[] v) { this.factores = v; }
    public double[][] getIgcTable() { return igcTable; }
    public void setIgcTable(double[][] v) { this.igcTable = v; }

    public double calcularIGC(double base) {
        if (base <= 0) return 0;
        for (double[] row : igcTable) {
            if (base >= row[0] && base < row[1]) {
                return Math.max(0, base * row[2] - row[3]);
            }
        }
        return Math.max(0, base * 0.40 - igcTable[7][3]);
    }
}

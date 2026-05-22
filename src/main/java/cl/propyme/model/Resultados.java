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

/**
 * Computed results for Recuadros 22, 23 and F1947.
 * All values are derived — nothing stored, everything calculated on demand.
 */
public class Resultados {

    // ── Recuadro 22 ──────────────────────────────────────────────────────────
    public double ing1600;   // Ingresos giro percibidos
    public double ing1819;   // Devengados ejercicio anterior
    public double ing1601;   // Fuente extranjera
    public double ing1602;   // Intereses/reajustes
    public double ing1603;   // Mayor valor inversiones
    public double ing1604;   // Dividendos
    public double ing1605;   // Incremento IDPC
    public double ing1606;   // Devengados relacionadas
    public double ing1607;   // Otros ingresos (incl. reajuste PPM)
    public double ing1608;   // Ingreso diferido
    public double ing1609;   // Crédito 33 bis
    public double totalIngresos1610;

    public double eg1611;    // Saldo inicial existencias cambio régimen
    public double eg1612;    // Saldo inicial activo fijo cambio régimen
    public double eg1613;    // Pérdida tributaria cambio régimen
    public double eg1614;    // Existencias/servicios pagados
    public double eg1820;    // Existencias adeudadas año anterior
    public double eg1615;    // Gastos fuente extranjera
    public double eg1616;    // Remuneraciones
    public double eg1617;    // Honorarios
    public double eg1618;    // Activo fijo (dep. instantánea)
    public double eg1620;    // Arriendos
    public double eg1621;    // Gastos medioambientales
    public double eg1622;    // Intereses/reajustes pagados
    public double eg1624;    // Pérdida inversiones
    public double eg1625;    // Otros gastos
    public double eg1626;    // Gastos relacionadas
    public double eg1627;    // Pérdida ejercicios anteriores
    public double eg1628;    // Créditos incobrables
    public double eg1909;    // Donaciones
    public double totalEgresos1629;

    public double baseImponible1630; // positive = utilidad, negative = pérdida

    // ── Recuadro 23 ──────────────────────────────────────────────────────────
    public double cpts1580;
    public double cpts1582;
    public double cpts1573;
    public double cpts1574;
    public double cpts1575;
    public double cpts1712;  // base imponible asignable
    public double cpts1713;  // pérdida ejercicio
    public double cpts1714;  // pérdidas anteriores
    public double cpts1576;  // retiros históricos
    public double cpts1715;  // ingreso diferido
    public double cpts1577;  // art 21
    public double cpts1716;  // crédito 33 bis
    public double cpts1578;  // crédito IDPC
    public double cpts1584;  // otras partidas (+)
    public double cpts1585;  // otras partidas (-)
    public double cptsPositivo1581;
    public double cptsNegativo1583;

    // ── PPM ──────────────────────────────────────────────────────────────────
    public double ppmHistoricoTotal;
    public double ppmActualizadoTotal;
    public double reajustePPM;

    // ── Crédito 33 bis ───────────────────────────────────────────────────────
    public double credito33bis;

    // ── Ingreso diferido ─────────────────────────────────────────────────────
    public double ingresoDiferidoImputado;
    public double saldoIngresoDiferidoFinal;

    // ── IVA ──────────────────────────────────────────────────────────────────
    public double totalIVADebito;
    public double totalIVACreditoFiscal;
    public double totalIVANoRecuperable;
    public double ivaNetoAPagar; // DF - CF (positive = pagar, negative = CF favorece)
}

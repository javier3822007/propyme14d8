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

import java.util.ArrayList;
import java.util.List;

/**
 * Datos mensuales de un mes específico (1=Enero .. 12=Diciembre).
 * Se alimenta desde importación RCV o ingreso manual.
 */
public class DatosMes {
    private int mes; // 1-12

    // INGRESOS (desde RCV Ventas)
    private double ventasAfectasNetas;
    // Notas de crédito y débito sobre ventas (montos totales — el sistema sabe que NC resta, ND suma)
    private double notasCreditoVenta;
    private double notasDebitoVenta;
    private double ventasExentasIVA;
    private double ivaDebito;
    private double otrosIngresosPercibidos;
    private double ingresosNoPercibidos; // monto devengado no cobrado

    // PPM
    private double tasaPPM = 0.002; // 0.2% default

    // EGRESOS (desde RCV Compras)
    private double comprasAfectasNetas;
    // Notas de crédito y débito sobre compras (NC resta egresos, ND suma egresos)
    private double notasCreditoCompra;
    private double notasDebitoCompra;
    private double ivaCredFiscal;
    private double comprasExentasIVA;
    private double comprasAfectasIVAnoRecNeto;
    private double ivaNoRecuperable; // calculado
    private double activoFijoPagado;
    private double remuneracionesPagadas;
    private double honorariosPagados;
    private double arrendosPagados;
    private double gastosGenerales;
    private double otrosEgresos;
    private double egresosAdeudados;

    // RETIROS
    private double retiroSocio1Historico;
    private double retiroSocio2Historico;

    // Documentos importados del RCV
    private List<RegistroRCV> documentosVenta = new ArrayList<>();
    private List<RegistroRCV> documentosCompra = new ArrayList<>();

    public DatosMes() {}
    public DatosMes(int mes) { this.mes = mes; }

    // ── Derived calculations ──────────────────────────────────────────────────
    // Las Notas de Crédito (NC) restan, las Notas de Débito (ND) suman.
    // Esto aplica tanto en ventas (afecta PPM e ingresos) como en compras (afecta egresos).
    public double getBasePPM() {
        return ventasAfectasNetas - notasCreditoVenta + notasDebitoVenta
             + ventasExentasIVA + otrosIngresosPercibidos;
    }

    public double getPPMHistorico() {
        return Math.round(getBasePPM() * tasaPPM);
    }

    /** PPM Actualizado = PPM Histórico × factor manual. Si factor es 0, usa 1.0 (sin reajuste). */
    public double getPPMActualizado() {
        double f = factorReajustePPM > 0 ? factorReajustePPM : 1.0;
        return Math.round(getPPMHistorico() * f);
    }

    public double getIvaNoRecuperable() {
        return Math.round(comprasAfectasIVAnoRecNeto * 0.19);
    }

    public double getTotalEgresoCompras1614() {
        return comprasAfectasNetas - notasCreditoCompra + notasDebitoCompra
             + comprasExentasIVA + comprasAfectasIVAnoRecNeto + getIvaNoRecuperable();
    }

    public double getTotalEgresosPagados() {
        return getTotalEgresoCompras1614() + activoFijoPagado +
               remuneracionesPagadas + honorariosPagados +
               arrendosPagados + gastosGenerales + otrosEgresos;
    }

    public double getPercibidoAfecto() {
        return ventasAfectasNetas - notasCreditoVenta + notasDebitoVenta - ingresosNoPercibidos;
    }

    public double getTotalPercibido() {
        return getPercibidoAfecto() + ventasExentasIVA + otrosIngresosPercibidos;
    }

    public double getTotalRetirosHistorico() {
        return retiroSocio1Historico + retiroSocio2Historico;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int getMes() { return mes; }
    public void setMes(int v) { this.mes = v; }
    public double getVentasAfectasNetas() { return ventasAfectasNetas; }
    public void setVentasAfectasNetas(double v) { this.ventasAfectasNetas = v; }
    public double getNotasCreditoVenta() { return notasCreditoVenta; }
    public void setNotasCreditoVenta(double v) { this.notasCreditoVenta = v; }
    public double getNotasDebitoVenta() { return notasDebitoVenta; }
    public void setNotasDebitoVenta(double v) { this.notasDebitoVenta = v; }
    public double getVentasExentasIVA() { return ventasExentasIVA; }
    public void setVentasExentasIVA(double v) { this.ventasExentasIVA = v; }
    public double getIvaDebito() { return ivaDebito; }
    public void setIvaDebito(double v) { this.ivaDebito = v; }
    public double getOtrosIngresosPercibidos() { return otrosIngresosPercibidos; }
    public void setOtrosIngresosPercibidos(double v) { this.otrosIngresosPercibidos = v; }
    public double getIngresosNoPercibidos() { return ingresosNoPercibidos; }
    public void setIngresosNoPercibidos(double v) { this.ingresosNoPercibidos = v; }
    public double getTasaPPM() { return tasaPPM; }
    public void setTasaPPM(double v) { this.tasaPPM = v; }
    public double getComprasAfectasNetas() { return comprasAfectasNetas; }
    public void setComprasAfectasNetas(double v) { this.comprasAfectasNetas = v; }
    public double getNotasCreditoCompra() { return notasCreditoCompra; }
    public void setNotasCreditoCompra(double v) { this.notasCreditoCompra = v; }
    public double getNotasDebitoCompra() { return notasDebitoCompra; }
    public void setNotasDebitoCompra(double v) { this.notasDebitoCompra = v; }
    public double getIvaCredFiscal() { return ivaCredFiscal; }
    public void setIvaCredFiscal(double v) { this.ivaCredFiscal = v; }
    public double getComprasExentasIVA() { return comprasExentasIVA; }
    public void setComprasExentasIVA(double v) { this.comprasExentasIVA = v; }
    public double getComprasAfectasIVAnoRecNeto() { return comprasAfectasIVAnoRecNeto; }
    public void setComprasAfectasIVAnoRecNeto(double v) { this.comprasAfectasIVAnoRecNeto = v; }
    public double getActivoFijoPagado() { return activoFijoPagado; }
    public void setActivoFijoPagado(double v) { this.activoFijoPagado = v; }
    public double getRemuneracionesPagadas() { return remuneracionesPagadas; }
    public void setRemuneracionesPagadas(double v) { this.remuneracionesPagadas = v; }
    public double getHonorariosPagados() { return honorariosPagados; }
    public void setHonorariosPagados(double v) { this.honorariosPagados = v; }
    public double getArrendosPagados() { return arrendosPagados; }
    public void setArrendosPagados(double v) { this.arrendosPagados = v; }
    public double getGastosGenerales() { return gastosGenerales; }
    public void setGastosGenerales(double v) { this.gastosGenerales = v; }
    public double getOtrosEgresos() { return otrosEgresos; }
    public void setOtrosEgresos(double v) { this.otrosEgresos = v; }
    public double getEgresosAdeudados() { return egresosAdeudados; }
    public void setEgresosAdeudados(double v) { this.egresosAdeudados = v; }
    public double getRetiroSocio1Historico() { return retiroSocio1Historico; }
    public void setRetiroSocio1Historico(double v) { this.retiroSocio1Historico = v; }
    public double getRetiroSocio2Historico() { return retiroSocio2Historico; }
    public void setRetiroSocio2Historico(double v) { this.retiroSocio2Historico = v; }

    // ── Reajuste manual del PPM ──
    // factorReajustePPM = factor multiplicativo del PPM histórico para obtener PPM actualizado.
    // 0 = vacío/sin definir → se trata como 1.0 (sin reajuste).
    private double factorReajustePPM = 0.0;

    public List<RegistroRCV> getDocumentosVenta() { return documentosVenta; }
    public void setDocumentosVenta(List<RegistroRCV> v) { this.documentosVenta = v; }
    public List<RegistroRCV> getDocumentosCompra() { return documentosCompra; }
    public void setDocumentosCompra(List<RegistroRCV> v) { this.documentosCompra = v; }

    public double getFactorReajustePPM() { return factorReajustePPM; }
    public void setFactorReajustePPM(double v) { this.factorReajustePPM = v; }

}

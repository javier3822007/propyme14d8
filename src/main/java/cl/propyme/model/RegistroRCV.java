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
 * Representa una fila del CSV RCV del SII (compras o ventas).
 */
public class RegistroRCV {

    public enum Tipo { VENTA, COMPRA }

    // SII document type codes
    public static final int FACTURA_AFECTA       = 33;
    public static final int FACTURA_NO_AFECTA    = 34;
    public static final int LIQUIDACION_FACTURA  = 43;
    public static final int LIQUIDACION          = 46;
    public static final int NOTA_DEBITO          = 56;
    public static final int NOTA_CREDITO         = 61;
    public static final int BOLETA_AFECTA        = 39;
    public static final int BOLETA_NO_AFECTA     = 41;
    public static final int GUIA_DESPACHO        = 52; // sin efecto contable
    public static final int FACT_EXPORT          = 110;
    public static final int LIQUIDACION_EXPORT   = 111;
    public static final int NOTA_DEBITO_EXPORT   = 112;

    private Tipo tipo;
    private int tipoDoc;
    private String tipoCompraVenta; // "Del Giro", "Activo Fijo", etc.
    private String rutContraparte;
    private String razonSocial;
    private String folio;
    private String fechaDocto;
    private double montoExento;
    private double montoNeto;
    private double montoIVARecuperable;
    private double montoIVANoRecuperable;
    private double montoTotal;
    private double montoNetoActivoFijo;
    private double ivaActivoFijo;
    private double ivaUsoComun;
    private boolean anulado;

    public RegistroRCV() {}

    /**
     * Retorna true si este tipo de documento tiene efecto contable.
     * Guías de despacho (52) are excluded.
     */
    public boolean tieneEfectoContable() {
        return tipoDoc != GUIA_DESPACHO;
    }

    /**
     * Retorna una descripción legible del tipo de documento.
     */
    public static String descripcionTipoDoc(int cod) {
        return switch (cod) {
            case 33  -> "Factura Afecta";
            case 34  -> "Factura No Afecta/Exenta";
            case 39  -> "Boleta Afecta";
            case 41  -> "Boleta No Afecta/Exenta";
            case 43  -> "Liquidación-Factura";
            case 46  -> "Liquidación";
            case 52  -> "Guía de Despacho (sin efecto contable)";
            case 56  -> "Nota de Débito";
            case 61  -> "Nota de Crédito";
            case 110 -> "Factura de Exportación";
            case 111 -> "Liquidación de Exportación";
            case 112 -> "Nota de Débito Exportación";
            default  -> "Tipo Doc " + cod;
        };
    }

    /**
     * Retorna true si es nota de crédito (reduce montos).
     */
    public boolean esNotaCredito() {
        return tipoDoc == NOTA_CREDITO;
    }

    /**
     * Retorna true si es nota de débito (aumenta montos).
     */
    public boolean esNotaDebito() {
        return tipoDoc == NOTA_DEBITO || tipoDoc == NOTA_DEBITO_EXPORT;
    }

    /**
     * Retorna true si es activo fijo.
     */
    public boolean esActivoFijo() {
        return "Activo Fijo".equalsIgnoreCase(tipoCompraVenta) || montoNetoActivoFijo > 0;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo v) { this.tipo = v; }
    public int getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(int v) { this.tipoDoc = v; }
    public String getTipoCompraVenta() { return tipoCompraVenta; }
    public void setTipoCompraVenta(String v) { this.tipoCompraVenta = v; }
    public String getRutContraparte() { return rutContraparte; }
    public void setRutContraparte(String v) { this.rutContraparte = v; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String v) { this.razonSocial = v; }
    public String getFolio() { return folio; }
    public void setFolio(String v) { this.folio = v; }
    public String getFechaDocto() { return fechaDocto; }
    public void setFechaDocto(String v) { this.fechaDocto = v; }
    public double getMontoExento() { return montoExento; }
    public void setMontoExento(double v) { this.montoExento = v; }
    public double getMontoNeto() { return montoNeto; }
    public void setMontoNeto(double v) { this.montoNeto = v; }
    public double getMontoIVARecuperable() { return montoIVARecuperable; }
    public void setMontoIVARecuperable(double v) { this.montoIVARecuperable = v; }
    public double getMontoIVANoRecuperable() { return montoIVANoRecuperable; }
    public void setMontoIVANoRecuperable(double v) { this.montoIVANoRecuperable = v; }
    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double v) { this.montoTotal = v; }
    public double getMontoNetoActivoFijo() { return montoNetoActivoFijo; }
    public void setMontoNetoActivoFijo(double v) { this.montoNetoActivoFijo = v; }
    public double getIvaActivoFijo() { return ivaActivoFijo; }
    public void setIvaActivoFijo(double v) { this.ivaActivoFijo = v; }
    public double getIvaUsoComun() { return ivaUsoComun; }
    public void setIvaUsoComun(double v) { this.ivaUsoComun = v; }
    public boolean isAnulado() { return anulado; }
    public void setAnulado(boolean v) { this.anulado = v; }
}

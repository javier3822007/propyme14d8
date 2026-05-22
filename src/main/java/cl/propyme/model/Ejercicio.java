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

public class Ejercicio {
    private int anioComercial;
    // Antecedentes año anterior
    private double cptsPositivoInicial;
    private double cptsNegativoInicial;
    private double perdidaAnterior;
    private double existenciasAdeudadasAnteriores;
    // Capital aportado para empresas que inician actividades (Cód. 1573)
    private double capitalInicial;
    // Ingreso diferido
    private double saldoIngresoDiferido;
    // Dividendos (hasta 5)
    private double[] dividendoMonto = new double[5];
    private double[] dividendoCredito = new double[5];
    private String[] dividendoDetalle = new String[5];
    // 33 bis
    private double activoFijoFactor33bis;
    // UF / UTM
    private double ufDiciembre;
    private double utmDiciembre;
    // PPM tasa (puede variar mes a mes — se guarda en DatosMensuales)

    public Ejercicio() {
        for (int i = 0; i < 5; i++) {
            dividendoDetalle[i] = "";
        }
        activoFijoFactor33bis = 1.0;
    }

    public Ejercicio(int anio) {
        this();
        this.anioComercial = anio;
    }

    public int getAnioComercial() { return anioComercial; }
    public void setAnioComercial(int v) { this.anioComercial = v; }
    public double getCptsPositivoInicial() { return cptsPositivoInicial; }
    public void setCptsPositivoInicial(double v) { this.cptsPositivoInicial = v; }
    public double getCptsNegativoInicial() { return cptsNegativoInicial; }
    public void setCptsNegativoInicial(double v) { this.cptsNegativoInicial = v; }
    public double getPerdidaAnterior() { return perdidaAnterior; }
    public void setPerdidaAnterior(double v) { this.perdidaAnterior = v; }
    public double getExistenciasAdeudadasAnteriores() { return existenciasAdeudadasAnteriores; }
    public void setExistenciasAdeudadasAnteriores(double v) { this.existenciasAdeudadasAnteriores = v; }
    public double getCapitalInicial() { return capitalInicial; }
    public void setCapitalInicial(double v) { this.capitalInicial = v; }
    public double getSaldoIngresoDiferido() { return saldoIngresoDiferido; }
    public void setSaldoIngresoDiferido(double v) { this.saldoIngresoDiferido = v; }
    public double[] getDividendoMonto() { return dividendoMonto; }
    public void setDividendoMonto(double[] v) { this.dividendoMonto = v; }
    public double[] getDividendoCredito() { return dividendoCredito; }
    public void setDividendoCredito(double[] v) { this.dividendoCredito = v; }
    public String[] getDividendoDetalle() { return dividendoDetalle; }
    public void setDividendoDetalle(String[] v) { this.dividendoDetalle = v; }
    public double getActivoFijoFactor33bis() { return activoFijoFactor33bis; }
    public void setActivoFijoFactor33bis(double v) { this.activoFijoFactor33bis = v; }
    public double getUfDiciembre() { return ufDiciembre; }
    public void setUfDiciembre(double v) { this.ufDiciembre = v; }
    public double getUtmDiciembre() { return utmDiciembre; }
    public void setUtmDiciembre(double v) { this.utmDiciembre = v; }

}

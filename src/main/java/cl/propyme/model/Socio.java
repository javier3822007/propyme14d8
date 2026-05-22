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

public class Socio {
    private String rut;
    private String nombre;
    private double porcentaje; // 0.0 - 1.0
    private double capitalAportado;
    private int numeroCertificado;

    public Socio() {}
    public Socio(String rut, String nombre, double porcentaje, double capitalAportado, int cert) {
        this.rut = rut; this.nombre = nombre;
        this.porcentaje = porcentaje; this.capitalAportado = capitalAportado;
        this.numeroCertificado = cert;
    }

    public String getRut() { return rut; }
    public void setRut(String v) { this.rut = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double v) { this.porcentaje = v; }
    public double getCapitalAportado() { return capitalAportado; }
    public void setCapitalAportado(double v) { this.capitalAportado = v; }
    public int getNumeroCertificado() { return numeroCertificado; }
    public void setNumeroCertificado(int v) { this.numeroCertificado = v; }
    @Override public String toString() { return nombre + " (" + rut + ")"; }
}

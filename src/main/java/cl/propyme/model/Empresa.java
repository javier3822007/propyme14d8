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

public class Empresa {
    private String rut;
    private String razonSocial;
    private String giro;
    private String domicilio;
    private List<Socio> socios = new ArrayList<>();

    public Empresa() {}

    public Empresa(String rut, String razonSocial, String giro, String domicilio) {
        this.rut = rut;
        this.razonSocial = razonSocial;
        this.giro = giro;
        this.domicilio = domicilio;
    }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String v) { this.razonSocial = v; }
    public String getGiro() { return giro; }
    public void setGiro(String v) { this.giro = v; }
    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String v) { this.domicilio = v; }
    public List<Socio> getSocios() { return socios; }
    public void setSocios(List<Socio> s) { this.socios = s; }

    @Override
    public String toString() { return razonSocial + " (" + rut + ")"; }
}

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
package cl.propyme.service;

import cl.propyme.model.DatosMes;
import cl.propyme.model.Ejercicio;
import cl.propyme.model.GlobalConfig;
import cl.propyme.model.Resultados;
import cl.propyme.model.Socio;
import java.util.List;
import java.util.Map;

public class CalculadorImpuesto {

    public static Resultados calcular(Ejercicio ej, Map<Integer,DatosMes> meses) {
        return calcular(ej, meses, null);
    }

    public static Resultados calcular(Ejercicio ej, Map<Integer,DatosMes> meses, GlobalConfig cfg) {
        Resultados r = new Resultados();

        double totVtaAfectas=0,totVtaExentas=0,totOtrosIng=0,totNoPercibido=0;
        double totIvaDF=0,totIvaCF=0,totIvaNoRec=0;
        double totComp=0,totCompExentas=0,totCompNoRecNeto=0;
        double totAF=0,totRemun=0,totHonor=0,totArr=0;
        double totGastos=0,totOtrosEg=0,totAdeudados=0;
        double totPPMhist=0,totPPMact=0;
        double totRetS1hist=0,totRetS2hist=0;
        double totRetS1act=0,totRetS2act=0;

        for (DatosMes dm : meses.values()) {
            // Ventas: la base se ajusta con NC (resta) y ND (suma)
            totVtaAfectas  += dm.getVentasAfectasNetas()
                            - dm.getNotasCreditoVenta()
                            + dm.getNotasDebitoVenta();
            totVtaExentas  += dm.getVentasExentasIVA();
            totOtrosIng    += dm.getOtrosIngresosPercibidos();
            totNoPercibido += dm.getIngresosNoPercibidos();
            totIvaDF       += dm.getIvaDebito();
            totIvaCF       += dm.getIvaCredFiscal();
            totIvaNoRec    += dm.getIvaNoRecuperable();
            // Compras afectas: calculadas desde el IVA CF para coincidir con el criterio
            // del SII (que usa el IVA como dato primario). Las notas NC/ND ya están
            // reflejadas en el IVA CF que ingresa el contador, por lo que NO se restan/suman aquí.
            totComp        += Math.round(dm.getIvaCredFiscal() / 0.19);
            totCompExentas += dm.getComprasExentasIVA();
            totCompNoRecNeto += dm.getComprasAfectasIVAnoRecNeto();
            totAF          += dm.getActivoFijoPagado();
            totRemun       += dm.getRemuneracionesPagadas();
            totHonor       += dm.getHonorariosPagados();
            totArr         += dm.getArrendosPagados();
            totGastos      += dm.getGastosGenerales();
            totOtrosEg     += dm.getOtrosEgresos();
            totAdeudados   += dm.getEgresosAdeudados();
            // NOTA: totAdeudados es informativo solamente. En el régimen 14D N°8
            // (base caja), los egresos adeudados NO afectan la Base Imponible del
            // año actual. Cuando se paguen efectivamente, se registrarán al año
            // siguiente como "existencias adeudadas año anterior" en el Cód. 1820.
            totPPMhist     += dm.getPPMHistorico();
            // PPM Actualizado: factor manual por mes (0 = sin reajuste, default)
            totPPMact      += dm.getPPMActualizado();
            totRetS1hist   += dm.getRetiroSocio1Historico();
            totRetS2hist   += dm.getRetiroSocio2Historico();
            // Retiros no se reajustan tributariamente; usar valor histórico
            totRetS1act    += dm.getRetiroSocio1Historico();
            totRetS2act    += dm.getRetiroSocio2Historico();
        }

        r.totalIVADebito        = totIvaDF;
        r.totalIVACreditoFiscal = totIvaCF;
        r.totalIVANoRecuperable = totIvaNoRec;
        r.ivaNetoAPagar         = totIvaDF - totIvaCF;
        r.ppmHistoricoTotal     = totPPMhist;
        r.ppmActualizadoTotal   = totPPMact;
        r.reajustePPM           = totPPMact - totPPMhist;

        // Crédito 33 bis
        double af33 = totAF * ej.getActivoFijoFactor33bis();
        r.credito33bis = Math.round(af33 * 0.06);

        // Ingreso diferido
        r.ingresoDiferidoImputado   = Math.round(ej.getSaldoIngresoDiferido() / 10.0);
        r.saldoIngresoDiferidoFinal = ej.getSaldoIngresoDiferido() - r.ingresoDiferidoImputado;

        // Dividendos
        double totalDivMonto=0, totalDivCredito=0;
        double[] divMontos   = ej.getDividendoMonto();
        double[] divCreditos = ej.getDividendoCredito();
        if (divMontos != null && divCreditos != null) {
            int maxLen = Math.min(divMontos.length, divCreditos.length);
            for (int i = 0; i < maxLen; i++) {
                totalDivMonto   += divMontos[i];
                totalDivCredito += divCreditos[i];
            }
        }

        // ── RECUADRO 22 ───────────────────────────────────────────────────────
        double percibidoAfecto = totVtaAfectas - totNoPercibido;
        r.ing1600 = Math.round(percibidoAfecto + totVtaExentas + totOtrosIng);
        r.ing1819 = 0;
        r.ing1601 = 0; r.ing1602 = 0; r.ing1603 = 0;
        r.ing1604 = totalDivMonto;
        r.ing1605 = totalDivCredito;
        r.ing1606 = 0;
        r.ing1607 = r.reajustePPM;
        r.ing1608 = r.ingresoDiferidoImputado;
        r.ing1609 = r.credito33bis;

        r.totalIngresos1610 = r.ing1600+r.ing1819+r.ing1601+r.ing1602+r.ing1603
                            + r.ing1604+r.ing1605+r.ing1606+r.ing1607+r.ing1608+r.ing1609;

        r.eg1611=0; r.eg1612=0; r.eg1613=0;
        r.eg1614 = Math.round(totComp+totCompExentas+totCompNoRecNeto+totIvaNoRec);
        r.eg1820 = ej.getExistenciasAdeudadasAnteriores();
        r.eg1615=0;
        r.eg1616 = totRemun;
        r.eg1617 = totHonor;
        r.eg1618 = totAF;
        r.eg1620 = totArr;
        r.eg1621=0; r.eg1622=0; r.eg1624=0;
        r.eg1625 = totGastos+totOtrosEg;
        r.eg1626=0;
        r.eg1627 = ej.getPerdidaAnterior();
        r.eg1628=0; r.eg1909=0;

        r.totalEgresos1629 = r.eg1611+r.eg1612+r.eg1613+r.eg1614+r.eg1820
                           + r.eg1615+r.eg1616+r.eg1617+r.eg1618+r.eg1620
                           + r.eg1621+r.eg1622+r.eg1624+r.eg1625+r.eg1626
                           + r.eg1627+r.eg1628+r.eg1909;

        r.baseImponible1630 = Math.round(r.totalIngresos1610 - r.totalEgresos1629);

        // ── RECUADRO 23 ──────────────────────────────────────────────────────
        r.cpts1580 = ej.getCptsPositivoInicial();
        r.cpts1582 = ej.getCptsNegativoInicial();
        r.cpts1573=ej.getCapitalInicial(); r.cpts1574=0; r.cpts1575=0;
        r.cpts1712 = Math.max(0, r.baseImponible1630);
        r.cpts1713 = Math.max(0,-r.baseImponible1630);
        r.cpts1714 = ej.getPerdidaAnterior();
        r.cpts1576 = totRetS1hist + totRetS2hist;
        r.cpts1715 = r.ingresoDiferidoImputado;
        r.cpts1577=0;
        r.cpts1716 = r.credito33bis;
        r.cpts1578 = totalDivCredito;
        r.cpts1584=0; r.cpts1585=0;

        double cptsCalc = r.cpts1580-r.cpts1582+r.cpts1573+r.cpts1574-r.cpts1575
                        + r.cpts1712-r.cpts1713+r.cpts1714-r.cpts1576-r.cpts1715
                        - r.cpts1577-r.cpts1716-r.cpts1578+r.cpts1584-r.cpts1585;
        r.cptsPositivo1581 = Math.max(0, cptsCalc);
        r.cptsNegativo1583 = Math.max(0,-cptsCalc);

        return r;
    }

    public static double calcularIGC(double base, double utmDiciembre) {
        // Mantenido por compatibilidad. NO usa tabla IGC hardcodeada (sería incorrecta).
        // Si llegamos aquí sin GlobalConfig válido, retornamos 0 — el caller
        // debe usar la sobrecarga calcularIGC(base, GlobalConfig) con config cargado.
        return 0;
    }

    public static double calcularIGC(double base, GlobalConfig cfg) {
        if (base <= 0 || cfg == null) return 0;
        return cfg.calcularIGC(base);
    }

    public static double[] distribuirBaseImponible(Resultados res, List<Socio> socios) {
        double[] result = new double[socios.size()];
        if (socios.isEmpty()) return result;
        double acumulado = 0;
        // Todos menos el último: redondeo normal
        for (int i = 0; i < socios.size() - 1; i++) {
            result[i] = Math.round(res.baseImponible1630 * socios.get(i).getPorcentaje());
            acumulado += result[i];
        }
        // El último socio se lleva el resto exacto para cuadrar a 0
        int last = socios.size() - 1;
        result[last] = res.baseImponible1630 - acumulado;
        return result;
    }
}

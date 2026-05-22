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

import cl.propyme.model.Empresa;
import cl.propyme.service.BalanceMiniService.FilaBalance;
import cl.propyme.service.BalanceMiniService.Totales;
import cl.propyme.util.NotasLegales;

import java.awt.*;
import java.awt.print.*;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * BalancePrintService — Impresión del Balance Tributario en formato estándar.
 *
 * Usa Java Print API nativa, sin dependencias externas. El usuario puede:
 *   - Imprimir en papel
 *   - "Imprimir a PDF" (todos los OS modernos soportan esto)
 *   - Cancelar desde el diálogo del sistema
 *
 * Formato del documento (siguiendo estándar contable chileno):
 *   1. Encabezado: razón social, RUT, giro, dirección
 *   2. Título: "BALANCE TRIBUTARIO (a nivel 4) — Al 31 de Diciembre de [año]"
 *   3. Tabla 8 columnas: Cuenta + Sumas (D/C) + Saldos (D/A) + Inv. (A/P) + Result. (P/G)
 *   4. Cierre: Subtotal / Utilidad del Ejercicio / Totales
 *   5. Pie: Nota legal 14D8 + Art. 100 CT + líneas de firmas
 */
public class BalancePrintService {

    // ── Configuración de fuente y márgenes ───────────────────────────────────

    private static final Font FONT_HEADER     = new Font("SansSerif", Font.BOLD, 9);
    private static final Font FONT_HEADER_REG = new Font("SansSerif", Font.PLAIN, 9);
    private static final Font FONT_TITLE      = new Font("SansSerif", Font.BOLD, 11);
    private static final Font FONT_SUBTITLE   = new Font("SansSerif", Font.PLAIN, 9);
    private static final Font FONT_COL_HDR    = new Font("SansSerif", Font.BOLD, 7);
    private static final Font FONT_DATA       = new Font("SansSerif", Font.PLAIN, 7);
    private static final Font FONT_TOTAL      = new Font("SansSerif", Font.BOLD, 7);
    private static final Font FONT_FOOTER     = new Font("SansSerif", Font.PLAIN, 7);
    private static final Font FONT_SIGN       = new Font("SansSerif", Font.PLAIN, 8);

    /**
     * Imprime el balance.
     * @param parent componente padre para mostrar diálogos
     * @param empresa datos de la empresa
     * @param anio año del ejercicio
     * @param filas lista de cuentas a imprimir
     * @param totales totales calculados
     */
    public static void imprimir(Component parent, Empresa empresa, int anio,
                                List<FilaBalance> filas, Totales totales) {
        if (filas == null || filas.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                "No hay datos para imprimir. Cargue o complete el balance primero.",
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Balance Tributario - " +
            (empresa != null ? empresa.getRazonSocial() : "ProPyme") + " - " + anio);

        // Orientación horizontal (landscape) por defecto
        PageFormat pageFormat = job.defaultPage();
        pageFormat.setOrientation(PageFormat.LANDSCAPE);
        Paper paper = pageFormat.getPaper();
        // Márgenes razonables: 1.5cm = ~42 puntos
        double margen = 42;
        paper.setImageableArea(margen, margen,
            paper.getWidth() - margen * 2,
            paper.getHeight() - margen * 2);
        pageFormat.setPaper(paper);

        job.setPrintable(new BalancePrintable(empresa, anio, filas, totales), pageFormat);

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent,
                    "Error al imprimir:\n" + ex.getMessage(),
                    "Error de impresión", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Implementación de Printable ──────────────────────────────────────────

    private static class BalancePrintable implements Printable {
        private final Empresa empresa;
        private final int     anio;
        private final List<FilaBalance> filas;
        private final Totales totales;

        // Ancho relativo de cada columna (suma debe ser 100)
        private static final int[] ANCHOS_COL = {28, 9, 9, 9, 9, 9, 9, 9, 9};
        private static final String[] HDR_PRINCIPAL = {
            "", "SUMAS", "SUMAS", "SALDOS", "SALDOS",
            "INVENTARIO", "INVENTARIO", "RESULTADOS", "RESULTADOS"
        };
        private static final String[] HDR_SUB = {
            "CUENTA", "DEBITO", "CREDITOS", "DEUDOR", "ACREEDOR",
            "ACTIVO", "PASIVO", "PERDIDAS", "GANANCIAS"
        };

        BalancePrintable(Empresa empresa, int anio,
                         List<FilaBalance> filas, Totales totales) {
            this.empresa = empresa;
            this.anio    = anio;
            this.filas   = filas;
            this.totales = totales;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                throws PrinterException {

            // Calcular cuántas filas caben por página
            int filasPorPagina = calcularFilasPorPagina(pageFormat);
            int totalPaginas   = (int) Math.ceil((double) filas.size() / filasPorPagina);
            if (totalPaginas == 0) totalPaginas = 1;

            if (pageIndex >= totalPaginas) return NO_SUCH_PAGE;

            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                              RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double x = pageFormat.getImageableX();
            double y = pageFormat.getImageableY();
            double w = pageFormat.getImageableWidth();
            g.translate(x, y);

            int yPos = 0;

            // ── 1. Encabezado de empresa (solo en página 1) ──────────────────
            if (pageIndex == 0) {
                yPos = dibujarEncabezadoEmpresa(g, (int) w, yPos);
                yPos += 12;
                yPos = dibujarTitulo(g, (int) w, yPos);
                yPos += 10;
            } else {
                // En páginas siguientes, título resumido
                g.setFont(FONT_SUBTITLE);
                String cont = NotasLegales.TITULO_BALANCE + " — Continuación (página "
                            + (pageIndex + 1) + " de " + totalPaginas + ")";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(cont, (int) (w - fm.stringWidth(cont)) / 2, yPos + 12);
                yPos += 20;
            }

            // ── 2. Headers de columnas ───────────────────────────────────────
            yPos = dibujarHeadersTabla(g, (int) w, yPos);

            // ── 3. Filas de datos para esta página ───────────────────────────
            int inicio = pageIndex * filasPorPagina;
            int fin    = Math.min(inicio + filasPorPagina, filas.size());
            for (int i = inicio; i < fin; i++) {
                yPos = dibujarFila(g, (int) w, yPos, filas.get(i), false, false, false);
            }

            // ── 4. Footer con totales y firma (solo en última página) ────────
            if (pageIndex == totalPaginas - 1) {
                yPos += 4;
                yPos = dibujarSubtotal(g, (int) w, yPos);
                yPos = dibujarUtilidad(g, (int) w, yPos);
                yPos = dibujarTotalFinal(g, (int) w, yPos);

                yPos += 18;
                yPos = dibujarNotasLegales(g, (int) w, yPos);
                yPos += 8;
                dibujarFirmas(g, (int) w, yPos);
            }

            return PAGE_EXISTS;
        }

        // ── Dibujo de secciones ───────────────────────────────────────────────

        private int dibujarEncabezadoEmpresa(Graphics2D g, int w, int y) {
            g.setFont(FONT_HEADER);
            int yL = y + 12;
            g.drawString(empresa != null ? empresa.getRazonSocial() : "", 0, yL);
            g.setFont(FONT_HEADER_REG);
            int linea = 11;
            yL += linea;
            g.drawString("R.U.T          : " + (empresa != null ? empresa.getRut() : ""), 0, yL);
            yL += linea;
            g.drawString("GIRO           : " + (empresa != null ? empresa.getGiro() : ""), 0, yL);
            yL += linea;
            g.drawString("DIRECCION      : " + (empresa != null ? empresa.getDomicilio() : ""), 0, yL);
            return yL + 4;
        }

        private int dibujarTitulo(Graphics2D g, int w, int y) {
            g.setFont(FONT_TITLE);
            FontMetrics fm = g.getFontMetrics();
            String titulo = NotasLegales.TITULO_BALANCE;
            g.drawString(titulo, (w - fm.stringWidth(titulo)) / 2, y + 14);

            g.setFont(FONT_SUBTITLE);
            FontMetrics fm2 = g.getFontMetrics();
            String fecha = "Al 31 de Diciembre de " + anio;
            g.drawString(fecha, (w - fm2.stringWidth(fecha)) / 2, y + 26);
            return y + 30;
        }

        private int dibujarHeadersTabla(Graphics2D g, int w, int y) {
            g.setFont(FONT_COL_HDR);
            // Línea horizontal superior
            g.drawLine(0, y, w, y);
            y += 12;

            // Headers principales (SUMAS / SALDOS / INVENTARIO / RESULTADOS)
            // Cada uno se centra sobre 2 columnas: 1-2, 3-4, 5-6, 7-8
            int[] anchosPx = anchosPx(w);
            String[] grupos = {"SUMAS", "SALDOS", "INVENTARIO", "RESULTADOS"};
            int xGrupo = anchosPx[0]; // saltamos la columna CUENTA
            FontMetrics fm = g.getFontMetrics();
            for (int gi = 0; gi < grupos.length; gi++) {
                int idxCol1 = 1 + gi * 2;
                int idxCol2 = idxCol1 + 1;
                int anchoGrupo = anchosPx[idxCol1] + anchosPx[idxCol2];
                int tx = xGrupo + (anchoGrupo - fm.stringWidth(grupos[gi])) / 2;
                g.drawString(grupos[gi], tx, y);
                xGrupo += anchoGrupo;
            }
            y += 11;

            // Sub-headers (CUENTA / DEBITO / CREDITOS / etc.)
            int x = 0;
            for (int i = 0; i < HDR_SUB.length; i++) {
                FontMetrics fm2 = g.getFontMetrics();
                int tx;
                if (i == 0) {
                    tx = x + 2; // CUENTA alineada a la izquierda
                } else {
                    tx = x + anchosPx[i] - fm2.stringWidth(HDR_SUB[i]) - 2; // resto a la derecha
                }
                g.drawString(HDR_SUB[i], tx, y);
                x += anchosPx[i];
            }
            y += 4;
            g.drawLine(0, y, w, y);
            return y + 4;
        }

        private int dibujarFila(Graphics2D g, int w, int y, FilaBalance f,
                                boolean esSubtotal, boolean esUtilidad, boolean esTotalFinal) {
            g.setFont(esSubtotal || esUtilidad || esTotalFinal ? FONT_TOTAL : FONT_DATA);
            int[] anchosPx = anchosPx(w);
            int x = 0;
            FontMetrics fm = g.getFontMetrics();

            // Columna Cuenta (izquierda)
            String cuenta = f.cuenta != null ? f.cuenta : "";
            // Truncar si es muy larga
            int maxW = anchosPx[0] - 4;
            while (fm.stringWidth(cuenta) > maxW && cuenta.length() > 5) {
                cuenta = cuenta.substring(0, cuenta.length() - 2);
            }
            g.drawString(cuenta, x + 2, y + 9);
            x += anchosPx[0];

            // Columnas numéricas (derecha)
            double[] valores = {
                f.sumasDebito, f.sumasCredito,
                f.saldoDeudor, f.saldoAcreedor,
                f.invActivo, f.invPasivo,
                f.resPerdidas, f.resGanancias
            };
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] != 0) {
                    String s = String.format("%,.0f", valores[i]);
                    int tx = x + anchosPx[i + 1] - fm.stringWidth(s) - 2;
                    g.drawString(s, tx, y + 9);
                }
                x += anchosPx[i + 1];
            }
            return y + 11;
        }

        private int dibujarSubtotal(Graphics2D g, int w, int y) {
            FilaBalance sub = new FilaBalance();
            sub.cuenta        = "SUBTOTAL";
            sub.sumasDebito   = totales.sumasDebito;
            sub.sumasCredito  = totales.sumasCredito;
            sub.saldoDeudor   = totales.saldoDeudor;
            sub.saldoAcreedor = totales.saldoAcreedor;
            sub.invActivo     = totales.invActivo;
            sub.invPasivo     = totales.invPasivo;
            sub.resPerdidas   = totales.resPerdidas;
            sub.resGanancias  = totales.resGanancias;
            g.drawLine(0, y - 2, w, y - 2);
            return dibujarFila(g, w, y, sub, true, false, false);
        }

        private int dibujarUtilidad(Graphics2D g, int w, int y) {
            double util = totales.utilidad;
            FilaBalance u = new FilaBalance();
            u.cuenta = util >= 0 ? "UTILIDAD DEL EJERCICIO" : "PÉRDIDA DEL EJERCICIO";
            double abs = Math.abs(util);
            if (util >= 0) {
                u.invPasivo = abs;
                u.resPerdidas = abs;
            } else {
                u.invActivo = abs;
                u.resGanancias = abs;
            }
            return dibujarFila(g, w, y, u, false, true, false);
        }

        private int dibujarTotalFinal(Graphics2D g, int w, int y) {
            double util = totales.utilidad;
            FilaBalance t = new FilaBalance();
            t.cuenta        = "TOTALES";
            t.sumasDebito   = totales.sumasDebito;
            t.sumasCredito  = totales.sumasCredito;
            t.saldoDeudor   = totales.saldoDeudor;
            t.saldoAcreedor = totales.saldoAcreedor;
            t.invActivo     = totales.invActivo  + (util < 0 ? Math.abs(util) : 0);
            t.invPasivo     = totales.invPasivo  + (util > 0 ? util : 0);
            t.resPerdidas   = totales.resPerdidas  + (util > 0 ? util : 0);
            t.resGanancias  = totales.resGanancias + (util < 0 ? Math.abs(util) : 0);
            g.drawLine(0, y - 2, w, y - 2);
            int r = dibujarFila(g, w, y, t, false, false, true);
            g.drawLine(0, r - 1, w, r - 1);
            return r;
        }

        private int dibujarNotasLegales(Graphics2D g, int w, int y) {
            g.setFont(FONT_FOOTER);
            int yL = y;
            yL = dibujarTextoMultilinea(g, NotasLegales.NOTA_BALANCE_14D8, 0, yL, w);
            yL += 4;
            yL = dibujarTextoMultilinea(g, NotasLegales.ARTICULO_100_CT, 0, yL, w);
            return yL;
        }

        private int dibujarTextoMultilinea(Graphics2D g, String texto,
                                           int x, int y, int maxW) {
            FontMetrics fm = g.getFontMetrics();
            String[] palabras = texto.split(" ");
            StringBuilder linea = new StringBuilder();
            int yL = y + 9;
            for (String p : palabras) {
                String prueba = linea.length() == 0 ? p : linea + " " + p;
                if (fm.stringWidth(prueba) > maxW) {
                    g.drawString(linea.toString(), x, yL);
                    yL += 10;
                    linea = new StringBuilder(p);
                } else {
                    linea = new StringBuilder(prueba);
                }
            }
            if (linea.length() > 0) {
                g.drawString(linea.toString(), x, yL);
                yL += 10;
            }
            return yL;
        }

        private void dibujarFirmas(Graphics2D g, int w, int y) {
            g.setFont(FONT_SIGN);
            int yLinea = y + 30;
            // Línea izquierda: Contador General
            int xIzqIni = w / 6;
            int xIzqFin = xIzqIni + w / 4;
            g.drawLine(xIzqIni, yLinea, xIzqFin, yLinea);
            FontMetrics fm = g.getFontMetrics();
            String c1 = "CONTADOR GENERAL";
            g.drawString(c1, xIzqIni + ((xIzqFin - xIzqIni) - fm.stringWidth(c1)) / 2,
                         yLinea + 12);

            // Línea derecha: Representante Legal
            int xDerIni = w - w / 6 - w / 4;
            int xDerFin = w - w / 6;
            g.drawLine(xDerIni, yLinea, xDerFin, yLinea);
            String c2 = "REPRESENTANTE LEGAL";
            g.drawString(c2, xDerIni + ((xDerFin - xDerIni) - fm.stringWidth(c2)) / 2,
                         yLinea + 12);
        }

        // ── Helpers ──────────────────────────────────────────────────────────

        private int[] anchosPx(int w) {
            int[] result = new int[ANCHOS_COL.length];
            for (int i = 0; i < ANCHOS_COL.length; i++)
                result[i] = w * ANCHOS_COL[i] / 100;
            return result;
        }

        private int calcularFilasPorPagina(PageFormat pf) {
            // Estimación conservadora: ~50 filas por página landscape A4
            // Reservamos espacio para encabezado (~150px) y pie (~120px)
            double altoUtil = pf.getImageableHeight() - 270;
            return Math.max(10, (int) (altoUtil / 11));
        }
    }
}

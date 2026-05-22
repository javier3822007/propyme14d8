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

import cl.propyme.model.RegistroRCV;
import cl.propyme.model.RegistroRCV.Tipo;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import cl.propyme.model.DatosMes;

/**
 * Parsea archivos CSV del RCV del SII (compras y ventas).
 * Maneja codificaciones UTF-8 e ISO-8859-1, separado por punto y coma.
 * Guías de despacho (tipo 52) are imported but flagged as no accounting effect.
 */
public class RCVParser {

    public enum TipoArchivo { VENTA, COMPRA }

    /**
     * Parsea un archivo RCV y retorna una lista de RegistroRCV.
     * @param file the CSV file from SII
     * @param tipoArchivo VENTA or COMPRA
     */
    public static List<RegistroRCV> parse(File file, TipoArchivo tipoArchivo) throws IOException {
        // Detectar codificación
        byte[] bytes = Files.readAllBytes(file.toPath());
        String encoding = detectEncoding(bytes);
        String content = new String(bytes, Charset.forName(encoding));

        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) return Collections.emptyList();

        // Buscar línea de cabecera (contiene "Tipo Doc" o "RUT")
        int headerIdx = -1;
        String[] headers = null;
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            String line = lines[i].trim();
            if (line.contains("Tipo Doc") || line.contains("RUT") || line.contains("Folio")) {
                headers = splitCsv(line);
                headerIdx = i;
                break;
            }
        }
        if (headers == null || headerIdx < 0) {
            throw new IOException("No se encontró la línea de encabezados en el archivo.");
        }

        // Construir mapa de índices de columnas
        Map<String, Integer> colIdx = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIdx.put(normalize(headers[i]), i);
        }

        List<RegistroRCV> result = new ArrayList<>();
        Tipo tipo = tipoArchivo == TipoArchivo.VENTA ? Tipo.VENTA : Tipo.COMPRA;

        // Validador de consistencia de columnas: detecta líneas con estructura anómala
        // que pueden indicar un CSV malformado o un separador mal detectado.
        // Tolerancia: ±3 columnas respecto al header (algunas líneas legítimas pueden
        // tener menos campos al final si son opcionales).
        int colsEsperadas = headers.length;
        int lineasInconsistentes = 0;
        int totalLineasDatos = 0;

        for (int i = headerIdx + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = splitCsv(line);

            // Validación de consistencia: contar líneas anómalas
            totalLineasDatos++;
            if (Math.abs(cols.length - colsEsperadas) > 3) {
                lineasInconsistentes++;
            }

            try {
                RegistroRCV reg = new RegistroRCV();
                reg.setTipo(tipo);

                // Tipo documento
                int tipoDoc = parseInt(getCol(cols, colIdx, "tipodoc", "tipo doc", "tipo_doc"));
                reg.setTipoDoc(tipoDoc);

                // Tipo compra/venta (Del Giro, Activo Fijo, etc.)
                reg.setTipoCompraVenta(getCol(cols, colIdx,
                    "tipocompraventa","tipo compra venta","tipo de compra","tipo de venta","tipodoc2"));

                // RUT contraparte
                reg.setRutContraparte(getCol(cols, colIdx,
                    "rutdoc","rut doc","rut del vendedor","rut del emisor","rut",
                    "rutemisor","rut emisor","rut proveedor","rutproveedor"));

                // Razón social
                reg.setRazonSocial(getCol(cols, colIdx,
                    "razonsocial","razon social","razón social","nombre o razón social",
                    "nombre","denominacion","denominación"));

                // Folio
                reg.setFolio(getCol(cols, colIdx, "folio","n° folio","numero folio"));

                // Fecha
                reg.setFechaDocto(getCol(cols, colIdx,
                    "fechadocto","fecha docto","fecha documento","fecha emisión","fechaemision"));

                // Montos
                reg.setMontoExento(parseMonto(getCol(cols, colIdx,
                    "montoexento","monto exento","exento")));
                reg.setMontoNeto(parseMonto(getCol(cols, colIdx,
                    "montoneto","monto neto","neto")));
                reg.setMontoIVARecuperable(parseMonto(getCol(cols, colIdx,
                    "montoivarecuperable","monto iva recuperable","iva recuperable",
                    "iva","montoiva","monto iva")));
                reg.setMontoIVANoRecuperable(parseMonto(getCol(cols, colIdx,
                    "montoivanorecuperable","monto iva no recuperable","iva no recuperable")));
                reg.setMontoTotal(parseMonto(getCol(cols, colIdx,
                    "montototal","monto total","total")));

                // Activo fijo (compras)
                reg.setMontoNetoActivoFijo(parseMonto(getCol(cols, colIdx,
                    "montonetoactiofijo","monto neto activo fijo","neto activo fijo",
                    "activofijo","activo fijo")));
                reg.setIvaActivoFijo(parseMonto(getCol(cols, colIdx,
                    "ivaactivofijo","iva activo fijo")));
                reg.setIvaUsoComun(parseMonto(getCol(cols, colIdx,
                    "ivausocomunproporcional","iva uso comun","uso comun")));

                // Anulado
                String est = getCol(cols, colIdx, "estado","estadodoc","anulado");
                reg.setAnulado("ANULADO".equalsIgnoreCase(est) || "1".equals(est));

                // Saltar anulados y guías de despacho
                if (reg.isAnulado()) continue;

                result.add(reg);
            } catch (Exception e) {
                System.err.println("Línea " + (i+1) + " ignorada: " + e.getMessage());
            }
        }

        // Almacenar reporte de consistencia para que el caller pueda mostrarlo al usuario
        ultimaInconsistenciaCsv = null;
        if (totalLineasDatos > 0) {
            double pctInconsistente = (lineasInconsistentes * 100.0) / totalLineasDatos;
            if (pctInconsistente > 5.0) {
                ultimaInconsistenciaCsv = "Se detectaron " + lineasInconsistentes +
                    " líneas con estructura inconsistente de " + totalLineasDatos +
                    " líneas totales (" + String.format("%.1f", pctInconsistente) + "%).\n" +
                    "El archivo CSV puede estar malformado o el separador puede haberse " +
                    "detectado incorrectamente. Los datos importados pueden estar incompletos.";
            }
        }

        return result;
    }

    /**
     * Reporte de inconsistencia del último parseo, o null si todo estaba OK.
     * El caller (ImportarRCVPanel) puede usar esto para mostrar una advertencia
     * al usuario después de la importación.
     */
    public static String getUltimaInconsistenciaCsv() {
        return ultimaInconsistenciaCsv;
    }

    private static String ultimaInconsistenciaCsv = null;

    /**
     * Agrega una lista de RegistroRCV en totales resumidos para un DatosMes.
     * Notas de crédito restan, notas de débito suman.
     * Guías de despacho have no effect.
     */
    public static AggregatedTotals aggregate(List<RegistroRCV> registros, Tipo tipo) {
        AggregatedTotals t = new AggregatedTotals();
        for (RegistroRCV r : registros) {
            if (!r.tieneEfectoContable()) continue;
            double sign = r.esNotaCredito() ? -1 : 1;

            if (tipo == Tipo.VENTA) {
                t.ventasAfectasNetas += sign * r.getMontoNeto();
                t.ventasExentasNetas += sign * r.getMontoExento();
                t.ivaDebito          += sign * r.getMontoIVARecuperable();
            } else {
                // Compras
                if (r.esActivoFijo()) {
                    t.activoFijoNeto += sign * (r.getMontoNetoActivoFijo() > 0
                                        ? r.getMontoNetoActivoFijo() : r.getMontoNeto());
                    t.ivaActivoFijo  += sign * (r.getIvaActivoFijo() > 0
                                        ? r.getIvaActivoFijo() : r.getMontoIVARecuperable());
                } else {
                    t.comprasAfectasNetas    += sign * r.getMontoNeto();
                    t.comprasExentasNetas    += sign * r.getMontoExento();
                    t.ivaCredFiscal          += sign * r.getMontoIVARecuperable();
                    t.ivaNoRecuperable       += sign * r.getMontoIVANoRecuperable();
                }
            }
        }
        return t;
    }

    public static class AggregatedTotals {
        // Ventas
        public double ventasAfectasNetas;
        public double ventasExentasNetas;
        public double ivaDebito;
        // Compras
        public double comprasAfectasNetas;
        public double comprasExentasNetas;
        public double ivaCredFiscal;
        public double ivaNoRecuperable;
        public double activoFijoNeto;
        public double ivaActivoFijo;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String detectEncoding(byte[] bytes) {
        // Verificar BOM UTF-8
        if (bytes.length >= 3 && bytes[0]==(byte)0xEF
                && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF) return "UTF-8";
        // Try to detect: if high bytes present, assume ISO-8859-1
        for (byte b : bytes) if ((b & 0xFF) > 0x7F) return "ISO-8859-1";
        return "UTF-8";
    }

    private static String[] splitCsv(String line) {
        // FRAGILIDAD CONOCIDA (v1.3.2):
        // El separador se detecta línea por línea con simple contains(";").
        // Edge case: si una línea tiene ambos separadores (ej. una razón social
        // entrecomillada que contiene ";" dentro), siempre gana el ";" sobre el ",".
        // En la práctica los archivos del SII usan un solo separador consistente,
        // por lo que esto no causa problemas reales. Si en el futuro se detecta
        // un caso problemático, una solución más robusta sería detectar el
        // separador desde el header (primera línea) o contar separadores fuera
        // de comillas y elegir el más frecuente.
        String sep = line.contains(";") ? ";" : ",";
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; }
            else if (!inQuotes && String.valueOf(c).equals(sep)) {
                result.add(cur.toString().trim()); cur = new StringBuilder();
            } else { cur.append(c); }
        }
        result.add(cur.toString().trim());
        return result.toArray(new String[0]);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase()
            .replace("á","a").replace("é","e").replace("í","i")
            .replace("ó","o").replace("ú","u").replace("ñ","n")
            .replace(" ","").replace("_","").replace("°","").replace("#","");
    }

    private static String getCol(String[] cols, Map<String,Integer> idx, String... keys) {
        for (String k : keys) {
            String nk = normalize(k);
            if (idx.containsKey(nk)) {
                int i = idx.get(nk);
                if (i < cols.length) return cols[i].replace("\"","").trim();
            }
        }
        return "";
    }

    private static double parseMonto(String s) {
        if (s == null || s.isEmpty() || s.equals("-")) return 0.0;
        s = s.replace("$","").replace(".","").replace(",",".").trim();
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private static int parseInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}

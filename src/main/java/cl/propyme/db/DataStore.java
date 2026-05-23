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
package cl.propyme.db;

import cl.propyme.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistencia simple basada en archivos JSON.
 * Todos los datos se almacenan en ./propyme-data/ relativo al JAR ejecutable.
 * Cada empresa tiene su propia carpeta: ./propyme-data/{rut}/
 *   empresa.json      - empresa + socios
 *   {anio}.json       - ejercicio + datos mensuales
 *
 * Correcciones de robustez v0.1.0 (sin cambios de formato):
 *   - Escritura atómica mediante archivo temporal + rename (evita corrupción ante cierres inesperados)
 *   - Corregido manejo de escapes en jget() y error off-by-one en validación de backslash
 *   - Corregido jstringArray() para manejar secuencias de escape en valores string
 *   - Corregido splitJsonObjects() para ignorar { y } dentro de literales string
 *   - Reemplazado lastIndexOf(']') por buscador de corchete de cierre correspondiente
 *   - Corregido guardarConfig(): Double.MAX_VALUE → número JSON válido
 *   - Corregido mpPPM: 0 (sin definir) se conserva como 0 y no se fuerza a 1
 *   - Agregadas validaciones null-safety y verificaciones de límites en todo el código
 *   - Logging mediante java.util.logging (sin dependencias externas)
 */
public class DataStore {

    private static final Logger LOG = Logger.getLogger(DataStore.class.getName());
    private static final String DATA_DIR = "propyme-data";

    /**
     * Errores no fatales acumulados durante la última carga de ejercicio.
     * Permite que la UI muestre un aviso al usuario si meses específicos
     * fallaron al parsearse (datos parcialmente recuperados).
     */
    private static final java.util.List<String> ultimosErroresMeses = new java.util.ArrayList<>();

    /** Retorna copia de los errores no fatales de la última carga. */
    public static java.util.List<String> getUltimosErroresCarga() {
        return new java.util.ArrayList<>(ultimosErroresMeses);
    }

    /**
     * Sentinel usado en GlobalConfig.igcTable[fila][1] (columna "hasta") para representar
     * el tramo superior sin límite (último tramo IGC). En memoria se usa Double.MAX_VALUE,
     * pero ese valor no es JSON válido, por lo que se serializa como IGC_SIN_LIMITE_JSON.
     * Al leer, cualquier valor >= IGC_SIN_LIMITE_UMBRAL se restaura a Double.MAX_VALUE.
     *
     * 1e15 supera con creces cualquier base imponible real posible en Chile
     * y es representable exactamente como número JSON.
     */
    private static final double IGC_SIN_LIMITE_JSON   = 1.0E15;
    private static final double IGC_SIN_LIMITE_UMBRAL = 1.0E14; // al leer: >= umbral → MAX_VALUE
    private final Path baseDir;

    public DataStore() {
        String jarDir = System.getProperty("app.home", System.getProperty("user.dir"));
        baseDir = Paths.get(jarDir, DATA_DIR);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data directory: " + baseDir, e);
        }
    }

    public Path getBaseDir() { return baseDir; }

    // ── Empresa ───────────────────────────────────────────────────────────────

    public List<Empresa> listarEmpresas() {
        List<Empresa> result = new ArrayList<>();
        File[] dirs = baseDir.toFile().listFiles(File::isDirectory);
        if (dirs == null) return result;
        for (File dir : dirs) {
            Path ep = dir.toPath().resolve("empresa.json");
            if (Files.exists(ep)) {
                try {
                    result.add(readEmpresa(ep));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error reading empresa: " + ep, e);
                }
            }
        }
        result.sort(Comparator.comparing(Empresa::getRazonSocial));
        return result;
    }

    public void guardarEmpresa(Empresa emp) throws IOException {
        Path dir = empresaDir(emp.getRut());
        Files.createDirectories(dir);
        writeAtomic(dir.resolve("empresa.json"), empresaToJson(emp));
    }

    public Empresa cargarEmpresa(String rut) throws IOException {
        Path ep = empresaDir(rut).resolve("empresa.json");
        if (!Files.exists(ep)) throw new FileNotFoundException("Empresa no encontrada: " + rut);
        return readEmpresa(ep);
    }

    public void eliminarEmpresa(String rut) throws IOException {
        deleteDirectory(empresaDir(rut).toFile());
    }

    // ── Ejercicio + DatosMes ─────────────────────────────────────────────────

    public List<Integer> listarEjercicios(String rut) {
        List<Integer> result = new ArrayList<>();
        File[] files = empresaDir(rut).toFile().listFiles(
            f -> f.getName().matches("\\d{4}\\.json"));
        if (files == null) return result;
        for (File f : files) {
            try {
                result.add(Integer.parseInt(f.getName().replace(".json", "")));
            } catch (NumberFormatException ignored) {}
        }
        result.sort(Comparator.reverseOrder());
        return result;
    }

    public void guardarEjercicio(String rut, Ejercicio ej,
                                  Map<Integer, DatosMes> datos) throws IOException {
        Path dir = empresaDir(rut);
        Files.createDirectories(dir);
        writeAtomic(dir.resolve(ej.getAnioComercial() + ".json"), ejercicioToJson(ej, datos));
    }

    public Object[] cargarEjercicio(String rut, int anio) throws IOException {
        Path fp = empresaDir(rut).resolve(anio + ".json");
        if (!Files.exists(fp)) return null;
        String json = readJsonValidando(fp);
        return parseEjercicio(json);
    }

    // ── Lectura validada de JSON ──────────────────────────────────────────────

    /**
     * Lee un archivo JSON desde disco verificando que parezca completo.
     * Detecta archivos truncados, vacíos o corruptos antes de parsearlos.
     * Esto previene que datos faltantes se interpreten silenciosamente como 0.
     *
     * Validaciones:
     *   - El archivo no está vacío
     *   - Empieza con '{' o '[' (después de espacios/BOM)
     *   - Termina con '}' o ']' (después de espacios)
     *   - Hay igual número de '{' que de '}' (no truncado a la mitad)
     */
    private static String readJsonValidando(Path path) throws IOException {
        String json = Files.readString(path);
        validarJson(json, path);
        return json;
    }

    private static void validarJson(String json, Path path) throws IOException {
        if (json == null || json.isEmpty()) {
            throw new IOException("Archivo JSON vacío: " + path);
        }
        String trimmed = json.trim();
        // Quitar BOM UTF-8 si está presente
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '\uFEFF') {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()) {
            throw new IOException("Archivo JSON vacío: " + path);
        }
        char primer = trimmed.charAt(0);
        char ultimo = trimmed.charAt(trimmed.length() - 1);
        if (primer != '{' && primer != '[') {
            throw new IOException("Archivo JSON corrupto (no empieza con { o [): " + path);
        }
        char cierreEsperado = (primer == '{') ? '}' : ']';
        if (ultimo != cierreEsperado) {
            throw new IOException("Archivo JSON corrupto o truncado (no termina con " +
                cierreEsperado + "): " + path);
        }
        // Conteo de llaves (ignorando contenido dentro de strings)
        int balance = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) { escape = false; continue; }
            if (inString) {
                if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') balance++;
            else if (c == '}') balance--;
        }
        if (balance != 0) {
            throw new IOException("Archivo JSON corrupto (llaves no balanceadas, " +
                "balance=" + balance + "): " + path);
        }
    }

    // ── Paths ─────────────────────────────────────────────────────────────────

    private Path empresaDir(String rut) {
        // Sanitize RUT for use as directory name
        return baseDir.resolve(rut.replace("/", "_").replace(".", "_"));
    }

    // ── Escritura atómica (previene archivos corruptos en crash/corte de energía) ─

    /**
     * Escribe contenido a un archivo temporal en el mismo directorio, luego lo
     * renombra atómicamente. Si la escritura falla, el archivo original no se toca.
     */
    private static void writeAtomic(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(tmp, content, java.nio.charset.StandardCharsets.UTF_8);
            // Rename atómico: en el mismo filesystem está garantizado como atómico en POSIX,
            // y best-effort en Windows (reemplaza archivo existente).
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                       StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback: no atómico pero aún más seguro que sobrescritura directa
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Limpiar archivo temporal si algo salió mal
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw e;
        }
    }

    // ── JSON Serialization ────────────────────────────────────────────────────

    private String empresaToJson(Empresa e) {
        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"rut\": ").append(jstr(e.getRut())).append(",\n");
        sb.append("  \"razonSocial\": ").append(jstr(e.getRazonSocial())).append(",\n");
        sb.append("  \"giro\": ").append(jstr(e.getGiro())).append(",\n");
        sb.append("  \"domicilio\": ").append(jstr(e.getDomicilio())).append(",\n");
        sb.append("  \"socios\": [\n");
        List<Socio> socios = e.getSocios() != null ? e.getSocios() : Collections.emptyList();
        for (int i = 0; i < socios.size(); i++) {
            Socio s = socios.get(i);
            sb.append("    {");
            sb.append("\"rut\":").append(jstr(s.getRut())).append(",");
            sb.append("\"nombre\":").append(jstr(s.getNombre())).append(",");
            sb.append("\"porcentaje\":").append(s.getPorcentaje()).append(",");
            sb.append("\"capitalAportado\":").append(s.getCapitalAportado()).append(",");
            sb.append("\"numeroCertificado\":").append(s.getNumeroCertificado());
            sb.append("}").append(i < socios.size()-1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    private Empresa readEmpresa(Path p) throws IOException {
        String json = readJsonValidando(p);
        Empresa e = new Empresa();
        e.setRut(jget(json, "rut"));
        e.setRazonSocial(jget(json, "razonSocial"));
        e.setGiro(jget(json, "giro"));
        e.setDomicilio(jget(json, "domicilio"));
        List<Socio> socios = new ArrayList<>();
        int arr = json.indexOf("\"socios\"");
        if (arr >= 0) {
            int start = json.indexOf('[', arr);
            if (start >= 0) {
                int end = matchingBracket(json, start, '[', ']');
                if (end > start) {
                    String arrStr = json.substring(start+1, end);
                    for (String obj : splitJsonObjects(arrStr)) {
                        if (obj.trim().isEmpty()) continue;
                        Socio s = new Socio();
                        s.setRut(jget(obj, "rut"));
                        s.setNombre(jget(obj, "nombre"));
                        s.setPorcentaje(jdouble(obj, "porcentaje"));
                        s.setCapitalAportado(jdouble(obj, "capitalAportado"));
                        s.setNumeroCertificado((int)jdouble(obj, "numeroCertificado"));
                        socios.add(s);
                    }
                }
            }
        }
        e.setSocios(socios);
        return e;
    }

    private String ejercicioToJson(Ejercicio ej, Map<Integer, DatosMes> datos) {
        if (datos == null) datos = new TreeMap<>();
        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"anio\":").append(ej.getAnioComercial()).append(",\n");
        sb.append("  \"cptsPositivoInicial\":").append(ej.getCptsPositivoInicial()).append(",\n");
        sb.append("  \"cptsNegativoInicial\":").append(ej.getCptsNegativoInicial()).append(",\n");
        sb.append("  \"perdidaAnterior\":").append(ej.getPerdidaAnterior()).append(",\n");
        sb.append("  \"existenciasAdeudadas\":").append(ej.getExistenciasAdeudadasAnteriores()).append(",\n");
        sb.append("  \"capitalInicial\":").append(ej.getCapitalInicial()).append(",\n");
        sb.append("  \"saldoIngresoDiferido\":").append(ej.getSaldoIngresoDiferido()).append(",\n");
        sb.append("  \"activoFijoFactor33bis\":").append(ej.getActivoFijoFactor33bis()).append(",\n");
        sb.append("  \"ufDiciembre\":").append(ej.getUfDiciembre()).append(",\n");
        sb.append("  \"utmDiciembre\":").append(ej.getUtmDiciembre()).append(",\n");
        // dividendos
        sb.append("  \"dividendoMonto\":[");
        double[] dm2 = ej.getDividendoMonto() != null ? ej.getDividendoMonto() : new double[5];
        for (int i=0;i<5;i++) sb.append(safeDouble(dm2.length>i?dm2[i]:0)).append(i<4?",":"");
        sb.append("],\n  \"dividendoCredito\":[");
        double[] dc = ej.getDividendoCredito() != null ? ej.getDividendoCredito() : new double[5];
        for (int i=0;i<5;i++) sb.append(safeDouble(dc.length>i?dc[i]:0)).append(i<4?",":"");
        sb.append("],\n  \"dividendoDetalle\":[");
        String[] dd = ej.getDividendoDetalle() != null ? ej.getDividendoDetalle() : new String[5];
        for (int i=0;i<5;i++) sb.append(jstr(dd.length>i?dd[i]:"")).append(i<4?",":"");
        sb.append("],\n");
        // meses
        sb.append("  \"meses\":[\n");
        List<Integer> meses = new ArrayList<>(datos.keySet());
        Collections.sort(meses);
        for (int mi = 0; mi < meses.size(); mi++) {
            DatosMes d = datos.get(meses.get(mi));
            if (d == null) continue;
            sb.append("    ").append(mesToJson(d));
            if (mi < meses.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    private String mesToJson(DatosMes dm) {
        return "{" +
            "\"mes\":" + dm.getMes() +
            ",\"vtaAfectas\":" + safeDouble(dm.getVentasAfectasNetas()) +
            ",\"ncVenta\":" + safeDouble(dm.getNotasCreditoVenta()) +
            ",\"ndVenta\":" + safeDouble(dm.getNotasDebitoVenta()) +
            ",\"vtaExentas\":" + safeDouble(dm.getVentasExentasIVA()) +
            ",\"ivaDebito\":" + safeDouble(dm.getIvaDebito()) +
            ",\"otrosIng\":" + safeDouble(dm.getOtrosIngresosPercibidos()) +
            ",\"noPercibido\":" + safeDouble(dm.getIngresosNoPercibidos()) +
            ",\"tasaPPM\":" + safeDouble(dm.getTasaPPM()) +
            ",\"compAfectas\":" + safeDouble(dm.getComprasAfectasNetas()) +
            ",\"ncCompra\":" + safeDouble(dm.getNotasCreditoCompra()) +
            ",\"ndCompra\":" + safeDouble(dm.getNotasDebitoCompra()) +
            ",\"ivaCF\":" + safeDouble(dm.getIvaCredFiscal()) +
            ",\"compExentas\":" + safeDouble(dm.getComprasExentasIVA()) +
            ",\"compNoRecNeto\":" + safeDouble(dm.getComprasAfectasIVAnoRecNeto()) +
            ",\"activoFijo\":" + safeDouble(dm.getActivoFijoPagado()) +
            ",\"remun\":" + safeDouble(dm.getRemuneracionesPagadas()) +
            ",\"honor\":" + safeDouble(dm.getHonorariosPagados()) +
            ",\"arriendos\":" + safeDouble(dm.getArrendosPagados()) +
            ",\"gastos\":" + safeDouble(dm.getGastosGenerales()) +
            ",\"otrosEg\":" + safeDouble(dm.getOtrosEgresos()) +
            ",\"adeudados\":" + safeDouble(dm.getEgresosAdeudados()) +
            ",\"retS1\":" + safeDouble(dm.getRetiroSocio1Historico()) +
            ",\"retS2\":" + safeDouble(dm.getRetiroSocio2Historico()) +
            ",\"factorPPM\":" + safeDouble(dm.getFactorReajustePPM()) +
            "}";
    }

    @SuppressWarnings("unchecked")
    private Object[] parseEjercicio(String json) {
        ultimosErroresMeses.clear();
        Ejercicio ej = new Ejercicio();
        ej.setAnioComercial((int)jdouble(json, "anio"));
        ej.setCptsPositivoInicial(jdouble(json, "cptsPositivoInicial"));
        ej.setCptsNegativoInicial(jdouble(json, "cptsNegativoInicial"));
        ej.setPerdidaAnterior(jdouble(json, "perdidaAnterior"));
        ej.setExistenciasAdeudadasAnteriores(jdouble(json, "existenciasAdeudadas"));
        ej.setCapitalInicial(jdouble(json, "capitalInicial"));
        ej.setSaldoIngresoDiferido(jdouble(json, "saldoIngresoDiferido"));
        ej.setActivoFijoFactor33bis(jdouble(json, "activoFijoFactor33bis"));
        ej.setUfDiciembre(jdouble(json, "ufDiciembre"));
        ej.setUtmDiciembre(jdouble(json, "utmDiciembre"));

        double[] dividendoMonto2  = jdoubleArray(json, "dividendoMonto", 5);
        double[] dc = jdoubleArray(json, "dividendoCredito", 5);
        String[] dd = jstringArray(json, "dividendoDetalle", 5);
        ej.setDividendoMonto(dividendoMonto2);
        ej.setDividendoCredito(dc);
        ej.setDividendoDetalle(dd);

        Map<Integer, DatosMes> datos = new TreeMap<>();
        int mArr = json.indexOf("\"meses\"");
        if (mArr >= 0) {
            int start = json.indexOf('[', mArr);
            if (start >= 0) {
                int end = matchingBracket(json, start, '[', ']');
                if (end > start) {
                    for (String obj : splitJsonObjects(json.substring(start+1, end))) {
                        if (obj.trim().isEmpty()) continue;
                        try {
                            DatosMes mes = new DatosMes();
                            mes.setMes((int)jdouble(obj, "mes"));
                            if (mes.getMes() < 1 || mes.getMes() > 12) continue; // sanity
                            mes.setVentasAfectasNetas(jdouble(obj, "vtaAfectas"));
                            mes.setNotasCreditoVenta(jdouble(obj, "ncVenta"));
                            mes.setNotasDebitoVenta(jdouble(obj, "ndVenta"));
                            mes.setVentasExentasIVA(jdouble(obj, "vtaExentas"));
                            mes.setIvaDebito(jdouble(obj, "ivaDebito"));
                            mes.setOtrosIngresosPercibidos(jdouble(obj, "otrosIng"));
                            mes.setIngresosNoPercibidos(jdouble(obj, "noPercibido"));
                            mes.setTasaPPM(jdouble(obj, "tasaPPM"));
                            mes.setComprasAfectasNetas(jdouble(obj, "compAfectas"));
                            mes.setNotasCreditoCompra(jdouble(obj, "ncCompra"));
                            mes.setNotasDebitoCompra(jdouble(obj, "ndCompra"));
                            mes.setIvaCredFiscal(jdouble(obj, "ivaCF"));
                            mes.setComprasExentasIVA(jdouble(obj, "compExentas"));
                            mes.setComprasAfectasIVAnoRecNeto(jdouble(obj, "compNoRecNeto"));
                            mes.setActivoFijoPagado(jdouble(obj, "activoFijo"));
                            mes.setRemuneracionesPagadas(jdouble(obj, "remun"));
                            mes.setHonorariosPagados(jdouble(obj, "honor"));
                            mes.setArrendosPagados(jdouble(obj, "arriendos"));
                            mes.setGastosGenerales(jdouble(obj, "gastos"));
                            mes.setOtrosEgresos(jdouble(obj, "otrosEg"));
                            mes.setEgresosAdeudados(jdouble(obj, "adeudados"));
                            mes.setRetiroSocio1Historico(jdouble(obj, "retS1"));
                            mes.setRetiroSocio2Historico(jdouble(obj, "retS2"));
                            mes.setFactorReajustePPM(jdouble(obj, "factorPPM"));
                            datos.put(mes.getMes(), mes);
                        } catch (Exception ex) {
                            String preview = obj.substring(0, Math.min(60, obj.length()));
                            LOG.log(Level.WARNING, "Error parsing DatosMes object, skipping: " + preview, ex);
                            ultimosErroresMeses.add("Mes con error al cargar: " + preview);
                        }
                    }
                }
            }
        }
        return new Object[]{ej, datos};
    }

    // ── GlobalConfig ──────────────────────────────────────────────────────────

    public GlobalConfig cargarConfig(int anio) {
        Path p = baseDir.resolve("config").resolve("ipc_" + anio + ".json");
        if (!Files.exists(p)) return new GlobalConfig(anio);
        try {
            String json = readJsonValidando(p);
            GlobalConfig cfg = new GlobalConfig(anio);
            double[] f = jdoubleArray(json, "factores", 13);
            cfg.setFactores(f);
            // Parsear tabla IGC
            double[][] igc = new double[8][4];
            int arrIdx = json.indexOf("\"igcTable\"");
            if (arrIdx >= 0) {
                int start = json.indexOf('[', arrIdx);
                if (start >= 0) {
                    int end = matchingBracket(json, start, '[', ']');
                    if (end > start) {
                        String arrStr = json.substring(start+1, end);
                        List<String> rows = splitJsonArrays(arrStr);
                        for (int i = 0; i < Math.min(rows.size(), 8); i++) {
                            String row = rows.get(i).trim().replace("[","").replace("]","");
                            String[] vals = row.split(",");
                            for (int j = 0; j < Math.min(vals.length, 4); j++) {
                                try {
                                    double v = Double.parseDouble(vals[j].trim());
                                    igc[i][j] = (v >= IGC_SIN_LIMITE_UMBRAL) ? Double.MAX_VALUE : v;
                                } catch (NumberFormatException ex) {}
                            }
                        }
                        cfg.setIgcTable(igc);
                    }
                }
            }
            return cfg;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error loading config for year " + anio + ", using defaults", e);
            return new GlobalConfig(anio);
        }
    }

    public void guardarConfig(GlobalConfig cfg) throws IOException {
        Path dir = baseDir.resolve("config");
        Files.createDirectories(dir);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"anio\":").append(cfg.getAnio());
        sb.append(",\n  \"factores\":[");
        double[] factores = cfg.getFactores();
        if (factores == null) factores = new double[13];
        for (int i = 0; i <= 12; i++) {
            sb.append(safeDouble(factores.length > i ? factores[i] : 1.0)).append(i < 12 ? "," : "");
        }
        sb.append("],\n  \"igcTable\":[");
        double[][] igc = cfg.getIgcTable();
        if (igc == null) igc = new double[0][0];
        for (int i = 0; i < igc.length; i++) {
            sb.append("[");
            sb.append(safeDouble(igc[i][0])).append(",");
            // Double.MAX_VALUE no es JSON válido — se serializa como IGC_SIN_LIMITE_JSON
            double hasta = igc[i].length > 1 ? igc[i][1] : 0;
            sb.append(hasta == Double.MAX_VALUE ? IGC_SIN_LIMITE_JSON : safeDouble(hasta));
            sb.append(",").append(igc[i].length > 2 ? safeDouble(igc[i][2]) : 0);
            sb.append(",").append(igc[i].length > 3 ? safeDouble(igc[i][3]) : 0);
            sb.append("]").append(i < igc.length-1 ? "," : "");
        }
        sb.append("]\n}");
        writeAtomic(dir.resolve("ipc_" + cfg.getAnio() + ".json"), sb.toString());
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    /**
     * Serialize a double to JSON string, guarding against NaN/Infinity.
     */
    private static String safeDouble(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0";
        // Avoid scientific notation for monetary amounts where possible
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long)v);
        return String.valueOf(v);
    }

    /**
     * Escapa un valor de tipo string para JSON.
     * Maneja: backslash, comillas dobles, salto de línea, retorno de carro, tab.
     */
    private static String jstr(String v) {
        if (v == null) return "null";
        StringBuilder sb = new StringBuilder(v.length() + 4);
        sb.append('"');
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Extrae un valor (string o número) para una clave dada desde un objeto JSON plano.
     * Maneja caracteres escapados en valores de tipo string.
     * NO recursa en objetos/arrays anidados.
     */
    private static String jget(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return "";
        char first = json.charAt(start);
        if (first == '"') {
            // Valor de string: parsear manejando escapes
            StringBuilder sb = new StringBuilder();
            int i = start + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char esc = json.charAt(i + 1);
                    switch (esc) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (i + 5 < json.length()) {
                                try {
                                    int cp = Integer.parseInt(json.substring(i+2, i+6), 16);
                                    sb.append((char) cp);
                                    i += 4;
                                } catch (NumberFormatException e) {
                                    sb.append(esc);
                                }
                            }
                            break;
                        default:   sb.append(esc); break;
                    }
                    i += 2;
                } else if (c == '"') {
                    break; // end of string
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        } else if (first == 'n') {
            return ""; // null
        } else {
            // Numeric or boolean value
            int end = start;
            while (end < json.length() && ",}\n]".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }
    }

    private static double jdouble(String json, String key) {
        String v = jget(json, key);
        if (v.isEmpty()) return 0.0;
        try { return Double.parseDouble(v); } catch (Exception e) { return 0.0; }
    }

    /**
     * Parsea un array numérico simple (sin objetos anidados) para una clave dada.
     */
    private static double[] jdoubleArray(String json, String key, int size) {
        double[] result = new double[size];
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return result;
        int arr = json.indexOf('[', idx);
        if (arr < 0) return result;
        int end = matchingBracket(json, arr, '[', ']');
        if (end < 0) return result;
        String[] parts = json.substring(arr+1, end).split(",");
        for (int i = 0; i < Math.min(parts.length, size); i++) {
            try { result[i] = Double.parseDouble(parts[i].trim()); } catch (Exception e) {}
        }
        return result;
    }

    /**
     * Parsea un array de strings para una clave dada, manejando secuencias de escape.
     */
    private static String[] jstringArray(String json, String key, int size) {
        String[] result = new String[size];
        Arrays.fill(result, "");
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return result;
        int arr = json.indexOf('[', idx);
        if (arr < 0) return result;
        int end = matchingBracket(json, arr, '[', ']');
        if (end < 0) return result;
        // Parse each string element using full escape handling
        List<String> items = new ArrayList<>();
        int pos = arr + 1;
        while (pos < end) {
            char c = json.charAt(pos);
            if (c == '"') {
                // Usar jgetAtPos para parsear un string con escapes entre comillas
                StringBuilder sb = new StringBuilder();
                int i = pos + 1;
                while (i < end) {
                    char ch = json.charAt(i);
                    if (ch == '\\' && i + 1 < end) {
                        char esc = json.charAt(i + 1);
                        switch (esc) {
                            case '"':  sb.append('"');  break;
                            case '\\': sb.append('\\'); break;
                            case 'n':  sb.append('\n'); break;
                            case 'r':  sb.append('\r'); break;
                            case 't':  sb.append('\t'); break;
                            default:   sb.append(esc); break;
                        }
                        i += 2;
                    } else if (ch == '"') {
                        pos = i + 1; // advance outer pos past closing quote
                        break;
                    } else {
                        sb.append(ch);
                        i++;
                    }
                }
                // FIX: Si el ciclo termina sin encontrar la comilla de cierre
                // (string malformado), forzar avance de pos para evitar loop infinito.
                if (i == end) pos = end;
                items.add(sb.toString());
            } else {
                pos++;
            }
        }
        for (int i = 0; i < Math.min(items.size(), size); i++) result[i] = items.get(i);
        return result;
    }

    /**
     * Splits a JSON array body into individual {...} objects.
     * CORRECCIÓN: ignora correctamente '{' y '}' que aparezcan dentro de literales string.
     */
    private static List<String> splitJsonObjects(String arrBody) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inStr = false;
        for (int i = 0; i < arrBody.length(); i++) {
            char c = arrBody.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; } // skip escaped char
                else if (c == '"') { inStr = false; }
            } else {
                if (c == '"') { inStr = true; }
                else if (c == '{') { if (depth == 0) start = i; depth++; }
                else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        result.add(arrBody.substring(start, i+1));
                        start = -1;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Splits a JSON array body into sub-array [...] elements.
     * Usado para el parseo de igcTable.
     */
    private static List<String> splitJsonArrays(String s) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; }
                else if (c == '"') { inStr = false; }
            } else {
                if (c == '"') { inStr = true; }
                else if (c == '[') { if (depth == 0) start = i; depth++; }
                else if (c == ']') {
                    depth--;
                    if (depth == 0 && start >= 0) { result.add(s.substring(start, i+1)); start = -1; }
                }
            }
        }
        return result;
    }

    /**
     * Encuentra el corchete de cierre correspondiente al corchete de apertura en la posición 'open'.
     * Maneja correctamente strings (ignora brackets dentro de comillas).
     * Retorna -1 si no se encuentra.
     */
    private static int matchingBracket(String s, int open, char openCh, char closeCh) {
        int depth = 0;
        boolean inStr = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; } // skip escape
                else if (c == '"') { inStr = false; }
            } else {
                if (c == '"') { inStr = true; }
                else if (c == openCh) { depth++; }
                else if (c == closeCh) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * Elimina recursivamente un directorio. Valida el retorno de cada delete()
     * y agrupa los fallos en una IOException con la lista de archivos no eliminados.
     * Esto evita estado inconsistente (carpetas parcialmente borradas sin aviso).
     */
    private void deleteDirectory(File dir) throws IOException {
        java.util.List<String> fallidos = new java.util.ArrayList<>();
        deleteDirectoryInterno(dir, fallidos);
        if (!fallidos.isEmpty()) {
            throw new IOException("No se pudieron eliminar " + fallidos.size() +
                " archivo(s)/carpeta(s):\n  " + String.join("\n  ", fallidos));
        }
    }

    private void deleteDirectoryInterno(File dir, java.util.List<String> fallidos) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryInterno(f, fallidos);
                } else if (!f.delete()) {
                    fallidos.add(f.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) {
            fallidos.add(dir.getAbsolutePath());
        }
    }
}

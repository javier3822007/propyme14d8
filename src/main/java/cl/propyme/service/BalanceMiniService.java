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

import cl.propyme.model.Resultados;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BalanceMiniService — Balance de 8 Columnas tributario.
 *
 * Módulo completamente independiente. Solo LEE desde Resultados,
 * nunca escribe en él ni en ningún otro objeto del sistema.
 *
 * Lógica contable (partida doble):
 *   Ingresos  → Crédito → Saldo Acreedor → Resultado Ganancias
 *   Egresos   → Débito  → Saldo Deudor   → Resultado Pérdidas
 *   Activos   → Débito  → Saldo Deudor   → Inventario Activo
 *   Pasivos   → Crédito → Saldo Acreedor → Inventario Pasivo
 *   Capital   → Crédito → Saldo Acreedor → Inventario Pasivo
 *   Retiros   → Débito  → Saldo Deudor   → Inventario Activo
 *
 * Persistencia propia: propyme-data/{rut}/balance_{anio}.json
 */
public class BalanceMiniService {

    // ── Tipo de cuenta ────────────────────────────────────────────────────────

    public enum TipoCuenta {
        ACTIVO    ("Activo"),
        PASIVO    ("Pasivo"),
        CAPITAL   ("Capital"),
        RETIRO    ("Retiro"),
        INGRESO   ("Ingreso"),
        EGRESO    ("Egreso");

        public final String etiqueta;
        TipoCuenta(String e) { this.etiqueta = e; }

        public static TipoCuenta fromString(String s) {
            if (s == null) return EGRESO;
            for (TipoCuenta t : values())
                if (t.name().equals(s) || t.etiqueta.equalsIgnoreCase(s)) return t;
            return EGRESO;
        }
    }

    // ── Fila ──────────────────────────────────────────────────────────────────

    public static class FilaBalance {
        public String     cuenta       = "";
        public TipoCuenta tipo         = TipoCuenta.EGRESO;
        public double     sumasDebito  = 0;
        public double     sumasCredito = 0;
        // Los siguientes se calculan automáticamente para filas auto;
        // para filas manuales el usuario los ingresa directamente.
        public double     saldoDeudor   = 0;
        public double     saldoAcreedor = 0;
        public double     invActivo     = 0;
        public double     invPasivo     = 0;
        public double     resPerdidas   = 0;
        public double     resGanancias  = 0;
        public boolean    esAuto        = true; // false = ingresada manualmente

        public FilaBalance() {}

        /**
         * Constructor para filas auto-generadas.
         * Calcula todas las columnas siguiendo la lógica contable del tipo.
         */
        public FilaBalance(String cuenta, TipoCuenta tipo,
                           double debito, double credito) {
            this.cuenta       = cuenta;
            this.tipo         = tipo;
            this.sumasDebito  = debito;
            this.sumasCredito = credito;
            this.esAuto       = true;
            calcularSaldos();
        }

        /** Recalcula saldos e inventario/resultado a partir de débito/crédito y tipo. */
        public void calcularSaldos() {
            double dif = sumasDebito - sumasCredito;
            saldoDeudor   = dif > 0 ?  dif : 0;
            saldoAcreedor = dif < 0 ? -dif : 0;

            invActivo    = 0;
            invPasivo    = 0;
            resPerdidas  = 0;
            resGanancias = 0;

            switch (tipo) {
                case ACTIVO:
                    invActivo   = saldoDeudor;
                    invPasivo   = saldoAcreedor; // saldo acreedor en activo = corrección
                    break;
                case PASIVO:
                case CAPITAL:
                    invPasivo   = saldoAcreedor;
                    invActivo   = saldoDeudor;   // saldo deudor en pasivo = corrección
                    break;
                case RETIRO:
                    invActivo   = saldoDeudor;   // retiros reducen patrimonio → activo
                    break;
                case INGRESO:
                    resGanancias = saldoAcreedor;
                    break;
                case EGRESO:
                    resPerdidas  = saldoDeudor;
                    break;
            }
        }
    }

    // ── Totales ───────────────────────────────────────────────────────────────

    public static class Totales {
        public double sumasDebito;
        public double sumasCredito;
        public double saldoDeudor;
        public double saldoAcreedor;
        public double invActivo;
        public double invPasivo;
        public double resPerdidas;
        public double resGanancias;
        // Utilidad o pérdida del ejercicio (fila de cierre)
        public double utilidad;   // > 0 = utilidad, < 0 = pérdida
        // Cuadra la partida doble?
        public boolean cuadra;
        public double  diferencia;
    }

    // ── Construcción desde Resultados ─────────────────────────────────────────

    public static List<FilaBalance> construirDesdeResultados(Resultados r) {
        List<FilaBalance> filas = new ArrayList<>();

        // Precalcular capital neto (necesario para Banco/Caja más abajo)
        // cpts1573 = capital aportado empresas que inician actividades (Cód. 1573)
        // Se suma al CPTS positivo — un contador no debería ingresar ambos simultáneamente
        double capitalNeto = r.cpts1580 - r.cpts1582 + r.cpts1573;

        // ── 1. ACTIVO ─────────────────────────────────────────────────────────
        // Banco/Caja va primero — es el activo más líquido (se calcula al final
        // pero se inserta aquí; usamos un índice para insertarlo en posición 0)
        // PPM por Recuperar
        if (r.ppmHistoricoTotal != 0)
            agregar(filas, "PPM por Recuperar",
                    TipoCuenta.ACTIVO, r.ppmHistoricoTotal, 0);

        // ── 2. PASIVO ────────────────────────────────────────────────────────
        // (sin cuentas de pasivo en 14D8 puro; el usuario puede agregar manualmente)

        // ── 3. PATRIMONIO ────────────────────────────────────────────────────
        // Capital Propio Tributario inicial
        // cpts1580 = positivo, cpts1582 = negativo (valor absoluto)
        // Si capitalNeto > 0 → Crédito → Inventario Pasivo (normal)
        // Si capitalNeto < 0 → Débito  → Inventario Activo (patrimonio en déficit)
        if (capitalNeto > 0)
            agregar(filas, "Capital Propio Tributario inicial",
                    TipoCuenta.CAPITAL, 0, capitalNeto);
        else if (capitalNeto < 0)
            agregar(filas, "Capital Propio Tributario inicial (déficit)",
                    TipoCuenta.CAPITAL, -capitalNeto, 0);

        // Retiros de propietarios
        if (r.cpts1576 != 0)
            agregar(filas, "Retiros de propietarios",
                    TipoCuenta.RETIRO, r.cpts1576, 0);

        // ── 4. RESULTADO PÉRDIDAS (Egresos) ──────────────────────────────────
        if (r.eg1614 != 0)
            agregar(filas, "Existencias e insumos pagados (Cód. 1614)",
                    TipoCuenta.EGRESO, r.eg1614, 0);
        if (r.eg1820 != 0)
            agregar(filas, "Existencias adeudadas año anterior (Cód. 1820)",
                    TipoCuenta.EGRESO, r.eg1820, 0);
        if (r.eg1616 != 0)
            agregar(filas, "Remuneraciones pagadas (Cód. 1616)",
                    TipoCuenta.EGRESO, r.eg1616, 0);
        if (r.eg1617 != 0)
            agregar(filas, "Honorarios pagados (Cód. 1617)",
                    TipoCuenta.EGRESO, r.eg1617, 0);
        if (r.eg1618 != 0)
            agregar(filas, "Activo fijo / dep. instantánea (Cód. 1618)",
                    TipoCuenta.EGRESO, r.eg1618, 0);
        if (r.eg1620 != 0)
            agregar(filas, "Arriendos pagados (Cód. 1620)",
                    TipoCuenta.EGRESO, r.eg1620, 0);
        if (r.eg1622 != 0)
            agregar(filas, "Intereses y reajustes pagados (Cód. 1622)",
                    TipoCuenta.EGRESO, r.eg1622, 0);
        if (r.eg1625 != 0)
            agregar(filas, "Otros gastos deducibles (Cód. 1625)",
                    TipoCuenta.EGRESO, r.eg1625, 0);
        if (r.eg1627 != 0)
            agregar(filas, "Pérdida tributaria ejercicio anterior (Cód. 1627)",
                    TipoCuenta.EGRESO, r.eg1627, 0);
        if (r.eg1628 != 0)
            agregar(filas, "Créditos incobrables (Cód. 1628)",
                    TipoCuenta.EGRESO, r.eg1628, 0);
        if (r.eg1615 != 0)
            agregar(filas, "Gastos fuente extranjera (Cód. 1615)",
                    TipoCuenta.EGRESO, r.eg1615, 0);
        if (r.eg1621 != 0)
            agregar(filas, "Gastos medioambientales (Cód. 1621)",
                    TipoCuenta.EGRESO, r.eg1621, 0);
        if (r.eg1624 != 0)
            agregar(filas, "Pérdida en inversiones (Cód. 1624)",
                    TipoCuenta.EGRESO, r.eg1624, 0);
        if (r.eg1626 != 0)
            agregar(filas, "Gastos empresas relacionadas (Cód. 1626)",
                    TipoCuenta.EGRESO, r.eg1626, 0);
        if (r.eg1909 != 0)
            agregar(filas, "Donaciones (Cód. 1909)",
                    TipoCuenta.EGRESO, r.eg1909, 0);

        // ── 5. RESULTADO GANANCIAS (Ingresos) ────────────────────────────────
        agregar(filas, "Ingresos del giro percibidos (Cód. 1600)",
                TipoCuenta.INGRESO, 0, r.ing1600);
        if (r.ing1819 != 0)
            agregar(filas, "Devengados ejercicio anterior (Cód. 1819)",
                    TipoCuenta.INGRESO, 0, r.ing1819);
        if (r.ing1601 != 0)
            agregar(filas, "Rentas fuente extranjera (Cód. 1601)",
                    TipoCuenta.INGRESO, 0, r.ing1601);
        if (r.ing1602 != 0)
            agregar(filas, "Intereses y reajustes (Cód. 1602)",
                    TipoCuenta.INGRESO, 0, r.ing1602);
        if (r.ing1603 != 0)
            agregar(filas, "Mayor valor inversiones (Cód. 1603)",
                    TipoCuenta.INGRESO, 0, r.ing1603);
        if (r.ing1604 != 0)
            agregar(filas, "Dividendos y participaciones (Cód. 1604)",
                    TipoCuenta.INGRESO, 0, r.ing1604);
        if (r.ing1605 != 0)
            agregar(filas, "Incremento IDPC (Cód. 1605)",
                    TipoCuenta.INGRESO, 0, r.ing1605);
        if (r.ing1606 != 0)
            agregar(filas, "Devengados empresas relacionadas (Cód. 1606)",
                    TipoCuenta.INGRESO, 0, r.ing1606);
        if (r.ing1607 != 0)
            agregar(filas, "Otros ingresos / reajuste PPM (Cód. 1607)",
                    TipoCuenta.INGRESO, 0, r.ing1607);
        if (r.ing1608 != 0)
            agregar(filas, "Ingreso diferido imputado (Cód. 1608)",
                    TipoCuenta.INGRESO, 0, r.ing1608);
        if (r.ing1609 != 0)
            agregar(filas, "Crédito Art. 33 bis LIR (Cód. 1609)",
                    TipoCuenta.INGRESO, 0, r.ing1609);

        // ── Banco / Caja consolidado — contrapartida universal ────────────────
        // Lógica contable: el balance muestra saldos finales, no asientos.
        // Banco/Caja absorbe la contrapartida de TODAS las cuentas:
        //   Débito  = entradas (ingresos percibidos + aportes de capital)
        //   Crédito = salidas  (egresos pagados + retiros + PPM pagado)
        double bancoDebito  = 0;
        double bancoCredito = 0;

        // Entradas (Débito a Banco/Caja)
        bancoDebito += r.ing1600;
        bancoDebito += r.ing1819;
        bancoDebito += r.ing1601;
        bancoDebito += r.ing1602;
        bancoDebito += r.ing1603;
        bancoDebito += r.ing1604;
        bancoDebito += r.ing1605;
        bancoDebito += r.ing1606;
        bancoDebito += r.ing1607;
        bancoDebito += r.ing1608;
        bancoDebito += r.ing1609;
        if (capitalNeto > 0) bancoDebito += capitalNeto;

        // Salidas (Crédito a Banco/Caja)
        bancoCredito += r.eg1614;
        bancoCredito += r.eg1820;
        bancoCredito += r.eg1616;
        bancoCredito += r.eg1617;
        bancoCredito += r.eg1618;
        bancoCredito += r.eg1620;
        bancoCredito += r.eg1622;
        bancoCredito += r.eg1625;
        bancoCredito += r.eg1627;
        bancoCredito += r.eg1628;
        bancoCredito += r.eg1615;
        bancoCredito += r.eg1621;
        bancoCredito += r.eg1624;
        bancoCredito += r.eg1626;
        bancoCredito += r.eg1909;
        bancoCredito += r.cpts1576;          // retiros
        bancoCredito += r.ppmHistoricoTotal; // PPM pagado
        if (capitalNeto < 0) bancoCredito += -capitalNeto; // déficit de capital

        if (bancoDebito != 0 || bancoCredito != 0)
            filas.add(0, new FilaBalance("Banco / Caja",
                    TipoCuenta.ACTIVO, bancoDebito, bancoCredito));

        return filas;
    }

    private static void agregar(List<FilaBalance> lista, String cuenta,
                                TipoCuenta tipo, double debito, double credito) {
        lista.add(new FilaBalance(cuenta, tipo, debito, credito));
    }

    // ── Cálculo de totales y cuadre ───────────────────────────────────────────

    public static Totales calcularTotales(List<FilaBalance> filas) {
        Totales t = new Totales();
        for (FilaBalance f : filas) {
            t.sumasDebito   += f.sumasDebito;
            t.sumasCredito  += f.sumasCredito;
            t.saldoDeudor   += f.saldoDeudor;
            t.saldoAcreedor += f.saldoAcreedor;
            t.invActivo     += f.invActivo;
            t.invPasivo     += f.invPasivo;
            t.resPerdidas   += f.resPerdidas;
            t.resGanancias  += f.resGanancias;
        }
        // Utilidad = Ganancias - Pérdidas
        // Si positivo → utilidad → va a Inventario Pasivo y Resultado Pérdidas
        // Si negativo → pérdida  → va a Inventario Activo y Resultado Ganancias
        // Utilidad redondeada para evitar basura flotante en escenarios cercanos a cero
        t.utilidad = Math.round(t.resGanancias - t.resPerdidas);

        // Verificar cuadre de partida doble (Sumas Totales deben igualar)
        double totalInvActivo  = t.invActivo  + (t.utilidad < 0 ? -t.utilidad : 0);
        double totalInvPasivo  = t.invPasivo  + (t.utilidad > 0 ?  t.utilidad : 0);
        double totalResPerd    = t.resPerdidas  + (t.utilidad > 0 ?  t.utilidad : 0);
        double totalResGan     = t.resGanancias + (t.utilidad < 0 ? -t.utilidad : 0);

        // Diferencia en Sumas Totales finales
        t.diferencia = Math.abs(t.sumasDebito - t.sumasCredito);
        t.cuadra = t.diferencia < 1.0
                && Math.abs(totalInvActivo - totalInvPasivo) < 1.0
                && Math.abs(totalResPerd   - totalResGan)    < 1.0;

        return t;
    }

    // ── Persistencia propia ───────────────────────────────────────────────────

    private static Path rutaArchivo(String baseDir, String rut, int anio) {
        String safeRut = rut.replace("/", "_").replace(".", "_");
        return Paths.get(baseDir, "propyme-data", safeRut, "balance_" + anio + ".json");
    }

    public static void guardar(String baseDir, String rut, int anio,
                               List<FilaBalance> filas) throws IOException {
        Path path = rutaArchivo(baseDir, rut, anio);
        Files.createDirectories(path.getParent());

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < filas.size(); i++) {
            FilaBalance f = filas.get(i);
            sb.append("  {");
            sb.append("\"cuenta\":").append(jstr(f.cuenta)).append(",");
            sb.append("\"tipo\":\"").append(f.tipo.name()).append("\",");
            sb.append("\"sd\":").append(f.sumasDebito).append(",");
            sb.append("\"sc\":").append(f.sumasCredito).append(",");
            sb.append("\"svd\":").append(f.saldoDeudor).append(",");
            sb.append("\"sva\":").append(f.saldoAcreedor).append(",");
            sb.append("\"ia\":").append(f.invActivo).append(",");
            sb.append("\"ip\":").append(f.invPasivo).append(",");
            sb.append("\"rp\":").append(f.resPerdidas).append(",");
            sb.append("\"rg\":").append(f.resGanancias).append(",");
            sb.append("\"auto\":").append(f.esAuto);
            sb.append("}");
            if (i < filas.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");

        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tmp, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING,
                   StandardCopyOption.ATOMIC_MOVE);
    }

    public static List<FilaBalance> cargar(String baseDir, String rut,
                                           int anio) throws IOException {
        Path path = rutaArchivo(baseDir, rut, anio);
        if (!Files.exists(path)) return null;
        String json = Files.readString(path,
                          java.nio.charset.StandardCharsets.UTF_8).trim();
        List<FilaBalance> filas = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inStr = false;
        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"' && (i == 0 || json.charAt(i-1) != '\\'))
                inStr = !inStr;
            if (!inStr) {
                if (ch == '{') { if (depth++ == 0) start = i; }
                else if (ch == '}') {
                    if (--depth == 0 && start >= 0) {
                        filas.add(parseFila(json.substring(start, i + 1)));
                        start = -1;
                    }
                }
            }
        }
        return filas;
    }

    public static boolean existeArchivo(String baseDir, String rut, int anio) {
        return Files.exists(rutaArchivo(baseDir, rut, anio));
    }

    // ── Helpers JSON ──────────────────────────────────────────────────────────

    private static String jstr(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                       .replace("\n","\\n").replace("\r","\\r") + "\"";
    }

    private static FilaBalance parseFila(String obj) {
        FilaBalance f = new FilaBalance();
        f.cuenta       = jget(obj, "cuenta");
        f.tipo         = TipoCuenta.fromString(jget(obj, "tipo"));
        f.sumasDebito  = jdbl(obj, "sd");
        f.sumasCredito = jdbl(obj, "sc");
        f.saldoDeudor  = jdbl(obj, "svd");
        f.saldoAcreedor= jdbl(obj, "sva");
        f.invActivo    = jdbl(obj, "ia");
        f.invPasivo    = jdbl(obj, "ip");
        f.resPerdidas  = jdbl(obj, "rp");
        f.resGanancias = jdbl(obj, "rg");
        f.esAuto       = "true".equals(jget(obj, "auto"));
        return f;
    }

    private static double jdbl(String obj, String key) {
        try { return Double.parseDouble(jget(obj, key)); }
        catch (Exception e) { return 0; }
    }

    private static String jget(String obj, String key) {
        String search = "\"" + key + "\":";
        int idx = obj.indexOf(search);
        if (idx < 0) return "";
        int vs = idx + search.length();
        while (vs < obj.length() && obj.charAt(vs) == ' ') vs++;
        if (vs >= obj.length()) return "";
        char first = obj.charAt(vs);
        if (first == '"') {
            StringBuilder sb = new StringBuilder();
            int i = vs + 1;
            while (i < obj.length()) {
                char c = obj.charAt(i);
                if (c == '\\' && i + 1 < obj.length()) {
                    char nx = obj.charAt(i + 1);
                    if      (nx == '"')  { sb.append('"');  i += 2; }
                    else if (nx == '\\') { sb.append('\\'); i += 2; }
                    else if (nx == 'n')  { sb.append('\n'); i += 2; }
                    else                 { sb.append(c);    i++; }
                } else if (c == '"') {
                    break;
                } else { sb.append(c); i++; }
            }
            return sb.toString();
        } else {
            int end = vs;
            while (end < obj.length() && ",}\n\r ".indexOf(obj.charAt(end)) < 0)
                end++;
            return obj.substring(vs, end).trim();
        }
    }
}

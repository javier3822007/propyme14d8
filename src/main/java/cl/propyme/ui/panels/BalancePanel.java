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
package cl.propyme.ui.panels;

import cl.propyme.model.Ejercicio;
import cl.propyme.model.Resultados;
import cl.propyme.service.BalanceMiniService;
import cl.propyme.service.BalanceMiniService.FilaBalance;
import cl.propyme.service.BalanceMiniService.Totales;
import cl.propyme.service.BalanceMiniService.TipoCuenta;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BalancePanel — Balance de 8 Columnas tributario.
 *
 * Módulo independiente. Lee desde Resultados, nunca modifica el sistema principal.
 * Persistencia propia: propyme-data/{rut}/balance_{anio}.json
 *
 * Columnas: Cuenta | Sumas Débito | Sumas Crédito | Saldo Deudor | Saldo Acreedor
 *           | Inventario Activo | Inventario Pasivo | Resultado Pérdidas | Resultado Ganancias
 *
 * Filas fijas al final: Sumas Parciales | Utilidad/Pérdida del Ejercicio | Sumas Totales
 */
public class BalancePanel extends JPanel {

    // ── Columnas ──────────────────────────────────────────────────────────────
    private static final String[] COLUMNAS = {
        "Cuenta",
        "Sumas Débito", "Sumas Crédito",
        "Saldo Deudor",  "Saldo Acreedor",
        "Inv. Activo",   "Inv. Pasivo",
        "Res. Pérdidas", "Res. Ganancias"
    };
    private static final int[] ANCHOS = {260, 110, 110, 110, 110, 110, 110, 110, 110};

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color C_FILA_PAR   = Color.WHITE;
    private static final Color C_FILA_IMPAR = new Color(0xF5, 0xF8, 0xFF);
    private static final Color C_TOTAL      = Theme.TOTAL_BG;
    private static final Color C_RESULTADO  = Theme.ORANGE;
    private static final Color C_GRAN_TOTAL = new Color(0xD0, 0xD8, 0xE8);
    private static final Color C_CUADRA     = new Color(0xD5, 0xF0, 0xD5);
    private static final Color C_DESCUADRA  = new Color(0xFA, 0xD9, 0xD9);

    // ── Marcadores de fila especial ───────────────────────────────────────────
    private static final int TAG_NORMAL     = 0;
    private static final int TAG_PARCIALES  = 1;
    private static final int TAG_RESULTADO  = 2;
    private static final int TAG_TOTALES    = 3;

    // ── Estado ────────────────────────────────────────────────────────────────
    private final MainFrame frame;
    private DefaultTableModel modelTabla;
    private JTable tabla;
    private JLabel lblEstado;

    /** Filas de datos (sin incluir Sumas Parciales, Utilidad, Sumas Totales). */
    private List<FilaBalance> filas = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public BalancePanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        construirUI();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void construirUI() {
        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirTabla(),      BorderLayout.CENTER);
        add(construirPie(),        BorderLayout.SOUTH);
    }

    private JPanel construirEncabezado() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 2));
        titulos.setOpaque(false);
        JLabel titulo = new JLabel("Balance de 8 Columnas — Tributario");
        titulo.setFont(Theme.FONT_HEADER);
        titulo.setForeground(Color.WHITE);
        JLabel sub = new JLabel(
            "Régimen Pro Pyme Transparente Art. 14 D N°8  ·  Partida doble");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.LIGHT_BLUE);
        titulos.add(titulo);
        titulos.add(sub);
        hdr.add(titulos, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);

        JButton btnCompletar = btn("🔄 Datos actuales");
        JButton btnCargar    = btn("📂 Cargar");
        JButton btnAgregar   = btn("+ Cuenta");
        JButton btnEliminar  = btn("− Cuenta");
        JButton btnGuardar   = btn("💾 Guardar");
        JButton btnImprimir  = btn("🖨 Imprimir");

        btnCompletar.addActionListener(e -> accionCompletar());
        btnCargar   .addActionListener(e -> accionCargar());
        btnAgregar  .addActionListener(e -> accionAgregar());
        btnEliminar .addActionListener(e -> accionEliminar());
        btnGuardar  .addActionListener(e -> accionGuardar());
        btnImprimir .addActionListener(e -> accionImprimir());

        // Grupo 1: Datos
        btns.add(btnCompletar);
        btns.add(btnCargar);
        btns.add(separador());
        // Grupo 2: Editar
        btns.add(btnAgregar);
        btns.add(btnEliminar);
        btns.add(separador());
        // Grupo 3: Salida
        btns.add(btnGuardar);
        btns.add(btnImprimir);
        hdr.add(btns, BorderLayout.EAST);
        return hdr;
    }

    /** Separador vertical entre grupos de botones. */
    private static JComponent separador() {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(1, 24));
        s.setMaximumSize(new Dimension(1, 24));
        s.setBackground(new Color(0xFF, 0xFF, 0xFF, 80));
        s.setOpaque(true);
        return s;
    }

    private JScrollPane construirTabla() {
        modelTabla = new DefaultTableModel(COLUMNAS, 0) {
            public boolean isCellEditable(int fila, int col) {
                Object tag = getValueAt(fila, 0);
                if (!(tag instanceof TagFila)) return false;
                return ((TagFila) tag).tipo == TAG_NORMAL;
            }
            public Class<?> getColumnClass(int c) { return Object.class; }
        };

        tabla = new JTable(modelTabla);
        Theme.styleTable(tabla);

        tabla.setTableHeader(new javax.swing.table.JTableHeader(tabla.getColumnModel()) {
            public void paintComponent(java.awt.Graphics g) {
                g.setColor(Theme.DARK_BLUE);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        });
        tabla.getTableHeader().setDefaultRenderer(Theme.makeHeaderRenderer());
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tabla.setRowHeight(26);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int i = 0; i < ANCHOS.length; i++)
            tabla.getColumnModel().getColumn(i).setPreferredWidth(ANCHOS[i]);

        tabla.setDefaultRenderer(Object.class, new RenderizadorBalance());

        // Cuando el usuario edita → sincronizar con filas y recalcular
        modelTabla.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE)
                onCeldaEditada(e.getFirstRow(), e.getColumn());
        });

        JScrollPane scroll = new JScrollPane(tabla,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, esquina());
        scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER,  esquina());
        scroll.getViewport().setBackground(Theme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private JPanel construirPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(Theme.BG);
        pie.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        lblEstado = new JLabel("—");
        lblEstado.setFont(Theme.FONT_BOLD);
        lblEstado.setOpaque(true);
        lblEstado.setBackground(Color.WHITE);
        lblEstado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        pie.add(lblEstado, BorderLayout.CENTER);
        return pie;
    }

    // ── Refresh público ───────────────────────────────────────────────────────

    public void refresh() {
        redibujar();
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    private void accionCompletar() {
        Resultados r = frame.getCachedResultados();
        if (r == null) {
            JOptionPane.showMessageDialog(this,
                "No hay datos calculados.\nVaya a Recuadro 22 y presione Recalcular primero.",
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int resp = JOptionPane.showConfirmDialog(this,
            "Los datos del balance actual serán reemplazados con los datos\n" +
            "calculados del ejercicio. Desea continuar?",
            "Completar con datos actuales",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resp != JOptionPane.OK_OPTION) return;
        filas = BalanceMiniService.construirDesdeResultados(r);
        redibujar();
    }

    private void accionCargar() {
        String[] ctx = contexto();
        if (ctx == null) return;
        if (!BalanceMiniService.existeArchivo(ctx[0], ctx[1], Integer.parseInt(ctx[2]))) {
            JOptionPane.showMessageDialog(this,
                "No existe balance guardado para el ejercicio " + ctx[2] + ".",
                "Archivo no encontrado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int resp = JOptionPane.showConfirmDialog(this,
            "Los datos guardados en disco serán escritos en el balance actual.\n" +
            "Los cambios no guardados se perderán. Desea continuar?",
            "Cargar datos guardados",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resp != JOptionPane.OK_OPTION) return;
        try {
            List<FilaBalance> cargadas = BalanceMiniService.cargar(
                ctx[0], ctx[1], Integer.parseInt(ctx[2]));
            if (cargadas == null || cargadas.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "El archivo existe pero no contiene datos.",
                    "Archivo vacío", JOptionPane.WARNING_MESSAGE);
                return;
            }
            filas = cargadas;
            redibujar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionAgregar() {
        JTextField campoCuenta = Theme.inputField("Nombre de la cuenta");
        JComboBox<TipoCuenta> comboTipo = new JComboBox<>(TipoCuenta.values());
        JTextField campoDebito  = Theme.inputField("0");
        JTextField campoCredito = Theme.inputField("0");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill   = GridBagConstraints.HORIZONTAL;

        String[]     etiquetas = {"Cuenta:", "Tipo:", "Sumas Débito:", "Sumas Crédito:"};
        JComponent[] campos    = {campoCuenta, comboTipo, campoDebito, campoCredito};
        for (int i = 0; i < etiquetas.length; i++) {
            c.gridx = 0; c.gridy = i; c.weightx = 0;
            panel.add(new JLabel(etiquetas[i]), c);
            c.gridx = 1; c.weightx = 1;
            panel.add(campos[i], c);
        }
        JLabel nota = new JLabel(
            "<html><small>Para filas manuales, el resto de las columnas<br>" +
            "se calculan automáticamente según el tipo de cuenta.</small></html>");
        c.gridx = 0; c.gridy = etiquetas.length; c.gridwidth = 2;
        panel.add(nota, c);

        int resp = JOptionPane.showConfirmDialog(this, panel,
            "Agregar cuenta manual",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resp != JOptionPane.OK_OPTION) return;

        String nombre = campoCuenta.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El nombre de la cuenta no puede estar vacío.",
                "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double debito  = parseDbl(campoDebito.getText());
        double credito = parseDbl(campoCredito.getText());
        TipoCuenta tipo = (TipoCuenta) comboTipo.getSelectedItem();

        FilaBalance nueva = new FilaBalance(nombre, tipo, debito, credito);
        nueva.esAuto = false;
        filas.add(nueva);
        redibujar();
    }

    private void accionEliminar() {
        int sel = tabla.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una cuenta para eliminar.",
                "Sin selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object raw = modelTabla.getValueAt(sel, 0);
        if (!(raw instanceof TagFila) || ((TagFila) raw).tipo != TAG_NORMAL) {
            JOptionPane.showMessageDialog(this,
                "Solo se pueden eliminar cuentas, no las filas de totales.",
                "No eliminable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idx = ((TagFila) raw).indice;
        if (idx < 0 || idx >= filas.size()) return;

        int conf = JOptionPane.showConfirmDialog(this,
            "Eliminar la cuenta \"" + filas.get(idx).cuenta + "\"?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;
        filas.remove(idx);
        redibujar();
    }

    private void accionGuardar() {
        String[] ctx = contexto();
        if (ctx == null) return;
        try {
            BalanceMiniService.guardar(ctx[0], ctx[1],
                Integer.parseInt(ctx[2]), filas);
            JOptionPane.showMessageDialog(this,
                "Balance guardado correctamente (ejercicio " + ctx[2] + ").",
                "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al guardar: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionImprimir() {
        if (filas == null || filas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No hay datos para imprimir.\nUse \"Datos actuales\" o \"Cargar\" primero.",
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Ejercicio ej = frame.getEjercicioActual();
        cl.propyme.model.Empresa emp = frame.getEmpresaActual();
        if (ej == null) {
            JOptionPane.showMessageDialog(this,
                "No hay ejercicio cargado.",
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cl.propyme.service.BalanceMiniService.Totales tot =
            cl.propyme.service.BalanceMiniService.calcularTotales(filas);
        cl.propyme.service.BalancePrintService.imprimir(
            this, emp, ej.getAnioComercial(), filas, tot);
    }

    // ── Dibujo de la tabla ────────────────────────────────────────────────────

    private void redibujar() {
        // Bloquear listener durante redibujo para evitar recursión
        modelTabla.setRowCount(0);

        // Filas de datos
        for (int i = 0; i < filas.size(); i++) {
            FilaBalance f = filas.get(i);
            modelTabla.addRow(new Object[]{
                new TagFila(f.cuenta, TAG_NORMAL, i),
                fmt(f.sumasDebito),  fmt(f.sumasCredito),
                fmt(f.saldoDeudor),  fmt(f.saldoAcreedor),
                fmt(f.invActivo),    fmt(f.invPasivo),
                fmt(f.resPerdidas),  fmt(f.resGanancias)
            });
        }

        Totales t = BalanceMiniService.calcularTotales(filas);

        // Sumas Parciales
        modelTabla.addRow(new Object[]{
            new TagFila("Sumas Parciales", TAG_PARCIALES, -1),
            fmt(t.sumasDebito),  fmt(t.sumasCredito),
            fmt(t.saldoDeudor),  fmt(t.saldoAcreedor),
            fmt(t.invActivo),    fmt(t.invPasivo),
            fmt(t.resPerdidas),  fmt(t.resGanancias)
        });

        // Utilidad / Pérdida del Ejercicio
        double util = t.utilidad;
        String etiqUtil = util >= 0 ? "Utilidad del Ejercicio" : "Pérdida del Ejercicio";
        double utilAbs = Math.abs(util);
        // Utilidad → Inventario Pasivo + Resultado Pérdidas (para cuadrar)
        // Pérdida  → Inventario Activo + Resultado Ganancias (para cuadrar)
        modelTabla.addRow(new Object[]{
            new TagFila(etiqUtil, TAG_RESULTADO, -1),
            "", "",  "", "",
            util < 0 ? fmt(utilAbs) : "",
            util >= 0 ? fmt(utilAbs) : "",
            util >= 0 ? fmt(utilAbs) : "",
            util < 0  ? fmt(utilAbs) : ""
        });

        // Sumas Totales (Parciales + Utilidad/Pérdida)
        double totInvActivo  = t.invActivo  + (util < 0 ? utilAbs : 0);
        double totInvPasivo  = t.invPasivo  + (util >= 0 ? utilAbs : 0);
        double totResPerd    = t.resPerdidas  + (util >= 0 ? utilAbs : 0);
        double totResGan     = t.resGanancias + (util < 0  ? utilAbs : 0);
        modelTabla.addRow(new Object[]{
            new TagFila("Sumas Totales", TAG_TOTALES, -1),
            fmt(t.sumasDebito),  fmt(t.sumasCredito),
            fmt(t.saldoDeudor),  fmt(t.saldoAcreedor),
            fmt(totInvActivo),   fmt(totInvPasivo),
            fmt(totResPerd),     fmt(totResGan)
        });

        actualizarEstado(t);
    }

    private void actualizarEstado(Totales t) {
        if (filas.isEmpty()) {
            lblEstado.setText("  Sin datos. Use \"Completar con datos actuales\" para iniciar.");
            lblEstado.setBackground(Color.WHITE);
            lblEstado.setForeground(Theme.TEXT_MUTED);
            return;
        }
        if (t.cuadra) {
            String tipo = t.utilidad >= 0 ? "Utilidad" : "Pérdida";
            lblEstado.setText(String.format(
                "  ✔  Partida doble cuadra correctamente.  %s del Ejercicio: %s",
                tipo, fmt(Math.abs(t.utilidad))));
            lblEstado.setBackground(C_CUADRA);
            lblEstado.setForeground(new Color(0x1A, 0x5C, 0x1A));
        } else {
            lblEstado.setText(String.format(
                "  ⚠  El balance NO cuadra. Diferencia en Sumas: %s — Se requieren ajustes manuales.",
                fmt(t.diferencia)));
            lblEstado.setBackground(C_DESCUADRA);
            lblEstado.setForeground(Theme.DANGER);
        }
    }

    // ── Edición de celdas ─────────────────────────────────────────────────────

    private boolean editando = false;

    private void onCeldaEditada(int filaTabla, int col) {
        if (editando) return;
        Object raw = modelTabla.getValueAt(filaTabla, 0);
        if (!(raw instanceof TagFila)) return;
        TagFila tag = (TagFila) raw;
        if (tag.tipo != TAG_NORMAL || tag.indice < 0 || tag.indice >= filas.size()) return;

        editando = true;
        FilaBalance fb = filas.get(tag.indice);

        switch (col) {
            case 0: // Cuenta
                if (raw instanceof TagFila) fb.cuenta = ((TagFila) raw).etiqueta;
                break;
            case 1: fb.sumasDebito  = parseDbl(str(modelTabla.getValueAt(filaTabla, 1))); break;
            case 2: fb.sumasCredito = parseDbl(str(modelTabla.getValueAt(filaTabla, 2))); break;
            case 3: fb.saldoDeudor   = parseDbl(str(modelTabla.getValueAt(filaTabla, 3))); break;
            case 4: fb.saldoAcreedor = parseDbl(str(modelTabla.getValueAt(filaTabla, 4))); break;
            case 5: fb.invActivo     = parseDbl(str(modelTabla.getValueAt(filaTabla, 5))); break;
            case 6: fb.invPasivo     = parseDbl(str(modelTabla.getValueAt(filaTabla, 6))); break;
            case 7: fb.resPerdidas   = parseDbl(str(modelTabla.getValueAt(filaTabla, 7))); break;
            case 8: fb.resGanancias  = parseDbl(str(modelTabla.getValueAt(filaTabla, 8))); break;
        }

        // Si editó Débito o Crédito en una fila auto, recalcular las columnas derivadas
        if (fb.esAuto && (col == 1 || col == 2)) {
            fb.calcularSaldos();
        }

        editando = false;
        redibujar();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Retorna [baseDir, rut, anio] o null si no hay empresa cargada. */
    private String[] contexto() {
        Ejercicio ej = frame.getEjercicioActual();
        if (ej == null || frame.getEmpresaActual() == null) {
            JOptionPane.showMessageDialog(this,
                "No hay empresa cargada.", "Sin datos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new String[]{
            System.getProperty("app.home", System.getProperty("user.dir")),
            frame.getEmpresaActual().getRut(),
            String.valueOf(ej.getAnioComercial())
        };
    }

    private static String fmt(double v) {
        if (v == 0) return "";
        return String.format("%,.0f", v);
    }

    /**
     * Parsea un número en formato chileno o estándar.
     * Reconoce:
     *   - Formato chileno completo: "1.234.567,89" → 1234567.89
     *   - Solo decimal con coma: "12,5" → 12.5
     *   - Solo punto como decimal: "0.5" → 0.5
     *   - Solo punto como miles: "1.234" → 1234
     *   - Símbolos de moneda y espacios: "$ 1.234" → 1234
     * Retorna 0 si el string es vacío o no se puede parsear.
     */
    private static double parseDbl(String s) {
        if (s == null || s.isBlank()) return 0;
        String texto = s.trim().replace("$", "").replace(" ", "");
        if (texto.isEmpty()) return 0;
        // Normalización inteligente según presencia de . y ,
        if (texto.contains(".") && texto.contains(",")) {
            // Formato chileno: punto=miles, coma=decimal
            texto = texto.replace(".", "").replace(",", ".");
        } else if (texto.contains(",")) {
            // Solo coma → decimal
            texto = texto.replace(",", ".");
        } else if (texto.contains(".")) {
            // Solo punto: decidir según contexto
            int idxPunto = texto.lastIndexOf('.');
            int decimales = texto.length() - idxPunto - 1;
            long puntos = texto.chars().filter(c -> c == '.').count();
            if (puntos == 1 && decimales <= 2) {
                // Probablemente decimal (ej. "0.5", "12.34"), se deja como está
            } else {
                // Probablemente separador de miles (ej. "1.234", "1.234.567")
                texto = texto.replace(".", "");
            }
        }
        try { return Double.parseDouble(texto); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static JButton btn(String texto) {
        JButton b = new JButton(texto);
        b.setFont(Theme.FONT_LABEL);
        b.setBackground(Color.WHITE);
        b.setForeground(Theme.DARK_BLUE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return b;
    }

    private static JPanel esquina() {
        JPanel p = new JPanel();
        p.setBackground(Theme.DARK_BLUE);
        return p;
    }

    // ── Compatibilidad con MainFrame (API pública sin cambios) ────────────────

    public List<String[]> getBalanceData()            { return new ArrayList<>(); }
    public void setBalanceData(List<String[]> rows)   { }

    // ── TagFila ───────────────────────────────────────────────────────────────

    private static class TagFila {
        final String etiqueta;
        final int    tipo;    // TAG_NORMAL, TAG_PARCIALES, TAG_RESULTADO, TAG_TOTALES
        final int    indice;  // índice en filas; -1 si es fila especial

        TagFila(String etiqueta, int tipo, int indice) {
            this.etiqueta = etiqueta;
            this.tipo     = tipo;
            this.indice   = indice;
        }
        public String toString() { return etiqueta; }
    }

    // ── Renderizador ─────────────────────────────────────────────────────────

    private class RenderizadorBalance extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean focus, int fila, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, fila, col);

            Object raw = modelTabla.getValueAt(fila, 0);
            int tipo = (raw instanceof TagFila) ? ((TagFila) raw).tipo : TAG_NORMAL;

            if (!sel) {
                switch (tipo) {
                    case TAG_PARCIALES:
                        setBackground(C_TOTAL);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                        break;
                    case TAG_RESULTADO:
                        setBackground(C_RESULTADO);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                        break;
                    case TAG_TOTALES:
                        setBackground(C_GRAN_TOTAL);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                        break;
                    default:
                        setBackground(fila % 2 == 0 ? C_FILA_PAR : C_FILA_IMPAR);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_LABEL);
                        break;
                }
            }

            setHorizontalAlignment(col == 0 ? LEFT : RIGHT);
            if (col == 0 && raw instanceof TagFila)
                setText(((TagFila) raw).etiqueta);
            return this;
        }
    }
}

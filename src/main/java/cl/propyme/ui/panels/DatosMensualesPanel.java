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

import cl.propyme.model.DatosMes;
import cl.propyme.model.GlobalConfig;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatosMensualesPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(DatosMensualesPanel.class.getName());

    private static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    private final MainFrame frame;
    private boolean updating = false;

    // ─── INGRESOS table ───────────────────────────────────────────────────────
    private JTable ingTable;
    private DefaultTableModel ingModel;
    private javax.swing.Timer ingTimer;
    private int pendingIngRow = -1;

    private static final int
        // Ingresos columns
        I_MES       = 0,
        I_VTA_AF    = 1,   // Ventas Afectas Netas
        I_NC_VTA    = 2,   // NC Venta (resta)
        I_ND_VTA    = 3,   // ND Venta (suma)
        I_IVA_DF    = 4,   // IVA Débito
        I_VTA_BRUTA = 5,   // Venta Bruta (calculado)
        I_VTA_EX    = 6,   // Ventas/Serv. Exentos
        I_TASA_PPM  = 7,   // Tasa PPM %
        I_PPM_HIST  = 8,   // PPM Histórico (calc)
        I_FACTOR    = 9,   // Factor reajuste manual (input)
        I_PPM_ACT   = 10,  // PPM Actualizado (calc = hist * factor)
        I_OTROS_ING = 11,  // Otros Ingresos Percibidos
        I_NO_PERC   = 12;  // Ingresos No Percibidos

    private static final Set<Integer> ING_INPUT = new HashSet<>(Arrays.asList(
        I_VTA_AF, I_NC_VTA, I_ND_VTA, I_IVA_DF, I_VTA_EX, I_TASA_PPM, I_FACTOR, I_OTROS_ING, I_NO_PERC
    ));
    private static final Set<Integer> ING_CALC = new HashSet<>(Arrays.asList(
        I_VTA_BRUTA, I_PPM_HIST, I_PPM_ACT
    ));

    // ─── EGRESOS table ────────────────────────────────────────────────────────
    private JTable egTable;
    private DefaultTableModel egModel;
    private javax.swing.Timer egTimer;
    private int pendingEgRow = -1;

    private static final int
        E_MES       = 0,
        E_COMP_AF   = 1,
        E_NC_COMP   = 2,   // NC Compra (resta egresos)
        E_ND_COMP   = 3,   // ND Compra (suma egresos)
        E_IVA_CF    = 4,
        E_COMP_EX   = 5,
        E_COMP_NOREC= 6,
        E_IVA_NOREC = 7,   // calc
        E_AF        = 8,
        E_REMUN     = 9,
        E_HONOR     = 10,
        E_ARR       = 11,
        E_GASTOS    = 12,
        E_OTROS_EG  = 13,
        E_ADEUD     = 14,
        E_RET_S1    = 15,
        E_RET_S2    = 16;

    private static final Set<Integer> EG_INPUT = new HashSet<>(Arrays.asList(
        E_COMP_AF, E_NC_COMP, E_ND_COMP, E_IVA_CF, E_COMP_EX, E_COMP_NOREC,
        E_AF, E_REMUN, E_HONOR, E_ARR, E_GASTOS, E_OTROS_EG, E_ADEUD,
        E_RET_S1, E_RET_S2
    ));
    private static final Set<Integer> EG_CALC = new HashSet<>(Arrays.asList(
        E_IVA_NOREC
    ));

    public DatosMensualesPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel title = new JLabel("Datos Mensuales — Ingresos y Egresos del Ejercicio");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Color.WHITE);

        JLabel hint = new JLabel(
            "Verde = ingreso manual  |  Blanco = calculado automático  |  Amarillo = factor personalizado  |  Enter/Tab = confirmar");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.LIGHT_BLUE);

        JPanel tp = new JPanel(new GridLayout(2, 1, 0, 2));
        tp.setOpaque(false);
        tp.add(title);
        tp.add(hint);
        hdr.add(tp, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        JButton autoFactorBtn = new JButton("✨ Auto-llenar factores");
        autoFactorBtn.setFont(Theme.FONT_LABEL);
        autoFactorBtn.setBackground(Color.WHITE);
        autoFactorBtn.setForeground(Theme.MID_BLUE);
        autoFactorBtn.setFocusPainted(false);
        autoFactorBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        autoFactorBtn.setToolTipText("Llena la columna Factor con los valores del GlobalConfig (mes siguiente al período)");
        autoFactorBtn.addActionListener(e -> autoFillFactores());
        btns.add(autoFactorBtn);

        JButton recalcBtn = new JButton("⟳ Recalcular");
        recalcBtn.setFont(Theme.FONT_LABEL);
        recalcBtn.setBackground(Color.WHITE);
        recalcBtn.setForeground(Theme.MID_BLUE);
        recalcBtn.setFocusPainted(false);
        recalcBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        recalcBtn.addActionListener(e -> { saveToModel(); frame.invalidateCache(); frame.recalculate(); });
        btns.add(recalcBtn);
        hdr.add(btns, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        // ── Two tables stacked in a scroll pane ───────────────────────────────
        JPanel tablesPanel = new JPanel();
        tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.Y_AXIS));
        tablesPanel.setBackground(Theme.BG);

        // INGRESOS
        tablesPanel.add(makeSectionHeader("INGRESOS", new Color(0x0D, 0x47, 0xA1)));
        ingModel = buildIngresosModel();
        ingTable = buildTable(ingModel, ING_INPUT, ING_CALC, I_MES, I_TASA_PPM);
        configureIngresosWidths();
        setupIngresosEditors();
        setupIngresosListeners();
        initIngresosRows();
        JScrollPane ingScroll = wrapTable(ingTable);
        ingScroll.setPreferredSize(new Dimension(1130, ingTable.getRowHeight() * 13 + 24));
        tablesPanel.add(ingScroll);

        // EGRESOS
        tablesPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        tablesPanel.add(makeSectionHeader("EGRESOS", new Color(0x1A, 0x5E, 0x1A)));
        egModel = buildEgresosModel();
        egTable = buildTable(egModel, EG_INPUT, EG_CALC, E_MES, -1);
        configureEgresosWidths();
        setupEgresosEditors();
        setupEgresosListeners();
        initEgresosRows();
        JScrollPane egScroll = wrapTable(egTable);
        egScroll.setPreferredSize(new Dimension(1588, egTable.getRowHeight() * 13 + 24));
        tablesPanel.add(egScroll);

        // Legend
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        bottom.setBackground(Theme.BG);
        addLegend(bottom, Theme.INPUT_GREEN, new Color(0, 0, 153), "Ingreso manual");
        addLegend(bottom, Color.WHITE, new Color(68, 68, 68), "Calculado automático");
        bottom.add(Theme.mutedLabel("  |  ⚙ = control reajuste por mes  |  Columnas redimensionables  |  "));
        JLabel reajusteHint = new JLabel();
        checkReajusteConfig(reajusteHint);
        bottom.add(reajusteHint);

        JScrollPane outerScroll = new JScrollPane(tablesPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outerScroll.setBorder(BorderFactory.createEmptyBorder());
        add(outerScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        loadFromModel();
    }

    // ── Model builders ────────────────────────────────────────────────────────

    private DefaultTableModel buildIngresosModel() {
        String[] cols = {
            "Mes",
            "Ventas Afectas\nNetas ($)",
            "Neto NC\n($)", "Neto ND\n($)",
            "IVA\nDébito ($)", "Venta\nBruta ($)",
            "Ventas/Serv.\nExentos IVA ($)",
            "Tasa\nPPM (%)", "PPM\nHistórico ($)", "Factor\nReajuste", "PPM\nActualizado ($)",
            "Otros Ing.\nPercibidos ($)", "Ing. No\nPercibidos ($)"
        };
        return new DefaultTableModel(cols, 13) {
            public boolean isCellEditable(int r, int c) {
                if (r == 12) return false;
                return c != I_MES && !ING_CALC.contains(c);
            }
            public Class<?> getColumnClass(int c) { return String.class; }
        };
    }

    private DefaultTableModel buildEgresosModel() {
        String[] cols = {
            "Mes",
            "Compras\nAfectas Netas ($)",
            "Neto NC\n($)", "Neto ND\n($)",
            "IVA CF\n($)",
            "Compras\nExentas IVA ($)", "Compras IVA\nNo Rec. Neto ($)", "IVA No\nRec. ($)",
            "Activo\nFijo ($)", "Remuneraciones\n($)", "Honorarios\n($)",
            "Arriendos\n($)", "Gastos\nGenerales ($)", "Otros\nEgresos ($)",
            "Egresos\nAdeudados ($)",
            "Retiro\nSocio 1 ($)", "Retiro\nSocio 2 ($)"
        };
        return new DefaultTableModel(cols, 13) {
            public boolean isCellEditable(int r, int c) {
                if (r == 12) return false;
                return c != E_MES && !EG_CALC.contains(c);
            }
            public Class<?> getColumnClass(int c) { return String.class; }
        };
    }

    // ── Generic table factory ─────────────────────────────────────────────────

    private JTable buildTable(DefaultTableModel mdl, Set<Integer> inputCols,
                               Set<Integer> calcCols, int mesCol, int pctCol) {
        JTable t = new JTable(mdl);

        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v,
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = new JLabel(v == null ? "" : v.toString().replace("\n", " "));
                lbl.setFont(Theme.FONT_BOLD);
                lbl.setForeground(Color.WHITE);
                lbl.setBackground(Theme.DARK_BLUE);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.MID_BLUE));
                lbl.setHorizontalAlignment(CENTER);
                return lbl;
            }
        });
        t.getTableHeader().setBackground(Theme.DARK_BLUE);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(Theme.FONT_BOLD);
        t.getTableHeader().setPreferredSize(new Dimension(0, 44));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setResizingAllowed(true);

        t.setRowHeight(26);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setGridColor(Theme.LIGHT_BLUE);
        t.setShowGrid(true);
        t.setFillsViewportHeight(false);

        // Color columna Factor: amarillo pastel cuando el valor manual difiere del default global
        final Color FACTOR_AUTO = Theme.INPUT_GREEN;
        final Color FACTOR_MANUAL = new Color(0xFF, 0xE9, 0xA8); // pastel yellow
        final Color IVA_DISCREPANCY = new Color(0xFF, 0xE9, 0xA8); // mismo amarillo

        t.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, v, sel, focus, row, col);
                setHorizontalAlignment(col == mesCol ? LEFT : RIGHT);
                if (!sel) {
                    if (row == 12) {
                        setBackground(Theme.TOTAL_BG);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                        setHorizontalAlignment(col == mesCol ? LEFT : RIGHT);
                        return this;
                    }
                    if (col == mesCol) {
                        setBackground(Theme.LIGHT_BLUE);
                        setForeground(Theme.TEXT_DARK);
                    } else if (col == I_FACTOR && tbl == ingTable) {
                        // Amarillo si valor difiere del auto-llenado global; verde en caso contrario
                        setBackground(isFactorManual(row, v) ? FACTOR_MANUAL : FACTOR_AUTO);
                        setForeground(new Color(0x00, 0x00, 0x99));
                    } else if ((tbl == ingTable && col == I_IVA_DF && row < 12)
                            || (tbl == egTable  && col == E_IVA_CF && row < 12)) {
                        // IVA: amarillo si hay discrepancia con el cálculo automático
                        if (isIvaDiscrepant(tbl, row)) {
                            setBackground(IVA_DISCREPANCY);
                            setForeground(new Color(0x85, 0x6D, 0x00));
                            setToolTipText(buildIvaTooltip(tbl, row));
                        } else {
                            setBackground(Theme.INPUT_GREEN);
                            setForeground(new Color(0x00, 0x00, 0x99));
                            setToolTipText(null);
                        }
                    } else if (calcCols.contains(col)) {
                        setBackground(Color.WHITE);
                        setForeground(new Color(0x44, 0x44, 0x44));
                    } else if (inputCols.contains(col)) {
                        setBackground(Theme.INPUT_GREEN);
                        setForeground(new Color(0x00, 0x00, 0x99));
                    } else {
                        setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF5, 0xF8, 0xFF));
                        setForeground(Theme.TEXT_DARK);
                    }
                }
                if (v != null && col != mesCol && col != pctCol && col != I_FACTOR) {
                    String s = v.toString().trim();
                    if (!s.isEmpty() && !s.equals("0")) {
                        try {
                            double d = parseRaw(s);
                            if (d != 0) setText(formatNum(d));
                        } catch (Exception ignored) {}
                    }
                }
                return this;
            }
        });

        // Enter key
        t.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "selectNextRow");
        t.getActionMap().put("selectNextRow", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int row = t.getEditingRow(); int col = t.getEditingColumn();
                if (t.isEditing()) t.getCellEditor().stopCellEditing();
                if (row >= 0 && row < 11) t.changeSelection(row + 1, col, false, false);
            }
        });
        // Tab key
        t.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "selectNextCol");
        t.getActionMap().put("selectNextCol", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int row = t.getEditingRow(); int col = t.getEditingColumn();
                if (t.isEditing()) t.getCellEditor().stopCellEditing();
                if (col >= 0 && col < mdl.getColumnCount() - 1) t.changeSelection(row, col + 1, false, false);
            }
        });

        return t;
    }

    // ── Column widths ─────────────────────────────────────────────────────────

    private void configureIngresosWidths() {
        // {Mes, VtaAf, IvaDF, VtaBruta, VtaEx, TasaPPM, PPMHist, Factor, PPMAct, OtrosIng, NoPerc}
        int[] w = {88, 115, 95, 105, 115, 72, 110, 80, 115, 110, 105};
        for (int i = 0; i < w.length && i < ingTable.getColumnCount(); i++) {
            TableColumn tc = ingTable.getColumnModel().getColumn(i);
            tc.setPreferredWidth(w[i]);
            tc.setMinWidth(50);
        }
    }

    private void configureEgresosWidths() {
        int[] w = {88, 115, 95, 105, 120, 95, 95, 115, 100, 100, 110, 100, 110, 110, 110};
        for (int i = 0; i < w.length && i < egTable.getColumnCount(); i++) {
            TableColumn tc = egTable.getColumnModel().getColumn(i);
            tc.setPreferredWidth(w[i]);
            tc.setMinWidth(50);
        }
    }

    private JScrollPane wrapTable(JTable t) {
        JScrollPane sp = new JScrollPane(t,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createEmptyBorder());
        // Altura fija: 12 filas de datos + 1 totales + cabecera
        int h = t.getTableHeader().getPreferredSize().height + t.getRowHeight() * 13 + 4;
        sp.setPreferredSize(new Dimension(Integer.MAX_VALUE, h));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        return sp;
    }

    // ── Editors ───────────────────────────────────────────────────────────────

    private DefaultCellEditor makeNumEditor() {
        return new DefaultCellEditor(new JTextField()) {
            { setClickCountToStart(1); }
            public boolean stopCellEditing() {
                try {
                    String val = (String) getCellEditorValue();
                    ((JTextField) getComponent()).setText(normalizeInput(val));
                } catch (Exception ignored) {}
                return super.stopCellEditing();
            }
        };
    }

    private DefaultCellEditor makePctEditor() {
        return new DefaultCellEditor(new JTextField()) {
            { setClickCountToStart(1); }
            public boolean stopCellEditing() {
                try {
                    String val = (String) getCellEditorValue();
                    String clean = val.trim().replace("%", "").replace(" ", "");
                    ((JTextField) getComponent()).setText(clean);
                } catch (Exception ignored) {}
                return super.stopCellEditing();
            }
        };
    }

    private DefaultCellEditor makeFactorEditor() {
        return new DefaultCellEditor(new JTextField()) {
            { setClickCountToStart(1); }
            public boolean stopCellEditing() {
                try {
                    String val = (String) getCellEditorValue();
                    String clean = val.trim().replace(" ", "");
                    ((JTextField) getComponent()).setText(clean);
                } catch (Exception ignored) {}
                return super.stopCellEditing();
            }
        };
    }

    private void setupIngresosEditors() {
        DefaultCellEditor num = makeNumEditor();
        DefaultCellEditor pct = makePctEditor();
        DefaultCellEditor factor = makeFactorEditor();
        for (int c = 1; c < ingModel.getColumnCount(); c++) {
            TableColumn tc = ingTable.getColumnModel().getColumn(c);
            if (c == I_TASA_PPM)      tc.setCellEditor(pct);
            else if (c == I_FACTOR)   tc.setCellEditor(factor);
            else                      tc.setCellEditor(num);
        }
    }

    private void setupEgresosEditors() {
        DefaultCellEditor num = makeNumEditor();
        for (int c = 1; c < egModel.getColumnCount(); c++) {
            egTable.getColumnModel().getColumn(c).setCellEditor(num);
        }
    }

    // ── Table model listeners ─────────────────────────────────────────────────

    private void setupIngresosListeners() {
        ingModel.addTableModelListener(e -> {
            if (updating) return;
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow(); int col = e.getColumn();
                if (row < 0 || col == I_MES || ING_CALC.contains(col)) return;
                frame.markDirty();
                pendingIngRow = row;
                if (ingTimer != null && ingTimer.isRunning()) { ingTimer.restart(); return; }
                ingTimer = new javax.swing.Timer(300, ev -> {
                    if (pendingIngRow >= 0) { recalcIngRow(pendingIngRow, col); pendingIngRow = -1; }
                });
                ingTimer.setRepeats(false); ingTimer.start();
            }
        });
    }

    private void setupEgresosListeners() {
        egModel.addTableModelListener(e -> {
            if (updating) return;
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow(); int col = e.getColumn();
                if (row < 0 || col == E_MES || EG_CALC.contains(col)) return;
                frame.markDirty();
                pendingEgRow = row;
                if (egTimer != null && egTimer.isRunning()) { egTimer.restart(); return; }
                egTimer = new javax.swing.Timer(300, ev -> {
                    if (pendingEgRow >= 0) { recalcEgRow(pendingEgRow, col); pendingEgRow = -1; }
                });
                egTimer.setRepeats(false); egTimer.start();
            }
        });
    }

    // ── Row init ──────────────────────────────────────────────────────────────

    private void initIngresosRows() {
        for (int i = 0; i < 12; i++) {
            ingModel.setValueAt(MESES[i], i, I_MES);
            ingModel.setValueAt("0,200", i, I_TASA_PPM);
        }
        ingModel.setValueAt("TOTALES", 12, I_MES);
        ingModel.setValueAt("—", 12, I_TASA_PPM);
    }

    private void initEgresosRows() {
        for (int i = 0; i < 12; i++) egModel.setValueAt(MESES[i], i, E_MES);
        egModel.setValueAt("TOTALES", 12, E_MES);
    }

    // ── Section header ────────────────────────────────────────────────────────

    private JPanel makeSectionHeader(String text, Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.setBackground(bg);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        p.add(lbl);
        return p;
    }

    // ── Recalculate rows ──────────────────────────────────────────────────────

    private void recalcIngRow(int row, int triggerCol) {
        if (updating) return;
        updating = true;
        try {
            double vtaAf  = parseIG(row, I_VTA_AF);
            double ncVta  = parseIG(row, I_NC_VTA);
            double ndVta  = parseIG(row, I_ND_VTA);
            double ivaDF  = parseIG(row, I_IVA_DF);
            double vtaEx  = parseIG(row, I_VTA_EX);
            double otros  = parseIG(row, I_OTROS_ING);

            // Neto efectivo afecto a IVA (ventas afectas ajustadas por notas)
            double netoEfectivo = vtaAf - ncVta + ndVta;

            if (triggerCol == I_IVA_DF) {
                if (ivaDF > 0 && vtaAf == 0) { vtaAf = Math.round(ivaDF / 0.19); ingModel.setValueAt(fmt(vtaAf), row, I_VTA_AF); netoEfectivo = vtaAf - ncVta + ndVta; }
            } else if (triggerCol == I_VTA_AF || triggerCol == I_NC_VTA || triggerCol == I_ND_VTA) {
                if (netoEfectivo > 0) ivaDF = Math.round(netoEfectivo * 0.19);
                else if (netoEfectivo == 0) ivaDF = 0;
            } else {
                if (ivaDF == 0 && netoEfectivo > 0) ivaDF = Math.round(netoEfectivo * 0.19);
            }
            double vtaBruta = netoEfectivo + ivaDF;
            double tasa = parsePctIG(row);
            // Base PPM ajustada por NC (resta) y ND (suma)
            double basePPM = netoEfectivo + vtaEx + otros;
            double ppmHist = Math.round(basePPM * tasa);

            // Factor manual: si vacío → 1.0 (sin reajuste). PPM Actualizado = PPM Hist × Factor.
            double factor = parseFactorIG(row);
            if (factor <= 0) factor = 1.0;
            double ppmAct = Math.round(ppmHist * factor);

            ingModel.setValueAt(fmt(ivaDF),    row, I_IVA_DF);
            ingModel.setValueAt(fmt(vtaBruta), row, I_VTA_BRUTA);
            ingModel.setValueAt(fmt(ppmHist),  row, I_PPM_HIST);
            ingModel.setValueAt(fmt(ppmAct),   row, I_PPM_ACT);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error recalculando fila de ingresos " + row, ex);
        } finally {
            updating = false;
        }
        recalcIngTotals();
    }

    private void recalcEgRow(int row, int triggerCol) {
        if (updating) return;
        updating = true;
        try {
            double compAf  = parseEG(row, E_COMP_AF);
            double ncComp  = parseEG(row, E_NC_COMP);
            double ndComp  = parseEG(row, E_ND_COMP);
            double ivaCF   = parseEG(row, E_IVA_CF);
            double compNR  = parseEG(row, E_COMP_NOREC);

            // Neto efectivo de compras (ajustado por notas)
            double compNetoEfect = compAf - ncComp + ndComp;

            if (triggerCol == E_IVA_CF) {
                if (ivaCF > 0 && compAf == 0) { compAf = Math.round(ivaCF / 0.19); egModel.setValueAt(fmt(compAf), row, E_COMP_AF); compNetoEfect = compAf - ncComp + ndComp; }
            } else {
                if (compNetoEfect > 0) ivaCF = Math.round(compNetoEfect * 0.19);
                else if (compNetoEfect == 0) ivaCF = 0;
            }
            double ivaNoRec = Math.round(compNR * 0.19);

            egModel.setValueAt(fmt(ivaCF),    row, E_IVA_CF);
            egModel.setValueAt(fmt(ivaNoRec), row, E_IVA_NOREC);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error recalculando fila de egresos " + row, ex);
        } finally {
            updating = false;
        }
        recalcEgTotals();
    }

    private static final int[] ING_TOTAL_COLS = {
        I_VTA_AF, I_NC_VTA, I_ND_VTA, I_IVA_DF, I_VTA_BRUTA, I_VTA_EX, I_PPM_HIST, I_PPM_ACT, I_OTROS_ING, I_NO_PERC
    };
    private static final int[] EG_TOTAL_COLS = {
        E_COMP_AF, E_NC_COMP, E_ND_COMP, E_IVA_CF, E_COMP_EX, E_COMP_NOREC, E_IVA_NOREC,
        E_AF, E_REMUN, E_HONOR, E_ARR, E_GASTOS, E_OTROS_EG, E_ADEUD, E_RET_S1, E_RET_S2
    };

    private void recalcIngTotals() {
        if (updating) return;
        updating = true;
        try {
            for (int c : ING_TOTAL_COLS) {
                double sum = 0;
                for (int r = 0; r < 12; r++) sum += parseIG(r, c);
                ingModel.setValueAt(fmt(sum), 12, c);
            }
        } finally { updating = false; }
    }

    private void recalcEgTotals() {
        if (updating) return;
        updating = true;
        try {
            for (int c : EG_TOTAL_COLS) {
                double sum = 0;
                for (int r = 0; r < 12; r++) sum += parseEG(r, c);
                egModel.setValueAt(fmt(sum), 12, c);
            }
        } finally { updating = false; }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    public void loadFromModel() {
        Map<Integer, DatosMes> datos = frame.getDatos();
        updating = true;
        try {
            for (int i = 0; i < 12; i++) {
                DatosMes dm = datos.getOrDefault(i + 1, new DatosMes(i + 1));
                double ivaDF   = dm.getIvaDebito() > 0 ? dm.getIvaDebito() : Math.round(dm.getVentasAfectasNetas() * 0.19);
                double ivaCF   = dm.getIvaCredFiscal() > 0 ? dm.getIvaCredFiscal() : Math.round(dm.getComprasAfectasNetas() * 0.19);
                double vtaBruta= dm.getVentasAfectasNetas() + ivaDF;
                double ivaNoRec= dm.getIvaNoRecuperable();

                // Ingresos
                ingModel.setValueAt(MESES[i],                              i, I_MES);
                ingModel.setValueAt(fmt(dm.getVentasAfectasNetas()),        i, I_VTA_AF);
                ingModel.setValueAt(fmt(dm.getNotasCreditoVenta()),         i, I_NC_VTA);
                ingModel.setValueAt(fmt(dm.getNotasDebitoVenta()),          i, I_ND_VTA);
                ingModel.setValueAt(fmt(ivaDF),                            i, I_IVA_DF);
                ingModel.setValueAt(fmt(vtaBruta),                         i, I_VTA_BRUTA);
                ingModel.setValueAt(fmt(dm.getVentasExentasIVA()),          i, I_VTA_EX);
                ingModel.setValueAt(fmtPct(dm.getTasaPPM()),               i, I_TASA_PPM);
                ingModel.setValueAt(fmt(dm.getPPMHistorico()),              i, I_PPM_HIST);
                double f = dm.getFactorReajustePPM();
                ingModel.setValueAt(f > 0 ? fmtFactor(f) : "",              i, I_FACTOR);
                ingModel.setValueAt(fmt(dm.getPPMActualizado()),            i, I_PPM_ACT);
                ingModel.setValueAt(fmt(dm.getOtrosIngresosPercibidos()),   i, I_OTROS_ING);
                ingModel.setValueAt(fmt(dm.getIngresosNoPercibidos()),      i, I_NO_PERC);

                // Egresos
                egModel.setValueAt(MESES[i],                               i, E_MES);
                egModel.setValueAt(fmt(dm.getComprasAfectasNetas()),        i, E_COMP_AF);
                egModel.setValueAt(fmt(dm.getNotasCreditoCompra()),         i, E_NC_COMP);
                egModel.setValueAt(fmt(dm.getNotasDebitoCompra()),          i, E_ND_COMP);
                egModel.setValueAt(fmt(ivaCF),                             i, E_IVA_CF);
                egModel.setValueAt(fmt(dm.getComprasExentasIVA()),          i, E_COMP_EX);
                egModel.setValueAt(fmt(dm.getComprasAfectasIVAnoRecNeto()), i, E_COMP_NOREC);
                egModel.setValueAt(fmt(ivaNoRec),                          i, E_IVA_NOREC);
                egModel.setValueAt(fmt(dm.getActivoFijoPagado()),           i, E_AF);
                egModel.setValueAt(fmt(dm.getRemuneracionesPagadas()),      i, E_REMUN);
                egModel.setValueAt(fmt(dm.getHonorariosPagados()),          i, E_HONOR);
                egModel.setValueAt(fmt(dm.getArrendosPagados()),            i, E_ARR);
                egModel.setValueAt(fmt(dm.getGastosGenerales()),            i, E_GASTOS);
                egModel.setValueAt(fmt(dm.getOtrosEgresos()),               i, E_OTROS_EG);
                egModel.setValueAt(fmt(dm.getEgresosAdeudados()),           i, E_ADEUD);
                egModel.setValueAt(fmt(dm.getRetiroSocio1Historico()),      i, E_RET_S1);
                egModel.setValueAt(fmt(dm.getRetiroSocio2Historico()),      i, E_RET_S2);
            }
        } finally { updating = false; }
        for (int i = 0; i < 12; i++) recalcIngRow(i, -1);
        recalcIngTotals();
        recalcEgTotals();
    }

    public void saveToModel() {
        if (ingTable.isEditing()) ingTable.getCellEditor().stopCellEditing();
        if (egTable.isEditing()) egTable.getCellEditor().stopCellEditing();
        Map<Integer, DatosMes> datos = frame.getDatos();
        for (int i = 0; i < 12; i++) {
            DatosMes dm = datos.getOrDefault(i + 1, new DatosMes(i + 1));
            dm.setMes(i + 1);
            // Ingresos
            dm.setVentasAfectasNetas(parseIG(i, I_VTA_AF));
            dm.setNotasCreditoVenta(parseIG(i, I_NC_VTA));
            dm.setNotasDebitoVenta(parseIG(i, I_ND_VTA));
            dm.setIvaDebito(parseIG(i, I_IVA_DF));
            dm.setVentasExentasIVA(parseIG(i, I_VTA_EX));
            dm.setTasaPPM(parsePctIG(i));
            dm.setOtrosIngresosPercibidos(parseIG(i, I_OTROS_ING));
            dm.setIngresosNoPercibidos(parseIG(i, I_NO_PERC));
            dm.setFactorReajustePPM(parseFactorIG(i));
            // Egresos
            dm.setComprasAfectasNetas(parseEG(i, E_COMP_AF));
            dm.setNotasCreditoCompra(parseEG(i, E_NC_COMP));
            dm.setNotasDebitoCompra(parseEG(i, E_ND_COMP));
            dm.setIvaCredFiscal(parseEG(i, E_IVA_CF));
            dm.setComprasExentasIVA(parseEG(i, E_COMP_EX));
            dm.setComprasAfectasIVAnoRecNeto(parseEG(i, E_COMP_NOREC));
            dm.setActivoFijoPagado(parseEG(i, E_AF));
            dm.setRemuneracionesPagadas(parseEG(i, E_REMUN));
            dm.setHonorariosPagados(parseEG(i, E_HONOR));
            dm.setArrendosPagados(parseEG(i, E_ARR));
            dm.setGastosGenerales(parseEG(i, E_GASTOS));
            dm.setOtrosEgresos(parseEG(i, E_OTROS_EG));
            dm.setEgresosAdeudados(parseEG(i, E_ADEUD));
            dm.setRetiroSocio1Historico(parseEG(i, E_RET_S1));
            dm.setRetiroSocio2Historico(parseEG(i, E_RET_S2));
            datos.put(i + 1, dm);
        }
        frame.setDatos(datos);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void checkReajusteConfig(JLabel hint) {
        GlobalConfig cfg = frame.getGlobalConfig();
        boolean allDefault = true;
        if (cfg != null) {
            for (int m = 1; m <= 11; m++) {
                if (cfg.getFactor(m) != 1.0) { allDefault = false; break; }
            }
        }
        if (allDefault) {
            hint.setText("⚠ Factores corrección monetaria = 1,0 — Configure en ⚙ Configuración");
            hint.setForeground(new Color(0xC0, 0x39, 0x2B));
            hint.setFont(new Font("SansSerif", Font.BOLD, 11));
        } else {
            hint.setText("✓ Factores de corrección monetaria configurados");
            hint.setForeground(Theme.GREEN_DARK);
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        }
    }

    private void addLegend(JPanel p, Color bg, Color fg, String text) {
        JLabel l = new JLabel("  " + text + "  ");
        l.setFont(Theme.FONT_SMALL);
        l.setBackground(bg); l.setForeground(fg);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createLineBorder(Theme.LIGHT_BLUE));
        p.add(l);
    }

    private static String formatNum(double v) {
        if (v == 0) return "0";
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("es", "CL"));
        nf.setGroupingUsed(true);
        return nf.format(Math.round(v));
    }

    private static String fmt(double v) { return String.valueOf(Math.round(v)); }

    private static String fmtPct(double v) {
        return String.format("%.3f", v * 100).replace(".", ",");
    }

    private static double parseRaw(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        s = s.trim().replace("$", "").replace(" ", "");
        if (s.matches(".*\\.\\d{3}$") || s.matches("\\d{1,3}(\\.\\d{3})+")) {
            s = s.replace(".", "");
        } else {
            s = s.replace(",", ".").replace(".", "");
        }
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private static String normalizeInput(String s) {
        if (s == null || s.trim().isEmpty()) return "0";
        return String.valueOf(Math.round(parseRaw(s)));
    }

    private double parseIG(int row, int col) {
        Object v = ingModel.getValueAt(row, col);
        return v == null ? 0 : parseRaw(v.toString());
    }

    private double parseEG(int row, int col) {
        Object v = egModel.getValueAt(row, col);
        return v == null ? 0 : parseRaw(v.toString());
    }

    private double parsePctIG(int row) {
        Object v = ingModel.getValueAt(row, I_TASA_PPM);
        if (v == null) return 0.002;
        String s = v.toString().trim().replace("%", "").replace(" ", "").replace(",", ".");
        try { return Double.parseDouble(s) / 100.0; } catch (Exception e) { return 0.002; }
    }

    private double parseFactorIG(int row) {
        Object v = ingModel.getValueAt(row, I_FACTOR);
        if (v == null) return 0;
        String s = v.toString().trim().replace(" ", "").replace(",", ".");
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private static String fmtFactor(double v) {
        return String.format("%.4f", v).replace(".", ",");
    }

    /** True if the factor value in the cell differs from the global auto-fill value for that row. */
    private boolean isFactorManual(int row, Object cellValue) {
        if (cellValue == null) return false;
        String s = cellValue.toString().trim();
        if (s.isEmpty()) return false;
        double current;
        try { current = Double.parseDouble(s.replace(",", ".")); }
        catch (Exception e) { return false; }
        double autoVal = autoFillFactorForRow(row);
        return Math.abs(current - autoVal) > 0.0001;
    }

    /**
     * Devuelve true si el IVA ingresado difiere del cálculo automático
     * (neto efectivo × 0.19) por más de $1 peso. Aplica tanto a ingresos
     * (IVA Débito) como egresos (IVA CF).
     */
    private boolean isIvaDiscrepant(JTable tbl, int row) {
        try {
            double ivaActual, ivaEsperado;
            if (tbl == ingTable) {
                double vtaAf = parseIG(row, I_VTA_AF);
                double nc    = parseIG(row, I_NC_VTA);
                double nd    = parseIG(row, I_ND_VTA);
                ivaActual    = parseIG(row, I_IVA_DF);
                ivaEsperado  = Math.round((vtaAf - nc + nd) * 0.19);
            } else {
                double compAf = parseEG(row, E_COMP_AF);
                double nc     = parseEG(row, E_NC_COMP);
                double nd     = parseEG(row, E_ND_COMP);
                ivaActual     = parseEG(row, E_IVA_CF);
                ivaEsperado   = Math.round((compAf - nc + nd) * 0.19);
            }
            // Si ambos son 0 → no hay discrepancia
            if (ivaActual == 0 && ivaEsperado == 0) return false;
            return Math.abs(ivaActual - ivaEsperado) > 1.0;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildIvaTooltip(JTable tbl, int row) {
        try {
            double ivaEsperado;
            if (tbl == ingTable) {
                double vtaAf = parseIG(row, I_VTA_AF);
                double nc    = parseIG(row, I_NC_VTA);
                double nd    = parseIG(row, I_ND_VTA);
                ivaEsperado  = Math.round((vtaAf - nc + nd) * 0.19);
            } else {
                double compAf = parseEG(row, E_COMP_AF);
                double nc     = parseEG(row, E_NC_COMP);
                double nd     = parseEG(row, E_ND_COMP);
                ivaEsperado   = Math.round((compAf - nc + nd) * 0.19);
            }
            return "<html>⚠ IVA editado manualmente.<br>" +
                   "Cálculo automático: $ " + String.format("%,.0f", ivaEsperado) + "</html>";
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the auto-fill factor for a row based on GlobalConfig and default mes de pago.
     *  Default mes de pago = mes siguiente al período (Ene→Feb, etc). Dic queda en 1.0. */
    private double autoFillFactorForRow(int row) {
        GlobalConfig cfg = frame.getGlobalConfig();
        if (cfg == null) return 1.0;
        int mesPeriodo = row + 1;
        if (mesPeriodo >= 12) return 1.0;
        int mesPago = mesPeriodo + 1;
        return cfg.getFactor(mesPago);
    }

    public void autoFillFactores() {
        updating = true;
        try {
            for (int i = 0; i < 12; i++) {
                double f = autoFillFactorForRow(i);
                ingModel.setValueAt(fmtFactor(f), i, I_FACTOR);
            }
        } finally { updating = false; }
        for (int i = 0; i < 12; i++) recalcIngRow(i, -1);
    }
}

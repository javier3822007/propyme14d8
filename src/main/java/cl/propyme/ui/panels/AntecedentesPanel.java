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

import cl.propyme.model.*;
import cl.propyme.ui.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import cl.propyme.model.Ejercicio;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;
import cl.propyme.model.GlobalConfig;
import java.text.NumberFormat;
import java.util.Locale;

public class AntecedentesPanel extends JPanel {

    private final MainFrame frame;

    // Año anterior
    private JTextField fCptsPos, fCptsNeg, fPerdida, fExistAnt, fCapInicial;
    // Ingreso diferido
    private JTextField fIdSaldo;
    // Dividendos (5)
    private JTextField[] fDivDetalle = new JTextField[5];
    private JTextField[] fDivMonto   = new JTextField[5];
    private JTextField[] fDivCredito = new JTextField[5];
    // 33 bis
    private JTextField fFactor33bis;
    // UF/UTM
    private JTextField fUF, fUTM;
    private boolean loading = false;

    public AntecedentesPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0,0));
        build();
        loadData();
        attachDirtyListeners();
    }

    /** Attach a single DocumentListener to every JTextField that calls frame.markDirty()
     *  unless loading is true (which is set during loadData). */
    private void attachDirtyListeners() {
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            private void onChange() { if (!loading) frame.markDirty(); }
        };
        JTextField[] all = { fCptsPos, fCptsNeg, fPerdida, fExistAnt, fCapInicial, fIdSaldo,
                             fFactor33bis, fUF, fUTM };
        for (JTextField f : all) if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : fDivDetalle) if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : fDivMonto)   if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : fDivCredito) if (f != null) f.getDocument().addDocumentListener(dl);

        // ── Validación visual: todos los campos numéricos solo aceptan positivos ──
        // (Los valores con signo negativo en el cálculo se manejan internamente)
        for (JTextField f : all)         if (f != null) cl.propyme.ui.InputValidator.attach(f, this);
        for (JTextField f : fDivMonto)   if (f != null) cl.propyme.ui.InputValidator.attach(f, this);
        for (JTextField f : fDivCredito) if (f != null) cl.propyme.ui.InputValidator.attach(f, this);
    }

    private void build() {
        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14,24,14,24));
        JLabel t = new JLabel("Antecedentes del Ejercicio");
        t.setFont(Theme.FONT_HEADER); t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Datos fijos del ejercicio — ingresar una sola vez por año");
        s.setFont(Theme.FONT_SMALL); s.setForeground(Theme.LIGHT_BLUE);
        JPanel tp = new JPanel(new GridLayout(2,1,0,2)); tp.setOpaque(false);
        tp.add(t); tp.add(s);
        hdr.add(tp, BorderLayout.WEST);
        JButton saveBtn = Theme.primaryButton("💾 Guardar");
        saveBtn.addActionListener(e -> {
            if (!cl.propyme.ui.InputValidator.validarTodos(this)) {
                int errs = cl.propyme.ui.InputValidator.contarErrores(this);
                int resp = JOptionPane.showConfirmDialog(this,
                    "⚠ Se detectaron " + errs + " campo(s) con valores inválidos (marcados en rojo).\n\n" +
                    "Guardar igualmente puede producir cálculos incorrectos.\n" +
                    "Desea guardar de todas formas?",
                    "Errores de validación",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (resp != JOptionPane.YES_OPTION) return;
            }
            saveData(); frame.guardarTodo();
        });
        hdr.add(saveBtn, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        // Scrollable form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.BG);
        form.setBorder(BorderFactory.createEmptyBorder(16,24,16,24));

        // ── I. Año anterior ───────────────────────────────────────────────────
        form.add(sectionHeader("I. Antecedentes Año Anterior (desde F-22 AT anterior)"));
        JPanel p1 = inputGrid();
        fCptsPos  = addRow(p1, "CPTS Positivo Inicial (Cód. 1580 F-22):", "0",
                           "Desde Recuadro 23 del AT anterior");
        fCptsNeg  = addRow(p1, "CPTS Negativo Inicial (Cód. 1582 F-22):", "0",
                           "Ingresar si aplica (en positivo)");
        fPerdida  = addRow(p1, "Pérdida tributaria año anterior (Cód. 1627):", "0",
                           "Desde Recuadro 22 del AT anterior");
        fExistAnt = addRow(p1, "Existencias adeudadas ejerc. ant., pagadas este año (Cód. 1820):", "0",
                           "Deudas de compras del año anterior pagadas en este ejercicio");
        fCapInicial = addRow(p1, "Capital aportado inicio actividades (Cód. 1573):", "0",
                           "Solo para empresas en su primer ejercicio comercial");
        form.add(p1);
        form.add(mutedNote("⚠ No ingrese simultáneamente CPTS (1580/1582) y Capital inicial (1573) — pueden generar inconsistencias en el cálculo."));

        // ── II. Ingreso diferido ───────────────────────────────────────────────
        form.add(Box.createRigidArea(new Dimension(0,10)));
        form.add(sectionHeader("II. Ingreso Diferido Pendiente (Recuadro N°7) — si aplica"));
        JPanel p2 = inputGrid();
        fIdSaldo = addRow(p2, "Saldo ingreso diferido pendiente de tributación (Cód. 1358):", "0",
                          "Se amortiza en mínimo 10 ejercicios (1/10 por año)");
        form.add(p2);
        form.add(mutedNote("El sistema calculará automáticamente 1/10 del saldo como ingreso diferido del ejercicio (Cód. 1608)."));

        // ── III. Dividendos ────────────────────────────────────────────────────
        form.add(Box.createRigidArea(new Dimension(0,10)));
        form.add(sectionHeader("III. Dividendos / Participaciones Percibidas — si aplica"));
        JPanel divPanel = new JPanel(new GridBagLayout());
        divPanel.setBackground(Theme.PANEL_BG);
        divPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(8,10,8,10)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4); gc.fill = GridBagConstraints.HORIZONTAL;
        // Column headers
        String[] divHdrs = {"#","Detalle / Tipo","Monto Percibido ($)","Crédito IDPC ($)"};
        for(int i=0;i<4;i++){
            gc.gridx=i; gc.gridy=0; gc.weightx = i==1?2:1;
            JLabel lh = new JLabel(divHdrs[i]);
            lh.setFont(Theme.FONT_BOLD); lh.setForeground(Theme.DARK_BLUE);
            divPanel.add(lh, gc);
        }
        String[] divDefaults = {
            "Dividendo afecto — IDPC sujeto a restitución con D° dev. (Art. 14 A)",
            "Dividendo afecto — IDPC no sujeto a restitución con D° dev.",
            "Dividendo afecto — crédito acumulado al 31.12.2016",
            "Dividendo afecto — sin crédito IDPC",
            "Dividendo INR (Ingreso No Renta)"
        };
        for(int i=0;i<5;i++){
            fDivDetalle[i] = Theme.inputField(divDefaults[i]);
            fDivMonto[i]   = Theme.inputField("0");
            fDivCredito[i] = Theme.inputField("0");
            gc.gridy=i+1;
            gc.gridx=0; gc.weightx=0;
            JLabel num = new JLabel(String.valueOf(i+1));
            num.setFont(Theme.FONT_SMALL); num.setForeground(Theme.TEXT_DARK);
            divPanel.add(num, gc);
            gc.gridx=1; gc.weightx=2; divPanel.add(fDivDetalle[i], gc);
            gc.gridx=2; gc.weightx=1; divPanel.add(fDivMonto[i], gc);
            gc.gridx=3; gc.weightx=1; divPanel.add(fDivCredito[i], gc);
        }
        form.add(divPanel);

        // ── IV. Crédito 33 bis ─────────────────────────────────────────────────
        form.add(Box.createRigidArea(new Dimension(0,10)));
        form.add(sectionHeader("IV. Crédito Art. 33 bis LIR (Activo Fijo Adquirido en el Ejercicio)"));
        JPanel p4 = inputGrid();
        fFactor33bis = addRow(p4, "Factor IPC para actualizar activo fijo (mes adquisición → dic):", "1.0000",
                              "Crédito = Activo Fijo × Factor × 6%");
        form.add(p4);
        form.add(mutedNote("El activo fijo se obtiene automáticamente desde Datos Mensuales."));

        // ── V. IPC y valores de referencia ────────────────────────────────────
        form.add(Box.createRigidArea(new Dimension(0,10)));
        form.add(sectionHeader("V. Valores de Referencia del Ejercicio"));
        JPanel p5 = inputGrid();
        fUF     = addRow(p5, "Valor UF al 31 de diciembre ($):", "39727.96",
                         "Desde www.cmfchile.cl");
        fUTM    = addRow(p5, "Valor UTM de diciembre ($):", "69542",
                         "Desde www.sii.cl — requerido para tabla IGC");
        form.add(p5);

        // ── VI. Factores corrección monetaria (solo lectura) ─────────────────────
        form.add(Box.createRigidArea(new Dimension(0,10)));
        form.add(sectionHeader("VI. Factores de Corrección Monetaria del Año (solo lectura)"));
        form.add(buildFactoresReadOnly());
        form.add(mutedNote("Para modificar estos valores vaya a ⚙ Configuración en el menú lateral"));

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Data binding ──────────────────────────────────────────────────────────

    public void loadData() {
        Ejercicio ej = frame.getEjercicioActual();
        if (ej == null) return;
        loading = true;
        try {
            fCptsPos.setText(fmt(ej.getCptsPositivoInicial()));
            fCptsNeg.setText(fmt(ej.getCptsNegativoInicial()));
            fPerdida.setText(fmt(ej.getPerdidaAnterior()));
            fExistAnt.setText(fmt(ej.getExistenciasAdeudadasAnteriores()));
            fCapInicial.setText(fmt(ej.getCapitalInicial()));
            fIdSaldo.setText(fmt(ej.getSaldoIngresoDiferido()));
            for(int i=0;i<5;i++){
                fDivDetalle[i].setText(ej.getDividendoDetalle()[i]);
                fDivMonto[i].setText(fmt(ej.getDividendoMonto()[i]));
                fDivCredito[i].setText(fmt(ej.getDividendoCredito()[i]));
            }
            fFactor33bis.setText(String.format("%.4f", ej.getActivoFijoFactor33bis()));
            fUF.setText(fmt(ej.getUfDiciembre()));
            fUTM.setText(fmt(ej.getUtmDiciembre()));
        } finally { loading = false; }
    }

    public void saveData() {
        Ejercicio ej = frame.getEjercicioActual();
        if (ej == null) return;
        ej.setCptsPositivoInicial(parseD(fCptsPos));
        ej.setCptsNegativoInicial(parseD(fCptsNeg));
        ej.setPerdidaAnterior(parseD(fPerdida));
        ej.setExistenciasAdeudadasAnteriores(parseD(fExistAnt));
        ej.setCapitalInicial(parseD(fCapInicial));
        ej.setSaldoIngresoDiferido(parseD(fIdSaldo));
        double[] dm = new double[5]; double[] dc = new double[5]; String[] dd = new String[5];
        for(int i=0;i<5;i++){
            dm[i]=parseD(fDivMonto[i]); dc[i]=parseD(fDivCredito[i]);
            dd[i]=fDivDetalle[i].getText().trim();
        }
        ej.setDividendoMonto(dm); ej.setDividendoCredito(dc); ej.setDividendoDetalle(dd);
        ej.setActivoFijoFactor33bis(parseD(fFactor33bis));
        ej.setUfDiciembre(parseD(fUF));
        ej.setUtmDiciembre(parseD(fUTM));
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        p.setBackground(Theme.MID_BLUE);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_BOLD); l.setForeground(Color.WHITE);
        p.add(l); return p;
    }

    private JPanel inputGrid() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.PANEL_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,200));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(8,10,8,10)));
        return p;
    }

    private JTextField addRow(JPanel p, String label, String defVal, String note) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4); gc.anchor = GridBagConstraints.WEST;
        int row = p.getComponentCount() / 3;
        gc.gridy = row; gc.fill = GridBagConstraints.NONE;
        gc.gridx=0; gc.weightx=0;
        JLabel lbl = Theme.label(label);
        lbl.setPreferredSize(new Dimension(420,22));
        p.add(lbl, gc);
        gc.gridx=1; gc.weightx=1; gc.fill=GridBagConstraints.HORIZONTAL;
        JTextField f = Theme.inputField(defVal);
        f.setPreferredSize(new Dimension(160,28));
        p.add(f, gc);
        gc.gridx=2; gc.weightx=2; gc.fill=GridBagConstraints.HORIZONTAL;
        JLabel n = Theme.mutedLabel(note);
        p.add(n, gc);
        return f;
    }

    private JLabel mutedNote(String text) {
        JLabel l = new JLabel("  💡 " + text);
        l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_DARK);
        l.setBorder(BorderFactory.createEmptyBorder(2,0,6,0));
        return l;
    }

    private String fmt(double v) {
        if (v==0) return "0";
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(new java.util.Locale("es","CL"));
        return nf.format(Math.round(v));
    }
    private double parseD(JTextField f) {
        String s = f.getText().trim().replace("$","").replace(" ","");
        // Manejar separador chileno de miles (punto) vs decimal (coma)
        s = s.replace(".", "").replace(",", ".");
        try { return Double.parseDouble(s); } catch(Exception e) { return 0; }
    }

    private javax.swing.JPanel buildFactoresReadOnly() {
        String[] meses = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                          "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.GridBagLayout());
        p.setBackground(Theme.PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(6,10,6,10)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.insets = new java.awt.Insets(2,6,2,6);
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cl.propyme.model.GlobalConfig cfg = frame.getGlobalConfig();
        for (int i=0;i<12;i++) {
            int mes=i+1;
            gc.gridy=i/3; gc.gridx=(i%3)*2; gc.weightx=1;
            JLabel lbl = Theme.label(meses[i]+":");
            p.add(lbl,gc);
            gc.gridx=(i%3)*2+1; gc.weightx=1;
            double f = cfg!=null ? cfg.getFactor(mes) : 1.0;
            JLabel val = new JLabel(String.format("%.4f",f).replace(".",","));
            val.setFont(Theme.FONT_BOLD); val.setForeground(Theme.MID_BLUE);
            p.add(val,gc);
        }
        return p;
    }

}

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

import cl.propyme.model.GlobalConfig;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class ConfigPanel extends JPanel {

    private static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    private final MainFrame frame;
    private JComboBox<Integer> yearCombo;
    private JLabel activeYearLabel;
    private final JTextField[] factorFields = new JTextField[13];
    // IGC table fields: [desde, hasta, factor, rebaja] x 8 rows
    private final JTextField[] igcDesde   = new JTextField[8];
    private final JTextField[] igcHasta   = new JTextField[8];
    private final JTextField[] igcFactor  = new JTextField[8];
    private final JTextField[] igcRebaja  = new JTextField[8];
    private GlobalConfig currentConfig;
    private boolean loading = false;

    public ConfigPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        build();
        attachDirtyListeners();
    }

    private void attachDirtyListeners() {
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            private void onChange() { if (!loading) frame.markDirty(); }
        };
        for (JTextField f : factorFields) if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : igcDesde)     if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : igcHasta)     if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : igcFactor)    if (f != null) f.getDocument().addDocumentListener(dl);
        for (JTextField f : igcRebaja)    if (f != null) f.getDocument().addDocumentListener(dl);
    }

    private void build() {
        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14,24,14,24));
        JLabel t = new JLabel("⚙  Configuración Global — Parámetros Tributarios por Año");
        t.setFont(Theme.FONT_HEADER); t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Factores IPC y Tabla IGC — aplican a todas las empresas como valor por defecto");
        s.setFont(Theme.FONT_SMALL); s.setForeground(Theme.LIGHT_BLUE);
        JPanel tp = new JPanel(new GridLayout(2,1,0,2)); tp.setOpaque(false);
        tp.add(t); tp.add(s);
        hdr.add(tp, BorderLayout.WEST);

        // Year selector bar
        JPanel yearBar = new JPanel(new FlowLayout(FlowLayout.LEFT,12,10));
        yearBar.setBackground(Theme.PANEL_BG);
        yearBar.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Theme.LIGHT_BLUE));
        yearBar.add(Theme.label("Año:"));
        int cy = Calendar.getInstance().get(Calendar.YEAR);
        yearCombo = new JComboBox<>(buildYearOptions(cy));
        yearCombo.setFont(Theme.FONT_LABEL);
        yearBar.add(yearCombo);
        JButton loadBtn = Theme.secondaryButton("Cargar");
        loadBtn.addActionListener(e -> loadYear((Integer)yearCombo.getSelectedItem()));
        yearBar.add(loadBtn);
        JButton saveBtn = Theme.primaryButton("💾 Guardar Configuración");
        saveBtn.addActionListener(e -> saveConfig());
        yearBar.add(saveBtn);

        activeYearLabel = new JLabel("  ⚠ Sin año cargado  ");
        activeYearLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        activeYearLabel.setForeground(Color.WHITE);
        activeYearLabel.setBackground(Theme.DANGER);
        activeYearLabel.setOpaque(true);
        activeYearLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        yearBar.add(Box.createHorizontalStrut(16));
        yearBar.add(activeYearLabel);


        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(hdr, BorderLayout.NORTH);
        northWrapper.add(yearBar, BorderLayout.SOUTH);
        add(northWrapper, BorderLayout.NORTH);

        // Main scrollable content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(16,24,16,24));

        // ── I. Factores corrección monetaria ─────────────────────────────────
        content.add(sectionHdr("I. Factores de Corrección Monetaria por Mes"));
        content.add(Theme.mutedLabel(
            "  Formato: 1,015 = 1,5% | 1,000 = sin reajuste | Diciembre siempre = 1,0000"));
        content.add(Box.createRigidArea(new Dimension(0,6)));

        JPanel factGrid = new JPanel(new GridBagLayout());
        factGrid.setBackground(Theme.PANEL_BG);
        factGrid.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(8,12,8,12)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3,6,3,6); gc.fill = GridBagConstraints.HORIZONTAL;
        // Headers
        gc.gridy=0;
        for (int c=0;c<3;c++) {
            gc.gridx=c; gc.weightx=c==1?2:1;
            JLabel lh = new JLabel(new String[]{"Mes","Factor de Reajuste","Nota"}[c]);
            lh.setFont(Theme.FONT_BOLD); lh.setForeground(Theme.DARK_BLUE);
            factGrid.add(lh,gc);
        }
        for (int i=0;i<12;i++) {
            int mes=i+1; gc.gridy=i+1;
            Color bg = i%2==0 ? Theme.LIGHT_BLUE : Theme.PANEL_BG;
            gc.gridx=0; gc.weightx=1;
            JLabel lbl = new JLabel(MESES[i]);
            lbl.setFont(Theme.FONT_LABEL); lbl.setForeground(Theme.TEXT_DARK);
            lbl.setOpaque(true); lbl.setBackground(bg);
            lbl.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
            factGrid.add(lbl,gc);
            gc.gridx=1; gc.weightx=2;
            JTextField tf = mes==12 ? Theme.calcField() : Theme.inputField("1,0000");
            tf.setPreferredSize(new Dimension(120,26));
            if (mes==12) { tf.setText("1,0000"); tf.setToolTipText("Diciembre = 1,0000 (sin reajuste)"); }
            factorFields[mes]=tf;
            factGrid.add(tf,gc);
            gc.gridx=2; gc.weightx=2;
            // PPM reference note
            String nota = mes<12 ? "PPM "+MESES[i]+" → paga "+MESES[mes] : "Sin reajuste";
            JLabel nl = new JLabel(nota);
            nl.setFont(new Font("SansSerif",Font.ITALIC,10));
            nl.setForeground(Theme.TEXT_DARK);
            factGrid.add(nl,gc);
        }
        factGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE,500));
        content.add(factGrid);

        // ── II. Tabla IGC ─────────────────────────────────────────────────────
        content.add(Box.createRigidArea(new Dimension(0,16)));
        content.add(sectionHdr("II. Tabla Impuesto Global Complementario (IGC)"));
        content.add(Theme.mutedLabel(
            "  Ingrese los tramos según tabla oficial del SII para el año tributario"));
        content.add(Box.createRigidArea(new Dimension(0,6)));

        JPanel igcGrid = new JPanel(new GridBagLayout());
        igcGrid.setBackground(Theme.PANEL_BG);
        igcGrid.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(8,12,8,12)));
        GridBagConstraints gc2 = new GridBagConstraints();
        gc2.insets = new Insets(3,6,3,6); gc2.fill = GridBagConstraints.HORIZONTAL;
        // Headers
        gc2.gridy=0;
        String[] igcHdrs = {"Tramo","Desde ($)","Hasta ($)","Factor","Rebaja ($)"};
        int[] igcW = {0,2,2,1,2};
        for (int c=0;c<5;c++) {
            gc2.gridx=c; gc2.weightx=igcW[c];
            JLabel lh = new JLabel(igcHdrs[c]);
            lh.setFont(Theme.FONT_BOLD); lh.setForeground(Theme.DARK_BLUE);
            igcGrid.add(lh,gc2);
        }
        for (int i=0;i<8;i++) {
            Color bg = i%2==0 ? Theme.LIGHT_BLUE : Theme.PANEL_BG;
            gc2.gridy=i+1;
            gc2.gridx=0; gc2.weightx=0;
            JLabel num = new JLabel(String.valueOf(i+1));
            num.setFont(Theme.FONT_LABEL); num.setForeground(Theme.TEXT_DARK);
            num.setOpaque(true); num.setBackground(bg);
            num.setHorizontalAlignment(SwingConstants.CENTER);
            igcGrid.add(num,gc2);
            gc2.gridx=1; gc2.weightx=2;
            igcDesde[i] = Theme.inputField("0"); igcDesde[i].setPreferredSize(new Dimension(130,26));
            igcGrid.add(igcDesde[i],gc2);
            gc2.gridx=2; gc2.weightx=2;
            igcHasta[i] = Theme.inputField(i==7?"y más":"0"); igcHasta[i].setPreferredSize(new Dimension(130,26));
            igcGrid.add(igcHasta[i],gc2);
            gc2.gridx=3; gc2.weightx=1;
            igcFactor[i] = Theme.inputField("0"); igcFactor[i].setPreferredSize(new Dimension(80,26));
            igcGrid.add(igcFactor[i],gc2);
            gc2.gridx=4; gc2.weightx=2;
            igcRebaja[i] = Theme.inputField("0"); igcRebaja[i].setPreferredSize(new Dimension(130,26));
            igcGrid.add(igcRebaja[i],gc2);
        }
        igcGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE,340));
        content.add(igcGrid);

        // ── III. Sistema de Respaldo (Backup) ─────────────────────────────────
        content.add(Box.createRigidArea(new Dimension(0,24)));
        content.add(sectionHdr("III. Sistema de Respaldo (Backup)"));
        content.add(Theme.mutedLabel(
            "Crea copias de seguridad de todos sus datos (empresas, ejercicios y balances)."));
        content.add(Box.createRigidArea(new Dimension(0,8)));

        JPanel backupBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        backupBtns.setOpaque(false);
        backupBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        backupBtns.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnCrearBackup    = Theme.primaryButton("💾 Crear respaldo manual");
        JButton btnRestaurar      = Theme.secondaryButton("📂 Restaurar respaldo");
        JButton btnListar         = Theme.secondaryButton("📋 Ver respaldos");

        btnCrearBackup.addActionListener(e -> accionCrearBackup());
        btnRestaurar  .addActionListener(e -> accionRestaurar());
        btnListar     .addActionListener(e -> accionListar());

        backupBtns.add(btnCrearBackup);
        backupBtns.add(btnRestaurar);
        backupBtns.add(btnListar);
        content.add(backupBtns);

        content.add(Box.createRigidArea(new Dimension(0,6)));
        content.add(Theme.mutedLabel(
            "ℹ Los respaldos automáticos se generan al guardar (últimos 10 conservados)."));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadYear(cy);
    }



    /** Refreshes the year dropdown to reflect current empresa's ejercicios */
    public void refreshYears() {
        int selected = yearCombo.getSelectedItem() != null ? (int) yearCombo.getSelectedItem()
            : Calendar.getInstance().get(Calendar.YEAR);
        yearCombo.removeAllItems();
        for (Integer y : buildYearOptions(selected)) yearCombo.addItem(y);
        // Restore selection
        yearCombo.setSelectedItem(selected);
    }

    /** Builds year list: ejercicios of active empresa + current/prev year, sorted descending */
    private Integer[] buildYearOptions(int defaultYear) {
        java.util.TreeSet<Integer> set = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
        set.add(defaultYear);
        set.add(defaultYear - 1);
        cl.propyme.model.Empresa emp = frame.getEmpresaActual();
        if (emp != null) {
            java.util.List<Integer> ejs = frame.getStore().listarEjercicios(emp.getRut());
            set.addAll(ejs);
        }
        return set.toArray(new Integer[0]);
    }

    public void loadYear(int anio) {
        loading = true;
        try {
            currentConfig = frame.getStore().cargarConfig(anio);
            activeYearLabel.setText("  ✏ Editando año: " + anio + "  ");
            activeYearLabel.setBackground(Theme.GREEN_DARK);
            // Factores
            for (int mes=1; mes<=12; mes++) {
                double f = currentConfig.getFactor(mes);
                factorFields[mes].setText(String.format("%.4f",f).replace(".",","));
            }
            // IGC table
            double[][] igc = currentConfig.getIgcTable();
            NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("es","CL"));
            for (int i=0;i<8&&i<igc.length;i++) {
                igcDesde[i].setText(nf.format(Math.round(igc[i][0])));
                igcHasta[i].setText(i==7 ? "y más" : nf.format(Math.round(igc[i][1])));
                igcFactor[i].setText(String.valueOf(igc[i][2]));
                igcRebaja[i].setText(nf.format(Math.round(igc[i][3])));
            }
        } finally { loading = false; }
    }

    private void saveConfig() {
        if (currentConfig==null) return;
        // Factores
        for (int mes=1;mes<=11;mes++) {
            String s=factorFields[mes].getText().trim().replace(",",".");
            try { double v=Double.parseDouble(s); currentConfig.setFactor(mes,v>0?v:1.0); }
            catch (Exception e) { currentConfig.setFactor(mes,1.0); }
        }
        currentConfig.setFactor(12,1.0);
        // IGC table
        double[][] igc = new double[8][4];
        for (int i=0;i<8;i++) {
            igc[i][0] = parseNum(igcDesde[i].getText());
            igc[i][1] = i==7 ? Double.MAX_VALUE : parseNum(igcHasta[i].getText());
            igc[i][2] = parseDecimal(igcFactor[i].getText());
            igc[i][3] = parseNum(igcRebaja[i].getText());
        }
        currentConfig.setIgcTable(igc);

        new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception {
                frame.getStore().guardarConfig(currentConfig); return null;
            }
            protected void done() {
                try {
                    get();
                    frame.setGlobalConfig(currentConfig);
                    String orig=frame.getTitle();
                    frame.setTitle("✓ Configuración guardada — "+orig);
                    new Timer(2000,e->frame.setTitle(orig)){{setRepeats(false);}}.start();
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(ConfigPanel.this,"Error: "+ex.getMessage());
                }
            }
        }.execute();
    }

    private JLabel sectionHdr(String text) {
        JLabel l = new JLabel("  "+text);
        l.setFont(Theme.FONT_BOLD); l.setForeground(Color.WHITE);
        l.setOpaque(true); l.setBackground(Theme.MID_BLUE);
        l.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        return l;
    }

    private double parseNum(String s) {
        if (s==null||s.trim().isEmpty()||s.trim().equalsIgnoreCase("y más")) return 0;
        try { return Double.parseDouble(s.replace(".","").replace(",",".").replace("$","").replace(" ","")); }
        catch (Exception e) { return 0; }
    }

    private double parseDecimal(String s) {
        if (s==null||s.trim().isEmpty()) return 0;
        try { return Double.parseDouble(s.trim().replace(",",".")); }
        catch (Exception e) { return 0; }
    }

    // ── Acciones de Backup ────────────────────────────────────────────────────

    private void accionCrearBackup() {
        try {
            String baseDir = System.getProperty("app.home", System.getProperty("user.dir"));
            java.nio.file.Path archivo =
                cl.propyme.service.BackupService.crear(baseDir);
            JOptionPane.showMessageDialog(this,
                "✔ Respaldo creado correctamente.\n\n" +
                "Archivo: " + archivo.getFileName() + "\n" +
                "Ubicación: " + archivo.getParent(),
                "Respaldo creado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al crear el respaldo:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionRestaurar() {
        String baseDir = System.getProperty("app.home", System.getProperty("user.dir"));
        java.util.List<java.nio.file.Path> backups =
            cl.propyme.service.BackupService.listarBackups(baseDir);
        if (backups.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No hay respaldos disponibles para restaurar.",
                "Sin respaldos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] opciones = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++)
            opciones[i] = cl.propyme.service.BackupService.descripcionBackup(backups.get(i));
        String sel = (String) JOptionPane.showInputDialog(this,
            "Seleccione el respaldo a restaurar:",
            "Restaurar respaldo",
            JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (sel == null) return;
        int idx = -1;
        for (int i = 0; i < opciones.length; i++) if (opciones[i].equals(sel)) { idx = i; break; }
        if (idx < 0) return;
        int conf = JOptionPane.showConfirmDialog(this,
            "⚠ Esta acción REEMPLAZARÁ los datos actuales con los del respaldo.\n" +
            "Se creará un respaldo de seguridad antes de continuar.\n\n" +
            "Desea continuar?",
            "Confirmar restauración",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;
        try {
            cl.propyme.service.BackupService.restaurar(baseDir, backups.get(idx));
            JOptionPane.showMessageDialog(this,
                "✔ Respaldo restaurado correctamente.\n\n" +
                "Reinicie el programa para que los cambios surtan efecto.",
                "Restauración exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al restaurar:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionListar() {
        String baseDir = System.getProperty("app.home", System.getProperty("user.dir"));
        java.util.List<java.nio.file.Path> backups =
            cl.propyme.service.BackupService.listarBackups(baseDir);
        if (backups.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No hay respaldos disponibles.",
                "Sin respaldos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Total: ").append(backups.size()).append(" respaldo(s)\n\n");
        for (java.nio.file.Path p : backups) {
            sb.append("• ").append(cl.propyme.service.BackupService.descripcionBackup(p)).append("\n");
        }
        JTextArea ta = new JTextArea(sb.toString(), 15, 50);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this,
            new JScrollPane(ta),
            "Respaldos disponibles", JOptionPane.INFORMATION_MESSAGE);
    }
}

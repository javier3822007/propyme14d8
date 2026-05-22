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
import cl.propyme.service.*;
import cl.propyme.ui.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
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
import java.io.File;
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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import cl.propyme.model.DatosMes;
import cl.propyme.model.RegistroRCV;
import cl.propyme.service.RCVParser;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;

public class ImportarRCVPanel extends JPanel {

    private final MainFrame frame;
    private JComboBox<String> mesCombo;
    private JComboBox<String> tipoCombo;
    private JTable previewTable;
    private DefaultTableModel previewModel;
    private JLabel statusLabel;
    private JLabel summaryLabel;
    private List<RegistroRCV> registrosCargados = new ArrayList<>();

    private static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    public ImportarRCVPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 8));
        build();
    }

    private void build() {
        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JLabel title = new JLabel("Importar Registro de Compras y Ventas (RCV) — SII");
        title.setFont(Theme.FONT_HEADER); title.setForeground(Color.WHITE);
        JLabel hint = new JLabel("Archivos CSV exportados desde www.sii.cl → Registro de Compras y Ventas");
        hint.setFont(Theme.FONT_SMALL); hint.setForeground(Theme.LIGHT_BLUE);
        JPanel t2 = new JPanel(new GridLayout(2,1,0,2)); t2.setOpaque(false);
        t2.add(title); t2.add(hint);
        hdr.add(t2, BorderLayout.WEST);

        // Control bar
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        ctrl.setBackground(Theme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Theme.LIGHT_BLUE));

        ctrl.add(Theme.label("Mes:"));
        mesCombo = new JComboBox<>(MESES);
        mesCombo.setFont(Theme.FONT_LABEL);
        ctrl.add(mesCombo);

        ctrl.add(Theme.label("Tipo:"));
        tipoCombo = new JComboBox<>(new String[]{"Ventas","Compras"});
        tipoCombo.setFont(Theme.FONT_LABEL);
        ctrl.add(tipoCombo);

        JButton loadBtn = Theme.primaryButton("📂  Cargar Archivo CSV");
        ctrl.add(loadBtn);
        loadBtn.addActionListener(e -> loadFile());

        JButton applyBtn = Theme.secondaryButton("✓  Aplicar al Mes");
        ctrl.add(applyBtn);
        applyBtn.addActionListener(e -> applyToMonth());

        statusLabel = new JLabel("Sin archivo cargado");
        statusLabel.setFont(Theme.FONT_SMALL); statusLabel.setForeground(Theme.TEXT_DARK); //0x55,0x66));
        ctrl.add(statusLabel);

        // Wrap header + control bar en un panel NORTH compartido
        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(hdr, BorderLayout.NORTH);
        northWrapper.add(ctrl, BorderLayout.SOUTH);
        add(northWrapper, BorderLayout.NORTH);

        // Summary bar
        summaryLabel = new JLabel("  Totales del archivo: —");
        summaryLabel.setFont(Theme.FONT_SMALL); summaryLabel.setForeground(Theme.TEXT_DARK);
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        summaryLabel.setOpaque(true); summaryLabel.setBackground(Theme.TOTAL_BG);

        // Preview table
        String[] cols = {"#","Tipo Doc","Folio","RUT","Razón Social","Fecha","Neto ($)","Exento ($)","IVA ($)","Total ($)","Sin efecto"};
        previewModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        previewTable = new JTable(previewModel);
        Theme.styleTable(previewTable);
        previewTable.setTableHeader(new javax.swing.table.JTableHeader(previewTable.getColumnModel()) {
            @Override public void paintComponent(java.awt.Graphics g) {
                g.setColor(Theme.DARK_BLUE);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        });
        previewTable.getTableHeader().setDefaultRenderer(Theme.makeHeaderRenderer());
        previewTable.setRowHeight(22);
        int[] pw = {35,160,70,100,200,90,110,110,110,110,80};
        for(int i=0;i<pw.length&&i<previewTable.getColumnCount();i++)
            previewTable.getColumnModel().getColumn(i).setPreferredWidth(pw[i]);
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Color rows: guías de despacho in gray, NCs in orange
        previewTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t,v,sel,focus,row,col);
                if (!sel) {
                    String sinEfecto = (String) previewModel.getValueAt(row, 10);
                    String tipoDoc   = (String) previewModel.getValueAt(row, 1);
                    if ("Sí".equals(sinEfecto))
                        setBackground(new Color(0xF0,0xF0,0xF0));
                    else if (tipoDoc != null && tipoDoc.contains("Crédito"))
                        setBackground(new Color(0xFF,0xF0,0xCC));
                    else if (tipoDoc != null && tipoDoc.contains("Débito"))
                        setBackground(new Color(0xCC,0xEE,0xFF));
                    else
                        setBackground(row%2==0 ? Color.WHITE : new Color(0xF0,0xF5,0xFF));
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(previewTable);

        JPanel center = new JPanel(new BorderLayout());
        center.add(summaryLabel, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        legend.setBackground(Theme.BG);
        addLegendItem(legend, new Color(0xFF,0xF0,0xCC), "Nota de Crédito (resta)");
        addLegendItem(legend, new Color(0xCC,0xEE,0xFF), "Nota de Débito (suma)");
        addLegendItem(legend, new Color(0xF0,0xF0,0xF0), "Guía de Despacho (sin efecto contable)");
        center.add(legend, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // Info panel
        JPanel info = Theme.card(null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(10,16,10,16));
        String[] tipos = {
            "Documentos reconocidos:",
            "33 — Factura Afecta  |  34 — Factura No Afecta/Exenta  |  39 — Boleta Afecta",
            "41 — Boleta No Afecta  |  43/46 — Liquidación-Factura",
            "56 — Nota de Débito  |  61 — Nota de Crédito  |  110-112 — Exportación",
            "52 — Guía de Despacho → se importa pero NO tiene efecto contable (no se acumula).",
        };
        for(String s : tipos){
            JLabel l = new JLabel(s);
            l.setFont(Theme.FONT_SMALL);
            l.setForeground(s.startsWith("Documentos") ? Theme.DARK_BLUE : Theme.TEXT_MUTED);
            info.add(l);
        }
        add(info, BorderLayout.EAST);
    }

    private void loadFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar archivo RCV del SII");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)","csv"));
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        RCVParser.TipoArchivo tipo = tipoCombo.getSelectedIndex() == 0
            ? RCVParser.TipoArchivo.VENTA : RCVParser.TipoArchivo.COMPRA;

        try {
            registrosCargados = RCVParser.parse(f, tipo);
            showPreview(registrosCargados);
            statusLabel.setText("✓ " + f.getName() + " — " + registrosCargados.size() + " documentos");
            statusLabel.setForeground(Theme.SUCCESS);

            // Advertir si el parser detectó CSV inconsistente
            String inconsistencia = RCVParser.getUltimaInconsistenciaCsv();
            if (inconsistencia != null) {
                JOptionPane.showMessageDialog(frame,
                    "⚠ Advertencia de importación:\n\n" + inconsistencia +
                    "\n\nRevise los datos cargados antes de aplicarlos a los meses.",
                    "CSV con estructura inconsistente", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error al leer archivo:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("✗ Error: " + ex.getMessage());
            statusLabel.setForeground(Theme.DANGER);
        }
    }

    private void showPreview(List<RegistroRCV> regs) {
        previewModel.setRowCount(0);
        double totNeto=0, totExento=0, totIVA=0, totTotal=0;
        for(int i=0;i<regs.size();i++){
            RegistroRCV r = regs.get(i);
            previewModel.addRow(new Object[]{
                i+1,
                RegistroRCV.descripcionTipoDoc(r.getTipoDoc()),
                r.getFolio(),
                r.getRutContraparte(),
                r.getRazonSocial(),
                r.getFechaDocto(),
                fmt(r.getMontoNeto()),
                fmt(r.getMontoExento()),
                fmt(r.getMontoIVARecuperable()),
                fmt(r.getMontoTotal()),
                r.tieneEfectoContable() ? "No" : "Sí"
            });
            if(r.tieneEfectoContable()){
                double sign = r.esNotaCredito() ? -1 : 1;
                totNeto   += sign*r.getMontoNeto();
                totExento += sign*r.getMontoExento();
                totIVA    += sign*r.getMontoIVARecuperable();
                totTotal  += sign*r.getMontoTotal();
            }
        }
        summaryLabel.setText(String.format(
            "  Totales contables: Neto $%s  |  Exento $%s  |  IVA $%s  |  Total $%s",
            Theme.PESO_FMT.format(Math.round(totNeto)),
            Theme.PESO_FMT.format(Math.round(totExento)),
            Theme.PESO_FMT.format(Math.round(totIVA)),
            Theme.PESO_FMT.format(Math.round(totTotal))));
    }

    private void applyToMonth() {
        if (registrosCargados.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Primero cargue un archivo CSV.");
            return;
        }
        int mes = mesCombo.getSelectedIndex() + 1;
        RegistroRCV.Tipo tipo = tipoCombo.getSelectedIndex() == 0
            ? RegistroRCV.Tipo.VENTA : RegistroRCV.Tipo.COMPRA;

        RCVParser.AggregatedTotals totals = RCVParser.aggregate(registrosCargados, tipo);

        Map<Integer, DatosMes> datos = frame.getDatos();
        DatosMes dm = datos.getOrDefault(mes, new DatosMes(mes));
        dm.setMes(mes);

        if (tipo == RegistroRCV.Tipo.VENTA) {
            dm.setVentasAfectasNetas(totals.ventasAfectasNetas);
            dm.setVentasExentasIVA(totals.ventasExentasNetas);
            dm.setIvaDebito(totals.ivaDebito);
        } else {
            dm.setComprasAfectasNetas(totals.comprasAfectasNetas);
            dm.setComprasExentasIVA(totals.comprasExentasNetas);
            dm.setComprasAfectasIVAnoRecNeto(0); // IVA no rec set separately
            dm.setActivoFijoPagado(totals.activoFijoNeto);
            // IVA Crédito Fiscal total = CF compras + CF activo fijo (útil para auditoría F29)
            dm.setIvaCredFiscal(totals.ivaCredFiscal + totals.ivaActivoFijo);
        }
        dm.setDocumentosVenta(tipo == RegistroRCV.Tipo.VENTA ? registrosCargados : dm.getDocumentosVenta());
        dm.setDocumentosCompra(tipo == RegistroRCV.Tipo.COMPRA ? registrosCargados : dm.getDocumentosCompra());
        datos.put(mes, dm);
        frame.setDatos(datos);
        frame.invalidateCache();
        if (frame.getDatosMensualesPanel() != null) frame.getDatosMensualesPanel().loadFromModel();
        frame.recalculate();

        JOptionPane.showMessageDialog(frame,
            "✓ Datos aplicados a " + MESES[mes-1] + " (" + tipo + ").\n" +
            "Revise y ajuste en la pantalla de Datos Mensuales si es necesario.",
            "Importación exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addLegendItem(JPanel p, Color c, String text) {
        JPanel dot = new JPanel();
        dot.setBackground(c);
        dot.setPreferredSize(new Dimension(14,14));
        dot.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        p.add(dot);
        p.add(Theme.mutedLabel(text));
    }

    private String fmt(double v) {
        return v == 0 ? "-" : Theme.PESO_FMT.format(Math.round(v));
    }
}

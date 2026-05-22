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
import cl.propyme.service.CalculadorImpuesto;
import cl.propyme.ui.*;
import javax.swing.*;
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
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import cl.propyme.model.DatosMes;
import cl.propyme.model.Ejercicio;
import cl.propyme.model.Empresa;
import cl.propyme.model.Resultados;
import cl.propyme.model.Socio;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;

public class CertificacionPanel extends JPanel {

    private final MainFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JPanel igcPanel;

    public CertificacionPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0,0));
        build();
    }

    private void build() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14,24,14,24));
        JLabel t = new JLabel("Certificación de Rentas — DJ F1947 y Liquidación IGC");
        t.setFont(Theme.FONT_HEADER); t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Régimen Art. 14 D N°8 — Distribución a propietarios y cálculo de impuesto");
        s.setFont(Theme.FONT_SMALL); s.setForeground(Theme.LIGHT_BLUE);
        JPanel tp = new JPanel(new GridLayout(2,1,0,2)); tp.setOpaque(false);
        tp.add(t); tp.add(s);
        hdr.add(tp, BorderLayout.WEST);
        JButton ref = new JButton("⟳ Recalcular");
        ref.setFont(Theme.FONT_LABEL);
        ref.setBackground(Color.WHITE);
        ref.setForeground(Theme.DARK_BLUE);
        ref.setOpaque(true);
        ref.setFocusPainted(false);
        ref.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14))); ref.addActionListener(e -> { frame.invalidateCache(); frame.recalculate(); });
        hdr.add(ref, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0,8));
        center.setBackground(Theme.BG);
        center.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));

        // F1947 table
        String[] cols = {"N°","RUT Titular","% Asig.","BI Asignada ($)",
            "Retiros ($)","Créd.IDPC N/R S/Dev","Créd.IDPC N/R C/Dev",
            "Créd.IDPC Rest.S/Dev","Créd.IDPC Rest.C/Dev",
            "Créd.33 bis ($)","PPM Disp. ($)"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        Theme.styleTable(table);
        // Override paintComponent to guarantee dark background fills entire header
        table.setTableHeader(new javax.swing.table.JTableHeader(table.getColumnModel()) {
            @Override public void paintComponent(java.awt.Graphics g) {
                g.setColor(Theme.DARK_BLUE);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        });
        table.getTableHeader().setBackground(Theme.DARK_BLUE);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setDefaultRenderer(Theme.makeHeaderRenderer());
        table.setRowHeight(28);
        int[] pw = {30,110,70,140,110,140,140,140,140,110,110};
        for(int i=0;i<pw.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(pw[i]);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createDarkCorner());
        scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, createDarkCorner());
        scroll.getViewport().setBackground(Theme.PANEL_BG);
        // Paint header viewport dark to eliminate white gap
        scroll.setPreferredSize(new Dimension(0,160));
        center.add(scroll, BorderLayout.NORTH);

        // IGC section per socio
        igcPanel = new JPanel();
        igcPanel.setLayout(new BoxLayout(igcPanel, BoxLayout.Y_AXIS));
        igcPanel.setBackground(Theme.BG);
        JScrollPane igcScroll = new JScrollPane(igcPanel);
        igcScroll.setBorder(BorderFactory.createEmptyBorder());
        center.add(igcScroll, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        Ejercicio ej = frame.getEjercicioActual();
        Empresa emp = frame.getEmpresaActual();
        if (ej == null || emp == null) return;

        Resultados r = frame.getCachedResultados();
        if (r == null) return;
        List<Socio> socios = emp.getSocios();
        double[] biPorSocio = CalculadorImpuesto.distribuirBaseImponible(r, socios);

        model.setRowCount(0);
        double totRetHist = 0;
        Map<Integer,DatosMes> datos = frame.getDatos();
        for(DatosMes dm : datos.values()) totRetHist += dm.getTotalRetirosHistorico();

        // Distribuir cred33, ppm y retiros con cuadre exacto (último socio se lleva el resto)
        double[] cred33PorSocio = distribuirExacto(r.credito33bis,        socios);
        double[] ppmPorSocio    = distribuirExacto(r.ppmActualizadoTotal, socios);
        double[] retiroPorSocio = distribuirExacto(totRetHist,             socios);

        for(int i=0;i<socios.size();i++){
            Socio s = socios.get(i);
            double bi = biPorSocio[i];
            double cred33 = cred33PorSocio[i];
            double ppm    = ppmPorSocio[i];
            double retiro = retiroPorSocio[i];
            model.addRow(new Object[]{
                i+1, s.getRut(),
                Theme.formatPct(s.getPorcentaje()),
                Theme.formatPeso(bi),
                Theme.formatPeso(retiro),
                "—","—","—","—",
                Theme.formatPeso(cred33),
                Theme.formatPeso(ppm)
            });
        }
        // Totals row
        model.addRow(new Object[]{"","TOTALES","100%",
            Theme.formatPeso(r.baseImponible1630),
            Theme.formatPeso(totRetHist),
            "—","—","—","—",
            Theme.formatPeso(r.credito33bis),
            Theme.formatPeso(r.ppmActualizadoTotal)
        });

        // IGC per socio
        igcPanel.removeAll();
        igcPanel.add(makeSectionLabel("Liquidación Impuesto Global Complementario (IGC) — AT "+
            (ej.getAnioComercial()+1)));
        for(int i=0;i<socios.size();i++){
            Socio s = socios.get(i);
            double bi = biPorSocio[i];
            double igcBruto = CalculadorImpuesto.calcularIGC(bi, frame.getGlobalConfig());
            double cred33   = cred33PorSocio[i];
            double ppm      = ppmPorSocio[i];
            double igcNeto  = Math.max(0, igcBruto - cred33);
            double resultado = igcNeto - ppm;

            igcPanel.add(buildSocioIGC(s, i, bi, igcBruto, cred33, ppm, igcNeto, resultado));
            igcPanel.add(Box.createRigidArea(new Dimension(0,8)));
        }
        igcPanel.revalidate(); igcPanel.repaint();
    }

    /**
     * Distribuye un total entre socios según su porcentaje, asignando el resto
     * exacto al último socio para evitar diferencias por redondeo.
     */
    private static double[] distribuirExacto(double total, List<Socio> socios) {
        double[] result = new double[socios.size()];
        if (socios.isEmpty()) return result;
        double acumulado = 0;
        for (int i = 0; i < socios.size() - 1; i++) {
            result[i] = Math.round(total * socios.get(i).getPorcentaje());
            acumulado += result[i];
        }
        result[socios.size() - 1] = Math.round(total - acumulado);
        return result;
    }

    private JPanel buildSocioIGC(Socio s, int idx, double bi, double igcBruto,
            double cred33, double ppm, double igcNeto, double resultado) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(10,14,10,14)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        JLabel title = new JLabel("Socio "+(idx+1)+": "+s.getNombre()+" — "+s.getRut());
        title.setFont(Theme.FONT_BOLD); title.setForeground(Theme.DARK_BLUE);
        p.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0,2,6,4));
        grid.setBackground(Theme.PANEL_BG);
        grid.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));

        addIGCRow(grid,"Base Imponible Asignada ("+Theme.formatPct(s.getPorcentaje())+")",Theme.formatPeso(bi));
        addIGCRow(grid,"IGC según tabla Art. 52 LIR",Theme.formatPeso(igcBruto));
        addIGCRow(grid,"(-) Crédito Art. 33 bis",Theme.formatPeso(cred33));
        addIGCRow(grid,"IGC Neto",Theme.formatPeso(igcNeto));
        addIGCRow(grid,"(-) PPM puesto a disposición",Theme.formatPeso(ppm));

        JLabel resLabel = new JLabel(resultado >= 0
            ? "IMPUESTO A PAGAR: "+Theme.formatPeso(resultado)
            : "DEVOLUCIÓN A SOLICITAR: "+Theme.formatPeso(Math.abs(resultado)));
        resLabel.setFont(new Font("SansSerif",Font.BOLD,13));
        resLabel.setForeground(resultado >= 0 ? Theme.DANGER : Theme.GREEN_DARK);
        resLabel.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
        grid.add(new JLabel("Resultado Liquidación:"));
        grid.add(resLabel);

        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private void addIGCRow(JPanel p, String label, String value) {
        JLabel l = Theme.label(label);
        JLabel v = Theme.label(value);
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        v.setFont(Theme.FONT_BOLD);
        p.add(l); p.add(v);
    }

    private JLabel makeSectionLabel(String text) {
        JLabel l = new JLabel("  " + text);
        l.setFont(Theme.FONT_BOLD); l.setForeground(Color.WHITE);
        l.setOpaque(true); l.setBackground(Theme.MID_BLUE);
        l.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));
        return l;
    }

    private static javax.swing.JPanel createDarkCorner() {
        javax.swing.JPanel p = new javax.swing.JPanel();
        p.setBackground(Theme.DARK_BLUE);
        return p;
    }

}

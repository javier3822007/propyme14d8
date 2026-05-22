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
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import cl.propyme.model.Ejercicio;
import cl.propyme.model.Resultados;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;

public class Recuadro23Panel extends JPanel {

    private final MainFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JLabel cptsLabel;
    private JLabel ppmLabel;

    public Recuadro23Panel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0,0));
        build();
    }

    private void build() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14,24,14,24));
        JLabel t = new JLabel("Recuadro N°23 — Capital Propio Tributario Simplificado al 31.12");
        t.setFont(Theme.FONT_HEADER); t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Art. 14 D N°8 LIR — calculado automáticamente");
        s.setFont(Theme.FONT_SMALL); s.setForeground(Theme.LIGHT_BLUE);
        JPanel tp = new JPanel(new GridLayout(2,1,0,2)); tp.setOpaque(false);
        tp.add(t); tp.add(s);
        hdr.add(tp, BorderLayout.WEST);
        JButton refBtn = new JButton("⟳ Recalcular");
        refBtn.setFont(Theme.FONT_LABEL);
        refBtn.setBackground(Color.WHITE);
        refBtn.setForeground(Theme.DARK_BLUE);
        refBtn.setOpaque(true);
        refBtn.setFocusPainted(false);
        refBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        refBtn.addActionListener(e -> { frame.invalidateCache(); frame.recalculate(); });
        hdr.add(refBtn, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        String[] cols = {"Código","Concepto","(+/-)","Monto ($)"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){return false;}
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
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(550);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t,v,sel,focus,row,col);
                if (!sel) {
                    Object tag = model.getValueAt(row, 2);
                    String sign = tag==null?"":tag.toString();
                    if ("RESULT".equals(sign)) {
                        setBackground(Theme.ORANGE);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                    } else if ("(=)".equals(sign)) {
                        setBackground(Theme.TOTAL_BG);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                    } else {
                        setBackground(row%2==0?Color.WHITE:new Color(0xF5,0xF8,0xFF));
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_LABEL);
                    }
                    setHorizontalAlignment(col==3||col==2 ? RIGHT : (col==0?CENTER:LEFT));
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createDarkCorner());
        scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, createDarkCorner());
        scroll.getViewport().setBackground(Theme.PANEL_BG);
        // Paint header viewport dark to eliminate white gap
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1,2,8,0));
        bottom.setBackground(Theme.BG);
        bottom.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        cptsLabel = new JLabel("CPTS Final: —");
        cptsLabel.setFont(new Font("SansSerif",Font.BOLD,14));
        cptsLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ORANGE),
            BorderFactory.createEmptyBorder(10,14,10,14)));
        cptsLabel.setOpaque(true); cptsLabel.setBackground(new Color(0xFF,0xF8,0xE8));
        ppmLabel = new JLabel("PPM Actualizado Total: —");
        ppmLabel.setFont(new Font("SansSerif",Font.BOLD,14));
        ppmLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(10,14,10,14)));
        ppmLabel.setOpaque(true); ppmLabel.setBackground(Theme.TOTAL_BG);
        bottom.add(cptsLabel); bottom.add(ppmLabel);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        Ejercicio ej = frame.getEjercicioActual();
        if (ej == null) return;
        Resultados r = frame.getCachedResultados();
        if (r == null) return;
        model.setRowCount(0);
        addRow("1580","CPT o CPTS Positivo inicial al 01.01 (desde F-22 AT anterior)","(+)",r.cpts1580);
        addRow("1582","CPT o CPTS Negativo inicial al 01.01 (desde F-22 AT anterior)","(-)",r.cpts1582);
        addRow("1573","Capital aportado (empresas que inician actividades)","(+)",r.cpts1573);
        addRow("1574","Aumentos efectivos de capital del ejercicio","(+)",r.cpts1574);
        addRow("1575","Disminuciones efectivas de capital del ejercicio","(-)",r.cpts1575);
        addRow("1712","Base imponible del ejercicio asignable a propietarios","(+)",r.cpts1712);
        addRow("1713","Pérdida tributaria del ejercicio al 31 de diciembre","(-)",r.cpts1713);
        addRow("1714","Pérdidas tributarias de ejercicios anteriores","(+)",r.cpts1714);
        addRow("1576","Remesas, retiros o dividendos repartidos en el ejercicio (histórico)","(-)",r.cpts1576);
        addRow("1715","Ingreso diferido imputado en el ejercicio","(-)",r.cpts1715);
        addRow("1577","Partidas de gastos no aceptados (Art. 21 LIR)","(-)",r.cpts1577);
        addRow("1716","Crédito Art. 33 bis LIR","(-)",r.cpts1716);
        addRow("1578","Crédito IDPC por participaciones que incrementaron la BI","(-)",r.cpts1578);
        addRow("1584","Otras partidas a agregar","(+)",r.cpts1584);
        addRow("1585","Otras partidas a deducir","(-)",r.cpts1585);

        // Results
        model.addRow(new Object[]{"1581","CPTS POSITIVO FINAL","RESULT",
            r.cptsPositivo1581 > 0
                ? "$ "+Theme.PESO_FMT.format(Math.round(r.cptsPositivo1581))
                : "—"});
        model.addRow(new Object[]{"1583","CPTS NEGATIVO FINAL","(=)",
            r.cptsNegativo1583 > 0
                ? "$ "+Theme.PESO_FMT.format(Math.round(r.cptsNegativo1583))
                : "—"});

        // PPM summary section
        model.addRow(new Object[]{"","","",""});
        model.addRow(new Object[]{"","── RESUMEN PPM ──","",""});
        addRow("","PPM Histórico Total","",r.ppmHistoricoTotal);
        addRow("","PPM Actualizado Total (reajustado IPC)","",r.ppmActualizadoTotal);
        addRow("","Reajuste PPM (Cód. 1607)","",r.reajustePPM);
        addRow("","Crédito Art. 33 bis LIR (Cód. 1609)","",r.credito33bis);
        addRow("","Ingreso diferido imputado en el ejercicio (Cód. 1608)","",r.ingresoDiferidoImputado);
        addRow("","Saldo ingreso diferido para ejercicios siguientes","",r.saldoIngresoDiferidoFinal);

        String cptsStr = r.cptsPositivo1581 > 0
            ? "🏦 CPTS Positivo Final (Cód. 1581): $ "+Theme.PESO_FMT.format(Math.round(r.cptsPositivo1581))
            : "🏦 CPTS Negativo Final (Cód. 1583): $ "+Theme.PESO_FMT.format(Math.round(r.cptsNegativo1583));
        cptsLabel.setText(cptsStr);
        cptsLabel.setForeground(r.cptsPositivo1581 > 0 ? Theme.GREEN_DARK : Theme.DANGER);
        ppmLabel.setText("💰 PPM Actualizado: $ "+Theme.PESO_FMT.format(Math.round(r.ppmActualizadoTotal)));
    }

    private void addRow(String cod, String desc, String sign, double val) {
        model.addRow(new Object[]{cod, desc, sign,
            val==0 ? "—" : "$ "+Theme.PESO_FMT.format(Math.round(val))});
    }

    private static javax.swing.JPanel createDarkCorner() {
        javax.swing.JPanel p = new javax.swing.JPanel();
        p.setBackground(Theme.DARK_BLUE);
        return p;
    }

}

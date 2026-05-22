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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import cl.propyme.model.DatosMes;
import cl.propyme.model.Ejercicio;
import cl.propyme.model.Resultados;
import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;

public class Recuadro22Panel extends JPanel {

    private final MainFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JLabel biLabel;

    public Recuadro22Panel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0,0));
        build();
    }

    private void build() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14,24,14,24));
        JLabel t = new JLabel("Recuadro N°22 — Base Imponible Régimen Pro Pyme Transparente");
        t.setFont(Theme.FONT_HEADER); t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Art. 14 Letra D) N°8 LIR — calculado automáticamente desde Datos Mensuales y Antecedentes");
        s.setFont(Theme.FONT_SMALL); s.setForeground(Theme.LIGHT_BLUE);
        JPanel tp = new JPanel(new GridLayout(2,1,0,2)); tp.setOpaque(false);
        tp.add(t); tp.add(s);
        hdr.add(tp, BorderLayout.WEST);
        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        btnP.setOpaque(false);
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
        btnP.add(refBtn);
        hdr.add(btnP, BorderLayout.EAST);
        add(hdr, BorderLayout.NORTH);

        // Table
        String[] cols = {"(+/-)", "Código", "Concepto", "Monto ($)"};
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
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(550);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Custom renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t,v,sel,focus,row,col);
                if (!sel) {
                    Object tag = model.getValueAt(row, 0);
                    String sign = tag == null ? "" : tag.toString();
                    if ("TOTAL".equals(sign) || "(=)".equals(sign)) {
                        setBackground(Theme.TOTAL_BG);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                    } else if ("BI".equals(sign)) {
                        setBackground(Theme.ORANGE);
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_BOLD);
                    } else if ("SECT".equals(sign)) {
                        setBackground(Theme.MID_BLUE);
                        setForeground(Color.WHITE);
                        setFont(Theme.FONT_BOLD);
                    } else {
                        setBackground(row%2==0 ? Color.WHITE : new Color(0xF5,0xF8,0xFF));
                        setForeground(Theme.TEXT_DARK);
                        setFont(Theme.FONT_LABEL);
                    }
                    setHorizontalAlignment(col == 3 ? RIGHT : (col==0||col==1 ? CENTER : LEFT));
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

        // Bottom: result bars
        JPanel bottom = new JPanel(new GridLayout(1,2,8,0));
        bottom.setBackground(Theme.BG);
        bottom.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        biLabel = new JLabel("Base Imponible (Cód. 1630): —");
        biLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        biLabel.setForeground(Theme.DARK_BLUE);
        biLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ORANGE),
            BorderFactory.createEmptyBorder(10,14,10,14)));
        biLabel.setOpaque(true); biLabel.setBackground(new Color(0xFF,0xF8,0xE8));

        bottom.add(biLabel);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        Ejercicio ej = frame.getEjercicioActual();
        Map<Integer,DatosMes> datos = frame.getDatos();
        if (ej == null) return;

        Resultados r = frame.getCachedResultados();
        if (r == null) return;
        model.setRowCount(0);

        addSect("INGRESOS DEL EJERCICIO");
        addRow("(+)","1600","Ingresos del giro percibidos (afectos + exentos IVA)", r.ing1600);
        addRow("(+)","1819","Ingresos devengados ejercicios anteriores, percibidos este año", r.ing1819);
        addRow("(+)","1601","Rentas de fuente extranjera percibidas", r.ing1601);
        addRow("(+)","1602","Intereses y reajustes percibidos", r.ing1602);
        addRow("(+)","1603","Mayor valor por rescate/enajenación de inversiones", r.ing1603);
        addRow("(+)","1604","Dividendos / retiros percibidos por participación en otras empresas", r.ing1604);
        addRow("(+)","1605","Incremento por crédito IDPC asociado a dividendos percibidos", r.ing1605);
        addRow("(+)","1606","Ingresos devengados con empresas relacionadas Art. 14 A)", r.ing1606);
        addRow("(+)","1607","Otros ingresos percibidos o devengados (incl. reajuste PPM)", r.ing1607);
        addRow("(+)","1608","Ingreso diferido imputado en el ejercicio", r.ing1608);
        addRow("(+)","1609","Crédito Art. 33 bis LIR (activo fijo adquirido en el ejercicio)", r.ing1609);
        addTotal("(=)","1610","TOTAL DE INGRESOS ANUALES", r.totalIngresos1610);

        addSect("EGRESOS DEL EJERCICIO");
        addRow("(-)","1611","Gasto saldo inicial existencias en cambio de régimen", r.eg1611);
        addRow("(-)","1612","Gasto saldo inicial activo fijo depreciable en cambio de régimen", r.eg1612);
        addRow("(-)","1613","Pérdida tributaria en cambio de régimen", r.eg1613);
        addRow("(-)","1614","Existencias, insumos y servicios del negocio, pagados", r.eg1614);
        addRow("(-)","1820","Existencias adeudadas ejerc. anterior, pagadas este año", r.eg1820);
        addRow("(-)","1615","Gastos por rentas de fuente extranjera, pagados", r.eg1615);
        addRow("(-)","1616","Remuneraciones pagadas", r.eg1616);
        addRow("(-)","1617","Honorarios pagados", r.eg1617);
        addRow("(-)","1618","Adquisición bienes activo fijo pagados (depreciación instantánea)", r.eg1618);
        addRow("(-)","1620","Arriendos pagados", r.eg1620);
        addRow("(-)","1621","Gastos por exigencias medioambientales, pagados", r.eg1621);
        addRow("(-)","1622","Intereses y reajustes pagados por préstamos y otros", r.eg1622);
        addRow("(-)","1624","Pérdida en rescate/enajenación de inversiones o bienes", r.eg1624);
        addRow("(-)","1625","Otros gastos deducibles de los ingresos", r.eg1625);
        addRow("(-)","1626","Gastos/egresos por op. con empresas relacionadas Art. 14 A)", r.eg1626);
        addRow("(-)","1627","Pérdidas tributarias de ejercicios anteriores", r.eg1627);
        addRow("(-)","1628","Créditos incobrables castigados en el ejercicio", r.eg1628);
        addRow("(-)","1909","Gastos aceptados por donaciones", r.eg1909);
        addTotal("(=)","1629","TOTAL DE EGRESOS ANUALES", r.totalEgresos1629);

        // Base imponible
        model.addRow(new Object[]{"BI","1630",
            "BASE IMPONIBLE AFECTA A IMPUESTOS FINALES O PÉRDIDA TRIBUTARIA DEL EJERCICIO",
            r.baseImponible1630 >= 0
                ? "$ " + Theme.PESO_FMT.format(Math.round(r.baseImponible1630))
                : "(Pérdida) $ " + Theme.PESO_FMT.format(Math.round(Math.abs(r.baseImponible1630)))
        });

        // Summary labels
        String biStr = r.baseImponible1630 >= 0
            ? "📊 Base Imponible (Cód. 1630): $ " + Theme.PESO_FMT.format(Math.round(r.baseImponible1630))
            : "📊 Pérdida Tributaria (Cód. 1630): ($ " + Theme.PESO_FMT.format(Math.round(Math.abs(r.baseImponible1630))) + ")";
        biLabel.setText(biStr);
        biLabel.setForeground(r.baseImponible1630 >= 0 ? Theme.GREEN_DARK : Theme.DANGER);
    }

    private void addSect(String text) {
        model.addRow(new Object[]{"SECT","","  " + text, ""});
    }
    private void addRow(String sign, String cod, String desc, double val) {
        model.addRow(new Object[]{sign, cod, desc,
            val == 0 ? "—" : "$ " + Theme.PESO_FMT.format(Math.round(val))});
    }
    private void addTotal(String sign, String cod, String desc, double val) {
        model.addRow(new Object[]{"(=)", cod, desc,
            "$ " + Theme.PESO_FMT.format(Math.round(val))});
    }

    private static javax.swing.JPanel createDarkCorner() {
        javax.swing.JPanel p = new javax.swing.JPanel();
        p.setBackground(Theme.DARK_BLUE);
        return p;
    }

}

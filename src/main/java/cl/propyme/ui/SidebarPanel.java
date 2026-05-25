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
package cl.propyme.ui;

import javax.swing.*;
import cl.propyme.ui.MainFrame;
import javax.swing.border.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import cl.propyme.model.Empresa;

public class SidebarPanel extends JPanel {

    private final MainFrame frame;
    private final Map<String, JButton> buttons = new LinkedHashMap<>();
    private final JLabel empresaLabel;
    private final JLabel anioLabel;
    private String activeItem = "home";

    private static final String[][] MENU_ITEMS = {
        {"home",          "🏠  Inicio"},
        {null,            "── EMPRESA ──"},
        {"datos",         "📅  Datos Mensuales"},
        {"antecedentes",  "📋  Antecedentes"},
        {"importar",      "📂  Importar RCV (SII)"},
        {null,            "── DECLARACIÓN ──"},
        {"r22",           "📊  Recuadro 22 (BI)"},
        {"r23",           "🏦  Recuadro 23 (CPTS)"},
        {"cert",          "📜  Certificación F1947"},
        {"balance",       "⚖️  Balance 8 Columnas"},
        {null,            "── SISTEMA ──"},
        {"config",        "⚙  Configuración"},
        {"about",         "ℹ️  Acerca de"},
    };

    public SidebarPanel(MainFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(210, 0));
        setBackground(Theme.SIDEBAR_BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Logo / title
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(Theme.DARK_BLUE);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));
        JLabel logo = new JLabel("<html><b>ProPyme</b><br>Transparente</html>");
        logo.setFont(new Font("SansSerif", Font.BOLD, 16));
        logo.setForeground(Color.WHITE);
        logoPanel.add(logo, BorderLayout.CENTER);
        logoPanel.setMaximumSize(new Dimension(210, 80));
        add(logoPanel);

        // Empresa info
        JPanel empPanel = new JPanel();
        empPanel.setLayout(new BoxLayout(empPanel, BoxLayout.Y_AXIS));
        empPanel.setBackground(new Color(0x13, 0x1E, 0x36));
        empPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        empPanel.setMaximumSize(new Dimension(210, 70));
        empresaLabel = new JLabel("Sin empresa");
        empresaLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        empresaLabel.setForeground(Theme.SIDEBAR_FG);
        anioLabel = new JLabel("");
        anioLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        anioLabel.setForeground(new Color(0x44,0x55,0x66));
        empPanel.add(empresaLabel); empPanel.add(anioLabel);
        add(empPanel);

        add(Box.createRigidArea(new Dimension(0, 8)));

        // Menu items
        for (String[] item : MENU_ITEMS) {
            if (item[0] == null) {
                // Separador de sección
                JLabel sep = new JLabel(item[1]);
                sep.setFont(new Font("SansSerif", Font.PLAIN, 10));
                sep.setForeground(new Color(0x5C, 0x7A, 0x9A));
                sep.setBorder(BorderFactory.createEmptyBorder(12, 14, 4, 8));
                sep.setMaximumSize(new Dimension(210, 28));
                add(sep);
            } else {
                JButton btn = createNavButton(item[1], item[0]);
                buttons.put(item[0], btn);
                add(btn);
            }
        }

        add(Box.createVerticalGlue());

        // Inferior: Guardar + versión
        JButton saveBtn = new JButton("💾  Guardar Todo");
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        saveBtn.setBackground(Theme.GREEN_DARK);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false); saveBtn.setBorderPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setMaximumSize(new Dimension(210, 36));
        saveBtn.setAlignmentX(LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> frame.guardarTodo());
        add(saveBtn);

        JLabel ver = new JLabel("  v0.10.0  •  ProPyme Transparente");
        ver.setFont(new Font("SansSerif", Font.PLAIN, 9));
        ver.setForeground(new Color(0x44,0x55,0x66));
        ver.setMaximumSize(new Dimension(210, 24));
        add(ver);
        add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private JButton createNavButton(String text, String panelName) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setForeground(Theme.SIDEBAR_FG);
        b.setBackground(Theme.SIDEBAR_BG);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 8));
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setMaximumSize(new Dimension(210, 38));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!panelName.equals(activeItem))
                    b.setBackground(new Color(0x26, 0x3B, 0x5E));
            }
            public void mouseExited(MouseEvent e) {
                if (!panelName.equals(activeItem))
                    b.setBackground(Theme.SIDEBAR_BG);
            }
        });

        b.addActionListener(e -> frame.showPanel(panelName));
        return b;
    }

    public void setActiveItem(String name) {
        activeItem = name;
        buttons.forEach((k, btn) -> {
            if (k.equals(name)) {
                btn.setBackground(Theme.SIDEBAR_SEL);
                btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            } else {
                btn.setBackground(Theme.SIDEBAR_BG);
                btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            }
        });
    }

    public void setEmpresaActiva(String nombre, int anio) {
        // Truncate long names
        String short_name = nombre.length() > 22 ? nombre.substring(0, 20) + "…" : nombre;
        empresaLabel.setText(short_name);
        anioLabel.setText("Ejercicio " + anio);
    }
}

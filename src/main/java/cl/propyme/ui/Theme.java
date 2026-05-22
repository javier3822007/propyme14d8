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
import javax.swing.border.*;
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
import java.text.*;
import java.util.Locale;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class Theme {
    // Colors
    public static final Color DARK_BLUE   = new Color(0x1F, 0x38, 0x64);
    public static final Color MID_BLUE    = new Color(0x2E, 0x5F, 0xA3);
    public static final Color LIGHT_BLUE  = new Color(0xBD, 0xD7, 0xEE);
    public static final Color INPUT_GREEN = new Color(0xE2, 0xEF, 0xDA);
    public static final Color GREEN_DARK  = new Color(0x37, 0x56, 0x23);
    public static final Color TOTAL_BG    = new Color(0xD6, 0xE4, 0xF0);
    public static final Color ORANGE      = new Color(0xF4, 0xB9, 0x42);
    public static final Color BG          = new Color(0xF5, 0xF7, 0xFA);
    public static final Color PANEL_BG    = Color.WHITE;
    public static final Color TEXT_DARK   = new Color(0x1A, 0x1A, 0x2E);
    public static final Color TEXT_MUTED  = new Color(0x44, 0x55, 0x66);
    public static final Color SUCCESS     = new Color(0x27, 0xAE, 0x60);
    public static final Color WARNING     = new Color(0xF3, 0x9C, 0x12);
    public static final Color DANGER      = new Color(0xC0, 0x39, 0x2B);
    public static final Color SIDEBAR_BG  = new Color(0x1A, 0x2A, 0x4A);
    public static final Color SIDEBAR_FG  = new Color(0xCF, 0xD8, 0xE3);
    public static final Color SIDEBAR_SEL = new Color(0x2E, 0x5F, 0xA3);

    // Fonts
    public static final Font FONT_TITLE   = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_HEADER  = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_LABEL   = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BOLD    = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_SMALL   = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO    = new Font("Monospaced", Font.PLAIN, 12);

    // Number formats
    public static final NumberFormat PESO_FMT;
    public static final NumberFormat PCT_FMT;
    static {
        PESO_FMT = NumberFormat.getIntegerInstance(new Locale("es","CL"));
        PESO_FMT.setGroupingUsed(true);
        PCT_FMT = NumberFormat.getPercentInstance(new Locale("es","CL"));
        PCT_FMT.setMinimumFractionDigits(3);
        PCT_FMT.setMaximumFractionDigits(3);
    }

    public static String formatPeso(double v) {
        return "$ " + PESO_FMT.format(Math.round(v));
    }

    public static String formatPct(double v) {
        return String.format("%.3f%%", v * 100);
    }

    // ── Component factories ───────────────────────────────────────────────────

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE); l.setForeground(DARK_BLUE);
        return l;
    }

    public static JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADER); l.setForeground(DARK_BLUE);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL); l.setForeground(TEXT_DARK);
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(new Color(0x44, 0x55, 0x66));
        return l;
    }

    public static JTextField inputField() {
        JTextField f = new JTextField();
        f.setFont(FONT_LABEL);
        f.setBackground(INPUT_GREEN);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xA8, 0xD0, 0x8D)),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return f;
    }

    public static JTextField inputField(String text) {
        JTextField f = inputField();
        f.setText(text);
        return f;
    }

    public static JTextField calcField() {
        JTextField f = new JTextField();
        f.setFont(FONT_LABEL);
        f.setEditable(false);
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_BLUE),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return f;
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setBackground(MID_BLUE); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_LABEL);
        b.setBackground(Color.WHITE);
        b.setForeground(DARK_BLUE);  // dark text, always visible
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MID_BLUE),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_LABEL);
        b.setBackground(DANGER); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_BLUE),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        if (title != null && !title.isEmpty()) {
            JLabel lbl = new JLabel(title);
            lbl.setFont(FONT_BOLD); lbl.setForeground(DARK_BLUE);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            p.add(lbl, BorderLayout.NORTH);
        }
        return p;
    }

    public static JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBackground(MID_BLUE);
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD); l.setForeground(Color.WHITE);
        p.add(l);
        return p;
    }

    public static JPanel totalBar(String text, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(ORANGE);
        p.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD); lbl.setForeground(TEXT_DARK);
        JLabel val = new JLabel(value);
        val.setFont(FONT_BOLD); val.setForeground(TEXT_DARK);
        p.add(lbl, BorderLayout.WEST);
        p.add(val, BorderLayout.EAST);
        return p;
    }



    public static void styleTable(JTable t) {
        t.setFont(FONT_LABEL);
        t.setRowHeight(24);
        t.setShowGrid(true);
        t.setGridColor(LIGHT_BLUE);
        t.setBackground(PANEL_BG);
        t.setSelectionBackground(MID_BLUE);
        t.setSelectionForeground(Color.WHITE);
        t.setIntercellSpacing(new Dimension(1, 1));
        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(DARK_BLUE);
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        // Alternate row colors
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,col);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? PANEL_BG : new Color(0xF0, 0xF5, 0xFF));
                }
                return this;
            }
        });
    }

    public static Border paddedBorder(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    /** Returns a standalone header renderer for tables that need explicit dark headers */
    public static javax.swing.table.TableCellRenderer makeHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = new JLabel(value == null ? "" : value.toString());
                lbl.setFont(FONT_BOLD);
                lbl.setForeground(Color.WHITE);
                lbl.setBackground(DARK_BLUE);
                lbl.setOpaque(true);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setVerticalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 1, MID_BLUE),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));
                return lbl;
            }
        };
    }

}

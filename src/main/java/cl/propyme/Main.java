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
package cl.propyme;

import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;
import java.awt.Color;
import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // ── Look & Feel cross-platform ──────────────────────────────────────
        // Default automático según el sistema operativo:
        //   - Windows  → L&F nativo (se ve consistente con el sistema)
        //   - Linux    → Metal (resuelve problemas estéticos del L&F nativo)
        //   - macOS    → Metal (consistencia cross-platform)
        //
        // El usuario puede forzar otro L&F al invocar el programa:
        //   java -Dpropyme.laf=metal   -jar ProPyme.jar
        //   java -Dpropyme.laf=nimbus  -jar ProPyme.jar
        //   java -Dpropyme.laf=native  -jar ProPyme.jar
        String os = System.getProperty("os.name", "").toLowerCase();
        String defaultLaf = os.contains("win") ? "native" : "metal";
        String laf = System.getProperty("propyme.laf", defaultLaf).toLowerCase();

        try {
            switch (laf) {
                case "nimbus":
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                    break;
                case "native":
                case "system":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "metal":
                default:
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    aplicarTweaksMetal();
                    break;
            }
        } catch (Exception e) {
            // Si falla, Java usa el L&F por defecto. No es crítico — el programa
            // funciona igual con cualquier L&F.
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Aplica ajustes visuales sobre Metal para que coincida con el theme
     * azul del programa. Solo afecta los componentes "del sistema" que Metal
     * pinta con su estilo retro por defecto (scrollbars, dropdowns, listas,
     * botones de diálogos modales).
     *
     * Usa los colores definidos en Theme para coherencia visual total.
     */
    private static void aplicarTweaksMetal() {
        // ── ScrollBars ──────────────────────────────────────────────────
        UIManager.put("ScrollBar.thumb",            Theme.MID_BLUE);
        UIManager.put("ScrollBar.thumbShadow",      Theme.MID_BLUE);
        UIManager.put("ScrollBar.thumbHighlight",   Theme.MID_BLUE);
        UIManager.put("ScrollBar.thumbDarkShadow",  Theme.DARK_BLUE);
        UIManager.put("ScrollBar.track",            Theme.LIGHT_BLUE);
        UIManager.put("ScrollBar.trackHighlight",   Theme.LIGHT_BLUE);
        UIManager.put("ScrollBar.background",       Theme.BG);
        UIManager.put("ScrollBar.foreground",       Theme.DARK_BLUE);
        UIManager.put("ScrollBar.shadow",           Theme.LIGHT_BLUE);
        UIManager.put("ScrollBar.highlight",        Theme.LIGHT_BLUE);
        UIManager.put("ScrollBar.darkShadow",       Theme.MID_BLUE);

        // ── ComboBox / Dropdowns ────────────────────────────────────────
        UIManager.put("ComboBox.background",            Color.WHITE);
        UIManager.put("ComboBox.foreground",            Theme.TEXT_DARK);
        UIManager.put("ComboBox.selectionBackground",   Theme.MID_BLUE);
        UIManager.put("ComboBox.selectionForeground",   Color.WHITE);
        UIManager.put("ComboBox.buttonBackground",      Theme.LIGHT_BLUE);
        UIManager.put("ComboBox.buttonShadow",          Theme.MID_BLUE);
        UIManager.put("ComboBox.buttonDarkShadow",      Theme.DARK_BLUE);
        UIManager.put("ComboBox.buttonHighlight",       Theme.LIGHT_BLUE);

        // ── JList (listas en diálogos como "Restaurar Backup") ──────────
        UIManager.put("List.background",            Color.WHITE);
        UIManager.put("List.foreground",            Theme.TEXT_DARK);
        UIManager.put("List.selectionBackground",   Theme.MID_BLUE);
        UIManager.put("List.selectionForeground",   Color.WHITE);

        // ── OptionPane (diálogos modales) ───────────────────────────────
        UIManager.put("OptionPane.background",          Theme.BG);
        UIManager.put("OptionPane.messageForeground",   Theme.TEXT_DARK);
        UIManager.put("Panel.background",               Theme.BG);

        // ── Botones (en diálogos modales y otros) ───────────────────────
        UIManager.put("Button.background",  Color.WHITE);
        UIManager.put("Button.foreground",  Theme.DARK_BLUE);
        UIManager.put("Button.select",      Theme.MID_BLUE);
        UIManager.put("Button.focus",       Theme.MID_BLUE);
        UIManager.put("Button.shadow",      Theme.MID_BLUE);
        UIManager.put("Button.darkShadow",  Theme.DARK_BLUE);
        UIManager.put("Button.highlight",   Color.WHITE);
    }
}

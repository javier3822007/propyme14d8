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
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * InputValidator — validación visual de campos numéricos.
 *
 * Marca el campo en rojo con tooltip explicativo si el valor no es numérico
 * o está fuera del rango permitido. NO bloquea la edición — solo advierte.
 *
 * Uso típico:
 *   InputValidator.attach(campoUF, false);                // solo positivos
 *   InputValidator.attach(campoCpts, true);               // permite negativos
 *   InputValidator.attach(campoTasa, false, 0.0, 100.0);  // rango específico
 *
 * Antes de guardar:
 *   if (!InputValidator.validarTodos(this)) {
 *       JOptionPane.showMessageDialog(this, "Corrija los campos en rojo antes de guardar.");
 *       return;
 *   }
 */
public class InputValidator {

    private static final Color BG_ERROR  = new Color(0xFF, 0xE5, 0xE5);
    private static final Color FG_ERROR  = new Color(0xC0, 0x00, 0x00);
    private static final Border BORDER_ERROR =
        BorderFactory.createLineBorder(new Color(0xC0, 0x00, 0x00), 2);

    /** Lista global de campos validados, por panel. */
    private static final java.util.Map<JComponent, List<ValidatedField>> registry =
        new java.util.WeakHashMap<>();

    // ── API pública ──────────────────────────────────────────────────────────

    /** Acepta solo números positivos (incluido cero). */
    public static void attach(JTextField field, JComponent panelOwner) {
        attach(field, panelOwner, false, 0, Double.POSITIVE_INFINITY);
    }

    /** Permite negativos si allowNegative es true. */
    public static void attach(JTextField field, JComponent panelOwner, boolean allowNegative) {
        attach(field, panelOwner, allowNegative,
               allowNegative ? Double.NEGATIVE_INFINITY : 0,
               Double.POSITIVE_INFINITY);
    }

    /** Validación con rango específico. */
    public static void attach(JTextField field, JComponent panelOwner,
                              boolean allowNegative, double min, double max) {
        ValidatedField vf = new ValidatedField(field, allowNegative, min, max);
        registry.computeIfAbsent(panelOwner, k -> new ArrayList<>()).add(vf);
        // Listener que revalida en cada cambio
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { vf.validar(); }
            public void removeUpdate(DocumentEvent e)  { vf.validar(); }
            public void changedUpdate(DocumentEvent e) { vf.validar(); }
        });
        vf.validar();
    }

    /**
     * Valida todos los campos registrados en un panel.
     * @return true si todos son válidos, false si hay al menos uno con error
     */
    public static boolean validarTodos(JComponent panelOwner) {
        List<ValidatedField> lista = registry.get(panelOwner);
        if (lista == null) return true;
        boolean ok = true;
        for (ValidatedField vf : lista) {
            if (!vf.validar()) ok = false;
        }
        return ok;
    }

    /** Cuenta cuántos campos tienen error en un panel. */
    public static int contarErrores(JComponent panelOwner) {
        List<ValidatedField> lista = registry.get(panelOwner);
        if (lista == null) return 0;
        int n = 0;
        for (ValidatedField vf : lista) {
            if (!vf.validar()) n++;
        }
        return n;
    }

    // ── Implementación interna ───────────────────────────────────────────────

    private static class ValidatedField {
        final JTextField field;
        final boolean    allowNegative;
        final double     min;
        final double     max;
        final Color      originalBg;
        final Color      originalFg;
        final Border     originalBorder;
        final String     originalTooltip;

        ValidatedField(JTextField f, boolean allowNeg, double min, double max) {
            this.field           = f;
            this.allowNegative   = allowNeg;
            this.min             = min;
            this.max             = max;
            this.originalBg      = f.getBackground();
            this.originalFg      = f.getForeground();
            this.originalBorder  = f.getBorder();
            this.originalTooltip = f.getToolTipText();
        }

        boolean validar() {
            String texto = field.getText().trim()
                .replace("$","").replace(" ","");
            if (texto.isEmpty()) { marcarOk(); return true; }
            // Normalización inteligente:
            //   - Si tiene punto Y coma → formato chileno (punto=miles, coma=decimal)
            //   - Si tiene solo coma → coma es decimal
            //   - Si tiene solo punto → puede ser miles o decimal:
            //       · Si hay UN punto y los dígitos a la derecha son ≤ 2 → decimal (ej. "0.5", "12.34")
            //       · En cualquier otro caso → separador de miles (ej. "1.234", "1.234.567")
            if (texto.contains(".") && texto.contains(",")) {
                // Chileno: punto=miles, coma=decimal
                texto = texto.replace(".", "").replace(",", ".");
            } else if (texto.contains(",")) {
                // Solo coma → decimal
                texto = texto.replace(",", ".");
            } else if (texto.contains(".")) {
                // Solo punto: decidir según contexto
                int idxPunto = texto.lastIndexOf('.');
                int decimales = texto.length() - idxPunto - 1;
                long puntos = texto.chars().filter(c -> c == '.').count();
                if (puntos == 1 && decimales <= 2) {
                    // Probablemente decimal (ej. "0.5", "12.34") — se deja como está
                } else {
                    // Probablemente miles (ej. "1.234", "1.234.567")
                    texto = texto.replace(".", "");
                }
            }
            try {
                double v = Double.parseDouble(texto);
                if (!allowNegative && v < 0) {
                    marcarError("Este campo no admite valores negativos.");
                    return false;
                }
                if (v < min) {
                    marcarError("El valor mínimo permitido es " + min + ".");
                    return false;
                }
                if (v > max) {
                    marcarError("El valor máximo permitido es " + max + ".");
                    return false;
                }
                marcarOk();
                return true;
            } catch (NumberFormatException ex) {
                marcarError("El valor ingresado no es un número válido.");
                return false;
            }
        }

        void marcarError(String msg) {
            field.setBackground(BG_ERROR);
            field.setForeground(FG_ERROR);
            field.setBorder(BORDER_ERROR);
            field.setToolTipText("⚠ " + msg);
        }

        void marcarOk() {
            field.setBackground(originalBg);
            field.setForeground(originalFg);
            field.setBorder(originalBorder);
            field.setToolTipText(originalTooltip);
        }
    }
}

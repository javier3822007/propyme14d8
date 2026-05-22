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

import cl.propyme.db.DataStore;
import cl.propyme.model.GlobalConfig;
import cl.propyme.model.*;
import cl.propyme.ui.panels.*;
import javax.swing.*;
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
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import cl.propyme.model.DatosMes;
import cl.propyme.model.Ejercicio;
import cl.propyme.model.Empresa;
import cl.propyme.model.Resultados;
import cl.propyme.service.CalculadorImpuesto;
import javax.swing.JButton;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import cl.propyme.ui.panels.ConfigPanel;
import cl.propyme.ui.panels.AboutPanel;

public class MainFrame extends JFrame {

    private final DataStore store = new DataStore();
    private Empresa empresaActual;
    private Ejercicio ejercicioActual;
    private Map<Integer, DatosMes> datosActuales = new TreeMap<>();

    private GlobalConfig globalConfig = new GlobalConfig();

    // Cached results (computed once, shared across panels)
    private cl.propyme.model.Resultados cachedResultados = null;

    // Unsaved changes tracking
    private boolean dirty = false;
    // True si la última carga del ejercicio falló — bloquea guardarTodo() para
    // evitar sobreescribir el archivo original con datos en blanco.
    private boolean cargaFallida = false;

    public boolean isDirty() { return dirty; }
    public void markDirty() { this.dirty = true; }
    public void clearDirty() { this.dirty = false; }

    // Layout
    private final JPanel contentPanel = new JPanel(new CardLayout());
    private final SidebarPanel sidebar;

    // Panels (lazy init)
    private HomePanel homePanel;
    private DatosMensualesPanel datosMensualesPanel;
    private AntecedentesPanel antecedentesPanel;
    private ImportarRCVPanel importarPanel;
    private Recuadro22Panel r22Panel;
    private Recuadro23Panel r23Panel;
    private BalancePanel balancePanel;
    private CertificacionPanel certPanel;
    private ConfigPanel configPanel;
    private AboutPanel aboutPanel;

    public MainFrame() {
        super("Sistema ProPyme Transparente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { confirmExit(); }
        });
        setMinimumSize(new Dimension(1100, 720));
        setPreferredSize(new Dimension(1280, 800));

        // Main layout: sidebar | content
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        sidebar = new SidebarPanel(this);
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Inicializar panel home
        homePanel = new HomePanel(this, store);
        contentPanel.add(homePanel, "home");

        configPanel = new ConfigPanel(this);
        contentPanel.add(configPanel, "config");

        aboutPanel = new AboutPanel(this);
        contentPanel.add(aboutPanel, "about");

        showPanel("home");
        pack();
        setLocationRelativeTo(null);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void showPanel(String name) {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, name);
        sidebar.setActiveItem(name);
    }

    public void loadEmpresa(Empresa emp, int anio) {
        this.empresaActual = emp;
        // Por defecto la carga es exitosa. Si entra al catch se activa cargaFallida
        // para bloquear futuros guardados que podrían sobreescribir datos legítimos.
        this.cargaFallida = false;
        try {
            Object[] loaded = store.cargarEjercicio(emp.getRut(), anio);
            if (loaded != null) {
                this.ejercicioActual = (Ejercicio) loaded[0];
                this.datosActuales   = (Map<Integer, DatosMes>) loaded[1];
            } else {
                this.ejercicioActual = new Ejercicio(anio);
                this.datosActuales   = new TreeMap<>();
            }
        } catch (Exception e) {
            this.ejercicioActual = new Ejercicio(anio);
            this.datosActuales   = new TreeMap<>();
            this.cargaFallida = true;
            JOptionPane.showMessageDialog(this,
                "Error al cargar el ejercicio:\n" + e.getMessage() +
                "\n\n⚠ MODO LECTURA: El guardado quedó BLOQUEADO para evitar\n" +
                "sobreescribir el archivo original con datos vacíos.\n\n" +
                "Causas posibles:\n" +
                "  • Archivo bloqueado por antivirus o servicio de sincronización\n" +
                "    (OneDrive, Dropbox, etc.)\n" +
                "  • Archivo JSON corrupto\n" +
                "  • Problema de permisos del sistema de archivos\n\n" +
                "Recomendación:\n" +
                "  1. Cerrar el programa.\n" +
                "  2. Verificar que el archivo no esté en uso por otro proceso.\n" +
                "  3. Si está corrupto, restaurar desde un respaldo en\n" +
                "     Configuración → Sistema de Respaldo.",
                "Error de carga — guardado bloqueado", JOptionPane.ERROR_MESSAGE);
        }

        // Si la carga del ejercicio produjo errores no fatales en meses, avisar al usuario
        java.util.List<String> errMeses = cl.propyme.db.DataStore.getUltimosErroresCarga();
        if (!errMeses.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Se encontraron ").append(errMeses.size())
              .append(" mes(es) con errores al cargar el ejercicio.\n");
            sb.append("Los meses afectados no se cargaron y deberán reingresarse.\n\n");
            sb.append("Revise los datos antes de continuar.\n");
            JOptionPane.showMessageDialog(this, sb.toString(),
                "Datos parcialmente cargados", JOptionPane.WARNING_MESSAGE);
        }

        // Cargar configuración global para este año específico
        globalConfig = store.cargarConfig(anio);
        cachedResultados = null; // invalidate cache for new year
        // Inicializar todos los paneles
        initPanels();
        String tituloEmp = cargaFallida
            ? "⚠ " + emp.getRazonSocial() + "  [MODO LECTURA — guardado bloqueado]"
            : emp.getRazonSocial();
        sidebar.setEmpresaActiva(tituloEmp, anio);
        clearDirty(); // discard any dirty flags raised during initial load
        showPanel("datos");
    }

    private void initPanels() {
        // Remove old panels
        contentPanel.removeAll();

        // Re-add home
        homePanel = new HomePanel(this, store);
        contentPanel.add(homePanel, "home");

        datosMensualesPanel = new DatosMensualesPanel(this);
        contentPanel.add(datosMensualesPanel, "datos");

        antecedentesPanel = new AntecedentesPanel(this);
        contentPanel.add(antecedentesPanel, "antecedentes");

        importarPanel = new ImportarRCVPanel(this);
        contentPanel.add(importarPanel, "importar");

        r22Panel = new Recuadro22Panel(this);
        contentPanel.add(r22Panel, "r22");

        r23Panel = new Recuadro23Panel(this);
        contentPanel.add(r23Panel, "r23");

        balancePanel = new BalancePanel(this);
        contentPanel.add(balancePanel, "balance");

        certPanel = new CertificacionPanel(this);
        contentPanel.add(certPanel, "cert");

        configPanel = new ConfigPanel(this);
        contentPanel.add(configPanel, "config");

        aboutPanel = new AboutPanel(this);
        contentPanel.add(aboutPanel, "about");

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public void guardarTodo() {
        if (empresaActual == null || ejercicioActual == null) return;
        // BLOQUEO DE SEGURIDAD: si la última carga falló, no guardar para evitar
        // sobreescribir un archivo legítimo con datos vacíos en memoria.
        if (cargaFallida) {
            JOptionPane.showMessageDialog(this,
                "⚠ Guardado bloqueado.\n\n" +
                "La última carga del ejercicio falló, por lo que los datos\n" +
                "en memoria pueden no reflejar el contenido real del archivo.\n\n" +
                "Guardar ahora sobreescribiría el archivo original con datos\n" +
                "incompletos o vacíos, causando PÉRDIDA DE DATOS.\n\n" +
                "Cierre el programa y verifique el archivo o restaure un respaldo.",
                "Guardado bloqueado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Desactivar feedback del botón guardar
        javax.swing.JButton saveIndicator = new javax.swing.JButton();
        new javax.swing.SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                store.guardarEmpresa(empresaActual);
                store.guardarEjercicio(empresaActual.getRut(), ejercicioActual, datosActuales);
                // Backup automático silencioso — falla silenciosamente si hay error
                String baseDir = System.getProperty("app.home", System.getProperty("user.dir"));
                cl.propyme.service.BackupService.crearAuto(baseDir);
                return null;
            }
            protected void done() {
                try {
                    get();
                    clearDirty();
                    invalidateCache();
                    recalculate();
                    // Mostrar estado breve en barra de título en lugar de diálogo bloqueante
                    String origTitle = getTitle();
                    setTitle("✓ Guardado — " + origTitle);
                    javax.swing.Timer t = new javax.swing.Timer(2000, e -> setTitle(origTitle));
                    t.setRepeats(false); t.start();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "Error al guardar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void recalculate() {
        if (ejercicioActual == null) return;
        // Run calculation in background, update UI on EDT
        new javax.swing.SwingWorker<cl.propyme.model.Resultados, Void>() {
            protected cl.propyme.model.Resultados doInBackground() {
                return cl.propyme.service.CalculadorImpuesto.calcular(ejercicioActual, datosActuales, globalConfig);
            }
            protected void done() {
                try {
                    cachedResultados = get();
                    if (r22Panel != null) r22Panel.refresh();
                    if (r23Panel != null) r23Panel.refresh();
                    if (certPanel != null) certPanel.refresh();
                    // El balance es manual — no requiere refresco
                } catch (Exception e) {
                    System.err.println("Recalc error: " + e.getMessage());
                }
            }
        }.execute();
    }

    public cl.propyme.model.Resultados getCachedResultados() {
        if (cachedResultados == null && ejercicioActual != null) {
            cachedResultados = cl.propyme.service.CalculadorImpuesto.calcular(ejercicioActual, datosActuales, globalConfig);
        }
        return cachedResultados;
    }

    public void invalidateCache() { cachedResultados = null; }

    private void confirmExit() {
        if (!dirty) { dispose(); System.exit(0); return; }
        int opt = JOptionPane.showConfirmDialog(this,
            "Hay cambios sin guardar.\nDesea guardar antes de salir?",
            "Cambios sin guardar",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.CANCEL_OPTION || opt == JOptionPane.CLOSED_OPTION) return;
        if (opt == JOptionPane.YES_OPTION) {
            // Bloqueo: no guardar si la carga falló (evita sobreescribir el archivo legítimo)
            if (cargaFallida) {
                int resp = JOptionPane.showConfirmDialog(this,
                    "⚠ La última carga del ejercicio falló.\n\n" +
                    "Si guarda ahora, podría sobreescribir el archivo original\n" +
                    "con datos vacíos. ¿Salir sin guardar?",
                    "Guardado bloqueado",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (resp != JOptionPane.YES_OPTION) return;
                System.exit(0);
                return;
            }
            // Guardado síncrono (no vía SwingWorker) para que la app no cierre antes de completar la escritura
            try {
                if (empresaActual != null && ejercicioActual != null) {
                    store.guardarEmpresa(empresaActual);
                    store.guardarEjercicio(empresaActual.getRut(), ejercicioActual, datosActuales);
                }
                clearDirty();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error al guardar: " + e.getMessage() + "\nSalir igualmente?",
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
                int again = JOptionPane.showConfirmDialog(this,
                    "Salir sin guardar?", "Confirmar",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (again != JOptionPane.YES_OPTION) return;
            }
        }
        dispose();
        System.exit(0);
    }

    /** Limpia el estado de empresa activa y vuelve al panel home. Se llama al eliminar la empresa abierta. */
    public void clearEmpresa() {
        empresaActual   = null;
        ejercicioActual = null;
        datosActuales   = new java.util.TreeMap<>();
        cachedResultados = null;
        sidebar.setEmpresaActiva("", 0);
        showPanel("home");
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public GlobalConfig getGlobalConfig() { return globalConfig; }
    public void setGlobalConfig(GlobalConfig c) { this.globalConfig = c; invalidateCache(); }
    public DataStore getStore()               { return store; }
    public Empresa getEmpresaActual()         { return empresaActual; }
    public Ejercicio getEjercicioActual()     { return ejercicioActual; }
    public Map<Integer,DatosMes> getDatos()   { return datosActuales; }
    public void setDatos(Map<Integer,DatosMes> d) { this.datosActuales = d; }
    public DatosMensualesPanel getDatosMensualesPanel() { return datosMensualesPanel; }
}

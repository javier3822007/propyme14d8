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

import cl.propyme.db.DataStore;
import cl.propyme.model.Empresa;
import cl.propyme.model.Ejercicio;
import cl.propyme.model.Socio;
import cl.propyme.ui.Theme;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import cl.propyme.ui.MainFrame;
import javax.swing.SwingWorker;
import cl.propyme.model.DatosMes;
import cl.propyme.model.GlobalConfig;
import cl.propyme.model.Resultados;
import cl.propyme.service.CalculadorImpuesto;
import java.util.Map;

public class HomePanel extends JPanel {

    private final MainFrame frame;
    private final DataStore store;
    private JTable empresaTable;
    private DefaultTableModel tableModel;

    public HomePanel(MainFrame frame, DataStore store) {
        this.frame = frame; this.store = store;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.DARK_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(24,32,24,32));
        JLabel title = new JLabel("Sistema ProPyme Transparente");
        title.setFont(new Font("SansSerif",Font.BOLD,22)); title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Régimen Art. 14 D N°8 LIR — Gestión de Declaración de Renta");
        subtitle.setFont(Theme.FONT_LABEL); subtitle.setForeground(Theme.LIGHT_BLUE);
        JPanel titles = new JPanel(new GridLayout(2,1,0,4)); titles.setOpaque(false);
        titles.add(title); titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);
        JLabel pathLabel = new JLabel("Datos: " + store.getBaseDir());
        pathLabel.setFont(Theme.FONT_SMALL); pathLabel.setForeground(Theme.LIGHT_BLUE);
        header.add(pathLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(16,0));
        center.setBackground(Theme.BG);
        center.setBorder(BorderFactory.createEmptyBorder(24,32,24,32));

        // ── Left: empresa list ────────────────────────────────────────────────
        JPanel listPanel = Theme.card("Empresas registradas");
        String[] cols = {"Razón Social","RUT","Giro","Ejercicios"};
        tableModel = new DefaultTableModel(cols,0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        empresaTable = new JTable(tableModel);
        Theme.styleTable(empresaTable);
        empresaTable.setTableHeader(new javax.swing.table.JTableHeader(empresaTable.getColumnModel()) {
            @Override public void paintComponent(java.awt.Graphics g) {
                g.setColor(Theme.DARK_BLUE);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        });
        empresaTable.getTableHeader().setDefaultRenderer(Theme.makeHeaderRenderer());
        empresaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        empresaTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        empresaTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        empresaTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        empresaTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        empresaTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) openSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(empresaTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LIGHT_BLUE));
        listPanel.add(scroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));
        btnRow.setBackground(Color.WHITE);
        JButton openBtn   = Theme.primaryButton("✓  Abrir");
        JButton editBtn   = Theme.secondaryButton("✏  Editar");
        JButton newYearBtn= Theme.secondaryButton("＋  Nuevo Año");
        JButton deleteBtn = Theme.dangerButton("✕  Eliminar");
        btnRow.add(openBtn); btnRow.add(editBtn);
        btnRow.add(newYearBtn); btnRow.add(deleteBtn);
        openBtn.addActionListener(e -> openSelected());
        editBtn.addActionListener(e -> editSelected());
        newYearBtn.addActionListener(e -> addNewYear());
        deleteBtn.addActionListener(e -> deleteSelected());
        listPanel.add(btnRow, BorderLayout.SOUTH);
        center.add(listPanel, BorderLayout.CENTER);

        // ── Right: new empresa form ───────────────────────────────────────────
        JPanel newPanel = Theme.card("Nueva Empresa");
        newPanel.setPreferredSize(new Dimension(340,0));
        newPanel.setLayout(new BorderLayout());

        JTextField fRut=Theme.inputField(),fNom=Theme.inputField(),fGiro=Theme.inputField(),
            fDom=Theme.inputField(),fAnio=Theme.inputField(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)-1));
        JTextField fS1n=Theme.inputField(),fS1r=Theme.inputField(),fS1p=Theme.inputField("100"),fS1c=Theme.inputField("0");
        JTextField fS2n=Theme.inputField(),fS2r=Theme.inputField(),fS2p=Theme.inputField("0"),fS2c=Theme.inputField("0");

        JPanel form = buildForm(fRut,fNom,fGiro,fDom,fAnio,fS1n,fS1r,fS1p,fS1c,fS2n,fS2r,fS2p,fS2c);
        JScrollPane fs = new JScrollPane(form); fs.setBorder(null);

        // Nota llamativa de advertencia sobre el RUT — debe ir arriba del formulario
        JPanel avisoCrear = new JPanel(new BorderLayout());
        avisoCrear.setBackground(new java.awt.Color(0xFF, 0xF3, 0xCD));
        avisoCrear.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(0xFF, 0xA5, 0x00)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel lblAvisoCrear = new JLabel(
            "<html><b>⚠  IMPORTANTE:</b> El RUT no podrá modificarse una vez creada la empresa.<br>" +
            "Verifique cuidadosamente antes de crear.</html>");
        lblAvisoCrear.setFont(Theme.FONT_LABEL);
        lblAvisoCrear.setForeground(new java.awt.Color(0x85, 0x6D, 0x00));
        avisoCrear.add(lblAvisoCrear, BorderLayout.CENTER);

        newPanel.add(avisoCrear, BorderLayout.NORTH);
        newPanel.add(fs, BorderLayout.CENTER);

        JButton createBtn = Theme.primaryButton("➕  Crear Empresa");
        createBtn.addActionListener(e -> createEmpresa(fRut,fNom,fGiro,fDom,fAnio,fS1n,fS1r,fS1p,fS1c,fS2n,fS2r,fS2p,fS2c));
        newPanel.add(createBtn, BorderLayout.SOUTH);
        center.add(newPanel, BorderLayout.EAST);

        add(center, BorderLayout.CENTER);
        loadEmpresas();
    }

    private JPanel buildForm(JTextField fRut,JTextField fNom,JTextField fGiro,
            JTextField fDom,JTextField fAnio,
            JTextField fS1n,JTextField fS1r,JTextField fS1p,JTextField fS1c,
            JTextField fS2n,JTextField fS2r,JTextField fS2p,JTextField fS2c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets=new Insets(4,4,4,4); gc.anchor=GridBagConstraints.WEST; gc.fill=GridBagConstraints.HORIZONTAL;
        Object[][] rows = {
            {"RUT empresa:",fRut},{"Razón Social:",fNom},{"Giro:",fGiro},
            {"Domicilio:",fDom},{"Año Comercial:",fAnio},
            {"── Socio 1 ──",null},
            {"Nombre:",fS1n},{"RUT:",fS1r},{"% Participación:",fS1p},{"Capital ($):",fS1c},
            {"── Socio 2 (opcional) ──",null},
            {"Nombre:",fS2n},{"RUT:",fS2r},{"% Participación:",fS2p},{"Capital ($):",fS2c},
        };
        int row=0;
        for (Object[] r2 : rows) {
            String lbl=(String)r2[0]; JTextField tf=(JTextField)r2[1];
            gc.gridx=0; gc.gridy=row; gc.weightx=0;
            if (lbl.startsWith("──")) {
                JLabel sep=new JLabel(lbl); sep.setFont(new Font("SansSerif",Font.BOLD,11)); sep.setForeground(Theme.MID_BLUE);
                gc.gridwidth=2; gc.weightx=1; p.add(sep,gc); gc.gridwidth=1;
            } else if (tf != null) {
                p.add(Theme.label(lbl),gc);
                gc.gridx=1; gc.weightx=1; p.add(tf,gc);
            }
            // si tf == null, saltar esta fila (ej. Año removido del diálogo de edición)
            row++;
        }
        return p;
    }

    private void createEmpresa(JTextField fRut,JTextField fNom,JTextField fGiro,JTextField fDom,JTextField fAnio,
            JTextField fS1n,JTextField fS1r,JTextField fS1p,JTextField fS1c,
            JTextField fS2n,JTextField fS2r,JTextField fS2p,JTextField fS2c) {
        try {
            if (fRut.getText().trim().isEmpty() || fNom.getText().trim().isEmpty())
                throw new Exception("RUT y Razón Social son obligatorios.");

            // Validar formato del RUT chileno (algoritmo Módulo 11). Si es inválido,
            // ofrecer al usuario continuar de todas formas (algunos casos especiales
            // pueden no cumplir el algoritmo estándar).
            String rutIngresado = fRut.getText().trim();
            if (!cl.propyme.util.RutValidator.esValido(rutIngresado)) {
                int resp = JOptionPane.showConfirmDialog(this,
                    "⚠ El RUT ingresado podría no ser válido (el dígito verificador\n" +
                    "no coincide con el algoritmo estándar chileno).\n\n" +
                    "RUT ingresado: " + rutIngresado + "\n\n" +
                    "Desea continuar de todas formas?",
                    "Validación de RUT",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (resp != JOptionPane.YES_OPTION) return;
            }

            // Validar el año PRIMERO para no persistir empresa con año inválido
            String anioTxt = fAnio.getText().trim();
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            int anioVal;
            try {
                anioVal = Integer.parseInt(anioTxt);
                if (anioVal < 2018 || anioVal > currentYear) {
                    throw new Exception("El año de inicio debe estar entre 2018 y " + currentYear + ".");
                }
            } catch (NumberFormatException nfe) {
                throw new Exception("Año de inicio inválido. Ingrese un año entre 2018 y " + currentYear + ".");
            }

            Empresa emp = new Empresa(fRut.getText().trim(),fNom.getText().trim(),
                fGiro.getText().trim(),fDom.getText().trim());
            List<Socio> socios = new ArrayList<>();
            if (!fS1n.getText().trim().isEmpty()) {
                double p=Double.parseDouble(fS1p.getText().trim().replace(",",".").replace("%",""))/100;
                socios.add(new Socio(fS1r.getText().trim(),fS1n.getText().trim(),p,parseD(fS1c),1));
            }
            if (!fS2n.getText().trim().isEmpty()) {
                double p=Double.parseDouble(fS2p.getText().trim().replace(",",".").replace("%",""))/100;
                socios.add(new Socio(fS2r.getText().trim(),fS2n.getText().trim(),p,parseD(fS2c),2));
            }
            emp.setSocios(socios);
            store.guardarEmpresa(emp);
            final Empresa empFinal = emp;
            final int anio = anioVal;
            new SwingWorker<Void,Void>(){
                protected Void doInBackground() throws Exception {
                    store.guardarEjercicio(empFinal.getRut(), new Ejercicio(anio), new java.util.TreeMap<>());
                    return null;
                }
                protected void done() { loadEmpresas(); frame.loadEmpresa(empFinal,anio); }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelected() {
        int row = empresaTable.getSelectedRow();
        if (row<0){JOptionPane.showMessageDialog(frame,"Seleccione una empresa."); return;}
        String rut=(String)tableModel.getValueAt(row,1);
        try {
            Empresa emp = store.cargarEmpresa(rut);
            JDialog dlg = new JDialog(frame,"Editar Empresa: "+emp.getRazonSocial(),true);
            dlg.setLayout(new BorderLayout());
            dlg.setSize(420,580); dlg.setLocationRelativeTo(frame);

            // Nota llamativa de advertencia sobre el RUT
            JPanel avisoPanel = new JPanel(new BorderLayout());
            avisoPanel.setBackground(new java.awt.Color(0xFF, 0xF3, 0xCD));
            avisoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(0xFF, 0xA5, 0x00)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            JLabel aviso = new JLabel(
                "<html><b>⚠  IMPORTANTE:</b> El RUT no puede modificarse una vez creada la empresa.<br>" +
                "Si necesita cambiar el RUT, elimine esta empresa y cree una nueva.</html>");
            aviso.setFont(Theme.FONT_LABEL);
            aviso.setForeground(new java.awt.Color(0x85, 0x6D, 0x00));
            avisoPanel.add(aviso, BorderLayout.CENTER);
            dlg.add(avisoPanel, BorderLayout.NORTH);

            JTextField fRut=Theme.inputField(emp.getRut());
            fRut.setEditable(false);
            fRut.setBackground(new java.awt.Color(0xF0, 0xF0, 0xF0));
            fRut.setToolTipText("El RUT no puede modificarse una vez creada la empresa.");
            JTextField fNom=Theme.inputField(emp.getRazonSocial());
            JTextField fGiro=Theme.inputField(emp.getGiro());
            JTextField fDom=Theme.inputField(emp.getDomicilio());

            Socio s1=emp.getSocios().size()>0?emp.getSocios().get(0):new Socio();
            Socio s2=emp.getSocios().size()>1?emp.getSocios().get(1):new Socio();
            JTextField fS1n=Theme.inputField(s1.getNombre()!=null?s1.getNombre():"");
            JTextField fS1r=Theme.inputField(s1.getRut()!=null?s1.getRut():"");
            JTextField fS1p=Theme.inputField(String.format("%.1f",s1.getPorcentaje()*100));
            JTextField fS1c=Theme.inputField(String.valueOf(Math.round(s1.getCapitalAportado())));
            JTextField fS2n=Theme.inputField(s2.getNombre()!=null?s2.getNombre():"");
            JTextField fS2r=Theme.inputField(s2.getRut()!=null?s2.getRut():"");
            JTextField fS2p=Theme.inputField(String.format("%.1f",s2.getPorcentaje()*100));
            JTextField fS2c=Theme.inputField(String.valueOf(Math.round(s2.getCapitalAportado())));

            JPanel form = buildForm(fRut,fNom,fGiro,fDom,null,fS1n,fS1r,fS1p,fS1c,fS2n,fS2r,fS2p,fS2c);
            JScrollPane fs=new JScrollPane(form); fs.setBorder(null);
            dlg.add(fs,BorderLayout.CENTER);

            JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
            JButton cancelBtn=Theme.secondaryButton("Cancelar");
            JButton saveBtn=Theme.primaryButton("💾  Guardar cambios");
            cancelBtn.addActionListener(e->dlg.dispose());
            saveBtn.addActionListener(e->{
                try {
                    // NOTA: NO se actualiza el RUT — es inmutable después de crear la empresa
                    emp.setRazonSocial(fNom.getText().trim());
                    emp.setGiro(fGiro.getText().trim());
                    emp.setDomicilio(fDom.getText().trim());
                    List<Socio> socios=new ArrayList<>();
                    if(!fS1n.getText().trim().isEmpty()){
                        double p=Double.parseDouble(fS1p.getText().trim().replace(",",".").replace("%",""))/100;
                        socios.add(new Socio(fS1r.getText().trim(),fS1n.getText().trim(),p,parseD(fS1c),1));
                    }
                    if(!fS2n.getText().trim().isEmpty()){
                        double p=Double.parseDouble(fS2p.getText().trim().replace(",",".").replace("%",""))/100;
                        socios.add(new Socio(fS2r.getText().trim(),fS2n.getText().trim(),p,parseD(fS2c),2));
                    }
                    emp.setSocios(socios);
                    store.guardarEmpresa(emp);
                    loadEmpresas();
                    dlg.dispose();
                    JOptionPane.showMessageDialog(frame,"✓ Empresa actualizada.");
                } catch(Exception ex){
                    JOptionPane.showMessageDialog(dlg,"Error: "+ex.getMessage());
                }
            });
            btns.add(cancelBtn); btns.add(saveBtn);
            dlg.add(btns,BorderLayout.SOUTH);
            dlg.setVisible(true);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(frame,"Error: "+ex.getMessage());
        }
    }

    private void addNewYear() {
        int row = empresaTable.getSelectedRow();
        if (row<0){JOptionPane.showMessageDialog(frame,"Seleccione una empresa."); return;}
        String rut=(String)tableModel.getValueAt(row,1);
        try {
            Empresa emp = store.cargarEmpresa(rut);
            List<Integer> existing = store.listarEjercicios(rut);
            int sugerido = existing.isEmpty()
                ? Calendar.getInstance().get(Calendar.YEAR)-1
                : existing.get(0)+1;
            String input=JOptionPane.showInputDialog(frame,
                "Año Comercial del nuevo ejercicio\n(Los ejercicios existentes se conservan):",
                String.valueOf(sugerido));
            if (input==null||input.trim().isEmpty()) return;
            int anio=Integer.parseInt(input.trim());
            if (existing.contains(anio)){
                int opt=JOptionPane.showConfirmDialog(frame,
                    "El ejercicio "+anio+" ya existe. Desea abrirlo?","Ejercicio existente",JOptionPane.YES_NO_OPTION);
                if(opt==JOptionPane.YES_OPTION) frame.loadEmpresa(emp,anio);
                return;
            }
            // Optionally copy antecedentes from previous year
            Ejercicio ejNuevo = new Ejercicio(anio);
            if (!existing.isEmpty()) {
                int prev=existing.get(0);
                int opt=JOptionPane.showConfirmDialog(frame,
                    "Copiar antecedentes del año "+prev+" como punto de partida?\n"+
                    "(CPTS, factores IPC, etc. Datos mensuales quedarán en blanco).",
                    "Copiar antecedentes",JOptionPane.YES_NO_OPTION);
                if (opt==JOptionPane.YES_OPTION) {
                    Object[] prev_data = store.cargarEjercicio(rut,prev);
                    if (prev_data!=null) {
                        Ejercicio prevEj=(Ejercicio)prev_data[0];
                        @SuppressWarnings("unchecked")
                        java.util.Map<Integer,cl.propyme.model.DatosMes> prevDatos =
                            (java.util.Map<Integer,cl.propyme.model.DatosMes>)prev_data[1];
                        // Calcular resultados del año anterior para obtener CPTS final
                        cl.propyme.model.GlobalConfig prevCfg = store.cargarConfig(prev);
                        cl.propyme.model.Resultados prevRes =
                            cl.propyme.service.CalculadorImpuesto.calcular(prevEj, prevDatos, prevCfg);
                        // CPTS final del año anterior → CPTS inicial del año nuevo
                        ejNuevo.setCptsPositivoInicial(prevRes.cptsPositivo1581);
                        ejNuevo.setCptsNegativoInicial(prevRes.cptsNegativo1583);
                        ejNuevo.setPerdidaAnterior(prevRes.cpts1713); // pérdida ejercicio anterior
                        ejNuevo.setUfDiciembre(prevEj.getUfDiciembre());
                        ejNuevo.setUtmDiciembre(prevEj.getUtmDiciembre());
                        ejNuevo.setActivoFijoFactor33bis(prevEj.getActivoFijoFactor33bis());
                    }
                }
            }
            final Empresa empRef = emp;
            final int anioRef = anio;
            final Ejercicio ejRef = ejNuevo;
            final String rutRef = rut;
            new SwingWorker<Void,Void>(){
                protected Void doInBackground() throws Exception {
                    store.guardarEjercicio(rutRef, ejRef, new java.util.TreeMap<>());
                    return null;
                }
                protected void done() { loadEmpresas(); frame.loadEmpresa(empRef, anioRef); }
            }.execute();
        } catch(Exception ex){
            JOptionPane.showMessageDialog(frame,"Error: "+ex.getMessage());
        }
    }

    private void openSelected() {
        int row=empresaTable.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(frame,"Seleccione una empresa."); return;}
        String rut=(String)tableModel.getValueAt(row,1);
        try {
            Empresa emp=store.cargarEmpresa(rut);
            List<Integer> ejs=store.listarEjercicios(rut);
            int anio;
            if(ejs.isEmpty()){
                String input=JOptionPane.showInputDialog(frame,"Año Comercial:",
                    String.valueOf(Calendar.getInstance().get(Calendar.YEAR)-1));
                if(input==null) return;
                anio=Integer.parseInt(input.trim());
            } else if(ejs.size()==1){
                anio=ejs.get(0);
            } else {
                Object sel=JOptionPane.showInputDialog(frame,
                    "Seleccione el año a abrir:","Seleccionar ejercicio",
                    JOptionPane.QUESTION_MESSAGE,null,ejs.toArray(),ejs.get(0));
                if(sel==null) return;
                anio=(int)sel;
            }
            frame.loadEmpresa(emp,anio);
        } catch(Exception ex){
            JOptionPane.showMessageDialog(frame,"Error: "+ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row=empresaTable.getSelectedRow();
        if(row<0) return;
        String nombre=(String)tableModel.getValueAt(row,0);
        String rut=(String)tableModel.getValueAt(row,1);
        int res=JOptionPane.showConfirmDialog(frame,
            "Eliminar empresa \""+nombre+"\" y TODOS sus ejercicios y datos?\nEsta acción no se puede deshacer.",
            "Confirmar eliminación",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(res!=JOptionPane.YES_OPTION) return;
        try {
            store.eliminarEmpresa(rut);
            // Si se eliminó la empresa actualmente abierta, limpiar estado y volver a home
            if (frame.getEmpresaActual() != null && rut.equals(frame.getEmpresaActual().getRut())) {
                frame.clearEmpresa();
            }
            loadEmpresas();
        }
        catch(Exception ex){JOptionPane.showMessageDialog(frame,"Error: "+ex.getMessage());}
    }

    private void loadEmpresas() {
        tableModel.setRowCount(0);
        for(Empresa e:store.listarEmpresas()){
            List<Integer> ejs=store.listarEjercicios(e.getRut());
            String ejStr=ejs.isEmpty()?"—":ejs.stream().map(String::valueOf).reduce((a,b)->a+", "+b).orElse("—");
            tableModel.addRow(new Object[]{e.getRazonSocial(),e.getRut(),e.getGiro(),ejStr});
        }
    }

    private double parseD(JTextField f){
        String s=f.getText().trim().replace(".","").replace(",",".");
        try{return Double.parseDouble(s);}catch(Exception e){return 0;}
    }
}

package cl.propyme.ui.panels;

import cl.propyme.ui.MainFrame;
import cl.propyme.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.net.URI;

/**
 * AboutPanel — Pantalla "Acerca de".
 * Muestra información del programa, autor, versión y licencia AGPLv3.
 */
public class AboutPanel extends JPanel {

    public static final String VERSION = "0.10.0";
    public static final String AUTOR   = "Javier Ignacio Aguilar G.";
    public static final String EMAIL   = "javier.aguilar382@protonmail.com";

    private final MainFrame frame;

    public AboutPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.DARK_BLUE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JLabel t = new JLabel("Acerca de — ProPyme Transparente");
        t.setFont(Theme.FONT_HEADER);
        t.setForeground(Color.WHITE);
        hdr.add(t, BorderLayout.WEST);
        add(hdr, BorderLayout.NORTH);

        // Contenido scrollable
        JPanel content = new JPanel();
        content.setBackground(Theme.BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        content.add(seccion("Programa",
            "ProPyme Transparente\n" +
            "Sistema de declaración tributaria para Pymes acogidas al\n" +
            "Régimen Pro Pyme Transparente, Art. 14 D N°8 LIR (Chile)."));

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        content.add(seccion("Versión",  "v" + VERSION));

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        content.add(seccion("Autor",
            "Por\n" +
            AUTOR + "\n\n" +
            "Contacto:\n" +
            EMAIL + "\n\n" +
            "Este proyecto se desarrolla de forma independiente y en tiempo libre."));

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        content.add(seccion("Donaciones",
            "Si este programa te fue útil, sería muy feliz si me compras una pizza 🍕\n\n" +
            "PayPal:\npaypal.me/jav382"));

        content.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnDonar = botonAccion("Abrir PayPal en el navegador");
        btnDonar.addActionListener(e -> abrirURL("https://paypal.me/jav382"));
        JPanel pBtnDonar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pBtnDonar.setOpaque(false);
        pBtnDonar.add(btnDonar);
        pBtnDonar.setAlignmentX(LEFT_ALIGNMENT);
        content.add(pBtnDonar);

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        content.add(seccion("Características del proyecto",
            "• Funcionamiento offline\n" +
            "• Datos almacenados localmente\n" +
            "• Balance tributario simplificado referencial\n" +
            "• Multiplataforma"));

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        content.add(seccion("Licencia",
            "Este programa es software libre, distribuido bajo los términos de la\n" +
            "GNU Affero General Public License versión 3 (AGPL-3.0).\n\n" +
            "El programa se comparte con la esperanza de que sea útil, pero SIN\n" +
            "NINGUNA GARANTÍA; incluso sin la garantía implícita de COMERCIABILIDAD\n" +
            "o IDONEIDAD PARA UN PROPÓSITO PARTICULAR.\n\n" +
            "Consulte el archivo LICENSE incluido con el programa para más detalles\n" +
            "o visite: https://www.gnu.org/licenses/agpl-3.0.html"));

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        // Botón para abrir la licencia
        JButton btnLicencia = botonAccion("Ver texto completo de la licencia (web)");
        btnLicencia.addActionListener(e -> abrirURL("https://www.gnu.org/licenses/agpl-3.0.html"));
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pBtn.setOpaque(false);
        pBtn.add(btnLicencia);
        pBtn.setAlignmentX(LEFT_ALIGNMENT);
        content.add(pBtn);

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        content.add(seccion("Aviso de uso",
            "Este programa fue desarrollado originalmente para mi uso personal y privado.\n" +
            "Si te resulta útil y representa un aporte para ti, eres libre de utilizarlo.\n\n" +
            "El autor no se responsabiliza por errores en cálculos tributarios ni\n" +
            "por el uso indebido de la información generada. Es responsabilidad del\n" +
            "usuario verificar la información antes de enviarla al SII o usarla de\n" +
            "alguna manera."));

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel seccion(String titulo, String texto) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LIGHT_BLUE),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Theme.FONT_BOLD);
        lblTitulo.setForeground(Theme.DARK_BLUE);
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblTitulo);
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        JTextArea ta = new JTextArea(texto);
        ta.setFont(Theme.FONT_LABEL);
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setForeground(Theme.TEXT_DARK);
        ta.setBorder(null);
        ta.setLineWrap(false);
        ta.setAlignmentX(LEFT_ALIGNMENT);
        p.add(ta);
        return p;
    }

    private static JButton botonAccion(String texto) {
        JButton b = new JButton(texto);
        b.setFont(Theme.FONT_LABEL);
        b.setBackground(Theme.DARK_BLUE);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return b;
    }

    private static void abrirURL(String url) {
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {}
    }
}

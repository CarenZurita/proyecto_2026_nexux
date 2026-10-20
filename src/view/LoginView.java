package view;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/** Presentación del login según la guía visual de Salud Vida. */
public class LoginView extends JFrame {
    private static final Color AZUL_OSCURO = new Color(0x1E293B);
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color ROJO = new Color(0xEF4444);
    private static final Color TEXTO_SECUNDARIO = new Color(0x64748B);
    private final JTextField campoUsuario = new JTextField(22);
    private final JPasswordField campoClave = new JPasswordField(22);
    private final JButton botonIngresar = new JButton("Ingresar");
    private final JCheckBox mostrarClave = new JCheckBox("Mostrar contraseña");
    private final JLabel mensaje = new JLabel(" ");

    public LoginView() {
        setTitle("Gestor de Farmacia - Salud Vida");
        setIconImage(crearIconoVentana());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 570));
        JPanel principal = new JPanel(new GridBagLayout());
        principal.setPreferredSize(new Dimension(920, 580));
        principal.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.43;
        principal.add(crearIdentidad(), c);
        c.gridx = 1;
        c.weightx = 0.57;
        principal.add(crearFormulario(), c);
        setContentPane(principal);
        getRootPane().setDefaultButton(botonIngresar);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel crearIdentidad() {
        JPanel identidad = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                p.setPaint(new GradientPaint(0, 0, new Color(0xE9F5FF), w, h, new Color(0xDCEEFF)));
                p.fillRect(0, 0, w, h);
                Path2D ola = new Path2D.Double();
                ola.moveTo(0, h * .77);
                ola.curveTo(w * .35, h * .62, w * .6, h * .98, w, h * .79);
                ola.lineTo(w, h); ola.lineTo(0, h); ola.closePath();
                p.setColor(new Color(0xCDE5FA)); p.fill(ola);
                // Ilustración de frasco y cápsulas, dibujada sin archivos externos.
                p.setColor(new Color(0xB8D9F0));
                p.fillRoundRect(44, h - 110, 47, 78, 12, 12);
                p.fillRoundRect(49, h - 120, 37, 17, 5, 5);
                p.setColor(new Color(0xF6FBFF)); p.fillRoundRect(50, h - 86, 35, 37, 4, 4);
                p.setColor(new Color(0x91C6E8)); p.fillRect(64, h - 78, 7, 21);
                p.fillRect(57, h - 71, 21, 7);
                p.rotate(-.38, 132, h - 49);
                p.setColor(Color.WHITE); p.fillRoundRect(107, h - 62, 66, 26, 26, 26);
                p.setColor(new Color(0x60A5FA)); p.fillRoundRect(140, h - 62, 33, 26, 26, 26);
                p.dispose();
            }
        };
        identidad.setMinimumSize(new Dimension(315, 0));
        identidad.setPreferredSize(new Dimension(390, 580));
        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel(new MarcaIcon(118));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nombre = new JLabel("Salud Vida");
        nombre.setFont(new Font("SansSerif", Font.BOLD, 36));
        nombre.setForeground(AZUL_OSCURO);
        nombre.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lema = new JLabel("Tu salud, nuestra prioridad");
        lema.setFont(new Font("SansSerif", Font.ITALIC, 15));
        lema.setForeground(new Color(0x1D4ED8));
        lema.setAlignmentX(Component.CENTER_ALIGNMENT);
        marca.add(logo); marca.add(Box.createVerticalStrut(18));
        marca.add(nombre); marca.add(Box.createVerticalStrut(10)); marca.add(lema);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 18, 65, 18);
        identidad.add(marca, c);
        return identidad;
    }

    private JPanel crearFormulario() {
        JPanel exterior = new JPanel(new GridBagLayout());
        exterior.setBackground(Color.WHITE);
        exterior.setBorder(BorderFactory.createEmptyBorder(32, 42, 22, 42));
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        JLabel titulo = new JLabel("Iniciar sesión");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 29));
        titulo.setForeground(AZUL_OSCURO);
        agregar(formulario, c, titulo, 0, 0, 8);
        JLabel subtitulo = new JLabel("Accede a tu cuenta de Salud Vida");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        agregar(formulario, c, subtitulo, 1, 0, 30);
        agregar(formulario, c, etiqueta("Usuario", campoUsuario), 2, 0, 8);
        agregar(formulario, c, crearCampo(campoUsuario, false), 3, 0, 22);
        agregar(formulario, c, etiqueta("Contraseña", campoClave), 4, 0, 8);
        agregar(formulario, c, crearCampo(campoClave, true), 5, 0, 10);
        char caracterOculto = campoClave.getEchoChar();
        mostrarClave.setOpaque(false);
        mostrarClave.setForeground(TEXTO_SECUNDARIO);
        mostrarClave.setFont(new Font("SansSerif", Font.PLAIN, 13));
        mostrarClave.setFocusPainted(false);
        mostrarClave.addActionListener(e -> campoClave.setEchoChar(
                mostrarClave.isSelected() ? (char) 0 : caracterOculto));
        agregar(formulario, c, mostrarClave, 6, 0, 22);
        prepararBoton(botonIngresar);
        agregar(formulario, c, botonIngresar, 7, 0, 10);
        mensaje.setForeground(ROJO);
        mensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mensaje.setPreferredSize(new Dimension(325, 45));
        agregar(formulario, c, mensaje, 8, 0, 6);
        GridBagConstraints centro = new GridBagConstraints();
        centro.weightx = 1; centro.fill = GridBagConstraints.HORIZONTAL;
        exterior.add(formulario, centro);
        return exterior;
    }

    private JLabel etiqueta(String texto, JComponent campo) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(AZUL_OSCURO); label.setLabelFor(campo);
        return label;
    }

    private JPanel crearCampo(JTextField campo, boolean clave) {
        JPanel contenedor = new JPanel(new BorderLayout(10, 0));
        contenedor.setBackground(Color.WHITE);
        contenedor.setBorder(new CampoBorde(new Color(0xCADCF0)));
        contenedor.setPreferredSize(new Dimension(330, 48));
        JLabel icono = new JLabel(new AccesoIcon(clave));
        icono.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        contenedor.add(icono, BorderLayout.WEST);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 15));
        campo.setForeground(AZUL_OSCURO);
        campo.setCaretColor(AZUL);
        campo.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 14));
        campo.setOpaque(false);
        campo.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { contenedor.setBorder(new CampoBorde(AZUL)); }
            @Override public void focusLost(FocusEvent e) { contenedor.setBorder(new CampoBorde(new Color(0xCADCF0))); }
        });
        contenedor.add(campo, BorderLayout.CENTER);
        return contenedor;
    }

    private void agregar(JPanel panel, GridBagConstraints c, JComponent componente,
                         int fila, int arriba, int abajo) {
        c.gridy = fila; c.insets = new Insets(arriba, 0, abajo, 0);
        panel.add(componente, c);
    }

    private static void prepararBoton(JButton boton) {
        boton.setBackground(AZUL); boton.setForeground(Color.WHITE);
        boton.setOpaque(true); boton.setContentAreaFilled(true);
        boton.setFocusPainted(false); boton.setBorderPainted(false);
        boton.setFont(new Font("SansSerif", Font.BOLD, 15));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(330, 46));
    }

    public String getUsuario() { return campoUsuario.getText().trim(); }
    public char[] getClave() { return campoClave.getPassword(); }
    public void alIngresar(ActionListener accion) { botonIngresar.addActionListener(accion); }
    public void limpiarClave() { campoClave.setText(""); campoClave.requestFocusInWindow(); }
    public void mostrarMensaje(String texto) { mensaje.setText("<html>" + texto + "</html>"); }

    /** Modal de autenticación indicado en el documento de referencia. */
    public void mostrarErrorAutenticacion(int restantes) {
        JDialog dialogo = new JDialog(this, "Error de autenticación", true);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.setResizable(false);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 30, 26, 30));
        JLabel aviso = new JLabel(new Icon() {
            public int getIconWidth() { return 58; }
            public int getIconHeight() { return 58; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                p.setColor(ROJO); p.fillOval(x + 2, y + 2, 54, 54);
                p.setColor(Color.WHITE); p.setFont(new Font("SansSerif", Font.BOLD, 38));
                p.drawString("!", x + 21, y + 43); p.dispose();
            }
        });
        aviso.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel titulo = new JLabel("Error de autenticación");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(AZUL_OSCURO); titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel detalle = new JLabel("<html><div style='text-align:center;width:285px'>"
                + "Los credenciales ingresadas no son válidas<br>o la cuenta está deshabilitada.<br>"
                + "Intente nuevamente.</div></html>");
        detalle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detalle.setForeground(AZUL_OSCURO); detalle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel intentos = new JLabel(restantes > 0 ? "Intentos restantes: " + restantes
                : "Se agotaron los intentos. La aplicación se cerrará.");
        intentos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        intentos.setForeground(TEXTO_SECUNDARIO); intentos.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton aceptar = new JButton("Aceptar"); prepararBoton(aceptar);
        aceptar.setMaximumSize(new Dimension(160, 40));
        aceptar.setAlignmentX(Component.CENTER_ALIGNMENT);
        aceptar.addActionListener(e -> dialogo.dispose());
        panel.add(aviso); panel.add(Box.createVerticalStrut(16)); panel.add(titulo);
        panel.add(Box.createVerticalStrut(15)); panel.add(detalle);
        panel.add(Box.createVerticalStrut(15)); panel.add(intentos);
        panel.add(Box.createVerticalStrut(22)); panel.add(aceptar);
        dialogo.setContentPane(panel); dialogo.getRootPane().setDefaultButton(aceptar);
        dialogo.pack(); dialogo.setLocationRelativeTo(this); dialogo.setVisible(true);
        campoClave.requestFocusInWindow();
    }

    public void setProcesando(boolean procesando) {
        campoUsuario.setEnabled(!procesando); campoClave.setEnabled(!procesando);
        mostrarClave.setEnabled(!procesando); botonIngresar.setEnabled(!procesando);
        botonIngresar.setText(procesando ? "Verificando..." : "Ingresar");
        setDefaultCloseOperation(procesando ? DO_NOTHING_ON_CLOSE : EXIT_ON_CLOSE);
    }

    /** Isotipo de cruz y hoja recreado en Java2D para que no se pixele al escalar. */
    public static class MarcaIcon implements Icon {
        private final int tamano;
        public MarcaIcon(int tamano) { this.tamano = tamano; }
        public int getIconWidth() { return tamano; }
        public int getIconHeight() { return tamano; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.translate(x, y); p.scale(tamano / 64.0, tamano / 64.0);
            p.setPaint(new GradientPaint(8, 8, new Color(0x06B6D4), 45, 55, new Color(0x0D9488)));
            p.fillRoundRect(22, 3, 17, 55, 5, 5); p.fillRoundRect(5, 21, 51, 18, 5, 5);
            Path2D hoja = new Path2D.Double();
            hoja.moveTo(29, 39); hoja.curveTo(26, 24, 44, 18, 61, 19);
            hoja.curveTo(60, 39, 49, 49, 34, 43); hoja.closePath();
            p.setPaint(new GradientPaint(30, 24, new Color(0x10B981), 56, 44, new Color(0x059669)));
            p.fill(hoja); p.setColor(Color.WHITE); p.setStroke(new BasicStroke(1.5f)); p.draw(hoja);
            Path2D nervio = new Path2D.Double(); nervio.moveTo(25, 52);
            nervio.curveTo(29, 40, 40, 35, 53, 28); p.draw(nervio); p.dispose();
        }
    }

    public static Image crearIconoVentana() {
        BufferedImage imagen = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics(); new MarcaIcon(64).paintIcon(null, g, 0, 0); g.dispose();
        return imagen;
    }

    private static class CampoBorde extends AbstractBorder {
        private final Color color;
        CampoBorde(Color color) { this.color = color; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.top = i.left = i.bottom = i.right = 1; return i;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(color); p.drawRoundRect(x, y, w - 1, h - 1, 9, 9); p.dispose();
        }
    }

    private static class AccesoIcon implements Icon {
        private final boolean clave;
        AccesoIcon(boolean clave) { this.clave = clave; }
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create(); p.translate(x, y);
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(AZUL); p.setStroke(new BasicStroke(1.7f));
            if (clave) {
                p.drawRoundRect(6, 2, 9, 12, 8, 8); p.fillRoundRect(3, 9, 15, 12, 3, 3);
                p.setColor(Color.WHITE); p.fillOval(9, 13, 3, 3); p.fillRect(10, 15, 1, 3);
            } else { p.fillOval(7, 2, 8, 8); p.fillRoundRect(3, 12, 16, 10, 9, 9); }
            p.dispose();
        }
    }
}

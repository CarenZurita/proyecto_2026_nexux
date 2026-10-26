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
    private final JButton botonIngresar = new BotonRedondeado("Ingresar");
    private final JCheckBox mostrarClave = new JCheckBox("Mostrar contraseña");
    private final JLabel mensaje = new JLabel(" ");
    private final JButton botonRecuperar = new JButton("¿Olvidaste tu contraseña?");

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
        lema.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lema.setForeground(TEXTO_SECUNDARIO);
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
        prepararCasilla(mostrarClave);
        mostrarClave.addActionListener(e -> campoClave.setEchoChar(
                mostrarClave.isSelected() ? (char) 0 : caracterOculto));
        agregar(formulario, c, mostrarClave, 6, 0, 22);
        prepararBoton(botonIngresar);
        agregar(formulario, c, botonIngresar, 7, 0, 10);
        botonRecuperar.setForeground(AZUL);
        botonRecuperar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        botonRecuperar.setContentAreaFilled(false); botonRecuperar.setBorderPainted(false);
        botonRecuperar.setFocusPainted(false);
        botonRecuperar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botonRecuperar.setRolloverEnabled(true);
        Font fuenteEnlace = botonRecuperar.getFont();
        Runnable actualizarEnlace = () -> {
            boolean resaltado = botonRecuperar.isEnabled()
                    && (botonRecuperar.getModel().isRollover() || botonRecuperar.hasFocus());
            botonRecuperar.setForeground(!botonRecuperar.isEnabled() ? TEXTO_SECUNDARIO
                    : resaltado ? new Color(0x1D4ED8) : AZUL);
            botonRecuperar.setFont(resaltado ? fuenteEnlace.deriveFont(java.util.Map.of(
                    java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON))
                    : fuenteEnlace);
        };
        botonRecuperar.getModel().addChangeListener(e -> actualizarEnlace.run());
        botonRecuperar.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { actualizarEnlace.run(); }
            @Override public void focusLost(FocusEvent e) { actualizarEnlace.run(); }
        });
        agregar(formulario, c, botonRecuperar, 8, 0, 2);
        mensaje.setForeground(ROJO);
        mensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mensaje.setPreferredSize(new Dimension(325, 45));
        agregar(formulario, c, mensaje, 9, 0, 6);
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
        boton.setOpaque(!(boton instanceof BotonRedondeado));
        boton.setContentAreaFilled(!(boton instanceof BotonRedondeado));
        boton.setFocusPainted(false); boton.setBorderPainted(false);
        boton.setFont(new Font("SansSerif", Font.BOLD, 15));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(330, 46));
    }

    public String getUsuario() { return campoUsuario.getText().trim(); }
    public char[] getClave() { return campoClave.getPassword(); }
    public void alIngresar(ActionListener accion) { botonIngresar.addActionListener(accion); }
    public void alRecuperar(ActionListener accion) { botonRecuperar.addActionListener(accion); }
    public void establecerUsuario(String nombre) { campoUsuario.setText(nombre); }
    public void limpiarClave() { campoClave.setText(""); campoClave.requestFocusInWindow(); }
    public void mostrarMensaje(String texto) {
        mensaje.setForeground(ROJO); mensaje.setText("<html>" + escapar(texto) + "</html>");
    }
    public void mostrarExito(String texto) {
        mensaje.setForeground(new Color(0x047857));
        mensaje.setText("<html>" + escapar(texto) + "</html>");
    }
    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public DialogoRecuperacion crearDialogoRecuperacion(boolean configuracion, String usuario) {
        return new DialogoRecuperacion(this, configuracion, usuario);
    }

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
                + "Los credenciales ingresados no son válidos<br>o la cuenta está deshabilitada.<br>"
                + "Intente nuevamente.</div></html>");
        detalle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detalle.setForeground(AZUL_OSCURO); detalle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel intentos = new JLabel(restantes > 0 ? "Intentos restantes: " + restantes
                : "Se agotaron los intentos. La aplicación se cerrará.");
        intentos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        intentos.setForeground(TEXTO_SECUNDARIO); intentos.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton aceptar = new BotonRedondeado("Aceptar"); prepararBoton(aceptar);
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
        botonRecuperar.setEnabled(!procesando);
        botonIngresar.setText(procesando ? "Verificando..." : "Ingresar");
        setDefaultCloseOperation(procesando ? DO_NOTHING_ON_CLOSE : EXIT_ON_CLOSE);
    }

    /** Un mismo diálogo presenta las dos preguntas y, tras verificarlas, la nueva clave. */
    public static class DialogoRecuperacion extends JDialog {
        private final boolean configuracion;
        private final JPasswordField campoColor = new JPasswordField(22);
        private final JPasswordField campoComida = new JPasswordField(22);
        private final JPasswordField campoNueva = new JPasswordField(22);
        private final JPasswordField campoConfirmacion = new JPasswordField(22);
        private final JCheckBox mostrarRespuestas = new JCheckBox("Mostrar respuestas");
        private final JCheckBox mostrarNueva = new JCheckBox("Mostrar contraseña");
        private final JButton botonRespuestas;
        private final JButton botonRestablecer = new BotonRedondeado("Restablecer contraseña");
        private final JButton cancelar = new BotonRedondeado("Cancelar");
        private final JLabel error = new JLabel(" ");
        private final JLabel titulo = new JLabel();
        private final JLabel detalle = new JLabel();
        private final CardLayout tarjetas = new CardLayout();
        private final JPanel contenido = new JPanel(tarjetas);
        private boolean procesando;

        private DialogoRecuperacion(LoginView padre, boolean configuracion, String usuario) {
            super(padre, configuracion ? "Configurar recuperación" : "Recuperar contraseña", true);
            this.configuracion = configuracion;
            botonRespuestas = new BotonRedondeado(configuracion ? "Guardar respuestas" : "Verificar respuestas");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE); setResizable(false);
            JPanel base = new JPanel(new BorderLayout(0, 16));
            base.setBackground(Color.WHITE);
            base.setBorder(BorderFactory.createEmptyBorder(24, 30, 22, 30));
            JPanel encabezado = new JPanel(); encabezado.setOpaque(false);
            encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
            JLabel marca = new JLabel("Salud Vida", new MarcaIcon(30), SwingConstants.LEFT);
            marca.setIconTextGap(9); marca.setFont(new Font("SansSerif", Font.BOLD, 16));
            marca.setForeground(AZUL_OSCURO);
            titulo.setFont(new Font("SansSerif", Font.BOLD, 23)); titulo.setForeground(AZUL_OSCURO);
            detalle.setFont(new Font("SansSerif", Font.PLAIN, 12)); detalle.setForeground(TEXTO_SECUNDARIO);
            JLabel cuenta = new JLabel("Cuenta: " + usuario);
            cuenta.setFont(new Font("SansSerif", Font.PLAIN, 12)); cuenta.setForeground(TEXTO_SECUNDARIO);
            encabezado.add(marca); encabezado.add(Box.createVerticalStrut(17));
            encabezado.add(titulo); encabezado.add(Box.createVerticalStrut(9));
            encabezado.add(detalle); encabezado.add(Box.createVerticalStrut(9)); encabezado.add(cuenta);
            base.add(encabezado, BorderLayout.NORTH);

            JPanel preguntas = formulario();
            GridBagConstraints c = restricciones();
            fila(preguntas, c, etiquetaDialogo("¿Cuál es tu color favorito?", campoColor), 0);
            fila(preguntas, c, campoDialogo(campoColor), 1);
            fila(preguntas, c, etiquetaDialogo("¿Cuál es tu comida favorita?", campoComida), 2);
            fila(preguntas, c, campoDialogo(campoComida), 3);
            fila(preguntas, c, mostrarRespuestas, 4);
            prepararBoton(botonRespuestas); fila(preguntas, c, botonRespuestas, 5);
            char ecoColor = campoColor.getEchoChar(), ecoComida = campoComida.getEchoChar();
            prepararCasilla(mostrarRespuestas);
            mostrarRespuestas.addActionListener(e -> {
                campoColor.setEchoChar(mostrarRespuestas.isSelected() ? (char) 0 : ecoColor);
                campoComida.setEchoChar(mostrarRespuestas.isSelected() ? (char) 0 : ecoComida);
            });

            JPanel nueva = formulario(); c = restricciones();
            fila(nueva, c, etiquetaDialogo("Nueva contraseña", campoNueva), 0);
            fila(nueva, c, campoDialogo(campoNueva), 1);
            fila(nueva, c, etiquetaDialogo("Confirmar nueva contraseña", campoConfirmacion), 2);
            fila(nueva, c, campoDialogo(campoConfirmacion), 3);
            prepararCasilla(mostrarNueva);
            fila(nueva, c, mostrarNueva, 4);
            prepararBoton(botonRestablecer); fila(nueva, c, botonRestablecer, 5);
            char ecoClave = campoNueva.getEchoChar();
            mostrarNueva.addActionListener(e -> {
                campoNueva.setEchoChar(mostrarNueva.isSelected() ? (char) 0 : ecoClave);
                campoConfirmacion.setEchoChar(mostrarNueva.isSelected() ? (char) 0 : ecoClave);
            });
            contenido.setOpaque(false);
            contenido.add(preguntas, "preguntas"); contenido.add(nueva, "nueva");
            base.add(contenido, BorderLayout.CENTER);
            JPanel pie = new JPanel(new BorderLayout(0, 8)); pie.setOpaque(false);
            error.setForeground(ROJO); error.setFont(new Font("SansSerif", Font.PLAIN, 12));
            error.setPreferredSize(new Dimension(390, 60));
            cancelar.setFocusPainted(false); cancelar.setBackground(new Color(0xF1F5F9));
            cancelar.setFont(new Font("SansSerif", Font.BOLD, 13));
            cancelar.setForeground(AZUL_OSCURO);
            cancelar.setPreferredSize(new Dimension(120, 35)); cancelar.addActionListener(e -> dispose());
            pie.add(error, BorderLayout.CENTER); pie.add(cancelar, BorderLayout.SOUTH);
            base.add(pie, BorderLayout.SOUTH);
            titulo.setText(configuracion ? "Configura tus preguntas" : "Preguntas de seguridad");
            detalle.setText(configuracion ? "Registra tus respuestas antes de olvidar la contraseña."
                    : "Paso 1 de 2 · Responde ambas preguntas.");
            getRootPane().setDefaultButton(botonRespuestas);
            setContentPane(base); base.setPreferredSize(new Dimension(470, 560));
            pack(); setLocationRelativeTo(padre);
        }

        private static JPanel formulario() {
            JPanel p = new JPanel(new GridBagLayout()); p.setOpaque(false); return p;
        }
        private static GridBagConstraints restricciones() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
            c.insets = new Insets(4, 0, 4, 0); return c;
        }
        private static void fila(JPanel p, GridBagConstraints c, JComponent elemento, int n) {
            c.gridy = n; p.add(elemento, c);
        }
        private static JLabel etiquetaDialogo(String texto, JComponent campo) {
            JLabel l = new JLabel(texto); l.setLabelFor(campo); l.setForeground(AZUL_OSCURO);
            l.setFont(new Font("SansSerif", Font.BOLD, 13)); return l;
        }
        private static JPasswordField campoDialogo(JPasswordField campo) {
            campo.setFont(new Font("SansSerif", Font.PLAIN, 15));
            campo.setForeground(AZUL_OSCURO); campo.setCaretColor(AZUL);
            campo.setPreferredSize(new Dimension(390, 42));
            campo.setBorder(bordeCampoDialogo(new Color(0xCADCF0)));
            campo.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { campo.setBorder(bordeCampoDialogo(AZUL)); }
                @Override public void focusLost(FocusEvent e) { campo.setBorder(bordeCampoDialogo(new Color(0xCADCF0))); }
            });
            return campo;
        }
        private static javax.swing.border.Border bordeCampoDialogo(Color color) {
            return BorderFactory.createCompoundBorder(new CampoBorde(color),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }
        public char[] getColor() { return campoColor.getPassword(); }
        public char[] getComida() { return campoComida.getPassword(); }
        public char[] getNuevaClave() { return campoNueva.getPassword(); }
        public char[] getConfirmacion() { return campoConfirmacion.getPassword(); }
        public void alVerificar(ActionListener accion) { botonRespuestas.addActionListener(accion); }
        public void alRestablecer(ActionListener accion) { botonRestablecer.addActionListener(accion); }
        public boolean estaProcesando() { return procesando; }
        public void mostrarError(String texto) { error.setText("<html>" + escapar(texto) + "</html>"); }
        public void mostrarRestablecimiento() {
            campoColor.setText(""); campoComida.setText(""); error.setText(" ");
            titulo.setText("Restablecer contraseña");
            detalle.setText("Paso 2 de 2 · Escribe una clave de 8 a 128 caracteres.");
            tarjetas.show(contenido, "nueva");
            getRootPane().setDefaultButton(botonRestablecer); campoNueva.requestFocusInWindow();
        }
        public void setProcesando(boolean valor) {
            procesando = valor;
            campoColor.setEnabled(!valor); campoComida.setEnabled(!valor);
            campoNueva.setEnabled(!valor); campoConfirmacion.setEnabled(!valor);
            mostrarRespuestas.setEnabled(!valor); mostrarNueva.setEnabled(!valor);
            botonRespuestas.setEnabled(!valor); botonRestablecer.setEnabled(!valor); cancelar.setEnabled(!valor);
            botonRespuestas.setText(valor ? "Verificando..." : configuracion ? "Guardar respuestas" : "Verificar respuestas");
            botonRestablecer.setText(valor ? "Guardando..." : "Restablecer contraseña");
            setDefaultCloseOperation(valor ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        }
        @Override public void dispose() {
            campoColor.setText(""); campoComida.setText(""); campoNueva.setText(""); campoConfirmacion.setText("");
            super.dispose();
        }
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

    /** Apariencia común de las casillas; conserva su comportamiento y teclado. */
    private static void prepararCasilla(JCheckBox casilla) {
        casilla.setOpaque(false);
        casilla.setForeground(TEXTO_SECUNDARIO);
        casilla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        casilla.setIcon(new CasillaIcon());
        casilla.setIconTextGap(8);
        casilla.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        casilla.setFocusPainted(false);
        casilla.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static class CasillaIcon implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton boton = (AbstractButton)c;
            Graphics2D p = (Graphics2D)g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean seleccionado = boton.isSelected();
            p.setColor(!boton.isEnabled() ? new Color(0xE2E8F0) : seleccionado ? AZUL : Color.WHITE);
            p.fillRoundRect(x + 1, y + 1, 17, 17, 6, 6);
            p.setColor(!boton.isEnabled() ? new Color(0xCBD5E1) : seleccionado ? AZUL : TEXTO_SECUNDARIO);
            p.setStroke(new BasicStroke(1.4f));
            p.drawRoundRect(x + 1, y + 1, 17, 17, 6, 6);
            if (seleccionado) {
                p.setColor(Color.WHITE); p.setStroke(new BasicStroke(2));
                p.drawLine(x + 5, y + 9, x + 8, y + 12);
                p.drawLine(x + 8, y + 12, x + 14, y + 6);
            }
            if (boton.hasFocus()) {
                p.setColor(AZUL); p.setStroke(new BasicStroke(1));
                p.drawRoundRect(x, y, 19, 19, 7, 7);
            }
            p.dispose();
        }
    }

    /** Botón redondeado sin línea interior; conserva estados de hover, presión y espera. */
    private static class BotonRedondeado extends JButton {
        BotonRedondeado(String texto) {
            super(texto);
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false);
            setFocusPainted(false); setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D)g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fondo = getBackground();
            if (!isEnabled()) fondo = AZUL.equals(fondo) ? new Color(0x93B4F5) : new Color(0xE2E8F0);
            else if (getModel().isPressed()) fondo = AZUL.equals(fondo) ? new Color(0x1E40AF) : new Color(0xCBD5E1);
            else if (getModel().isRollover()) fondo = AZUL.equals(fondo) ? new Color(0x1D4ED8) : new Color(0xE2E8F0);
            p.setColor(fondo); p.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            p.dispose();
            super.paintComponent(g);
        }
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
            p.setColor(color); p.drawRoundRect(x, y, w - 1, h - 1, 20, 20); p.dispose();
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

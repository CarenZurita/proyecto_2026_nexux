
package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import model.Rol;
import model.Usuario;

public class MenuAdministradorView extends JFrame {

    private final Usuario sesion;
    private final Runnable alCerrarSesion;

    private CardLayout cardLayout;
    private JPanel panelContenido;

    private final Color AZUL_OSCURO = new Color(30, 41, 59);
    private final Color AZUL = new Color(37, 99, 235);
    private final Color FONDO = new Color(241, 245, 249);
    private final Color GRIS = new Color(100, 116, 139);

    public MenuAdministradorView(Usuario sesion, Runnable alCerrarSesion) {

        if (sesion == null || !sesion.estaActivo()
                || !sesion.estaEnSesion()
                || !Rol.ADMINISTRADOR.equals(sesion.getRol())) {
            throw new SecurityException(
                    "Se requiere una sesión de administrador.");
        }

        this.sesion = sesion;
        this.alCerrarSesion = alCerrarSesion;

        setTitle("Salud Vida - Panel Administrador");
        setSize(1100, 700);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarSesion();
            }
        });

        setLayout(new BorderLayout());

        // PANEL LATERAL
        JPanel menuLateral = new JPanel(new BorderLayout());
        menuLateral.setBackground(AZUL_OSCURO);
        menuLateral.setPreferredSize(new Dimension(230, 0));

        JLabel logo = new JLabel("✚  SALUD VIDA");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setBorder(
                BorderFactory.createEmptyBorder(30, 15, 30, 10));

        menuLateral.add(logo, BorderLayout.NORTH);

        JPanel botones = new JPanel(new GridLayout(6, 1, 0, 10));
        botones.setOpaque(false);
        botones.setBorder(
                BorderFactory.createEmptyBorder(15, 12, 15, 12));

        // PANEL CENTRAL
        cardLayout = new CardLayout();
        panelContenido = new JPanel(cardLayout);

        crearPantalla("Inicio");
        crearPantalla("Medicamentos");
        crearPantalla("Ventas");
        crearPantalla("Usuarios");
        crearPantalla("Reportes");
        crearPantalla("Configuracion");

        agregarBoton(botones, "Inicio");
        agregarBoton(botones, "Medicamentos");
        agregarBoton(botones, "Ventas");
        agregarBoton(botones, "Usuarios");
        agregarBoton(botones, "Reportes");
        agregarBoton(botones, "Configuracion");

        menuLateral.add(botones, BorderLayout.CENTER);

        // CERRAR SESION
        JButton cerrarSesion = new JButton("Cerrar sesión");
        cerrarSesion.setBackground(new Color(239, 68, 68));
        cerrarSesion.setForeground(Color.WHITE);
        cerrarSesion.setFont(
                new Font("SansSerif", Font.BOLD, 14));
        cerrarSesion.setFocusPainted(false);
        cerrarSesion.setOpaque(true);
        cerrarSesion.setBorderPainted(false);
        cerrarSesion.setCursor(
                new Cursor(Cursor.HAND_CURSOR));
        cerrarSesion.setBorder(
                BorderFactory.createEmptyBorder(15, 10, 15, 10));

        cerrarSesion.addActionListener(e -> cerrarSesion());

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBackground(AZUL_OSCURO);
        panelInferior.setBorder(
                BorderFactory.createEmptyBorder(10, 12, 20, 12));
        panelInferior.add(cerrarSesion, BorderLayout.CENTER);

        menuLateral.add(panelInferior, BorderLayout.SOUTH);

        add(menuLateral, BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);

        cardLayout.show(panelContenido, "Inicio");
    }

    // BOTONES DEL MENU LATERAL
    private void agregarBoton(JPanel contenedor, String nombre) {

        JButton boton = new JButton(nombre);

        boton.setFont(
                new Font("SansSerif", Font.BOLD, 15));
        boton.setBackground(AZUL_OSCURO);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(
                BorderFactory.createEmptyBorder(12, 18, 12, 10));
        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        boton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(new Color(51, 65, 85));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(AZUL_OSCURO);
            }
        });

        boton.addActionListener(e ->
                cardLayout.show(panelContenido, nombre));

        contenedor.add(boton);
    }

    // CREAR PANTALLAS
    private void crearPantalla(String nombre) {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO);
        panel.setBorder(
                BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel(
                nombre.equals("Inicio")
                ? "Bienvenido, " + sesion.getNombreCompleto()
                : nombre
        );

        titulo.setFont(
                new Font("SansSerif", Font.BOLD, 28));
        titulo.setForeground(AZUL_OSCURO);

        JLabel descripcion = new JLabel(
                nombre.equals("Inicio")
                ? "Sistema de gestión de farmacia Salud Vida"
                : "Módulo de " + nombre + " - Próximamente"
        );

        descripcion.setFont(
                new Font("SansSerif", Font.PLAIN, 16));
        descripcion.setForeground(GRIS);

        JPanel encabezado = new JPanel(
                new GridLayout(2, 1, 0, 10));

        encabezado.setOpaque(false);
        encabezado.add(titulo);
        encabezado.add(descripcion);

        panel.add(encabezado, BorderLayout.NORTH);

        if (nombre.equals("Inicio")) {

            JPanel tarjetas = new JPanel(
                    new GridLayout(2, 2, 20, 20));

            tarjetas.setOpaque(false);
            tarjetas.setBorder(
                    BorderFactory.createEmptyBorder(35, 0, 0, 0));

            tarjetas.add(crearTarjeta("Medicamentos"));
            tarjetas.add(crearTarjeta("Ventas"));
            tarjetas.add(crearTarjeta("Usuarios"));
            tarjetas.add(crearTarjeta("Reportes"));
            panel.add(tarjetas, BorderLayout.CENTER);
        }

        // MOSTRAR FORMULARIO DE VENTAS
        if (nombre.equals("Ventas")) {
            VentaView vistaVentas = new VentaView(sesion);
            panel.add(vistaVentas, BorderLayout.CENTER);
        }

        panelContenido.add(panel, nombre);
    }
           

    // CREAR TARJETAS CON ICONOS GRAFICOS
    private JPanel crearTarjeta(String nombre) {

        Color colorIcono;
        String descripcion;

        switch (nombre) {
            case "Medicamentos":
                colorIcono = new Color(16, 185, 129);
                descripcion = "Inventario y medicamentos";
                break;

            case "Ventas":
                colorIcono = new Color(37, 99, 235);
                descripcion = "Registro y control de ventas";
                break;

            case "Usuarios":
                colorIcono = new Color(139, 92, 246);
                descripcion = "Administración de usuarios";
                break;

            case "Reportes":
                colorIcono = new Color(245, 158, 11);
                descripcion = "Estadísticas e informes";
                break;

            default:
                colorIcono = AZUL;
                descripcion = "Administración del sistema";
                break;
        }

        JPanel tarjeta = new JPanel(new BorderLayout(0, 12));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(226, 232, 240), 1),
                        BorderFactory.createEmptyBorder(
                                18, 20, 18, 20)
                )
        );

        // PANEL QUE DIBUJA EL ICONO
        JPanel icono = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Fondo circular
                g2.setColor(new Color(
                        colorIcono.getRed(),
                        colorIcono.getGreen(),
                        colorIcono.getBlue(), 28));

                g2.fillOval(cx - 48, cy - 48, 96, 96);

                g2.setColor(colorIcono);
                g2.setStroke(new BasicStroke(
                        5, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));

                if (nombre.equals("Medicamentos")) {

                    // Capsula diagonal
                    Graphics2D capsula =
                            (Graphics2D) g2.create();

                    capsula.rotate(
                            Math.toRadians(-35), cx, cy);

                    capsula.drawRoundRect(
                            cx - 30, cy - 14, 60, 28, 28, 28);

                    capsula.drawLine(
                            cx, cy - 14, cx, cy + 14);

                    capsula.dispose();

                } else if (nombre.equals("Ventas")) {

                    // Carrito de compras
                    g2.drawLine(cx - 33, cy - 26,
                                cx - 24, cy - 26);

                    g2.drawLine(cx - 24, cy - 26,
                                cx - 14, cy + 13);

                    g2.drawLine(cx - 14, cy + 13,
                                cx + 28, cy + 13);

                    g2.drawLine(cx - 18, cy - 16,
                                cx + 34, cy - 16);

                    g2.drawLine(cx + 34, cy - 16,
                                cx + 26, cy + 4);

                    g2.drawLine(cx + 26, cy + 4,
                                cx - 17, cy + 4);

                    g2.fillOval(cx - 14, cy + 20, 10, 10);
                    g2.fillOval(cx + 18, cy + 20, 10, 10);

                } else if (nombre.equals("Usuarios")) {

                    // Personas
                    g2.fillOval(cx - 12, cy - 32, 24, 24);
                    g2.fillOval(cx - 39, cy - 17, 17, 17);
                    g2.fillOval(cx + 22, cy - 17, 17, 17);

                    g2.drawArc(
                            cx - 23, cy - 5, 46, 48, 0, 180);

                    g2.drawArc(
                            cx - 46, cy + 5, 27, 27, 0, 180);

                    g2.drawArc(
                            cx + 19, cy + 5, 27, 27, 0, 180);

                } else if (nombre.equals("Reportes")) {

                    // Grafico de barras
                    g2.fillRoundRect(
                            cx - 32, cy + 1, 15, 29, 4, 4);

                    g2.fillRoundRect(
                            cx - 7, cy - 17, 15, 47, 4, 4);

                    g2.fillRoundRect(
                            cx + 18, cy - 31, 15, 61, 4, 4);
                }

                g2.dispose();
            }
        };

        icono.setOpaque(false);
        icono.setPreferredSize(new Dimension(120, 110));

        // TITULO
        JLabel etiqueta = new JLabel(nombre);
        etiqueta.setFont(
                new Font("SansSerif", Font.BOLD, 21));
        etiqueta.setForeground(AZUL_OSCURO);
        etiqueta.setHorizontalAlignment(
                SwingConstants.CENTER);

        // DESCRIPCION
        JLabel detalle = new JLabel(descripcion);
        detalle.setFont(
                new Font("SansSerif", Font.PLAIN, 13));
        detalle.setForeground(GRIS);
        detalle.setHorizontalAlignment(
                SwingConstants.CENTER);

        JPanel informacion = new JPanel(
                new GridLayout(2, 1, 0, 4));

        informacion.setOpaque(false);
        informacion.add(etiqueta);
        informacion.add(detalle);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(icono, BorderLayout.CENTER);
        centro.add(informacion, BorderLayout.SOUTH);

        // BOTON
        JButton acceder = new JButton("Acceder  →");
        acceder.setBackground(AZUL);
        acceder.setForeground(Color.WHITE);
        acceder.setFont(
                new Font("SansSerif", Font.BOLD, 14));
        acceder.setFocusPainted(false);
        acceder.setOpaque(true);
        acceder.setBorderPainted(false);
        acceder.setPreferredSize(new Dimension(0, 40));
        acceder.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        acceder.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                acceder.setBackground(
                        new Color(29, 78, 216));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                acceder.setBackground(AZUL);
            }
        });

        acceder.addActionListener(e ->
                cardLayout.show(panelContenido, nombre));

        tarjeta.add(centro, BorderLayout.CENTER);
        tarjeta.add(acceder, BorderLayout.SOUTH);

        return tarjeta;
    }

    // CERRAR SESION
    private void cerrarSesion() {

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea cerrar sesión?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta == JOptionPane.YES_OPTION) {
            sesion.cerrarSesion();
            dispose();
            alCerrarSesion.run();
        }
    }
}


package view;

import javax.swing.*;
import java.awt.*;

public class MenuAdministradorView extends JFrame {

    private CardLayout cardLayout;
    private JPanel panelContenido;

    private final Color AZUL_OSCURO = new Color(30, 41, 59);
    private final Color AZUL = new Color(37, 99, 235);
    private final Color FONDO = new Color(241, 245, 249);

    public MenuAdministradorView() {
        setTitle("Salud Vida - Panel Administrador");
        setSize(1100, 700);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        // Panel lateral
        JPanel menuLateral = new JPanel();
        menuLateral.setBackground(AZUL_OSCURO);
        menuLateral.setPreferredSize(new Dimension(230, 0));
        menuLateral.setLayout(new BorderLayout());

        JLabel logo = new JLabel("✚  SALUD VIDA");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setBorder(BorderFactory.createEmptyBorder(30, 15, 30, 10));

        menuLateral.add(logo, BorderLayout.NORTH);

        // Botones del menu
        JPanel botones = new JPanel();
        botones.setLayout(new GridLayout(6, 1, 0, 10));
        botones.setOpaque(false);
        botones.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));

        // Panel central con CardLayout
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

        // Boton cerrar sesion
        JButton cerrarSesion = new JButton("Cerrar sesion");
        cerrarSesion.setBackground(new Color(239, 68, 68));
        cerrarSesion.setForeground(Color.WHITE);
        cerrarSesion.setFocusPainted(false);
        cerrarSesion.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        cerrarSesion.addActionListener(e -> {
            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea cerrar sesion?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                dispose();
                // Aqui conectaremos LoginView posteriormente.
            }
        });

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBackground(AZUL_OSCURO);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(10, 12, 20, 12));
        panelInferior.add(cerrarSesion);

        menuLateral.add(panelInferior, BorderLayout.SOUTH);

        add(menuLateral, BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);

        cardLayout.show(panelContenido, "Inicio");
    }

    private void agregarBoton(JPanel contenedor, String nombre) {
        JButton boton = new JButton(nombre);
        boton.setFont(new Font("SansSerif", Font.BOLD, 15));
        boton.setBackground(AZUL_OSCURO);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 10));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setOpaque(true);

        boton.addActionListener(e -> {
            cardLayout.show(panelContenido, nombre);
        });

        contenedor.add(boton);
    }

    private void crearPantalla(String nombre) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel(
            nombre.equals("Inicio") ? "Bienvenido, Administrador" : nombre
        );
        titulo.setFont(new Font("SansSerif", Font.BOLD, 28));
        titulo.setForeground(AZUL_OSCURO);

        JLabel descripcion = new JLabel(
            nombre.equals("Inicio")
                ? "Sistema de gestion de farmacia Salud Vida"
                : "Modulo de " + nombre + " - Proximamente"
        );
        descripcion.setFont(new Font("SansSerif", Font.PLAIN, 16));
        descripcion.setForeground(Color.GRAY);

        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 10));
        encabezado.setOpaque(false);
        encabezado.add(titulo);
        encabezado.add(descripcion);

        panel.add(encabezado, BorderLayout.NORTH);

        if (nombre.equals("Inicio")) {
            JPanel tarjetas = new JPanel(new GridLayout(2, 2, 20, 20));
            tarjetas.setOpaque(false);
            tarjetas.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));

            tarjetas.add(crearTarjeta("Medicamentos"));
            tarjetas.add(crearTarjeta("Ventas"));
            tarjetas.add(crearTarjeta("Usuarios"));
            tarjetas.add(crearTarjeta("Reportes"));

            panel.add(tarjetas, BorderLayout.CENTER);
        }

        panelContenido.add(panel, nombre);
    }

    private JPanel crearTarjeta(String nombre) {
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(25, 20, 25, 20)
        ));

        JLabel etiqueta = new JLabel(nombre);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 21));
        etiqueta.setForeground(AZUL);

        JButton acceder = new JButton("Acceder");
        acceder.setBackground(AZUL);
        acceder.setForeground(Color.WHITE);
        acceder.setFocusPainted(false);

        acceder.addActionListener(e ->
            cardLayout.show(panelContenido, nombre)
        );

        tarjeta.add(etiqueta, BorderLayout.NORTH);
        tarjeta.add(acceder, BorderLayout.SOUTH);

        return tarjeta;
    }

    // Metodo temporal para probar el menu
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MenuAdministradorView().setVisible(true);
        });
    }
}

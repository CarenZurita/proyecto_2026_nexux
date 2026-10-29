
package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import model.Rol;
import model.Usuario;

public class MenuEncargadoView extends JFrame {

    private final Usuario sesion;
    private final Runnable alCerrarSesion;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenido = new JPanel(cardLayout);

    private MedicamentoView vistaMedicamentos;

    private final Map<String, JButton> botonesMenu =
            new LinkedHashMap<>();

    private String pantallaActual = "Inicio";

    // COLORES IGUALES AL ADMINISTRADOR
    private final Color AZUL_OSCURO = new Color(30, 41, 59);
    private final Color AZUL = new Color(37, 99, 235);
    private final Color FONDO = new Color(241, 245, 249);
    private final Color GRIS = new Color(100, 116, 139);

    public MenuEncargadoView(
            Usuario sesion, Runnable alCerrarSesion) {

        // VALIDAR SESION DEL ENCARGADO
        if (sesion == null
                || !sesion.estaActivo()
                || !sesion.estaEnSesion()
                || !Rol.ENCARGADO.equals(sesion.getRol())) {

            throw new SecurityException(
                    "Se requiere una sesión de encargado.");
        }

        this.sesion = sesion;
        this.alCerrarSesion = alCerrarSesion;

        setTitle("Salud Vida - Panel Encargado");
        setIconImage(LoginView.crearIconoVentana());

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
        getContentPane().setBackground(FONDO);

        // PANEL LATERAL IGUAL AL ADMINISTRADOR
        JPanel menuLateral =
                new ComponentesRedondeados.Panel(
                        new BorderLayout(), AZUL_OSCURO, 30);

        menuLateral.setBackground(AZUL_OSCURO);
        menuLateral.setPreferredSize(new Dimension(230, 0));

        // LOGO
        JLabel logo = new JLabel(
                "SALUD VIDA",
                new LoginView.MarcaIcon(32),
                SwingConstants.LEFT);

        logo.setIconTextGap(10);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setBorder(
                BorderFactory.createEmptyBorder(30, 15, 30, 10));

        menuLateral.add(logo, BorderLayout.NORTH);

        // BOTONES DEL MENU
        JPanel botones = new JPanel(
                new GridLayout(3, 1, 0, 10));

        botones.setOpaque(false);
        botones.setBorder(
                BorderFactory.createEmptyBorder(15, 12, 15, 12));

        // CREAR PANTALLAS
        crearPantallas();

        // MODULOS PERMITIDOS PARA ENCARGADO
        agregarBoton(botones, "Inicio");
        agregarBoton(botones, "Medicamentos");
        agregarBoton(botones, "Ventas");

        // MANTENER LOS BOTONES ARRIBA
        JPanel navegacion = new JPanel(new BorderLayout());
        navegacion.setOpaque(false);
        navegacion.add(botones, BorderLayout.NORTH);

        menuLateral.add(navegacion, BorderLayout.CENTER);

        // BOTON CERRAR SESION
        JButton cerrarSesion =
                new ComponentesRedondeados.Boton(
                        "Cerrar sesión",
                        new ComponentesRedondeados.IconoModulo(
                                "salir", 24));

        cerrarSesion.setIconTextGap(10);
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

        JPanel panelInferior =
                new JPanel(new BorderLayout());

        panelInferior.setBackground(AZUL_OSCURO);
        panelInferior.setBorder(
                BorderFactory.createEmptyBorder(10, 12, 20, 12));

        panelInferior.add(
                cerrarSesion, BorderLayout.CENTER);

        menuLateral.add(panelInferior, BorderLayout.SOUTH);

        // AGREGAR PANELES A LA VENTANA
        add(menuLateral, BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);

        mostrarModulo("Inicio");
    }

    // BOTONES DEL MENU LATERAL
    private void agregarBoton(
            JPanel contenedor, String nombre) {

        JButton boton = new ComponentesRedondeados.Boton(
                nombre,
                new ComponentesRedondeados.IconoModulo(
                        nombre, 32));

        boton.setIconTextGap(10);

        boton.setFont(
                new Font("SansSerif", Font.BOLD, 15));

        boton.setBackground(AZUL_OSCURO);
        boton.setForeground(Color.WHITE);

        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);

        boton.setHorizontalAlignment(SwingConstants.LEFT);

        boton.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 8));

        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        // EFECTO HOVER
        boton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(
                        new Color(51, 65, 85));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(
                        nombre.equals(pantallaActual)
                                ? AZUL : AZUL_OSCURO);
            }
        });

        boton.addActionListener(e ->
                mostrarModulo(nombre));

        botonesMenu.put(nombre, boton);
        contenedor.add(boton);
    }

    // CREAR PANTALLAS DEL ENCARGADO
    private void crearPantallas() {

        // PANTALLA INICIO
        JPanel inicio = new JPanel(new BorderLayout());

        inicio.setBackground(FONDO);
        inicio.setBorder(
                BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel(
                "Bienvenido, " + sesion.getNombreCompleto());

        titulo.setFont(
                new Font("SansSerif", Font.BOLD, 28));
        titulo.setForeground(AZUL_OSCURO);

        JLabel descripcion = new JLabel(
                "Sistema de gestión de farmacia Salud Vida");

        descripcion.setFont(
                new Font("SansSerif", Font.PLAIN, 16));
        descripcion.setForeground(GRIS);

        JPanel encabezado =
                new JPanel(new GridLayout(2, 1, 0, 10));

        encabezado.setOpaque(false);
        encabezado.add(titulo);
        encabezado.add(descripcion);

        inicio.add(encabezado, BorderLayout.NORTH);

        // TARJETAS IGUALES AL ADMINISTRADOR
        JPanel tarjetas = new JPanel(
                new GridLayout(1, 2, 20, 20));

        tarjetas.setOpaque(false);
        tarjetas.setPreferredSize(new Dimension(0, 250));

        tarjetas.add(crearTarjeta("Medicamentos"));
        tarjetas.add(crearTarjeta("Ventas"));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);

        centro.setBorder(
                BorderFactory.createEmptyBorder(35, 0, 0, 0));

        centro.add(tarjetas, BorderLayout.NORTH);

        inicio.add(centro, BorderLayout.CENTER);

        panelContenido.add(inicio, "Inicio");

        // PANTALLA MEDICAMENTOS
        JPanel medicamentos = new JPanel(new BorderLayout());

        vistaMedicamentos = new MedicamentoView(sesion);

        medicamentos.add(
                vistaMedicamentos, BorderLayout.CENTER);

        panelContenido.add(medicamentos, "Medicamentos");

        // PANTALLA VENTAS
        JPanel ventas = new JPanel(new BorderLayout());

        ventas.setBackground(FONDO);
        ventas.setBorder(
                BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel tituloVentas =
                new JLabel("Registro de ventas");

        tituloVentas.setFont(
                new Font("SansSerif", Font.BOLD, 28));
        tituloVentas.setForeground(AZUL_OSCURO);

        JPanel encabezadoVentas =
                new JPanel(new BorderLayout());

        encabezadoVentas.setOpaque(false);
        encabezadoVentas.add(
                tituloVentas, BorderLayout.CENTER);

        ventas.add(encabezadoVentas, BorderLayout.NORTH);

        // SE CONSERVA EL FORMULARIO ORIGINAL
        VentaView vistaVentas = new VentaView(sesion);

        ventas.add(vistaVentas, BorderLayout.CENTER);

        panelContenido.add(ventas, "Ventas");
    }

    // CREAR TARJETAS CON EL ESTILO DEL ADMINISTRADOR
    private JPanel crearTarjeta(String nombre) {

        String descripcion;

        switch (nombre) {

            case "Medicamentos":
                descripcion = "Inventario y medicamentos";
                break;

            case "Ventas":
                descripcion = "Registro y control de ventas";
                break;

            default:
                descripcion = "Administración del sistema";
                break;
        }

        JPanel tarjeta =
                new ComponentesRedondeados.Panel(
                        new BorderLayout(0, 12),
                        Color.WHITE,
                        32,
                        new Color(226, 232, 240));

        tarjeta.setBackground(Color.WHITE);

        tarjeta.setBorder(
                BorderFactory.createEmptyBorder(
                        18, 20, 18, 20));

        // ICONO GRANDE
        JLabel icono = new JLabel(
                new ComponentesRedondeados.IconoModulo(
                        nombre, 96),
                SwingConstants.CENTER);

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

        JPanel informacion =
                new JPanel(new GridLayout(2, 1, 0, 4));

        informacion.setOpaque(false);
        informacion.add(etiqueta);
        informacion.add(detalle);

        JPanel centro = new JPanel(new BorderLayout());

        centro.setOpaque(false);
        centro.add(icono, BorderLayout.CENTER);
        centro.add(informacion, BorderLayout.SOUTH);

        // BOTON ACCEDER
        JButton acceder =
                new ComponentesRedondeados.Boton("Acceder  →");

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
                mostrarModulo(nombre));

        tarjeta.add(centro, BorderLayout.CENTER);
        tarjeta.add(acceder, BorderLayout.SOUTH);

        return tarjeta;
    }

    // NAVEGACION ENTRE MODULOS
    private void mostrarModulo(String nombre) {

        // CONSERVAR VALIDACION DE SEGURIDAD
        if (!sesion.estaActivo()
                || !sesion.estaEnSesion()
                || !Rol.ENCARGADO.equals(sesion.getRol())) {

            throw new SecurityException(
                    "Se requiere una sesión de encargado.");
        }

        if (!botonesMenu.containsKey(nombre)) {
            throw new IllegalArgumentException(
                    "Módulo no permitido.");
        }

        // ACTUALIZAR INVENTARIO AL ENTRAR
        if (nombre.equals("Medicamentos")) {
            vistaMedicamentos.recargar();
        }

        pantallaActual = nombre;

        cardLayout.show(panelContenido, nombre);

        // RESALTAR EL MODULO SELECCIONADO
        for (Map.Entry<String, JButton> entrada
                : botonesMenu.entrySet()) {

            entrada.getValue().setBackground(
                    entrada.getKey().equals(nombre)
                            ? AZUL : AZUL_OSCURO);
        }
    }

    // CERRAR SESION
    private void cerrarSesion() {

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea cerrar sesión?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {

            sesion.cerrarSesion();
            dispose();
            alCerrarSesion.run();
        }
    }
}


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
    private MedicamentoView vistaMedicamentos;
    private controller.VencimientoController controladorVencimientos;
    private controller.UsuarioController controladorUsuarios;
    private controller.ReporteVentasController controladorReporteVentas;
    private final java.util.Map<String, JButton> botonesMenu = new java.util.LinkedHashMap<>();
    private String pantallaActual = "Inicio";

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

        // PANEL LATERAL
        JPanel menuLateral = new ComponentesRedondeados.Panel(new BorderLayout(), AZUL_OSCURO, 30);
        menuLateral.setBackground(AZUL_OSCURO);
        menuLateral.setPreferredSize(new Dimension(230, 0));

        JLabel logo = new JLabel("SALUD VIDA",new LoginView.MarcaIcon(32),SwingConstants.LEFT);
        logo.setIconTextGap(10);
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
        JButton cerrarSesion = new ComponentesRedondeados.Boton("Cerrar sesión",
                new ComponentesRedondeados.IconoModulo("salir", 24));
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

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBackground(AZUL_OSCURO);
        panelInferior.setBorder(
                BorderFactory.createEmptyBorder(10, 12, 20, 12));
        panelInferior.add(cerrarSesion, BorderLayout.CENTER);

        menuLateral.add(panelInferior, BorderLayout.SOUTH);

        add(menuLateral, BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);

        mostrarPantalla("Inicio");
    }

    // BOTONES DEL MENU LATERAL
    private void agregarBoton(JPanel contenedor, String nombre) {

        JButton boton = new ComponentesRedondeados.Boton(nombre,
                new ComponentesRedondeados.IconoModulo(nombre, 32));
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

        boton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(new Color(51, 65, 85));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(nombre.equals(pantallaActual) ? AZUL : AZUL_OSCURO);
            }
        });

        boton.addActionListener(e ->
                mostrarPantalla(nombre));

        botonesMenu.put(nombre, boton);
        contenedor.add(boton);
    }

    private void mostrarPantalla(String nombre) {
        if (!sesion.estaActivo() || !sesion.estaEnSesion() || !sesion.esAdministrador()) {
            throw new SecurityException("Se requiere una sesión de administrador.");
        }
        pantallaActual = nombre;
        botonesMenu.forEach((clave, boton) -> boton.setBackground(clave.equals(nombre) ? AZUL : AZUL_OSCURO));
        cardLayout.show(panelContenido, nombre);
        if (nombre.equals("Medicamentos")) vistaMedicamentos.recargar();
        if (nombre.equals("Usuarios")) controladorUsuarios.actualizar();
        if (nombre.equals("Reportes")) {
            controladorVencimientos.actualizar();
            controladorReporteVentas.actualizar();
        }
    }

    // CREAR PANTALLAS
    private void crearPantalla(String nombre) {
        if (nombre.equals("Configuracion")) {
            panelContenido.add(new ConfiguracionView(sesion), nombre);
            return;
        }
        if (nombre.equals("Usuarios")) {
            UsuarioView vistaUsuarios = new UsuarioView();
            controladorUsuarios = new controller.UsuarioController(sesion, vistaUsuarios);
            panelContenido.add(vistaUsuarios, nombre);
            return;
        }

        if (nombre.equals("Medicamentos")) {
            vistaMedicamentos = new MedicamentoView(sesion);
            panelContenido.add(vistaMedicamentos, nombre);
            return;
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO);
        panel.setBorder(
                BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel(
                nombre.equals("Inicio")
                ? "Bienvenido, " + sesion.getNombreCompleto()
                : nombre.equals("Ventas") ? "Registro de ventas"
                : nombre.equals("Reportes") ? "Reportes" : nombre
        );

        titulo.setFont(
                new Font("SansSerif", Font.BOLD, 28));
        titulo.setForeground(AZUL_OSCURO);

        JLabel descripcion = new JLabel(
                nombre.equals("Inicio")
                ? "Sistema de gestión de farmacia Salud Vida"
                : nombre.equals("Reportes")
                ? "Consulta los reportes de la farmacia"
                : "Módulo de " + nombre + " - Próximamente"
        );

        descripcion.setFont(
                new Font("SansSerif", Font.PLAIN, 16));
        descripcion.setForeground(GRIS);

        JPanel encabezado = new JPanel(
                new GridLayout(nombre.equals("Ventas") ? 1 : 2, 1, 0, 10));

        encabezado.setOpaque(false);
        encabezado.add(titulo);
        if (!nombre.equals("Ventas")) encabezado.add(descripcion);
        else panel.setLayout(new BorderLayout(0, 16));

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

        if (nombre.equals("Reportes")) {
            VencimientoView vistaVencimientos = new VencimientoView();
            controladorVencimientos = new controller.VencimientoController(sesion, vistaVencimientos);
            ReporteVentasView vistaVentas = new ReporteVentasView();
            controladorReporteVentas = new controller.ReporteVentasController(sesion, vistaVentas);
            ReportesView reportes = new ReportesView(vistaVencimientos, vistaVentas);
            reportes.alSeleccionarVencimientos(() -> controladorVencimientos.actualizar());
            reportes.alSeleccionarVentas(() -> controladorReporteVentas.actualizar());
            panel.add(reportes, BorderLayout.CENTER);
        }

        panelContenido.add(panel, nombre);
    }
           

    // CREAR TARJETAS CON ICONOS GRAFICOS
    private JPanel crearTarjeta(String nombre) {

        String descripcion;

        switch (nombre) {
            case "Medicamentos":
                descripcion = "Inventario y medicamentos";
                break;

            case "Ventas":
                descripcion = "Registro y control de ventas";
                break;

            case "Usuarios":
                descripcion = "Administración de usuarios";
                break;

            case "Reportes":
                descripcion = "Estadísticas e informes";
                break;

            default:
                descripcion = "Administración del sistema";
                break;
        }

        JPanel tarjeta = new ComponentesRedondeados.Panel(new BorderLayout(0, 12),
                Color.WHITE, 32, new Color(226, 232, 240));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // El icono compartido conserva el mismo dibujo que el menú lateral.
        JLabel icono = new JLabel(new ComponentesRedondeados.IconoModulo(nombre, 96),
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
        JButton acceder = new ComponentesRedondeados.Boton("Acceder  →");
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
                mostrarPantalla(nombre));

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

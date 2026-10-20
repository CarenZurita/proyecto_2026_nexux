package view;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import model.Rol;
import model.Usuario;

/** Menú de encargado con la misma estructura visual del panel administrador. */
public class MenuEncargadoView extends JFrame {
    private static final Color AZUL_OSCURO = new Color(0x1E293B);
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color FONDO = new Color(0xF1F5F9);
    private static final Color VERDE = new Color(0x10B981);
    private static final Color ROJO = new Color(0xEF4444);
    private final Usuario sesion;
    private final Runnable alCerrarSesion;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenido = new JPanel(cardLayout);
    private final Map<String, JButton> botones = new LinkedHashMap<>();

    public MenuEncargadoView(Usuario sesion, Runnable alCerrarSesion) {
        if (sesion == null || !sesion.estaActivo() || !sesion.estaEnSesion()
                || !Rol.ENCARGADO.equals(sesion.getRol())) {
            throw new SecurityException("Se requiere una sesión de encargado.");
        }
        this.sesion = sesion; this.alCerrarSesion = alCerrarSesion;
        setTitle("Salud Vida - Panel Encargado");
        setIconImage(LoginView.crearIconoVentana());
        setSize(1100, 700); setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { cerrarSesion(); }
        });
        setLayout(new BorderLayout());
        crearPantallas();
        add(crearMenuLateral(), BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);
        mostrarModulo("Inicio");
    }

    private JPanel crearMenuLateral() {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(AZUL_OSCURO); lateral.setPreferredSize(new Dimension(230, 0));
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        marca.setOpaque(false); marca.setBorder(BorderFactory.createEmptyBorder(26, 8, 25, 8));
        marca.add(new JLabel(new LoginView.MarcaIcon(36)));
        JLabel nombre = new JLabel("SALUD VIDA");
        nombre.setFont(new Font("SansSerif", Font.BOLD, 19)); nombre.setForeground(Color.WHITE);
        marca.add(nombre); lateral.add(marca, BorderLayout.NORTH);
        JPanel navegacion = new JPanel(new BorderLayout()); navegacion.setOpaque(false);
        JPanel opciones = new JPanel(new GridLayout(3, 1, 0, 9)); opciones.setOpaque(false);
        opciones.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));
        agregarBoton(opciones, "Inicio", "inicio");
        agregarBoton(opciones, "Medicamentos", "medicamentos");
        agregarBoton(opciones, "Ventas", "ventas");
        navegacion.add(opciones, BorderLayout.NORTH);
        lateral.add(navegacion, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(0, 18)); pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(12, 12, 20, 12));
        JPanel cuenta = new JPanel(new GridLayout(2, 1, 0, 4)); cuenta.setOpaque(false);
        JLabel rol = new JLabel("ENCARGADO"); rol.setForeground(new Color(0x93C5FD));
        rol.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel usuario = new JLabel(sesion.getNombreCompleto()); usuario.setForeground(Color.WHITE);
        usuario.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cuenta.add(rol); cuenta.add(usuario);
        JButton salir = new JButton("Cerrar sesión", new ModuloIcon("salir", Color.WHITE, 20));
        salir.setBackground(ROJO); salir.setForeground(Color.WHITE); salir.setOpaque(true);
        salir.setFocusPainted(false); salir.setBorderPainted(false);
        salir.setFont(new Font("SansSerif", Font.BOLD, 14));
        salir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        salir.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        salir.addActionListener(e -> cerrarSesion());
        pie.add(cuenta, BorderLayout.NORTH); pie.add(salir, BorderLayout.SOUTH);
        lateral.add(pie, BorderLayout.SOUTH);
        return lateral;
    }

    private void agregarBoton(JPanel contenedor, String nombre, String icono) {
        JButton boton = new JButton(nombre, new ModuloIcon(icono, Color.WHITE, 21));
        boton.setIconTextGap(13); boton.setBackground(AZUL_OSCURO); boton.setForeground(Color.WHITE);
        boton.setFont(new Font("SansSerif", Font.BOLD, 15)); boton.setFocusPainted(false);
        boton.setOpaque(true); boton.setBorderPainted(false);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 10));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(e -> mostrarModulo(nombre));
        botones.put(nombre, boton); contenedor.add(boton);
    }

    private JPanel crearBase(String titulo, String descripcion) {
        JPanel panel = new JPanel(new BorderLayout(0, 28)); panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 10)); encabezado.setOpaque(false);
        JLabel nombre = new JLabel(titulo); nombre.setFont(new Font("SansSerif", Font.BOLD, 27));
        nombre.setForeground(AZUL_OSCURO);
        JLabel detalle = new JLabel(descripcion); detalle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        detalle.setForeground(new Color(0x64748B));
        encabezado.add(nombre); encabezado.add(detalle); panel.add(encabezado, BorderLayout.NORTH);
        return panel;
    }

    private void crearPantallas() {
        JPanel inicio = crearBase("Bienvenido, Encargado", "Sistema de gestión de farmacia Salud Vida");
        JPanel accesos = new JPanel(new GridLayout(1, 2, 20, 0)); accesos.setOpaque(false);
        accesos.setPreferredSize(new Dimension(0, 200));
        accesos.add(crearTarjeta("Medicamentos", "Buscar medicamentos y consultar stock", AZUL, "medicamentos"));
        accesos.add(crearTarjeta("Ventas", "Registrar nuevas ventas", VERDE, "ventas"));
        JPanel centro = new JPanel(new BorderLayout()); centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        centro.add(accesos, BorderLayout.NORTH); inicio.add(centro, BorderLayout.CENTER);
        panelContenido.add(inicio, "Inicio");

        JPanel medicamentos = crearBase("Medicamentos", "Consulta del inventario y control de vencimientos");
        JPanel operaciones = new JPanel(new GridLayout(3, 1, 0, 14)); operaciones.setOpaque(false);
        operaciones.add(crearOperacion("Buscar medicamento", "Localiza medicamentos por nombre o código."));
        operaciones.add(crearOperacion("Consultar stock", "Revisa la cantidad disponible de un medicamento."));
        operaciones.add(crearOperacion("Revisar vencimientos", "Consulta medicamentos vencidos y próximos a vencer."));
        medicamentos.add(operaciones, BorderLayout.CENTER); panelContenido.add(medicamentos, "Medicamentos");

        JPanel ventas = crearBase("Ventas", "Registro de ventas de farmacia");
        JPanel caja = new JPanel(new BorderLayout()); caja.setOpaque(false);
        JPanel registro = crearOperacion("Registrar venta", "Registra la venta de medicamentos y verifica el stock.");
        registro.setPreferredSize(new Dimension(0, 160)); caja.add(registro, BorderLayout.NORTH);
        ventas.add(caja, BorderLayout.CENTER); panelContenido.add(ventas, "Ventas");
    }

    private JPanel crearTarjeta(String nombre, String descripcion, Color color, String icono) {
        Color texto = color.equals(VERDE) ? AZUL_OSCURO : Color.WHITE;
        JPanel tarjeta = new JPanel(new BorderLayout(0, 12)); tarjeta.setBackground(color);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(22, 22, 20, 22));
        tarjeta.add(new JLabel(new ModuloIcon(icono, texto, 34)), BorderLayout.NORTH);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 5)); textos.setOpaque(false);
        JLabel titulo = new JLabel(nombre); titulo.setFont(new Font("SansSerif", Font.BOLD, 21));
        titulo.setForeground(texto);
        JLabel detalle = new JLabel("<html>" + descripcion + "</html>");
        detalle.setFont(new Font("SansSerif", Font.PLAIN, 12)); detalle.setForeground(texto);
        textos.add(titulo); textos.add(detalle); tarjeta.add(textos, BorderLayout.CENTER);
        JButton acceder = new JButton("Acceder  →"); acceder.setHorizontalAlignment(SwingConstants.LEFT);
        acceder.setForeground(texto); acceder.setFont(new Font("SansSerif", Font.BOLD, 13));
        acceder.setContentAreaFilled(false); acceder.setFocusPainted(false); acceder.setBorderPainted(false);
        acceder.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        acceder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        acceder.addActionListener(e -> mostrarModulo(nombre)); tarjeta.add(acceder, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel crearOperacion(String nombre, String descripcion) {
        JPanel panel = new JPanel(new BorderLayout(16, 12)); panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 6)); textos.setOpaque(false);
        JLabel titulo = new JLabel(nombre); titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        titulo.setForeground(AZUL_OSCURO);
        JLabel detalle = new JLabel("<html>" + descripcion + "</html>");
        detalle.setFont(new Font("SansSerif", Font.PLAIN, 13)); detalle.setForeground(new Color(0x64748B));
        textos.add(titulo); textos.add(detalle); panel.add(textos, BorderLayout.CENTER);
        JButton abrir = new JButton("Acceder"); abrir.setBackground(AZUL); abrir.setForeground(Color.WHITE);
        abrir.setOpaque(true); abrir.setBorderPainted(false); abrir.setFocusPainted(false);
        abrir.setFont(new Font("SansSerif", Font.BOLD, 13));
        abrir.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        abrir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        abrir.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Este módulo estará disponible próximamente.", nombre, JOptionPane.INFORMATION_MESSAGE));
        JPanel accion = new JPanel(new GridBagLayout()); accion.setOpaque(false); accion.add(abrir);
        panel.add(accion, BorderLayout.EAST); return panel;
    }

    private void mostrarModulo(String nombre) {
        if (!sesion.estaActivo() || !sesion.estaEnSesion() || !Rol.ENCARGADO.equals(sesion.getRol())) {
            throw new SecurityException("Se requiere una sesión de encargado.");
        }
        if (!botones.containsKey(nombre)) throw new IllegalArgumentException("Módulo no permitido.");
        cardLayout.show(panelContenido, nombre);
        for (Map.Entry<String, JButton> entrada : botones.entrySet()) {
            entrada.getValue().setBackground(entrada.getKey().equals(nombre) ? AZUL : AZUL_OSCURO);
        }
    }

    private void cerrarSesion() {
        int respuesta = JOptionPane.showConfirmDialog(this, "¿Desea cerrar sesión?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (respuesta == JOptionPane.YES_OPTION) {
            sesion.cerrarSesion(); dispose(); alCerrarSesion.run();
        }
    }

    /** Iconos vectoriales pequeños para mantener el mismo aspecto en Windows. */
    private static class ModuloIcon implements Icon {
        private final String tipo; private final Color color; private final int tamano;
        ModuloIcon(String tipo, Color color, int tamano) { this.tipo = tipo; this.color = color; this.tamano = tamano; }
        public int getIconWidth() { return tamano; }
        public int getIconHeight() { return tamano; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create(); p.translate(x, y); p.scale(tamano / 24.0, tamano / 24.0);
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(color); p.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case "inicio" -> {
                    p.drawPolyline(new int[]{3, 12, 21}, new int[]{11, 3, 11}, 3);
                    p.drawRect(6, 10, 12, 11); p.drawRect(10, 15, 4, 6);
                }
                case "medicamentos" -> {
                    p.rotate(.65, 12, 12); p.drawRoundRect(7, 1, 10, 22, 10, 10); p.drawLine(7, 12, 17, 12);
                }
                case "ventas" -> {
                    p.drawPolyline(new int[]{2, 5, 8, 20, 22}, new int[]{3, 3, 15, 15, 7}, 5);
                    p.drawLine(7, 7, 21, 7); p.fillOval(8, 19, 3, 3); p.fillOval(18, 19, 3, 3);
                }
                case "salir" -> {
                    p.drawPolyline(new int[]{11, 4, 4, 11}, new int[]{3, 3, 21, 21}, 4);
                    p.drawLine(10, 12, 22, 12); p.drawLine(18, 8, 22, 12); p.drawLine(18, 16, 22, 12);
                }
                default -> { }
            }
            p.dispose();
        }
    }
}

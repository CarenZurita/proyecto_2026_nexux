package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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
    private MedicamentoView vistaMedicamentos;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenido = new JPanel(cardLayout);
    private final Map<String, JButton> botones = new LinkedHashMap<>();
    private String pantallaActual = "Inicio";

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
        getContentPane().setBackground(FONDO);
        crearPantallas();
        add(crearMenuLateral(), BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);
        mostrarModulo("Inicio");
    }

    private JPanel crearMenuLateral() {
        JPanel lateral = new ComponentesRedondeados.Panel(new BorderLayout(), AZUL_OSCURO, 30);
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
        agregarBoton(opciones, "Inicio");
        agregarBoton(opciones, "Medicamentos");
        agregarBoton(opciones, "Ventas");
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
        JButton salir = new ComponentesRedondeados.Boton("Cerrar sesión", new ComponentesRedondeados.IconoModulo("salir", 24));
        salir.setIconTextGap(10);
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

    private void agregarBoton(JPanel contenedor, String nombre) {
        JButton boton = new ComponentesRedondeados.Boton(nombre, new ComponentesRedondeados.IconoModulo(nombre, 32));
        boton.setIconTextGap(10); boton.setBackground(AZUL_OSCURO); boton.setForeground(Color.WHITE);
        boton.setFont(new Font("SansSerif", Font.BOLD, 15)); boton.setFocusPainted(false);
        boton.setOpaque(true); boton.setBorderPainted(false);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 8));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                boton.setBackground(new Color(51, 65, 85));
            }
            @Override public void mouseExited(MouseEvent e) {
                boton.setBackground(nombre.equals(pantallaActual) ? AZUL : AZUL_OSCURO);
            }
        });
        boton.addActionListener(e -> mostrarModulo(nombre));
        botones.put(nombre, boton); contenedor.add(boton);
    }

    private JPanel crearBase(String titulo, String descripcion) {
        JPanel panel = new JPanel(new BorderLayout(0, 28)); panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        JPanel encabezado = new JPanel(new GridLayout(descripcion.isBlank() ? 1 : 2, 1, 0, 10)); encabezado.setOpaque(false);
        JLabel nombre = new JLabel(titulo); nombre.setFont(new Font("SansSerif", Font.BOLD, 27));
        nombre.setForeground(AZUL_OSCURO);
        JLabel detalle = new JLabel(descripcion); detalle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        detalle.setForeground(new Color(0x64748B));
        encabezado.add(nombre);
        if (!descripcion.isBlank()) encabezado.add(detalle);
        panel.add(encabezado, BorderLayout.NORTH);
        return panel;
    }

    private void crearPantallas() {
        JPanel inicio = crearBase("Bienvenido, Encargado", "Sistema de gestión de farmacia Salud Vida");
        JPanel accesos = new JPanel(new GridLayout(1, 2, 20, 0)); accesos.setOpaque(false);
        accesos.setPreferredSize(new Dimension(0, 200));
        accesos.add(crearTarjeta("Medicamentos", "Buscar medicamentos y consultar stock", AZUL));
        accesos.add(crearTarjeta("Ventas", "Registrar nuevas ventas", VERDE));
        JPanel centro = new JPanel(new BorderLayout()); centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        centro.add(accesos, BorderLayout.NORTH); inicio.add(centro, BorderLayout.CENTER);
        panelContenido.add(inicio, "Inicio");

        JPanel medicamentos = new JPanel(new BorderLayout());
        vistaMedicamentos = new MedicamentoView(sesion);
        medicamentos.add(vistaMedicamentos, BorderLayout.CENTER);
        panelContenido.add(medicamentos, "Medicamentos");

        JPanel ventas = crearBase("Registro de ventas", "");
        JPanel caja = new JPanel(new BorderLayout()); caja.setOpaque(false);
        caja.add(new VentaView(sesion), BorderLayout.CENTER);
        ventas.add(caja, BorderLayout.CENTER); panelContenido.add(ventas, "Ventas");
    }

    private JPanel crearTarjeta(String nombre, String descripcion, Color color) {
        Color texto = color.equals(VERDE) ? AZUL_OSCURO : Color.WHITE;
        JPanel tarjeta = new ComponentesRedondeados.Panel(new BorderLayout(0, 12), color, 32);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(22, 22, 20, 22));
        tarjeta.add(new JLabel(new ComponentesRedondeados.IconoModulo(nombre, 34)), BorderLayout.NORTH);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 5)); textos.setOpaque(false);
        JLabel titulo = new JLabel(nombre); titulo.setFont(new Font("SansSerif", Font.BOLD, 21));
        titulo.setForeground(texto);
        JLabel detalle = new JLabel("<html>" + descripcion + "</html>");
        detalle.setFont(new Font("SansSerif", Font.PLAIN, 12)); detalle.setForeground(texto);
        textos.add(titulo); textos.add(detalle); tarjeta.add(textos, BorderLayout.CENTER);
        JButton acceder = new ComponentesRedondeados.Boton("Acceder  →"); acceder.setHorizontalAlignment(SwingConstants.LEFT);
        acceder.setBackground(Color.WHITE);
        acceder.setForeground(color.equals(VERDE) ? AZUL_OSCURO : AZUL); acceder.setFont(new Font("SansSerif", Font.BOLD, 13));
        acceder.setContentAreaFilled(false); acceder.setFocusPainted(false); acceder.setBorderPainted(false);
        acceder.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        acceder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        acceder.addActionListener(e -> mostrarModulo(nombre)); tarjeta.add(acceder, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void mostrarModulo(String nombre) {
        if (!sesion.estaActivo() || !sesion.estaEnSesion() || !Rol.ENCARGADO.equals(sesion.getRol())) {
            throw new SecurityException("Se requiere una sesión de encargado.");
        }
        if (!botones.containsKey(nombre)) throw new IllegalArgumentException("Módulo no permitido.");
        if (nombre.equals("Medicamentos")) vistaMedicamentos.recargar();
        pantallaActual = nombre;
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

}

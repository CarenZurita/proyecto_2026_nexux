package view;

import java.awt.*;
import javax.swing.*;
import model.Usuario;

/** Configuración de la cuenta del administrador que ha iniciado sesión. */
public class ConfiguracionView extends JPanel {
    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color GRIS = new Color(0x64748B);
    private static final Color AZUL = new Color(0x2563EB);
    private final Usuario sesion;
    private final JLabel estado = new JLabel(" ");

    public ConfiguracionView(Usuario sesion) {
        super(new BorderLayout(0, 20));
        if (sesion == null || !sesion.estaActivo() || !sesion.estaEnSesion()
                || !sesion.esAdministrador()) {
            throw new SecurityException("Se requiere una sesión de administrador.");
        }
        this.sesion = sesion;
        setBackground(new Color(0xF1F5F9));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 8));
        encabezado.setOpaque(false);
        encabezado.add(etiqueta("Configuración", 28, Font.BOLD, TEXTO));
        encabezado.add(etiqueta("Administra la seguridad de tu cuenta.", 15, Font.PLAIN, GRIS));
        add(encabezado, BorderLayout.NORTH);

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setOpaque(false);
        JPanel cuenta = crearTarjeta();
        cuenta.setAlignmentX(LEFT_ALIGNMENT);
        cuenta.setMaximumSize(new Dimension(460, cuenta.getPreferredSize().height));
        contenido.add(cuenta);
        contenido.add(Box.createVerticalStrut(14));
        JLabel ayuda = etiqueta("Los cambios se conservan al cerrar el sistema.", 11, Font.PLAIN, GRIS);
        ayuda.setAlignmentX(LEFT_ALIGNMENT);
        contenido.add(ayuda);
        contenido.add(Box.createVerticalStrut(10));
        estado.setFont(new Font("SansSerif", Font.PLAIN, 12));
        estado.setForeground(new Color(0x166534));
        estado.setAlignmentX(LEFT_ALIGNMENT);
        contenido.add(estado);
        contenido.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel crearTarjeta() {
        JPanel tarjeta = new ComponentesRedondeados.Panel(new GridBagLayout(),
                Color.WHITE, 22, new Color(0xE2E8F0));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = -1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        agregar(tarjeta, g, etiqueta("Mi cuenta y seguridad", 18, Font.BOLD, TEXTO), 0, 22);

        JPanel datos = new JPanel(new GridBagLayout());
        datos.setOpaque(false);
        GridBagConstraints d = new GridBagConstraints();
        d.anchor = GridBagConstraints.WEST;
        d.insets = new Insets(0, 0, 10, 24);
        datos.add(etiqueta("Usuario", 12, Font.PLAIN, GRIS), d);
        d.gridx = 1;
        d.weightx = 1;
        datos.add(etiqueta(sesion.getNombreUsuario(), 13, Font.PLAIN, TEXTO), d);
        d.gridx = 0;
        d.gridy = 1;
        d.weightx = 0;
        d.insets.bottom = 0;
        datos.add(etiqueta("Rol", 12, Font.PLAIN, GRIS), d);
        d.gridx = 1;
        d.weightx = 1;
        datos.add(etiqueta("Administrador", 13, Font.PLAIN, TEXTO), d);
        agregar(tarjeta, g, datos, 0, 18);
        agregar(tarjeta, g, separador(), 0, 18);
        agregar(tarjeta, g, etiqueta("Contraseña", 14, Font.BOLD, TEXTO), 0, 5);
        agregar(tarjeta, g, etiqueta("Actualiza la contraseña de tu cuenta.", 12, Font.PLAIN, GRIS), 0, 12);
        agregar(tarjeta, g, boton("Cambiar contraseña", false), 0, 18);
        agregar(tarjeta, g, separador(), 0, 18);
        agregar(tarjeta, g, etiqueta("Recuperación de acceso", 14, Font.BOLD, TEXTO), 0, 5);
        agregar(tarjeta, g, etiqueta("Actualiza tus respuestas de seguridad.", 12, Font.PLAIN, GRIS), 0, 12);
        agregar(tarjeta, g, boton("Actualizar preguntas", true), 0, 0);
        return tarjeta;
    }

    private JButton boton(String texto, boolean preguntas) {
        JButton boton = new ComponentesRedondeados.Boton(texto);
        boton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        boton.setBackground(AZUL);
        boton.setForeground(Color.WHITE);
        boton.setPreferredSize(new Dimension(220, 36));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(e -> {
            estado.setText(" ");
            new SeguridadCuentaDialog(SwingUtilities.getWindowAncestor(this), sesion,
                    preguntas, () -> estado.setText(preguntas
                            ? "Respuestas de seguridad actualizadas."
                            : "Contraseña actualizada correctamente.")).setVisible(true);
        });
        return boton;
    }

    private static void agregar(JPanel panel, GridBagConstraints g, JComponent componente,
                                int arriba, int abajo) {
        g.gridy++;
        g.insets = new Insets(arriba, 0, abajo, 0);
        g.fill = componente instanceof JButton ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        panel.add(componente, g);
    }

    private static JSeparator separador() {
        JSeparator linea = new JSeparator();
        linea.setForeground(new Color(0xE2E8F0));
        linea.setBackground(Color.WHITE);
        return linea;
    }

    private static JLabel etiqueta(String texto, int tamano, int estilo, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.putClientProperty("html.disable", Boolean.TRUE);
        etiqueta.setFont(new Font("SansSerif", estilo, tamano));
        etiqueta.setForeground(color);
        return etiqueta;
    }
}

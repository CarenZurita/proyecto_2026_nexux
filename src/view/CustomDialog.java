package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

/** Diálogo modal reutilizable para avisos, confirmaciones, errores y éxitos. */
public final class CustomDialog {
    public enum TipoModal {
        AVISO(360, false, new Color(0xE0F2FE), new Color(0x0284C7),
                new Color(0x2563EB), "i"),
        ADVERTENCIA(420, true, new Color(0xFEF3C7), new Color(0xD97706),
                new Color(0x2563EB), "!"),
        ELIMINACION(420, true, new Color(0xFEE2E2), new Color(0xEF4444),
                new Color(0xEF4444), "!"),
        EXITO(360, false, new Color(0xDCFCE7), new Color(0x16A34A),
                new Color(0x16A34A), "✓"),
        ERROR(360, false, new Color(0xFEE2E2), new Color(0xEF4444),
                new Color(0x2563EB), "!"),
        AVISO_PRECAUCION(360, false, new Color(0xFEF3C7), new Color(0xD97706),
                new Color(0x2563EB), "!"),
        CONFIRMACION(420, true, new Color(0xE0F2FE), new Color(0x0284C7),
                new Color(0x2563EB), "?");

        private final int ancho;
        private final boolean confirmacion;
        private final Color fondoIcono;
        private final Color colorIcono;
        private final Color colorPrimario;
        private final String glifo;

        TipoModal(int ancho, boolean confirmacion, Color fondoIcono,
                  Color colorIcono, Color colorPrimario, String glifo) {
            this.ancho = ancho;
            this.confirmacion = confirmacion;
            this.fondoIcono = fondoIcono;
            this.colorIcono = colorIcono;
            this.colorPrimario = colorPrimario;
            this.glifo = glifo;
        }
    }

    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color SECUNDARIO = new Color(0x4B5563);

    private CustomDialog() { }

    /** Devuelve true solo cuando el usuario pulsa Aceptar en una confirmación. */
    public static boolean mostrar(Component padre, String titulo, String mensaje, TipoModal tipo) {
        if (tipo == null) throw new IllegalArgumentException("El tipo de modal es obligatorio.");
        if (tipo.confirmacion) return mostrarConfirmacion(padre, titulo, mensaje, tipo);
        mostrarAviso(padre, titulo, mensaje, tipo);
        return false;
    }

    public static void mostrarInfo(Component padre, String titulo, String mensaje) {
        mostrarAviso(padre, titulo, mensaje, TipoModal.AVISO);
    }

    public static void mostrarExito(Component padre, String titulo, String mensaje) {
        mostrarAviso(padre, titulo, mensaje, TipoModal.EXITO);
    }

    private static void mostrarAviso(Component padre, String titulo, String mensaje, TipoModal tipo) {
        JDialog dialog = crearDialogo(padre, titulo);
        JPanel contenido = crearContenido(titulo, mensaje, tipo);
        JButton aceptar = boton("Aceptar", tipo.colorPrimario, Color.WHITE);
        aceptar.setPreferredSize(new Dimension(240, 46));
        aceptar.addActionListener(evento -> dialog.dispose());
        JPanel acciones = new JPanel(new GridLayout(1, 1));
        acciones.setOpaque(false);
        acciones.setPreferredSize(new Dimension(260, 46));
        acciones.add(aceptar);
        agregarFila(contenido, acciones, 24, 0, true);
        configurarYMostrar(dialog, contenido, aceptar, padre, tipo.ancho);
    }

    private static boolean mostrarConfirmacion(Component padre, String titulo,
                                               String mensaje, TipoModal tipo) {
        boolean[] confirmado = {false};
        JDialog dialog = crearDialogo(padre, titulo);
        JPanel contenido = crearContenido(titulo, mensaje, tipo);
        JButton cancelar = boton("Cancelar", new Color(0xF3F4F6), TEXTO);
        cancelar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        JButton aceptar = boton("Aceptar", tipo.colorPrimario, Color.WHITE);
        ActionListener cerrar = evento -> dialog.dispose();
        cancelar.addActionListener(cerrar);
        aceptar.addActionListener(evento -> {
            confirmado[0] = true;
            dialog.dispose();
        });
        JPanel acciones = new JPanel(new GridLayout(1, 2, 12, 0));
        acciones.setOpaque(false);
        acciones.setPreferredSize(new Dimension(340, 46));
        acciones.add(cancelar);
        acciones.add(aceptar);
        agregarFila(contenido, acciones, 28, 0, true);
        configurarYMostrar(dialog, contenido, aceptar, padre, tipo.ancho);
        return confirmado[0];
    }

    private static JDialog crearDialogo(Component padre, String titulo) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(padre), titulo,
                JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        return dialog;
    }

    private static JPanel crearContenido(String titulo, String mensaje, TipoModal tipo) {
        JPanel contenido = new JPanel(new java.awt.GridBagLayout());
        contenido.setBackground(Color.WHITE);
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel icono = new JLabel(new Icono(tipo));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel encabezado = new JLabel(escapar(titulo), SwingConstants.CENTER);
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 22));
        encabezado.setForeground(TEXTO);
        encabezado.setAlignmentX(Component.CENTER_ALIGNMENT);
        encabezado.setHorizontalAlignment(SwingConstants.CENTER);

        String textoSeguro = escapar(mensaje == null ? "" : mensaje).replace("\n", "<br>");
        int anchoTexto = tipo.ancho - 100;
        JLabel descripcion = new JLabel("<html><div style='text-align: center; width: "
                + anchoTexto + "px; color: #4B5563; font-family: sans-serif;'>"
                + textoSeguro + "</div></html>", SwingConstants.CENTER);
        descripcion.setHorizontalAlignment(SwingConstants.CENTER);
        descripcion.setAlignmentX(Component.CENTER_ALIGNMENT);
        descripcion.setForeground(SECUNDARIO);
        descripcion.setFont(new Font("SansSerif", Font.PLAIN, 14));
        Dimension descripcionPreferida = descripcion.getPreferredSize();
        descripcion.setPreferredSize(new Dimension(anchoTexto + 12, descripcionPreferida.height));
        descripcion.setMaximumSize(new Dimension(anchoTexto + 12, descripcionPreferida.height));

        agregarFila(contenido, icono, 0, 0, false);
        agregarFila(contenido, encabezado, 16, 0, true);
        agregarFila(contenido, descripcion, 12, 0, true);
        return contenido;
    }

    private static void agregarFila(JPanel panel, JComponent componente, int arriba,
                                    int abajo, boolean llenarAncho) {
        componente.setAlignmentX(Component.CENTER_ALIGNMENT);
        java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = panel.getComponentCount();
        constraints.weightx = 1;
        constraints.fill = llenarAncho
                ? java.awt.GridBagConstraints.HORIZONTAL : java.awt.GridBagConstraints.NONE;
        constraints.anchor = java.awt.GridBagConstraints.CENTER;
        constraints.insets = new Insets(arriba, 0, abajo, 0);
        panel.add(componente, constraints);
    }

    private static JButton boton(String texto, Color fondo, Color frente) {
        ComponentesRedondeados.Boton boton = new ComponentesRedondeados.Boton(texto);
        boton.setArco(8);
        boton.setBackground(fondo);
        boton.setForeground(frente);
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        boton.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        boton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        boton.setAlignmentX(Component.CENTER_ALIGNMENT);
        return boton;
    }

    private static void configurarYMostrar(JDialog dialog, JPanel contenido, JButton predeterminado,
                                           Component padre, int ancho) {
        dialog.setContentPane(contenido);
        dialog.getRootPane().setDefaultButton(predeterminado);
        dialog.getRootPane().registerKeyboardAction(evento -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setSize(ancho, Math.max(dialog.getHeight(), dialog.getPreferredSize().height));
        dialog.setLocationRelativeTo(padre);
        dialog.setVisible(true);
        if (padre instanceof JComponent componente) componente.requestFocusInWindow();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static final class Icono implements Icon {
        private final TipoModal tipo;
        Icono(TipoModal tipo) { this.tipo = tipo; }
        @Override public int getIconWidth() { return 84; }
        @Override public int getIconHeight() { return 84; }
        @Override public void paintIcon(Component componente, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(tipo.fondoIcono);
            p.fillOval(x, y, 84, 84);
            p.setColor(tipo.colorIcono);
            p.setFont(new Font("SansSerif", Font.BOLD, tipo.glifo.equals("✓") ? 42 : 48));
            int ancho = p.getFontMetrics().stringWidth(tipo.glifo);
            p.drawString(tipo.glifo, x + (84 - ancho) / 2, y + 58);
            p.dispose();
        }
    }
}

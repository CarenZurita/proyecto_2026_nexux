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

/**
 * Componente de presentación reutilizable para avisos y confirmaciones;
 * las vistas lo usan para comunicar resultados sin acoplarse al dominio.
 */
public final class CustomDialog {
    /** Variantes visuales disponibles para los diálogos. */
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

    /** Devuelve true solo cuando el usuario confirma la acción. */
    public static boolean mostrar(Component padre, String titulo, String mensaje, TipoModal tipo) {
        if (tipo == null) throw new IllegalArgumentException("El tipo de modal es obligatorio.");
        if (tipo.confirmacion) {
            return mostrarConfirmacion(
                    padre, titulo, mensaje, tipo, textoAccionPredeterminado(titulo));
        }
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
        JPanel contenido = crearContenido(dialog, titulo, mensaje, tipo);
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
                                               String mensaje, TipoModal tipo,
                                               String textoAccion) {
        boolean[] confirmado = {false};
        JDialog dialog = crearDialogo(padre, titulo);
        JPanel contenido = crearContenido(dialog, titulo, mensaje, tipo);
        JButton cancelar = boton("Cancelar", new Color(0xEEF6FD), new Color(0x173A63));
        cancelar.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        JButton aceptar = boton(textoAccion, tipo.colorPrimario, Color.WHITE);
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
        dialog.setUndecorated(true);
        return dialog;
    }

    private static JPanel crearContenido(JDialog dialog, String titulo, String mensaje,
                                         TipoModal tipo) {
        Color borde = tipo == TipoModal.ERROR
                ? new Color(0xFCA5A5) : new Color(0xE5E7EB);
        Color fondo = tipo == TipoModal.ERROR
                ? new Color(0xFFF7F7) : Color.WHITE;
        JPanel contenido = new ComponentesRedondeados.Panel(
                new java.awt.GridBagLayout(), fondo, 18, borde);
        contenido.setBorder(BorderFactory.createEmptyBorder(8, 24, 24, 24));

        JButton cerrar = new JButton("×");
        cerrar.setFont(new Font("SansSerif", Font.PLAIN, 25));
        cerrar.setForeground(new Color(0x64748B));
        cerrar.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        cerrar.setContentAreaFilled(false);
        cerrar.setFocusPainted(false);
        cerrar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        cerrar.addActionListener(evento -> dialog.dispose());
        java.awt.GridBagConstraints cerrarConstraints = new java.awt.GridBagConstraints();
        cerrarConstraints.gridx = 0;
        cerrarConstraints.gridy = 0;
        cerrarConstraints.weightx = 1;
        cerrarConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cerrarConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cerrarConstraints.insets = new Insets(0, 0, 4, 0);
        contenido.add(cerrar, cerrarConstraints);

        JLabel icono = new JLabel(new Icono(tipo));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel encabezado = new JLabel(escapar(titulo), SwingConstants.CENTER);
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 22));
        encabezado.setForeground(tipo == TipoModal.ERROR
                ? new Color(0xDC2626)
                : tipo.confirmacion ? new Color(0x173A63) : TEXTO);
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

    private static String textoAccionPredeterminado(String titulo) {
        String normalizado = titulo == null
                ? "" : titulo.toLowerCase(java.util.Locale.ROOT);
        if (normalizado.contains("elimin")) return "Eliminar";
        if (normalizado.contains("desactiv")) return "Desactivar";
        if (normalizado.contains("activ")) return "Activar";
        if (normalizado.contains("sesión") || normalizado.contains("sesion")) {
            return "Cerrar sesión";
        }
        if (normalizado.contains("vaciar")) return "Vaciar";
        if (normalizado.contains("venta")) return "Confirmar";
        return "Aceptar";
    }

    /** Icono vectorial correspondiente al tipo de diálogo. */
    private static final class Icono implements Icon {
        private final TipoModal tipo;
        Icono(TipoModal tipo) { this.tipo = tipo; }
        private int tamano() { return tipo.confirmacion ? 100 : 84; }
        @Override public int getIconWidth() { return tamano(); }
        @Override public int getIconHeight() { return tamano(); }
        @Override public void paintIcon(Component componente, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (tipo.confirmacion) {
                p.setColor(new Color(0xF97316));
                p.fillOval(x, y, tamano(), tamano());
                java.awt.geom.Path2D triangulo = new java.awt.geom.Path2D.Double();
                triangulo.moveTo(x + 50, y + 22);
                triangulo.lineTo(x + 78, y + 73);
                triangulo.lineTo(x + 22, y + 73);
                triangulo.closePath();
                p.setColor(Color.WHITE);
                p.setStroke(new java.awt.BasicStroke(5, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                p.draw(triangulo);
                p.setStroke(new java.awt.BasicStroke(5, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                p.drawLine(x + 50, y + 39, x + 50, y + 55);
                p.fillOval(x + 47, y + 62, 6, 6);
                p.dispose();
                return;
            }
            p.setColor(tipo.fondoIcono);
            p.fillOval(x, y, tamano(), tamano());
            p.setColor(tipo.colorIcono);
            p.setFont(new Font("SansSerif", Font.BOLD, tipo.glifo.equals("✓") ? 42 : 48));
            int ancho = p.getFontMetrics().stringWidth(tipo.glifo);
            p.drawString(tipo.glifo, x + (tamano() - ancho) / 2, y + 58);
            p.dispose();
        }
    }
}

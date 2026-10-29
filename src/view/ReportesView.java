package view;

import java.awt.*;
import javax.swing.*;

/** Selector de reportes con botones redondeados y sin marco de pestañas. */
public class ReportesView extends JPanel {
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color GRIS = new Color(0xE2E8F0);
    private static final Color TEXTO = new Color(0x1E293B);
    private final JButton vencimientos = boton("Vencimientos");
    private final JButton ventas = boton("Ventas");
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private String seleccionado = "Vencimientos";
    private Runnable alVencimientos = () -> { };
    private Runnable alVentas = () -> { };

    public ReportesView(VencimientoView vistaVencimientos, ReporteVentasView vistaVentas) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botones.setOpaque(false);
        botones.add(vencimientos); botones.add(Box.createHorizontalStrut(12)); botones.add(ventas);
        add(botones, BorderLayout.NORTH);
        contenido.setOpaque(false);
        contenido.add(vistaVencimientos, "Vencimientos"); contenido.add(vistaVentas, "Ventas");
        add(contenido, BorderLayout.CENTER);
        vencimientos.addActionListener(e -> seleccionar("Vencimientos"));
        ventas.addActionListener(e -> seleccionar("Ventas"));
        actualizarBotones();
    }

    public void alSeleccionarVencimientos(Runnable accion) { alVencimientos = accion; }
    public void alSeleccionarVentas(Runnable accion) { alVentas = accion; }

    private void seleccionar(String nombre) {
        if (seleccionado.equals(nombre)) return;
        seleccionado = nombre; tarjetas.show(contenido, nombre); actualizarBotones();
        if (nombre.equals("Ventas")) alVentas.run(); else alVencimientos.run();
    }

    private void actualizarBotones() {
        for (JButton boton : new JButton[]{vencimientos, ventas}) {
            boolean activo = boton.getText().equals(seleccionado);
            boton.setSelected(activo);
            boton.setBackground(activo ? AZUL : GRIS);
            boton.setForeground(activo ? Color.WHITE : TEXTO);
            boton.getAccessibleContext().setAccessibleDescription(activo
                    ? "Reporte seleccionado" : "Mostrar reporte de " + boton.getText().toLowerCase());
        }
    }

    private static JButton boton(String texto) {
        JButton boton = new ComponentesRedondeados.Boton(texto);
        boton.setFont(new Font("SansSerif", Font.BOLD, 16));
        boton.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }
}

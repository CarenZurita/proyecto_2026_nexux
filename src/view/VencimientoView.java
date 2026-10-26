package view;

import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import service.VencimientoService;

/** Panel integrado en Reportes del menú del administrador. */
public class VencimientoView extends JPanel {
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color GRIS = new Color(0x64748B);
    private static final Color ROJO = new Color(0xB91C1C);
    private static final Color AMARILLO = new Color(0x854D0E);
    private static final Color VERDE = new Color(0x166534);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JButton actualizar = new JButton("Actualizar");
    private final JLabel resumen = new JLabel(" ");
    private final JTextArea mensaje = new JTextArea("Abre Reportes para consultar el inventario.");
    private final JPanel aviso = new JPanel(new BorderLayout(12, 0));
    private final JLabel iconoAviso = new JLabel(new AvisoIcon());
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Nombre", "Presentación", "Fecha de vencimiento", "Días restantes"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
        @Override public Class<?> getColumnClass(int columna) {
            return columna == 2 ? LocalDate.class : columna == 3 ? Long.class : String.class;
        }
    };
    private final JTable tabla = new JTable(modelo);

    public VencimientoView() {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        JPanel cabecera = new JPanel(new BorderLayout(12, 0));
        cabecera.setOpaque(false);
        resumen.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resumen.setForeground(GRIS);
        cabecera.add(resumen, BorderLayout.CENTER);
        actualizar.setFont(new Font("SansSerif", Font.BOLD, 13));
        actualizar.setBackground(AZUL);
        actualizar.setForeground(Color.WHITE);
        actualizar.setOpaque(true);
        actualizar.setFocusPainted(false);
        actualizar.setBorderPainted(false);
        actualizar.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        actualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cabecera.add(actualizar, BorderLayout.EAST);
        add(cabecera, BorderLayout.NORTH);

        tabla.setRowHeight(42);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setForeground(TEXTO);
        tabla.setBackground(Color.WHITE);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(0xE2E8F0));
        tabla.setSelectionBackground(new Color(0xDBEAFE));
        tabla.setSelectionForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 42));
        tabla.getTableHeader().setBackground(new Color(0xDBEAFE));
        tabla.getTableHeader().setForeground(new Color(0x1E40AF));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabla.setDefaultRenderer(String.class, new TextoRenderer());
        tabla.setDefaultRenderer(LocalDate.class, new FechaRenderer());
        tabla.setDefaultRenderer(Long.class, new DiasRenderer());
        tabla.getColumnModel().getColumn(0).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(165);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(155);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        add(scroll, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 14));
        pie.setOpaque(false);
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        leyenda.setOpaque(false);
        leyenda.add(etiqueta("Rojo: vencidos y 0–10 días", ROJO));
        leyenda.add(etiqueta("Amarillo: 11–20 días", AMARILLO));
        leyenda.add(etiqueta("Verde: 21 días o más", VERDE));
        pie.add(leyenda, BorderLayout.NORTH);
        aviso.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        aviso.setBackground(new Color(0xEFF6FF));
        mensaje.setEditable(false);
        mensaje.setLineWrap(true);
        mensaje.setWrapStyleWord(true);
        mensaje.setOpaque(false);
        mensaje.setRows(2);
        mensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));
        mensaje.setForeground(new Color(0x1E40AF));
        aviso.add(iconoAviso, BorderLayout.WEST);
        aviso.add(mensaje, BorderLayout.CENTER);
        pie.add(aviso, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    private JLabel etiqueta(String texto, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        etiqueta.setForeground(color);
        return etiqueta;
    }

    public void alActualizar(ActionListener accion) { actualizar.addActionListener(accion); }

    public void setCargando(boolean cargando) {
        actualizar.setEnabled(!cargando);
        if (cargando) {
            modelo.setRowCount(0);
            resumen.setText("Consultando inventario…");
            mostrarAviso("Cargando el reporte de vencimientos…", false);
        }
    }

    public void mostrarDatos(List<VencimientoService.Fila> filas) {
        modelo.setRowCount(0);
        long vencidos = 0;
        long proximos = 0;
        for (VencimientoService.Fila fila : filas) {
            modelo.addRow(new Object[]{fila.nombre(), fila.presentacion(),
                    fila.fechaVencimiento(), fila.diasRestantes()});
            if (fila.diasRestantes() < 0) vencidos++;
            else if (fila.diasRestantes() <= VencimientoService.DIAS_ALERTA) proximos++;
        }
        resumen.setText(filas.size() + " medicamentos · Ordenados por vencimiento");
        if (filas.isEmpty()) {
            mostrarAviso("No hay medicamentos registrados para generar el reporte.", false);
        } else if (vencidos == 0 && proximos == 0) {
            mostrarAviso("No existen medicamentos próximos a vencer ni vencidos. "
                    + "Se revisan los próximos " + VencimientoService.DIAS_ALERTA + " días.", false);
        } else {
            mostrarAviso("Medicamentos vencidos: " + vencidos
                    + ". Próximos a vencer (0–" + VencimientoService.DIAS_ALERTA
                    + " días): " + proximos + ".", true);
        }
    }

    public void mostrarError(String texto) {
        modelo.setRowCount(0);
        resumen.setText("Reporte no disponible");
        mostrarAviso(texto, true);
    }

    private void mostrarAviso(String texto, boolean alerta) {
        mensaje.setText(texto);
        mensaje.setCaretPosition(0);
        mensaje.setForeground(alerta ? AMARILLO : new Color(0x1E40AF));
        aviso.setBackground(alerta ? new Color(0xFFFBEB) : new Color(0xEFF6FF));
        iconoAviso.setVisible(!alerta);
    }

    private static class TextoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean seleccion, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(t, valor, seleccion, foco, fila, columna);
            // Evita que los nombres del archivo se interpreten como HTML.
            putClientProperty("html.disable", Boolean.TRUE);
            setText(valor == null ? "" : valor.toString());
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setBackground(seleccion ? t.getSelectionBackground()
                    : fila % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            setForeground(TEXTO);
            return this;
        }
    }

    private static class FechaRenderer extends TextoRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean seleccion, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(t, valor, seleccion, foco, fila, columna);
            setHorizontalAlignment(CENTER);
            setText(valor instanceof LocalDate fecha ? FECHA.format(fecha) : "");
            return this;
        }
    }

    private static class DiasRenderer extends DefaultTableCellRenderer {
        private Color fondo;
        DiasRenderer() { setHorizontalAlignment(CENTER); }
        @Override public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean seleccion, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(t, valor, seleccion, foco, fila, columna);
            long dias = ((Number) valor).longValue();
            setText(dias < 0 ? "Vencido (" + -dias + " d)" : dias == 0 ? "Vence hoy" : Long.toString(dias));
            setToolTipText(dias < 0 ? "Venció hace " + -dias + " días" : "Días restantes: " + dias);
            setForeground(dias <= VencimientoService.LIMITE_ROJO ? ROJO
                    : dias <= VencimientoService.LIMITE_AMARILLO ? AMARILLO : VERDE);
            fondo = dias <= VencimientoService.LIMITE_ROJO ? new Color(0xFEE2E2)
                    : dias <= VencimientoService.LIMITE_AMARILLO ? new Color(0xFEF3C7) : new Color(0xDCFCE7);
            setBackground(seleccion ? t.getSelectionBackground()
                    : fila % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            setFont(t.getFont().deriveFont(Font.BOLD));
            setBorder(BorderFactory.createEmptyBorder());
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setColor(getBackground());
            p.fillRect(0, 0, getWidth(), getHeight());
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(fondo);
            p.fillRoundRect(8, 6, Math.max(0, getWidth() - 16), getHeight() - 12, 10, 10);
            p.dispose();
            setOpaque(false);
            super.paintComponent(g);
        }
    }

    private static class AvisoIcon implements Icon {
        public int getIconWidth() { return 34; }
        public int getIconHeight() { return 34; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(AZUL);
            p.fillOval(x + 1, y + 1, 32, 32);
            p.setColor(Color.WHITE);
            p.setFont(new Font("SansSerif", Font.BOLD, 23));
            p.drawString("i", x + 14, y + 25);
            p.dispose();
        }
    }
}

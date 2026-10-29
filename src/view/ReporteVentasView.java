package view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import service.ReporteVentasService;
import service.ReporteVentasService.Fila;

/** Reporte integrado en el menú existente, sin modificar el inventario. */
public class ReporteVentasView extends JPanel {
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color GRIS = new Color(0x64748B);
    private static final DateTimeFormatter FECHA = DateTimeFormatter
            .ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final JTextField desde = new ComponentesRedondeados.Campo(10);
    private final JTextField hasta = new ComponentesRedondeados.Campo(10);
    private final JButton consultar = boton("Consultar", AZUL, Color.WHITE);
    private final JButton limpiar = boton("Limpiar filtros", new Color(0xE2E8F0), TEXTO);
    private final JLabel resumen = etiqueta("Abre el reporte para consultar las ventas.", 13, false);
    private final JLabel unidades = etiqueta("Total de unidades: —", 13, false);
    private final JLabel total = etiqueta("Total vendido: —", 16, true);
    private final JTextArea aviso = new JTextArea(2, 20);
    private final DefaultTableModel modelo = modelo(
            new String[]{"Fecha", "Usuario", "Unidades", "Total (Bs)", "Acciones"},
            new Class<?>[]{LocalDate.class, String.class, Long.class, BigDecimal.class, Object.class}, true);
    private final JTable tabla = new JTable(modelo);
    private List<Fila> filas = List.of();
    private Runnable alConsultar = () -> { };
    private Consumer<Fila> alDetalle = fila -> { };

    public ReporteVentasView() {
        super(new BorderLayout(0, 14));
        setOpaque(true);
        setBackground(new Color(0xF1F5F9));
        setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        JPanel cabecera = new JPanel(new BorderLayout(0, 10));
        cabecera.setOpaque(false);
        cabecera.add(etiqueta("Reporte de ventas", 20, true), BorderLayout.NORTH);
        cabecera.add(crearFiltros(), BorderLayout.CENTER);
        resumen.setForeground(GRIS);
        cabecera.add(resumen, BorderLayout.SOUTH);
        add(cabecera, BorderLayout.NORTH);

        prepararTabla(tabla);
        int[] anchos = {120, 135, 80, 115, 115};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(4).setMinWidth(105);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new AccionRenderer());
        tabla.getColumnModel().getColumn(4).setCellEditor(new AccionEditor());
        add(scroll(tabla), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 10));
        pie.setOpaque(false);
        JPanel totales = new JPanel(new BorderLayout(10, 0));
        totales.setOpaque(false);
        totales.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        totales.add(unidades, BorderLayout.WEST);
        totales.add(total, BorderLayout.EAST);
        pie.add(totales, BorderLayout.NORTH);
        aviso.setEditable(false);
        aviso.setOpaque(false);
        aviso.setLineWrap(true);
        aviso.setWrapStyleWord(true);
        aviso.setFont(new Font("SansSerif", Font.PLAIN, 12));
        aviso.setForeground(GRIS);
        pie.add(aviso, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);

        consultar.addActionListener(e -> alConsultar.run());
        desde.addActionListener(e -> alConsultar.run());
        hasta.addActionListener(e -> alConsultar.run());
        limpiar.addActionListener(e -> {
            desde.setText("");
            hasta.setText("");
            alConsultar.run();
        });
    }

    private JPanel crearFiltros() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 10);
        c.gridy = 0;
        c.gridx = 0;
        panel.add(etiqueta("Desde", 13, true), c);
        c.gridx = 1;
        panel.add(etiqueta("Hasta", 13, true), c);
        c.gridy = 1;
        for (int i = 0; i < 2; i++) {
            JTextField campo = i == 0 ? desde : hasta;
            campo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            campo.setMargin(new Insets(8, 8, 8, 8));
            campo.setPreferredSize(new Dimension(110, 36));
            campo.setMinimumSize(new Dimension(95, 36));
            campo.setToolTipText("dd/MM/aaaa. Vacío: sin límite de fecha.");
            c.gridx = i;
            c.weightx = 0.5;
            panel.add(campo, c);
        }
        c.weightx = 0;
        c.gridx = 2;
        panel.add(consultar, c);
        c.gridx = 3;
        c.insets = new Insets(0, 0, 6, 0);
        panel.add(limpiar, c);
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 4;
        c.insets = new Insets(0, 0, 0, 0);
        JLabel formato = etiqueta("Formato: dd/MM/aaaa · Deja las fechas vacías para ver todas las ventas.", 11, false);
        formato.setForeground(GRIS);
        panel.add(formato, c);
        return panel;
    }

    public void alConsultar(Runnable accion) { alConsultar = accion; }
    public void alVerDetalle(Consumer<Fila> accion) { alDetalle = accion; }
    public LocalDate getDesde() { return leerFecha(desde, "Desde"); }
    public LocalDate getHasta() { return leerFecha(hasta, "Hasta"); }

    private LocalDate leerFecha(JTextField campo, String nombre) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) return null;
        try {
            return LocalDate.parse(texto, FECHA);
        } catch (DateTimeParseException e) {
            campo.requestFocusInWindow();
            throw new IllegalArgumentException("La fecha " + nombre
                    + " no es válida. Usa dd/MM/aaaa (por ejemplo, 28/10/2026).");
        }
    }

    public void setCargando(boolean valor) {
        if (tabla.isEditing()) tabla.getCellEditor().cancelCellEditing();
        consultar.setEnabled(!valor);
        limpiar.setEnabled(!valor);
        desde.setEnabled(!valor);
        hasta.setEnabled(!valor);
        tabla.setEnabled(!valor);
        if (valor) {
            vaciar();
            resumen.setText("Consultando ventas…");
            aviso.setForeground(GRIS);
            aviso.setText("");
        }
    }

    public void mostrarDatos(ReporteVentasService.Resultado datos) {
        vaciar();
        filas = datos.filas();
        for (Fila fila : filas) {
            modelo.addRow(new Object[]{fila.fecha(), fila.usuario(), fila.unidades(), fila.total(), "Ver detalle"});
        }
        resumen.setText(filas.size() + (filas.size() == 1 ? " venta encontrada" : " ventas encontradas"));
        unidades.setText("Total de unidades: " + datos.unidades());
        total.setText(datos.ventasSinImporte() > 0 && datos.ventasSinImporte() == filas.size()
                ? "Total vendido: Sin registro"
                : (datos.ventasSinImporte() > 0 ? "Total conocido: Bs " : "Total vendido: Bs ")
                        + importe(datos.total()));
        String mensaje = filas.isEmpty() ? "No hay ventas para las fechas indicadas." : "";
        if (datos.ventasSinImporte() > 0) {
            mensaje += datos.ventasSinImporte() + " venta(s) antigua(s) sin precios guardados; no se incluyen en el importe total. ";
        }
        if (datos.sinFechaExcluidas() > 0) {
            mensaje += " " + datos.sinFechaExcluidas() + " venta(s) sin fecha: usa Limpiar filtros para verlas.";
        }
        aviso.setForeground(GRIS);
        aviso.setText(mensaje.trim());
        aviso.setCaretPosition(0);
    }

    public void mostrarError(String mensaje) {
        vaciar();
        resumen.setText("Reporte no disponible");
        aviso.setForeground(new Color(0xB91C1C));
        aviso.setText(mensaje);
        aviso.setCaretPosition(0);
    }

    private void vaciar() {
        if (tabla.isEditing()) tabla.getCellEditor().cancelCellEditing();
        filas = List.of();
        modelo.setRowCount(0);
        unidades.setText("Total de unidades: —");
        total.setText("Total vendido: —");
    }

    public void mostrarDetalle(Fila fila) {
        Window ventana = SwingUtilities.getWindowAncestor(this);
        JDialog dialogo = new JDialog(ventana, "Detalle de venta", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel contenido = new JPanel(new BorderLayout(0, 18));
        contenido.setBackground(new Color(0xF1F5F9));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel cabecera = new JPanel(new GridLayout(3, 1, 0, 6));
        cabecera.setOpaque(false);
        cabecera.add(etiqueta("Detalle de venta", 22, true));
        cabecera.add(etiqueta("Fecha: " + (fila.fecha() == null ? "Sin registro" : FECHA.format(fila.fecha())), 13, false));
        cabecera.add(etiqueta("Usuario: " + fila.usuario(), 13, false));
        contenido.add(cabecera, BorderLayout.NORTH);

        DefaultTableModel detalle = modelo(
                new String[]{"Código", "Medicamento", "Cantidad", "Precio (Bs)", "Subtotal (Bs)"},
                new Class<?>[]{String.class, String.class, Integer.class, BigDecimal.class, BigDecimal.class}, false);
        for (ReporteVentasService.Detalle linea : fila.detalles()) {
            detalle.addRow(new Object[]{linea.codigo(), linea.medicamento(), linea.cantidad(),
                    linea.precioUnitario(), linea.subtotal()});
        }
        JTable tablaDetalle = new JTable(detalle);
        prepararTabla(tablaDetalle);
        tablaDetalle.getColumnModel().getColumn(1).setPreferredWidth(190);
        contenido.add(scroll(tablaDetalle), BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.setOpaque(false);
        pie.add(etiqueta(fila.total() == null ? "Importe: Sin registro"
                : "Total de la venta: Bs " + importe(fila.total()), 17, true), BorderLayout.NORTH);
        if (fila.total() == null) {
            pie.add(etiqueta("Esta venta antigua no guardó usuario ni precios.", 12, false), BorderLayout.CENTER);
        }
        JButton cerrar = boton("Cerrar", AZUL, Color.WHITE);
        cerrar.addActionListener(e -> dialogo.dispose());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        acciones.add(cerrar);
        pie.add(acciones, BorderLayout.SOUTH);
        contenido.add(pie, BorderLayout.SOUTH);
        dialogo.setContentPane(contenido);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.setSize(760, 460);
        dialogo.setMinimumSize(new Dimension(620, 350));
        dialogo.setLocationRelativeTo(this);
        dialogo.getRootPane().setDefaultButton(cerrar);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.setVisible(true);
    }

    private static JLabel etiqueta(String texto, int tamano, boolean negrita) {
        JLabel label = new JLabel();
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setText(texto);
        label.setForeground(TEXTO);
        label.setFont(new Font("SansSerif", negrita ? Font.BOLD : Font.PLAIN, tamano));
        return label;
    }

    private static JButton boton(String texto, Color fondo, Color colorTexto) {
        JButton boton = new ComponentesRedondeados.Boton(texto);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setBackground(fondo);
        boton.setForeground(colorTexto);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private static String importe(BigDecimal valor) {
        if (valor == null) return "Sin registro";
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "BO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }

    private static DefaultTableModel modelo(String[] columnas, Class<?>[] clases, boolean acciones) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return acciones && columna == 4; }
            @Override public Class<?> getColumnClass(int columna) { return clases[columna]; }
        };
    }

    private static JScrollPane scroll(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setColumnHeaderView(tabla.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private static void prepararTabla(JTable tabla) {
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setForeground(TEXTO);
        tabla.setBackground(Color.WHITE);
        tabla.setRowHeight(44);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(0xE2E8F0));
        tabla.setSelectionBackground(new Color(0xDBEAFE));
        tabla.setSelectionForeground(TEXTO);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 42));
        tabla.getTableHeader().setBackground(new Color(0xF8FAFC));
        tabla.getTableHeader().setForeground(TEXTO);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        for (Class<?> clase : new Class<?>[]{Object.class, String.class, LocalDate.class,
                Integer.class, Long.class, BigDecimal.class}) {
            tabla.setDefaultRenderer(clase, new TextoRenderer());
        }
    }

    private static class TextoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccion, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            putClientProperty("html.disable", Boolean.TRUE);
            setText(valor == null ? "Sin registro" : valor instanceof LocalDate fecha
                    ? FECHA.format(fecha) : valor instanceof BigDecimal monto ? importe(monto) : valor.toString());
            setHorizontalAlignment(valor instanceof Number ? RIGHT : LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setBackground(seleccion ? tabla.getSelectionBackground()
                    : fila % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            setForeground(TEXTO);
            return this;
        }
    }

    private static class AccionRenderer implements javax.swing.table.TableCellRenderer {
        private final JButton boton = boton("Ver detalle", new Color(0xF8FAFC), AZUL);
        @Override public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccion, boolean foco, int fila, int columna) {
            boton.setEnabled(tabla.isEnabled());
            return boton;
        }
    }

    private class AccionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JButton boton = boton("Ver detalle", new Color(0xF8FAFC), AZUL);
        private Fila fila;
        AccionEditor() {
            boton.addActionListener((ActionEvent e) -> {
                Fila seleccionada = fila;
                fireEditingStopped();
                if (seleccionada != null) alDetalle.accept(seleccionada);
            });
        }
        @Override public Component getTableCellEditorComponent(JTable tabla, Object valor,
                boolean seleccion, int filaVista, int columna) {
            fila = filas.get(tabla.convertRowIndexToModel(filaVista));
            return boton;
        }
        @Override public Object getCellEditorValue() { return "Ver detalle"; }
    }
}

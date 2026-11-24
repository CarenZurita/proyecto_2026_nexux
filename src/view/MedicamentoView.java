package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import model.Medicamento;
import model.Rol;
import model.Usuario;
import service.DatosMedicamento;
import controller.MedicamentoController;

/**
 * Vista MVC del inventario: presenta búsqueda, stock y acciones de administración;
 * delega las operaciones al controlador y no accede directamente al repositorio.
 */
public class MedicamentoView extends JPanel {
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color AZUL_CLARO = new Color(0xDBEAFE);
    private static final Color OSCURO = new Color(0x1E293B);
    private static final Color FONDO = new Color(0xF1F5F9);
    private static final Color TEXTO_SECUNDARIO = new Color(0x64748B);
    private static final Color BORDE = EstiloSistema.BORDE;
    private static final Color BLUE = new Color(0x2563EB);
    private static final Color RED_ACCION = new Color(0xF43737);

    private final Usuario sesion;
    private final boolean puedeGestionar;
    private final MedicamentoController medicamentoController;
    private final JTextField busqueda = new CampoBusqueda("Buscar por nombre o código...");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Código", "Nombre", "Presentación", "Stock", "Precio (Bs)", "Vencimiento"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return puedeGestionar && columna == getColumnCount() - 1;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private List<Medicamento> inventario = new ArrayList<>();
    private JLabel registroVisible;
    private long solicitudInventario;
    private SwingWorker<List<Medicamento>, Void> cargaInventario;

    public MedicamentoView(Usuario sesion, MedicamentoController medicamentoController) {
        if (sesion == null || !sesion.estaActivo() || !sesion.estaEnSesion()
                || (!Rol.ADMINISTRADOR.equals(sesion.getRol())
                && !Rol.ENCARGADO.equals(sesion.getRol()))) {
            throw new SecurityException("Se requiere una sesión válida para consultar medicamentos.");
        }
        this.sesion = sesion;
        this.medicamentoController = medicamentoController;
        this.puedeGestionar = Rol.ADMINISTRADOR.equals(sesion.getRol());
        construirInterfaz();
        cargarInventario();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(0, 22));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel encabezado = EstiloSistema.encabezado("medicamentos",
                "Gestión de medicamentos",
                "Consulta y administra el inventario de la farmacia");
        add(encabezado, BorderLayout.NORTH);

        RoundedPanel contenido = new RoundedPanel(18, Color.WHITE, BORDE);
        contenido.setLayout(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 18));

        JPanel herramientas = new JPanel(new BorderLayout(12, 0));
        herramientas.setOpaque(false);
        busqueda.setToolTipText("Buscar por nombre, código, marca o precio exacto");
        busqueda.setBorder(new RoundedLineBorder(BORDE, 12, 1, 12, 14));
        busqueda.setPreferredSize(new java.awt.Dimension(300, 44));
        herramientas.add(busqueda, BorderLayout.CENTER);

        if (puedeGestionar) {
            JButton nuevo = crearBoton("Registrar medicamento", AZUL);
            nuevo.setIcon(new AccionIcono(AccionIcono.Tipo.AGREGAR, Color.WHITE, 18));
            nuevo.setIconTextGap(9);
            nuevo.addActionListener(e -> abrirFormulario(null));
            herramientas.add(nuevo, BorderLayout.EAST);
        }
        contenido.add(herramientas, BorderLayout.NORTH);

        EstiloSistema.aplicarTabla(tabla);
        tabla.setRowHeight(48);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(0xE6EDF5));
        tabla.setIntercellSpacing(new java.awt.Dimension(1, 1));
        tabla.setForeground(OSCURO);
        tabla.setBackground(Color.WHITE);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 42));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(EstiloSistema.ENCABEZADO_TABLA);
        tabla.getTableHeader().setForeground(new Color(0x1E40AF));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE));
        tabla.setSelectionBackground(new Color(0xDBEAFE));
        tabla.setSelectionForeground(OSCURO);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new StockRenderer());
        tabla.setDefaultRenderer(Object.class, new CeldaInventarioRenderer());
        tabla.getColumnModel().getColumn(3).setCellRenderer(new StockRenderer());
        if (puedeGestionar) {
            modelo.addColumn("Acciones");
            tabla.getColumnModel().getColumn(6).setCellRenderer(new AccionesRenderer());
            tabla.getColumnModel().getColumn(6).setCellEditor(
                    new AccionesEditor(this::editarPorCodigo, this::eliminarPorCodigo));
            tabla.getColumnModel().getColumn(6).setPreferredWidth(112);
        }
        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(new RoundedLineBorder(BORDE, 12, 1, 0, 0));
        desplazamiento.getViewport().setBackground(Color.WHITE);
        desplazamiento.getViewport().setOpaque(true);
        desplazamiento.setBackground(Color.WHITE);
        contenido.add(desplazamiento, BorderLayout.CENTER);

        JLabel cantidadRegistros = new JLabel();
        cantidadRegistros.setForeground(TEXTO_SECUNDARIO);
        cantidadRegistros.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(cantidadRegistros, BorderLayout.WEST);
        JLabel permiso = new JLabel(puedeGestionar
                ? "Inventario administrado por " + sesion.getNombreCompleto()
                : "Consulta de inventario");
        permiso.setForeground(TEXTO_SECUNDARIO);
        permiso.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pie.add(permiso, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);
        add(contenido, BorderLayout.CENTER);

        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cargarInventario();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cargarInventario();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cargarInventario();
            }
        });
        registroVisible = cantidadRegistros;
        EstiloSistema.aplicarCampos(this);
    }

    private JButton crearBoton(String texto, Color color) {
        return new BotonRedondeado(texto, color, Color.WHITE);
    }

    private void cargarInventario() {
        String consulta = busqueda.getText().trim();
        long solicitud = ++solicitudInventario;
        if (cargaInventario != null) {
            cargaInventario.cancel(true);
        }
        // La lectura del inventario ocurre fuera del EDT; done() actualiza componentes Swing.
        cargaInventario = new SwingWorker<>() {
            @Override
            protected List<Medicamento> doInBackground() throws Exception {
                return consulta.isEmpty()
                        ? medicamentoController.listar(sesion)
                        : medicamentoController.buscar(sesion, consulta);
            }

            @Override
            protected void done() {
                if (solicitud != solicitudInventario) {
                    return;
                }
                try {
                    inventario = get();
                    mostrarInventario();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La carga del inventario fue interrumpida.");
                } catch (ExecutionException e) {
                    mostrarError(mensajeError(e));
                }
            }
        };
        cargaInventario.execute();
    }

    public void recargar() {
        cargarInventario();
    }

    private void mostrarInventario() {
        modelo.setRowCount(0);
        for (Medicamento medicamento : inventario) {
            modelo.addRow(new Object[]{
                    medicamento.getCodigo(),
                    medicamento.getNombre(),
                    medicamento.getPresentacion(),
                    medicamento.getCantidadDisponible(),
                    String.format(Locale.US, "Bs %.2f", medicamento.getPrecio()),
                    medicamento.getFechaVencimientoDate().format(DateTimeFormatter.ofPattern("MM/yyyy"))
            });
        }
        if (registroVisible != null) {
            int visibles = modelo.getRowCount();
            registroVisible.setText(visibles + (visibles == 1
                    ? " medicamento registrado" : " medicamentos registrados"));
        }
    }

    private void editarPorCodigo(String codigo) {
        Medicamento medicamento = buscarEnInventario(codigo);
        if (medicamento != null) {
            abrirFormulario(medicamento);
        }
    }

    private Medicamento buscarEnInventario(String codigo) {
        return inventario.stream()
                .filter(medicamento -> medicamento.getCodigo().equalsIgnoreCase(codigo))
                .findFirst()
                .orElse(null);
    }

    private void abrirFormulario(Medicamento existente) {
        try {
            validarSesionAdministradora();
        } catch (SecurityException e) {
            mostrarError(e.getMessage());
            return;
        }
        MedicamentoFormDialog dialogo = new MedicamentoFormDialog(this, existente);
        dialogo.alGuardar(datos -> guardarMedicamento(dialogo, existente, datos));
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    private void guardarMedicamento(MedicamentoFormDialog dialogo, Medicamento existente,
                                    DatosMedicamento datos) {
        try {
            validarSesionAdministradora();
        } catch (SecurityException e) {
            dialogo.mostrarError(e.getMessage());
            return;
        }
        dialogo.setProcesando(true);
        // El guardado del formulario realiza I/O fuera del EDT y reporta el resultado en done().
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (existente == null) {
                    medicamentoController.registrar(sesion, datos);
                } else {
                    medicamentoController.actualizar(sesion, existente.getCodigo(), datos);
                }
                return null;
            }

            @Override
            protected void done() {
                dialogo.setProcesando(false);
                try {
                    get();
                    dialogo.dispose();
                    cargarInventario();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dialogo.mostrarError("La operación fue interrumpida. Vuelve a intentarlo.");
                } catch (ExecutionException e) {
                    dialogo.mostrarError(mensajeError(e));
                }
            }
        }.execute();
    }

    private void eliminarPorCodigo(String codigo) {
        try {
            validarSesionAdministradora();
        } catch (SecurityException e) {
            mostrarError(e.getMessage());
            return;
        }
        Medicamento medicamento = buscarEnInventario(codigo);
        if (medicamento == null) {
            mostrarError("El medicamento seleccionado ya no está en el inventario.");
            return;
        }
        if (!confirmarEliminacion(medicamento)) {
            return;
        }
        // La eliminación escribe el archivo fuera del EDT para mantener receptiva la interfaz.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                medicamentoController.eliminar(sesion, medicamento.getCodigo());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cargarInventario();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La operación fue interrumpida. Vuelve a intentarlo.");
                } catch (ExecutionException e) {
                    mostrarError(mensajeError(e));
                }
            }
        }.execute();
    }

    private boolean confirmarEliminacion(Medicamento medicamento) {
        String nombre = medicamento.getNombre();
        String mensaje = "¿Está seguro de eliminar el medicamento \"" + nombre + "\" del inventario?";
        return LoginView.mostrarConfirmacion(this, "Confirmar eliminación", mensaje,
                LoginView.TonoDialogo.DESTRUCTIVO);
    }

    private void validarSesionAdministradora() {
        if (!sesion.estaActivo() || !sesion.estaEnSesion()
                || !Rol.ADMINISTRADOR.equals(sesion.getRol())) {
            throw new SecurityException("Se requiere una sesión activa de administrador.");
        }
    }

    private void mostrarError(String mensaje) {
        mostrarAlertaError(this, mensaje);
    }

    private void mostrarAlertaError(Component padre, String mensaje) {
        LoginView.mostrarAlertaError(padre, "Error", mensaje);
    }

    private static String mensajeError(ExecutionException excepcion) {
        Throwable causa = excepcion.getCause();
        if (causa == null) {
            return excepcion.toString();
        }
        return causa.getMessage() == null ? causa.toString() : causa.getMessage();
    }

    /** Campo de búsqueda con presentación y foco propios de la vista. */
    private static class CampoBusqueda extends JTextField {
        private final String textoAyuda;

        CampoBusqueda(String textoAyuda) {
            this.textoAyuda = textoAyuda;
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D fondo = (Graphics2D) g.create();
            fondo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            fondo.setColor(Color.WHITE);
            fondo.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            fondo.setColor(hasFocus() ? AZUL : BORDE);
            fondo.setStroke(new java.awt.BasicStroke(hasFocus() ? 1.6f : 1f));
            fondo.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            fondo.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setColor(TEXTO_SECUNDARIO);
                graphics.setFont(getFont());
                Insets insets = getInsets();
                graphics.drawString(textoAyuda, insets.left + 3,
                        (getHeight() + graphics.getFontMetrics().getAscent()
                                - graphics.getFontMetrics().getDescent()) / 2);
                graphics.dispose();
            }
        }
    }

    /** Renderiza el nivel de stock como indicador visual en la tabla. */
    private static class StockRenderer extends JLabel implements TableCellRenderer {
        StockRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(new Color(0x047857));
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                                                       boolean seleccionado, boolean enfocado, int fila, int columna) {
            setText(String.valueOf(valor));
            setBackground(seleccionado ? new Color(0xDBEAFE) : Color.WHITE);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            int alto = 24;
            int ancho = Math.min(getWidth() - 12,
                    Math.max(42, getFontMetrics(getFont()).stringWidth(getText()) + 22));
            int x = (getWidth() - ancho) / 2;
            int y = (getHeight() - alto) / 2;
            graphics.setColor(new Color(0xD1FAE5));
            graphics.fillRoundRect(x, y, ancho, alto, 20, 20);
            graphics.dispose();
            super.paintComponent(g);
        }
    }

    /** Aplica estilos a las celdas ordinarias del inventario. */
    private static class CeldaInventarioRenderer extends DefaultTableCellRenderer {
        CeldaInventarioRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
            setFont(new Font("SansSerif", Font.PLAIN, 13));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                                                       boolean seleccionado, boolean enfocado, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionado, enfocado, fila, columna);
            if (!seleccionado) {
                setBackground(fila % 2 == 0 ? Color.WHITE : new Color(0xFBFDFF));
            }
            setForeground(OSCURO);
            if (columna == 0) {
                setFont(getFont().deriveFont(Font.BOLD));
                setForeground(new Color(0x334E78));
            }
            return this;
        }
    }

    /** Presenta los botones de edición y eliminación en la tabla. */
    private class AccionesRenderer extends JPanel implements TableCellRenderer {
        AccionesRenderer() {
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.CENTER, 8, 9));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            removeAll();
            JButton edit = new JButton("Editar");
            edit.setFont(new Font("SansSerif", Font.BOLD, 11));
            edit.setForeground(Color.WHITE);
            edit.setBackground(BLUE);
            edit.setFocusPainted(false);
            edit.setBorderPainted(false);
            edit.setContentAreaFilled(false);
            edit.setOpaque(false);
            edit.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            edit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            edit.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(c.getBackground());
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                    g2.dispose();
                    super.paint(g, c);
                }
            });
            JButton del = new JButton("Eliminar");
            del.setFont(new Font("SansSerif", Font.BOLD, 11));
            del.setForeground(Color.WHITE);
            del.setBackground(RED_ACCION);
            del.setFocusPainted(false);
            del.setBorderPainted(false);
            del.setContentAreaFilled(false);
            del.setOpaque(false);
            del.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            del.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(c.getBackground());
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                    g2.dispose();
                    super.paint(g, c);
                }
            });
            add(edit);
            add(del);
            setBackground(selected ? t.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }

    /** Gestiona las acciones de fila sin trasladar reglas de negocio a la tabla. */
    private class AccionesEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 9));
        private final Consumer<String> editarAccion;
        private final Consumer<String> eliminarAccion;
        private String codigo;

        AccionesEditor(Consumer<String> editarAccion, Consumer<String> eliminarAccion) {
            this.editarAccion = editarAccion;
            this.eliminarAccion = eliminarAccion;
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            JButton editar = new JButton("Editar");
            styleAccion(editar, BLUE);
            JButton eliminar = new JButton("Eliminar");
            styleAccion(eliminar, RED_ACCION);
            editar.addActionListener(e -> ejecutar(editarAccion));
            eliminar.addActionListener(e -> ejecutar(eliminarAccion));
            panel.add(editar);
            panel.add(eliminar);
        }

        private void styleAccion(JButton b, Color color) {
            b.setFont(new Font("SansSerif", Font.BOLD, 11));
            b.setForeground(Color.WHITE);
            b.setBackground(color);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setOpaque(false);
            b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(c.getBackground());
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                    g2.dispose();
                    super.paint(g, c);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable tabla, Object valor,
                                                     boolean seleccionado, int fila, int columna) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            codigo = String.valueOf(tabla.getModel().getValueAt(filaModelo, 0));
            panel.setBackground(Color.WHITE);
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        private void ejecutar(Consumer<String> accion) {
            String codigoSeleccionado = codigo;
            fireEditingStopped();
            accion.accept(codigoSeleccionado);
        }
    }

    /** Botón de acción con bordes redondeados para esta vista. */
    private static class BotonRedondeado extends JButton {
        private final Color colorBase;
        private final Color colorTexto;

        BotonRedondeado(String texto, Color colorBase, Color colorTexto) {
            super(texto);
            this.colorBase = colorBase;
            this.colorTexto = colorTexto;
            setForeground(colorTexto);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = colorBase;
            if (getModel().isPressed()) {
                color = colorBase.darker();
            } else if (getModel().isRollover()) {
                color = colorBase.brighter();
            }
            graphics.setColor(color);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            graphics.dispose();
            setForeground(colorTexto);
            super.paintComponent(g);
        }
    }

    /** Panel con esquinas y borde personalizados para la composición visual. */
    private static class RoundedPanel extends JPanel {
        private final int radio;
        private final Color fondo;
        private final Color borde;

        RoundedPanel(int radio, Color fondo, Color borde) {
            this.radio = radio;
            this.fondo = fondo;
            this.borde = borde;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(fondo);
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            graphics.setColor(borde);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            graphics.dispose();
            super.paintComponent(g);
        }
    }

    /** Borde redondeado usado por paneles y controles del inventario. */
    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radio;
        private final int grosor;
        private final Insets insets;

        RoundedLineBorder(Color color, int radio, int grosor, int vertical, int horizontal) {
            this.color = color;
            this.radio = radio;
            this.grosor = grosor;
            this.insets = new Insets(vertical, horizontal, vertical, horizontal);
        }

        @Override
        public Insets getBorderInsets(Component componente) {
            return (Insets) insets.clone();
        }

        @Override
        public Insets getBorderInsets(Component componente, Insets destino) {
            destino.set(insets.top, insets.left, insets.bottom, insets.right);
            return destino;
        }

        @Override
        public void paintBorder(Component componente, Graphics g, int x, int y, int ancho, int alto) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(color);
            graphics.setStroke(new java.awt.BasicStroke(grosor));
            graphics.drawRoundRect(x, y, ancho - 1, alto - 1, radio, radio);
            graphics.dispose();
        }
    }

    /** Iconos de las acciones de fila y del botón de alta. */
    private static class AccionIcono implements Icon {
        /** Acciones posibles que se representan con el icono. */
        private enum Tipo {
            AGREGAR, EDITAR, ELIMINAR
        }

        private final Tipo tipo;
        private final Color color;
        private final int tamano;

        AccionIcono(Tipo tipo, Color color, int tamano) {
            this.tipo = tipo;
            this.color = color;
            this.tamano = tamano;
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }

        @Override
        public void paintIcon(Component componente, Graphics g, int x, int y) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.translate(x, y);
            graphics.scale(tamano / 20.0, tamano / 20.0);
            graphics.setColor(color);
            graphics.setStroke(new java.awt.BasicStroke(2.2f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case AGREGAR -> {
                    graphics.drawLine(10, 3, 10, 17);
                    graphics.drawLine(3, 10, 17, 10);
                }
                case EDITAR -> {
                    graphics.rotate(-Math.PI / 4, 10, 10);
                    graphics.drawRoundRect(7, 2, 6, 13, 2, 2);
                    graphics.drawLine(7, 12, 13, 12);
                    graphics.drawLine(7, 17, 13, 17);
                }
                case ELIMINAR -> {
                    graphics.drawRoundRect(5, 7, 10, 11, 2, 2);
                    graphics.drawLine(4, 5, 16, 5);
                    graphics.drawLine(8, 3, 12, 3);
                    graphics.drawLine(8, 10, 8, 15);
                    graphics.drawLine(12, 10, 12, 15);
                }
            }
            graphics.dispose();
        }
    }
}
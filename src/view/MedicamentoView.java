package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import model.Medicamento;
import model.Rol;
import model.Usuario;
import persistence.MedicamentoRepository;

/** Consulta del inventario y, para administradores, mantenimiento de medicamentos. */
public class MedicamentoView extends JPanel {
    private static final Color AZUL = new Color(0x2563EB);
    private static final Color AZUL_CLARO = new Color(0xDBEAFE);
    private static final Color OSCURO = new Color(0x1E293B);
    private static final Color FONDO = new Color(0xF1F5F9);
    private static final Color TEXTO_SECUNDARIO = new Color(0x64748B);
    private static final Color BORDE = new Color(0xDCE6F1);
    private static final Color ROJO = new Color(0xEF4444);
    private static final Color BLUE = new Color(0x2563EB);
    private static final Color RED_ACCION = new Color(0xF43737);
    private static final Color TEXTO_FORM = new Color(0x1E293B);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Usuario sesion;
    private final boolean puedeGestionar;
    private final MedicamentoRepository repositorio = new MedicamentoRepository();
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

    public MedicamentoView(Usuario sesion) {
        if (sesion == null || !sesion.estaActivo() || !sesion.estaEnSesion()
                || (!Rol.ADMINISTRADOR.equals(sesion.getRol())
                && !Rol.ENCARGADO.equals(sesion.getRol()))) {
            throw new SecurityException("Se requiere una sesión válida para consultar medicamentos.");
        }
        this.sesion = sesion;
        this.puedeGestionar = Rol.ADMINISTRADOR.equals(sesion.getRol());
        construirInterfaz();
        cargarInventario();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(0, 22));
        setBackground(FONDO);

        JPanel encabezado = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        encabezado.setOpaque(false);
        JLabel icono = new JLabel(new IconoMedicamento(54));
        JLabel titulo = new JLabel("Gestión de medicamentos");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 27));
        titulo.setForeground(OSCURO);
        JLabel subtitulo = new JLabel("Consulta y administra el inventario de la farmacia");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new javax.swing.BoxLayout(titulos, javax.swing.BoxLayout.Y_AXIS));
        titulos.add(titulo);
        titulos.add(javax.swing.Box.createVerticalStrut(4));
        titulos.add(subtitulo);
        encabezado.add(icono);
        encabezado.add(titulos);
        add(encabezado, BorderLayout.NORTH);

        RoundedPanel contenido = new RoundedPanel(18, Color.WHITE, BORDE);
        contenido.setLayout(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 18));

        JPanel herramientas = new JPanel(new BorderLayout(12, 0));
        herramientas.setOpaque(false);
        busqueda.setToolTipText("Buscar por código, nombre, presentación, categoría, laboratorio o marca");
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
        tabla.getTableHeader().setBackground(new Color(0xF1F6FC));
        tabla.getTableHeader().setForeground(OSCURO);
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
                mostrarFiltrados();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mostrarFiltrados();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                mostrarFiltrados();
            }
        });
        registroVisible = cantidadRegistros;
        mostrarFiltrados();
    }

    private JButton crearBoton(String texto, Color color) {
        return new BotonRedondeado(texto, color, Color.WHITE);
    }

    private void cargarInventario() {
        try {
            inventario = repositorio.listar();
            mostrarFiltrados();
        } catch (IOException e) {
            mostrarError("No se pudo cargar el inventario: " + e.getMessage());
        }
    }

    public void recargar() {
        cargarInventario();
    }

    private void mostrarFiltrados() {
        String consulta = busqueda.getText().trim().toLowerCase(Locale.ROOT);
        modelo.setRowCount(0);
        for (Medicamento medicamento : inventario) {
            String campos = String.join(" ",
                            medicamento.getCodigo(), medicamento.getNombre(), medicamento.getPresentacion(),
                            medicamento.getCategoria(), medicamento.getLaboratorio(), medicamento.getMarca())
                    .toLowerCase(Locale.ROOT);
            if (!campos.contains(consulta)) {
                continue;
            }
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
        boolean nuevo = existente == null;
        JDialog dialogo = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                nuevo ? "Registrar medicamento" : "Editar medicamento",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.setResizable(false);

        JPanel root = new RoundedPanel(16, Color.WHITE, BORDE);
        root.setLayout(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JLabel titulo = new JLabel(nuevo ? "Registrar nuevo medicamento" : "Editar medicamento");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(TEXTO_FORM);
        JLabel subtitulo = new JLabel("Los campos marcados con * son obligatorios");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        header.add(titulo);
        header.add(subtitulo);
        root.add(header, BorderLayout.NORTH);

        JTextField nombre = campo(existente == null ? "" : existente.getNombre(), "Nombre comercial");
        JTextField codigo = campo(existente == null ? "" : existente.getCodigo(), "Código interno");
        JComboBox<String> categoria = crearCombo(new String[]{
                "Analgésico", "Antibiótico", "Antiinflamatorio", "Antihistamínico",
                "Antiácido", "Vitamina", "Otro"
        }, existente == null ? null : existente.getCategoria());
        JComboBox<String> laboratorio = crearCombo(new String[]{
                "INTI", "Bagó", "COFAR", "Farmacorp", "Otro"
        }, existente == null ? null : existente.getLaboratorio());
        JTextField presentacion = campo(existente == null ? "" : existente.getPresentacion(), "Ej. Caja x 20 tabletas");
        JTextField precio = campo(existente == null ? "" : String.format(Locale.US, "%.2f", existente.getPrecio()), "Precio en Bs.");
        JTextField cantidad = campo(existente == null ? "" : Integer.toString(existente.getCantidadDisponible()), "Stock inicial");
        JTextField vencimiento = campo(existente == null ? "" : existente.getFechaVencimientoDate().format(FORMATO_FECHA), "MM/AAAA");

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 14, 12);

        addField(grid, "Nombre *", nombre, 0, 0, c);
        addField(grid, "Código *", codigo, 1, 0, c);
        c.insets = new Insets(0, 0, 14, 0);
        addField(grid, "Categoría *", categoria, 2, 0, c);
        c.insets = new Insets(0, 0, 14, 12);
        addField(grid, "Laboratorio *", laboratorio, 0, 1, c);
        addField(grid, "Presentación *", presentacion, 1, 1, c);
        addField(grid, "Vencimiento (MM/AAAA) *", vencimiento, 2, 1, c);
        c.insets = new Insets(0, 0, 0, 12);
        addField(grid, "Precio (Bs) *", precio, 0, 2, c);
        addField(grid, "Cantidad *", cantidad, 1, 2, c);
        c.insets = new Insets(0, 0, 0, 0);
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        grid.add(empty, c);

        root.add(grid, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton cancel = boton("Cancelar", new Color(0xE5E7EB));
        cancel.setForeground(TEXTO_FORM);
        JButton save = boton("Guardar", BLUE);
        save.setForeground(Color.WHITE);

        cancel.addActionListener(e -> dialogo.dispose());
        save.addActionListener(e -> guardarMedicamento(dialogo, existente, nombre, codigo,
                categoria, presentacion, laboratorio, precio, cantidad, vencimiento));

        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);

        dialogo.setContentPane(root);
        dialogo.pack();
        dialogo.setMinimumSize(new java.awt.Dimension(720, 480));
        dialogo.setLocationRelativeTo(this);
        dialogo.getRootPane().setDefaultButton(save);
        dialogo.setVisible(true);
    }

    private JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        Color colorTexto = color.equals(new Color(0xE5E7EB)) ? TEXTO_FORM : Color.WHITE;
        b.putClientProperty("colorBase", color);
        b.putClientProperty("colorTexto", colorTexto);
        b.setBackground(color);
        b.setForeground(colorTexto);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(btn.getBackground());
                g2.fillRoundRect(0, 0, btn.getWidth(), btn.getHeight(), 10, 10);
                g2.dispose();
                super.paint(g, c);
            }
        });
        return b;
    }

    private JTextField campo(String value, String placeholder) {
        JTextField f = new JTextField(value);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(TEXTO_FORM);
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        if (placeholder != null && !placeholder.isEmpty()) {
            f.setToolTipText(placeholder);
        }
        return f;
    }

    private JTextField campo(String texto) {
        return campo(texto, "");
    }

    private void addField(JPanel panel, String label, JComponent field, int x, int y, GridBagConstraints c) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXTO_FORM);
        wrapper.add(l, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        GridBagConstraints nc = (GridBagConstraints) c.clone();
        nc.gridx = x;
        nc.gridy = y;
        nc.gridwidth = 1;
        panel.add(wrapper, nc);
    }

    private JComboBox<String> crearCombo(String[] opciones, String seleccionado) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("Seleccione una opción");
        for (String opcion : opciones) {
            combo.addItem(opcion);
        }
        combo.setEditable(false);
        combo.setSelectedIndex(0);
        if (seleccionado != null) {
            boolean encontrado = false;
            for (int i = 1; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).equalsIgnoreCase(seleccionado)) {
                    combo.setSelectedIndex(i);
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                combo.addItem(seleccionado);
                combo.setSelectedItem(seleccionado);
            }
        }
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setBorder(new RoundedLineBorder(BORDE, 9, 1, 4, 8));
        return combo;
    }

    private JPanel crearFormulario(JTextField nombre, JTextField codigo,
                                   JComboBox<String> categoria, JComboBox<String> laboratorio,
                                   JTextField presentacion, JTextField precio,
                                   JTextField cantidad, JTextField vencimiento) {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18));
        agregarCampo(formulario, "Nombre *", nombre, 0, 0, 1);
        agregarCampo(formulario, "Código *", codigo, 1, 0, 1);
        agregarCampo(formulario, "Categoría *", categoria, 0, 1, 1);
        agregarCampo(formulario, "Laboratorio *", laboratorio, 1, 1, 1);
        agregarCampo(formulario, "Presentación *", presentacion, 0, 2, 2);
        agregarCampo(formulario, "Precio (Bs) *", precio, 0, 3, 1);
        agregarCampo(formulario, "Cantidad *", cantidad, 1, 3, 1);
        agregarCampo(formulario, "Vencimiento (MM/AAAA) *", vencimiento, 2, 3, 1);
        return formulario;
    }

    private void agregarCampo(JPanel panel, String etiqueta, Component campo,
                              int columna, int fila, int ancho) {
        JPanel grupo = new JPanel(new BorderLayout(0, 6));
        grupo.setOpaque(false);
        JLabel label = new JLabel(etiqueta);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(OSCURO);
        grupo.add(label, BorderLayout.NORTH);
        grupo.add(campo, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = columna;
        constraints.gridy = fila;
        constraints.gridwidth = ancho;
        constraints.weightx = ancho == 2 ? 2 : 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(0, columna == 0 ? 0 : 10, 14, 0);
        panel.add(grupo, constraints);
    }

    private void guardarMedicamento(JDialog dialogo, Medicamento existente,
                                    JTextField nombre, JTextField codigo,
                                    JComboBox<String> categoria, JTextField presentacion,
                                    JComboBox<String> laboratorio, JTextField precio,
                                    JTextField cantidad, JTextField vencimiento) {
        try {
            validarSesionAdministradora();
            String nombreIngresado = nombre.getText().trim();
            String codigoIngresado = codigo.getText().trim();
            String presentacionIngresada = presentacion.getText().trim();
            String vencimientoIngresado = vencimiento.getText().trim();
            if (nombreIngresado.isEmpty() || codigoIngresado.isEmpty()
                    || presentacionIngresada.isEmpty() || vencimientoIngresado.isEmpty()
                    || categoria.getSelectedIndex() == 0 || laboratorio.getSelectedIndex() == 0) {
                mostrarError(dialogo, "Completa todos los campos obligatorios.");
                return;
            }

            for (Medicamento medicamentoExistente : repositorio.listar()) {
                if (medicamentoExistente.getCodigo().equalsIgnoreCase(codigoIngresado)
                        && (existente == null || !medicamentoExistente.getCodigo()
                        .equalsIgnoreCase(existente.getCodigo()))) {
                    mostrarError(dialogo, "El código " + codigoIngresado
                            + " ya se encuentra registrado.");
                    return;
                }
            }

            double precioIngresado = Double.parseDouble(precio.getText().trim().replace(',', '.'));
            int cantidadIngresada = Integer.parseInt(cantidad.getText().trim());
            if (precioIngresado < 0 || cantidadIngresada < 0) {
                throw new NumberFormatException();
            }
            if (!vencimientoIngresado.matches("(0[1-9]|1[0-2])/\\d{4}")) {
                throw new DateTimeParseException("Fecha de vencimiento inválida.",
                        vencimientoIngresado, 0);
            }
            YearMonth mesVencimiento = YearMonth.parse(vencimientoIngresado, FORMATO_FECHA);
            LocalDate fecha = mesVencimiento.atEndOfMonth();

            Medicamento medicamento = new Medicamento(
                    nombreIngresado, codigoIngresado,
                    String.valueOf(categoria.getSelectedItem()), presentacionIngresada,
                    String.valueOf(laboratorio.getSelectedItem()),
                    existente == null ? "Sin marca" : existente.getMarca(),
                    cantidadIngresada, fecha.toString(), precioIngresado,
                    existente == null ? 0 : existente.getStockMinimo());
            if (existente == null) {
                if (repositorio.buscarPorCodigo(codigoIngresado).isPresent()) {
                    throw new IllegalArgumentException("Ya existe un medicamento con ese código.");
                }
                repositorio.registrar(medicamento);
            } else if (!repositorio.actualizar(medicamento)) {
                throw new IllegalStateException("El medicamento ya no existe en el inventario.");
            }
            dialogo.dispose();
            cargarInventario();
        } catch (NumberFormatException e) {
            mostrarError(dialogo, "Precio o cantidad no válidos. Usa números positivos.");
        } catch (DateTimeParseException e) {
            mostrarError(dialogo, "La fecha debe tener formato MM/AAAA, por ejemplo 12/2027.");
        } catch (IOException | IllegalArgumentException | SecurityException | IllegalStateException e) {
            mostrarError(dialogo, "Error al guardar: " + e.getMessage());
        }
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
        try {
            validarSesionAdministradora();
            if (!repositorio.eliminar(medicamento.getCodigo())) {
                throw new IllegalStateException("El medicamento ya no existe en el inventario.");
            }
            cargarInventario();
        } catch (IOException | SecurityException | IllegalStateException e) {
            mostrarError("No se pudo eliminar el medicamento: " + e.getMessage());
        }
    }

    private boolean confirmarEliminacion(Medicamento medicamento) {
        JDialog confirmacion = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Confirmar eliminación", Dialog.ModalityType.APPLICATION_MODAL);
        confirmacion.setUndecorated(true);
        confirmacion.setResizable(false);
        confirmacion.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        boolean[] confirmado = {false};

        RoundedPanel marco = new RoundedPanel(18, new Color(0xE7EEF7), new Color(0x7892B3));
        marco.setLayout(new BorderLayout());
        marco.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        RoundedPanel tarjeta = new RoundedPanel(14, Color.WHITE, new Color(0xDCE6F1));
        tarjeta.setLayout(new BorderLayout(0, 8));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(8, 22, 22, 22));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JLabel tituloBarra = new JLabel("Confirmación");
        tituloBarra.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tituloBarra.setForeground(TEXTO_SECUNDARIO);
        JButton cerrar = new JButton("×");
        cerrar.setFont(new Font("SansSerif", Font.PLAIN, 20));
        cerrar.setForeground(TEXTO_SECUNDARIO);
        cerrar.setBorderPainted(false);
        cerrar.setContentAreaFilled(false);
        cerrar.setFocusPainted(false);
        cerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cerrar.addActionListener(e -> confirmacion.dispose());
        barra.add(tituloBarra, BorderLayout.WEST);
        barra.add(cerrar, BorderLayout.EAST);
        tarjeta.add(barra, BorderLayout.NORTH);

        JPanel detalle = new JPanel();
        detalle.setOpaque(false);
        detalle.setLayout(new javax.swing.BoxLayout(detalle, javax.swing.BoxLayout.Y_AXIS));
        JLabel icono = new JLabel(new IconoAdvertencia(58));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel titulo = new JLabel("Confirmar eliminación");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(new Color(0x173B68));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        String nombreSeguro = medicamento.getNombre()
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel mensaje = new JLabel("<html><div style='width:330px;text-align:center;'>"
                + "¿Está seguro de eliminar el medicamento <b>\""
                + nombreSeguro + "\"</b> del inventario?</div></html>");
        mensaje.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mensaje.setForeground(new Color(0x36577D));
        mensaje.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        acciones.setOpaque(false);
        JButton cancelar = boton("Cancelar", new Color(0xEAF2FB));
        cancelar.setForeground(OSCURO);
        cancelar.addActionListener(e -> confirmacion.dispose());
        JButton eliminar = boton("Eliminar", RED_ACCION);
        eliminar.addActionListener(e -> {
            confirmado[0] = true;
            confirmacion.dispose();
        });
        acciones.add(cancelar);
        acciones.add(eliminar);

        detalle.add(icono);
        detalle.add(javax.swing.Box.createVerticalStrut(10));
        detalle.add(titulo);
        detalle.add(javax.swing.Box.createVerticalStrut(14));
        detalle.add(mensaje);
        detalle.add(javax.swing.Box.createVerticalStrut(22));
        detalle.add(acciones);
        tarjeta.add(detalle, BorderLayout.CENTER);
        marco.add(tarjeta, BorderLayout.CENTER);

        confirmacion.setContentPane(marco);
        confirmacion.getRootPane().setDefaultButton(cancelar);
        confirmacion.getRootPane().registerKeyboardAction(
                e -> confirmacion.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        confirmacion.setMinimumSize(new java.awt.Dimension(430, 310));
        confirmacion.pack();
        confirmacion.setLocationRelativeTo(this);
        confirmacion.setVisible(true);
        return confirmado[0];
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

    private void mostrarError(Component padre, String mensaje) {
        mostrarAlertaError(padre, mensaje);
    }

    private void mostrarAlertaError(Component padre, String mensaje) {
        JDialog alerta = new JDialog(SwingUtilities.getWindowAncestor(padre),
                "Error al guardar", Dialog.ModalityType.APPLICATION_MODAL);
        alerta.setUndecorated(true);
        alerta.setResizable(false);
        alerta.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        RoundedPanel marco = new RoundedPanel(18, new Color(0xE7EEF7), new Color(0x7892B3));
        marco.setLayout(new BorderLayout());
        marco.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        RoundedPanel tarjeta = new RoundedPanel(14, Color.WHITE, new Color(0xDCE6F1));
        tarjeta.setLayout(new BorderLayout(0, 8));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(8, 20, 20, 20));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JLabel tituloBarra = new JLabel("Aviso");
        tituloBarra.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tituloBarra.setForeground(TEXTO_SECUNDARIO);
        JButton cerrar = new JButton("×");
        cerrar.setFont(new Font("SansSerif", Font.PLAIN, 20));
        cerrar.setForeground(TEXTO_SECUNDARIO);
        cerrar.setBorderPainted(false);
        cerrar.setContentAreaFilled(false);
        cerrar.setFocusPainted(false);
        cerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cerrar.addActionListener(e -> alerta.dispose());
        barra.add(tituloBarra, BorderLayout.WEST);
        barra.add(cerrar, BorderLayout.EAST);
        tarjeta.add(barra, BorderLayout.NORTH);

        JPanel detalle = new JPanel();
        detalle.setOpaque(false);
        detalle.setLayout(new javax.swing.BoxLayout(detalle, javax.swing.BoxLayout.Y_AXIS));
        JLabel icono = new JLabel(new IconoAlerta(56));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel titulo = new JLabel("Error al guardar");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(new Color(0x173B68));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        String textoSeguro = mensaje == null ? "" : mensaje
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        JLabel texto = new JLabel("<html><div style='width:330px;text-align:center;'>"
                + textoSeguro + "</div></html>");
        texto.setFont(new Font("SansSerif", Font.PLAIN, 14));
        texto.setForeground(new Color(0x36577D));
        texto.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton aceptar = boton("Aceptar", AZUL);
        aceptar.setAlignmentX(Component.CENTER_ALIGNMENT);
        aceptar.addActionListener(e -> alerta.dispose());
        detalle.add(icono);
        detalle.add(javax.swing.Box.createVerticalStrut(10));
        detalle.add(titulo);
        detalle.add(javax.swing.Box.createVerticalStrut(12));
        detalle.add(texto);
        detalle.add(javax.swing.Box.createVerticalStrut(22));
        detalle.add(aceptar);
        tarjeta.add(detalle, BorderLayout.CENTER);
        marco.add(tarjeta, BorderLayout.CENTER);

        alerta.setContentPane(marco);
        alerta.getRootPane().setDefaultButton(aceptar);
        alerta.setMinimumSize(new java.awt.Dimension(410, 280));
        alerta.pack();
        alerta.setLocationRelativeTo(padre);
        alerta.setVisible(true);
    }

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

    private static JButton botonIcono(AccionIcono.Tipo tipo, Color color) {
        BotonRedondeado boton = new BotonRedondeado("", color, Color.WHITE);
        boton.setIcon(new AccionIcono(tipo, Color.WHITE, 17));
        boton.setPreferredSize(new java.awt.Dimension(40, 38));
        boton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        boton.setToolTipText(switch (tipo) {
            case EDITAR -> "Editar medicamento";
            case ELIMINAR -> "Eliminar medicamento";
            case AGREGAR -> "Registrar medicamento";
        });
        return boton;
    }

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

    private static class IconoMedicamento implements Icon {
        private final int tamano;

        IconoMedicamento(int tamano) {
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
            graphics.setColor(AZUL_CLARO);
            graphics.fillOval(x, y, tamano, tamano);
            graphics.translate(x + tamano / 2.0, y + tamano / 2.0);
            graphics.rotate(-Math.PI / 4);
            graphics.setColor(AZUL);
            graphics.fillRoundRect(-7, -17, 14, 34, 14, 14);
            graphics.fillRect(-7, -1, 14, 2);
            graphics.dispose();
        }
    }

    private static class IconoAlerta implements Icon {
        private final int tamano;

        IconoAlerta(int tamano) {
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
            graphics.setColor(new Color(0xEF4444));
            graphics.fillOval(x + 3, y + 3, tamano - 6, tamano - 6);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new java.awt.BasicStroke(4f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            int centroX = x + tamano / 2;
            graphics.drawLine(centroX, y + 17, centroX, y + 34);
            graphics.fillOval(centroX - 2, y + 41, 4, 4);
            graphics.dispose();
        }
    }

    private static class IconoAdvertencia implements Icon {
        private final int tamano;

        IconoAdvertencia(int tamano) {
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
            int centroX = x + tamano / 2;
            int[] xs = {centroX, x + 5, x + tamano - 5};
            int[] ys = {y + 4, y + tamano - 5, y + tamano - 5};
            graphics.setColor(new Color(0xF59E0B));
            graphics.fillPolygon(xs, ys, 3);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new java.awt.BasicStroke(4f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            graphics.drawLine(centroX, y + 21, centroX, y + 37);
            graphics.fillOval(centroX - 2, y + 43, 4, 4);
            graphics.dispose();
        }
    }

    private static class AccionIcono implements Icon {
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

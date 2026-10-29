
package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import model.Administrador;
import model.DetalleVenta;
import model.Medicamento;
import model.Usuario;
import model.Venta;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

public class VentaView extends JPanel {

    private final Usuario sesion;

    private final MedicamentoRepository medicamentos =
            new MedicamentoRepository();

    private final VentaRepository ventas =
            new VentaRepository();

    private final Map<String, ItemCarrito> carrito =
            new LinkedHashMap<>();

    private JTextField txtBuscar;
    private JTextField txtCantidad;
    private JTextField txtEfectivo;

    private JLabel lblProducto;
    private JLabel lblStock;
    private JLabel lblPrecio;
    private JLabel lblTotal;
    private JLabel lblCambio;
    private JLabel lblItems;

    private JTable tabla;
    private DefaultTableModel modeloTabla;

    private Medicamento seleccionado;

    private final Color AZUL = new Color(37, 99, 235);
    private final Color OSCURO = new Color(30, 41, 59);
    private final Color FONDO = new Color(241, 245, 249);
    private final Color GRIS = new Color(100, 116, 139);

    private static class ItemCarrito {
        Medicamento medicamento;
        int cantidad;

        ItemCarrito(Medicamento medicamento, int cantidad) {
            this.medicamento = medicamento;
            this.cantidad = cantidad;
        }

        BigDecimal subtotal() {
            return BigDecimal.valueOf(medicamento.getPrecio())
                    .multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public VentaView(Usuario sesion) {
        this.sesion = Objects.requireNonNull(sesion);

        setLayout(new BorderLayout());
        setBackground(FONDO);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        crearFormulario();
    }

    private JPanel tarjeta() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        return p;
    }

    private JLabel titulo(String texto, int tamano) {
        JLabel l = new JLabel(texto);
        l.setForeground(OSCURO);
        l.setFont(new Font("SansSerif", Font.BOLD, tamano));
        return l;
    }

    private JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(12, 15, 12, 15));
        return b;
    }

    private void crearFormulario() {

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.setBorder(
                new EmptyBorder(0, 0, 15, 0));

        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(titulo("Punto de venta", 25));

        JLabel subtitulo = new JLabel(
                "Farmacia Salud Vida - Registro de ventas");
        subtitulo.setForeground(GRIS);
        textos.add(subtitulo);

        encabezado.add(textos, BorderLayout.WEST);

        JButton btnHistorial = boton(
                "Historial de ventas", AZUL);

        btnHistorial.addActionListener(e -> mostrarHistorial());
        btnHistorial.setVisible(sesion instanceof Administrador);

        encabezado.add(btnHistorial, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new GridLayout(1, 2, 15, 0));
        cuerpo.setOpaque(false);

        // SECCION IZQUIERDA: BUSQUEDA
        JPanel izquierda = tarjeta();
        izquierda.setLayout(
                new BoxLayout(izquierda, BoxLayout.Y_AXIS));

        agregar(izquierda, titulo("Buscar medicamento", 19));

        agregar(izquierda, Box.createVerticalStrut(15));

        agregar(izquierda, new JLabel("Código o nombre:"));

        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("SansSerif", Font.PLAIN, 15));
        txtBuscar.setPreferredSize(new Dimension(100, 40));
        txtBuscar.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42));

        agregar(izquierda, txtBuscar);

        agregar(izquierda, Box.createVerticalStrut(10));

        JButton btnBuscar = boton("Buscar", AZUL);
        btnBuscar.addActionListener(e -> buscarMedicamento());
        txtBuscar.addActionListener(e -> buscarMedicamento());

        agregar(izquierda, btnBuscar);

        agregar(izquierda, Box.createVerticalStrut(20));

        JPanel info = new JPanel(new GridLayout(3, 1, 5, 8));
        info.setBackground(new Color(248, 250, 252));
        info.setBorder(new EmptyBorder(15, 15, 15, 15));
        info.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 125));

        lblProducto = new JLabel("Producto: Ninguno");
        lblStock = new JLabel("Stock: -");
        lblPrecio = new JLabel("Precio: Bs 0.00");

        info.add(lblProducto);
        info.add(lblStock);
        info.add(lblPrecio);

        agregar(izquierda, info);

        agregar(izquierda, Box.createVerticalStrut(20));

        agregar(izquierda, new JLabel("Cantidad:"));

        txtCantidad = new JTextField("1");
        txtCantidad.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));

        agregar(izquierda, txtCantidad);

        agregar(izquierda, Box.createVerticalStrut(15));

        JButton btnAgregar = boton(
                "Agregar al carrito", AZUL);

        btnAgregar.addActionListener(e -> agregarAlCarrito());

        agregar(izquierda, btnAgregar);

        izquierda.add(Box.createVerticalGlue());

        // SECCION DERECHA: CARRITO
        JPanel derecha = tarjeta();
        derecha.setLayout(new BorderLayout(0, 14));

        JPanel cabeceraCarrito =
                new JPanel(new BorderLayout());
        cabeceraCarrito.setOpaque(false);

        cabeceraCarrito.add(
                titulo("Carrito de compra", 19),
                BorderLayout.WEST);

        lblItems = new JLabel("0 productos");
        lblItems.setForeground(GRIS);

        cabeceraCarrito.add(lblItems, BorderLayout.EAST);
        derecha.add(cabeceraCarrito, BorderLayout.NORTH);

        String[] columnas = {
                "Medicamento", "Cant.", "Precio", "Subtotal"
        };

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tabla = new JTable(modeloTabla);
        tabla.setRowHeight(30);
        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(tabla);
        derecha.add(scroll, BorderLayout.CENTER);

        JPanel zonaInferior = new JPanel();
        zonaInferior.setOpaque(false);
        zonaInferior.setLayout(
                new BoxLayout(zonaInferior, BoxLayout.Y_AXIS));

        JPanel accionesCarrito =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accionesCarrito.setOpaque(false);

        JButton btnQuitar = boton(
                "Quitar producto", new Color(220, 38, 38));

        JButton btnVaciar = boton(
                "Vaciar carrito", GRIS);

        JButton btnCantidad = boton(
                "Cambiar cantidad", AZUL);

        btnQuitar.addActionListener(e -> quitarProducto());
        btnVaciar.addActionListener(e -> vaciarCarrito());
        btnCantidad.addActionListener(e -> cambiarCantidad());

        accionesCarrito.add(btnQuitar);
        accionesCarrito.add(btnCantidad);
        accionesCarrito.add(btnVaciar);

        agregar(zonaInferior, accionesCarrito);
        agregar(zonaInferior, Box.createVerticalStrut(15));

        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(219, 234, 254));
        totalPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        totalPanel.add(
                titulo("TOTAL", 17), BorderLayout.WEST);

        lblTotal = titulo("Bs 0.00", 23);
        lblTotal.setForeground(AZUL);

        totalPanel.add(lblTotal, BorderLayout.EAST);
        agregar(zonaInferior, totalPanel);

        agregar(zonaInferior, Box.createVerticalStrut(15));

        agregar(zonaInferior, new JLabel("Efectivo recibido (Bs):"));

        txtEfectivo = new JTextField();
        txtEfectivo.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));

        agregar(zonaInferior, txtEfectivo);

        txtEfectivo.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        calcularCambio();
                    }

                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        calcularCambio();
                    }

                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        calcularCambio();
                    }
                });

        agregar(zonaInferior, Box.createVerticalStrut(10));

        lblCambio = titulo("Cambio: Bs 0.00", 17);
        agregar(zonaInferior, lblCambio);

        agregar(zonaInferior, Box.createVerticalStrut(15));

        JButton btnRegistrar = boton(
                "CONFIRMAR VENTA", new Color(22, 163, 74));

        btnRegistrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRegistrar.addActionListener(e -> registrarVenta());

        agregar(zonaInferior, btnRegistrar);

        derecha.add(zonaInferior, BorderLayout.SOUTH);

        cuerpo.add(izquierda);
        cuerpo.add(derecha);

        add(cuerpo, BorderLayout.CENTER);
    }

    private void agregar(JPanel panel, Component componente) {
        if (panel.getLayout() instanceof BoxLayout) {
            if (componente instanceof JComponent) {
                ((JComponent) componente).setAlignmentX(
                        Component.LEFT_ALIGNMENT);
            }
        }
        panel.add(componente);
    }

    // BUSCAR POR CODIGO O NOMBRE
    private void buscarMedicamento() {

        String consulta = txtBuscar.getText().trim();

        seleccionado = null;
        lblProducto.setText("Producto: Ninguno");
        lblStock.setText("Stock: -");
        lblPrecio.setText("Precio: Bs 0.00");

        if (consulta.isEmpty()) {
            aviso("Ingrese el código o nombre del medicamento.");
            return;
        }

        try {
            List<Medicamento> resultados =
                    sesion.buscarMedicamento(consulta, medicamentos);

            if (resultados.isEmpty()) {
                aviso("No se encontraron medicamentos.");
                return;
            }

            Medicamento elegido;

            if (resultados.size() == 1) {
                elegido = resultados.get(0);
            } else {
                Map<String, Medicamento> opciones =
                        new LinkedHashMap<>();

                for (Medicamento m : resultados) {
                    opciones.put(
                            m.getCodigo() + " - " + m.getNombre(),
                            m);
                }

                Object opcion = JOptionPane.showInputDialog(
                        this,
                        "Seleccione el medicamento:",
                        "Resultados de búsqueda",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        opciones.keySet().toArray(),
                        opciones.keySet().iterator().next());

                if (opcion == null) return;

                elegido = opciones.get(opcion.toString());
            }

            if (elegido.estaVencido()) {
                aviso("No se puede vender un medicamento vencido.");
                return;
            }

            seleccionado = elegido;

            lblProducto.setText(
                    "Producto: " + elegido.getNombre());

            lblStock.setText(
                    "Stock: " + elegido.getCantidadDisponible());

            lblPrecio.setText(
                    "Precio: " + dinero(
                            BigDecimal.valueOf(elegido.getPrecio())));

        } catch (IOException | RuntimeException ex) {
            error(ex);
        }
    }

    // AGREGAR PRODUCTO
    private void agregarAlCarrito() {

        if (seleccionado == null) {
            aviso("Primero busque un medicamento.");
            return;
        }

        try {
            int cantidad = Integer.parseInt(
                    txtCantidad.getText().trim());

            if (cantidad <= 0)
                throw new IllegalArgumentException(
                        "La cantidad debe ser mayor que cero.");

            String codigo = seleccionado.getCodigo();

            ItemCarrito existente = carrito.get(codigo);

            int actual = existente == null
                    ? 0 : existente.cantidad;

            int nuevaCantidad = Math.addExact(actual, cantidad);

            if (nuevaCantidad >
                    seleccionado.getCantidadDisponible()) {

                aviso("Stock insuficiente para esa cantidad.");
                return;
            }

            carrito.put(codigo,
                    new ItemCarrito(seleccionado, nuevaCantidad));

            actualizarCarrito();

        } catch (NumberFormatException ex) {
            aviso("Ingrese una cantidad entera válida.");
        } catch (ArithmeticException ex) {
            aviso("La cantidad es demasiado grande.");
        } catch (IllegalArgumentException ex) {
            aviso(ex.getMessage());
        }
    }

    // QUITAR PRODUCTO DEL CARRITO
    private void quitarProducto() {

        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            aviso("Seleccione un producto de la tabla.");
            return;
        }

        String codigo = codigoEnFila(fila);

        carrito.remove(codigo);
        actualizarCarrito();
    }

    // MODIFICAR CANTIDAD
    private void cambiarCantidad() {

        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            aviso("Seleccione un producto.");
            return;
        }

        String codigo = codigoEnFila(fila);
        ItemCarrito item = carrito.get(codigo);

        String respuesta = JOptionPane.showInputDialog(
                this, "Nueva cantidad:", item.cantidad);

        if (respuesta == null) return;

        try {
            int nueva = Integer.parseInt(respuesta.trim());

            if (nueva <= 0) {
                aviso("La cantidad debe ser positiva.");
                return;
            }

            if (nueva > item.medicamento.getCantidadDisponible()) {
                aviso("La cantidad supera el stock.");
                return;
            }

            item.cantidad = nueva;
            actualizarCarrito();

        } catch (NumberFormatException ex) {
            aviso("Ingrese un número entero válido.");
        }
    }

    private String codigoEnFila(int fila) {

        int indice = 0;

        for (String codigo : carrito.keySet()) {
            if (indice == fila) return codigo;
            indice++;
        }

        throw new IllegalArgumentException("Fila inválida.");
    }

    private void vaciarCarrito() {

        if (carrito.isEmpty()) return;

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea vaciar el carrito?",
                "Confirmación",
                JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            limpiarCarrito();
        }
    }

    private void limpiarCarrito() {
        carrito.clear();
        seleccionado = null;

        txtBuscar.setText("");
        txtCantidad.setText("1");
        txtEfectivo.setText("");

        lblProducto.setText("Producto: Ninguno");
        lblStock.setText("Stock: -");
        lblPrecio.setText("Precio: Bs 0.00");

        actualizarCarrito();
    }

    private BigDecimal totalCarrito() {

        BigDecimal total = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.values()) {
            total = total.add(item.subtotal());
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String dinero(BigDecimal valor) {
        return String.format(Locale.US,
                "Bs %.2f", valor);
    }

    private void actualizarCarrito() {

        modeloTabla.setRowCount(0);

        for (ItemCarrito item : carrito.values()) {

            modeloTabla.addRow(new Object[]{
                    item.medicamento.getNombre(),
                    item.cantidad,
                    dinero(BigDecimal.valueOf(
                            item.medicamento.getPrecio())),
                    dinero(item.subtotal())
            });
        }

        lblItems.setText(
                carrito.size() + " productos");

        lblTotal.setText(dinero(totalCarrito()));

        calcularCambio();
    }

    private BigDecimal leerEfectivo() {

        String valor = txtEfectivo.getText()
                .trim().replace(',', '.');

        BigDecimal efectivo = new BigDecimal(valor);

        if (efectivo.signum() < 0 ||
                efectivo.scale() > 2) {
            throw new NumberFormatException(
                    "Importe no válido.");
        }

        return efectivo.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void calcularCambio() {

        BigDecimal total = totalCarrito();

        try {
            BigDecimal efectivo = leerEfectivo();
            BigDecimal cambio = efectivo.subtract(total);

            if (cambio.signum() < 0) {
                lblCambio.setText("Efectivo insuficiente");
                lblCambio.setForeground(Color.RED);
            } else {
                lblCambio.setText(
                        "Cambio: " + dinero(cambio));
                lblCambio.setForeground(
                        new Color(22, 163, 74));
            }

        } catch (NumberFormatException | ArithmeticException ex) {
            lblCambio.setText("Cambio: Bs 0.00");
            lblCambio.setForeground(OSCURO);
        }
    }

    // REGISTRAR UNA VENTA CON VARIOS DETALLES
    private void registrarVenta() {

        if (carrito.isEmpty()) {
            aviso("Agregue al menos un medicamento.");
            return;
        }

        BigDecimal efectivo;

        try {
            efectivo = leerEfectivo();
        } catch (NumberFormatException | ArithmeticException ex) {
            aviso("Ingrese el efectivo recibido con máximo 2 decimales.");
            return;
        }

        BigDecimal totalMostrado = totalCarrito();

        if (efectivo.compareTo(totalMostrado) < 0) {
            aviso("El efectivo recibido es insuficiente.");
            return;
        }

        // Reconsultar productos para evitar datos desactualizados
        Venta venta = new Venta(sesion);
        BigDecimal totalActual = BigDecimal.ZERO;

        try {
            for (ItemCarrito item : carrito.values()) {

                Medicamento actual = sesion.consultarStock(
                        item.medicamento.getCodigo(),
                        medicamentos
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medicamento no encontrado: "
                                + item.medicamento.getCodigo()));

                if (actual.estaVencido()) {
                    throw new IllegalStateException(
                            "Medicamento vencido: " + actual.getNombre());
                }

                if (item.cantidad > actual.getCantidadDisponible()) {
                    throw new IllegalStateException(
                            "Stock insuficiente: " + actual.getNombre());
                }

                // Evitar registrar precios antiguos sin avisar
                if (Double.compare(
                        actual.getPrecio(),
                        item.medicamento.getPrecio()) != 0) {
                    throw new IllegalStateException(
                            "Cambió el precio de "
                            + actual.getNombre()
                            + ". Actualice el carrito.");
                }

                venta.agregarDetalle(
                        new DetalleVenta(actual, item.cantidad));

                totalActual = totalActual.add(
                        BigDecimal.valueOf(actual.getPrecio())
                                .multiply(BigDecimal.valueOf(
                                        item.cantidad)));
            }

            totalActual = totalActual.setScale(
                    2, RoundingMode.HALF_UP);

            if (efectivo.compareTo(totalActual) < 0) {
                aviso("El efectivo no alcanza para el total actualizado.");
                return;
            }

            BigDecimal cambio = efectivo.subtract(totalActual);

            int confirmar = JOptionPane.showConfirmDialog(
                    this,
                    "Total: " + dinero(totalActual)
                    + "\nRecibido: " + dinero(efectivo)
                    + "\nCambio: " + dinero(cambio)
                    + "\n\n¿Confirmar esta venta?",
                    "Confirmar venta",
                    JOptionPane.YES_NO_OPTION);

            if (confirmar != JOptionPane.YES_OPTION) return;

            // UTILIZA EL METODO ORIGINAL DEL MODELO
            venta.registrar(sesion, medicamentos, ventas);

            String comprobante = crearComprobante(
                    venta, efectivo, cambio);

            limpiarCarrito();
            mostrarComprobante(comprobante);

        } catch (IOException | RuntimeException ex) {
            error(ex);
        }
    }

    // GENERAR COMPROBANTE DE TEXTO
    private String crearComprobante(
            Venta venta, BigDecimal efectivo,
            BigDecimal cambio) {

        StringBuilder texto = new StringBuilder();

        texto.append("           FARMACIA SALUD VIDA\n");
        texto.append("========================================\n");
        texto.append("          COMPROBANTE DE VENTA\n");
        texto.append("========================================\n");

        texto.append("Fecha: ");
        texto.append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        texto.append("\n");

        texto.append("Atendido por: ");
        texto.append(sesion.getNombreCompleto());
        texto.append("\n\n");

        for (DetalleVenta detalle : venta.obtenerDetalle()) {

            texto.append(detalle.getMedicamento().getNombre());
            texto.append("\n");

            texto.append("  ");
            texto.append(detalle.getCantidad());
            texto.append(" x ");
            texto.append(dinero(BigDecimal.valueOf(
                    detalle.getPrecioUnitario())));
            texto.append(" = ");
            texto.append(dinero(BigDecimal.valueOf(
                    detalle.getSubtotal())));
            texto.append("\n");
        }

        texto.append("----------------------------------------\n");
        texto.append("TOTAL: ");
        texto.append(dinero(BigDecimal.valueOf(
                venta.calcularTotal())));
        texto.append("\n");

        texto.append("EFECTIVO: ");
        texto.append(dinero(efectivo));
        texto.append("\n");

        texto.append("CAMBIO: ");
        texto.append(dinero(cambio));
        texto.append("\n");

        texto.append("========================================\n");
        texto.append("       Gracias por su compra\n");

        return texto.toString();
    }

    // MOSTRAR E IMPRIMIR COMPROBANTE
    private void mostrarComprobante(String contenido) {

        JTextArea area = new JTextArea(contenido, 20, 42);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(area);

        Object[] opciones = {"Imprimir", "Cerrar"};

        int opcion = JOptionPane.showOptionDialog(
                this, scroll, "Venta registrada correctamente",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, opciones, opciones[1]);

        if (opcion == 0) {
            try {
                if (!area.print()) {
                    aviso("Se canceló la impresión.");
                }
            } catch (PrinterException ex) {
                error(ex);
            }
        }
    }

    // HISTORIAL: PERMISO DE ADMINISTRADOR
    private void mostrarHistorial() {

        if (!(sesion instanceof Administrador)) {
            aviso("No tiene permiso para consultar el historial.");
            return;
        }

        try {
            List<Venta> historial =
                    ((Administrador) sesion)
                            .consultarHistorialVentas(ventas);

            DefaultTableModel modelo = new DefaultTableModel(
                    new String[]{
                            "Fecha", "Usuario",
                            "Productos", "Total (Bs)"
                    }, 0) {

                @Override
                public boolean isCellEditable(int fila, int col) {
                    return false;
                }
            };

            for (Venta v : historial) {
                modelo.addRow(new Object[]{
                        v.getFecha(),
                        v.getUsuarioRegistrado(),
                        v.obtenerDetalle().size(),
                        dinero(BigDecimal.valueOf(
                                v.calcularTotal()))
                });
            }

            JTable tablaHistorial = new JTable(modelo);
            tablaHistorial.setRowHeight(28);

            JScrollPane scroll =
                    new JScrollPane(tablaHistorial);

            scroll.setPreferredSize(new Dimension(720, 350));

            JOptionPane.showMessageDialog(
                    this, scroll, "Historial de ventas",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException | RuntimeException ex) {
            error(ex);
        }
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this, mensaje, "Aviso",
                JOptionPane.WARNING_MESSAGE);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(
                this, "Operación no completada:\n"
                        + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}

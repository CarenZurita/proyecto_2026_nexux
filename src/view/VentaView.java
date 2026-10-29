
package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.Optional;

import model.Medicamento;
import model.Usuario;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

public class VentaView extends JPanel {

    private final Usuario sesion;

    private final MedicamentoRepository medicamentos;
    private final VentaRepository ventas;

    private JTextField txtCodigo;
    private JTextField txtCantidad;

    private JLabel lblNombre;
    private JLabel lblStock;
    private JLabel lblPrecio;
    private JLabel lblTotal;

    private Medicamento medicamentoActual;

    private final Color AZUL = new Color(37, 99, 235);
    private final Color FONDO = new Color(241, 245, 249);
    private final Color TEXTO = new Color(30, 41, 59);

    public VentaView(Usuario sesion) {

        this.sesion = sesion;

        this.medicamentos = new MedicamentoRepository();
        this.ventas = new VentaRepository();

        setLayout(new BorderLayout(0, 20));
        setBackground(FONDO);
        setBorder(new EmptyBorder(15, 10, 15, 10));

        crearFormulario();
    }

    // FORMULARIO COMPARTIDO POR ADMINISTRADOR Y ENCARGADO
    private void crearFormulario() {
        JPanel principal = new ComponentesRedondeados.Panel(
                new BorderLayout(0, 18), Color.WHITE, 32, new Color(0xE2E8F0));
        principal.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel titulo = new JLabel("Registrar venta");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(TEXTO);
        JLabel subtitulo = new JLabel("Busca un medicamento e indica la cantidad a vender.");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitulo.setForeground(new Color(0x64748B));
        JPanel encabezado = new JPanel(new BorderLayout(0, 6));
        encabezado.setOpaque(false);
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(subtitulo, BorderLayout.CENTER);
        principal.add(encabezado, BorderLayout.NORTH);

        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        JLabel lblCodigo = etiquetaCampo("Código del medicamento");
        txtCodigo = new ComponentesRedondeados.Campo();
        prepararCampo(txtCodigo);
        lblCodigo.setLabelFor(txtCodigo);
        JButton btnBuscar = new ComponentesRedondeados.Boton("Buscar medicamento");
        estiloBoton(btnBuscar, AZUL);
        btnBuscar.addActionListener(e -> buscarMedicamento());
        txtCodigo.addActionListener(e -> buscarMedicamento());
        JPanel filaBusqueda = new JPanel(new BorderLayout(12, 0));
        filaBusqueda.setOpaque(false);
        filaBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaBusqueda.setPreferredSize(new Dimension(0, 42));
        filaBusqueda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        filaBusqueda.add(txtCodigo, BorderLayout.CENTER);
        filaBusqueda.add(btnBuscar, BorderLayout.EAST);
        formulario.add(lblCodigo);
        formulario.add(Box.createVerticalStrut(7));
        formulario.add(filaBusqueda);
        formulario.add(Box.createVerticalStrut(16));

        JPanel info = new ComponentesRedondeados.Panel(new GridLayout(1, 3, 18, 0),
                new Color(0xF8FAFC), 24);
        info.setBorder(new EmptyBorder(14, 16, 14, 16));
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        lblNombre = new JLabel("No seleccionado");
        lblStock = new JLabel("-");
        lblPrecio = new JLabel("Bs 0.00");
        agregarDato(info, "Medicamento", lblNombre);
        agregarDato(info, "Stock disponible", lblStock);
        agregarDato(info, "Precio unitario", lblPrecio);
        formulario.add(info);
        formulario.add(Box.createVerticalStrut(16));

        JLabel lblCantidad = etiquetaCampo("Cantidad a vender");
        txtCantidad = new ComponentesRedondeados.Campo();
        prepararCampo(txtCantidad);
        lblCantidad.setLabelFor(txtCantidad);
        txtCantidad.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { calcularTotal(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { calcularTotal(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { calcularTotal(); }
        });
        formulario.add(lblCantidad);
        formulario.add(Box.createVerticalStrut(7));
        formulario.add(txtCantidad);
        formulario.add(Box.createVerticalStrut(16));

        JPanel panelTotal = new ComponentesRedondeados.Panel(new BorderLayout(10, 0),
                new Color(0xDBEAFE), 24);
        panelTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelTotal.setBorder(new EmptyBorder(14, 18, 14, 18));
        panelTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        JLabel textoTotal = new JLabel("TOTAL A PAGAR");
        textoTotal.setFont(new Font("SansSerif", Font.BOLD, 15));
        textoTotal.setForeground(new Color(0x1E40AF));
        lblTotal = new JLabel("Bs 0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblTotal.setForeground(AZUL);
        panelTotal.add(textoTotal, BorderLayout.WEST);
        panelTotal.add(lblTotal, BorderLayout.EAST);
        formulario.add(panelTotal);
        principal.add(formulario, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        JButton btnLimpiar = new ComponentesRedondeados.Boton("Limpiar");
        estiloBoton(btnLimpiar, new Color(100, 116, 139));
        JButton btnRegistrar = new ComponentesRedondeados.Boton("Registrar venta");
        estiloBoton(btnRegistrar, new Color(22, 163, 74));
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnRegistrar.addActionListener(e -> registrarVenta());
        acciones.add(btnLimpiar);
        acciones.add(Box.createHorizontalStrut(12));
        acciones.add(btnRegistrar);
        principal.add(acciones, BorderLayout.SOUTH);
        add(principal, BorderLayout.CENTER);
    }

    private JLabel etiquetaCampo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 14));
        etiqueta.setForeground(TEXTO);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return etiqueta;
    }

    private void prepararCampo(JTextField campo) {
        campo.setFont(new Font("SansSerif", Font.PLAIN, 15));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setPreferredSize(new Dimension(0, 42));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    }

    private void agregarDato(JPanel panel, String texto, JLabel valor) {
        JPanel bloque = new JPanel(new BorderLayout(0, 8));
        bloque.setOpaque(false);
        if (panel.getComponentCount() > 0) {
            bloque.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xE2E8F0)),
                    new EmptyBorder(0, 14, 0, 0)));
        }
        JLabel etiqueta = etiquetaCampo(texto);
        etiqueta.setFont(etiqueta.getFont().deriveFont(13f));
        valor.putClientProperty("html.disable", Boolean.TRUE);
        valor.setFont(new Font("SansSerif", Font.PLAIN, 14));
        valor.setForeground(TEXTO);
        bloque.add(etiqueta, BorderLayout.NORTH);
        bloque.add(valor, BorderLayout.CENTER);
        panel.add(bloque);
    }

    // ESTILO DE LOS BOTONES
    private void estiloBoton(JButton boton, Color color) {

        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
        boton.setFont(
                new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setBorder(
                new EmptyBorder(12, 18, 12, 18));
    }

    // BUSCAR MEDICAMENTO
    private void buscarMedicamento() {

        String codigo = txtCodigo.getText().trim()
                .toUpperCase(java.util.Locale.ROOT);

        medicamentoActual = null;
        lblNombre.setText("No seleccionado");
        lblStock.setText("-");
        lblPrecio.setText("Bs 0.00");
        lblTotal.setText("Bs 0.00");

        if (codigo.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese el código del medicamento.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        try {

            Optional<Medicamento> resultado =
                    sesion.consultarStock(codigo, medicamentos);

            if (resultado.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "No se encontró el medicamento.",
                        "Búsqueda",
                        JOptionPane.INFORMATION_MESSAGE);

                return;
            }

            Medicamento medicamento = resultado.get();

            if (medicamento.estaVencido()) {

                JOptionPane.showMessageDialog(
                        this,
                        "No se puede vender un medicamento vencido.",
                        "Medicamento vencido",
                        JOptionPane.WARNING_MESSAGE);

                return;
            }

            medicamentoActual = medicamento;

            lblNombre.setText(medicamento.getNombre());
            lblStock.setText(
                    String.valueOf(
                            medicamento.getCantidadDisponible()));

            lblPrecio.setText(String.format(
                    java.util.Locale.US,
                    "Bs %.2f",
                    medicamento.getPrecio()));

            calcularTotal();

        } catch (IOException | SecurityException
                 | IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Error al buscar: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // CALCULAR TOTAL
    private void calcularTotal() {

        if (medicamentoActual == null) {
            lblTotal.setText("Bs 0.00");
            return;
        }

        try {

            int cantidad = Integer.parseInt(
                    txtCantidad.getText().trim());

            if (cantidad <= 0 ||
                    cantidad > medicamentoActual.getCantidadDisponible()) {

                lblTotal.setText("Bs 0.00");
                return;
            }

            double total =
                    medicamentoActual.getPrecio() * cantidad;

            lblTotal.setText(String.format(
                    java.util.Locale.US,
                    "Bs %.2f", total));

        } catch (NumberFormatException e) {
            lblTotal.setText("Bs 0.00");
        }
    }

    // REGISTRAR VENTA
    private void registrarVenta() {

        if (medicamentoActual == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Primero debe buscar un medicamento.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        int cantidad;

        try {

            cantidad = Integer.parseInt(
                    txtCantidad.getText().trim());

            if (cantidad <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese una cantidad entera mayor a cero.",
                    "Cantidad inválida",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        if (cantidad > medicamentoActual.getCantidadDisponible()) {

            JOptionPane.showMessageDialog(
                    this,
                    "La cantidad supera el stock disponible.",
                    "Stock insuficiente",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        double total =
                medicamentoActual.getPrecio() * cantidad;

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                String.format(
                        java.util.Locale.US,
                        "Medicamento: %s\nCantidad: %d\nTotal: Bs %.2f\n\n¿Confirmar venta?",
                        medicamentoActual.getNombre(),
                        cantidad,
                        total),
                "Confirmar venta",
                JOptionPane.YES_NO_OPTION);

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            sesion.registrarVenta(
                    medicamentoActual.getCodigo(),
                    cantidad,
                    medicamentos,
                    ventas);

            JOptionPane.showMessageDialog(
                    this,
                    "Venta registrada correctamente.",
                    "Venta exitosa",
                    JOptionPane.INFORMATION_MESSAGE);

            limpiarFormulario();

        } catch (IOException | SecurityException
                 | IllegalArgumentException
                 | IllegalStateException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo registrar la venta:\n"
                            + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

            // Los datos podrían haber cambiado.
            medicamentoActual = null;
            lblNombre.setText("No seleccionado");
            lblStock.setText("-");
            lblPrecio.setText("Bs 0.00");
            lblTotal.setText("Bs 0.00");
        }
    }

    // LIMPIAR FORMULARIO
    private void limpiarFormulario() {

        txtCodigo.setText("");
        txtCantidad.setText("");

        lblNombre.setText("No seleccionado");
        lblStock.setText("-");
        lblPrecio.setText("Bs 0.00");
        lblTotal.setText("Bs 0.00");

        medicamentoActual = null;
        txtCodigo.requestFocusInWindow();
    }
}

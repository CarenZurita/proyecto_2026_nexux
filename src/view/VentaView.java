
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

    // CREAR FORMULARIO DE VENTAS
    private void crearFormulario() {

        JPanel principal = new JPanel(new BorderLayout(0, 20));
        principal.setBackground(Color.WHITE);
        principal.setBorder(new EmptyBorder(25, 30, 25, 30));

        // TITULO
        JLabel titulo = new JLabel("Nueva venta");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(TEXTO);

        JLabel subtitulo = new JLabel(
                "Seleccione el medicamento y registre la venta");

        subtitulo.setFont(
                new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(Color.GRAY);

        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 5));
        encabezado.setOpaque(false);
        encabezado.add(titulo);
        encabezado.add(subtitulo);

        principal.add(encabezado, BorderLayout.NORTH);

        // CAMPOS
        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(
                new BoxLayout(formulario, BoxLayout.Y_AXIS));

        JLabel lblCodigo = new JLabel("Código del medicamento");
        lblCodigo.setFont(
                new Font("SansSerif", Font.BOLD, 14));

        txtCodigo = new JTextField();
        txtCodigo.setFont(
                new Font("SansSerif", Font.PLAIN, 15));
        txtCodigo.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));

        JButton btnBuscar = new JButton("Buscar medicamento");
        estiloBoton(btnBuscar, AZUL);

        btnBuscar.addActionListener(e -> buscarMedicamento());
        txtCodigo.addActionListener(e -> buscarMedicamento());

        JPanel filaBusqueda = new JPanel(new BorderLayout(10, 0));
        filaBusqueda.setOpaque(false);
        filaBusqueda.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42));

        filaBusqueda.add(txtCodigo, BorderLayout.CENTER);
        filaBusqueda.add(btnBuscar, BorderLayout.EAST);

        formulario.add(lblCodigo);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(filaBusqueda);
        formulario.add(Box.createVerticalStrut(25));

        // INFORMACION DEL MEDICAMENTO
        JPanel info = new JPanel(new GridLayout(3, 2, 10, 15));
        info.setBackground(new Color(248, 250, 252));
        info.setBorder(new EmptyBorder(20, 20, 20, 20));
        info.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 150));

        lblNombre = new JLabel("No seleccionado");
        lblStock = new JLabel("-");
        lblPrecio = new JLabel("Bs 0.00");

        agregarDato(info, "Medicamento:", lblNombre);
        agregarDato(info, "Stock disponible:", lblStock);
        agregarDato(info, "Precio unitario:", lblPrecio);

        formulario.add(info);
        formulario.add(Box.createVerticalStrut(25));

        // CANTIDAD
        JLabel lblCantidad = new JLabel("Cantidad a vender");
        lblCantidad.setFont(
                new Font("SansSerif", Font.BOLD, 14));

        txtCantidad = new JTextField();
        txtCantidad.setFont(
                new Font("SansSerif", Font.PLAIN, 15));
        txtCantidad.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));

        txtCantidad.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {

                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        calcularTotal();
                    }

                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        calcularTotal();
                    }

                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        calcularTotal();
                    }
                });

        formulario.add(lblCantidad);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(txtCantidad);
        formulario.add(Box.createVerticalStrut(25));

        // TOTAL
        JPanel panelTotal = new JPanel(new BorderLayout());
        panelTotal.setBackground(new Color(219, 234, 254));
        panelTotal.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelTotal.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 85));

        JLabel textoTotal = new JLabel("TOTAL A PAGAR");
        textoTotal.setFont(
                new Font("SansSerif", Font.BOLD, 16));
        textoTotal.setForeground(TEXTO);

        lblTotal = new JLabel("Bs 0.00");
        lblTotal.setFont(
                new Font("SansSerif", Font.BOLD, 26));
        lblTotal.setForeground(AZUL);

        panelTotal.add(textoTotal, BorderLayout.WEST);
        panelTotal.add(lblTotal, BorderLayout.EAST);

        formulario.add(panelTotal);

        principal.add(formulario, BorderLayout.CENTER);

        // BOTONES INFERIORES
        JPanel acciones = new JPanel(new FlowLayout(
                FlowLayout.RIGHT, 12, 0));
        acciones.setOpaque(false);

        JButton btnLimpiar = new JButton("Limpiar");
        estiloBoton(btnLimpiar, new Color(100, 116, 139));

        JButton btnRegistrar = new JButton("Registrar venta");
        estiloBoton(btnRegistrar, new Color(22, 163, 74));

        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnRegistrar.addActionListener(e -> registrarVenta());

        acciones.add(btnLimpiar);
        acciones.add(btnRegistrar);

        principal.add(acciones, BorderLayout.SOUTH);

        add(principal, BorderLayout.CENTER);
    }

    // AGREGAR DATOS INFORMATIVOS
    private void agregarDato(
            JPanel panel, String texto, JLabel valor) {

        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(
                new Font("SansSerif", Font.BOLD, 13));
        etiqueta.setForeground(TEXTO);

        valor.setFont(
                new Font("SansSerif", Font.PLAIN, 13));

        panel.add(etiqueta);
        panel.add(valor);
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

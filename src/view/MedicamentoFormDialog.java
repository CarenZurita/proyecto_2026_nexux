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
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import service.DatosMedicamento;
import model.Medicamento;

/**
 * Diálogo de captura de medicamentos; conserva decisiones visuales de los combos
 * y entrega datos crudos a MedicamentoView para su envío al controlador.
 */
final class MedicamentoFormDialog extends JDialog {
    private static final Color BORDE = EstiloSistema.BORDE;
    private static final Color TEXTO_FORM = new Color(0x1E293B);
    private static final Color TEXTO_SECUNDARIO = new Color(0x64748B);
    private static final Color AZUL = new Color(0x2563EB);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final JTextField nombre;
    private final JTextField codigo;
    private final JComboBox<String> categoria;
    private final JTextField presentacion;
    private final JComboBox<String> laboratorio;
    private final JTextField precio;
    private final JTextField cantidad;
    private final JTextField vencimiento;
    private final JButton guardar;
    private final JButton cancelar;
    private Consumer<DatosMedicamento> alGuardar;

    MedicamentoFormDialog(Component padre, Medicamento existente) {
        super(SwingUtilities.getWindowAncestor(padre),
                existente == null ? "Registrar medicamento" : "Editar medicamento",
                Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new RoundedPanel(16, Color.WHITE, BORDE);
        root.setLayout(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JLabel titulo = new JLabel(existente == null
                ? "Registrar nuevo medicamento" : "Editar medicamento");
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

        nombre = campo(existente == null ? "" : existente.getNombre(), "Nombre comercial");
        codigo = campo(existente == null ? "" : existente.getCodigo(), "Código interno");
        categoria = crearCombo(new String[]{
                "Analgésico", "Antibiótico", "Antiinflamatorio", "Antihistamínico",
                "Antiácido", "Vitamina", "Otro"
        }, existente == null ? null : existente.getCategoria());
        laboratorio = crearCombo(new String[]{
                "INTI", "Bagó", "COFAR", "Farmacorp", "Otro"
        }, existente == null ? null : existente.getLaboratorio());
        presentacion = campo(existente == null ? ""
                : existente.getPresentacion(), "Ej. Caja x 20 tabletas");
        precio = campo(existente == null ? ""
                : String.format(Locale.US, "%.2f", existente.getPrecio()), "Precio en Bs.");
        cantidad = campo(existente == null ? ""
                : Integer.toString(existente.getCantidadDisponible()), "Stock inicial");
        vencimiento = campo(existente == null ? ""
                : existente.getFechaVencimientoDate().format(FORMATO_FECHA), "MM/AAAA");

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
        cancelar = boton("Cancelar", new Color(0xE5E7EB));
        cancelar.setForeground(TEXTO_FORM);
        guardar = boton("Guardar", AZUL);
        guardar.setForeground(Color.WHITE);
        cancelar.addActionListener(e -> dispose());
        guardar.addActionListener(e -> enviarDatos());
        buttons.add(cancelar);
        buttons.add(guardar);
        root.add(buttons, BorderLayout.SOUTH);

        EstiloSistema.aplicarCampos(root);
        setContentPane(root);
        pack();
        setMinimumSize(new java.awt.Dimension(720, 480));
        setLocationRelativeTo(padre);
        getRootPane().setDefaultButton(guardar);
    }

    void alGuardar(Consumer<DatosMedicamento> callback) {
        alGuardar = callback;
    }

    void setProcesando(boolean procesando) {
        guardar.setEnabled(!procesando);
        cancelar.setEnabled(!procesando);
    }

    void mostrarError(String mensaje) {
        LoginView.mostrarAlertaError(this, "Error", mensaje);
    }

    private void enviarDatos() {
        if (categoria.getSelectedIndex() == 0 || laboratorio.getSelectedIndex() == 0) {
            mostrarError("Completa todos los campos obligatorios.");
            return;
        }
        alGuardar.accept(new DatosMedicamento(
                nombre.getText(), codigo.getText(), String.valueOf(categoria.getSelectedItem()),
                presentacion.getText(), String.valueOf(laboratorio.getSelectedItem()),
                precio.getText(), cantidad.getText(), vencimiento.getText()));
    }

    private JButton boton(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        Color colorTexto = color.equals(new Color(0xE5E7EB)) ? TEXTO_FORM : Color.WHITE;
        boton.putClientProperty("colorBase", color);
        boton.putClientProperty("colorTexto", colorTexto);
        boton.setBackground(color);
        boton.setForeground(colorTexto);
        boton.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton button = (JButton) c;
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(button.getBackground());
                graphics.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 10, 10);
                graphics.dispose();
                super.paint(g, c);
            }
        });
        return boton;
    }

    private JTextField campo(String valor, String textoAyuda) {
        JTextField campo = new JTextField(valor);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        campo.setForeground(TEXTO_FORM);
        campo.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        if (textoAyuda != null && !textoAyuda.isEmpty()) {
            campo.setToolTipText(textoAyuda);
        }
        return campo;
    }

    private void addField(JPanel panel, String label, JComponent field,
                          int x, int y, GridBagConstraints constraints) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        JLabel etiqueta = new JLabel(label);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 12));
        etiqueta.setForeground(TEXTO_FORM);
        wrapper.add(etiqueta, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        GridBagConstraints fieldConstraints = (GridBagConstraints) constraints.clone();
        fieldConstraints.gridx = x;
        fieldConstraints.gridy = y;
        fieldConstraints.gridwidth = 1;
        panel.add(wrapper, fieldConstraints);
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

    /** Panel del formulario con esquinas redondeadas. */
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

    /** Borde decorativo para los campos del diálogo. */
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
        public void paintBorder(Component componente, Graphics g,
                                int x, int y, int ancho, int alto) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(color);
            graphics.setStroke(new java.awt.BasicStroke(grosor));
            graphics.drawRoundRect(x, y, ancho - 1, alto - 1, radio, radio);
            graphics.dispose();
        }
    }
}

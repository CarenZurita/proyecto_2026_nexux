package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;

/** Paleta y componentes visuales compartidos por las pantallas de Salud Vida. */
public final class EstiloSistema {
    public static final Color AZUL = new Color(0x2563EB);
    public static final Color AZUL_SUAVE = new Color(0xCDE5FA);
    public static final Color TEXTO = new Color(0x1E293B);
    public static final Color SECUNDARIO = new Color(0x64748B);
    public static final Color BORDE = new Color(0xE5E7EB);
    public static final Color ENCABEZADO_TABLA = new Color(0xE4F2FF);

    private EstiloSistema() { }

    public static JPanel encabezado(String modulo, String titulo, String subtitulo) {
        JPanel encabezado = new JPanel(new BorderLayout(14, 0));
        encabezado.setOpaque(false);
        JLabel icono = new JLabel(new ComponentesRedondeados.IconoModulo(modulo, 52));
        icono.setVerticalAlignment(SwingConstants.CENTER);
        encabezado.add(icono, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setForeground(TEXTO);
        tituloLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        JLabel subtituloLabel = new JLabel(subtitulo);
        subtituloLabel.setForeground(SECUNDARIO);
        subtituloLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textos.add(tituloLabel);
        textos.add(javax.swing.Box.createVerticalStrut(4));
        textos.add(subtituloLabel);
        encabezado.add(textos, BorderLayout.CENTER);
        return encabezado;
    }

    public static JLabel subtituloSeccion(String texto) {
        JLabel subtitulo = new JLabel(texto);
        subtitulo.setFont(new Font("SansSerif", Font.BOLD, 19));
        subtitulo.setForeground(TEXTO);
        return subtitulo;
    }

    public static void aplicarTabla(JTable tabla) {
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setForeground(TEXTO);
        tabla.setBackground(Color.WHITE);
        tabla.setGridColor(BORDE);
        tabla.setSelectionBackground(new Color(0xDBEAFE));
        tabla.setSelectionForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 44));
        tabla.getTableHeader().setBackground(ENCABEZADO_TABLA);
        tabla.getTableHeader().setForeground(new Color(0x1E40AF));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabla.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE));
        tabla.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setOpaque(true);
                setBackground(ENCABEZADO_TABLA);
                setForeground(new Color(0x1E40AF));
                setFont(new Font("SansSerif", Font.BOLD, 12));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, BORDE),
                        new EmptyBorder(0, 12, 0, 8)));
                return this;
            }
        });
    }

    public static void aplicarCampos(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JTextField || componente instanceof JPasswordField) {
                JTextComponent campo = (JTextComponent) componente;
                campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
                campo.setBackground(Color.WHITE);
                campo.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0xD1D5DB), 1, true),
                        new EmptyBorder(7, 9, 7, 9)));
                campo.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) {
                        campo.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(AZUL, 1, true), new EmptyBorder(7, 9, 7, 9)));
                    }
                    @Override public void focusLost(java.awt.event.FocusEvent e) {
                        campo.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(new Color(0xD1D5DB), 1, true),
                                new EmptyBorder(7, 9, 7, 9)));
                    }
                });
            } else if (componente instanceof JComboBox<?> combo) {
                combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
                combo.setBackground(Color.WHITE);
                combo.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0xD1D5DB), 1, true),
                        new EmptyBorder(new Insets(3, 5, 3, 5))));
            } else if (componente instanceof JTextArea area) {
                area.setFont(new Font("SansSerif", Font.PLAIN, 13));
            }
            if (componente instanceof Container hijo) aplicarCampos(hijo);
        }
    }
}

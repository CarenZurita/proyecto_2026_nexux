package view;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.*;
import model.GestorUsuarios.FichaUsuario;

/** Tabla de cuentas integrada en el panel del administrador. */
public class UsuarioView extends JPanel {
    static final Color AZUL = new Color(0x2563EB);
    static final Color TEXTO = new Color(0x1E293B);
    private final JButton nuevo = boton("+  Nuevo usuario", AZUL);
    private final JLabel mensaje = new JLabel("Abre Usuarios para consultar las cuentas.");
    private List<FichaUsuario> cuentas = List.of();
    private Consumer<FichaUsuario> alEditar = u -> { };
    private Consumer<FichaUsuario> alEstado = u -> { };
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Rol", "Estado", "Acciones"}, 0) {
        @Override public boolean isCellEditable(int f, int c) { return c == 4; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : Object.class; }
    };
    private final JTable tabla = new JTable(modelo);

    public UsuarioView() {
        super(new BorderLayout(0, 24));
        setBackground(new Color(0xF1F5F9));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        JPanel encabezado = new JPanel(new BorderLayout(12, 0));
        encabezado.setOpaque(false);
        JLabel titulo = new JLabel("Gestión de usuarios");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        titulo.setForeground(TEXTO);
        encabezado.add(titulo, BorderLayout.CENTER);
        encabezado.add(nuevo, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);
        tabla.setRowHeight(68);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabla.setForeground(TEXTO);
        tabla.setBackground(Color.WHITE);
        tabla.setSelectionBackground(new Color(0xEFF6FF));
        tabla.setSelectionForeground(TEXTO);
        tabla.setGridColor(new Color(0xE2E8F0));
        tabla.setShowGrid(true);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 48));
        tabla.getTableHeader().setDefaultRenderer(new Cabecera());
        tabla.setDefaultRenderer(Object.class, new Texto());
        tabla.setDefaultRenderer(Integer.class, new Texto());
        tabla.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(175);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(135);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(4).setMinWidth(110);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new Estado());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new Acciones());
        tabla.getColumnModel().getColumn(4).setCellEditor(new EditorAcciones());
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xDBEAFE)));
        add(scroll, BorderLayout.CENTER);
        mensaje.setForeground(new Color(0x64748B));
        mensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));
        add(mensaje, BorderLayout.SOUTH);
    }

    public void alNuevo(Runnable accion) { nuevo.addActionListener(e -> accion.run()); }
    public void alEditar(Consumer<FichaUsuario> accion) { alEditar = accion; }
    public void alCambiarEstado(Consumer<FichaUsuario> accion) { alEstado = accion; }
    public void setCargando(boolean valor) {
        if (tabla.isEditing()) tabla.getCellEditor().cancelCellEditing();
        nuevo.setEnabled(!valor);
        tabla.setEnabled(!valor);
        if (valor) mensaje.setText("Procesando…");
    }
    public void mostrarDatos(List<FichaUsuario> datos) {
        cuentas = List.copyOf(datos);
        modelo.setRowCount(0);
        for (FichaUsuario u : cuentas) modelo.addRow(new Object[]{u.id(), u.nombre(),
                u.rol().esAdministrador() ? "Administrador" : "Encargado", u.activo(), u});
        mensaje.setForeground(new Color(0x64748B));
        mensaje.setText(cuentas.isEmpty() ? "No hay usuarios registrados." :
                cuentas.size() + " usuarios registrados. Las cuentas inactivas no pueden iniciar sesión.");
    }
    public void mostrarError(String detalle) {
        cuentas = List.of();
        modelo.setRowCount(0);
        mensaje.setForeground(new Color(0xB91C1C));
        mensaje.setText(detalle);
        mensaje.setToolTipText(detalle);
    }
    static JButton boton(String texto, Color fondo) {
        JButton b = new JButton(texto);
        b.setBackground(fondo); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setOpaque(true); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static class Texto extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
            setHorizontalAlignment(LEFT);
            return this;
        }
    }
    private static class Cabecera extends Texto {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            setBackground(new Color(0xEFF6FF)); setForeground(new Color(0x1E40AF));
            setFont(new Font("SansSerif", Font.BOLD, 13));
            return this;
        }
    }
    private static class Estado extends JPanel implements TableCellRenderer {
        private final JLabel texto = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D d = (Graphics2D)g.create();
                d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(getBackground()); d.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                d.dispose(); super.paintComponent(g);
            }
        };
        Estado() { super(new GridBagLayout()); add(texto); texto.setOpaque(false);
            texto.setFont(new Font("SansSerif", Font.BOLD, 13));
            texto.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12)); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            boolean activo = Boolean.TRUE.equals(v);
            setBackground(s ? t.getSelectionBackground() : Color.WHITE);
            texto.setText(activo ? "Activo" : "Inactivo");
            texto.setForeground(new Color(activo ? 0x166534 : 0x475569));
            texto.setBackground(new Color(activo ? 0xBBF7D0 : 0xE2E8F0));
            return this;
        }
    }
    private static JPanel acciones(boolean activo) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 14));
        p.setBackground(Color.WHITE);
        JButton editar = boton("", AZUL);
        editar.setIcon(new AccionIcon(true, false)); editar.setToolTipText("Editar usuario");
        editar.getAccessibleContext().setAccessibleName("Editar usuario");
        JButton estado = boton("", new Color(activo ? 0xEF4444 : 0x16A34A));
        estado.setIcon(new AccionIcon(false, !activo));
        estado.setToolTipText(activo ? "Desactivar usuario" : "Activar usuario");
        estado.getAccessibleContext().setAccessibleName(estado.getToolTipText());
        for (JButton b : new JButton[]{editar, estado}) {
            b.setPreferredSize(new Dimension(38, 36)); b.setBorder(BorderFactory.createEmptyBorder()); p.add(b);
        }
        return p;
    }
    private static class Acciones implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            return acciones(((FichaUsuario)v).activo());
        }
    }
    private class EditorAcciones extends AbstractCellEditor implements TableCellEditor {
        private FichaUsuario cuenta;
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            cuenta = (FichaUsuario)v;
            FichaUsuario elegida = cuenta;
            JPanel panel = acciones(elegida.activo());
            ((JButton)panel.getComponent(0)).addActionListener(e -> { fireEditingStopped(); alEditar.accept(elegida); });
            ((JButton)panel.getComponent(1)).addActionListener(e -> { fireEditingStopped(); alEstado.accept(elegida); });
            return panel;
        }
        @Override public Object getCellEditorValue() { return cuenta; }
    }
    private record AccionIcon(boolean lapiz, boolean activar) implements Icon {
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D d = (Graphics2D)g.create();
            d.translate(x, y); d.setColor(Color.WHITE); d.setStroke(new BasicStroke(2));
            d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (lapiz) { d.drawLine(4, 13, 13, 4); d.drawLine(6, 15, 15, 6); d.drawLine(4,13,3,16); d.drawLine(3,16,6,15); }
            else { d.drawOval(2,2,14,14); if(activar) { d.drawLine(5,9,8,12); d.drawLine(8,12,13,6); }
                   else d.drawLine(5,9,13,9); }
            d.dispose();
        }
    }
}

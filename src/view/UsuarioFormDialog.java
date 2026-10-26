package view;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import javax.swing.*;
import model.GestorUsuarios.FichaUsuario;
import model.Rol;

/** Modal de alta/edición. Los errores mantienen el formulario abierto. */
public class UsuarioFormDialog extends JDialog {
    private final JTextField nombre = new JTextField(24);
    private final JTextField usuario = new JTextField(24);
    private final JPasswordField clave = new JPasswordField(24);
    private final JPasswordField confirmacion = new JPasswordField(24);
    private final JComboBox<String> rol = new JComboBox<>(new String[]{"Encargado", "Administrador"});
    private final JTextArea error = new JTextArea(2, 24);
    private final JButton guardar = UsuarioView.boton("Guardar", UsuarioView.AZUL);
    private final JButton cancelar = new JButton("Cancelar");
    private final Integer id;
    private final boolean cuentaPropia;
    private boolean guardando;
    private Runnable alGuardar = () -> { };

    public UsuarioFormDialog(Window padre, FichaUsuario cuenta, int idSesion) {
        super(padre, cuenta == null ? "Nuevo usuario" : "Editar usuario", ModalityType.APPLICATION_MODAL);
        id = cuenta == null ? null : cuenta.id();
        cuentaPropia = cuenta != null && cuenta.id() == idSesion;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (!guardando) cerrar(); }
        });
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0xF8FAFC));
        panel.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        JLabel titulo = new JLabel(cuenta == null ? "Registrar nuevo usuario" : "Editar usuario · ID " + cuenta.id());
        titulo.setForeground(UsuarioView.TEXTO); titulo.setFont(new Font("SansSerif", Font.BOLD, 21));
        agregar(panel, g, titulo, 0, 14);
        campo(panel, g, "Nombre completo", nombre);
        campo(panel, g, "Usuario", usuario);
        campo(panel, g, "Contraseña", clave);
        campo(panel, g, "Confirmar contraseña", confirmacion);
        campo(panel, g, "Rol", rol);
        JTextArea ayuda = new JTextArea(cuenta == null
                ? "Usuario: 3–20 letras, números, punto, guion o guion bajo. Contraseña: 8–128 caracteres."
                : "Deja ambos campos de contraseña vacíos para conservar la actual. Una nueva contraseña requiere confirmación.");
        ayuda.setEditable(false); ayuda.setOpaque(false); ayuda.setLineWrap(true); ayuda.setWrapStyleWord(true);
        ayuda.setForeground(new Color(0x64748B)); ayuda.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ayuda.setRows(3); agregar(panel, g, ayuda, 8, 2);
        error.setEditable(false); error.setOpaque(false); error.setLineWrap(true); error.setWrapStyleWord(true);
        error.setForeground(new Color(0xB91C1C)); error.setFont(new Font("SansSerif", Font.PLAIN, 12));
        agregar(panel, g, error, 2, 8);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); botones.setOpaque(false);
        botones.add(cancelar); botones.add(guardar); agregar(panel, g, botones, 2, 0);
        if (cuenta != null) {
            nombre.setText(cuenta.nombre()); usuario.setText(cuenta.usuario());
            rol.setSelectedIndex(cuenta.rol().esAdministrador() ? 1 : 0);
            if (cuenta.id() == idSesion) {
                rol.setEnabled(false); rol.setToolTipText("Tu cuenta conserva el rol de administrador.");
            }
        }
        guardar.addActionListener(e -> alGuardar.run());
        cancelar.addActionListener(e -> cerrar());
        getRootPane().setDefaultButton(guardar);
        getRootPane().registerKeyboardAction(e -> { if (!guardando) cerrar(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContentPane(panel); pack(); setResizable(false); setLocationRelativeTo(padre);
    }
    private static void agregar(JPanel p, GridBagConstraints g, Component c, int arriba, int abajo) {
        g.gridy++; g.insets = new Insets(arriba, 0, abajo, 0); p.add(c, g);
    }
    private static void campo(JPanel p, GridBagConstraints g, String texto, JComponent c) {
        JLabel label = new JLabel(texto); label.setLabelFor(c); label.setForeground(UsuarioView.TEXTO);
        label.setFont(new Font("SansSerif", Font.BOLD, 13)); agregar(p, g, label, 8, 5);
        c.setFont(new Font("SansSerif", Font.PLAIN, 14));
        c.setPreferredSize(new Dimension(360, 34)); agregar(p, g, c, 0, 0);
    }
    public void alGuardar(Runnable accion) { alGuardar = accion; }
    public Integer getIdUsuario() { return id; }
    public String getNombre() { return nombre.getText(); }
    public String getUsuario() { return usuario.getText(); }
    public Rol getRolUsuario() { return rol.getSelectedIndex() == 1 ? Rol.ADMINISTRADOR : Rol.ENCARGADO; }
    public char[] getClave() { return clave.getPassword(); }
    public char[] getConfirmacion() { return confirmacion.getPassword(); }
    public void setGuardando(boolean valor) {
        guardando = valor; guardar.setEnabled(!valor); cancelar.setEnabled(!valor);
        rol.setEnabled(!valor && !cuentaPropia);
        nombre.setEnabled(!valor); usuario.setEnabled(!valor); clave.setEnabled(!valor); confirmacion.setEnabled(!valor);
        guardar.setText(valor ? "Guardando…" : "Guardar");
    }
    public void mostrarError(String mensaje) {
        error.setText(mensaje);
        if (mensaje.startsWith("Nombre:")) nombre.requestFocusInWindow();
        else if (mensaje.startsWith("Usuario:")) usuario.requestFocusInWindow();
        else if (mensaje.startsWith("Confirmación:")) confirmacion.requestFocusInWindow();
        else if (mensaje.startsWith("Contraseña:")) clave.requestFocusInWindow();
    }
    public void cerrar() {
        char[] c = clave.getPassword(); char[] r = confirmacion.getPassword();
        Arrays.fill(c, '\0'); Arrays.fill(r, '\0'); clave.setText(""); confirmacion.setText(""); dispose();
    }
}

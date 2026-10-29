package view;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import model.GestorUsuarios;
import model.Usuario;

/** Cambio de credenciales con contraseña actual y guardado fuera del hilo gráfico. */
public class SeguridadCuentaDialog extends JDialog {
    private final JPasswordField actual = new CampoClave();
    private final JPasswordField segunda = new CampoClave();
    private final JPasswordField tercera = new CampoClave();
    private final JButton guardar = boton("Guardar cambios", new Color(0x2563EB), Color.WHITE);
    private final JButton cancelar = boton("Cancelar", new Color(0xE2E8F0), new Color(0x1E293B));
    private final JTextArea error = new JTextArea(2, 30);
    private final Usuario sesion;
    private final boolean preguntas;
    private final Runnable alGuardar;
    private boolean guardando;

    public SeguridadCuentaDialog(Window padre, Usuario sesion, boolean preguntas, Runnable alGuardar) {
        super(padre, preguntas ? "Actualizar preguntas" : "Cambiar contraseña",
                ModalityType.APPLICATION_MODAL);
        this.sesion = sesion;
        this.preguntas = preguntas;
        this.alGuardar = alGuardar;
        setIconImage(LoginView.crearIconoVentana());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (!guardando) cerrar(); }
        });
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0xF8FAFC));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 26, 22, 26));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = -1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        JLabel titulo = new JLabel(getTitle());
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(new Color(0x1E293B));
        agregar(panel, g, titulo, 0, 12);
        JTextArea ayuda = texto(preguntas
                ? "Confirma tu contraseña actual y escribe las nuevas respuestas de seguridad."
                : "Confirma tu contraseña actual. La nueva debe tener de 8 a 128 caracteres.", 2);
        ayuda.setForeground(new Color(0x64748B));
        agregar(panel, g, ayuda, 0, 8);
        campo(panel, g, "Contraseña actual", actual);
        campo(panel, g, preguntas ? "¿Cuál es tu color favorito?" : "Nueva contraseña", segunda);
        campo(panel, g, preguntas ? "¿Cuál es tu comida favorita?" : "Confirmar nueva contraseña", tercera);
        error.setEditable(false); error.setOpaque(false);
        error.setLineWrap(true); error.setWrapStyleWord(true);
        error.setForeground(new Color(0xB91C1C));
        error.setFont(new Font("SansSerif", Font.PLAIN, 12));
        agregar(panel, g, error, 14, 8);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false); acciones.add(cancelar); acciones.add(guardar);
        agregar(panel, g, acciones, 0, 0);
        guardar.addActionListener(e -> guardar());
        cancelar.addActionListener(e -> cerrar());
        getRootPane().setDefaultButton(guardar);
        getRootPane().registerKeyboardAction(e -> { if (!guardando) cerrar(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContentPane(panel); pack(); setResizable(false); setLocationRelativeTo(padre);
    }

    private void guardar() {
        if (guardando) return;
        char[] a = actual.getPassword(), s = segunda.getPassword(), t = tercera.getPassword();
        if (a.length == 0 || s.length == 0 || t.length == 0) {
            Arrays.fill(a, '\0'); Arrays.fill(s, '\0'); Arrays.fill(t, '\0');
            error.setText("Completa todos los campos.");
            return;
        }
        error.setText(""); setGuardando(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try {
                    if (preguntas) GestorUsuarios.actualizarMisPreguntas(sesion, a, s, t);
                    else GestorUsuarios.cambiarMiClave(sesion, a, s, t);
                    return null;
                } finally {
                    Arrays.fill(a, '\0'); Arrays.fill(s, '\0'); Arrays.fill(t, '\0');
                }
            }
            @Override protected void done() {
                setGuardando(false);
                try {
                    get(); cerrar(); alGuardar.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    error.setText("La operación se interrumpió. Intenta nuevamente.");
                } catch (ExecutionException e) {
                    String mensaje = e.getCause().getMessage();
                    error.setText(mensaje == null ? "No se pudieron guardar los cambios." : mensaje);
                }
            }
        }.execute();
    }

    private void setGuardando(boolean valor) {
        guardando = valor;
        actual.setEnabled(!valor); segunda.setEnabled(!valor); tercera.setEnabled(!valor);
        guardar.setEnabled(!valor); cancelar.setEnabled(!valor);
        guardar.setText(valor ? "Guardando…" : "Guardar cambios");
    }

    private void cerrar() {
        for (JPasswordField campo : new JPasswordField[]{actual, segunda, tercera}) {
            char[] valor = campo.getPassword(); Arrays.fill(valor, '\0'); campo.setText("");
        }
        dispose();
    }

    private static JButton boton(String texto, Color fondo, Color frente) {
        JButton boton = new ComponentesRedondeados.Boton(texto);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setBackground(fondo); boton.setForeground(frente);
        boton.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private static JTextArea texto(String contenido, int filas) {
        JTextArea area = new JTextArea(contenido, filas, 30);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        area.setEditable(false); area.setOpaque(false);
        area.setLineWrap(true); area.setWrapStyleWord(true);
        return area;
    }

    private static void campo(JPanel panel, GridBagConstraints g, String texto, JPasswordField campo) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 13));
        etiqueta.setForeground(new Color(0x1E293B)); etiqueta.setLabelFor(campo);
        agregar(panel, g, etiqueta, 10, 6);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(370, 38));
        agregar(panel, g, campo, 0, 0);
    }

    private static void agregar(JPanel panel, GridBagConstraints g, JComponent componente, int arriba, int abajo) {
        g.gridy++; g.insets = new Insets(arriba, 0, abajo, 0); panel.add(componente, g);
    }

    private static class CampoClave extends JPasswordField {
        CampoClave() {
            super(24); setOpaque(false); setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(getBackground()); p.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            p.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            super.paintComponent(p); p.dispose();
        }
        @Override protected void paintBorder(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(hasFocus() ? new Color(0x2563EB) : new Color(0x94A3B8));
            p.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20); p.dispose();
        }
    }
}

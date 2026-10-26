package controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import model.GestorUsuarios;
import model.Rol;
import model.Usuario;
import view.LoginView;
import view.LoginView.DialogoRecuperacion;
import view.MenuAdministradorView;
import view.MenuEncargadoView;

public class LoginController {
    private final LoginView vista;
    private int intentosFallidos;
    private boolean procesando;

    public LoginController(LoginView vista) {
        this.vista = vista;
        vista.alIngresar(e -> ingresar());
        vista.alRecuperar(e -> recuperar());
    }

    public static void mostrarLogin() {
        LoginView vista = new LoginView();
        new LoginController(vista);
        vista.setVisible(true);
    }

    private void ingresar() {
        if (procesando) return;
        String nombre = vista.getUsuario();
        char[] clave = vista.getClave();
        if (nombre.isBlank() || clave.length == 0) {
            Arrays.fill(clave, '\0');
            vista.mostrarMensaje("Completa el usuario y la contraseña.");
            return;
        }
        procesando = true;
        vista.setProcesando(true);
        vista.mostrarMensaje(" ");
        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws Exception {
                try {
                    GestorUsuarios.inicializarAdministrador();
                    return GestorUsuarios.iniciarSesion(nombre, clave);
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }
            @Override
            protected void done() {
                procesando = false;
                vista.setProcesando(false);
                vista.limpiarClave();
                try {
                    Usuario sesion = get();
                    if (sesion == null) {
                        intentosFallidos++;
                        vista.mostrarErrorAutenticacion(3 - intentosFallidos);
                        if (intentosFallidos >= 3) vista.dispose();
                        else vista.mostrarMensaje("Datos incorrectos o cuenta deshabilitada. "
                                + "Intentos restantes: " + (3 - intentosFallidos));
                        return;
                    }
                    intentosFallidos = 0;
                    if (sesion.esAdministrador() && !sesion.tieneRecuperacion()) {
                        configurarPreguntas(sesion);
                    } else abrirMenu(sesion);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    vista.mostrarMensaje("La validación fue interrumpida. Vuelve a intentarlo.");
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(vista, mensajeError(e.getCause()),
                            "No se pudo iniciar sesión", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** La configuración inicial requiere una sesión real de administrador. */
    private void configurarPreguntas(Usuario sesion) {
        DialogoRecuperacion dialogo = vista.crearDialogoRecuperacion(true, sesion.getNombreUsuario());
        boolean[] guardado = {false};
        dialogo.alVerificar(e -> {
            if (dialogo.estaProcesando()) return;
            char[] color = dialogo.getColor(), comida = dialogo.getComida();
            ejecutar(dialogo, () -> {
                try {
                    GestorUsuarios.configurarRecuperacion(sesion, color, comida);
                    return true;
                } finally { Arrays.fill(color, '\0'); Arrays.fill(comida, '\0'); }
            }, resultado -> {
                guardado[0] = resultado;
                dialogo.dispose();
            });
        });
        dialogo.setVisible(true);
        if (guardado[0]) abrirMenu(sesion);
        else {
            sesion.cerrarSesion();
            vista.mostrarMensaje("Configuración cancelada. Ingresa nuevamente para registrar tus respuestas.");
        }
    }

    private void recuperar() {
        if (procesando) return;
        String nombre = vista.getUsuario();
        if (nombre.isBlank()) {
            vista.mostrarMensaje("Escribe tu usuario de administrador y pulsa ¿Olvidaste tu contraseña?");
            return;
        }
        if (!nombre.matches("(?i)[a-z0-9._-]{3,20}")) {
            vista.mostrarMensaje("Escribe un nombre de usuario válido.");
            return;
        }
        vista.limpiarClave();
        DialogoRecuperacion dialogo = vista.crearDialogoRecuperacion(false, nombre);
        GestorUsuarios.Recuperacion[] permiso = {null};
        dialogo.alVerificar(e -> {
            if (dialogo.estaProcesando()) return;
            char[] color = dialogo.getColor(), comida = dialogo.getComida();
            ejecutar(dialogo, () -> {
                try { return GestorUsuarios.verificarRecuperacion(nombre, color, comida); }
                finally { Arrays.fill(color, '\0'); Arrays.fill(comida, '\0'); }
            }, verificado -> {
                if (verificado == null) {
                    dialogo.mostrarError("No se pudo verificar la cuenta. Comprueba ambas respuestas "
                            + "y que la recuperación esté configurada para un administrador activo.");
                    return;
                }
                permiso[0] = verificado;
                dialogo.mostrarRestablecimiento();
            });
        });
        dialogo.alRestablecer(e -> {
            if (dialogo.estaProcesando()) return;
            char[] nueva = dialogo.getNuevaClave(), confirmacion = dialogo.getConfirmacion();
            ejecutar(dialogo, () -> {
                try {
                    GestorUsuarios.restablecerContrasena(permiso[0], nueva, confirmacion);
                    return true;
                } finally { Arrays.fill(nueva, '\0'); Arrays.fill(confirmacion, '\0'); }
            }, terminado -> {
                permiso[0] = null;
                intentosFallidos = 0;
                dialogo.dispose();
                vista.establecerUsuario(nombre);
                vista.limpiarClave();
                vista.mostrarExito("Contraseña restablecida. Ingresa con tu nueva contraseña.");
            });
        });
        dialogo.setVisible(true);
        permiso[0] = null;
    }

    /** El trabajo de archivos y PBKDF2 se realiza fuera del hilo de la interfaz. */
    private <T> void ejecutar(DialogoRecuperacion dialogo, Tarea<T> tarea, Consumer<T> completado) {
        dialogo.setProcesando(true);
        dialogo.mostrarError(" ");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tarea.realizar(); }
            @Override protected void done() {
                dialogo.setProcesando(false);
                try { completado.accept(get()); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dialogo.mostrarError("La operación fue interrumpida. Vuelve a intentarlo.");
                } catch (ExecutionException e) {
                    dialogo.mostrarError(mensajeError(e.getCause()));
                }
            }
        }.execute();
    }

    private static String mensajeError(Throwable causa) {
        if (causa instanceof IOException) {
            return "No se pudo leer o guardar datos/usuarios.txt. La operación no se completó; "
                    + "revisa el archivo y sus permisos.";
        }
        if (causa instanceof IllegalArgumentException || causa instanceof IllegalStateException
                || causa instanceof SecurityException) return causa.getMessage();
        return "No se pudo completar la operación. Revisa los datos del proyecto.";
    }

    @FunctionalInterface
    private interface Tarea<T> { T realizar() throws Exception; }

    private void abrirMenu(Usuario sesion) {
        JFrame menu;
        if (Rol.ADMINISTRADOR.equals(sesion.getRol())) {
            menu = new MenuAdministradorView(sesion, LoginController::mostrarLogin);
        } else if (Rol.ENCARGADO.equals(sesion.getRol())) {
            menu = new MenuEncargadoView(sesion, LoginController::mostrarLogin);
        } else {
            sesion.cerrarSesion();
            vista.mostrarMensaje("La cuenta no tiene un rol permitido.");
            return;
        }
        menu.setVisible(true);
        vista.dispose();
    }
}

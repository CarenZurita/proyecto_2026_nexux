package controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import model.GestorUsuarios;
import model.Rol;
import model.Usuario;
import view.LoginView;
import view.MenuAdministradorView;
import view.MenuEncargadoView;

public class LoginController {
    private final LoginView vista;
    private int intentosFallidos;
    private boolean procesando;

    public LoginController(LoginView vista) {
        this.vista = vista;
        vista.alIngresar(e -> ingresar());
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

        // La lectura del archivo y la comprobación de la clave no bloquean la ventana.
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
                        if (intentosFallidos >= 3) {
                            vista.dispose();
                        } else {
                            vista.mostrarMensaje("Datos incorrectos o cuenta deshabilitada. "
                                    + "Intentos restantes: " + (3 - intentosFallidos));
                        }
                        return;
                    }
                    abrirMenu(sesion);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    vista.mostrarMensaje("La validación fue interrumpida. Vuelve a intentarlo.");
                } catch (ExecutionException e) {
                    String texto = e.getCause() instanceof IOException
                            ? "No se pudo leer datos/usuarios.txt. Revisa el archivo y sus permisos."
                            : "No se pudo comprobar la cuenta. Revisa los datos del proyecto.";
                    JOptionPane.showMessageDialog(vista, texto,
                            "No se pudo iniciar sesión", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

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

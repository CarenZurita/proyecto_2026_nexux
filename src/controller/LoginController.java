package controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import model.PermisoRecuperacion;
import model.Rol;
import model.Usuario;
import service.AutenticacionService;
import service.RecuperacionService;
import service.UsuarioService;
import view.LoginView;
import view.LoginView.DialogoRecuperacion;
import view.MenuAdministradorView;
import view.MenuEncargadoView;

/**
 * Controlador de autenticación: conecta los eventos de LoginView con los
 * servicios y abre la vista principal del rol autenticado.
 */
public class LoginController {
    private final LoginView vista;
    private final AutenticacionService autenticacionService;
    private final RecuperacionService recuperacionService;
    private final UsuarioService usuarioService;
    private final SeguridadCuentaController seguridadCuentaController;
    private final MedicamentoController medicamentoController;
    private int intentosFallidos;
    private boolean procesando;

    /**
     * Conecta la vista de acceso con los servicios inyectados desde {@code Main}.
     *
     * @param vista interfaz de login
     * @param autenticacionService servicio de autenticación
     * @param recuperacionService servicio de recuperación
     * @param usuarioService servicio de gestión de usuarios
     * @param seguridadCuentaController controlador de seguridad de cuenta
     * @param medicamentoController controlador compartido del inventario
     */
    public LoginController(LoginView vista, AutenticacionService autenticacionService,
            RecuperacionService recuperacionService, UsuarioService usuarioService,
            SeguridadCuentaController seguridadCuentaController,
            MedicamentoController medicamentoController) {
        this.vista = vista;
        this.autenticacionService = autenticacionService;
        this.recuperacionService = recuperacionService;
        this.usuarioService = usuarioService;
        this.seguridadCuentaController = seguridadCuentaController;
        this.medicamentoController = medicamentoController;
        vista.alIngresar(e -> ingresar());
        vista.alRecuperar(e -> recuperar());
    }

    /**
     * Crea y muestra el login conservando las dependencias de la composición principal.
     *
     * @param autenticacionService servicio de autenticación
     * @param recuperacionService servicio de recuperación
     * @param usuarioService servicio de gestión de usuarios
     * @param seguridadCuentaController controlador de seguridad
     * @param medicamentoController controlador de medicamentos
     */
    public static void mostrarLogin(AutenticacionService autenticacionService,
            RecuperacionService recuperacionService, UsuarioService usuarioService,
            SeguridadCuentaController seguridadCuentaController,
            MedicamentoController medicamentoController) {
        LoginView vista = new LoginView();
        new LoginController(vista, autenticacionService, recuperacionService, usuarioService,
                seguridadCuentaController, medicamentoController);
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
                    autenticacionService.inicializarAdministrador();
                    return autenticacionService.iniciarSesion(nombre, clave);
                } finally {
                    // El trabajo criptográfico y de archivo va fuera del EDT; se borra la clave al acabar.
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
                    LoginView.mostrarAlertaError(vista, "No se pudo iniciar sesión", mensajeError(e.getCause()));
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
                    recuperacionService.configurarRecuperacion(sesion, color, comida);
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
        PermisoRecuperacion[] permiso = {null};
        dialogo.alVerificar(e -> {
            if (dialogo.estaProcesando()) return;
            char[] color = dialogo.getColor(), comida = dialogo.getComida();
            ejecutar(dialogo, () -> {
                try { return recuperacionService.verificarRecuperacion(nombre, color, comida); }
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
                    recuperacionService.restablecerContrasena(permiso[0], nueva, confirmacion);
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

    /** Operación bloqueante que se ejecuta en segundo plano desde el diálogo. */
    @FunctionalInterface
    private interface Tarea<T> { T realizar() throws Exception; }

    private void abrirMenu(Usuario sesion) {
        JFrame menu;
        if (Rol.ADMINISTRADOR.equals(sesion.getRol())) {
            menu = new MenuAdministradorView(sesion,
                    () -> mostrarLogin(autenticacionService, recuperacionService,
                            usuarioService, seguridadCuentaController, medicamentoController),
                    usuarioService, seguridadCuentaController, medicamentoController);
        } else if (Rol.ENCARGADO.equals(sesion.getRol())) {
            menu = new MenuEncargadoView(sesion,
                    () -> mostrarLogin(autenticacionService, recuperacionService,
                            usuarioService, seguridadCuentaController, medicamentoController),
                    seguridadCuentaController, medicamentoController);
        } else {
            sesion.cerrarSesion();
            vista.mostrarMensaje("La cuenta no tiene un rol permitido.");
            return;
        }
        menu.setVisible(true);
        vista.dispose();
    }
}

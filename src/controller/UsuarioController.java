package controller;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import model.GestorUsuarios.FichaUsuario;
import model.Rol;
import model.Usuario;
import service.UsuarioService;
import view.LoginView;
import view.UsuarioFormDialog;
import view.UsuarioView;

public class UsuarioController {
    private final Usuario sesion;
    private final UsuarioView vista;
    private final UsuarioService servicio;
    private boolean ocupado;

    public UsuarioController(Usuario sesion, UsuarioView vista) {
        this.sesion = sesion; this.vista = vista; this.servicio = new UsuarioService();
        vista.alNuevo(() -> formulario(null));
        vista.alEditar(this::formulario);
        vista.alCambiarEstado(this::cambiarEstado);
    }
    private boolean sesionActiva() {
        return sesion != null && sesion.esAdministrador() && sesion.estaActivo() && sesion.estaEnSesion();
    }
    public void actualizar() {
        if (ocupado) return;
        if (!sesionActiva()) { vista.mostrarError("Se requiere una sesión de administrador."); return; }
        ocupado = true; vista.setCargando(true);
        new SwingWorker<List<FichaUsuario>, Void>() {
            @Override protected List<FichaUsuario> doInBackground() throws Exception { return servicio.listar(sesion); }
            @Override protected void done() {
                ocupado = false; vista.setCargando(false);
                if (!sesionActiva()) { vista.mostrarError("La sesión ha finalizado."); return; }
                try { vista.mostrarDatos(get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); vista.mostrarError("Consulta interrumpida."); }
                catch (ExecutionException e) { vista.mostrarError(detalle(e)); }
            }
        }.execute();
    }
    private void formulario(FichaUsuario cuenta) {
        if (ocupado || !sesionActiva()) return;
        UsuarioFormDialog dialogo = new UsuarioFormDialog(SwingUtilities.getWindowAncestor(vista), cuenta, sesion.getId());
        dialogo.alGuardar(() -> guardar(dialogo));
        dialogo.setVisible(true);
    }
    private void guardar(UsuarioFormDialog dialogo) {
        if (ocupado) return;
        Integer id = dialogo.getIdUsuario();
        String nombre = dialogo.getNombre(); String usuario = dialogo.getUsuario(); Rol rol = dialogo.getRolUsuario();
        char[] clave = dialogo.getClave(); char[] confirmacion = dialogo.getConfirmacion();
        ocupado = true; dialogo.setGuardando(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try { servicio.guardar(sesion, id, nombre, usuario, rol, clave, confirmacion); return null; }
                finally { Arrays.fill(clave, '\0'); Arrays.fill(confirmacion, '\0'); }
            }
            @Override protected void done() {
                ocupado = false; dialogo.setGuardando(false);
                try { get(); dialogo.cerrar(); actualizar(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); dialogo.mostrarError("Guardado interrumpido. Revisa la tabla antes de reintentar."); }
                catch (ExecutionException e) { dialogo.mostrarError(detalle(e)); }
            }
        }.execute();
    }
    private void cambiarEstado(FichaUsuario cuenta) {
        if (ocupado || !sesionActiva()) return;
        if (cuenta.id() == sesion.getId()) {
            LoginView.mostrarAlertaError(vista, "Cuenta en uso", "No puedes desactivar tu propia cuenta.");
            return;
        }
        String accion = cuenta.activo() ? "Desactivar" : "Activar";
        String mensaje = accion + " a " + cuenta.nombre() + " (" + cuenta.usuario() + ")?\n"
                + (cuenta.activo() ? "El registro se conservará y ya no podrá iniciar sesión." : "Podrá volver a iniciar sesión.");
        LoginView.TonoDialogo tono = cuenta.activo()
                ? LoginView.TonoDialogo.DESTRUCTIVO : LoginView.TonoDialogo.INFORMACION;
        if (!LoginView.mostrarConfirmacion(vista, accion + " usuario", mensaje, tono)) return;
        ocupado = true; vista.setCargando(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                servicio.cambiarEstado(sesion, cuenta.id(), !cuenta.activo()); return null;
            }
            @Override protected void done() {
                ocupado = false; vista.setCargando(false);
                try { get(); actualizar(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); actualizar(); }
                catch (ExecutionException e) { LoginView.mostrarAlertaError(vista, "No se pudo cambiar el estado", detalle(e)); actualizar(); }
            }
        }.execute();
    }
    private static String detalle(ExecutionException e) {
        String mensaje = e.getCause().getMessage();
        return mensaje == null ? "No se pudo completar la operación. Revisa datos/usuarios.txt." : mensaje;
    }
}

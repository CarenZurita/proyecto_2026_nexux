package service;

import java.util.List;
import java.util.Locale;
import model.Usuario;

/**
 * Validaciones y búsquedas compartidas por los servicios de Usuarios y Seguridad.
 */
final class UsuarioServiceSupport {
    private UsuarioServiceSupport() { }

    static String normalizarUsuario(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
    }

    static Usuario buscar(List<Usuario> cuentas, String nombreUsuario) {
        for (Usuario cuenta : cuentas) {
            if (cuenta.getNombreUsuario().equals(nombreUsuario)) return cuenta;
        }
        return null;
    }

    static Usuario validarSesionPropia(Usuario sesion, List<Usuario> cuentas) {
        if (sesion == null || !sesion.estaEnSesion() || !sesion.estaActivo()) {
            throw new SecurityException("Se requiere una sesión activa.");
        }
        Usuario actual = cuentas.stream()
                .filter(u -> u.getId() == sesion.getId())
                .findFirst()
                .orElse(null);
        if (actual == null || !actual.estaActivo() || !actual.mismaCredencial(sesion)) {
            throw new SecurityException("La sesión cambió. Vuelve a iniciar sesión.");
        }
        return actual;
    }

    static Usuario validarAdministrador(Usuario sesion, List<Usuario> cuentas) {
        if (sesion == null || !sesion.estaEnSesion() || !administradorActivo(sesion)) {
            throw new SecurityException("Se requiere una sesión de administrador.");
        }
        Usuario actual = cuentas.stream()
                .filter(u -> u.getId() == sesion.getId())
                .findFirst()
                .orElse(null);
        if (!administradorActivo(actual) || !actual.mismaCredencial(sesion)) {
            throw new SecurityException("La sesión cambió. Vuelve a iniciar sesión.");
        }
        return actual;
    }

    static boolean administradorActivo(Usuario cuenta) {
        return cuenta != null && cuenta.esAdministrador() && cuenta.estaActivo();
    }
}

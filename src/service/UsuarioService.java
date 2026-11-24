package service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import model.FichaUsuario;
import model.Rol;
import model.Usuario;

/**
 * Fachada de aplicación para la vista de Usuarios: traduce guardar a alta o
 * edición y delega las reglas en {@link GestionUsuariosService}.
 */
public class UsuarioService {
    private final GestionUsuariosService gestionUsuariosService;

    /**
     * Crea el servicio de vista con la lógica central de gestión de cuentas.
     *
     * @param gestionUsuariosService servicio que aplica las reglas de negocio
     */
    public UsuarioService(GestionUsuariosService gestionUsuariosService) {
        this.gestionUsuariosService = gestionUsuariosService;
    }

    /**
     * Obtiene las fichas visibles de usuarios.
     *
     * @param sesion sesión administradora
     * @return cuentas visibles sin credenciales
     * @throws IOException si falla la lectura
     * @throws SecurityException si la sesión no es válida
     */
    public List<FichaUsuario> listar(Usuario sesion) throws IOException {
        return gestionUsuariosService.listarUsuarios(sesion);
    }
    /**
     * Guarda un usuario nuevo si el ID es nulo, o actualiza el existente.
     *
     * @param sesion sesión administradora
     * @param id ID existente; {@code null} indica alta
     * @param nombre nombre visible
     * @param usuario identificador de acceso
     * @param rol rol asignado
     * @param clave contraseña nueva
     * @param confirmacion confirmación de contraseña
     * @throws IOException si falla la persistencia
     * @throws GeneralSecurityException si falla la protección criptográfica
     * @throws IllegalArgumentException si los datos no cumplen las reglas
     */
    public void guardar(Usuario sesion, Integer id, String nombre, String usuario, Rol rol,
                        char[] clave, char[] confirmacion) throws IOException, GeneralSecurityException {
        if (id == null) {
            gestionUsuariosService.registrarUsuario(
                    sesion, nombre, usuario, rol, clave, confirmacion);
        } else {
            gestionUsuariosService.actualizarUsuario(
                    sesion, id, nombre, usuario, rol, clave, confirmacion);
        }
    }
    /**
     * Cambia el estado activo de una cuenta.
     *
     * @param sesion sesión administradora
     * @param id cuenta que cambia de estado
     * @param activo nuevo estado
     * @throws IOException si falla la persistencia
     * @throws SecurityException si la sesión no es válida
     * @throws IllegalArgumentException si se intenta una transición prohibida
     */
    public void cambiarEstado(Usuario sesion, int id, boolean activo) throws IOException {
        gestionUsuariosService.cambiarEstadoUsuario(sesion, id, activo);
    }
}

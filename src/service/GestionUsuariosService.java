package service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import model.FichaUsuario;
import model.Rol;
import model.Usuario;
import persistence.UsuarioRepository;

/**
 * Servicio de Usuarios: valida permisos y reglas de cuentas antes de persistir
 * los cambios solicitados por los controladores.
 */
public class GestionUsuariosService {
    private final UsuarioRepository repositorio;

    /**
     * Crea el servicio con el repositorio compartido por la aplicación.
     *
     * @param repositorio persistencia de cuentas
     */
    public GestionUsuariosService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Lista fichas públicas de las cuentas después de validar la sesión administradora.
     *
     * @param sesion cuenta autenticada que solicita la operación
     * @return fichas sin hashes ni respuestas de recuperación
     * @throws IOException si falla la lectura
     * @throws SecurityException si la sesión no es administradora vigente
     */
    public List<FichaUsuario> listarUsuarios(Usuario sesion) throws IOException {
        // Evita que la autorización y la lectura vean versiones distintas del archivo.
        synchronized (repositorio) {
            List<Usuario> cuentas = repositorio.listar();
            UsuarioServiceSupport.validarAdministrador(sesion, cuentas);
            return cuentas.stream().map(cuenta -> new FichaUsuario(
                    cuenta.getId(), cuenta.getNombreCompleto(), cuenta.getNombreUsuario(),
                    cuenta.getRol(), cuenta.estaActivo())).toList();
        }
    }

    /**
     * Registra una cuenta con identificador de acceso único y contraseña confirmada.
     *
     * @param sesion sesión administradora vigente
     * @param nombre nombre visible (1 a 100 caracteres, sin delimitadores TXT)
     * @param usuario nombre de acceso normalizado (3 a 20 caracteres permitidos)
     * @param rol rol de administrador o encargado
     * @param clave contraseña de 8 a 128 caracteres
     * @param confirmacion repetición exacta de la contraseña
     * @throws IOException si falla la lectura o escritura
     * @throws GeneralSecurityException si falla la protección de la contraseña
     * @throws SecurityException si la sesión no es administradora vigente
     * @throws IllegalArgumentException si los datos, unicidad o confirmación no son válidos
     */
    public void registrarUsuario(Usuario sesion, String nombre, String usuario, Rol rol,
            char[] clave, char[] confirmacion)
            throws IOException, GeneralSecurityException {
        synchronized (repositorio) {
            List<Usuario> cuentas = repositorio.listar();
            UsuarioServiceSupport.validarAdministrador(sesion, cuentas);
            String normalizado = UsuarioServiceSupport.normalizarUsuario(usuario);
            validarDatos(nombre, normalizado, rol, cuentas, 0);
            validarConfirmacion(clave, confirmacion);
            cuentas.add(Usuario.crear(normalizado, nombre.trim(), rol, clave));
            repositorio.guardarTodos(cuentas);
        }
    }

    /**
     * Actualiza una cuenta y protege las invariantes del último administrador activo.
     *
     * @param sesion sesión administradora vigente
     * @param id identificador de la cuenta
     * @param nombre nuevo nombre visible
     * @param usuario nuevo nombre de acceso
     * @param rol nuevo rol
     * @param clave contraseña nueva opcional; vacía conserva la actual
     * @param confirmacion confirmación de la contraseña opcional
     * @throws IOException si falla la lectura o escritura
     * @throws GeneralSecurityException si falla la protección de la contraseña nueva
     * @throws SecurityException si la sesión ya no es administradora válida
     * @throws IllegalArgumentException si los datos son inválidos o se rompe una invariante
     */
    public void actualizarUsuario(Usuario sesion, int id, String nombre, String usuario, Rol rol,
            char[] clave, char[] confirmacion)
            throws IOException, GeneralSecurityException {
        synchronized (repositorio) {
            List<Usuario> cuentas = repositorio.listar();
            Usuario administrador = UsuarioServiceSupport.validarAdministrador(sesion, cuentas);
            Usuario anterior = cuentas.stream().filter(u -> u.getId() == id).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La cuenta ya no existe. Actualiza la tabla."));
            String normalizado = UsuarioServiceSupport.normalizarUsuario(usuario);
            validarDatos(nombre, normalizado, rol, cuentas, id);
            if (id == administrador.getId() && !rol.esAdministrador()) {
                throw new IllegalArgumentException(
                        "No puedes quitar el rol de administrador a tu propia cuenta.");
            }
            if (anterior.esAdministrador() && anterior.estaActivo() && !rol.esAdministrador()
                    && ultimoAdministradorActivo(cuentas)) {
                throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
            }
            Usuario editado = anterior.conRol(rol);
            editado.setNombreCompleto(nombre.trim());
            editado.setNombreUsuario(normalizado);
            if (clave == null || confirmacion == null) {
                throw new IllegalArgumentException("Revisa los campos de contraseña.");
            }
            if (clave.length > 0 || confirmacion.length > 0) {
                validarConfirmacion(clave, confirmacion);
                editado.cambiarClave(clave);
            }
            cuentas.set(cuentas.indexOf(anterior), editado);
            repositorio.guardarTodos(cuentas);
            if (id == administrador.getId()) {
                sesion.sincronizarDatos(editado);
            }
        }
    }

    /**
     * Activa o desactiva una cuenta sin permitir desactivar al actor ni al último administrador.
     *
     * @param sesion sesión administradora vigente
     * @param id cuenta que cambia de estado
     * @param activo nuevo estado
     * @throws IOException si falla la lectura o escritura
     * @throws SecurityException si la sesión no es administradora vigente
     * @throws IllegalArgumentException si la cuenta desapareció o el cambio viola una invariante
     */
    public void cambiarEstadoUsuario(Usuario sesion, int id, boolean activo) throws IOException {
        synchronized (repositorio) {
            List<Usuario> cuentas = repositorio.listar();
            Usuario administrador = UsuarioServiceSupport.validarAdministrador(sesion, cuentas);
            Usuario cuenta = cuentas.stream().filter(u -> u.getId() == id).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La cuenta ya no existe. Actualiza la tabla."));
            if (!activo && id == administrador.getId()) {
                throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
            }
            if (!activo && cuenta.esAdministrador() && cuenta.estaActivo()
                    && ultimoAdministradorActivo(cuentas)) {
                throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
            }
            cuenta.setActivo(activo);
            repositorio.guardarTodos(cuentas);
        }
    }

    /**
     * Cambia la contraseña de la propia cuenta después de validar la clave actual.
     *
     * @param sesion sesión propia vigente
     * @param actual contraseña actual
     * @param nueva contraseña nueva de 8 a 128 caracteres
     * @param confirmar confirmación exacta de la contraseña nueva
     * @throws IOException si falla la lectura o escritura
     * @throws GeneralSecurityException si falla la comprobación o protección criptográfica
     * @throws SecurityException si la sesión es obsoleta o la clave actual es incorrecta
     * @throws IllegalArgumentException si la nueva contraseña o confirmación es inválida
     */
    public void cambiarMiClave(Usuario sesion, char[] actual, char[] nueva, char[] confirmar)
            throws IOException, GeneralSecurityException {
        synchronized (repositorio) {
            List<Usuario> cuentas = repositorio.listar();
            Usuario cuenta = UsuarioServiceSupport.validarSesionPropia(sesion, cuentas);
            if (!cuenta.comprobarClave(actual)) {
                throw new SecurityException("Contraseña actual incorrecta.");
            }
            validarConfirmacion(nueva, confirmar);
            cuenta.cambiarClave(nueva);
            repositorio.guardarTodos(cuentas);
            sesion.sincronizarDatos(cuenta);
        }
    }

    private static void validarDatos(String nombre, String usuario, Rol rol,
            List<Usuario> cuentas, int id) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.length() > 100
                || nombre.contains(";") || nombre.indexOf('\n') >= 0 || nombre.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "Nombre: escribe de 1 a 100 caracteres, sin punto y coma ni saltos de línea.");
        }
        if (!usuario.matches("[a-z0-9._-]{3,20}")) {
            throw new IllegalArgumentException(
                    "Usuario: utiliza de 3 a 20 letras, números, punto, guion o guion bajo.");
        }
        if (rol == null || (!rol.equals(Rol.ADMINISTRADOR) && !rol.equals(Rol.ENCARGADO))) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
        if (cuentas.stream().anyMatch(u -> u.getId() != id && u.getNombreUsuario().equals(usuario))) {
            throw new IllegalArgumentException(
                    "Usuario: ese nombre de usuario ya existe, incluso si está inactivo.");
        }
    }

    private static void validarConfirmacion(char[] clave, char[] confirmacion) {
        if (clave == null || clave.length < 8 || clave.length > 128) {
            throw new IllegalArgumentException("Contraseña: debe tener de 8 a 128 caracteres.");
        }
        if (!Arrays.equals(clave, confirmacion)) {
            throw new IllegalArgumentException("Confirmación: las contraseñas no coinciden.");
        }
    }

    private static boolean ultimoAdministradorActivo(List<Usuario> cuentas) {
        return cuentas.stream().filter(u -> u.esAdministrador() && u.estaActivo()).count() <= 1;
    }
}

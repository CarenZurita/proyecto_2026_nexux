package service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Rol;
import model.Usuario;
import persistence.UsuarioRepository;

/**
 * Servicio de autenticación de Usuarios: aplica las reglas de acceso y coordina
 * la inicialización de la cuenta administradora con el repositorio.
 */
public class AutenticacionService {
    private static final String USUARIO_INICIAL = "admin";
    private static final String CLAVE_INICIAL = "Farmacia2026!";

    private final UsuarioRepository repositorio;

    /**
     * Crea el servicio con el repositorio que comparte la composición de la aplicación.
     *
     * @param repositorio fuente y destino de las cuentas persistidas
     */
    public AutenticacionService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Crea el administrador inicial si aún no existe el archivo de cuentas.
     *
     * @throws IOException si no se puede leer o inicializar el archivo
     * @throws GeneralSecurityException si falla la protección de la clave inicial
     */
    public void inicializarAdministrador() throws IOException, GeneralSecurityException {
        // Serializa la comprobación y escritura para evitar dos inicializaciones simultáneas.
        synchronized (repositorio) {
            if (repositorio.existe()) {
                repositorio.listar();
                return;
            }
            char[] clave = CLAVE_INICIAL.toCharArray();
            try {
                List<Usuario> cuentas = new ArrayList<>();
                cuentas.add(Usuario.crear(USUARIO_INICIAL, "Administrador",
                        Rol.ADMINISTRADOR, clave));
                repositorio.guardarTodos(cuentas);
            } finally {
                // La contraseña inicial temporal no debe permanecer en memoria.
                Arrays.fill(clave, '\0');
            }
        }
    }

    /**
     * Autentica una cuenta activa por nombre normalizado y contraseña.
     *
     * @param nombre nombre de acceso ingresado
     * @param clave contraseña ingresada; el controlador limpia el arreglo al terminar el flujo
     * @return usuario con sesión abierta, o {@code null} si los datos no autentican
     * @throws IOException si falla la lectura del repositorio
     * @throws GeneralSecurityException si falla la verificación criptográfica
     */
    public Usuario iniciarSesion(String nombre, char[] clave)
            throws IOException, GeneralSecurityException {
        // Mantiene consistente la lectura con las escrituras concurrentes de cuentas.
        synchronized (repositorio) {
            if (nombre == null || nombre.isBlank() || clave == null || clave.length == 0) {
                return null;
            }
            Usuario encontrado = UsuarioServiceSupport.buscar(repositorio.listar(),
                    UsuarioServiceSupport.normalizarUsuario(nombre));
            if (encontrado != null && encontrado.estaActivo()
                    && encontrado.iniciarSesion(clave)) {
                return encontrado;
            }
            return null;
        }
    }

}

package service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import model.PermisoRecuperacion;
import model.Usuario;
import persistence.UsuarioRepository;

/**
 * Servicio de recuperación de cuentas: protege las respuestas, limita intentos
 * y emite permisos temporales de un solo uso para restablecer contraseñas.
 */
public class RecuperacionService {
    private final UsuarioRepository repositorio;
    private final Clock reloj;

    /**
     * Crea el servicio con el reloj del sistema.
     *
     * @param repositorio persistencia de cuentas
     */
    public RecuperacionService(UsuarioRepository repositorio) {
        this(repositorio, Clock.systemUTC());
    }

    RecuperacionService(UsuarioRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Configura por primera vez las respuestas de recuperación de la propia cuenta admin.
     *
     * @param sesion sesión administradora vigente
     * @param color respuesta a la pregunta de color
     * @param comida respuesta a la pregunta de comida
     * @throws IOException si falla la persistencia
     * @throws GeneralSecurityException si falla la protección de respuestas
     * @throws SecurityException si la sesión no es válida o quedó obsoleta
     * @throws IllegalStateException si la recuperación ya fue configurada
     * @throws IllegalArgumentException si una respuesta no cumple las reglas
     */
    public void configurarRecuperacion(Usuario sesion, char[] color, char[] comida)
            throws IOException, GeneralSecurityException {
        synchronized (repositorio) {
            if (sesion == null || !sesion.esAdministrador() || !sesion.estaActivo()
                    || !sesion.estaEnSesion()) {
                throw new SecurityException(
                        "Debes iniciar sesión como administrador para configurar las respuestas.");
            }
            List<Usuario> cuentas = repositorio.listar();
            Usuario cuenta = UsuarioServiceSupport.buscar(cuentas, sesion.getNombreUsuario());
            if (!UsuarioServiceSupport.administradorActivo(cuenta)
                    || !cuenta.mismaCredencial(sesion)) {
                throw new SecurityException("La sesión ya no es válida. Inicia sesión nuevamente.");
            }
            if (cuenta.tieneRecuperacion()) {
                throw new IllegalStateException("Las preguntas de esta cuenta ya están configuradas.");
            }
            cuenta.configurarRecuperacion(color, comida);
            repositorio.guardarTodos(cuentas);
            sesion.copiarRecuperacionDesde(cuenta);
        }
    }

    /**
     * Actualiza las respuestas de recuperación tras comprobar la contraseña actual.
     *
     * @param sesion sesión administradora vigente
     * @param actual contraseña actual
     * @param color nueva respuesta de color
     * @param comida nueva respuesta de comida
     * @throws IOException si falla la persistencia
     * @throws GeneralSecurityException si falla la protección criptográfica
     * @throws SecurityException si la sesión o contraseña actual no son válidas
     */
    public void actualizarMisPreguntas(Usuario sesion, char[] actual, char[] color, char[] comida)
            throws IOException, GeneralSecurityException {
        synchronized (repositorio) {
            List<Usuario> cuentas = repositorio.listar();
            Usuario cuenta = UsuarioServiceSupport.validarAdministrador(sesion, cuentas);
            if (!cuenta.comprobarClave(actual)) {
                throw new SecurityException("Contraseña actual incorrecta.");
            }
            cuenta.configurarRecuperacion(color, comida);
            repositorio.guardarTodos(cuentas);
            sesion.copiarRecuperacionDesde(cuenta);
        }
    }

    /**
     * Verifica las respuestas y emite permiso temporal si la cuenta está habilitada.
     *
     * @param nombre nombre de acceso de la cuenta
     * @param color respuesta de color
     * @param comida respuesta de comida
     * @return permiso de restablecimiento, o {@code null} si las respuestas no coinciden
     * @throws IOException si falla la lectura o el registro de intentos
     * @throws GeneralSecurityException si falla la verificación criptográfica
     * @throws IllegalArgumentException si las respuestas no cumplen las reglas de formato
     * @throws IllegalStateException si la cuenta está bloqueada tras agotar intentos
     */
    public PermisoRecuperacion verificarRecuperacion(String nombre, char[] color, char[] comida)
            throws IOException, GeneralSecurityException {
        // El bloqueo se persiste bajo el mismo monitor que la verificación para evitar carreras.
        synchronized (repositorio) {
            char[] colorNormal = Usuario.normalizarRespuesta(color);
            char[] comidaNormal = null;
            try {
                comidaNormal = Usuario.normalizarRespuesta(comida);
            } finally {
                // La verificación usa los arreglos originales; estas copias normalizadas se borran.
                Arrays.fill(colorNormal, '\0');
                if (comidaNormal != null) {
                    Arrays.fill(comidaNormal, '\0');
                }
            }
            List<Usuario> cuentas = repositorio.listar();
            Usuario cuenta = UsuarioServiceSupport.buscar(cuentas,
                    UsuarioServiceSupport.normalizarUsuario(nombre));
            if (!UsuarioServiceSupport.administradorActivo(cuenta)
                    || !cuenta.tieneRecuperacion()) {
                return null;
            }
            long ahora = reloj.millis();
            if (cuenta.getRecuperacionBloqueadaHasta() > ahora) {
                long segundos = (cuenta.getRecuperacionBloqueadaHasta() - ahora + 999) / 1000;
                throw new IllegalStateException("Espera " + segundos
                        + " segundos antes de intentar recuperar la contraseña.");
            }
            if (!cuenta.comprobarRespuestas(color, comida)) {
                // El contador persistido aplica el límite de tres fallos y la pausa de 30 segundos.
                boolean bloqueado = cuenta.registrarFalloRecuperacion(ahora);
                repositorio.guardarTodos(cuentas);
                if (bloqueado) {
                    throw new IllegalStateException(
                            "Se agotaron los 3 intentos. Espera 30 segundos para volver a intentarlo.");
                }
                return null;
            }
            cuenta.limpiarIntentosRecuperacion();
            repositorio.guardarTodos(cuentas);
            return new PermisoRecuperacion(cuenta, reloj.millis());
        }
    }

    /**
     * Restablece una contraseña usando un permiso vigente y todavía no utilizado.
     *
     * @param permiso permiso emitido tras verificar las respuestas
     * @param nueva contraseña nueva de 8 a 128 caracteres
     * @param confirmacion repetición exacta de la contraseña
     * @throws IOException si falla la persistencia
     * @throws GeneralSecurityException si falla la protección criptográfica
     * @throws SecurityException si el permiso venció, se usó o la cuenta cambió
     * @throws IllegalArgumentException si la contraseña o confirmación no es válida
     */
    public void restablecerContrasena(PermisoRecuperacion permiso, char[] nueva, char[] confirmacion)
            throws IOException, GeneralSecurityException {
        synchronized (repositorio) {
            long ahora = reloj.millis();
            if (permiso == null || !permiso.estaVigente(ahora)) {
                throw new SecurityException(
                        "Verifica nuevamente las preguntas para restablecer la contraseña.");
            }
            if (nueva == null || nueva.length < 8 || nueva.length > 128) {
                throw new IllegalArgumentException("La contraseña debe tener de 8 a 128 caracteres.");
            }
            if (!Arrays.equals(nueva, confirmacion)) {
                throw new IllegalArgumentException("Las contraseñas no coinciden.");
            }
            List<Usuario> cuentas = repositorio.listar();
            Usuario cuenta = cuentas.stream()
                    .filter(u -> permiso.esVigentePara(u, ahora))
                    .findFirst()
                    .orElse(null);
            if (!UsuarioServiceSupport.administradorActivo(cuenta)
                    || !cuenta.tieneRecuperacion()) {
                throw new SecurityException("La cuenta cambió. Verifica nuevamente las preguntas.");
            }
            cuenta.cambiarClave(nueva);
            cuenta.cerrarSesion();
            cuenta.limpiarIntentosRecuperacion();
            repositorio.guardarTodos(cuentas);
            permiso.marcarUsado();
        }
    }

}

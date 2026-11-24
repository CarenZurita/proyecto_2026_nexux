package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import model.PermisoRecuperacion;
import model.Rol;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.UsuarioRepository;

class RecuperacionServiceTest {
    private static final long INICIO = 1_000L;
    private static final long DURACION_PERMISO = 5 * 60_000L;
    private static final char[] CLAVE_INICIAL = "Farmacia2026!".toCharArray();
    private static final char[] RESPUESTA_COLOR = "azul".toCharArray();
    private static final char[] RESPUESTA_COMIDA = "arroz".toCharArray();

    @TempDir
    Path directorioTemporal;

    private UsuarioRepository repositorio;
    private AutenticacionService autenticacionService;
    private RecuperacionService recuperacionService;

    @BeforeEach
    void crearServiciosConRepositorioTemporal() throws IOException, GeneralSecurityException {
        repositorio = new UsuarioRepository(directorioTemporal.resolve("usuarios.txt"));
        autenticacionService = new AutenticacionService(repositorio);
        recuperacionService = crearServicio(repositorio, INICIO);
        autenticacionService.inicializarAdministrador();
    }

    @Test
    void configurarRecuperacionPersisteLasRespuestas()
            throws IOException, GeneralSecurityException {
        Usuario administrador = iniciarSesionAdministrador();

        recuperacionService.configurarRecuperacion(
                administrador, RESPUESTA_COLOR, RESPUESTA_COMIDA);

        Usuario cuentaGuardada = buscarAdministrador(repositorio);
        assertTrue(cuentaGuardada.tieneRecuperacion());
    }

    @Test
    void actualizarMisPreguntasReemplazaLasRespuestas()
            throws IOException, GeneralSecurityException {
        Usuario administrador = configurarRecuperacion();

        recuperacionService.actualizarMisPreguntas(administrador, CLAVE_INICIAL,
                "verde".toCharArray(), "pizza".toCharArray());

        assertNull(recuperacionService.verificarRecuperacion(
                "admin", RESPUESTA_COLOR, RESPUESTA_COMIDA));
        assertNotNull(recuperacionService.verificarRecuperacion(
                "admin", "verde".toCharArray(), "pizza".toCharArray()));
    }

    @Test
    void verificarRecuperacionIncorrectaAntesDelLimiteDevuelveNull()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();

        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
    }

    @Test
    void verificarRecuperacionCorrectaDevuelvePermiso()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();

        assertNotNull(recuperacionService.verificarRecuperacion(
                "admin", RESPUESTA_COLOR, RESPUESTA_COMIDA));
    }

    @Test
    void verificarRecuperacionBloqueaAlAgotarTresIntentos()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> recuperacionService.verificarRecuperacion(
                        "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));

        assertEquals("Se agotaron los 3 intentos. Espera 30 segundos para volver a intentarlo.",
                error.getMessage());
        assertThrows(IllegalStateException.class,
                () -> recuperacionService.verificarRecuperacion(
                        "admin", RESPUESTA_COLOR, RESPUESTA_COMIDA));
    }

    @Test
    void verificacionCorrectaReiniciaLosIntentos()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
        assertNotNull(recuperacionService.verificarRecuperacion(
                "admin", RESPUESTA_COLOR, RESPUESTA_COMIDA));

        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
        assertThrows(IllegalStateException.class,
                () -> recuperacionService.verificarRecuperacion(
                        "admin", "rojo".toCharArray(), RESPUESTA_COMIDA));
    }

    @Test
    void permisoDeRecuperacionSoloPuedeUsarseUnaVez()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        char[] nuevaClave = "NuevaClave123".toCharArray();

        recuperacionService.restablecerContrasena(permiso, nuevaClave, nuevaClave);

        assertNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
        assertThrows(SecurityException.class,
                () -> recuperacionService.restablecerContrasena(
                        permiso, "OtraClave123".toCharArray(), "OtraClave123".toCharArray()));
    }

    @Test
    void permisoEsValidoUnMilisegundoAntesDelVencimiento()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        RecuperacionService unMilisegundoAntes =
                crearServicio(repositorio, INICIO + DURACION_PERMISO - 1);
        char[] nuevaClave = "NuevaClave123".toCharArray();

        unMilisegundoAntes.restablecerContrasena(permiso, nuevaClave, nuevaClave);

        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
    }

    @Test
    void permisoExpiraExactamenteEnElLimite()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        RecuperacionService enLimite =
                crearServicio(repositorio, INICIO + DURACION_PERMISO);

        SecurityException error = assertThrows(SecurityException.class,
                () -> enLimite.restablecerContrasena(
                        permiso, "NuevaClave123".toCharArray(), "NuevaClave123".toCharArray()));

        assertEquals("Verifica nuevamente las preguntas para restablecer la contraseña.",
                error.getMessage());
    }

    @Test
    void permisoSeInvalidaSiLaCuentaCambiaDespuesDeVerificar()
            throws IOException, GeneralSecurityException {
        Usuario administrador = configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        char[] nuevaClave = "ClaveActualizada123".toCharArray();
        new GestionUsuariosService(repositorio).cambiarMiClave(
                administrador, CLAVE_INICIAL, nuevaClave, nuevaClave);

        SecurityException error = assertThrows(SecurityException.class,
                () -> recuperacionService.restablecerContrasena(
                        permiso, "OtraClave123".toCharArray(), "OtraClave123".toCharArray()));

        assertEquals("La cuenta cambió. Verifica nuevamente las preguntas.", error.getMessage());
    }

    @Test
    void permisoDeOtraCuentaNoPuedeRestablecerLaClave()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        Usuario otraCuenta = Usuario.crear(
                "admin2", "Otro administrador", Rol.ADMINISTRADOR,
                "ClaveSegura123".toCharArray());
        Path rutaOtraCuenta = directorioTemporal.resolve("otra-cuenta").resolve("usuarios.txt");
        UsuarioRepository otroRepositorio = new UsuarioRepository(rutaOtraCuenta);
        otroRepositorio.guardarTodos(List.of(otraCuenta));
        AutenticacionService otraAutenticacion =
                new AutenticacionService(otroRepositorio);
        Usuario otraSesion = otraAutenticacion.iniciarSesion(
                "admin2", "ClaveSegura123".toCharArray());
        RecuperacionService otraRecuperacion = crearServicio(otroRepositorio, INICIO);
        otraRecuperacion.configurarRecuperacion(
                otraSesion, RESPUESTA_COLOR, RESPUESTA_COMIDA);

        assertThrows(SecurityException.class,
                () -> otraRecuperacion.restablecerContrasena(
                        permiso, "OtraClave123".toCharArray(), "OtraClave123".toCharArray()));
    }

    @Test
    void contrasenaInvalidaNoConsumeElPermiso()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        char[] invalida = "corta".toCharArray();

        assertThrows(IllegalArgumentException.class,
                () -> recuperacionService.restablecerContrasena(permiso, invalida, invalida));
        char[] nuevaClave = "NuevaClave123".toCharArray();
        recuperacionService.restablecerContrasena(permiso, nuevaClave, nuevaClave);

        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
    }

    @Test
    void confirmacionDistintaNoConsumeElPermiso()
            throws IOException, GeneralSecurityException {
        configurarRecuperacion();
        PermisoRecuperacion permiso = verificarCorrectamente();
        char[] nuevaClave = "NuevaClave123".toCharArray();

        assertThrows(IllegalArgumentException.class,
                () -> recuperacionService.restablecerContrasena(
                        permiso, nuevaClave, "OtraClave123".toCharArray()));
        recuperacionService.restablecerContrasena(permiso, nuevaClave, nuevaClave);

        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
    }

    private Usuario iniciarSesionAdministrador()
            throws IOException, GeneralSecurityException {
        return autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
    }

    private Usuario configurarRecuperacion()
            throws IOException, GeneralSecurityException {
        Usuario administrador = iniciarSesionAdministrador();
        recuperacionService.configurarRecuperacion(
                administrador, RESPUESTA_COLOR, RESPUESTA_COMIDA);
        return administrador;
    }

    private PermisoRecuperacion verificarCorrectamente()
            throws IOException, GeneralSecurityException {
        return recuperacionService.verificarRecuperacion(
                "admin", RESPUESTA_COLOR, RESPUESTA_COMIDA);
    }

    private Usuario buscarAdministrador(UsuarioRepository usuarioRepository) throws IOException {
        return usuarioRepository.listar().stream()
                .filter(cuenta -> cuenta.getNombreUsuario().equals("admin"))
                .findFirst().orElseThrow();
    }

    private RecuperacionService crearServicio(UsuarioRepository usuarioRepository, long millis) {
        Clock reloj = Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
        return new RecuperacionService(usuarioRepository, reloj);
    }
}

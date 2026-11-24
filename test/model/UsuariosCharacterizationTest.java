package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.UsuarioRepository;
import service.AutenticacionService;
import service.GestionUsuariosService;
import service.RecuperacionService;

class UsuariosCharacterizationTest {
    private static final char[] CLAVE_INICIAL = "Farmacia2026!".toCharArray();
    private static final char[] CLAVE_ENCARGADO = "ClaveSegura123".toCharArray();

    @TempDir
    Path directorioTemporal;

    private Path archivoUsuarios;

    private UsuarioRepository repositorio;
    private AutenticacionService autenticacionService;
    private GestionUsuariosService gestionUsuariosService;
    private RecuperacionService recuperacionService;

    @BeforeEach
    void configurarRepositorioTemporal() {
        archivoUsuarios = directorioTemporal.resolve("usuarios.txt");
        repositorio = new UsuarioRepository(archivoUsuarios);
        autenticacionService = new AutenticacionService(repositorio);
        gestionUsuariosService = new GestionUsuariosService(repositorio);
        recuperacionService = new RecuperacionService(repositorio);
    }

    @Test
    void inicioSesionConUsuarioInexistenteDevuelveNull()
            throws IOException, GeneralSecurityException {
        inicializarAdministrador();

        assertNull(autenticacionService.iniciarSesion("inexistente", CLAVE_INICIAL));
    }

    @Test
    void inicioSesionCorrectoDevuelveCuenta()
            throws IOException, GeneralSecurityException {
        inicializarAdministrador();

        assertNotNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
    }

    @Test
    void inicioSesionConClaveIncorrectaDevuelveNull()
            throws IOException, GeneralSecurityException {
        inicializarAdministrador();

        assertNull(autenticacionService.iniciarSesion(
                "admin", "ClaveIncorrecta123".toCharArray()));
    }

    @Test
    void inicioSesionConCuentaDeshabilitadaDevuelveNull()
            throws IOException, GeneralSecurityException {
        inicializarAdministrador();
        Usuario deshabilitado = Usuario.crear(
                "inactivo", "Cuenta inactiva", Rol.ENCARGADO, CLAVE_ENCARGADO);
        deshabilitado.asignarId(2);
        deshabilitado.setActivo(false);
        Files.writeString(archivoUsuarios, deshabilitado.aLinea() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);

        assertNull(autenticacionService.iniciarSesion("inactivo", CLAVE_ENCARGADO));
    }

    @Test
    void inicioSesionConCamposVaciosDevuelveNullSinLeerElArchivo()
            throws IOException, GeneralSecurityException {
        assertNull(autenticacionService.iniciarSesion(null, CLAVE_INICIAL));
        assertNull(autenticacionService.iniciarSesion("  ", CLAVE_INICIAL));
        assertNull(autenticacionService.iniciarSesion("admin", new char[0]));
        assertFalse(Files.exists(archivoUsuarios));
    }

    @Test
    void listarUsuariosDevuelveDatosPublicosEIncluyeCuentasInactivas()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        gestionUsuariosService.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int idEncargada = repositorio.listar().stream()
                .filter(cuenta -> cuenta.getNombreUsuario().equals("encargada1"))
                .findFirst().orElseThrow().getId();
        gestionUsuariosService.cambiarEstadoUsuario(administrador, idEncargada, false);

        var fichas = gestionUsuariosService.listarUsuarios(administrador);

        assertEquals(2, fichas.size());
        FichaUsuario ficha = fichas.stream()
                .filter(cuenta -> cuenta.usuario().equals("encargada1"))
                .findFirst().orElseThrow();
        assertEquals(idEncargada, ficha.id());
        assertEquals("Encargada", ficha.nombre());
        assertEquals(Rol.ENCARGADO, ficha.rol());
        assertFalse(ficha.activo());
    }

    @Test
    void listarUsuariosRechazaSesionNula()
            throws IOException, GeneralSecurityException {
        inicializarAdministrador();

        SecurityException error = assertThrows(SecurityException.class,
                () -> gestionUsuariosService.listarUsuarios(null));

        assertEquals("Se requiere una sesión de administrador.", error.getMessage());
    }

    @Test
    void listarUsuariosRechazaSesionDeEncargado()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        gestionUsuariosService.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        Usuario encargado = autenticacionService.iniciarSesion("encargada1", CLAVE_ENCARGADO);

        SecurityException error = assertThrows(SecurityException.class,
                () -> gestionUsuariosService.listarUsuarios(encargado));

        assertEquals("Se requiere una sesión de administrador.", error.getMessage());
    }

    @Test
    void listarUsuariosRechazaSesionAdministrativaDesactualizada()
            throws IOException, GeneralSecurityException {
        Usuario administradorActual = inicializarAdministrador();
        Usuario administradorDesactualizado = autenticacionService.iniciarSesion(
                "admin", CLAVE_INICIAL);
        char[] nuevaClave = "NuevaClave123".toCharArray();
        gestionUsuariosService.actualizarUsuario(administradorActual, administradorActual.getId(),
                "Administrador", "admin", Rol.ADMINISTRADOR, nuevaClave, nuevaClave);

        SecurityException error = assertThrows(SecurityException.class,
                () -> gestionUsuariosService.listarUsuarios(administradorDesactualizado));

        assertEquals("La sesión cambió. Vuelve a iniciar sesión.", error.getMessage());
    }

    @Test
    void operacionesDeGestionRechazanSesionNula()
            throws IOException, GeneralSecurityException {
        inicializarAdministrador();
        Usuario encargado = registrarEncargado();

        assertThrows(SecurityException.class, () -> gestionUsuariosService.registrarUsuario(
                null, "Nuevo", "nuevo1", Rol.ENCARGADO,
                CLAVE_ENCARGADO, CLAVE_ENCARGADO));
        assertThrows(SecurityException.class, () -> gestionUsuariosService.actualizarUsuario(
                null, encargado.getId(), "Actualizado", "encargado1",
                Rol.ENCARGADO, new char[0], new char[0]));
        assertThrows(SecurityException.class, () -> gestionUsuariosService.cambiarEstadoUsuario(
                null, encargado.getId(), false));
    }

    @Test
    void operacionesDeGestionRechazanSesionDeEncargado()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        gestionUsuariosService.registrarUsuario(administrador, "Encargado",
                "encargado1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        Usuario encargado = autenticacionService.iniciarSesion("encargado1", CLAVE_ENCARGADO);
        int idEncargado = gestionUsuariosService.listarUsuarios(administrador).stream()
                .filter(ficha -> ficha.usuario().equals("encargado1"))
                .findFirst().orElseThrow().id();

        assertThrows(SecurityException.class, () -> gestionUsuariosService.registrarUsuario(
                encargado, "Nuevo", "nuevo1", Rol.ENCARGADO,
                CLAVE_ENCARGADO, CLAVE_ENCARGADO));
        assertThrows(SecurityException.class, () -> gestionUsuariosService.actualizarUsuario(
                encargado, idEncargado, "Actualizado", "encargado2",
                Rol.ENCARGADO, new char[0], new char[0]));
        assertThrows(SecurityException.class, () -> gestionUsuariosService.cambiarEstadoUsuario(
                encargado, idEncargado, false));
    }

    @Test
    void operacionesDeGestionRechazanSesionAdministrativaDesactualizada()
            throws IOException, GeneralSecurityException {
        Usuario administradorActual = inicializarAdministrador();
        Usuario administradorDesactualizado = autenticacionService.iniciarSesion(
                "admin", CLAVE_INICIAL);
        Usuario encargado = registrarEncargado();

        gestionUsuariosService.actualizarUsuario(administradorActual, administradorActual.getId(),
                "Administrador", "admin", Rol.ADMINISTRADOR,
                "NuevaClave123".toCharArray(), "NuevaClave123".toCharArray());

        assertThrows(SecurityException.class, () -> gestionUsuariosService.registrarUsuario(
                administradorDesactualizado, "Nuevo", "nuevo1", Rol.ENCARGADO,
                CLAVE_ENCARGADO, CLAVE_ENCARGADO));
        assertThrows(SecurityException.class, () -> gestionUsuariosService.actualizarUsuario(
                administradorDesactualizado, encargado.getId(), "Actualizado", "encargado2",
                Rol.ENCARGADO, new char[0], new char[0]));
        assertThrows(SecurityException.class, () -> gestionUsuariosService.cambiarEstadoUsuario(
                administradorDesactualizado, encargado.getId(), false));
    }

    @Test
    void registrarUsuarioRechazaNombreDuplicado()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        gestionUsuariosService.registrarUsuario(administrador, "Encargado",
                "encargado1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gestionUsuariosService.registrarUsuario(administrador, "Otro nombre",
                        "ENCARGADO1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO));

        assertTrue(error.getMessage().contains("ya existe"));
    }

    @Test
    void registrarUsuarioRechazaConfirmacionDistinta()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();

        assertThrows(IllegalArgumentException.class, () -> gestionUsuariosService.registrarUsuario(
                administrador, "Encargado", "encargado1", Rol.ENCARGADO,
                CLAVE_ENCARGADO, "OtraClave123".toCharArray()));
        assertFalse(gestionUsuariosService.listarUsuarios(administrador).stream()
                .anyMatch(ficha -> ficha.usuario().equals("encargado1")));
    }

    @Test
    void actualizarUsuarioRechazaNombreDuplicado()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        gestionUsuariosService.registrarUsuario(administrador, "Encargado 1",
                "encargado1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        gestionUsuariosService.registrarUsuario(administrador, "Encargado 2",
                "encargado2", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int idEncargado1 = gestionUsuariosService.listarUsuarios(administrador).stream()
                .filter(ficha -> ficha.usuario().equals("encargado1"))
                .findFirst().orElseThrow().id();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gestionUsuariosService.actualizarUsuario(administrador, idEncargado1,
                        "Encargado actualizado", "encargado2", Rol.ENCARGADO,
                        new char[0], new char[0]));

        assertTrue(error.getMessage().contains("ya existe"));
    }

    @Test
    void actualizarUsuarioRechazaConfirmacionDistinta()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        gestionUsuariosService.registrarUsuario(administrador, "Encargado",
                "encargado1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int idEncargado = gestionUsuariosService.listarUsuarios(administrador).stream()
                .filter(ficha -> ficha.usuario().equals("encargado1"))
                .findFirst().orElseThrow().id();

        assertThrows(IllegalArgumentException.class,
                () -> gestionUsuariosService.actualizarUsuario(administrador, idEncargado,
                        "Encargado", "encargado1", Rol.ENCARGADO,
                        "NuevaClave123".toCharArray(), "OtraClave123".toCharArray()));
        assertNotNull(autenticacionService.iniciarSesion("encargado1", CLAVE_ENCARGADO));
    }

    @Test
    void cambiarMiClaveRechazaClaveActualIncorrecta()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();

        assertThrows(SecurityException.class, () -> gestionUsuariosService.cambiarMiClave(
                administrador, "ClaveIncorrecta".toCharArray(),
                "NuevaClave123".toCharArray(), "NuevaClave123".toCharArray()));
        assertNotNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
    }

    @Test
    void cambiarMiClaveCorrectaActualizaLaClave()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        char[] nuevaClave = "NuevaClave123".toCharArray();

        gestionUsuariosService.cambiarMiClave(
                administrador, CLAVE_INICIAL, nuevaClave, nuevaClave);

        assertNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
    }

    @Test
    void actualizarMisPreguntasReemplazaLasRespuestasRegistradas()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        recuperacionService.configurarRecuperacion(administrador,
                "azul".toCharArray(), "arroz".toCharArray());

        recuperacionService.actualizarMisPreguntas(administrador, CLAVE_INICIAL,
                "verde".toCharArray(), "pizza".toCharArray());

        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "azul".toCharArray(), "arroz".toCharArray()));
        assertNotNull(recuperacionService.verificarRecuperacion(
                "admin", "verde".toCharArray(), "pizza".toCharArray()));
    }

    @Test
    void configurarRecuperacionPersisteLasRespuestas()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();

        recuperacionService.configurarRecuperacion(administrador,
                "azul".toCharArray(), "arroz".toCharArray());

        Usuario cuentaGuardada = repositorio.listar().stream()
                .filter(cuenta -> cuenta.getNombreUsuario().equals("admin"))
                .findFirst().orElseThrow();
        assertTrue(cuentaGuardada.tieneRecuperacion());
    }

    @Test
    void respuestasIncorrectasAgotanIntentosYBloqueanLaRecuperacion()
            throws IOException, GeneralSecurityException {
        Usuario administrador = inicializarAdministrador();
        recuperacionService.configurarRecuperacion(administrador,
                "azul".toCharArray(), "arroz".toCharArray());

        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), "arroz".toCharArray()));
        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), "arroz".toCharArray()));
        assertThrows(IllegalStateException.class, () -> recuperacionService.verificarRecuperacion(
                "admin", "rojo".toCharArray(), "arroz".toCharArray()));
        assertThrows(IllegalStateException.class, () -> recuperacionService.verificarRecuperacion(
                "admin", "azul".toCharArray(), "arroz".toCharArray()));
    }

    @Test
    void inicializarAdministradorConArchivoExistenteNoLoReemplaza()
            throws IOException, GeneralSecurityException {
        Usuario existente = Usuario.crear(
                "dueno1", "Dueño", Rol.ADMINISTRADOR, "ClaveExistente123".toCharArray());
        existente.asignarId(5);
        String contenido = existente.aLinea() + System.lineSeparator();
        Files.writeString(archivoUsuarios, contenido, StandardCharsets.UTF_8);

        autenticacionService.inicializarAdministrador();

        assertEquals(contenido, Files.readString(archivoUsuarios, StandardCharsets.UTF_8));
        assertNotNull(autenticacionService.iniciarSesion(
                "dueno1", "ClaveExistente123".toCharArray()));
        assertNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
    }

    private Usuario inicializarAdministrador()
            throws IOException, GeneralSecurityException {
        autenticacionService.inicializarAdministrador();
        return autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
    }

    private Usuario registrarEncargado()
            throws IOException, GeneralSecurityException {
        Usuario administrador = autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
        gestionUsuariosService.registrarUsuario(administrador, "Encargado",
                "encargado1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        return autenticacionService.iniciarSesion("encargado1", CLAVE_ENCARGADO);
    }
}

package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import model.Rol;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.UsuarioRepository;

class AutenticacionServiceTest {
    private static final char[] CLAVE_INICIAL = "Farmacia2026!".toCharArray();

    @TempDir
    Path directorioTemporal;

    private Path archivoUsuarios;
    private AutenticacionService servicio;

    @BeforeEach
    void crearServicioConRepositorioTemporal() {
        archivoUsuarios = directorioTemporal.resolve("usuarios.txt");
        servicio = new AutenticacionService(new UsuarioRepository(archivoUsuarios));
    }

    @Test
    void inicializarAdministradorCreaCuentaEnArchivoInexistente()
            throws IOException, GeneralSecurityException {
        servicio.inicializarAdministrador();

        assertTrue(Files.exists(archivoUsuarios));
        assertNotNull(servicio.iniciarSesion("admin", CLAVE_INICIAL));
    }

    @Test
    void inicializarAdministradorConArchivoExistenteConservaContenido()
            throws IOException, GeneralSecurityException {
        Usuario existente = Usuario.crear(
                "dueno1", "Dueño", Rol.ADMINISTRADOR, "ClaveExistente123".toCharArray());
        existente.asignarId(5);
        String contenido = existente.aLinea() + System.lineSeparator();
        Files.writeString(archivoUsuarios, contenido, StandardCharsets.UTF_8);

        servicio.inicializarAdministrador();

        assertEquals(contenido, Files.readString(archivoUsuarios, StandardCharsets.UTF_8));
        assertNotNull(servicio.iniciarSesion(
                "dueno1", "ClaveExistente123".toCharArray()));
        assertNull(servicio.iniciarSesion("admin", CLAVE_INICIAL));
    }

    @Test
    void iniciarSesionConCredencialesCorrectasDevuelveCuenta()
            throws IOException, GeneralSecurityException {
        servicio.inicializarAdministrador();

        assertNotNull(servicio.iniciarSesion("admin", CLAVE_INICIAL));
    }

    @Test
    void iniciarSesionConClaveIncorrectaDevuelveNull()
            throws IOException, GeneralSecurityException {
        servicio.inicializarAdministrador();

        assertNull(servicio.iniciarSesion("admin", "ClaveIncorrecta123".toCharArray()));
    }

    @Test
    void iniciarSesionConUsuarioInexistenteDevuelveNull()
            throws IOException, GeneralSecurityException {
        servicio.inicializarAdministrador();

        assertNull(servicio.iniciarSesion("inexistente", CLAVE_INICIAL));
    }

    @Test
    void iniciarSesionConCuentaDeshabilitadaDevuelveNull()
            throws IOException, GeneralSecurityException {
        servicio.inicializarAdministrador();
        Usuario deshabilitado = Usuario.crear(
                "inactivo", "Cuenta inactiva", Rol.ENCARGADO,
                "ClaveSegura123".toCharArray());
        deshabilitado.asignarId(2);
        String linea = deshabilitado.aLinea().replace(";true;", ";false;");
        Files.writeString(archivoUsuarios, linea + System.lineSeparator(),
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);

        assertNull(servicio.iniciarSesion("inactivo", "ClaveSegura123".toCharArray()));
    }

    @Test
    void iniciarSesionConCamposVaciosDevuelveNullSinLeerElArchivo()
            throws IOException, GeneralSecurityException {
        assertNull(servicio.iniciarSesion(null, CLAVE_INICIAL));
        assertNull(servicio.iniciarSesion("  ", CLAVE_INICIAL));
        assertNull(servicio.iniciarSesion("admin", new char[0]));
        assertFalse(Files.exists(archivoUsuarios));
    }

}

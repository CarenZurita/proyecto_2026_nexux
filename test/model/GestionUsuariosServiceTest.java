package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.UsuarioRepository;
import service.AutenticacionService;
import service.GestionUsuariosService;

class GestionUsuariosServiceTest {
    private static final char[] CLAVE_INICIAL = "Farmacia2026!".toCharArray();
    private static final char[] CLAVE_ENCARGADO = "ClaveSegura123".toCharArray();

    @TempDir
    Path directorioTemporal;

    private UsuarioRepository repositorio;
    private AutenticacionService autenticacionService;
    private GestionUsuariosService servicio;
    private Usuario administrador;

    @BeforeEach
    void crearServicioConRepositorioTemporal()
            throws IOException, GeneralSecurityException {
        repositorio = new UsuarioRepository(directorioTemporal.resolve("usuarios.txt"));
        autenticacionService = new AutenticacionService(repositorio);
        servicio = new GestionUsuariosService(repositorio);
        autenticacionService.inicializarAdministrador();
        administrador = autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
    }

    @Test
    void registraUsuarioConNombreRolYEstadoActivo()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);

        Usuario guardado = repositorio.listar().stream()
                .filter(cuenta -> cuenta.getNombreUsuario().equals("encargada1"))
                .findFirst().orElseThrow();
        assertEquals("Encargada", guardado.getNombreCompleto());
        assertEquals(Rol.ENCARGADO, guardado.getRol());
        assertTrue(guardado.estaActivo());
    }

    @Test
    void actualizaUsuarioSinCambiarLaSesionAdministrativa()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");

        servicio.actualizarUsuario(administrador, id, "Nombre actualizado",
                "encargada2", Rol.ENCARGADO, new char[0], new char[0]);

        Usuario guardado = repositorio.listar().stream()
                .filter(cuenta -> cuenta.getId() == id)
                .findFirst().orElseThrow();
        assertEquals("Nombre actualizado", guardado.getNombreCompleto());
        assertEquals("encargada2", guardado.getNombreUsuario());
        assertEquals(Rol.ENCARGADO, guardado.getRol());
        assertTrue(guardado.estaActivo());
        assertTrue(administrador.estaEnSesion());
    }

    @Test
    void cambiaEstadoDeUsuario()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");

        servicio.cambiarEstadoUsuario(administrador, id, false);

        Usuario guardado = repositorio.listar().stream()
                .filter(cuenta -> cuenta.getId() == id)
                .findFirst().orElseThrow();
        assertFalse(guardado.estaActivo());
    }

    @Test
    void listarUsuariosDevuelveFichaDeLaCuenta()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");

        List<FichaUsuario> fichas = servicio.listarUsuarios(administrador);

        FichaUsuario ficha = fichas.stream()
                .filter(cuenta -> cuenta.usuario().equals("encargada1"))
                .findFirst().orElseThrow();
        assertEquals(id, ficha.id());
        assertEquals("Encargada", ficha.nombre());
        assertEquals("encargada1", ficha.usuario());
        assertEquals(Rol.ENCARGADO, ficha.rol());
        assertTrue(ficha.activo());
    }

    @Test
    void listarUsuariosIncluyeCuentasInactivas()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");
        servicio.cambiarEstadoUsuario(administrador, id, false);

        FichaUsuario ficha = servicio.listarUsuarios(administrador).stream()
                .filter(cuenta -> cuenta.id() == id)
                .findFirst().orElseThrow();

        assertEquals("encargada1", ficha.usuario());
        assertFalse(ficha.activo());
    }

    @Test
    void listarUsuariosRechazaSesionNula() {
        SecurityException error = assertThrows(SecurityException.class,
                () -> servicio.listarUsuarios(null));

        assertEquals("Se requiere una sesión de administrador.", error.getMessage());
    }

    @Test
    void listarUsuariosRechazaSesionDeEncargado()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        Usuario encargado = autenticacionService.iniciarSesion("encargada1", CLAVE_ENCARGADO);

        SecurityException error = assertThrows(SecurityException.class,
                () -> servicio.listarUsuarios(encargado));

        assertEquals("Se requiere una sesión de administrador.", error.getMessage());
    }

    @Test
    void listarUsuariosRechazaSesionAdministrativaDesactualizada()
            throws IOException, GeneralSecurityException {
        Usuario desactualizado = autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
        char[] nuevaClave = "NuevaClave123".toCharArray();
        servicio.actualizarUsuario(administrador, administrador.getId(),
                "Administrador", "admin", Rol.ADMINISTRADOR, nuevaClave, nuevaClave);

        SecurityException error = assertThrows(SecurityException.class,
                () -> servicio.listarUsuarios(desactualizado));

        assertEquals("La sesión cambió. Vuelve a iniciar sesión.", error.getMessage());
    }

    @Test
    void operacionesDeGestionRechazanSesionNula()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");

        assertThrows(SecurityException.class, () -> servicio.registrarUsuario(
                null, "Otra", "otra1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO));
        assertThrows(SecurityException.class, () -> servicio.actualizarUsuario(
                null, id, "Actualizada", "encargada2", Rol.ENCARGADO,
                new char[0], new char[0]));
        assertThrows(SecurityException.class,
                () -> servicio.cambiarEstadoUsuario(null, id, false));
    }

    @Test
    void operacionesDeGestionRechazanSesionDeEncargado()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");
        Usuario encargado = autenticacionService.iniciarSesion("encargada1", CLAVE_ENCARGADO);

        assertThrows(SecurityException.class, () -> servicio.registrarUsuario(
                encargado, "Otra", "otra1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO));
        assertThrows(SecurityException.class, () -> servicio.actualizarUsuario(
                encargado, id, "Actualizada", "encargada2", Rol.ENCARGADO,
                new char[0], new char[0]));
        assertThrows(SecurityException.class,
                () -> servicio.cambiarEstadoUsuario(encargado, id, false));
    }

    @Test
    void operacionesDeGestionRechazanSesionAdministrativaDesactualizada()
            throws IOException, GeneralSecurityException {
        Usuario desactualizado = autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");
        char[] nuevaClave = "NuevaClave123".toCharArray();
        servicio.actualizarUsuario(administrador, administrador.getId(),
                "Administrador", "admin", Rol.ADMINISTRADOR, nuevaClave, nuevaClave);

        assertThrows(SecurityException.class, () -> servicio.registrarUsuario(
                desactualizado, "Otra", "otra1", Rol.ENCARGADO,
                CLAVE_ENCARGADO, CLAVE_ENCARGADO));
        assertThrows(SecurityException.class, () -> servicio.actualizarUsuario(
                desactualizado, id, "Actualizada", "encargada2", Rol.ENCARGADO,
                new char[0], new char[0]));
        assertThrows(SecurityException.class,
                () -> servicio.cambiarEstadoUsuario(desactualizado, id, false));
    }

    @Test
    void rechazaRegistroDeUsuarioDuplicado()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarUsuario(administrador, "Otra encargada",
                        "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO));
    }

    @Test
    void rechazaRegistroConConfirmacionDistintaSinPersistirUsuario()
            throws IOException, GeneralSecurityException {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarUsuario(
                administrador, "Encargada", "encargada1", Rol.ENCARGADO,
                CLAVE_ENCARGADO, "OtraClave123".toCharArray()));

        assertFalse(repositorio.listar().stream()
                .anyMatch(cuenta -> cuenta.getNombreUsuario().equals("encargada1")));
    }

    @Test
    void rechazaActualizacionConNombreDeUsuarioDuplicado()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada 1",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        servicio.registrarUsuario(administrador, "Encargada 2",
                "encargada2", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarUsuario(
                administrador, id, "Actualizada", "encargada2", Rol.ENCARGADO,
                new char[0], new char[0]));
    }

    @Test
    void rechazaActualizacionConConfirmacionDistintaYConservaClaveAnterior()
            throws IOException, GeneralSecurityException {
        servicio.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO, CLAVE_ENCARGADO, CLAVE_ENCARGADO);
        int id = buscarId("encargada1");

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarUsuario(
                administrador, id, "Encargada", "encargada1", Rol.ENCARGADO,
                "NuevaClave123".toCharArray(), "OtraClave123".toCharArray()));

        assertNotNull(autenticacionService.iniciarSesion("encargada1", CLAVE_ENCARGADO));
    }

    @Test
    void cambiaMiClaveYConservaLaSesion()
            throws IOException, GeneralSecurityException {
        char[] nuevaClave = "NuevaClave123".toCharArray();

        servicio.cambiarMiClave(administrador, CLAVE_INICIAL, nuevaClave, nuevaClave);

        assertTrue(administrador.estaEnSesion());
        assertNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
    }

    @Test
    void rechazaCambioDeClaveConClaveActualIncorrecta()
            throws IOException, GeneralSecurityException {
        assertThrows(SecurityException.class, () -> servicio.cambiarMiClave(
                administrador, "ClaveIncorrecta".toCharArray(),
                "NuevaClave123".toCharArray(), "NuevaClave123".toCharArray()));
        assertNotNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
    }

    private int buscarId(String usuario) throws IOException {
        return repositorio.listar().stream()
                .filter(cuenta -> cuenta.getNombreUsuario().equals(usuario))
                .findFirst().orElseThrow().getId();
    }
}

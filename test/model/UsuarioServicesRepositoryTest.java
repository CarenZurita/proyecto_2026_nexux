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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.UsuarioRepository;
import service.AutenticacionService;
import service.GestionUsuariosService;
import service.RecuperacionService;

class UsuarioServicesRepositoryTest {
    @TempDir
    Path directorioTemporal;

    private UsuarioRepository repositorio;
    private AutenticacionService autenticacionService;
    private GestionUsuariosService gestionUsuariosService;
    private RecuperacionService recuperacionService;

    @BeforeEach
    void configurarRepositorioTemporal() {
        repositorio = new UsuarioRepository(directorioTemporal.resolve("usuarios.txt"));
        autenticacionService = new AutenticacionService(repositorio);
        gestionUsuariosService = new GestionUsuariosService(repositorio);
        recuperacionService = new RecuperacionService(repositorio);
    }

    @Test
    void inicializaAdministradorEnArchivoTemporalInexistente()
            throws IOException, GeneralSecurityException {
        autenticacionService.inicializarAdministrador();

        assertTrue(directorioTemporal.resolve("usuarios.txt").toFile().exists());
        assertNotNull(autenticacionService.iniciarSesion(
                "admin", "Farmacia2026!".toCharArray()));
    }

    @Test
    void registraUsuarioConDatosCompletos()
            throws IOException, GeneralSecurityException {
        autenticacionService.inicializarAdministrador();
        Usuario administrador = autenticacionService.iniciarSesion(
                "admin", "Farmacia2026!".toCharArray());

        gestionUsuariosService.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO,
                "ClaveSegura123".toCharArray(), "ClaveSegura123".toCharArray());

        FichaUsuario encargada = gestionUsuariosService.listarUsuarios(administrador).stream()
                .filter(ficha -> ficha.usuario().equals("encargada1"))
                .findFirst().orElseThrow();

        assertEquals("encargada1", encargada.usuario());
        assertEquals("Encargada", encargada.nombre());
        assertEquals(Rol.ENCARGADO, encargada.rol());
        assertTrue(encargada.activo());
    }

    @Test
    void actualizaUsuarioDesdeRepositorioTemporal()
            throws IOException, GeneralSecurityException {
        autenticacionService.inicializarAdministrador();
        Usuario administrador = autenticacionService.iniciarSesion(
                "admin", "Farmacia2026!".toCharArray());
        gestionUsuariosService.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO,
                "ClaveSegura123".toCharArray(), "ClaveSegura123".toCharArray());
        int id = gestionUsuariosService.listarUsuarios(administrador).stream()
                .filter(ficha -> ficha.usuario().equals("encargada1"))
                .findFirst().orElseThrow().id();

        gestionUsuariosService.actualizarUsuario(administrador, id,
                "Nombre actualizado", "encargada2", Rol.ENCARGADO,
                new char[0], new char[0]);

        FichaUsuario actualizada = gestionUsuariosService.listarUsuarios(administrador)
                .stream().filter(ficha -> ficha.id() == id)
                .findFirst().orElseThrow();
        assertEquals("encargada2", actualizada.usuario());
        assertEquals("Nombre actualizado", actualizada.nombre());
        assertEquals(Rol.ENCARGADO, actualizada.rol());
        assertTrue(actualizada.activo());
    }

    @Test
    void cambiaEstadoDeUsuarioEnRepositorioTemporal()
            throws IOException, GeneralSecurityException {
        autenticacionService.inicializarAdministrador();
        Usuario administrador = autenticacionService.iniciarSesion(
                "admin", "Farmacia2026!".toCharArray());
        gestionUsuariosService.registrarUsuario(administrador, "Encargada",
                "encargada1", Rol.ENCARGADO,
                "ClaveSegura123".toCharArray(), "ClaveSegura123".toCharArray());
        int id = gestionUsuariosService.listarUsuarios(administrador).stream()
                .filter(ficha -> ficha.usuario().equals("encargada1"))
                .findFirst().orElseThrow().id();

        gestionUsuariosService.cambiarEstadoUsuario(administrador, id, false);

        FichaUsuario actualizada = gestionUsuariosService.listarUsuarios(administrador)
                .stream().filter(ficha -> ficha.id() == id)
                .findFirst().orElseThrow();
        assertFalse(actualizada.activo());
    }

    @Test
    void restableceContrasenaDeAdministradorUsandoElRepositorioTemporal()
            throws IOException, GeneralSecurityException {
        autenticacionService.inicializarAdministrador();
        Usuario administrador = autenticacionService.iniciarSesion(
                "admin", "Farmacia2026!".toCharArray());
        recuperacionService.configurarRecuperacion(administrador,
                "azul".toCharArray(), "arroz".toCharArray());

        PermisoRecuperacion permiso = recuperacionService.verificarRecuperacion(
                "admin", "azul".toCharArray(), "arroz".toCharArray());
        assertNotNull(permiso);
        recuperacionService.restablecerContrasena(permiso,
                "NuevaClave123".toCharArray(), "NuevaClave123".toCharArray());

        assertThrows(SecurityException.class, () -> recuperacionService.restablecerContrasena(
                permiso, "OtraClave123".toCharArray(), "OtraClave123".toCharArray()));
        assertNull(autenticacionService.iniciarSesion("admin", "Farmacia2026!".toCharArray()));
        assertNotNull(autenticacionService.iniciarSesion("admin", "NuevaClave123".toCharArray()));
    }
}

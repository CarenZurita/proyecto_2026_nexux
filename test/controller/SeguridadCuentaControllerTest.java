package controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import model.Usuario;
import persistence.UsuarioRepository;
import service.AutenticacionService;
import service.GestionUsuariosService;
import service.RecuperacionService;

class SeguridadCuentaControllerTest {
    private static final char[] CLAVE_INICIAL = "Farmacia2026!".toCharArray();

    @TempDir
    Path directorioTemporal;

    private AutenticacionService autenticacionService;
    private RecuperacionService recuperacionService;
    private SeguridadCuentaController controlador;
    private Usuario administrador;

    @BeforeEach
    void crearControladorConRepositorioTemporal()
            throws IOException, GeneralSecurityException {
        UsuarioRepository repositorio =
                new UsuarioRepository(directorioTemporal.resolve("usuarios.txt"));
        autenticacionService = new AutenticacionService(repositorio);
        recuperacionService = new RecuperacionService(repositorio);
        GestionUsuariosService gestionUsuariosService =
                new GestionUsuariosService(repositorio);
        controlador = new SeguridadCuentaController(
                gestionUsuariosService, recuperacionService);
        autenticacionService.inicializarAdministrador();
        administrador = autenticacionService.iniciarSesion("admin", CLAVE_INICIAL);
    }

    @Test
    void cambiarMiClaveActualizaLaCuenta()
            throws IOException, GeneralSecurityException {
        char[] nuevaClave = "NuevaClave123".toCharArray();

        controlador.cambiarMiClave(administrador, CLAVE_INICIAL, nuevaClave, nuevaClave);

        assertTrue(administrador.estaEnSesion());
        assertNull(autenticacionService.iniciarSesion("admin", CLAVE_INICIAL));
        assertNotNull(autenticacionService.iniciarSesion("admin", nuevaClave));
    }

    @Test
    void actualizarMisPreguntasReemplazaLasRespuestas()
            throws IOException, GeneralSecurityException {
        recuperacionService.configurarRecuperacion(administrador,
                "azul".toCharArray(), "arroz".toCharArray());

        controlador.actualizarMisPreguntas(administrador, CLAVE_INICIAL,
                "verde".toCharArray(), "pizza".toCharArray());

        assertNull(recuperacionService.verificarRecuperacion(
                "admin", "azul".toCharArray(), "arroz".toCharArray()));
        assertNotNull(recuperacionService.verificarRecuperacion(
                "admin", "verde".toCharArray(), "pizza".toCharArray()));
    }
}

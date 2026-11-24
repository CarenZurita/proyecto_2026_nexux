package controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import model.Usuario;
import service.GestionUsuariosService;
import service.RecuperacionService;

/**
 * Controlador MVC para las operaciones de seguridad de la cuenta autenticada.
 */
public class SeguridadCuentaController {
    private final GestionUsuariosService gestionUsuariosService;
    private final RecuperacionService recuperacionService;

    /**
     * Conecta los servicios de cuenta y recuperación.
     *
     * @param gestionUsuariosService cambios de contraseña
     * @param recuperacionService configuración de preguntas
     */
    public SeguridadCuentaController(GestionUsuariosService gestionUsuariosService,
            RecuperacionService recuperacionService) {
        this.gestionUsuariosService = gestionUsuariosService;
        this.recuperacionService = recuperacionService;
    }

    /**
     * Cambia la contraseña de la propia cuenta tras validar la actual.
     *
     * @param sesion sesión de la cuenta
     * @param actual contraseña vigente
     * @param nueva contraseña nueva
     * @param confirmacion confirmación de la nueva contraseña
     * @throws IOException si falla la persistencia
     * @throws GeneralSecurityException si falla la verificación o protección criptográfica
     * @throws SecurityException si la sesión o contraseña actual no son válidas
     * @throws IllegalArgumentException si la contraseña nueva no cumple las reglas
     */
    public void cambiarMiClave(Usuario sesion, char[] actual, char[] nueva, char[] confirmacion)
            throws IOException, GeneralSecurityException {
        gestionUsuariosService.cambiarMiClave(sesion, actual, nueva, confirmacion);
    }

    /**
     * Actualiza las respuestas de recuperación de la propia cuenta.
     *
     * @param sesion sesión administradora
     * @param actual contraseña vigente
     * @param color respuesta nueva de color
     * @param comida respuesta nueva de comida
     * @throws IOException si falla la persistencia
     * @throws GeneralSecurityException si falla la protección de respuestas
     * @throws SecurityException si la sesión o contraseña actual no son válidas
     */
    public void actualizarMisPreguntas(Usuario sesion, char[] actual, char[] color, char[] comida)
            throws IOException, GeneralSecurityException {
        recuperacionService.actualizarMisPreguntas(sesion, actual, color, comida);
    }
}

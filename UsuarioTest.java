package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsuarioTest {
    private static final char[] CLAVE = "ClaveSegura123".toCharArray();
    private Usuario usuario;

    @SuppressWarnings("unused")
    @BeforeEach
    void prepararUsuario() throws GeneralSecurityException {
        usuario = Usuario.crear(
                "Usuario.Prueba", "Usuario de Prueba", Rol.ENCARGADO, CLAVE);
    }

    /**
     * Comprueba que crear normalice el nombre de usuario y asigne rol y estado iniciales.
     */
    @Test
    void crearInicializaYNormalizaLaCuenta() throws GeneralSecurityException {
        Usuario administrador = Usuario.crear(
                "Admin.Prueba", "Administradora", Rol.ADMINISTRADOR, CLAVE);

        assertEquals("admin.prueba", administrador.getNombreUsuario());
        assertEquals("Administradora", administrador.getNombre());
        assertEquals("Administradora", administrador.getNombreCompleto());
        assertEquals(Rol.ADMINISTRADOR, administrador.getRol());
        assertTrue(administrador.estaActivo());
        assertTrue(administrador.esAdministrador());
        assertTrue(administrador instanceof Administrador);
        assertTrue(usuario instanceof Encargado);
    }

    /**
     * Comprueba que comprobarClave acepte la clave correcta y rechace otra distinta.
     */
    @Test
    void comprobarClaveValidaLaClaveIngresada() throws GeneralSecurityException {
        assertTrue(usuario.comprobarClave("ClaveSegura123".toCharArray()));
        assertFalse(usuario.comprobarClave("ClaveIncorrecta".toCharArray()));
    }

    @Test
    void iniciarYCerrarSesionActualizaElEstadoDeLaSesion()
            throws GeneralSecurityException {
        assertTrue(usuario.iniciarSesion("ClaveSegura123".toCharArray()));
        assertTrue(usuario.estaEnSesion());

        usuario.cerrarSesion();

        assertFalse(usuario.estaEnSesion());
    }

    @Test
    void cambiarContraseñaExigeSesionYActualizaLaClave()
            throws GeneralSecurityException {
        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> usuario.cambiarContraseña(
                        "ClaveSegura123".toCharArray(), "NuevaClave456".toCharArray()));
        assertEquals("Debe iniciar sesión con la contraseña actual.", excepcion.getMessage());

        assertTrue(usuario.iniciarSesion("ClaveSegura123".toCharArray()));
        usuario.cambiarContraseña(
                "ClaveSegura123".toCharArray(), "NuevaClave456".toCharArray());

        assertFalse(usuario.comprobarClave("ClaveSegura123".toCharArray()));
        assertTrue(usuario.comprobarClave("NuevaClave456".toCharArray()));
    }

    /**
     * Comprueba que cambiarClave invalide la clave anterior y acepte la nueva.
     */
    @Test
    void cambiarClaveReemplazaLaClaveAnterior() throws GeneralSecurityException {
        usuario.cambiarClave("NuevaClave456".toCharArray());

        assertFalse(usuario.comprobarClave("ClaveSegura123".toCharArray()));
        assertTrue(usuario.comprobarClave("NuevaClave456".toCharArray()));
    }

    /**
     * Comprueba que aLinea serialice los campos en el orden esperado.
     */
    @Test
    void aLineaSerializaLosDatosDeLaCuenta() {
        String[] campos = usuario.aLinea().split(";", -1);

        assertEquals(6, campos.length);
        assertEquals("usuario.prueba", campos[0]);
        assertEquals("Usuario de Prueba", campos[1]);
        assertEquals("ENCARGADO", campos[2]);
        assertEquals("true", campos[3]);
    }

    /**
     * Comprueba que desdeLinea reconstruya una cuenta serializada sin cambiar sus datos.
     */
    @Test
    void desdeLineaReconstruyeUnaCuentaValida() {
        Usuario reconstruido = Usuario.desdeLinea(usuario.aLinea());

        assertEquals(usuario.aLinea(), reconstruido.aLinea());
        assertEquals("usuario.prueba", reconstruido.getNombreUsuario());
        assertEquals(Rol.ENCARGADO, reconstruido.getRol());
        assertTrue(reconstruido instanceof Encargado);
    }

    /**
     * Comprueba que desdeLinea rechace una cuenta con un estado inválido.
     */
    @Test
    void desdeLineaRechazaDatosInvalidos() {
        String lineaInvalida = usuario.aLinea().replace(";true;", ";activo;");

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> Usuario.desdeLinea(lineaInvalida));
        assertEquals("Cuenta inválida", excepcion.getMessage());
    }

    /**
     * Comprueba que getNombreUsuario devuelva el identificador almacenado.
     */
    @Test
    void getNombreUsuarioDevuelveElIdentificador() {
        assertEquals("usuario.prueba", usuario.getNombreUsuario());
    }

    /**
     * Comprueba que getNombreCompleto devuelva el nombre almacenado.
     */
    @Test
    void getNombreCompletoDevuelveElNombre() {
        assertEquals("Usuario de Prueba", usuario.getNombreCompleto());
    }

    /**
     * Comprueba que getRol devuelva el rol asignado a la cuenta.
     */
    @Test
    void getRolDevuelveElRolAsignado() {
        assertEquals(Rol.ENCARGADO, usuario.getRol());
    }

    /**
     * Comprueba que estaActivo informe el estado actual de la cuenta.
     */
    @Test
    void estaActivoDevuelveElEstadoActual() {
        assertTrue(usuario.estaActivo());
        usuario.setActivo(false);
        assertFalse(usuario.estaActivo());
    }

    /**
     * Comprueba que esAdministrador distinga las cuentas administrativas.
     */
    @Test
    void esAdministradorIdentificaElRolAdministrativo() throws GeneralSecurityException {
        Usuario administrador = Usuario.crear(
                "Admin.Prueba", "Administradora", Rol.ADMINISTRADOR, CLAVE);

        assertTrue(administrador.esAdministrador());
        assertFalse(usuario.esAdministrador());
    }

    /**
     * Comprueba que setNombreUsuario actualice el identificador.
     */
    @Test
    void setNombreUsuarioActualizaElIdentificador() {
        usuario.setNombreUsuario("nuevo.usuario");

        assertEquals("nuevo.usuario", usuario.getNombreUsuario());
    }

    /**
     * Comprueba que setNombreCompleto actualice el nombre visible.
     */
    @Test
    void setNombreCompletoActualizaElNombre() {
        usuario.setNombreCompleto("Nombre Actualizado");

        assertEquals("Nombre Actualizado", usuario.getNombreCompleto());
    }

    /**
     * Comprueba que setActivo permita activar y desactivar una cuenta.
     */
    @Test
    void setActivoActualizaElEstado() {
        usuario.setActivo(false);
        assertFalse(usuario.estaActivo());

        usuario.setActivo(true);
        assertTrue(usuario.estaActivo());
    }
}

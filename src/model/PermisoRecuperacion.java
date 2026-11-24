package model;

/**
 * Credencial temporal, de un solo uso, emitida al verificar la recuperación
 * de una cuenta administradora.
 */
public final class PermisoRecuperacion {
    private final String usuario;
    private final String version;
    private final long vence;
    private boolean usado;

    /**
     * Emite un permiso que vence cinco minutos después de su creación.
     *
     * @param cuenta cuenta cuya recuperación fue verificada
     * @param ahora instante de emisión en milisegundos
     */
    public PermisoRecuperacion(Usuario cuenta, long ahora) {
        usuario = cuenta.getNombreUsuario();
        version = cuenta.versionRecuperacion();
        vence = ahora + 5 * 60_000;
    }

    /**
     * Comprueba que el permiso no se usó y aún no venció.
     *
     * @param ahora instante actual en milisegundos
     * @return {@code true} si el permiso sigue vigente
     */
    public boolean estaVigente(long ahora) {
        return !usado && ahora < vence;
    }

    /**
     * Comprueba vigencia, usuario y versión de credenciales para impedir reutilización.
     *
     * @param cuenta cuenta que solicita el restablecimiento
     * @param ahora instante actual en milisegundos
     * @return {@code true} si el permiso pertenece a esa versión de la cuenta
     */
    public boolean esVigentePara(Usuario cuenta, long ahora) {
        return estaVigente(ahora) && cuenta != null
                && usuario.equals(cuenta.getNombreUsuario())
                && version.equals(cuenta.versionRecuperacion());
    }

    /** Invalida el permiso después de completar un restablecimiento. */
    public void marcarUsado() {
        usado = true;
    }
}

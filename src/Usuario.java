import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Datos de una cuenta. El rol se asigna al crearla y no se modifica. */
public class Usuario {
    public enum Rol { ADMINISTRADOR, ENCARGADO }

    private static final int ITERACIONES = 210000;
    private String nombreUsuario;
    private String nombreCompleto;
    private final Rol rol;
    private boolean activo;
    private String sal;
    private String claveProtegida;

    private Usuario(String nombreUsuario, String nombreCompleto, Rol rol,
                    boolean activo, String sal, String claveProtegida) {
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.activo = activo;
        this.sal = sal;
        this.claveProtegida = claveProtegida;
    }

    static Usuario crear(String nombreUsuario, String nombreCompleto,
                         Rol rol, char[] clave) throws GeneralSecurityException {
        byte[] salNueva = new byte[16];
        new SecureRandom().nextBytes(salNueva);
        String sal = Base64.getEncoder().encodeToString(salNueva);
        String protegida = Base64.getEncoder().encodeToString(proteger(clave, salNueva));
        return new Usuario(nombreUsuario.toLowerCase(Locale.ROOT), nombreCompleto,
                rol, true, sal, protegida);
    }

    boolean comprobarClave(char[] clave) throws GeneralSecurityException {
        byte[] guardada = Base64.getDecoder().decode(claveProtegida);
        byte[] ingresada = proteger(clave, Base64.getDecoder().decode(sal));
        return MessageDigest.isEqual(guardada, ingresada);
    }

    void cambiarClave(char[] nueva) throws GeneralSecurityException {
        byte[] salNueva = new byte[16];
        new SecureRandom().nextBytes(salNueva);
        sal = Base64.getEncoder().encodeToString(salNueva);
        claveProtegida = Base64.getEncoder().encodeToString(proteger(nueva, salNueva));
    }

    private static byte[] proteger(char[] clave, byte[] sal)
            throws GeneralSecurityException {
        PBEKeySpec datos = new PBEKeySpec(clave, sal, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(datos).getEncoded();
        } finally {
            datos.clearPassword();
        }
    }

    String aLinea() {
        return nombreUsuario + ";" + nombreCompleto + ";" + rol + ";"
                + activo + ";" + sal + ";" + claveProtegida;
    }

    static Usuario desdeLinea(String linea) {
        String[] d = linea.split(";", -1);
        if (d.length != 6 || !d[0].matches("[a-z0-9._-]{3,20}")
                || d[1].isBlank() || (!d[3].equals("true") && !d[3].equals("false"))
                || Base64.getDecoder().decode(d[4]).length != 16
                || Base64.getDecoder().decode(d[5]).length != 32) {
            throw new IllegalArgumentException("Cuenta inválida");
        }
        return new Usuario(d[0], d[1], Rol.valueOf(d[2]),
                Boolean.parseBoolean(d[3]), d[4], d[5]);
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public Rol getRol() { return rol; }
    public boolean estaActivo() { return activo; }
    public boolean esAdministrador() { return rol == Rol.ADMINISTRADOR; }

    void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    void setActivo(boolean activo) { this.activo = activo; }
}

package model;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

/** Identidad y autenticación comunes a las cuentas de farmacia. */
public abstract class Usuario {
    private static final int ITERACIONES = 210000;
    private static final int LONGITUD_SAL = 16;
    private static final int LONGITUD_CLAVE_PROTEGIDA = 32;

    private String nombre;
    private String usuario;
    private final Rol rol;
    private boolean activo;
    private boolean sesionIniciada;
    private String sal;
    private String contraseña;

    protected Usuario(String nombre, String usuario, Rol rol, boolean activo,
                      String sal, String contraseñaProtegida) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.rol = rol;
        this.activo = activo;
        this.sal = sal;
        this.contraseña = contraseñaProtegida;
    }

    public static Usuario crear(String usuario, String nombre, Rol rol, char[] clave)
            throws GeneralSecurityException {
        validarIdentidad(usuario, nombre, rol, clave);
        byte[] sal = new byte[LONGITUD_SAL];
        new SecureRandom().nextBytes(sal);
        String salCodificada = Base64.getEncoder().encodeToString(sal);
        String hash = Base64.getEncoder().encodeToString(proteger(clave, sal));
        String usuarioNormalizado = usuario.toLowerCase(Locale.ROOT);
        return crearDesdeHash(nombre, usuarioNormalizado,
                Rol.porNombre(rol.getNombre()), true,
                salCodificada, hash);
    }

    private static void validarIdentidad(String usuario, String nombre, Rol rol, char[] clave) {
        if (usuario == null || !usuario.matches("(?i)[a-z0-9._-]{3,20}")) {
            throw new IllegalArgumentException("Nombre de usuario inválido.");
        }
        if (nombre == null || nombre.isBlank() || nombre.contains(";")) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        if (clave == null || clave.length < 8 || clave.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener de 8 a 128 caracteres.");
        }
    }

    private static Usuario crearDesdeHash(String nombre, String usuario, Rol rol,
                                          boolean activo, String sal, String hash) {
        return switch (rol.getNombre()) {
            case "ADMINISTRADOR" ->
                    new Administrador(nombre, usuario, rol, activo, sal, hash);
            case "ENCARGADO" ->
                    new Encargado(nombre, usuario, rol, activo, sal, hash);
            default -> throw new IllegalArgumentException("Rol no reconocido.");
        };
    }

    public boolean iniciarSesion(char[] clave) throws GeneralSecurityException {
        sesionIniciada = activo && comprobarClave(clave);
        return sesionIniciada;
    }

    public void cerrarSesion() {
        sesionIniciada = false;
    }

    public void cambiarContraseña(char[] actual, char[] nueva)
            throws GeneralSecurityException {
        if (!sesionIniciada || !comprobarClave(actual)) {
            throw new SecurityException("Debe iniciar sesión con la contraseña actual.");
        }
        validarClave(nueva);
        cambiarClave(nueva);
    }

    private static void validarClave(char[] clave) {
        if (clave == null || clave.length < 8 || clave.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener de 8 a 128 caracteres.");
        }
    }

    boolean comprobarClave(char[] clave) throws GeneralSecurityException {
        if (clave == null) {
            return false;
        }
        byte[] guardada = Base64.getDecoder().decode(contraseña);
        byte[] ingresada = proteger(clave, Base64.getDecoder().decode(sal));
        return MessageDigest.isEqual(guardada, ingresada);
    }

    void cambiarClave(char[] nueva) throws GeneralSecurityException {
        validarClave(nueva);
        byte[] salNueva = new byte[LONGITUD_SAL];
        new SecureRandom().nextBytes(salNueva);
        sal = Base64.getEncoder().encodeToString(salNueva);
        contraseña = Base64.getEncoder().encodeToString(proteger(nueva, salNueva));
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
        return usuario + ";" + nombre + ";" + rol.getNombre() + ";"
                + activo + ";" + sal + ";" + contraseña;
    }

    static Usuario desdeLinea(String linea) {
        String[] datos = linea.split(";", -1);
        try {
            if (datos.length != 6 || !datos[0].matches("[a-z0-9._-]{3,20}")
                    || datos[1].isBlank() || datos[1].contains(";")
                    || (!datos[3].equals("true") && !datos[3].equals("false"))
                    || Base64.getDecoder().decode(datos[4]).length != LONGITUD_SAL
                    || Base64.getDecoder().decode(datos[5]).length != LONGITUD_CLAVE_PROTEGIDA) {
                throw new IllegalArgumentException("Cuenta inválida");
            }
            Rol rol = Rol.porNombre(datos[2]);
            return crearDesdeHash(datos[1], datos[0], rol,
                    Boolean.parseBoolean(datos[3]), datos[4], datos[5]);
        } catch (IllegalArgumentException e) {
            if ("Cuenta inválida".equals(e.getMessage())) {
                throw e;
            }
            throw new IllegalArgumentException("Cuenta inválida", e);
        }
    }

    public List<Medicamento> buscarMedicamento(String texto,
                                                MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar un texto para buscar.");
        }
        return medicamentos.buscar(texto.trim());
    }

    public Optional<Medicamento> consultarStock(String codigo,
                                                 MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        return medicamentos.buscarPorCodigo(codigo);
    }

    public void registrarVenta(String codigo, int cantidad,
                               MedicamentoRepository medicamentos,
                               VentaRepository ventas) throws IOException {
        validarSesionActiva();
        new Venta(codigo, cantidad).registrar(this, medicamentos, ventas);
    }

    public List<Medicamento> revisarVencimientos(MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        List<Medicamento> porRevisar = new ArrayList<>();
        for (Medicamento medicamento : medicamentos.listar()) {
            try {
                if (medicamento.controlarMedicamento(30)
                        != Medicamento.EstadoVencimiento.VIGENTE) {
                    porRevisar.add(medicamento);
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Fecha inválida en el medicamento " + medicamento.getCodigo() + ".", e);
            }
        }
        return porRevisar;
    }

    protected final void validarSesionActiva() {
        if (!activo || !sesionIniciada) {
            throw new SecurityException("Debe iniciar sesión para realizar esta operación.");
        }
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreCompleto() {
        return nombre;
    }

    public String getNombreUsuario() {
        return usuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean estaActivo() {
        return activo;
    }

    public boolean estaEnSesion() {
        return sesionIniciada;
    }

    public boolean esAdministrador() {
        return rol.esAdministrador();
    }

    void setNombreCompleto(String nombre) {
        this.nombre = nombre;
    }

    void setNombreUsuario(String usuario) {
        if (usuario == null || !usuario.matches("[a-z0-9._-]{3,20}")) {
            throw new IllegalArgumentException("Nombre de usuario inválido.");
        }
        this.usuario = usuario;
    }

    void setActivo(boolean activo) {
        this.activo = activo;
        if (!activo) {
            cerrarSesion();
        }
    }
}

package model;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
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

    private int id;
    private String nombre;
    private String usuario;
    private final Rol rol;
    private boolean activo;
    private boolean sesionIniciada;
    private String sal;
    private String contraseña;
    private String salColor = "";
    private String respuestaColor = "";
    private String salComida = "";
    private String respuestaComida = "";
    private int intentosRecuperacion;
    private long recuperacionBloqueadaHasta;

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

    public boolean tieneRecuperacion() {
        return !respuestaColor.isEmpty() && !respuestaComida.isEmpty();
    }

    /** Solo GestorUsuarios puede configurar las respuestas después de autenticar. */
    void configurarRecuperacion(char[] color, char[] comida) throws GeneralSecurityException {
        if (!esAdministrador()) throw new SecurityException("Esta recuperación es para administradores.");
        char[] colorNormal = normalizarRespuesta(color);
        char[] comidaNormal = null;
        try {
            comidaNormal = normalizarRespuesta(comida);
            byte[] sal1 = new byte[LONGITUD_SAL];
            byte[] sal2 = new byte[LONGITUD_SAL];
            SecureRandom random = new SecureRandom();
            random.nextBytes(sal1); random.nextBytes(sal2);
            String hash1 = Base64.getEncoder().encodeToString(proteger(colorNormal, sal1));
            String hash2 = Base64.getEncoder().encodeToString(proteger(comidaNormal, sal2));
            salColor = Base64.getEncoder().encodeToString(sal1);
            salComida = Base64.getEncoder().encodeToString(sal2);
            respuestaColor = hash1; respuestaComida = hash2;
            limpiarIntentosRecuperacion();
        } finally {
            Arrays.fill(colorNormal, '\0');
            if (comidaNormal != null) Arrays.fill(comidaNormal, '\0');
        }
    }

    static char[] normalizarRespuesta(char[] respuesta) {
        if (respuesta == null || respuesta.length > 128) {
            throw new IllegalArgumentException("Cada respuesta debe tener entre 1 y 128 caracteres.");
        }
        String texto = Normalizer.normalize(new String(respuesta).trim(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        if (texto.isBlank()) throw new IllegalArgumentException("Completa las dos respuestas.");
        return texto.toCharArray();
    }

    boolean comprobarRespuestas(char[] color, char[] comida) throws GeneralSecurityException {
        if (!tieneRecuperacion()) return false;
        char[] colorNormal = normalizarRespuesta(color);
        char[] comidaNormal = null;
        try {
            comidaNormal = normalizarRespuesta(comida);
            boolean colorCorrecto = MessageDigest.isEqual(Base64.getDecoder().decode(respuestaColor),
                    proteger(colorNormal, Base64.getDecoder().decode(salColor)));
            boolean comidaCorrecta = MessageDigest.isEqual(Base64.getDecoder().decode(respuestaComida),
                    proteger(comidaNormal, Base64.getDecoder().decode(salComida)));
            return colorCorrecto & comidaCorrecta;
        } finally {
            Arrays.fill(colorNormal, '\0');
            if (comidaNormal != null) Arrays.fill(comidaNormal, '\0');
        }
    }

    boolean mismaCredencial(Usuario otro) {
        return otro != null && usuario.equals(otro.usuario)
                && sal.equals(otro.sal) && contraseña.equals(otro.contraseña);
    }

    String versionRecuperacion() {
        return sal + ";" + contraseña + ";" + salColor + ";" + respuestaColor
                + ";" + salComida + ";" + respuestaComida;
    }

    long getRecuperacionBloqueadaHasta() { return recuperacionBloqueadaHasta; }

    void limpiarIntentosRecuperacion() {
        intentosRecuperacion = 0;
        recuperacionBloqueadaHasta = 0;
    }

    boolean registrarFalloRecuperacion(long ahora) {
        recuperacionBloqueadaHasta = 0;
        intentosRecuperacion++;
        if (intentosRecuperacion >= 3) {
            intentosRecuperacion = 0;
            recuperacionBloqueadaHasta = ahora + 30_000;
            return true;
        }
        return false;
    }

    void copiarRecuperacionDesde(Usuario cuenta) {
        salColor = cuenta.salColor; respuestaColor = cuenta.respuestaColor;
        salComida = cuenta.salComida; respuestaComida = cuenta.respuestaComida;
        intentosRecuperacion = cuenta.intentosRecuperacion;
        recuperacionBloqueadaHasta = cuenta.recuperacionBloqueadaHasta;
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
        String base = usuario + ";" + nombre + ";" + rol.getNombre() + ";"
                + activo + ";" + sal + ";" + contraseña;
        if (!tieneRecuperacion()) return base + ";" + id;
        return base + ";" + salColor + ";" + respuestaColor + ";" + salComida
                + ";" + respuestaComida + ";" + intentosRecuperacion
                + ";" + recuperacionBloqueadaHasta + ";" + id;
    }

    static Usuario desdeLinea(String linea) {
        String[] originales = linea.split(";", -1);
        boolean conId = originales.length == 7 || originales.length == 13;
        String[] datos = conId ? Arrays.copyOf(originales, originales.length - 1) : originales;
        try {
            if ((datos.length != 6 && datos.length != 12) || !datos[0].matches("[a-z0-9._-]{3,20}")
                    || datos[1].isBlank() || datos[1].contains(";")
                    || (!datos[3].equals("true") && !datos[3].equals("false"))
                    || Base64.getDecoder().decode(datos[4]).length != LONGITUD_SAL
                    || Base64.getDecoder().decode(datos[5]).length != LONGITUD_CLAVE_PROTEGIDA) {
                throw new IllegalArgumentException("Cuenta inválida");
            }
            Rol rol = Rol.porNombre(datos[2]);
            Usuario cuenta = crearDesdeHash(datos[1], datos[0], rol,
                    Boolean.parseBoolean(datos[3]), datos[4], datos[5]);
            if (datos.length == 12) {
                if (!rol.esAdministrador()
                        || Base64.getDecoder().decode(datos[6]).length != LONGITUD_SAL
                        || Base64.getDecoder().decode(datos[7]).length != LONGITUD_CLAVE_PROTEGIDA
                        || Base64.getDecoder().decode(datos[8]).length != LONGITUD_SAL
                        || Base64.getDecoder().decode(datos[9]).length != LONGITUD_CLAVE_PROTEGIDA) {
                    throw new IllegalArgumentException("Cuenta inválida");
                }
                cuenta.salColor = datos[6]; cuenta.respuestaColor = datos[7];
                cuenta.salComida = datos[8]; cuenta.respuestaComida = datos[9];
                cuenta.intentosRecuperacion = Integer.parseInt(datos[10]);
                cuenta.recuperacionBloqueadaHasta = Long.parseLong(datos[11]);
                if (cuenta.intentosRecuperacion < 0 || cuenta.intentosRecuperacion > 2
                        || cuenta.recuperacionBloqueadaHasta < 0) {
                    throw new IllegalArgumentException("Cuenta inválida");
                }
            }
            if (conId) {
                cuenta.id = Integer.parseInt(originales[originales.length - 1]);
                if (cuenta.id <= 0) throw new IllegalArgumentException("ID inválido");
            }
            return cuenta;
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

    public int getId() { return id; }

    void asignarId(int nuevoId) {
        if (nuevoId <= 0 || (id != 0 && id != nuevoId)) {
            throw new IllegalArgumentException("No se puede cambiar el ID de una cuenta.");
        }
        id = nuevoId;
    }

    /** Conserva credenciales e ID y usa la subclase adecuada para el nuevo rol. */
    Usuario conRol(Rol nuevoRol) {
        Usuario copia = crearDesdeHash(nombre, usuario, Rol.porNombre(nuevoRol.getNombre()),
                activo, sal, contraseña);
        copia.id = id;
        if (nuevoRol.esAdministrador() && esAdministrador()) copia.copiarRecuperacionDesde(this);
        return copia;
    }

    void sincronizarDatos(Usuario cuenta) {
        nombre = cuenta.nombre;
        usuario = cuenta.usuario;
        sal = cuenta.sal;
        contraseña = cuenta.contraseña;
        id = cuenta.id;
        copiarRecuperacionDesde(cuenta);
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

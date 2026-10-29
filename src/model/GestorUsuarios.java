package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Inicio de sesión y administración de cuentas guardadas en usuarios.txt. */
public class GestorUsuarios {
    private static final Path ARCHIVO = Paths.get("datos/usuarios.txt");
    private static final String USUARIO_INICIAL = "admin";
    private static final String CLAVE_INICIAL = "Farmacia2026!";

    /** Permiso temporal de un solo uso, creado únicamente al acertar ambas respuestas. */
    public static final class Recuperacion {
        private final String usuario;
        private final String version;
        private final long vence;
        private boolean usado;

        private Recuperacion(Usuario cuenta) {
            usuario = cuenta.getNombreUsuario();
            version = cuenta.versionRecuperacion();
            vence = System.currentTimeMillis() + 5 * 60_000;
        }
    }

    public static synchronized void configurarRecuperacion(Usuario sesion, char[] color, char[] comida)
            throws IOException, GeneralSecurityException {
        if (sesion == null || !sesion.esAdministrador() || !sesion.estaActivo()
                || !sesion.estaEnSesion()) {
            throw new SecurityException("Debes iniciar sesión como administrador para configurar las respuestas.");
        }
        List<Usuario> cuentas = cargar();
        Usuario cuenta = buscar(cuentas, sesion.getNombreUsuario());
        if (!administradorActivo(cuenta) || !cuenta.mismaCredencial(sesion)) {
            throw new SecurityException("La sesión ya no es válida. Inicia sesión nuevamente.");
        }
        if (cuenta.tieneRecuperacion()) {
            throw new IllegalStateException("Las preguntas de esta cuenta ya están configuradas.");
        }
        cuenta.configurarRecuperacion(color, comida);
        guardar(cuentas);
        sesion.copiarRecuperacionDesde(cuenta);
    }

    public static synchronized Recuperacion verificarRecuperacion(String nombre,
            char[] color, char[] comida) throws IOException, GeneralSecurityException {
        // Se validan los campos antes de contar un intento.
        char[] c = Usuario.normalizarRespuesta(color);
        char[] m = null;
        try {
            m = Usuario.normalizarRespuesta(comida);
        } finally {
            Arrays.fill(c, '\0');
            if (m != null) Arrays.fill(m, '\0');
        }
        List<Usuario> cuentas = cargar();
        Usuario cuenta = buscar(cuentas, normalizarUsuario(nombre));
        if (!administradorActivo(cuenta) || !cuenta.tieneRecuperacion()) return null;
        long ahora = System.currentTimeMillis();
        if (cuenta.getRecuperacionBloqueadaHasta() > ahora) {
            long segundos = (cuenta.getRecuperacionBloqueadaHasta() - ahora + 999) / 1000;
            throw new IllegalStateException("Espera " + segundos + " segundos antes de intentar recuperar la contraseña.");
        }
        if (!cuenta.comprobarRespuestas(color, comida)) {
            boolean bloqueado = cuenta.registrarFalloRecuperacion(ahora);
            guardar(cuentas);
            if (bloqueado) throw new IllegalStateException(
                    "Se agotaron los 3 intentos. Espera 30 segundos para volver a intentarlo.");
            return null;
        }
        cuenta.limpiarIntentosRecuperacion();
        guardar(cuentas);
        return new Recuperacion(cuenta);
    }

    public static synchronized void restablecerContrasena(Recuperacion permiso,
            char[] nueva, char[] confirmacion) throws IOException, GeneralSecurityException {
        if (permiso == null || permiso.usado || System.currentTimeMillis() >= permiso.vence) {
            throw new SecurityException("Verifica nuevamente las preguntas para restablecer la contraseña.");
        }
        if (nueva == null || nueva.length < 8 || nueva.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener de 8 a 128 caracteres.");
        }
        if (!Arrays.equals(nueva, confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        List<Usuario> cuentas = cargar();
        Usuario cuenta = buscar(cuentas, permiso.usuario);
        if (!administradorActivo(cuenta) || !cuenta.tieneRecuperacion()
                || !cuenta.versionRecuperacion().equals(permiso.version)) {
            throw new SecurityException("La cuenta cambió. Verifica nuevamente las preguntas.");
        }
        cuenta.cambiarClave(nueva);
        cuenta.cerrarSesion();
        cuenta.limpiarIntentosRecuperacion();
        guardar(cuentas);
        permiso.usado = true;
    }

    public static synchronized void cambiarMiClave(Usuario sesion, char[] actual, char[] nueva, char[] confirmar) throws IOException, GeneralSecurityException {
        List<Usuario> cuentas = cargar();
        Usuario cuenta = validarAdministrador(sesion, cuentas);
        if (!cuenta.comprobarClave(actual)) throw new SecurityException("Contraseña actual incorrecta.");
        validarConfirmacion(nueva, confirmar);
        cuenta.cambiarClave(nueva);
        guardar(cuentas);
        sesion.sincronizarDatos(cuenta);
    }
    public static synchronized void actualizarMisPreguntas(Usuario sesion, char[] actual, char[] color, char[] comida) throws IOException, GeneralSecurityException {
        List<Usuario> cuentas = cargar();
        Usuario cuenta = validarAdministrador(sesion, cuentas);
        if (!cuenta.comprobarClave(actual)) throw new SecurityException("Contraseña actual incorrecta.");
        cuenta.configurarRecuperacion(color, comida);
        guardar(cuentas);
        sesion.copiarRecuperacionDesde(cuenta);
    }

    private static String normalizarUsuario(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean administradorActivo(Usuario cuenta) {
        return cuenta != null && cuenta.esAdministrador() && cuenta.estaActivo();
    }

    public static synchronized void inicializarAdministrador()
            throws IOException, GeneralSecurityException {
        Files.createDirectories(ARCHIVO.toAbsolutePath().getParent());
        if (Files.exists(ARCHIVO)) { cargar(); return; }
        char[] clave = CLAVE_INICIAL.toCharArray();
        try {
            List<Usuario> cuentas = new ArrayList<>();
            cuentas.add(Usuario.crear(USUARIO_INICIAL, "Administrador",
                    Rol.ADMINISTRADOR, clave));
            guardar(cuentas);
        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    /** Validación compartida por la consola y el formulario Swing. */
    public static synchronized Usuario iniciarSesion(String nombre, char[] clave)
            throws IOException, GeneralSecurityException {
        if (nombre == null || nombre.isBlank() || clave == null || clave.length == 0) {
            return null;
        }
        String normalizado = nombre.trim().toLowerCase(Locale.ROOT);
        Usuario encontrado = buscar(cargar(), normalizado);
        if (encontrado != null && encontrado.estaActivo()
                && encontrado.iniciarSesion(clave)) {
            return encontrado;
        }
        return null;
    }

    public static Usuario iniciarSesion(Scanner teclado)
            throws IOException, GeneralSecurityException {
        for (int intento = 1; intento <= 3; intento++) {
            System.out.println("\n=== INICIO DE SESIÓN ===");
            System.out.print("Usuario: ");
            String nombre = teclado.nextLine().trim().toLowerCase(Locale.ROOT);
            System.out.print("Contraseña: ");
            char[] clave = teclado.nextLine().toCharArray();
            try {
                Usuario encontrado = iniciarSesion(nombre, clave);
                if (encontrado != null) {
                    System.out.println("Bienvenido, " + encontrado.getNombreCompleto() + ".");
                    return encontrado;
                }
            } finally {
                Arrays.fill(clave, '\0');
            }
            System.out.println("Usuario o contraseña incorrectos, o cuenta deshabilitada.");
        }
        return null;
    }

    public static void gestionarEncargados(Scanner teclado, Usuario sesion)
            throws IOException, GeneralSecurityException {
        if (sesion == null || !sesion.esAdministrador() || !sesion.estaActivo()
                || !sesion.estaEnSesion()) {
            System.out.println("Acceso denegado: solo el administrador puede gestionar encargados.");
            return;
        }
        int opcion;
        do {
            System.out.println("\n=== GESTIONAR ENCARGADOS ===");
            System.out.println("1. Registrar encargado");
            System.out.println("2. Actualizar encargado");
            System.out.println("3. Consultar/Listar encargados");
            System.out.println("4. Deshabilitar encargado");
            System.out.println("0. Volver");
            opcion = leerNumero(teclado, "Opción: ", 0, 4);
            switch (opcion) {
                case 1 -> registrarEncargado(teclado);
                case 2 -> actualizarEncargado(teclado);
                case 3 -> listarEncargados();
                case 4 -> deshabilitarEncargado(teclado);
                default -> { }
            }
        } while (opcion != 0);
    }

    private static List<Usuario> cargar() throws IOException {
        List<Usuario> cuentas = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                try {
                    Usuario cuenta = Usuario.desdeLinea(linea);
                    if (buscar(cuentas, cuenta.getNombreUsuario()) != null) {
                        throw new IllegalArgumentException("Usuario repetido");
                    }
                    if (cuenta.getId() > 0 && cuentas.stream().anyMatch(u -> u.getId() == cuenta.getId())) {
                        throw new IllegalArgumentException("ID repetido");
                    }
                    cuentas.add(cuenta);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Datos inválidos en usuarios.txt, línea "
                            + numeroLinea, e);
                }
            }
        }
        if (cuentas.stream().anyMatch(u -> u.getId() == 0)) {
            asignarIds(cuentas);
            Path respaldo = ARCHIVO.resolveSibling("usuarios.antes-gestion.bak");
            if (!Files.exists(respaldo)) Files.copy(ARCHIVO, respaldo);
            guardar(cuentas);
        }
        return cuentas;
    }

    private static void asignarIds(List<Usuario> cuentas) throws IOException {
        long siguiente = (long) cuentas.stream().mapToInt(Usuario::getId).max().orElse(0) + 1;
        for (Usuario cuenta : cuentas) {
            if (cuenta.getId() == 0) {
                if (siguiente > Integer.MAX_VALUE) throw new IOException("Se agotaron los IDs de usuarios.");
                cuenta.asignarId((int) siguiente++);
            }
        }
    }

    private static void guardar(List<Usuario> cuentas) throws IOException {
        asignarIds(cuentas);
        // Respaldo único antes de ampliar el formato; nunca se reemplazan las cuentas por ejemplos.
        Path respaldo = ARCHIVO.resolveSibling("usuarios.antes-recuperacion.bak");
        if (Files.exists(ARCHIVO) && !Files.exists(respaldo)
                && cuentas.stream().anyMatch(Usuario::tieneRecuperacion)) {
            Files.copy(ARCHIVO, respaldo);
        }
        Path temporal = Files.createTempFile(ARCHIVO.toAbsolutePath().getParent(),
                "usuarios-", ".tmp");
        try {
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal,
                    StandardCharsets.UTF_8)) {
                for (Usuario cuenta : cuentas) {
                    escritor.write(cuenta.aLinea());
                    escritor.newLine();
                }
            }
            try {
                Files.move(temporal, ARCHIVO, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static Usuario buscar(List<Usuario> cuentas, String nombreUsuario) {
        for (Usuario cuenta : cuentas) {
            if (cuenta.getNombreUsuario().equals(nombreUsuario)) return cuenta;
        }
        return null;
    }

    /** Datos públicos de una cuenta; nunca incluye claves ni respuestas. */
    public record FichaUsuario(int id, String nombre, String usuario, Rol rol, boolean activo) { }

    private static Usuario validarAdministrador(Usuario sesion, List<Usuario> cuentas) {
        if (sesion == null || !sesion.estaEnSesion() || !administradorActivo(sesion)) {
            throw new SecurityException("Se requiere una sesión de administrador.");
        }
        Usuario actual = cuentas.stream().filter(u -> u.getId() == sesion.getId()).findFirst().orElse(null);
        if (!administradorActivo(actual) || !actual.mismaCredencial(sesion)) {
            throw new SecurityException("La sesión cambió. Vuelve a iniciar sesión.");
        }
        return actual;
    }

    public static synchronized List<FichaUsuario> listarUsuarios(Usuario sesion) throws IOException {
        List<Usuario> cuentas = cargar();
        validarAdministrador(sesion, cuentas);
        return cuentas.stream().map(u -> new FichaUsuario(u.getId(), u.getNombreCompleto(),
                u.getNombreUsuario(), u.getRol(), u.estaActivo())).toList();
    }

    private static void validarDatos(String nombre, String usuario, Rol rol, List<Usuario> cuentas, int id) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.length() > 100
                || nombre.contains(";") || nombre.indexOf('\n') >= 0 || nombre.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Nombre: escribe de 1 a 100 caracteres, sin punto y coma ni saltos de línea.");
        }
        if (!usuario.matches("[a-z0-9._-]{3,20}")) {
            throw new IllegalArgumentException("Usuario: utiliza de 3 a 20 letras, números, punto, guion o guion bajo.");
        }
        if (rol == null || (!rol.equals(Rol.ADMINISTRADOR) && !rol.equals(Rol.ENCARGADO))) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
        if (cuentas.stream().anyMatch(u -> u.getId() != id && u.getNombreUsuario().equals(usuario))) {
            throw new IllegalArgumentException("Usuario: ese nombre de usuario ya existe, incluso si está inactivo.");
        }
    }

    private static void validarConfirmacion(char[] clave, char[] confirmacion) {
        if (clave == null || clave.length < 8 || clave.length > 128) {
            throw new IllegalArgumentException("Contraseña: debe tener de 8 a 128 caracteres.");
        }
        if (!Arrays.equals(clave, confirmacion)) {
            throw new IllegalArgumentException("Confirmación: las contraseñas no coinciden.");
        }
    }

    public static synchronized void registrarUsuario(Usuario sesion, String nombre, String usuario,
            Rol rol, char[] clave, char[] confirmacion) throws IOException, GeneralSecurityException {
        List<Usuario> cuentas = cargar();
        validarAdministrador(sesion, cuentas);
        String normalizado = normalizarUsuario(usuario);
        validarDatos(nombre, normalizado, rol, cuentas, 0);
        validarConfirmacion(clave, confirmacion);
        cuentas.add(Usuario.crear(normalizado, nombre.trim(), rol, clave));
        guardar(cuentas);
    }

    public static synchronized void actualizarUsuario(Usuario sesion, int id, String nombre, String usuario,
            Rol rol, char[] clave, char[] confirmacion) throws IOException, GeneralSecurityException {
        List<Usuario> cuentas = cargar();
        Usuario administrador = validarAdministrador(sesion, cuentas);
        Usuario anterior = cuentas.stream().filter(u -> u.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La cuenta ya no existe. Actualiza la tabla."));
        String normalizado = normalizarUsuario(usuario);
        validarDatos(nombre, normalizado, rol, cuentas, id);
        if (id == administrador.getId() && !rol.esAdministrador()) {
            throw new IllegalArgumentException("No puedes quitar el rol de administrador a tu propia cuenta.");
        }
        if (anterior.esAdministrador() && anterior.estaActivo() && !rol.esAdministrador()
                && cuentas.stream().filter(u -> u.esAdministrador() && u.estaActivo()).count() <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
        }
        Usuario editado = anterior.conRol(rol);
        editado.setNombreCompleto(nombre.trim());
        editado.setNombreUsuario(normalizado);
        if (clave == null || confirmacion == null) throw new IllegalArgumentException("Revisa los campos de contraseña.");
        if (clave.length > 0 || confirmacion.length > 0) {
            validarConfirmacion(clave, confirmacion);
            editado.cambiarClave(clave);
        }
        cuentas.set(cuentas.indexOf(anterior), editado);
        guardar(cuentas);
        if (id == administrador.getId()) sesion.sincronizarDatos(editado);
    }

    public static synchronized void cambiarEstadoUsuario(Usuario sesion, int id, boolean activo) throws IOException {
        List<Usuario> cuentas = cargar();
        Usuario administrador = validarAdministrador(sesion, cuentas);
        Usuario cuenta = cuentas.stream().filter(u -> u.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La cuenta ya no existe. Actualiza la tabla."));
        if (!activo && id == administrador.getId()) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
        }
        if (!activo && cuenta.esAdministrador() && cuenta.estaActivo()
                && cuentas.stream().filter(u -> u.esAdministrador() && u.estaActivo()).count() <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
        }
        cuenta.setActivo(activo);
        guardar(cuentas);
    }

    private static String leerNombre(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String nombre = teclado.nextLine().trim();
            if (!nombre.isEmpty() && !nombre.contains(";")) return nombre;
            System.out.println("Ingrese un nombre no vacío y sin punto y coma.");
        }
    }

    private static String leerUsuario(Scanner teclado, List<Usuario> cuentas,
                                      String usuarioActual) {
        while (true) {
            System.out.print("Usuario (3 a 20 letras, números, . _ -): ");
            String nombre = teclado.nextLine().trim().toLowerCase(Locale.ROOT);
            if (!nombre.matches("[a-z0-9._-]{3,20}")) {
                System.out.println("Nombre de usuario inválido.");
            } else if (buscar(cuentas, nombre) != null && !nombre.equals(usuarioActual)) {
                System.out.println("Ese usuario ya existe.");
            } else {
                return nombre;
            }
        }
    }

    private static char[] leerClaveNueva(Scanner teclado) {
        while (true) {
            System.out.print("Contraseña nueva (8 a 128 caracteres): ");
            char[] clave = teclado.nextLine().toCharArray();
            if (clave.length < 8 || clave.length > 128) {
                Arrays.fill(clave, '\0');
                System.out.println("La contraseña debe tener de 8 a 128 caracteres.");
                continue;
            }
            System.out.print("Repita la contraseña: ");
            char[] repetida = teclado.nextLine().toCharArray();
            boolean iguales = Arrays.equals(clave, repetida);
            Arrays.fill(repetida, '\0');
            if (iguales) return clave;
            Arrays.fill(clave, '\0');
            System.out.println("Las contraseñas no coinciden. Intente de nuevo.");
        }
    }

    private static int leerNumero(Scanner teclado, String mensaje, int minimo, int maximo) {
        while (true) {
            System.out.print(mensaje);
            try {
                int numero = Integer.parseInt(teclado.nextLine().trim());
                if (numero >= minimo && numero <= maximo) return numero;
            } catch (NumberFormatException e) {
                // Se vuelve a pedir el número.
            }
            System.out.println("Ingrese una opción entre " + minimo + " y " + maximo + ".");
        }
    }

    private static void registrarEncargado(Scanner teclado)
            throws IOException, GeneralSecurityException {
        List<Usuario> cuentas = cargar();
        String nombre = leerNombre(teclado, "Nombre completo: ");
        String usuario = leerUsuario(teclado, cuentas, null);
        char[] clave = leerClaveNueva(teclado);
        try {
            cuentas.add(Usuario.crear(usuario, nombre, Rol.ENCARGADO, clave));
            guardar(cuentas);
        } finally {
            Arrays.fill(clave, '\0');
        }
        System.out.println("Encargado registrado: " + usuario + ".");
    }

    private static Usuario pedirEncargado(Scanner teclado, List<Usuario> cuentas) {
        System.out.print("Usuario del encargado: ");
        String nombre = teclado.nextLine().trim().toLowerCase(Locale.ROOT);
        Usuario cuenta = buscar(cuentas, nombre);
        if (cuenta == null || !cuenta.getRol().equals(Rol.ENCARGADO)) {
            System.out.println("Encargado no encontrado.");
            return null;
        }
        return cuenta;
    }

    private static void actualizarEncargado(Scanner teclado)
            throws IOException, GeneralSecurityException {
        List<Usuario> cuentas = cargar();
        Usuario cuenta = pedirEncargado(teclado, cuentas);
        if (cuenta == null) return;
        System.out.println("1. Cambiar nombre");
        System.out.println("2. Cambiar usuario");
        System.out.println("3. Cambiar contraseña");
        System.out.println("4. Reactivar cuenta");
        System.out.println("0. Cancelar");
        int opcion = leerNumero(teclado, "Opción: ", 0, 4);
        switch (opcion) {
            case 1 -> cuenta.setNombreCompleto(
                    leerNombre(teclado, "Nuevo nombre: "));
            case 2 -> cuenta.setNombreUsuario(leerUsuario(teclado, cuentas,
                    cuenta.getNombreUsuario()));
            case 3 -> {
                char[] nueva = leerClaveNueva(teclado);
                try {
                    cuenta.cambiarClave(nueva);
                } finally {
                    Arrays.fill(nueva, '\0');
                }
            }
            case 4 -> cuenta.setActivo(true);
            default -> {
                System.out.println("Operación cancelada.");
                return;
            }
        }
        guardar(cuentas);
        System.out.println("Encargado actualizado.");
    }

    private static void listarEncargados() throws IOException {
        int total = 0;
        for (Usuario cuenta : cargar()) {
            if (cuenta.getRol().equals(Rol.ENCARGADO)) {
                System.out.println(cuenta.getNombreUsuario() + " | "
                        + cuenta.getNombreCompleto() + " | "
                        + (cuenta.estaActivo() ? "ACTIVO" : "DESHABILITADO"));
                total++;
            }
        }
        if (total == 0) System.out.println("No hay encargados registrados.");
    }

    private static void deshabilitarEncargado(Scanner teclado) throws IOException {
        List<Usuario> cuentas = cargar();
        Usuario cuenta = pedirEncargado(teclado, cuentas);
        if (cuenta == null) return;
        if (!cuenta.estaActivo()) {
            System.out.println("La cuenta ya está deshabilitada.");
            return;
        }
        System.out.print("¿Deshabilitar a " + cuenta.getNombreCompleto() + "? (S/N): ");
        if (!teclado.nextLine().trim().equalsIgnoreCase("S")) {
            System.out.println("Operación cancelada.");
            return;
        }
        cuenta.setActivo(false);
        guardar(cuentas);
        System.out.println("Cuenta deshabilitada. Ya no puede iniciar sesión.");
    }
}

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

    public static void inicializarAdministrador()
            throws IOException, GeneralSecurityException {
        if (Files.exists(ARCHIVO)) return;
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

    public static Usuario iniciarSesion(Scanner teclado)
            throws IOException, GeneralSecurityException {
        for (int intento = 1; intento <= 3; intento++) {
            System.out.println("\n=== INICIO DE SESIÓN ===");
            System.out.print("Usuario: ");
            String nombre = teclado.nextLine().trim().toLowerCase(Locale.ROOT);
            System.out.print("Contraseña: ");
            char[] clave = teclado.nextLine().toCharArray();
            try {
                Usuario encontrado = buscar(cargar(), nombre);
                if (encontrado != null && encontrado.estaActivo()
                        && encontrado.iniciarSesion(clave)) {
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
                    cuentas.add(cuenta);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Datos inválidos en usuarios.txt, línea "
                            + numeroLinea, e);
                }
            }
        }
        return cuentas;
    }

    private static void guardar(List<Usuario> cuentas) throws IOException {
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

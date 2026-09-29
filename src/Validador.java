import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Scanner;

public class Validador {

    // STRICT rechaza fechas imposibles como 2026-02-30
    public static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT);

    // ---------- Lectura con validación ----------

    public static String leerTexto(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = teclado.nextLine().trim();

            if (entrada.isEmpty()) {
                System.out.println("El campo no puede estar vacío.");
            } else if (entrada.contains(";")) {
                // ';' es el separador de los archivos .txt
                System.out.println("No se permite el carácter ';'.");
            } else {
                return entrada;
            }
        }
    }

    public static String leerCodigo(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = teclado.nextLine().trim().toUpperCase(Locale.ROOT);

            if (codigoValido(entrada)) {
                return entrada;
            }
            System.out.println(
                    "Código inválido. Use de 2 a 15 caracteres: "
                    + "letras, números o guion.");
        }
    }

    public static int leerEntero(Scanner teclado, String mensaje,
                                 int min, int max) {
        while (true) {
            System.out.print(mensaje);
            try {
                int numero = Integer.parseInt(teclado.nextLine().trim());

                if (numero >= min && numero <= max) {
                    return numero;
                }
                System.out.println(
                        "Ingrese un número entre " + min + " y " + max + ".");

            } catch (NumberFormatException e) {
                System.out.println("Debe ingresar un número entero.");
            }
        }
    }

    public static String leerFechaFutura(Scanner teclado) {
        while (true) {
            System.out.print("Fecha de vencimiento (AAAA-MM-DD): ");
            String entrada = teclado.nextLine().trim();

            try {
                LocalDate fecha = LocalDate.parse(entrada, FORMATO_FECHA);

                if (fecha.isBefore(LocalDate.now())) {
                    System.out.println(
                            "La fecha ya venció. Ingrese una fecha futura.");
                } else {
                    return fecha.format(FORMATO_FECHA);
                }

            } catch (DateTimeParseException e) {
                System.out.println(
                        "Fecha inválida. Use el formato AAAA-MM-DD "
                        + "(ej: 2027-12-31).");
            }
        }
    }

    // ---------- Comprobaciones simples ----------

    public static boolean codigoValido(String codigo) {
        return codigo != null && codigo.matches("[A-Z0-9-]{2,15}");
    }

    public static boolean esEnteroNoNegativo(String texto) {
        try {
            return Integer.parseInt(texto.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean fechaValida(String texto) {
        try {
            LocalDate.parse(texto.trim(), FORMATO_FECHA);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean estaVencido(String fecha) {
        return LocalDate.parse(fecha, FORMATO_FECHA).isBefore(LocalDate.now());
    }

    public static long diasParaVencer(String fecha) {
        return ChronoUnit.DAYS.between(
                LocalDate.now(), LocalDate.parse(fecha, FORMATO_FECHA));
    }
}

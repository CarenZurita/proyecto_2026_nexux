import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        int opcion;

        do {

            System.out.println();
            System.out.println("=================================");
            System.out.println("     GESTOR DE MEDICAMENTOS");
            System.out.println("=================================");
            System.out.println("1. Registrar medicamento");
            System.out.println("2. Buscar medicamento");
            System.out.println("3. Salir");
            System.out.print("Opción: ");

            String entrada = teclado.nextLine().trim();

            try {
                opcion = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                opcion = -1;
            }

            switch (opcion) {

                case 1:
                    registrarMedicamento(teclado);
                    break;

                case 2:
                    buscarMedicamento(teclado);
                    break;

                case 3:
                    System.out.println();
                    System.out.println("Programa finalizado.");
                    break;

                default:
                    System.out.println("Opción inválida. Intente de nuevo.");
                    break;
            }

        } while (opcion != 3);

        teclado.close();
    }


    public static void registrarMedicamento(Scanner teclado) {

        System.out.println();
        System.out.println("=================================");
        System.out.println("     REGISTRAR MEDICAMENTO");
        System.out.println("=================================");

        System.out.print("Nombre: ");
        String nombre = formatearTexto(teclado.nextLine());

        System.out.print("Código: ");
        String codigo = teclado.nextLine()
                .trim()
                .toUpperCase(Locale.ROOT);

        String categoria = seleccionarCategoria(teclado);

        String presentacion = seleccionarPresentacion(teclado);

        System.out.println();
        System.out.println("Seleccione el laboratorio:");
        System.out.println("1. INTI");
        System.out.println("2. Bagó");
        System.out.println("3. COFAR");
        System.out.println("4. LAFAR");
        System.out.println("5. VITA");

        System.out.print("Opción: ");
        int opcionLaboratorio =
                Integer.parseInt(teclado.nextLine());

        String laboratorio;

        switch (opcionLaboratorio) {

            case 1:
                laboratorio = "INTI";
                break;

            case 2:
                laboratorio = "Bagó";
                break;

            case 3:
                laboratorio = "COFAR";
                break;

            case 4:
                laboratorio = "LAFAR";
                break;

            case 5:
                laboratorio = "VITA";
                break;

            default:
                laboratorio = "No especificado";
                break;
        }

        System.out.print("Cantidad disponible: ");
        int cantidadDisponible =
                Integer.parseInt(teclado.nextLine());

        String fechaVencimiento = leerFechaValida(teclado);

        Medicamento medicamento = new Medicamento(
                nombre,
                codigo,
                categoria,
                presentacion,
                laboratorio,
                cantidadDisponible,
                fechaVencimiento
        );

        guardarMedicamento(
                nombre,
                codigo,
                categoria,
                presentacion,
                laboratorio,
                cantidadDisponible,
                fechaVencimiento
        );

        System.out.println();
        System.out.println("=================================");
        System.out.println("MEDICAMENTO REGISTRADO");
        System.out.println("=================================");

        medicamento.mostrarDatos();
    }


    
    public static void buscarMedicamento(Scanner teclado) {

        System.out.println();
        System.out.println("=================================");
        System.out.println("     BUSCAR MEDICAMENTO");
        System.out.println("=================================");
        System.out.print("Ingrese nombre o código a buscar: ");

        String busqueda = teclado.nextLine()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (busqueda.isEmpty()) {
            System.out.println("Debe ingresar un texto para buscar.");
        }

        try (BufferedReader archivo =
                     new BufferedReader(new FileReader("medicamentos.txt"))) {

            String linea;
            int encontrados = 0;

            while ((linea = archivo.readLine()) != null) {

                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split(";");

                if (datos.length < 7) {
                    continue;
                }

                System.out.println("Medicamento encontrado");
                String nombre = datos[0];
                String codigo = datos[1];
                String categoria = datos[2];
                String presentacion = datos[3];
                String laboratorio = datos[4];
                String fechaVencimiento = datos[6];

                int cantidadDisponible;

                try {
                    cantidadDisponible = Integer.parseInt(datos[5]);
                } catch (NumberFormatException e) {
                    cantidadDisponible = 0;
                }

                boolean coincide =
                        nombre.toLowerCase(Locale.ROOT).contains(busqueda)
                        || codigo.toLowerCase(Locale.ROOT).contains(busqueda);

                if (coincide) {

                    encontrados++;

                    Medicamento medicamento = new Medicamento(
                            nombre,
                            codigo,
                            categoria,
                            presentacion,
                            laboratorio,
                            cantidadDisponible,
                            fechaVencimiento
                    );

                    System.out.println();
                    System.out.println("---------------------------------");
                    medicamento.mostrarDatos();
                }
            }

            System.out.println();

            if (encontrados == 0) {
                System.out.println("No se encontraron coincidencias.");
            } else {
                System.out.println(
                        encontrados + " medicamento(s) encontrado(s)."
                );
            }

        } catch (IOException e) {
            System.out.println(
                    "Aún no hay medicamentos registrados o no se pudo "
                    + "leer el archivo."
            );
        }
    }


    public static String seleccionarCategoria(Scanner teclado) {

        String[] categorias = {
                "Analgésico",
                "Antibiótico",
                "Antiinflamatorio",
                "Antialérgico",
                "Antigripal",
                "Vitaminas y suplementos",
                "Antiácido / digestivo",
                "Otro"
        };

        String categoria = null;

        while (categoria == null) {

            System.out.println();
            System.out.println("Seleccione la categoría:");

            for (int i = 0; i < categorias.length; i++) {
                System.out.println((i + 1) + ". " + categorias[i]);
            }

            System.out.print("Opción: ");
            String entrada = teclado.nextLine().trim();

            try {

                int opcion = Integer.parseInt(entrada);

                if (opcion >= 1 && opcion <= categorias.length) {

                    categoria = categorias[opcion - 1];

                    if (categoria.equals("Otro")) {
                        System.out.print("Especifique la categoría: ");
                        categoria = formatearTexto(teclado.nextLine());
                    }

                } else {
                    System.out.println(
                            "Opción fuera de rango. Intente de nuevo."
                    );
                }

            } catch (NumberFormatException e) {
                System.out.println(
                        "Debe ingresar un número. Intente de nuevo."
                );
            }
        }

        return categoria;
    }


    public static String seleccionarPresentacion(Scanner teclado) {

        String[] presentaciones = {
                "Tabletas",
                "Cápsulas",
                "Jarabe",
                "Inyección",
                "Crema / pomada",
                "Gotas",
                "Suspensión",
                "Supositorio",
                "Otro"
        };

        String presentacion = null;

        while (presentacion == null) {

            System.out.println();
            System.out.println("Seleccione la presentación:");

            for (int i = 0; i < presentaciones.length; i++) {
                System.out.println((i + 1) + ". " + presentaciones[i]);
            }

            System.out.print("Opción: ");
            String entrada = teclado.nextLine().trim();

            try {

                int opcion = Integer.parseInt(entrada);

                if (opcion >= 1 && opcion <= presentaciones.length) {

                    presentacion = presentaciones[opcion - 1];

                    if (presentacion.equals("Otro")) {
                        System.out.print("Especifique la presentación: ");
                        presentacion = formatearTexto(teclado.nextLine());
                    }

                } else {
                    System.out.println(
                            "Opción fuera de rango. Intente de nuevo."
                    );
                }

            } catch (NumberFormatException e) {
                System.out.println(
                        "Debe ingresar un número. Intente de nuevo."
                );
            }
        }

        return presentacion;
    }


    
    public static String leerFechaValida(Scanner teclado) {

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate fecha = null;

        while (fecha == null) {

            System.out.print(
                    "Fecha de vencimiento (AAAA-MM-DD): "
            );

            String entrada = teclado.nextLine().trim();

            try {

                fecha = LocalDate.parse(entrada, formato);

                if (fecha.isBefore(LocalDate.now())) {
                    System.out.println(
                            "La fecha ya venció. Ingrese una fecha futura."
                    );
                    fecha = null;
                }

            } catch (DateTimeParseException e) {
                System.out.println(
                        "Fecha inválida. Use el formato AAAA-MM-DD "
                        + "(ej: 2026-12-31)."
                );
            }
        }

        return fecha.format(formato);
    }


    public static void guardarMedicamento(
            String nombre,
            String codigo,
            String categoria,
            String presentacion,
            String laboratorio,
            int cantidadDisponible,
            String fechaVencimiento) {

        try {

            BufferedWriter archivo =
                    new BufferedWriter(
                            new FileWriter(
                                    "medicamentos.txt",
                                    true
                            )
                    );

            archivo.write(
                    nombre + ";" +
                    codigo + ";" +
                    categoria + ";" +
                    presentacion + ";" +
                    laboratorio + ";" +
                    cantidadDisponible + ";" +
                    fechaVencimiento
            );

            archivo.newLine();

            archivo.close();

            System.out.println(
                    "Medicamento guardado correctamente."
            );

        } catch (IOException e) {

            System.out.println(
                    "Error al guardar el medicamento."
            );
        }
    }


    public static String formatearTexto(String texto) {

        texto = texto.trim().toLowerCase();

        if (texto.isEmpty()) {
            return texto;
        }

        String[] palabras = texto.split("\\s+");

        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {

            String palabraFormateada =
                    palabra.substring(0, 1).toUpperCase()
                    + palabra.substring(1);

            resultado.append(palabraFormateada).append(" ");
        }

        return resultado.toString().trim();
    }
}
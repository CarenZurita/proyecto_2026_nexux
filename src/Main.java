import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    static final String ARCHIVO_MEDICAMENTOS = "medicamentos.txt";
    static final String ARCHIVO_RESPALDO = "medicamentos.bak";
    static final String ARCHIVO_VENTAS = "ventas.txt";

    static final int CAMPOS = 7;
    static final int UMBRAL_STOCK_BAJO = 10;
    static final int STOCK_MAXIMO = 100000;

    static final String[] CATEGORIAS = {
        "Analgésico", "Antibiótico", "Antiinflamatorio", "Antialérgico",
        "Antigripal", "Vitaminas y suplementos", "Antiácido / digestivo",
        "Otro"
    };

    static final String[] PRESENTACIONES = {
        "Tabletas", "Cápsulas", "Jarabe", "Inyección", "Crema / pomada",
        "Gotas", "Suspensión", "Supositorio", "Otro"
    };

    static final String[] LABORATORIOS = {
        "INTI", "Bagó", "COFAR", "LAFAR", "VITA"
    };

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);
        int opcion;

        do {
            mostrarMenu();
            opcion = Validador.leerEntero(teclado, "Opción: ", 1, 6);

            switch (opcion) {
                case 1: registrarMedicamento(teclado); break;
                case 2: buscarMedicamento(teclado);    break;
                case 3: consultarStock(teclado);       break;
                case 4: registrarVenta(teclado);       break;
                case 5: modificarMedicamento(teclado); break;
                case 6:
                    System.out.println();
                    System.out.println("Programa finalizado.");
                    break;
            }

        } while (opcion != 6);

        teclado.close();
    }

    static void mostrarMenu() {
        System.out.println();
        System.out.println("=================================");
        System.out.println("     GESTOR DE MEDICAMENTOS");
        System.out.println("=================================");
        System.out.println("1. Registrar medicamento");
        System.out.println("2. Buscar medicamento");
        System.out.println("3. Consultar stock");
        System.out.println("4. Registrar venta");
        System.out.println("5. Modificar medicamento");
        System.out.println("6. Salir");
    }

    static void titulo(String texto) {
        System.out.println();
        System.out.println("=================================");
        System.out.println("     " + texto);
        System.out.println("=================================");
    }

    static void mostrarMedicamento(String[] d) {
        Medicamento medicamento = new Medicamento(
                d[0], d[1], d[2], d[3], d[4],
                Integer.parseInt(d[5]), d[6]);
        medicamento.mostrarDatos();
    }

    static boolean confirmar(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje + " (S/N): ");
            String r = teclado.nextLine().trim().toUpperCase(Locale.ROOT);

            if (r.equals("S")) return true;
            if (r.equals("N")) return false;
            System.out.println("Responda S o N.");
        }
    }

    static String seleccionarOpcion(Scanner teclado, String titulo,
                                    String[] opciones) {
        System.out.println();
        System.out.println(titulo);

        for (int i = 0; i < opciones.length; i++) {
            System.out.println((i + 1) + ". " + opciones[i]);
        }

        int n = Validador.leerEntero(teclado, "Opción: ", 1, opciones.length);
        String elegida = opciones[n - 1];

        if (elegida.equals("Otro")) {
            elegida = formatearTexto(
                    Validador.leerTexto(teclado, "Especifique: "));
        }
        return elegida;
    }

    static List<String[]> cargarMedicamentos() throws IOException {

        List<String[]> lista = new ArrayList<>();
        File archivo = new File(ARCHIVO_MEDICAMENTOS);

        if (!archivo.exists()) {
            return lista;
        }

        try (BufferedReader lector =
                     new BufferedReader(new FileReader(archivo))) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                if (linea.isBlank()) {
                    continue;
                }

                String[] d = linea.split(";", -1);

                boolean valida = d.length == CAMPOS
                        && !d[0].isBlank()
                        && Validador.codigoValido(d[1])
                        && Validador.esEnteroNoNegativo(d[5])
                        && Validador.fechaValida(d[6]);

                if (!valida) {
                    System.out.println("Aviso: la línea " + numeroLinea
                            + " de " + ARCHIVO_MEDICAMENTOS
                            + " es inválida y se omite.");
                    continue;
                }

                lista.add(d);
            }
        }
        return lista;
    }

    /** Reescribe el archivo completo, guardando antes un respaldo. */
    static void guardarMedicamentos(List<String[]> lista)
            throws IOException {

        Path original = Paths.get(ARCHIVO_MEDICAMENTOS);

        if (Files.exists(original)) {
            Files.copy(original, Paths.get(ARCHIVO_RESPALDO),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        try (BufferedWriter escritor =
                     new BufferedWriter(new FileWriter(ARCHIVO_MEDICAMENTOS))) {

            for (String[] d : lista) {
                escritor.write(String.join(";", d));
                escritor.newLine();
            }
        }
    }

    static void agregarMedicamento(String[] d) throws IOException {
        try (BufferedWriter escritor =
                     new BufferedWriter(
                             new FileWriter(ARCHIVO_MEDICAMENTOS, true))) {
            escritor.write(String.join(";", d));
            escritor.newLine();
        }
    }

    static void guardarVenta(String codigo, String nombre, int cantidad)
            throws IOException {

        try (BufferedWriter escritor =
                     new BufferedWriter(new FileWriter(ARCHIVO_VENTAS, true))) {
            escritor.write(codigo + ";" + nombre + ";" + cantidad
                    + ";" + LocalDate.now().format(Validador.FORMATO_FECHA));
            escritor.newLine();
        }
    }

    static String[] buscarPorCodigo(List<String[]> lista, String codigo) {
        for (String[] d : lista) {
            if (d[1].equals(codigo)) {
                return d;
            }
        }
        return null;
    }

    static void registrarMedicamento(Scanner teclado) {

        titulo("REGISTRAR MEDICAMENTO");

        try {
            List<String[]> lista = cargarMedicamentos();

            String nombre = formatearTexto(
                    Validador.leerTexto(teclado, "Nombre: "));

            String codigo;
            while (true) {
                codigo = Validador.leerCodigo(teclado, "Código: ");

                if (buscarPorCodigo(lista, codigo) == null) {
                    break;
                }
                System.out.println("Ya existe un medicamento con ese código.");
            }

            String categoria = seleccionarOpcion(
                    teclado, "Seleccione la categoría:", CATEGORIAS);
            String presentacion = seleccionarOpcion(
                    teclado, "Seleccione la presentación:", PRESENTACIONES);
            String laboratorio = seleccionarOpcion(
                    teclado, "Seleccione el laboratorio:", LABORATORIOS);

            int cantidad = Validador.leerEntero(
                    teclado, "Cantidad disponible: ", 1, STOCK_MAXIMO);

            String fecha = Validador.leerFechaFutura(teclado);

            String[] nuevo = {
                nombre, codigo, categoria, presentacion,
                laboratorio, String.valueOf(cantidad), fecha
            };

            agregarMedicamento(nuevo);

            titulo("MEDICAMENTO REGISTRADO");
            mostrarMedicamento(nuevo);

        } catch (IOException e) {
            System.out.println("Error al guardar el medicamento.");
        }
    }

    static void buscarMedicamento(Scanner teclado) {

        titulo("BUSCAR MEDICAMENTO");
        System.out.print("Ingrese nombre o código a buscar: ");

        String busqueda = teclado.nextLine().trim().toLowerCase(Locale.ROOT);

        if (busqueda.isEmpty()) {
            System.out.println("Debe ingresar un texto para buscar.");
            return;
        }

        try {
            List<String[]> lista = cargarMedicamentos();

            if (lista.isEmpty()) {
                System.out.println("Aún no hay medicamentos registrados.");
                return;
            }

            int encontrados = 0;

            for (String[] d : lista) {
                boolean coincide =
                        d[0].toLowerCase(Locale.ROOT).contains(busqueda)
                        || d[1].toLowerCase(Locale.ROOT).contains(busqueda);

                if (coincide) {
                    encontrados++;
                    System.out.println();
                    System.out.println("---------------------------------");
                    mostrarMedicamento(d);
                }
            }

            System.out.println();

            if (encontrados == 0) {
                System.out.println("No se encontraron coincidencias.");
            } else {
                System.out.println(
                        encontrados + " medicamento(s) encontrado(s).");
            }

        } catch (IOException e) {
            System.out.println("No se pudo leer " + ARCHIVO_MEDICAMENTOS);
        }
    }

    static void consultarStock(Scanner teclado) {

        titulo("CONSULTAR STOCK");

        String codigo = Validador.leerCodigo(
                teclado, "Ingrese código del medicamento: ");

        try {
            String[] d = buscarPorCodigo(cargarMedicamentos(), codigo);

            if (d == null) {
                System.out.println("Medicamento no encontrado.");
                return;
            }

            int stock = Integer.parseInt(d[5]);

            System.out.println();
            System.out.println("Nombre: " + d[0]);
            System.out.println("Código: " + d[1]);
            System.out.println("Cantidad disponible: " + stock);

            if (stock == 0) {
                System.out.println("Estado: SIN STOCK");
            } else if (stock <= UMBRAL_STOCK_BAJO) {
                System.out.println("Estado: STOCK BAJO");
            } else {
                System.out.println("Estado: DISPONIBLE");
            }

            if (Validador.estaVencido(d[6])) {
                System.out.println("Vencimiento: VENCIDO (" + d[6] + ")");
            } else {
                System.out.println("Vencimiento: " + d[6] + " (faltan "
                        + Validador.diasParaVencer(d[6]) + " días)");
            }

        } catch (IOException e) {
            System.out.println("Error al leer " + ARCHIVO_MEDICAMENTOS);
        }
    }

    static void registrarVenta(Scanner teclado) {

        titulo("REGISTRAR VENTA");

        String codigo = Validador.leerCodigo(teclado, "Código medicamento: ");

        try {
            List<String[]> lista = cargarMedicamentos();
            String[] d = buscarPorCodigo(lista, codigo);

            if (d == null) {
                System.out.println("Medicamento no encontrado.");
                return;
            }

            if (Validador.estaVencido(d[6])) {
                System.out.println(
                        "No se puede vender: el medicamento está vencido.");
                return;
            }

            int stock = Integer.parseInt(d[5]);

            if (stock == 0) {
                System.out.println("No hay stock disponible.");
                return;
            }

            System.out.println("Medicamento: " + d[0]
                    + " | Stock disponible: " + stock);

            int cantidad = Validador.leerEntero(
                    teclado, "Cantidad vendida: ", 1, stock);

            int nuevoStock = stock - cantidad;
            d[5] = String.valueOf(nuevoStock);

            if (nuevoStock == 0) {
                lista.remove(d);
            }

            guardarMedicamentos(lista);
            guardarVenta(d[1], d[0], cantidad);

            System.out.println("Venta registrada correctamente.");

            if (nuevoStock == 0) {
                System.out.println("Stock agotado: " + d[0]
                        + " fue eliminado del sistema.");
            } else if (nuevoStock <= UMBRAL_STOCK_BAJO) {
                System.out.println("Aviso: quedan " + nuevoStock
                        + " unidades (stock bajo).");
            }

        } catch (IOException e) {
            System.out.println("Error registrando la venta.");
        }
    }

    static void modificarMedicamento(Scanner teclado) {

        titulo("MODIFICAR MEDICAMENTO");

        String codigo = Validador.leerCodigo(
                teclado, "Código del medicamento: ");

        try {
            List<String[]> lista = cargarMedicamentos();
            String[] d = buscarPorCodigo(lista, codigo);

            if (d == null) {
                System.out.println("Medicamento no encontrado.");
                return;
            }

            System.out.println();
            System.out.println("Datos actuales:");
            mostrarMedicamento(d);

            System.out.println();
            System.out.println("¿Qué desea modificar?");
            System.out.println("1. Nombre");
            System.out.println("2. Cantidad disponible");
            System.out.println("3. Fecha de vencimiento");
            System.out.println("4. Cancelar");

            int opcion = Validador.leerEntero(teclado, "Opción: ", 1, 4);

            switch (opcion) {
                case 1:
                    d[0] = formatearTexto(
                            Validador.leerTexto(teclado, "Nuevo nombre: "));
                    break;
                case 2:
                    d[5] = String.valueOf(Validador.leerEntero(
                            teclado, "Nueva cantidad: ", 0, STOCK_MAXIMO));
                    break;
                case 3:
                    d[6] = Validador.leerFechaFutura(teclado);
                    break;
                default:
                    System.out.println("Operación cancelada.");
                    return;
            }

            if (d[5].equals("0")) {
                lista.remove(d);
                guardarMedicamentos(lista);
                System.out.println("Stock en 0: el medicamento fue "
                        + "eliminado del sistema.");
            } else {
                guardarMedicamentos(lista);
                System.out.println("Medicamento actualizado correctamente.");
            }

        } catch (IOException e) {
            System.out.println("Error al modificar el medicamento.");
        }
    }

    static String formatearTexto(String texto) {

        texto = texto.trim().toLowerCase();

        if (texto.isEmpty()) {
            return texto;
        }

        String[] palabras = texto.split("\\s+");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            resultado.append(palabra.substring(0, 1).toUpperCase())
                     .append(palabra.substring(1))
                     .append(" ");
        }

        return resultado.toString().trim();
    }
}

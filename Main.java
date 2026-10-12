import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import model.Administrador;
import model.GestorUsuarios;
import model.Medicamento;
import model.Usuario;
import model.Venta;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;
import util.Validaciones;

public class Main {
    private static final MedicamentoRepository MEDICAMENTOS = new MedicamentoRepository();
    private static final VentaRepository VENTAS = new VentaRepository();

    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            GestorUsuarios.inicializarAdministrador();
            while (true) {
                Usuario sesion = GestorUsuarios.iniciarSesion(teclado);
                if (sesion == null) {
                    System.out.println("Se agotaron los intentos. Programa finalizado.");
                    break;
                }
                if (sesion.esAdministrador()) {
                    menuAdministrador(teclado, sesion);
                } else {
                    menuEncargado(teclado, sesion);
                }
                sesion.cerrarSesion();
                System.out.println("Sesión cerrada.\n");
            }
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("No se pudo gestionar usuarios: " + e.getMessage());
        }
    }

    private static int leerOpcion(Scanner teclado, int maximo) {
        while (true) {
            System.out.print("Opción: ");
            try {
                int opcion = Integer.parseInt(teclado.nextLine().trim());
                if (opcion >= 0 && opcion <= maximo) {
                    return opcion;
                }
            } catch (NumberFormatException e) {
                // Se vuelve a pedir una opción válida.
            }
            System.out.println("Opción inválida. Intente de nuevo.");
        }
    }

    private static void menuAdministrador(Scanner teclado, Usuario sesion)
            throws IOException, GeneralSecurityException {
        int opcion;
        do {
            System.out.println("\n=== MENÚ DEL ADMINISTRADOR ===");
            System.out.println("1. Buscar medicamento");
            System.out.println("2. Consultar stock");
            System.out.println("3. Registrar medicamento");
            System.out.println("4. Actualizar medicamento");
            System.out.println("5. Registrar venta");
            System.out.println("6. Revisar vencimiento");
            System.out.println("7. Eliminar medicamento caducado");
            System.out.println("8. Gestionar encargados");
            System.out.println("9. Consultar historial de ventas");
            System.out.println("0. Cerrar sesión");
            opcion = leerOpcion(teclado, 9);
            switch (opcion) {
                case 1 -> buscarMedicamento(teclado, sesion);
                case 2 -> consultarStock(teclado, sesion);
                case 3 -> registrarMedicamento(teclado, sesion);
                case 4 -> actualizarMedicamento(teclado, sesion);
                case 5 -> registrarVenta(teclado, sesion);
                case 6 -> controlarVencimientos(sesion);
                case 7 -> eliminarMedicamentoCaducado(teclado, sesion);
                case 8 -> ((Administrador) sesion).gestionarUsuarios(teclado);
                case 9 -> consultarHistorialVentas(sesion);
                default -> { }
            }
        } while (opcion != 0);
    }

    private static void menuEncargado(Scanner teclado, Usuario sesion) {
        int opcion;
        do {
            System.out.println("\n=== MENÚ DEL ENCARGADO ===");
            System.out.println("1. Buscar medicamento");
            System.out.println("2. Consultar stock");
            System.out.println("3. Registrar venta");
            System.out.println("4. Revisar vencimiento");
            System.out.println("0. Cerrar sesión");
            opcion = leerOpcion(teclado, 4);
            switch (opcion) {
                case 1 -> buscarMedicamento(teclado, sesion);
                case 2 -> consultarStock(teclado, sesion);
                case 3 -> registrarVenta(teclado, sesion);
                case 4 -> controlarVencimientos(sesion);
                default -> { }
            }
        } while (opcion != 0);
    }

    private static boolean permitirSesion(Usuario usuario) {
        if (usuario == null || !usuario.estaActivo()) {
            System.out.println("Acceso denegado: debe iniciar sesión.");
            return false;
        }
        return true;
    }

    private static boolean permitirAdministrador(Usuario usuario) {
        if (usuario == null || !usuario.esAdministrador() || !usuario.estaActivo()) {
            System.out.println(
                    "Acceso denegado: solo el administrador puede realizar esta operación.");
            return false;
        }
        return true;
    }

    public static void registrarMedicamento(Scanner teclado, Usuario sesion) {
        if (!permitirAdministrador(sesion)) {
            return;
        }

        System.out.println("\n=================================");
        System.out.println("     REGISTRAR MEDICAMENTO");
        System.out.println("=================================");
        System.out.print("Nombre: ");
        String nombre = formatearTexto(teclado.nextLine());
        System.out.print("Código: ");
        String codigo = teclado.nextLine().trim().toUpperCase(Locale.ROOT);
        String categoria = seleccionarCategoria(teclado);
        String presentacion = seleccionarPresentacion(teclado);
        String laboratorio = seleccionarLaboratorio(teclado);
        String marca = leerTextoNoVacio(teclado, "Marca: ");
        int cantidadDisponible = leerCantidad(teclado, "Cantidad disponible: ", false);
        String fechaVencimiento = leerFechaValida(teclado);
        double precio = leerPrecio(teclado, "Precio: ");
        int stockMinimo = leerCantidad(teclado, "Stock mínimo: ", false);

        Medicamento medicamento = new Medicamento(nombre, codigo, categoria,
                presentacion, laboratorio, marca, cantidadDisponible,
                fechaVencimiento, precio, stockMinimo);
        try {
            ((Administrador) sesion).registrarMedicamento(medicamento, MEDICAMENTOS);
            System.out.println("\n=================================");
            System.out.println("MEDICAMENTO REGISTRADO");
            System.out.println("=================================");
            medicamento.mostrarDatos();
        } catch (IOException | SecurityException e) {
            System.out.println("No se pudo registrar el medicamento: " + e.getMessage());
        }
    }

    public static void buscarMedicamento(Scanner teclado, Usuario sesion) {
        if (!permitirSesion(sesion)) {
            return;
        }
        System.out.println("\n=================================");
        System.out.println("     BUSCAR MEDICAMENTO");
        System.out.println("=================================");
        System.out.print("Ingrese nombre, código, marca o precio exacto: ");
        String busqueda = teclado.nextLine().trim();
        if (busqueda.isEmpty()) {
            System.out.println("Debe ingresar un texto para buscar.");
            return;
        }

        try {
            List<Medicamento> encontrados = sesion.buscarMedicamento(busqueda, MEDICAMENTOS);
            if (encontrados.isEmpty()) {
                System.out.println("No se encontraron coincidencias.");
                return;
            }
            for (Medicamento medicamento : encontrados) {
                System.out.println("\n---------------------------------");
                System.out.println("Medicamento encontrado");
                System.out.println("---------------------------------");
                medicamento.mostrarDatos();
            }
            System.out.println("\n" + encontrados.size() + " medicamento(s) encontrado(s).");
        } catch (IOException | SecurityException e) {
            System.out.println("No se pudieron consultar los medicamentos: " + e.getMessage());
        }
    }

    public static void consultarStock(Scanner teclado, Usuario sesion) {
        if (!permitirSesion(sesion)) {
            return;
        }
        System.out.println("\n===============================");
        System.out.println("      CONSULTAR STOCK");
        System.out.println("===============================");
        System.out.print("Ingrese código del medicamento: ");
        String codigo = teclado.nextLine().trim().toUpperCase(Locale.ROOT);

        try {
            Optional<Medicamento> resultado = sesion.consultarStock(codigo, MEDICAMENTOS);
            if (resultado.isEmpty()) {
                System.out.println("Medicamento no encontrado.");
                return;
            }
            Medicamento medicamento = resultado.get();
            System.out.println("\nMedicamento encontrado");
            System.out.println("Nombre: " + medicamento.getNombre());
            System.out.println("Código: " + medicamento.getCodigo());
            System.out.println("Cantidad disponible: " + medicamento.getCantidadDisponible());
            System.out.println(medicamento.getCantidadDisponible() == 0
                    ? "Estado: SIN STOCK" : "Estado: DISPONIBLE");
        } catch (IOException | SecurityException e) {
            System.out.println("No se pudo consultar el stock: " + e.getMessage());
        }
    }

    public static void registrarVenta(Scanner teclado, Usuario sesion) {
        if (!permitirSesion(sesion)) {
            return;
        }
        System.out.println("\n===============================");
        System.out.println("       REGISTRAR VENTA");
        System.out.println("===============================");
        System.out.print("Código medicamento: ");
        String codigo = teclado.nextLine().trim().toUpperCase(Locale.ROOT);
        int cantidad = leerCantidad(teclado, "Cantidad vendida: ", true);
        try {
            sesion.registrarVenta(codigo, cantidad, MEDICAMENTOS, VENTAS);
            System.out.println("Venta registrada correctamente.");
        } catch (IOException | SecurityException | IllegalArgumentException
                 | IllegalStateException e) {
            System.out.println("No se pudo registrar la venta: " + e.getMessage());
        }
    }

    public static void actualizarMedicamento(Scanner teclado, Usuario sesion) {
        if (!permitirAdministrador(sesion)) {
            return;
        }
        System.out.println("\n=================================");
        System.out.println("     ACTUALIZAR MEDICAMENTO");
        System.out.println("=================================");
        System.out.print("Ingrese código del medicamento: ");
        String codigo = teclado.nextLine().trim().toUpperCase(Locale.ROOT);
        if (codigo.isEmpty()) {
            System.out.println("El código no puede estar vacío.");
            return;
        }

        try {
            Optional<Medicamento> resultado =
                    ((Administrador) sesion).buscarMedicamentoPorCodigo(codigo, MEDICAMENTOS);
            if (resultado.isEmpty()) {
                System.out.println("No existe un medicamento con ese código.");
                return;
            }
            Medicamento medicamento = resultado.get();
            System.out.println("\nMedicamento encontrado.");
            System.out.println("Nombre actual: " + medicamento.getNombre());
            System.out.println("Categoría actual: " + medicamento.getCategoria());
            System.out.println("Presentación actual: " + medicamento.getPresentacion());
            System.out.println("Laboratorio actual: " + medicamento.getLaboratorio());
            System.out.println("Marca actual: " + medicamento.getMarca());
            System.out.println("Cantidad actual: " + medicamento.getCantidadDisponible());
            System.out.println("Precio actual: " + medicamento.getPrecio());
            System.out.println("Stock mínimo actual: " + medicamento.getStockMinimo());
            System.out.println("Vencimiento actual: " + medicamento.getFechaVencimiento());

            System.out.println("\nIngrese los nuevos datos.");
            String nombre = leerTextoNoVacio(teclado, "Nuevo nombre: ");
            String categoria = seleccionarCategoria(teclado);
            String presentacion = seleccionarPresentacion(teclado);
            String laboratorio = seleccionarLaboratorio(teclado);
            String marca = leerTextoNoVacio(teclado, "Nueva marca: ");
            int cantidad = leerCantidad(teclado, "Nueva cantidad disponible: ", false);
            String vencimiento = leerFechaValida(teclado);
            double precio = leerPrecio(teclado, "Nuevo precio: ");
            int stockMinimo = leerCantidad(teclado, "Nuevo stock mínimo: ", false);
            medicamento.actualizarInfo(nombre, categoria, presentacion,
                    laboratorio, marca, cantidad, vencimiento, precio, stockMinimo);

            if (((Administrador) sesion).actualizarMedicamento(medicamento, MEDICAMENTOS)) {
                System.out.println("Medicamento actualizado correctamente.");
            } else {
                System.out.println("No se pudo encontrar el medicamento para actualizarlo.");
            }
        } catch (IOException | SecurityException e) {
            System.out.println("No se pudo actualizar el medicamento: " + e.getMessage());
        }
    }

    public static String seleccionarCategoria(Scanner teclado) {
        String[] categorias = {
                "Analgésico", "Antibiótico", "Antiinflamatorio", "Antialérgico",
                "Antigripal", "Vitaminas y suplementos", "Antiácido / digestivo", "Otro"
        };
        while (true) {
            System.out.println("\nSeleccione la categoría:");
            for (int i = 0; i < categorias.length; i++) {
                System.out.println((i + 1) + ". " + categorias[i]);
            }
            int opcion = leerOpcion(teclado, categorias.length);
            if (opcion == 0) {
                System.out.println("Seleccione una opción de la lista.");
                continue;
            }
            String categoria = categorias[opcion - 1];
            if (categoria.equals("Otro")) {
                return leerTextoNoVacio(teclado, "Especifique la categoría: ");
            }
            return categoria;
        }
    }

    public static String seleccionarPresentacion(Scanner teclado) {
        String[] presentaciones = {
                "Tabletas", "Cápsulas", "Jarabe", "Inyección", "Crema / pomada",
                "Gotas", "Suspensión", "Supositorio", "Otro"
        };
        while (true) {
            System.out.println("\nSeleccione la presentación:");
            for (int i = 0; i < presentaciones.length; i++) {
                System.out.println((i + 1) + ". " + presentaciones[i]);
            }
            int opcion = leerOpcion(teclado, presentaciones.length);
            if (opcion == 0) {
                System.out.println("Seleccione una opción de la lista.");
                continue;
            }
            String presentacion = presentaciones[opcion - 1];
            if (presentacion.equals("Otro")) {
                return leerTextoNoVacio(teclado, "Especifique la presentación: ");
            }
            return presentacion;
        }
    }

    public static String seleccionarLaboratorio(Scanner teclado) {
        String[] laboratorios = {"INTI", "Bagó", "COFAR", "LAFAR", "VITA"};
        while (true) {
            System.out.println("\nSeleccione el laboratorio:");
            for (int i = 0; i < laboratorios.length; i++) {
                System.out.println((i + 1) + ". " + laboratorios[i]);
            }
            int opcion = leerOpcion(teclado, laboratorios.length);
            if (opcion > 0) {
                return laboratorios[opcion - 1];
            }
        }
    }

    public static String leerFechaValida(Scanner teclado) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print("Fecha de vencimiento (AAAA-MM-DD): ");
            String entrada = teclado.nextLine().trim();
            try {
                LocalDate fecha = LocalDate.parse(entrada, formato);
                if (fecha.isBefore(LocalDate.now())) {
                    System.out.println("La fecha ya venció. Ingrese una fecha futura.");
                    continue;
                }
                return fecha.format(formato);
            } catch (DateTimeParseException e) {
                System.out.println("Fecha inválida. Use el formato AAAA-MM-DD (ej: 2026-12-31).");
            }
        }
    }

    public static void controlarVencimientos(Usuario sesion) {
        if (!permitirSesion(sesion)) {
            return;
        }
        System.out.println("\n=================================");
        System.out.println("     CONTROL DE VENCIMIENTOS");
        System.out.println("=================================");
        try {
            List<Medicamento> cercanos = sesion.revisarVencimientos(MEDICAMENTOS);
            if (cercanos.isEmpty()) {
                System.out.println("No existen medicamentos próximos a vencer o vencidos.");
                return;
            }
            for (Medicamento medicamento : cercanos) {
                System.out.println("\n---------------------------------");
                System.out.println("Medicamento: " + medicamento.getNombre());
                System.out.println("Código: " + medicamento.getCodigo());
                System.out.println("Fecha de vencimiento: "
                        + medicamento.getFechaVencimiento());
                System.out.println("Estado: "
                        + (medicamento.estaVencido() ? "VENCIDO" : "PRÓXIMO A VENCER"));
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            System.out.println("No se pudieron revisar los vencimientos: " + e.getMessage());
        }
    }

    public static void eliminarMedicamentoCaducado(Scanner teclado, Usuario sesion) {
        if (!permitirAdministrador(sesion)) {
            return;
        }
        System.out.println("\n=================================");
        System.out.println(" ELIMINAR MEDICAMENTO CADUCADO");
        System.out.println("=================================");
        System.out.print("Ingrese código del medicamento: ");
        String codigo = teclado.nextLine().trim().toUpperCase(Locale.ROOT);
        if (codigo.isEmpty()) {
            System.out.println("El código no puede estar vacío.");
            return;
        }

        try {
            Optional<Medicamento> resultado =
                    ((Administrador) sesion).buscarMedicamentoPorCodigo(codigo, MEDICAMENTOS);
            if (resultado.isEmpty()) {
                System.out.println("Medicamento no encontrado.");
                return;
            }
            Medicamento medicamento = resultado.get();
            if (!medicamento.estaVencido()) {
                System.out.println("\nEl medicamento todavía está vigente.");
                System.out.println("No puede ser eliminado.");
                return;
            }
            System.out.println("\nMedicamento caducado encontrado:");
            System.out.println("Nombre: " + medicamento.getNombre());
            System.out.println("Código: " + medicamento.getCodigo());
            System.out.println("Fecha de vencimiento: " + medicamento.getFechaVencimiento());
            System.out.print("¿Desea eliminarlo? (S/N): ");
            if (!teclado.nextLine().trim().equalsIgnoreCase("S")) {
                System.out.println("Eliminación cancelada.");
                return;
            }
            if (((Administrador) sesion).eliminarMedicamentoCaducado(codigo, MEDICAMENTOS)) {
                System.out.println("Medicamento eliminado correctamente.");
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            System.out.println("No se pudo eliminar el medicamento: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void consultarHistorialVentas(Usuario sesion) {
        if (!permitirAdministrador(sesion)) {
            return;
        }
        System.out.println("\n=== HISTORIAL DE VENTAS ===");
        try {
            List<Venta> historial =
                    ((Administrador) sesion).consultarHistorialVentas(VENTAS);
            if (historial.isEmpty()) {
                System.out.println("Aún no hay ventas registradas.");
                return;
            }
            for (Venta venta : historial) {
                String fecha = venta.getFecha() == null
                        ? "sin fecha registrada" : venta.getFecha().toString();
                System.out.println(fecha + " | " + venta.getNombreMedicamento()
                        + " (" + venta.getCodigoMedicamento() + ") | Cantidad: "
                        + venta.getCantidad());
            }
        } catch (IOException | SecurityException e) {
            System.out.println("No se pudo consultar el historial de ventas: " + e.getMessage());
        }
    }

    public static String formatearTexto(String texto) {
        return Validaciones.formatearTexto(texto);
    }

    private static String leerTextoNoVacio(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String texto = formatearTexto(teclado.nextLine());
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("El texto no puede estar vacío.");
        }
    }

    private static int leerCantidad(Scanner teclado, String mensaje, boolean debeSerPositiva) {
        while (true) {
            System.out.print(mensaje);
            try {
                int cantidad = Integer.parseInt(teclado.nextLine().trim());
                if (cantidad > 0 || (!debeSerPositiva && cantidad >= 0)) {
                    return cantidad;
                }
            } catch (NumberFormatException e) {
                // Se vuelve a pedir una cantidad válida.
            }
            System.out.println(debeSerPositiva
                    ? "La cantidad debe ser un entero positivo."
                    : "La cantidad no puede ser negativa.");
        }
    }

    private static double leerPrecio(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            try {
                double precio = Double.parseDouble(teclado.nextLine().trim().replace(',', '.'));
                if (Double.isFinite(precio) && precio >= 0) {
                    return precio;
                }
            } catch (NumberFormatException e) {
                // Se vuelve a pedir un precio válido.
            }
            System.out.println("El precio debe ser un número mayor o igual a cero.");
        }
    }
}

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import model.Administrador;
import model.Usuario;
import model.Venta;
import persistence.MedicamentoRepository;
import persistence.UsuarioRepository;
import persistence.VentaRepository;
import service.AutenticacionService;
import service.GestionUsuariosService;
import service.MedicamentoService;
import service.RecuperacionService;
import service.UsuarioService;

/**
 * Punto de entrada y composición de dependencias de la aplicación Salud Vida.
 */
public class Main {
    private static final MedicamentoRepository MEDICAMENTOS =
            new MedicamentoRepository();

    private static final MedicamentoService MEDICAMENTO_SERVICE =
            new MedicamentoService(MEDICAMENTOS);

    private static final controller.MedicamentoController MEDICAMENTO_CONTROLLER =
            new controller.MedicamentoController(MEDICAMENTO_SERVICE);

    private static final VentaRepository VENTAS =
            new VentaRepository();

    /**
     * Construye los repositorios y servicios compartidos e inicia Swing en el EDT.
     *
     * @param args argumentos de línea de comandos, actualmente no utilizados
     */
    public static void main(String[] args) {
        UsuarioRepository usuarios = new UsuarioRepository();
        AutenticacionService autenticacion = new AutenticacionService(usuarios);
        RecuperacionService recuperacion = new RecuperacionService(usuarios);
        GestionUsuariosService gestionUsuarios = new GestionUsuariosService(usuarios);
        UsuarioService usuariosService = new UsuarioService(gestionUsuarios);
        controller.SeguridadCuentaController seguridadCuentaController =
                new controller.SeguridadCuentaController(gestionUsuarios, recuperacion);
        // Una sola instancia de cada dependencia mantiene consistente el ciclo de login/logout.
        javax.swing.SwingUtilities.invokeLater(() ->
                controller.LoginController.mostrarLogin(
                        autenticacion, recuperacion, usuariosService,
                        seguridadCuentaController, MEDICAMENTO_CONTROLLER));
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

}
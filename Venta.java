package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

public class Venta {
    private LocalDate fecha;
    private double total;
    private Usuario usuario;
    private String usuarioRegistrado;
    private final List<DetalleVenta> detalles = new ArrayList<>();
    private transient MedicamentoRepository repositorioMedicamentos;
    private transient VentaRepository repositorioVentas;
    private boolean registrada;
    private boolean cancelada;

    public Venta(Usuario usuario) {
        this.usuario = Objects.requireNonNull(usuario, "El usuario es obligatorio.");
        this.usuarioRegistrado = usuario.getNombreUsuario();
        this.fecha = LocalDate.now();
    }

    public Venta(String codigoMedicamento, int cantidad) {
        if (codigoMedicamento == null || codigoMedicamento.isBlank()) {
            throw new IllegalArgumentException("El código del medicamento es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        this.fecha = LocalDate.now();
        this.usuarioRegistrado = "";
        Medicamento medicamento = new Medicamento("Pendiente", codigoMedicamento,
                "Sin categoría", "Sin presentación", "Sin laboratorio",
                cantidad, "9999-12-31");
        detalles.add(new DetalleVenta(medicamento, cantidad));
        total = cantidad * medicamento.getPrecio();
    }

    public void registrar(Usuario usuario, MedicamentoRepository medicamentos,
                          VentaRepository ventas) throws IOException {
        if (cancelada || registrada) {
            throw new IllegalStateException("La venta ya fue registrada o cancelada.");
        }
        if (usuario == null || !usuario.estaActivo() || !usuario.estaEnSesion()) {
            throw new SecurityException("Debe iniciar sesión para registrar una venta.");
        }

        if (detalles.isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener al menos un detalle.");
        }

        Map<String, Medicamento> medicamentosActuales = new LinkedHashMap<>();
        Map<String, Integer> cantidadesPorCodigo = new LinkedHashMap<>();
        for (DetalleVenta detalle : detalles) {
            String codigo = detalle.getMedicamento().getCodigo().toUpperCase(Locale.ROOT);
            Medicamento actual = medicamentosActuales.get(codigo);
            if (actual == null) {
                actual = medicamentos.buscarPorCodigo(codigo)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Medicamento no encontrado."));
                medicamentosActuales.put(codigo, actual);
            }
            cantidadesPorCodigo.merge(codigo, detalle.getCantidad(), Math::addExact);
        }
        for (Map.Entry<String, Integer> entrada : cantidadesPorCodigo.entrySet()) {
            validarMedicamentoParaVenta(
                    medicamentosActuales.get(entrada.getKey()), entrada.getValue());
        }
        List<String> stocksReducidos = new ArrayList<>();

        this.usuario = usuario;
        this.usuarioRegistrado = usuario.getNombreUsuario();
        this.fecha = LocalDate.now();
        List<DetalleVenta> detallesSolicitados = new ArrayList<>(detalles);
        detalles.clear();
        for (DetalleVenta detalle : detallesSolicitados) {
            Medicamento actual = medicamentosActuales.get(
                    detalle.getMedicamento().getCodigo().toUpperCase(Locale.ROOT));
            detalles.add(new DetalleVenta(
                    actual, detalle.getCantidad(), detalle.getPrecioUnitario()));
        }
        this.total = calcularTotal();

        try {
            for (Map.Entry<String, Integer> entrada : cantidadesPorCodigo.entrySet()) {
                Medicamento medicamento = medicamentosActuales.get(entrada.getKey());
                medicamento.reducirCantidad(entrada.getValue());
                stocksReducidos.add(entrada.getKey());
                if (!medicamentos.actualizar(medicamento)) {
                    throw new IOException("No se pudo actualizar el stock del medicamento.");
                }
            }
            ventas.registrar(this);
            this.repositorioMedicamentos = medicamentos;
            this.repositorioVentas = ventas;
            this.registrada = true;
        } catch (IOException e) {
            for (int i = stocksReducidos.size() - 1; i >= 0; i--) {
                String codigo = stocksReducidos.get(i);
                Medicamento medicamento = medicamentosActuales.get(codigo);
                try {
                    medicamento.reponerCantidad(cantidadesPorCodigo.get(codigo));
                    if (!medicamentos.actualizar(medicamento)) {
                        e.addSuppressed(new IOException(
                                "No se pudo restaurar el stock tras fallar el registro de la venta."));
                    }
                } catch (IOException | IllegalArgumentException errorAlRestaurar) {
                    e.addSuppressed(errorAlRestaurar);
                }
            }
            throw e;
        }
    }

    private static void validarMedicamentoParaVenta(Medicamento medicamento, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida.");
        }
        if (medicamento.estaVencido()) {
            throw new IllegalStateException("El medicamento se encuentra vencido.");
        }
        if (cantidad > medicamento.getCantidadDisponible()) {
            throw new IllegalArgumentException("Stock insuficiente.");
        }
    }

    public void agregarDetalle(DetalleVenta detalle) {
        if (registrada || cancelada) {
            throw new IllegalStateException(
                    "No se pueden modificar los detalles de una venta finalizada.");
        }
        detalles.add(Objects.requireNonNull(detalle, "El detalle es obligatorio."));
        total = calcularTotal();
    }

    public double calcularTotal() {
        total = detalles.stream().mapToDouble(DetalleVenta::calcularSubtotal).sum();
        return total;
    }

    public List<DetalleVenta> obtenerDetalle() {
        return List.copyOf(detalles);
    }

    public void cancelar() throws IOException {
        if (cancelada) {
            throw new IllegalStateException("La venta ya fue cancelada.");
        }
        if (!registrada || repositorioMedicamentos == null || repositorioVentas == null) {
            throw new IllegalStateException("Solo se puede cancelar una venta registrada.");
        }

        Map<String, Integer> cantidadesPorCodigo = new LinkedHashMap<>();
        for (DetalleVenta detalle : detalles) {
            cantidadesPorCodigo.merge(
                    detalle.getMedicamento().getCodigo().toUpperCase(Locale.ROOT),
                    detalle.getCantidad(), Math::addExact);
        }
        Map<String, Medicamento> medicamentosActuales = new LinkedHashMap<>();
        for (String codigo : cantidadesPorCodigo.keySet()) {
            Medicamento existente = repositorioMedicamentos.buscarPorCodigo(codigo)
                    .orElseThrow(() -> new IOException(
                            "No se encontró un medicamento de la venta al cancelarla."));
            medicamentosActuales.put(codigo, existente);
        }

        List<String> stocksRepuestos = new ArrayList<>();
        try {
            for (Map.Entry<String, Integer> entrada : cantidadesPorCodigo.entrySet()) {
                Medicamento medicamento = medicamentosActuales.get(entrada.getKey());
                medicamento.reponerCantidad(entrada.getValue());
                stocksRepuestos.add(entrada.getKey());
                if (!repositorioMedicamentos.actualizar(medicamento)) {
                    throw new IOException("No se pudo restaurar el stock de la venta.");
                }
            }
            if (!repositorioVentas.eliminar(this)) {
                throw new IOException("No se encontró el registro persistido de la venta.");
            }
        } catch (IOException e) {
            for (int i = stocksRepuestos.size() - 1; i >= 0; i--) {
                String codigo = stocksRepuestos.get(i);
                Medicamento medicamento = medicamentosActuales.get(codigo);
                medicamento.reducirCantidad(cantidadesPorCodigo.get(codigo));
                try {
                    if (!repositorioMedicamentos.actualizar(medicamento)) {
                        e.addSuppressed(new IOException(
                                "No se pudo revertir el stock al cancelar la venta."));
                    }
                } catch (IOException errorAlRestaurar) {
                    e.addSuppressed(errorAlRestaurar);
                }
            }
            throw e;
        }
        registrada = false;
        cancelada = true;
        total = 0;
        detalles.clear();
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public double getTotal() {
        return total;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getUsuarioRegistrado() {
        return usuario != null ? usuario.getNombreUsuario() : usuarioRegistrado;
    }

    public String getCodigoMedicamento() {
        return detalles.isEmpty() ? "" : detalles.get(0).getMedicamento().getCodigo();
    }

    public String getNombreMedicamento() {
        return detalles.isEmpty() ? "" : detalles.get(0).getMedicamento().getNombre();
    }

    public int getCantidad() {
        return detalles.stream().mapToInt(DetalleVenta::getCantidad).sum();
    }

    public String aLinea() {
        if (detalles.isEmpty()) {
            throw new IllegalStateException("La venta no tiene detalles para guardar.");
        }
        List<String> campos = new ArrayList<>();
        campos.add("V2");
        campos.add(fecha.toString());
        campos.add(codificar(getUsuarioRegistrado()));
        campos.add(String.valueOf(detalles.size()));
        for (DetalleVenta detalle : detalles) {
            campos.add(codificarDetalle(detalle));
        }
        return String.join(";", campos);
    }

    public static Venta desdeLinea(String linea) {
        String[] datos = linea.split(";", -1);
        if (datos.length > 0 && datos[0].equals("V2")) {
            return desdeLineaV2(datos);
        }
        return desdeLineaAnterior(datos);
    }

    private static Venta desdeLineaV2(String[] datos) {
        try {
            if (datos.length < 5) {
                throw new IllegalArgumentException("El registro de venta no es válido.");
            }
            LocalDate fecha = LocalDate.parse(datos[1]);
            String usuario = decodificar(datos[2]);
            int cantidadDetalles = Integer.parseInt(datos[3]);
            if (cantidadDetalles <= 0 || datos.length != 4 + cantidadDetalles) {
                throw new IllegalArgumentException("El registro de venta no es válido.");
            }
            Venta venta = new Venta(fecha, usuario);
            for (int i = 0; i < cantidadDetalles; i++) {
                venta.detalles.add(decodificarDetalle(datos[4 + i]));
            }
            venta.total = venta.calcularTotal();
            venta.registrada = true;
            return venta;
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("El registro de venta no es válido.", e);
        }
    }

    private static Venta desdeLineaAnterior(String[] datos) {
        if (datos.length != 3 && datos.length != 4) {
            throw new IllegalArgumentException("El registro de venta no es válido.");
        }
        if (datos[0].isBlank() || datos[1].isBlank()) {
            throw new IllegalArgumentException("El registro de venta no es válido.");
        }
        try {
            int cantidad = Integer.parseInt(datos[2]);
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de venta no es válida.");
            }
            LocalDate fecha = datos.length == 4 ? LocalDate.parse(datos[3]) : null;
            Medicamento medicamento = new Medicamento(datos[1], datos[0],
                    "Sin categoría", "Sin presentación", "Sin laboratorio",
                    cantidad, "9999-12-31");
            Venta venta = new Venta(fecha, "");
            venta.detalles.add(new DetalleVenta(medicamento, cantidad));
            venta.total = venta.calcularTotal();
            venta.registrada = true;
            return venta;
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("El registro de venta no es válido.", e);
        }
    }

    private Venta(LocalDate fecha, String usuarioRegistrado) {
        this.fecha = fecha;
        this.usuarioRegistrado = usuarioRegistrado;
    }

    private static String codificarDetalle(DetalleVenta detalle) {
        Medicamento medicamento = detalle.getMedicamento();
        String datos = String.join("\u001f", medicamento.getNombre(),
                medicamento.getCodigo(), medicamento.getCategoria(),
                medicamento.getPresentacion(), medicamento.getLaboratorio(),
                String.valueOf(medicamento.getCantidadDisponible()),
                medicamento.getFechaVencimiento(), String.valueOf(medicamento.getPrecio()),
                String.valueOf(medicamento.getStockMinimo()),
                String.valueOf(detalle.getCantidad()),
                String.valueOf(detalle.getPrecioUnitario()));
        return codificar(datos);
    }

    private static DetalleVenta decodificarDetalle(String datos) {
        String[] campos = decodificar(datos).split("\u001f", -1);
        if (campos.length != 11) {
            throw new IllegalArgumentException("El detalle de venta no es válido.");
        }
        try {
            Medicamento medicamento = new Medicamento(campos[0], campos[1],
                    campos[2], campos[3], campos[4], Integer.parseInt(campos[5]),
                    campos[6], Double.parseDouble(campos[7]), Integer.parseInt(campos[8]));
            return new DetalleVenta(medicamento, Integer.parseInt(campos[9]),
                    Double.parseDouble(campos[10]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El detalle de venta no es válido.", e);
        }
    }

    private static String codificar(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodificar(String texto) {
        return new String(Base64.getUrlDecoder().decode(texto), StandardCharsets.UTF_8);
    }
}

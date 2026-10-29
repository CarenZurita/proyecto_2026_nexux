package service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import model.Administrador;
import model.DetalleVenta;
import model.Usuario;
import model.Venta;
import persistence.VentaRepository;

/** Consulta de solo lectura: usa los precios guardados al realizar cada venta. */
public class ReporteVentasService {
    private final VentaRepository ventas;

    public ReporteVentasService() { this(new VentaRepository()); }

    public ReporteVentasService(VentaRepository ventas) {
        this.ventas = Objects.requireNonNull(ventas);
    }

    public record Detalle(String codigo, String medicamento, int cantidad,
                          BigDecimal precioUnitario, BigDecimal subtotal) { }

    public record Fila(LocalDate fecha, String usuario, long unidades,
                       BigDecimal total, List<Detalle> detalles) {
        public Fila { detalles = List.copyOf(detalles); }
    }

    public record Resultado(List<Fila> filas, long unidades, BigDecimal total,
                            int ventasSinImporte, int sinFechaExcluidas) {
        public Resultado { filas = List.copyOf(filas); }
    }

    /** Fechas opcionales; ambos extremos son inclusivos. */
    public Resultado consultar(Usuario sesion, LocalDate desde, LocalDate hasta)
            throws IOException {
        if (!(sesion instanceof Administrador administrador)
                || !sesion.estaActivo() || !sesion.estaEnSesion()
                || !sesion.esAdministrador()) {
            throw new SecurityException("Se requiere una sesión de administrador.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha Desde no puede ser posterior a Hasta.");
        }

        List<Fila> filas = new ArrayList<>();
        int sinFecha = 0;
        for (Venta venta : administrador.consultarHistorialVentas(ventas)) {
            LocalDate fecha = venta.getFecha();
            if (fecha == null && (desde != null || hasta != null)) {
                sinFecha++;
                continue;
            }
            if (fecha != null && ((desde != null && fecha.isBefore(desde))
                    || (hasta != null && fecha.isAfter(hasta)))) continue;

            boolean conImporte = venta.tieneImportesRegistrados();
            List<Detalle> detalles = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            long unidades = 0;
            for (DetalleVenta detalle : venta.obtenerDetalle()) {
                BigDecimal precio = conImporte
                        ? BigDecimal.valueOf(detalle.getPrecioUnitario()) : null;
                BigDecimal subtotal = conImporte
                        ? precio.multiply(BigDecimal.valueOf(detalle.getCantidad())) : null;
                detalles.add(new Detalle(detalle.getMedicamento().getCodigo(),
                        detalle.getMedicamento().getNombre(), detalle.getCantidad(), precio, subtotal));
                unidades += detalle.getCantidad();
                if (conImporte) total = total.add(subtotal);
            }
            String usuario = venta.getUsuarioRegistrado();
            filas.add(new Fila(fecha, usuario == null || usuario.isBlank()
                    ? "Sin registro" : usuario, unidades, conImporte ? total : null, detalles));
        }
        filas.sort(Comparator.comparing(Fila::fecha,
                Comparator.nullsLast(Comparator.reverseOrder())));
        BigDecimal total = BigDecimal.ZERO;
        long unidades = 0;
        int sinImporte = 0;
        for (Fila fila : filas) {
            unidades += fila.unidades();
            if (fila.total() == null) sinImporte++;
            else total = total.add(fila.total());
        }
        return new Resultado(filas, unidades, total, sinImporte, sinFecha);
    }
}

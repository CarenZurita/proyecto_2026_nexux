package controller;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import model.Usuario;
import service.ReporteVentasService;
import view.ReporteVentasView;

/** Coordina filtros y detalles sin bloquear la interfaz ni registrar ventas. */
public class ReporteVentasController {
    private final Usuario sesion;
    private final ReporteVentasView vista;
    private final ReporteVentasService servicio;
    private boolean cargando;

    public ReporteVentasController(Usuario sesion, ReporteVentasView vista) {
        this(sesion, vista, new ReporteVentasService());
    }

    public ReporteVentasController(Usuario sesion, ReporteVentasView vista,
                                   ReporteVentasService servicio) {
        this.sesion = sesion;
        this.vista = vista;
        this.servicio = servicio;
        vista.alConsultar(this::actualizar);
        vista.alVerDetalle(fila -> {
            if (sesionValida()) vista.mostrarDetalle(fila);
            else vista.mostrarError("La sesión ha finalizado. Inicia sesión nuevamente.");
        });
    }

    public void actualizar() {
        if (cargando) return;
        if (!sesionValida()) {
            vista.mostrarError("Se requiere una sesión de administrador para consultar el reporte.");
            return;
        }
        final LocalDate desde;
        final LocalDate hasta;
        try {
            desde = vista.getDesde();
            hasta = vista.getHasta();
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                throw new IllegalArgumentException("La fecha Desde no puede ser posterior a Hasta.");
            }
        } catch (IllegalArgumentException e) {
            vista.mostrarError(e.getMessage());
            return;
        }
        cargando = true;
        vista.setCargando(true);
        new SwingWorker<ReporteVentasService.Resultado, Void>() {
            @Override protected ReporteVentasService.Resultado doInBackground() throws Exception {
                return servicio.consultar(sesion, desde, hasta);
            }

            @Override protected void done() {
                cargando = false;
                vista.setCargando(false);
                if (!sesionValida()) {
                    vista.mostrarError("La sesión ha finalizado. Inicia sesión nuevamente.");
                    return;
                }
                try {
                    vista.mostrarDatos(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    vista.mostrarError("La consulta se interrumpió. Intenta consultar nuevamente.");
                } catch (ExecutionException e) {
                    String mensaje = e.getCause().getMessage();
                    vista.mostrarError("No se pudo cargar el reporte. " + (mensaje == null
                            ? "Revisa datos/ventas.txt." : mensaje));
                }
            }
        }.execute();
    }

    private boolean sesionValida() {
        return sesion != null && sesion.estaActivo()
                && sesion.estaEnSesion() && sesion.esAdministrador();
    }
}

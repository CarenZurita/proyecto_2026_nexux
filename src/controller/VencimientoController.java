package controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import model.Usuario;
import service.VencimientoService;
import view.VencimientoView;

/** Carga el reporte sin bloquear la interfaz. */
public class VencimientoController {
    private final Usuario sesion;
    private final VencimientoView vista;
    private final VencimientoService servicio;
    private boolean cargando;

    public VencimientoController(Usuario sesion, VencimientoView vista) {
        this(sesion, vista, new VencimientoService());
    }

    public VencimientoController(Usuario sesion, VencimientoView vista,VencimientoService servicio) {
        this.sesion = sesion;
        this.vista = vista;
        this.servicio = servicio;
        vista.alActualizar(e -> actualizar());
    }

    public void actualizar() {
        if (cargando) return;
        if (!sesionValida()) {
            vista.mostrarError("Se requiere una sesión de administrador para consultar el reporte.");
            return;
        }
        cargando = true;
        vista.setCargando(true);
        new SwingWorker<List<VencimientoService.Fila>, Void>() {
            @Override
            protected List<VencimientoService.Fila> doInBackground() throws Exception {
                return servicio.consultar();
            }

            @Override
            protected void done() {
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
                    vista.mostrarError("La consulta se interrumpió. Intenta actualizar nuevamente.");
                } catch (ExecutionException e) {
                    String detalle = e.getCause().getMessage();
                    vista.mostrarError("No se pudo cargar el reporte. "
                            + (detalle == null ? "Revisa el archivo de medicamentos." : detalle));
                }
            }
        }.execute();
    }

    private boolean sesionValida() {
        return sesion != null && sesion.estaActivo()
                && sesion.estaEnSesion() && sesion.esAdministrador();
    }
}

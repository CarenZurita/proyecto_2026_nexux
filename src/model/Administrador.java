package model;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;
import service.MedicamentoService;

/**
 * Cuenta con permisos de administración; en MVC forma parte del modelo de dominio
 * y aplica las operaciones de medicamentos autorizadas al rol.
 */
public class Administrador extends Usuario {
    Administrador(String nombre, String usuario, Rol rol, boolean activo,
                  String sal, String claveProtegida) {
        super(nombre, usuario, rol, activo, sal, claveProtegida);
    }

    /**
     * Registra un medicamento mediante el servicio de dominio.
     *
     * @param medicamento medicamento que se agrega
     * @param medicamentos repositorio del inventario
     * @throws IOException si falla la persistencia
     * @throws SecurityException si la sesión o el permiso no son válidos
     */
    public void registrarMedicamento(Medicamento medicamento,
                                     MedicamentoRepository medicamentos)
            throws IOException {
        new MedicamentoService(medicamentos).registrar(this, medicamento);
    }

    /**
     * Busca un medicamento por su código dentro del inventario.
     *
     * @param codigo código que se busca
     * @param medicamentos repositorio del inventario
     * @return medicamento encontrado, o vacío si no existe
     * @throws IOException si falla la lectura
     * @throws SecurityException si no hay una sesión activa
     */
    public Optional<Medicamento> buscarMedicamentoPorCodigo(String codigo,
                                                              MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        return new MedicamentoService(medicamentos).consultarStock(this, codigo);
    }

    /**
     * Actualiza un medicamento existente por código.
     *
     * @param medicamento datos actualizados
     * @param medicamentos repositorio del inventario
     * @return {@code true} si el registro se actualizó
     * @throws IOException si falla la persistencia
     * @throws SecurityException si la sesión no tiene permiso de gestión
     */
    public boolean actualizarMedicamento(Medicamento medicamento,
                                          MedicamentoRepository medicamentos)
            throws IOException {
        return new MedicamentoService(medicamentos).actualizar(this, medicamento);
    }

    /**
     * Elimina por esta ruta solo medicamentos vencidos; los vigentes se rechazan.
     *
     * @param codigo código del medicamento que se elimina
     * @param medicamentos repositorio del inventario
     * @return {@code true} si el registro se eliminó
     * @throws IOException si falla la persistencia
     * @throws IllegalArgumentException si el código no existe
     * @throws IllegalStateException si el medicamento aún está vigente
     * @throws SecurityException si la sesión carece del permiso requerido
     */
    public boolean eliminarMedicamentoCaducado(String codigo,
                                                MedicamentoRepository medicamentos)
            throws IOException {
        validarPermiso("GESTIONAR_MEDICAMENTOS");
        MedicamentoService servicio = new MedicamentoService(medicamentos);
        Medicamento medicamento = servicio.consultarStock(this, codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un medicamento con ese código."));
        if (!medicamento.estaVencido()) {
            throw new IllegalStateException("El medicamento todavía está vigente.");
        }
        return servicio.eliminar(this, medicamento);
    }

    public List<Venta> consultarHistorialVentas(VentaRepository ventas) throws IOException {
        validarPermiso("CONSULTAR_VENTAS");
        return ventas.listar();
    }

    private void validarPermiso(String nombre) {
        validarSesionActiva();
        if (!getRol().verificarPermiso(
                new Permiso(nombre, "Permiso requerido para la operación."))) {
            throw new SecurityException("El rol no tiene el permiso " + nombre + ".");
        }
    }
}

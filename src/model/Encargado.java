package model;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;
import service.MedicamentoService;

/**
 * Cuenta operativa de la farmacia; en MVC es una entidad de dominio con
 * permisos de consulta de medicamentos, ventas y revisión de vencimientos.
 */
public class Encargado extends Usuario {
    Encargado(String nombre, String usuario, Rol rol, boolean activo,
              String sal, String claveProtegida) {
        super(nombre, usuario, rol, activo, sal, claveProtegida);
    }

    /**
     * Busca medicamentos por nombre, código, marca o precio exacto.
     *
     * @param texto criterio de búsqueda
     * @param medicamentos repositorio del inventario
     * @return coincidencias encontradas
     * @throws IOException si falla la lectura
     * @throws SecurityException si el encargado no tiene sesión activa
     */
    @Override
    public List<Medicamento> buscarMedicamento(String texto,
                                               MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        return new MedicamentoService(medicamentos).buscar(this, texto);
    }

    /**
     * Consulta el stock de un medicamento por código.
     *
     * @param codigo código que se busca
     * @param medicamentos repositorio del inventario
     * @return medicamento encontrado, o vacío si no existe
     * @throws IOException si falla la lectura
     * @throws SecurityException si el encargado no tiene sesión activa
     */
    @Override
    public Optional<Medicamento> consultarStock(String codigo,
                                                MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        return new MedicamentoService(medicamentos).consultarStock(this, codigo);
    }

    @Override
    public void registrarVenta(String codigo, int cantidad,
                               MedicamentoRepository medicamentos,
                               VentaRepository ventas) throws IOException {
        super.registrarVenta(codigo, cantidad, medicamentos, ventas);
    }

    @Override
    public List<Medicamento> revisarVencimientos(MedicamentoRepository medicamentos)
            throws IOException {
        return super.revisarVencimientos(medicamentos);
    }
}

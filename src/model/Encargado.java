package model;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

public class Encargado extends Usuario {
    Encargado(String nombre, String usuario, Rol rol, boolean activo,
              String sal, String claveProtegida) {
        super(nombre, usuario, rol, activo, sal, claveProtegida);
    }

    @Override
    public List<Medicamento> buscarMedicamento(String texto,
                                               MedicamentoRepository medicamentos)
            throws IOException {
        return super.buscarMedicamento(texto, medicamentos);
    }

    @Override
    public Optional<Medicamento> consultarStock(String codigo,
                                                MedicamentoRepository medicamentos)
            throws IOException {
        return super.consultarStock(codigo, medicamentos);
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

package model;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.security.GeneralSecurityException;
import java.util.Scanner;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

public class Administrador extends Usuario {
    Administrador(String nombre, String usuario, Rol rol, boolean activo,
                  String sal, String claveProtegida) {
        super(nombre, usuario, rol, activo, sal, claveProtegida);
    }

    public void registrarMedicamento(Medicamento medicamento,
                                     MedicamentoRepository medicamentos)
            throws IOException {
        validarPermiso("GESTIONAR_MEDICAMENTOS");
        
        medicamentos.registrar(medicamento);
    }

    public Optional<Medicamento> buscarMedicamentoPorCodigo(String codigo,
                                                              MedicamentoRepository medicamentos)
            throws IOException {
        validarSesionActiva();
        return medicamentos.buscarPorCodigo(codigo);
    }

    public boolean actualizarMedicamento(Medicamento medicamento,
                                          MedicamentoRepository medicamentos)
            throws IOException {
        validarPermiso("GESTIONAR_MEDICAMENTOS");
        return medicamentos.actualizar(medicamento);
    }

    public boolean eliminarMedicamentoCaducado(String codigo,
                                                MedicamentoRepository medicamentos)
            throws IOException {
        validarPermiso("GESTIONAR_MEDICAMENTOS");
        Medicamento medicamento = medicamentos.buscarPorCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un medicamento con ese código."));
        if (!medicamento.estaVencido()) {
            throw new IllegalStateException("El medicamento todavía está vigente.");
        }
        return medicamentos.eliminar(codigo);
    }

    public List<Venta> consultarHistorialVentas(VentaRepository ventas) throws IOException {
        validarPermiso("CONSULTAR_VENTAS");
        return ventas.listar();
    }

    public void gestionarUsuarios(Scanner teclado)
            throws IOException, GeneralSecurityException {
        validarPermiso("GESTIONAR_USUARIOS");
        GestorUsuarios.gestionarEncargados(teclado, this);
    }

    private void validarPermiso(String nombre) {
        validarSesionActiva();
        if (!getRol().verificarPermiso(
                new Permiso(nombre, "Permiso requerido para la operación."))) {
            throw new SecurityException("El rol no tiene el permiso " + nombre + ".");
        }
    }
}

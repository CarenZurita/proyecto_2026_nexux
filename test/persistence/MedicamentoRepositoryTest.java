package persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import model.Medicamento;

class MedicamentoRepositoryTest {
    @TempDir
    Path directorioTemporal;

    @Test
    void listarDevuelveListaVaciaSiElArchivoNoExiste() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();

        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void registrarPersisteElMedicamento() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();

        repositorio.registrar(crearMedicamento("MED-001", "Paracetamol", "Marca A", 2.5));

        assertEquals("MED-001", repositorio.listar().get(0).getCodigo());
    }

    @Test
    void actualizarReemplazaElMedicamentoConElMismoCodigo() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();
        repositorio.registrar(crearMedicamento("MED-001", "Paracetamol", "Marca A", 2.5));

        boolean actualizado = repositorio.actualizar(
                crearMedicamento("MED-001", "Aspirina", "Marca B", 4.0));

        assertTrue(actualizado);
        assertEquals("Aspirina", repositorio.buscarPorCodigo("MED-001")
                .orElseThrow().getNombre());
        assertEquals(4.0, repositorio.buscarPorCodigo("MED-001")
                .orElseThrow().getPrecio());
    }

    @Test
    void actualizarDevuelveFalseSiElCodigoNoExiste() throws Exception {
        assertFalse(crearRepositorio().actualizar(
                crearMedicamento("MED-404", "Aspirina", "Marca B", 4.0)));
    }

    @Test
    void eliminarPorCodigoEliminaElMedicamento() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();
        repositorio.registrar(crearMedicamento("MED-001", "Paracetamol", "Marca A", 2.5));

        assertTrue(repositorio.eliminar("MED-001"));
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void eliminarDevuelveFalseSiElCodigoNoExiste() throws Exception {
        assertFalse(crearRepositorio().eliminar("MED-404"));
    }

    @Test
    void buscarEncuentraPorNombreIgnorandoMayusculas() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();
        repositorio.registrar(crearMedicamento("MED-001", "Paracetamol", "Marca A", 2.5));

        List<Medicamento> encontrados = repositorio.buscar("PARACETAMOL");

        assertEquals(1, encontrados.size());
        assertEquals("MED-001", encontrados.get(0).getCodigo());
    }

    @Test
    void buscarEncuentraPorCodigoIgnorandoMayusculas() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();
        repositorio.registrar(crearMedicamento("MED-001", "Paracetamol", "Marca A", 2.5));

        List<Medicamento> encontrados = repositorio.buscar("med-001");

        assertEquals(1, encontrados.size());
        assertEquals("MED-001", encontrados.get(0).getCodigo());
    }

    @Test
    void buscarDevuelveListaVaciaSiNoHayCoincidencias() throws Exception {
        MedicamentoRepository repositorio = crearRepositorio();
        repositorio.registrar(crearMedicamento("MED-001", "Paracetamol", "Marca A", 2.5));

        assertTrue(repositorio.buscar("inexistente").isEmpty());
    }

    @Test
    void buscarRechazaUnTerminoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> crearRepositorio().buscar(""));
    }

    private MedicamentoRepository crearRepositorio() {
        return new MedicamentoRepository(directorioTemporal.resolve("medicamentos.txt"));
    }

    private Medicamento crearMedicamento(
            String codigo, String nombre, String marca, double precio) {
        return new Medicamento(nombre, codigo, "Analgésico", "Tabletas",
                "Laboratorio A", marca, 10, "2030-01-15", precio, 2);
    }
}
package persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import model.Medicamento;

class MedicamentoRepositorySearchTest {
    @TempDir
    Path directorioTemporal;

    @Test
    void buscaPorMarcaIgnorandoMayusculas() throws Exception {
        MedicamentoRepository repositorio =
                new MedicamentoRepository(directorioTemporal.resolve("medicamentos.txt"));
        repositorio.registrar(crearMedicamento("MED-001", "Marca Farma", 7.5));
        repositorio.registrar(crearMedicamento("MED-002", "Otra Marca", 12.0));

        assertEquals(1, repositorio.buscar("FARMA").size());
        assertEquals("MED-001", repositorio.buscar("FARMA").get(0).getCodigo());
    }

    @Test
    void buscaPorPrecioExactoYConComaDecimal() throws Exception {
        MedicamentoRepository repositorio =
                new MedicamentoRepository(directorioTemporal.resolve("medicamentos.txt"));
        repositorio.registrar(crearMedicamento("MED-001", "Marca A", 7.5));
        repositorio.registrar(crearMedicamento("MED-002", "Marca B", 7.0));

        assertEquals(1, repositorio.buscar("7,50").size());
        assertEquals("MED-001", repositorio.buscar("7.5").get(0).getCodigo());
    }

    private Medicamento crearMedicamento(String codigo, String marca, double precio) {
        return new Medicamento("Paracetamol", codigo, "Analgésico", "Tabletas",
                "Laboratorio A", marca, 10, "2030-01-15", precio, 2);
    }
}

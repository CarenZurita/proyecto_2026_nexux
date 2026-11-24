package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.MedicamentoRepository;

class EncargadoTest {
    @TempDir
    Path directorioTemporal;

    private Encargado encargado;
    private MedicamentoRepository medicamentos;

    @BeforeEach
    @SuppressWarnings("unused")
    void prepararEncargado() throws GeneralSecurityException, IOException {
        medicamentos = new MedicamentoRepository(
                directorioTemporal.resolve("medicamentos.txt"));
        encargado = (Encargado) Usuario.crear("encargado", "Encargado",
                Rol.ENCARGADO, "ClaveSegura123".toCharArray());
        encargado.iniciarSesion("ClaveSegura123".toCharArray());
        medicamentos.registrar(crearMedicamento("MED-001", 8,
                LocalDate.now().plusDays(10).toString()));
        medicamentos.registrar(crearMedicamento("MED-002", 5,
                LocalDate.now().plusDays(90).toString()));
        medicamentos.registrar(crearMedicamento("MED-003", 2, "2020-01-01"));
    }

    @Test
    void buscaMedicamentosPorNombre() throws IOException {
        List<Medicamento> porNombre =
                encargado.buscarMedicamento("aspir", medicamentos);

        assertEquals(3, porNombre.size());
    }

    @Test
    void buscaMedicamentosPorCodigo() throws IOException {
        List<Medicamento> porCodigo =
                encargado.buscarMedicamento("med-002", medicamentos);

        assertEquals("MED-002", porCodigo.get(0).getCodigo());
    }

    @Test
    void buscaMedicamentosPorMarca() throws IOException {
        medicamentos.registrar(crearMedicamento(
                "MED-004", "aspirina", "2030-01-01", "Marca Farma", 7.5));

        List<Medicamento> encontrados = encargado.buscarMedicamento("FARMA", medicamentos);

        assertEquals(1, encontrados.size());
        assertEquals("MED-004", encontrados.get(0).getCodigo());
    }

    @Test
    void buscaMedicamentosPorPrecioExactoConPuntoYComa() throws IOException {
        medicamentos.registrar(crearMedicamento(
                "MED-004", "aspirina", "2030-01-01", "Marca Farma", 7.5));

        assertEquals("MED-004",
                encargado.buscarMedicamento("7.5", medicamentos).get(0).getCodigo());
        assertEquals("MED-004",
                encargado.buscarMedicamento("7,50", medicamentos).get(0).getCodigo());
    }

    @Test
    void consultaStockPorCodigoExistente() throws IOException {
        assertEquals(8, encargado.consultarStock("MED-001", medicamentos)
                .orElseThrow().getCantidadDisponible());
    }

    @Test
    void consultaStockDeCodigoInexistente() throws IOException {
        assertFalse(encargado.consultarStock("no-existe", medicamentos).isPresent());
    }

    @Test
    void revisaMedicamentosVencidosYProximosAVencer() throws IOException {
        List<Medicamento> paraRevisar = encargado.revisarVencimientos(medicamentos);

        assertEquals(2, paraRevisar.size());
        assertTrue(paraRevisar.stream().anyMatch(m -> m.getCodigo().equals("MED-001")));
        assertTrue(paraRevisar.stream().anyMatch(m -> m.getCodigo().equals("MED-003")));
        assertFalse(paraRevisar.stream().anyMatch(m -> m.getCodigo().equals("MED-002")));
    }

    @Test
    void requiereUnaSesionActivaParaConsultar() {
        encargado.cerrarSesion();

        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> encargado.consultarStock("MED-001", medicamentos));
        assertTrue(excepcion.getMessage().contains("Debe iniciar sesión"));
    }

    private static Medicamento crearMedicamento(
            String codigo, int cantidad, String vencimiento) {
        return new Medicamento("Aspirina", codigo, "Analgésico",
                "Tabletas", "Laboratorio", cantidad, vencimiento);
    }

    private static Medicamento crearMedicamento(
            String codigo, String nombre, String vencimiento, String marca, double precio) {
        return new Medicamento(nombre, codigo, "Analgésico", "Tabletas",
                "Laboratorio", marca, 10, vencimiento, precio, 2);
    }
}

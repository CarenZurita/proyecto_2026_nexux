package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.MedicamentoRepository;
import persistence.VentaRepository;

class AdministradorTest {
    @TempDir
    Path directorioTemporal;

    private Administrador administrador;
    private MedicamentoRepository medicamentos;
    private VentaRepository ventas;

    @BeforeEach
    @SuppressWarnings("unused")
    void prepararAdministrador() throws GeneralSecurityException {
        medicamentos = new MedicamentoRepository(
                directorioTemporal.resolve("medicamentos.txt"));
        ventas = new VentaRepository(directorioTemporal.resolve("ventas.txt"));
        administrador = (Administrador) Usuario.crear("admin", "Administradora",
                Rol.ADMINISTRADOR, "ClaveSegura123".toCharArray());
        administrador.iniciarSesion("ClaveSegura123".toCharArray());
    }

    @Test
    void registraActualizaYEliminaMedicamentosCaducados() throws IOException {
        Medicamento medicamento = crearMedicamento("MED-001", "2020-01-01");
        administrador.registrarMedicamento(medicamento, medicamentos);
        assertTrue(administrador.buscarMedicamentoPorCodigo(
                "MED-001", medicamentos).isPresent());

        medicamento.actualizarInfo("Nombre actualizado", "Categoría",
                "Tabletas", "Laboratorio", 5, "2030-01-01");
        assertTrue(administrador.actualizarMedicamento(medicamento, medicamentos));
        assertEquals("Nombre actualizado",
                administrador.buscarMedicamentoPorCodigo("MED-001", medicamentos)
                        .orElseThrow().getNombre());

        medicamento.actualizarInfo("Aspirina", "Analgésico", "Tabletas",
                "Laboratorio", 5, "2020-01-01");
        assertTrue(administrador.actualizarMedicamento(medicamento, medicamentos));
        assertTrue(administrador.eliminarMedicamentoCaducado("MED-001", medicamentos));
        assertFalse(administrador.buscarMedicamentoPorCodigo(
                "MED-001", medicamentos).isPresent());
    }

    @Test
    void consultaHistorialDeVentas() throws IOException {
        ventas.registrar(new Venta("MED-002", 2));

        assertEquals(1, administrador.consultarHistorialVentas(ventas).size());
    }

    @Test
    void rechazaOperacionesSinSesion() throws GeneralSecurityException {
        administrador.cerrarSesion();

        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> administrador.registrarMedicamento(
                        crearMedicamento("MED-003", "2030-01-01"), medicamentos));
        assertTrue(excepcion.getMessage().contains("iniciar sesión"));
    }

    private static Medicamento crearMedicamento(String codigo, String vencimiento) {
        return new Medicamento("Aspirina", codigo, "Analgésico",
                "Tabletas", "Laboratorio", 10, vencimiento);
    }
}

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
    void registraMedicamento() throws IOException {
        Medicamento medicamento = crearMedicamento("MED-001", "2030-01-01");

        administrador.registrarMedicamento(medicamento, medicamentos);

        assertTrue(administrador.buscarMedicamentoPorCodigo(
                "MED-001", medicamentos).isPresent());
    }

    @Test
    void actualizaMedicamento() throws IOException {
        Medicamento medicamento = crearMedicamento("MED-001", "2030-01-01");
        administrador.registrarMedicamento(medicamento, medicamentos);

        medicamento.actualizarInfo("Nombre actualizado", "Categoría",
                "Tabletas", "Laboratorio", 5, "2031-01-01");

        assertTrue(administrador.actualizarMedicamento(medicamento, medicamentos));
        assertEquals("Nombre actualizado",
                administrador.buscarMedicamentoPorCodigo("MED-001", medicamentos)
                        .orElseThrow().getNombre());
    }

    @Test
    void eliminaMedicamentoCaducado() throws IOException {
        medicamentos.registrar(crearMedicamento("MED-001", "2020-01-01"));

        assertTrue(administrador.eliminarMedicamentoCaducado("MED-001", medicamentos));
        assertFalse(administrador.buscarMedicamentoPorCodigo(
                "MED-001", medicamentos).isPresent());
    }

    @Test
    void rechazaEliminarMedicamentoVigente() throws IOException {
        medicamentos.registrar(crearMedicamento("MED-001", "2999-12-31"));

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> administrador.eliminarMedicamentoCaducado("MED-001", medicamentos));

        assertEquals("El medicamento todavía está vigente.", excepcion.getMessage());
        assertTrue(administrador.buscarMedicamentoPorCodigo(
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

    @Test
    void rechazaRegistrarAntesDeIniciarSesion() throws GeneralSecurityException {
        Administrador sinSesion = (Administrador) Usuario.crear(
                "admin2", "Administradora", Rol.ADMINISTRADOR,
                "ClaveSegura123".toCharArray());

        assertThrows(SecurityException.class,
                () -> sinSesion.registrarMedicamento(
                        crearMedicamento("MED-004", "2030-01-01"), medicamentos));
    }

    @Test
    void rolEncargadoNoTienePermisoParaGestionarMedicamentos() {
        assertFalse(Rol.ENCARGADO.verificarPermiso(
                new Permiso("GESTIONAR_MEDICAMENTOS", "Permiso requerido.")));
    }

    private static Medicamento crearMedicamento(String codigo, String vencimiento) {
        return new Medicamento("Aspirina", codigo, "Analgésico",
                "Tabletas", "Laboratorio", 10, vencimiento);
    }
}

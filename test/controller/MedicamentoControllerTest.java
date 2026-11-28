package controller;

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
import model.Medicamento;
import model.Rol;
import model.Usuario;
import persistence.MedicamentoRepository;
import service.DatosMedicamento;
import service.MedicamentoService;

class MedicamentoControllerTest {
    @TempDir
    Path directorioTemporal;

    private MedicamentoRepository repositorio;
    private MedicamentoController controlador;
    private Usuario administrador;
    private Usuario encargado;

    @BeforeEach
    void preparar() throws GeneralSecurityException {
        repositorio = new MedicamentoRepository(
                directorioTemporal.resolve("medicamentos.txt"));
        controlador = new MedicamentoController(new MedicamentoService(repositorio));
        administrador = crearSesion("admin", Rol.ADMINISTRADOR);
        encargado = crearSesion("encargado", Rol.ENCARGADO);
    }

    @Test
    void listaYBuscaLosMedicamentosDelServicio() throws IOException {
        repositorio.registrar(crearMedicamento());

        assertEquals(1, controlador.listar(administrador).size());
        assertEquals("MED-001",
                controlador.buscar(encargado, "aspirina").get(0).getCodigo());
    }

    @Test
    void registraActualizaYEliminaUsandoElServicio() throws IOException {
        controlador.registrar(administrador, datos("MED-001", "Aspirina", "2.50"));

        DatosMedicamento actualizado =
                datos("MED-001", "Aspirina Forte", "3.75");
        controlador.actualizar(administrador, "MED-001", actualizado);
        Medicamento medicamento = controlador.listar(administrador).get(0);

        assertEquals("Aspirina forte", medicamento.getNombre());
        assertEquals(3.75, medicamento.getPrecio());

        controlador.eliminar(administrador, "MED-001");

        assertTrue(controlador.listar(administrador).isEmpty());
    }

    @Test
    void propagaLosErroresDeAutorizacionDelServicio() {
        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> controlador.registrar(encargado,
                        datos("MED-001", "Aspirina", "2.50")));

        assertEquals("Error al guardar: Se requiere una sesión activa de administrador.",
                excepcion.getMessage());
    }

    @Test
    void puedeEliminarUnMedicamentoVigente() throws IOException {
        repositorio.registrar(crearMedicamento());

        controlador.eliminar(administrador, "MED-001");

        assertFalse(controlador.consultarStock(administrador, "MED-001").isPresent());
    }

    private static Usuario crearSesion(String nombre, Rol rol)
            throws GeneralSecurityException {
        Usuario usuario = Usuario.crear(nombre, nombre, rol,
                "ClaveSegura123".toCharArray());
        usuario.iniciarSesion("ClaveSegura123".toCharArray());
        return usuario;
    }

    private static DatosMedicamento datos(String codigo, String nombre, String precio) {
        return new DatosMedicamento(nombre, codigo, "Analgésico", "Tabletas",
                "Laboratorio", precio, "5", "12/2030");
    }

    private static Medicamento crearMedicamento() {
        return new Medicamento("Aspirina", "MED-001", "Analgésico",
                "Tabletas", "Laboratorio", "Marca Farma", 4,
                "2999-12-31", 7.5, 1);
    }
}

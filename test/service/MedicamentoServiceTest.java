package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import model.Medicamento;
import model.Rol;
import model.Usuario;
import persistence.MedicamentoRepository;

class MedicamentoServiceTest {
    @TempDir
    Path directorioTemporal;

    private MedicamentoRepository repositorio;
    private MedicamentoService servicio;
    private Usuario administrador;
    private Usuario encargado;

    @BeforeEach
    void preparar() throws GeneralSecurityException {
        Path archivoMedicamentos = directorioTemporal.resolve("medicamentos.txt");
        repositorio = new MedicamentoRepository(archivoMedicamentos);
        servicio = new MedicamentoService(archivoMedicamentos);
        administrador = crearSesion("admin", Rol.ADMINISTRADOR);
        encargado = crearSesion("encargado", Rol.ENCARGADO);
    }

    @Test
    void listarDevuelveInventarioYPermiteConsultaAlEncargado() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Farma", 7.5, 4));

        assertEquals("MED-001", servicio.listar(encargado).get(0).getCodigo());
    }

    @Test
    void listarRechazaUnaSesionNula() {
        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> servicio.listar(null));

        assertEquals("Se requiere una sesión válida para consultar medicamentos.",
                excepcion.getMessage());
    }

    @Test
    void buscarAplicaNombreCodigoMarcaYPrecioExactoConPuntoYComa() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Farma", 7.5, 4));

        assertEquals(1, servicio.buscar(administrador, "aspir").size());
        assertEquals("MED-001",
                servicio.buscar(administrador, "med-001").get(0).getCodigo());
        assertEquals(1, servicio.buscar(administrador, "FARMA").size());
        assertEquals(1, servicio.buscar(administrador, "7.5").size());
        assertEquals(1, servicio.buscar(administrador, "7,50").size());
    }

    @Test
    void buscarRechazaUnTerminoVacioConElMensajeDelRepositorio() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.buscar(administrador, "  "));

        assertEquals("Debe ingresar un texto para buscar.", excepcion.getMessage());
    }

    @Test
    void buscarSinCoincidenciasDevuelveListaVacia() throws IOException {
        assertTrue(servicio.buscar(administrador, "no-existe").isEmpty());
    }

    @Test
    void consultarStockDevuelveMedicamentoOVacioSegunElCodigo() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Farma", 7.5, 4));

        assertEquals(4, servicio.consultarStock(administrador, "MED-001")
                .orElseThrow().getCantidadDisponible());
        assertTrue(servicio.consultarStock(administrador, "MED-999").isEmpty());
    }

    @Test
    void registrarAceptaComaDecimalYCantidadCero() throws IOException {
        servicio.registrar(administrador, datos(
                "MED-001", "1,25", "0", "12/2030"));

        Medicamento guardado = servicio.listar(administrador).get(0);
        assertEquals(1.25, guardado.getPrecio());
        assertEquals(0, guardado.getCantidadDisponible());
        assertEquals("2030-12-31", guardado.getFechaVencimiento());
        assertEquals("Sin marca", guardado.getMarca());
        assertEquals(0, guardado.getStockMinimo());
    }

    @Test
    void registrarRechazaCamposObligatoriosVacios() {
        DatosMedicamento incompleto = new DatosMedicamento(
                " ", "MED-001", "Analgésico", "Tabletas",
                "Laboratorio", "1.25", "1", "12/2030");

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador, incompleto));

        assertEquals("Completa todos los campos obligatorios.", excepcion.getMessage());
    }

    @Test
    void registrarRechazaCodigoDuplicadoSinDistinguirMayusculas() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Farma", 7.5, 4));

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("med-001", "1.25", "1", "12/2030")));

        assertEquals("El código med-001 ya se encuentra registrado.",
                excepcion.getMessage());
    }

    @Test
    void registrarRechazaPrecioNoNumericoConElMensajeActual() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-001", "precio", "1", "12/2030")));

        assertEquals("Precio o cantidad no válidos. Usa números positivos.",
                excepcion.getMessage());
    }

    @Test
    void registrarRechazaPrecioYCantidadVaciosComoDatosNoNumericos() {
        IllegalArgumentException precio = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-001", "", "1", "12/2030")));
        IllegalArgumentException cantidad = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-002", "1", "", "12/2030")));

        assertEquals("Precio o cantidad no válidos. Usa números positivos.",
                precio.getMessage());
        assertEquals("Precio o cantidad no válidos. Usa números positivos.",
                cantidad.getMessage());
    }

    @Test
    void registrarRechazaPrecioOCantidadNegativos() {
        IllegalArgumentException precio = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-001", "-1", "1", "12/2030")));
        IllegalArgumentException cantidad = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-002", "1", "-1", "12/2030")));

        assertEquals("Precio o cantidad no válidos. Usa números positivos.",
                precio.getMessage());
        assertEquals("Precio o cantidad no válidos. Usa números positivos.",
                cantidad.getMessage());
    }

    @Test
    void registrarRechazaFormatoDeFechaInvalido() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-001", "1.25", "1", "13/2030")));

        assertEquals("La fecha debe tener formato MM/AAAA, por ejemplo 12/2027.",
                excepcion.getMessage());
    }

    @Test
    void registrarRechazaPrecioNoFinitoConElMensajeDelModelo() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(administrador,
                        datos("MED-001", "NaN", "1", "12/2030")));

        assertEquals("Error al guardar: El precio no es válido.",
                excepcion.getMessage());
    }

    @Test
    void registrarRechazaUnaSesionNulaConElMensajeDeLaVista() {
        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> servicio.registrar(null,
                        datos("MED-001", "1.25", "1", "12/2030")));

        assertEquals("Error al guardar: Se requiere una sesión activa de administrador.",
                excepcion.getMessage());
    }

    @Test
    void registrarRechazaAlEncargado() {
        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> servicio.registrar(encargado,
                        datos("MED-001", "1.25", "1", "12/2030")));

        assertEquals("Error al guardar: Se requiere una sesión activa de administrador.",
                excepcion.getMessage());
    }

    @Test
    void actualizarConservaMarcaYStockMinimo() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Original", 7.5, 4));

        servicio.actualizar(administrador, "MED-001",
                datos("MED-001", "2,50", "6", "11/2031"));

        Medicamento actualizado = servicio.listar(administrador).get(0);
        assertEquals("Aspirina Nueva", actualizado.getNombre());
        assertEquals("Marca Original", actualizado.getMarca());
        assertEquals(4, actualizado.getStockMinimo());
        assertEquals(2.5, actualizado.getPrecio());
        assertEquals(6, actualizado.getCantidadDisponible());
    }

    @Test
    void actualizarRechazaUnCodigoQueYaPerteneceAOtroMedicamento() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca 1", 1.0, 0));
        repositorio.registrar(medicamento(
                "MED-002", "Ibuprofeno", "Marca 2", 2.0, 0));

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(administrador, "MED-001",
                        datos("MED-002", "2.50", "1", "12/2030")));

        assertEquals("El código MED-002 ya se encuentra registrado.",
                excepcion.getMessage());
    }

    @Test
    void actualizarConCodigoNuevoConservaElErrorActualCuandoNoEncuentraRegistro()
            throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Original", 7.5, 4));

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> servicio.actualizar(administrador, "MED-001",
                        datos("MED-NEW", "2.50", "1", "12/2030")));

        assertEquals("Error al guardar: El medicamento ya no existe en el inventario.",
                excepcion.getMessage());
    }

    @Test
    void eliminarPermiteUnMedicamentoVigente() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Farma", 7.5, 4));

        servicio.eliminar(administrador, "MED-001");

        assertFalse(servicio.consultarStock(administrador, "MED-001").isPresent());
    }

    @Test
    void eliminarRechazaAlEncargadoConElMensajeDeLaVista() throws IOException {
        repositorio.registrar(medicamento(
                "MED-001", "Aspirina", "Marca Farma", 7.5, 4));

        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> servicio.eliminar(encargado, "MED-001"));

        assertEquals("No se pudo eliminar el medicamento: "
                        + "Se requiere una sesión activa de administrador.",
                excepcion.getMessage());
    }

    private static Usuario crearSesion(String nombre, Rol rol)
            throws GeneralSecurityException {
        Usuario usuario = Usuario.crear(nombre, nombre, rol,
                "ClaveSegura123".toCharArray());
        usuario.iniciarSesion("ClaveSegura123".toCharArray());
        return usuario;
    }

    private static DatosMedicamento datos(
            String codigo, String precio, String cantidad, String vencimiento) {
        return new DatosMedicamento("Aspirina Nueva", codigo, "Analgésico",
                "Tabletas", "Laboratorio", precio, cantidad, vencimiento);
    }

    private static Medicamento medicamento(
            String codigo, String nombre, String marca, double precio, int stockMinimo) {
        return new Medicamento(nombre, codigo, "Analgésico", "Tabletas",
                "Laboratorio", marca, 4, "2999-12-31", precio, stockMinimo);
    }
}
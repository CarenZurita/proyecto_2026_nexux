package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

class VentaTest {
    @TempDir
    Path directorioTemporal;

    private Encargado encargado;
    private MedicamentoRepository medicamentos;
    private VentaRepository ventas;

    @BeforeEach
    @SuppressWarnings("unused")
    void prepararVenta() throws GeneralSecurityException, IOException {
        medicamentos = new MedicamentoRepository(
                directorioTemporal.resolve("medicamentos.txt"));
        ventas = new VentaRepository(directorioTemporal.resolve("ventas.txt"));
        encargado = (Encargado) Usuario.crear("encargado", "Encargado",
                Rol.ENCARGADO, "ClaveSegura123".toCharArray());
        encargado.iniciarSesion("ClaveSegura123".toCharArray());
        medicamentos.registrar(crearMedicamento("MED-001", 12, "2030-01-01"));
    }

    @Test
    void registrarGuardaVentaYActualizaElStock() throws IOException {
        encargado.registrarVenta("MED-001", 3, medicamentos, ventas);

        assertEquals(9, medicamentos.buscarPorCodigo("MED-001")
                .orElseThrow().getCantidadDisponible());
        Venta venta = ventas.listar().get(0);
        assertEquals("MED-001", venta.getCodigoMedicamento());
        assertEquals("Aspirina", venta.getNombreMedicamento());
        assertEquals(3, venta.getCantidad());
        assertEquals(1, ventas.listar().size());
    }

    @Test
    void conectaUsuarioMedicamentoYDetallesAlRegistrarLaVenta() throws IOException {
        Medicamento medicamento = medicamentos.buscarPorCodigo("MED-001").orElseThrow();
        Venta venta = new Venta(encargado);
        venta.agregarDetalle(new DetalleVenta(medicamento, 2, 2.5));
        venta.agregarDetalle(new DetalleVenta(medicamento, 1, 3.0));

        venta.registrar(encargado, medicamentos, ventas);

        assertSame(encargado, venta.getUsuario());
        assertEquals("encargado", venta.getUsuarioRegistrado());
        assertEquals(8.0, venta.getTotal());
        assertEquals(2, venta.obtenerDetalle().size());
        assertSame(venta.obtenerDetalle().get(0).getMedicamento(),
                venta.obtenerDetalle().get(1).getMedicamento());
        assertEquals(9, medicamentos.buscarPorCodigo("MED-001")
                .orElseThrow().getCantidadDisponible());

        Venta restaurada = ventas.listar().get(0);
        assertEquals("encargado", restaurada.getUsuarioRegistrado());
        assertEquals(8.0, restaurada.getTotal());
        assertEquals(2, restaurada.obtenerDetalle().size());
        assertEquals("MED-001",
                restaurada.obtenerDetalle().get(0).getMedicamento().getCodigo());
    }

    @Test
    void calculaTotalYNoPermiteModificarLaListaDeDetalles() {
        Venta venta = new Venta(encargado);
        venta.agregarDetalle(new DetalleVenta(
                crearMedicamentoConPrecio("MED-010", 10.0), 2));
        venta.agregarDetalle(new DetalleVenta(
                crearMedicamentoConPrecio("MED-011", 3.5), 4));

        assertEquals(34.0, venta.calcularTotal());
        assertEquals(6, venta.getCantidad());
        UnsupportedOperationException excepcion = assertThrows(
                UnsupportedOperationException.class, () -> venta.obtenerDetalle().clear());
        assertNotNull(excepcion);
    }

    @Test
    void registraDetallesDeMedicamentosDiferentesConPreciosCorrectos() throws IOException {
        Medicamento analgesico = crearMedicamentoConPrecio("MED-010", 2.5);
        Medicamento antibiotico = crearMedicamentoConPrecio("MED-011", 8.0);
        medicamentos.registrar(analgesico);
        medicamentos.registrar(antibiotico);
        Venta venta = new Venta(encargado);
        venta.agregarDetalle(new DetalleVenta(analgesico, 2));
        venta.agregarDetalle(new DetalleVenta(antibiotico, 1));

        venta.registrar(encargado, medicamentos, ventas);

        assertEquals(13.0, venta.getTotal());
        assertEquals(8, medicamentos.buscarPorCodigo("MED-010")
                .orElseThrow().getCantidadDisponible());
        assertEquals(9, medicamentos.buscarPorCodigo("MED-011")
                .orElseThrow().getCantidadDisponible());
        assertEquals(13.0, ventas.listar().get(0).getTotal());
    }

    @Test
    void cancelarVentaEliminaElRegistroYDevuelveElStock() throws IOException {
        encargado.registrarVenta("MED-001", 3, medicamentos, ventas);
        Venta venta = ventas.listar().get(0);

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class, venta::cancelar);
        assertTrue(excepcion.getMessage().contains("registrada"));

        Venta ventaActiva = new Venta(encargado);
        ventaActiva.agregarDetalle(new DetalleVenta(
                medicamentos.buscarPorCodigo("MED-001").orElseThrow(), 2));
        ventaActiva.registrar(encargado, medicamentos, ventas);
        ventaActiva.cancelar();

        assertEquals(9, medicamentos.buscarPorCodigo("MED-001")
                .orElseThrow().getCantidadDisponible());
        assertEquals(1, ventas.listar().size());
        assertEquals(0.0, ventaActiva.getTotal());
        assertTrue(ventaActiva.obtenerDetalle().isEmpty());
        IllegalStateException cancelarOtraVez = assertThrows(
                IllegalStateException.class, ventaActiva::cancelar);
        assertNotNull(cancelarOtraVez);
        IllegalStateException agregarTrasCancelar = assertThrows(
                IllegalStateException.class, () -> ventaActiva.agregarDetalle(
                        new DetalleVenta(crearMedicamentoConPrecio("MED-010", 1), 1)));
        assertNotNull(agregarTrasCancelar);
    }

    @Test
    void rechazaCantidadMayorQueElStock() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> encargado.registrarVenta("MED-001", 13, medicamentos, ventas));

        assertEquals("Stock insuficiente.", excepcion.getMessage());
    }

    @Test
    void rechazaMedicamentosVencidos() throws IOException {
        medicamentos.registrar(crearMedicamento("MED-002", 3, "2020-01-01"));

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> encargado.registrarVenta("MED-002", 1, medicamentos, ventas));

        assertTrue(excepcion.getMessage().contains("vencido"));
    }

    @Test
    void rechazaMedicamentoInexistenteSinCambiarInventario() throws IOException {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> encargado.registrarVenta("MED-404", 1, medicamentos, ventas));

        assertEquals("Medicamento no encontrado.", excepcion.getMessage());
        assertEquals(12, medicamentos.buscarPorCodigo("MED-001")
                .orElseThrow().getCantidadDisponible());
        assertTrue(ventas.listar().isEmpty());
    }

    @Test
    void rechazaRegistroSiElEncargadoNoTieneSesion() throws IOException {
        encargado.cerrarSesion();

        SecurityException excepcion = assertThrows(SecurityException.class,
                () -> encargado.registrarVenta("MED-001", 1, medicamentos, ventas));
        assertNotNull(excepcion);

        assertEquals(12, medicamentos.buscarPorCodigo("MED-001")
                .orElseThrow().getCantidadDisponible());
        assertTrue(ventas.listar().isEmpty());
    }

    @Test
    void constructorRechazaCodigoVacioYCantidadNoPositiva() {
        IllegalArgumentException codigoVacio = assertThrows(
                IllegalArgumentException.class, () -> new Venta(" ", 1));
        IllegalArgumentException cantidadCero = assertThrows(
                IllegalArgumentException.class, () -> new Venta("MED-001", 0));
        IllegalArgumentException cantidadNegativa = assertThrows(
                IllegalArgumentException.class, () -> new Venta("MED-001", -1));
        assertNotNull(codigoVacio);
        assertNotNull(cantidadCero);
        assertNotNull(cantidadNegativa);
    }

    @Test
    void agregarDetalleRechazaNulos() {
        Venta venta = new Venta(encargado);

        NullPointerException excepcion = assertThrows(
                NullPointerException.class, () -> venta.agregarDetalle(null));
        assertNotNull(excepcion);
    }

    @Test
    void restauraElStockSiFallaElGuardadoDeLaVenta() throws IOException {
        VentaRepository ventasConRutaQueEsDirectorio =
                new VentaRepository(directorioTemporal);

        IOException excepcion = assertThrows(IOException.class,
                () -> encargado.registrarVenta(
                        "MED-001", 2, medicamentos, ventasConRutaQueEsDirectorio));
        assertTrue(excepcion.getMessage() != null);

        assertEquals(12, medicamentos.buscarPorCodigo("MED-001")
                .orElseThrow().getCantidadDisponible());
    }

    @Test
    void desdeLineaReconstruyeUnaVenta() {
        Venta venta = Venta.desdeLinea("MED-003;Ibuprofeno;4;2026-09-10");

        assertEquals("MED-003", venta.getCodigoMedicamento());
        assertEquals("Ibuprofeno", venta.getNombreMedicamento());
        assertEquals(4, venta.getCantidad());
        assertEquals("2026-09-10", venta.getFecha().toString());
    }

    @Test
    void desdeLineaAntiguaSinFechaConservaLosDatosBasicos() {
        Venta venta = Venta.desdeLinea("MED-003;Ibuprofeno;4");

        assertEquals("MED-003", venta.getCodigoMedicamento());
        assertEquals("Ibuprofeno", venta.getNombreMedicamento());
        assertEquals(4, venta.getCantidad());
        assertNull(venta.getFecha());
    }

    @Test
    void desdeLineaRechazaRegistrosInvalidos() {
        IllegalArgumentException vacia = assertThrows(
                IllegalArgumentException.class, () -> Venta.desdeLinea(""));
        IllegalArgumentException cantidadNoNumerica = assertThrows(
                IllegalArgumentException.class,
                () -> Venta.desdeLinea("MED-003;Ibuprofeno;cero;2026-09-10"));
        IllegalArgumentException cantidadNoPositiva = assertThrows(
                IllegalArgumentException.class,
                () -> Venta.desdeLinea("MED-003;Ibuprofeno;0;2026-09-10"));
        IllegalArgumentException lineaV2MalFormada = assertThrows(
                IllegalArgumentException.class,
                () -> Venta.desdeLinea("V2;fecha;usuario;1;detalle"));
        assertNotNull(vacia);
        assertNotNull(cantidadNoNumerica);
        assertNotNull(cantidadNoPositiva);
        assertNotNull(lineaV2MalFormada);
    }

    @Test
    void lineaV2PersisteYReconstruyeLosDetallesYElUsuario() {
        Venta venta = new Venta(encargado);
        Medicamento medicamento = crearMedicamentoConPrecio("MED-010", 2.75);
        venta.agregarDetalle(new DetalleVenta(medicamento, 3));

        Venta restaurada = Venta.desdeLinea(venta.aLinea());

        assertEquals("encargado", restaurada.getUsuarioRegistrado());
        assertEquals(8.25, restaurada.getTotal());
        assertEquals(1, restaurada.obtenerDetalle().size());
        assertEquals("MED-010",
                restaurada.obtenerDetalle().get(0).getMedicamento().getCodigo());
        assertEquals(2.75, restaurada.obtenerDetalle().get(0).getPrecioUnitario());
    }

    @Test
    void aLineaRechazaVentasSinDetalles() {
        Venta venta = new Venta(encargado);

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class, venta::aLinea);
        assertNotNull(excepcion);
    }

    private static Medicamento crearMedicamento(
            String codigo, int cantidad, String vencimiento) {
        return new Medicamento("Aspirina", codigo, "Analgésico",
                "Tabletas", "Laboratorio", cantidad, vencimiento);
    }

    private static Medicamento crearMedicamentoConPrecio(String codigo, double precio) {
        return new Medicamento("Aspirina", codigo, "Analgésico",
                "Tabletas", "Laboratorio", 10, "2030-01-01", precio, 2);
    }
}

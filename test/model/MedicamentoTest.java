package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class MedicamentoTest {

	/**
	 * Verifica que el constructor cree un medicamento y conserve su cantidad inicial.
	 */
	@Test
	void constructorInicializaElMedicamento() {
		Medicamento medicamento = new Medicamento(
				"Paracetamol", "MED-001", "Analgesico",
				"Tabletas", "Laboratorio A", 20, "2030-01-15");

		assertNotNull(medicamento);
		assertEquals(20, medicamento.getCantidadDisponible());
	}

	/**
	 * Verifica que mostrarDatos imprima todos los atributos con sus etiquetas.
	 */
	@Test
	void mostrarDatosImprimeTodosLosAtributos() {
		Medicamento medicamento = new Medicamento(
				"Paracetamol", "MED-001", "Analgesico",
				"Tabletas", "Laboratorio A", 20, "2030-01-15");
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		PrintStream salidaOriginal = System.out;

		try (PrintStream capturador = new PrintStream(salida, true, StandardCharsets.UTF_8)) {
			System.setOut(capturador);
			medicamento.mostrarDatos();
		} finally {
			System.setOut(salidaOriginal);
		}

		String separador = System.lineSeparator();
		String esperado = String.join(separador,
				"Nombre: Paracetamol",
				"Código: MED-001",
				"Categoría: Analgesico",
				"Presentación: Tabletas",
				"Laboratorio: Laboratorio A",
				"Marca: Sin marca",
				"Cantidad disponible: 20",
				"Precio: 0.0",
				"Stock mínimo: 0",
				"Fecha de vencimiento: 2030-01-15") + separador;

		assertEquals(esperado, salida.toString(StandardCharsets.UTF_8));
	}

	@Test
	void serializacionConservaLaMarcaYLeeRegistrosAntiguos() {
		Medicamento medicamento = new Medicamento(
				"Paracetamol", "MED-001", "Analgesico", "Tabletas",
				"Laboratorio A", "Marca A", 20, "2030-01-15", 4.5, 2);

		Medicamento reconstruido = Medicamento.desdeLinea(medicamento.aLinea());
		Medicamento antiguo = Medicamento.desdeLinea(
				"Paracetamol;MED-002;Analgesico;Tabletas;Laboratorio A;10;2030-01-15;4.5;2");
		Medicamento sinPrecio = Medicamento.desdeLinea(
				"Paracetamol;MED-003;Analgesico;Tabletas;Laboratorio A;10;2030-01-15");

		assertEquals("Marca A", reconstruido.getMarca());
		assertEquals(4.5, reconstruido.getPrecio());
		assertEquals("Sin marca", antiguo.getMarca());
		assertEquals("Sin marca", sinPrecio.getMarca());
		assertEquals(0.0, sinPrecio.getPrecio());
	}

	/**
	 * Verifica que getCantidadDisponible devuelva la cantidad almacenada.
	 */
	@Test
	void getCantidadDisponibleDevuelveLaCantidadActual() {
		Medicamento medicamento = new Medicamento(
				"Paracetamol", "MED-001", "Analgesico",
				"Tabletas", "Laboratorio A", 20, "2030-01-15");

		assertEquals(20, medicamento.getCantidadDisponible());
	}

	/**
	 * Verifica que reducirCantidad descuente del inventario la cantidad indicada.
	 */
	@Test
	void reducirCantidadDescuentaDelInventario() {
		Medicamento medicamento = new Medicamento(
				"Paracetamol", "MED-001", "Analgesico",
				"Tabletas", "Laboratorio A", 20, "2030-01-15");

		medicamento.reducirCantidad(5);

		assertEquals(15, medicamento.getCantidadDisponible());
	}

	@Test
	void constructorRechazaPrecioNegativo() {
		assertThrows(IllegalArgumentException.class,
				() -> crearMedicamento(10, -0.01));
	}

	@Test
	void constructorRechazaPrecioNoFinito() {
		for (double precio : new double[]{Double.NaN,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			assertThrows(IllegalArgumentException.class,
					() -> crearMedicamento(10, precio));
		}
	}

	@Test
	void constructorRechazaCantidadNegativa() {
		assertThrows(IllegalArgumentException.class,
				() -> crearMedicamento(-1, 1.0));
	}

	@Test
	void constructorAceptaCantidadCero() {
		Medicamento medicamento = crearMedicamento(0, 1.0);

		assertEquals(0, medicamento.getCantidadDisponible());
	}

	@Test
	void constructorRechazaNombreVacio() {
		assertThrows(IllegalArgumentException.class,
				() -> new Medicamento("", "MED-010", "Analgesico",
						"Tabletas", "Laboratorio A", 10, "2030-01-15"));
	}

	@Test
	void constructorRechazaFechaInvalida() {
		assertThrows(DateTimeParseException.class,
				() -> new Medicamento("Paracetamol", "MED-010", "Analgesico",
						"Tabletas", "Laboratorio A", 10, "2030-13-15"));
	}

	@Test
	void constructorRechazaStockMinimoNegativo() {
		assertThrows(IllegalArgumentException.class,
				() -> crearMedicamento(10, 1.0, -1));
	}

	@Test
	void actualizarInfoRechazaPrecioNegativo() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		assertThrows(IllegalArgumentException.class,
				() -> actualizar(medicamento, 10, -0.01));
		assertEquals(1.0, medicamento.getPrecio());
		assertEquals(10, medicamento.getCantidadDisponible());
	}

	@Test
	void actualizarInfoRechazaPrecioNoFinito() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		for (double precio : new double[]{Double.NaN,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			assertThrows(IllegalArgumentException.class,
					() -> actualizar(medicamento, 10, precio));
		}
		assertEquals(1.0, medicamento.getPrecio());
	}

	@Test
	void actualizarInfoRechazaCantidadNegativa() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		assertThrows(IllegalArgumentException.class,
				() -> actualizar(medicamento, -1, 1.0));
		assertEquals(10, medicamento.getCantidadDisponible());
	}

	@Test
	void actualizarInfoAceptaCantidadCero() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		actualizar(medicamento, 0, 1.0);

		assertEquals(0, medicamento.getCantidadDisponible());
	}

	@Test
	void actualizarInfoRechazaNombreVacio() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		assertThrows(IllegalArgumentException.class,
				() -> medicamento.actualizarInfo("", "Analgesico", "Tabletas",
						"Laboratorio A", "Marca A", 5, "2030-01-15", 2.0, 2));
		assertEquals("Paracetamol", medicamento.getNombre());
	}

	@Test
	void actualizarInfoRechazaFechaInvalida() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		assertThrows(DateTimeParseException.class,
				() -> medicamento.actualizarInfo("Aspirina", "Analgesico", "Tabletas",
						"Laboratorio A", "Marca A", 5, "2030-13-15", 2.0, 2));
		assertEquals("Aspirina", medicamento.getNombre());
		assertEquals(5, medicamento.getCantidadDisponible());
		assertEquals("2030-01-15", medicamento.getFechaVencimiento());
		assertEquals(1.0, medicamento.getPrecio());
	}

	@Test
	void actualizarInfoRechazaStockMinimoNegativo() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		assertThrows(IllegalArgumentException.class,
				() -> actualizar(medicamento, 10, 1.0, -1));
		assertEquals(10, medicamento.getCantidadDisponible());
	}

	@Test
	void reponerCantidadAumentaElStock() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		medicamento.reponerCantidad(5);

		assertEquals(15, medicamento.getCantidadDisponible());
	}

	@Test
	void reponerCantidadRechazaCantidadNoPositiva() {
		Medicamento medicamento = crearMedicamento(10, 1.0);

		assertThrows(IllegalArgumentException.class,
				() -> medicamento.reponerCantidad(0));
	}

	private Medicamento crearMedicamento(int cantidad, double precio) {
		return crearMedicamento(cantidad, precio, 2);
	}

	private Medicamento crearMedicamento(int cantidad, double precio, int stockMinimo) {
		return new Medicamento("Paracetamol", "MED-010", "Analgesico",
				"Tabletas", "Laboratorio A", "Marca A", cantidad,
				"2030-01-15", precio, stockMinimo);
	}

	private void actualizar(Medicamento medicamento, int cantidad, double precio) {
		actualizar(medicamento, cantidad, precio, 2);
	}

	private void actualizar(
			Medicamento medicamento, int cantidad, double precio, int stockMinimo) {
		medicamento.actualizarInfo("Paracetamol", "Analgesico", "Tabletas",
				"Laboratorio A", "Marca A", cantidad, "2030-01-15", precio, stockMinimo);
	}
}

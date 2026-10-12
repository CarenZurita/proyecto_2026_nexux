package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
}

package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PermisoTest {
    @Test
    void constructorGuardaNombreYDescripcion() {
        Permiso permiso = new Permiso("CONSULTAR_STOCK", "Consultar existencias");

        assertEquals("CONSULTAR_STOCK", permiso.getNombre());
        assertEquals("Consultar existencias", permiso.getDescripcion());
    }

    @Test
    void permisosConElMismoNombreSonIguales() {
        Permiso primero = new Permiso("CONSULTAR_STOCK", "Consultar existencias");
        Permiso segundo = new Permiso("CONSULTAR_STOCK", "Otra descripción");

        assertEquals(primero, segundo);
        assertEquals(primero.hashCode(), segundo.hashCode());
    }

    @Test
    void noPermiteDatosObligatoriosVacios() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> new Permiso("", "Descripción"));

        assertEquals("El nombre y la descripción del permiso son obligatorios.",
                excepcion.getMessage());
    }
}

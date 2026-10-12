package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RolTest {
    @Test
    void rolAdministradorTienePermisosAdministrativos() {
        assertTrue(Rol.ADMINISTRADOR.esAdministrador());
        assertTrue(Rol.ADMINISTRADOR.verificarPermiso(
                new Permiso("GESTIONAR_USUARIOS", "Administrar usuarios")));
        assertTrue(Rol.ADMINISTRADOR.verificarPermiso(
                new Permiso("GESTIONAR_MEDICAMENTOS", "Gestionar medicamentos")));
    }

    @Test
    void rolEncargadoIncluyePermisoParaRegistrarVentas() {
        assertFalse(Rol.ENCARGADO.esAdministrador());
        assertTrue(Rol.ENCARGADO.verificarPermiso(
                new Permiso("REGISTRAR_VENTAS", "Registrar ventas")));
    }

    @Test
    void asignarYRevocarPermisoActualizaLosPermisosDelRol() {
        Rol rol = Rol.porNombre("ENCARGADO");
        Permiso nuevo = new Permiso("CONSULTAR_VENTAS", "Consultar historial");

        assertFalse(rol.verificarPermiso(nuevo));
        rol.asignarPermiso(nuevo);
        assertTrue(rol.verificarPermiso(nuevo));
        assertEquals(4, rol.getPermisos().size());
        rol.revocarPermiso(nuevo);
        assertFalse(rol.verificarPermiso(nuevo));
    }
}

package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Rol {
    public static final Rol ADMINISTRADOR =
            new Rol("ADMINISTRADOR", permisosAdministrador());
    public static final Rol ENCARGADO = new Rol("ENCARGADO", permisosEncargado());

    private final String nombre;
    private final List<Permiso> permisos;

    public Rol(String nombre) {
        this(nombre, List.of());
    }

    private Rol(String nombre, List<Permiso> permisos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }
        this.nombre = nombre.trim().toUpperCase(java.util.Locale.ROOT);
        this.permisos = new ArrayList<>(permisos);
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(nombre);
    }

    public void asignarPermiso(Permiso permiso) {
        Objects.requireNonNull(permiso, "El permiso es obligatorio.");
        if (!verificarPermiso(permiso)) {
            permisos.add(permiso);
        }
    }

    public void revocarPermiso(Permiso permiso) {
        if (permiso != null) {
            permisos.removeIf(actual -> actual.equals(permiso));
        }
    }

    public boolean verificarPermiso(Permiso permiso) {
        return permiso != null && permisos.contains(permiso);
    }

    public List<Permiso> getPermisos() {
        return List.copyOf(permisos);
    }

    static Rol porNombre(String nombre) {
        return switch (nombre) {
            case "ADMINISTRADOR" -> new Rol("ADMINISTRADOR", permisosAdministrador());
            case "ENCARGADO" -> new Rol("ENCARGADO", permisosEncargado());
            default -> throw new IllegalArgumentException("Rol inválido.");
        };
    }

    private static List<Permiso> permisosAdministrador() {
        return List.of(
                new Permiso("GESTIONAR_USUARIOS", "Administrar cuentas de encargados"),
                new Permiso("GESTIONAR_MEDICAMENTOS",
                        "Registrar, actualizar y eliminar medicamentos"),
                new Permiso("CONSULTAR_VENTAS", "Consultar el historial de ventas"));
    }

    private static List<Permiso> permisosEncargado() {
        return List.of(
                new Permiso("CONSULTAR_MEDICAMENTOS",
                        "Buscar medicamentos y consultar stock"),
                new Permiso("REGISTRAR_VENTAS", "Registrar ventas"),
                new Permiso("REVISAR_VENCIMIENTOS",
                        "Revisar medicamentos próximos a vencer"));
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Rol otro)) {
            return false;
        }
        return nombre.equals(otro.nombre);
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}

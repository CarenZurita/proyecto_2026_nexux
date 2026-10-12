package model;

import java.util.Objects;

public final class Permiso {
    private final String nombre;
    private final String descripcion;

    public Permiso(String nombre, String descripcion) {
        if (nombre == null || nombre.isBlank()
                || descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre y la descripción del permiso son obligatorios.");
        }
        this.nombre = nombre.trim();
        this.descripcion = descripcion.trim();
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Permiso otro)) {
            return false;
        }
        return nombre.equals(otro.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
}

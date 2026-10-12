package model;

import java.util.Objects;

public class DetalleVenta {
    private final Medicamento medicamento;
    private final int cantidad;
    private final double precioUnitario;
    private double subtotal;

    public DetalleVenta(Medicamento medicamento, int cantidad) {
        this(medicamento, cantidad,
                Objects.requireNonNull(medicamento, "El medicamento es obligatorio.")
                        .getPrecio());
    }

    public DetalleVenta(Medicamento medicamento, int cantidad, double precioUnitario) {
        this.medicamento = Objects.requireNonNull(
                medicamento, "El medicamento es obligatorio.");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        if (!Double.isFinite(precioUnitario) || precioUnitario < 0) {
            throw new IllegalArgumentException("El precio unitario no es válido.");
        }
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }

    public double calcularSubtotal() {
        subtotal = cantidad * precioUnitario;
        return subtotal;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public double getSubtotal() {
        return subtotal;
    }
}

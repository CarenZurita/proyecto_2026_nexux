package model;

import java.time.LocalDate;

public class Medicamento {
    public enum EstadoVencimiento {
        VIGENTE,
        PROXIMO_A_VENCER,
        VENCIDO
    }

    private String nombre;
    private String codigo;
    private String categoria;
    private String presentacion;
    private String laboratorio;
    private String marca;
    private int cantidadDisponible;
    private LocalDate fechaVencimiento;
    private double precio;
    private int stockMinimo;

    public Medicamento(String nombre, String codigo, String categoria,
                       String presentacion, String laboratorio,
                       int cantidadDisponible, String fechaVencimiento) {
        this(nombre, codigo, categoria, presentacion, laboratorio,
                cantidadDisponible, fechaVencimiento, 0.0, 0);
    }

    public Medicamento(String nombre, String codigo, String categoria,
                       String presentacion, String laboratorio,
                       int cantidadDisponible, String fechaVencimiento,
                       double precio, int stockMinimo) {
        this(nombre, codigo, categoria, presentacion, laboratorio, "Sin marca",
                cantidadDisponible, fechaVencimiento, precio, stockMinimo);
    }

    public Medicamento(String nombre, String codigo, String categoria,
                       String presentacion, String laboratorio, String marca,
                       int cantidadDisponible, String fechaVencimiento,
                       double precio, int stockMinimo) {
        validarCantidad(cantidadDisponible, stockMinimo);
        validarPrecio(precio);
        this.nombre = validarTexto(nombre, "El nombre es obligatorio.");
        this.codigo = validarTexto(codigo, "El código es obligatorio.");
        this.categoria = validarTexto(categoria, "La categoría es obligatoria.");
        this.presentacion = validarTexto(presentacion, "La presentación es obligatoria.");
        this.laboratorio = validarTexto(laboratorio, "El laboratorio es obligatorio.");
        this.marca = validarTexto(marca, "La marca es obligatoria.");
        this.cantidadDisponible = cantidadDisponible;
        this.fechaVencimiento = LocalDate.parse(fechaVencimiento);
        this.precio = precio;
        this.stockMinimo = stockMinimo;
    }

    public void mostrarDatos() {
        System.out.println("Nombre: " + nombre);
        System.out.println("Código: " + codigo);
        System.out.println("Categoría: " + categoria);
        System.out.println("Presentación: " + presentacion);
        System.out.println("Laboratorio: " + laboratorio);
        System.out.println("Marca: " + marca);
        System.out.println("Cantidad disponible: " + cantidadDisponible);
        System.out.println("Precio: " + precio);
        System.out.println("Stock mínimo: " + stockMinimo);
        System.out.println("Fecha de vencimiento: " + fechaVencimiento);
    }

    public void actualizarInfo(String nombre, String categoria,
                               String presentacion, String laboratorio,
                               int cantidadDisponible, String fechaVencimiento) {
        actualizarInfo(nombre, categoria, presentacion, laboratorio,
                cantidadDisponible, fechaVencimiento, precio, stockMinimo);
    }

    public void actualizarInfo(String nombre, String categoria,
                               String presentacion, String laboratorio,
                               int cantidadDisponible, String fechaVencimiento,
                               double precio, int stockMinimo) {
        validarCantidad(cantidadDisponible, stockMinimo);
        validarPrecio(precio);
        this.nombre = validarTexto(nombre, "El nombre es obligatorio.");
        this.categoria = validarTexto(categoria, "La categoría es obligatoria.");
        this.presentacion = validarTexto(presentacion, "La presentación es obligatoria.");
        this.laboratorio = validarTexto(laboratorio, "El laboratorio es obligatorio.");
        this.cantidadDisponible = cantidadDisponible;
        this.fechaVencimiento = LocalDate.parse(fechaVencimiento);
        this.precio = precio;
        this.stockMinimo = stockMinimo;
    }

    public void actualizarInfo(String nombre, String categoria,
                               String presentacion, String laboratorio, String marca,
                               int cantidadDisponible, String fechaVencimiento,
                               double precio, int stockMinimo) {
        validarCantidad(cantidadDisponible, stockMinimo);
        validarPrecio(precio);
        this.nombre = validarTexto(nombre, "El nombre es obligatorio.");
        this.categoria = validarTexto(categoria, "La categoría es obligatoria.");
        this.presentacion = validarTexto(presentacion, "La presentación es obligatoria.");
        this.laboratorio = validarTexto(laboratorio, "El laboratorio es obligatorio.");
        this.marca = validarTexto(marca, "La marca es obligatoria.");
        this.cantidadDisponible = cantidadDisponible;
        this.fechaVencimiento = LocalDate.parse(fechaVencimiento);
        this.precio = precio;
        this.stockMinimo = stockMinimo;
    }

    public EstadoVencimiento controlarMedicamento(int diasAviso) {
        if (diasAviso < 0) {
            throw new IllegalArgumentException("Los días de aviso no pueden ser negativos.");
        }
        if (fechaVencimiento.isBefore(LocalDate.now())) {
            return EstadoVencimiento.VENCIDO;
        }
        if (!fechaVencimiento.isAfter(LocalDate.now().plusDays(diasAviso))) {
            return EstadoVencimiento.PROXIMO_A_VENCER;
        }
        return EstadoVencimiento.VIGENTE;
    }

    public boolean estaVencido() {
        return controlarMedicamento(0) == EstadoVencimiento.VENCIDO;
    }

    public boolean estaProximoAVencer(int dias) {
        return controlarMedicamento(dias) == EstadoVencimiento.PROXIMO_A_VENCER;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public String getMarca() {
        return marca;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento.toString();
    }

    public LocalDate getFechaVencimientoDate() {
        return fechaVencimiento;
    }

    public double getPrecio() {
        return precio;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public String getDescripcion() {
        return categoria;
    }

    public void reducirCantidad(int cantidad) {
        if (cantidad <= 0 || cantidad > cantidadDisponible) {
            throw new IllegalArgumentException("La cantidad a reducir no es válida.");
        }
        cantidadDisponible -= cantidad;
    }

    public void reponerCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer no es válida.");
        }
        cantidadDisponible += cantidad;
    }

    public String aLinea() {
        return String.join(";", nombre, codigo, categoria, presentacion,
                laboratorio, String.valueOf(cantidadDisponible),
                fechaVencimiento.toString(), String.valueOf(precio),
                String.valueOf(stockMinimo), marca);
    }

    public static Medicamento desdeLinea(String linea) {
        String[] datos = linea.split(";", -1);
        if (datos.length != 7 && datos.length != 9 && datos.length != 10) {
            throw new IllegalArgumentException("El registro del medicamento no es válido.");
        }
        try {
            double precio = 0.0;
            int stockMinimo = 0;
            if (datos.length >= 9) {
                precio = Double.parseDouble(datos[7]);
                stockMinimo = Integer.parseInt(datos[8]);
            }
            String marca = datos.length == 10 ? datos[9] : "Sin marca";
            return new Medicamento(datos[0], datos[1], datos[2], datos[3],
                    datos[4], marca, Integer.parseInt(datos[5]), datos[6],
                    precio, stockMinimo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La cantidad o el precio del medicamento no son válidos.", e);
        }
    }

    private static String validarTexto(String texto, String mensaje) {
        if (texto == null || texto.isBlank() || texto.contains(";")) {
            throw new IllegalArgumentException(mensaje);
        }
        return texto.trim();
    }

    private static void validarCantidad(int cantidad, int minimo) {
        if (cantidad < 0 || minimo < 0) {
            throw new IllegalArgumentException(
                    "La cantidad disponible y el stock mínimo no pueden ser negativos.");
        }
    }

    private static void validarPrecio(double precio) {
        if (!Double.isFinite(precio) || precio < 0) {
            throw new IllegalArgumentException("El precio no es válido.");
        }
    }
}

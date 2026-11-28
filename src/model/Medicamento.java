package model;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Entidad de inventario del modelo MVC: concentra los datos y las reglas
 * locales de cantidad, precio, stock y vencimiento.
 */
public class Medicamento {
    private static final DateTimeFormatter FORMATO_FECHA_TEXTO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Estado calculado del medicamento respecto de su fecha de vencimiento. */
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

    /**
     * Crea un medicamento con valores predeterminados de precio y stock mínimo.
     *
     * @param nombre nombre visible
     * @param codigo identificador del inventario
     * @param categoria categoría del medicamento
     * @param presentacion presentación comercial
     * @param laboratorio laboratorio de origen
     * @param cantidadDisponible unidades disponibles; cero está permitido
     * @param fechaVencimiento fecha en formato ISO-8601
     * @throws IllegalArgumentException si algún campo requerido o valor numérico no es válido
     * @throws java.time.format.DateTimeParseException si la fecha no es ISO-8601
     */
    public Medicamento(String nombre, String codigo, String categoria,
                       String presentacion, String laboratorio,
                       int cantidadDisponible, String fechaVencimiento) {
        this(nombre, codigo, categoria, presentacion, laboratorio,
                cantidadDisponible, fechaVencimiento, 0.0, 0);
    }

    /**
     * Crea un medicamento con precio y stock mínimo, usando la marca predeterminada.
     *
     * @param nombre nombre visible
     * @param codigo identificador del inventario
     * @param categoria categoría del medicamento
     * @param presentacion presentación comercial
     * @param laboratorio laboratorio de origen
     * @param cantidadDisponible unidades disponibles; cero está permitido
     * @param fechaVencimiento fecha en formato ISO-8601
     * @param precio precio finito no negativo
     * @param stockMinimo umbral no negativo para alertas de inventario
     * @throws IllegalArgumentException si algún dato no cumple las reglas del modelo
     * @throws java.time.format.DateTimeParseException si la fecha no es ISO-8601
     */
    public Medicamento(String nombre, String codigo, String categoria,
                       String presentacion, String laboratorio,
                       int cantidadDisponible, String fechaVencimiento,
                       double precio, int stockMinimo) {
        this(nombre, codigo, categoria, presentacion, laboratorio, "Sin marca",
                cantidadDisponible, fechaVencimiento, precio, stockMinimo);
    }

    /**
     * Crea un medicamento con todos sus atributos de inventario.
     *
     * @param nombre nombre visible
     * @param codigo identificador del inventario
     * @param categoria categoría del medicamento
     * @param presentacion presentación comercial
     * @param laboratorio laboratorio de origen
     * @param marca marca comercial
     * @param cantidadDisponible unidades disponibles; cero está permitido
     * @param fechaVencimiento fecha en formato ISO-8601
     * @param precio precio finito no negativo
     * @param stockMinimo umbral no negativo para alertas de inventario
     * @throws IllegalArgumentException si algún dato no cumple las reglas del modelo
     * @throws java.time.format.DateTimeParseException si la fecha no es ISO-8601
     */
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

    /**
     * Actualiza los campos de negocio sin alterar precio, marca ni stock mínimo.
     *
     * @param nombre nuevo nombre
     * @param categoria nueva categoría
     * @param presentacion nueva presentación
     * @param laboratorio nuevo laboratorio
     * @param cantidadDisponible nueva cantidad; cero está permitido
     * @param fechaVencimiento nueva fecha ISO-8601
     * @throws IllegalArgumentException si un campo no es válido
     * @throws java.time.format.DateTimeParseException si la fecha no es ISO-8601
     */
    public void actualizarInfo(String nombre, String categoria,
                               String presentacion, String laboratorio,
                               int cantidadDisponible, String fechaVencimiento) {
        actualizarInfo(nombre, categoria, presentacion, laboratorio,
                cantidadDisponible, fechaVencimiento, precio, stockMinimo);
    }

    /**
     * Actualiza datos básicos, cantidad, fecha y precio sin cambiar marca o stock mínimo.
     *
     * @param nombre nuevo nombre
     * @param categoria nueva categoría
     * @param presentacion nueva presentación
     * @param laboratorio nuevo laboratorio
     * @param cantidadDisponible nueva cantidad; cero está permitido
     * @param fechaVencimiento nueva fecha ISO-8601
     * @param precio nuevo precio finito no negativo
     * @param stockMinimo umbral no negativo
     * @throws IllegalArgumentException si un campo no es válido
     * @throws java.time.format.DateTimeParseException si la fecha no es ISO-8601
     */
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

    /**
     * Actualiza los campos editables del medicamento, incluida la marca.
     *
     * @param nombre nuevo nombre
     * @param categoria nueva categoría
     * @param presentacion nueva presentación
     * @param laboratorio nuevo laboratorio
     * @param marca nueva marca
     * @param cantidadDisponible nueva cantidad; cero está permitido
     * @param fechaVencimiento nueva fecha ISO-8601
     * @param precio nuevo precio finito no negativo
     * @param stockMinimo umbral no negativo
     * @throws IllegalArgumentException si un campo no es válido
     * @throws java.time.format.DateTimeParseException si la fecha no es ISO-8601
     */
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

    /**
     * Clasifica el vencimiento comparándolo con la fecha actual y el plazo de aviso.
     *
     * @param diasAviso cantidad de días de anticipación, no negativa
     * @return estado vigente, próximo a vencer o vencido
     * @throws IllegalArgumentException si el plazo es negativo
     * @throws java.time.DateTimeException si el cálculo excede el rango de fechas
     */
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

    /**
     * Reduce el stock solo si la cantidad es positiva y no supera lo disponible.
     *
     * @param cantidad unidades que se descuentan
     * @throws IllegalArgumentException si la cantidad no es válida
     */
    public void reducirCantidad(int cantidad) {
        if (cantidad <= 0 || cantidad > cantidadDisponible) {
            throw new IllegalArgumentException("La cantidad a reducir no es válida.");
        }
        cantidadDisponible -= cantidad;
    }

    /**
     * Aumenta el stock con una cantidad estrictamente positiva.
     *
     * @param cantidad unidades que se reponen
     * @throws IllegalArgumentException si la cantidad no es positiva
     */
    public void reponerCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer no es válida.");
        }
        cantidadDisponible += cantidad;
    }

    /**
     * Serializa el medicamento con el formato TXT vigente.
     *
     * @return registro separado por punto y coma
     */
    public String aLinea() {
        return String.join(";", nombre, codigo, categoria, presentacion,
                laboratorio, String.valueOf(cantidadDisponible),
                fechaVencimiento.format(FORMATO_FECHA_TEXTO), String.valueOf(precio),
                String.valueOf(stockMinimo), marca);
    }

    /**
     * Reconstruye y valida un medicamento desde una línea TXT compatible.
     *
     * @param linea registro separado por punto y coma
     * @return medicamento reconstruido
     * @throws IllegalArgumentException si el formato o sus datos son inválidos
     */
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
                    datos[4], marca, Integer.parseInt(datos[5]), fechaIso(datos[6]),
                    precio, stockMinimo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La cantidad o el precio del medicamento no son válidos.", e);
        }
    }

    private static String fechaIso(String fecha) {
        try {
            LocalDate fechaParseada = fecha.matches("\\d{2}/\\d{2}/\\d{4}")
                    ? LocalDate.parse(fecha, FORMATO_FECHA_TEXTO)
                    : LocalDate.parse(fecha);
            return fechaParseada.toString();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("La fecha de vencimiento no es válida.", e);
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

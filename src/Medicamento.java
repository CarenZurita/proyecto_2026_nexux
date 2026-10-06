public class Medicamento {
    private String nombre;
    private String codigo;
    private String categoria;
    private String presentacion;
    private String laboratorio;
    private int cantidadDisponible;
    private String fechaVencimiento;

    public Medicamento(String nombre, String codigo, String categoria,
                       String presentacion, String laboratorio,
                       int cantidadDisponible, String fechaVencimiento) {
        this.nombre = nombre;
        this.codigo = codigo;
        this.categoria = categoria;
        this.presentacion = presentacion;
        this.laboratorio = laboratorio;
        this.cantidadDisponible = cantidadDisponible;
        this.fechaVencimiento = fechaVencimiento;
    }

    public void mostrarDatos() {
        System.out.println("Nombre: " + nombre);
        System.out.println("Código: " + codigo);
        System.out.println("Categoría: " + categoria);
        System.out.println("Presentación: " + presentacion);
        System.out.println("Laboratorio: " + laboratorio);
        System.out.println("Cantidad disponible: " + cantidadDisponible);
        System.out.println("Fecha de vencimiento: " + fechaVencimiento);
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void reducirCantidad(int cantidad) {
        cantidadDisponible = cantidadDisponible - cantidad;
    }
}

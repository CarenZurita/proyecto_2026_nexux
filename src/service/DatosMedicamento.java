package service;

/**
 * DTO de entrada para el formulario de medicamentos; conserva como texto
 * los valores numéricos y la fecha hasta que el servicio los valide.
 *
 * @param nombre nombre del medicamento
 * @param codigo identificador del inventario
 * @param categoria categoría seleccionada
 * @param presentacion presentación comercial
 * @param laboratorio laboratorio de origen
 * @param precio precio capturado con coma o punto decimal
 * @param cantidad cantidad capturada como entero
 * @param vencimiento mes y año capturados en formato {@code MM/AAAA}
 */
public record DatosMedicamento(
        String nombre,
        String codigo,
        String categoria,
        String presentacion,
        String laboratorio,
        String precio,
        String cantidad,
        String vencimiento) {
}

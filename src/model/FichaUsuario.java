package model;

/**
 * DTO inmutable que la capa de servicio entrega a la vista sin exponer
 * credenciales ni objetos de persistencia.
 *
 * @param id identificador de la cuenta
 * @param nombre nombre visible
 * @param usuario nombre de acceso
 * @param rol rol asignado
 * @param activo estado de acceso
 */
public record FichaUsuario(int id, String nombre, String usuario, Rol rol, boolean activo) { }

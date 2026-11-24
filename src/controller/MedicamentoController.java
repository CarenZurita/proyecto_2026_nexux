package controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import model.Medicamento;
import model.Usuario;
import service.DatosMedicamento;
import service.MedicamentoService;

/**
 * Controlador MVC del inventario: recibe acciones de las vistas y delega
 * autorización y reglas de negocio a {@link MedicamentoService}.
 */
public class MedicamentoController {
    private final MedicamentoService servicio;

    /**
     * Crea el controlador con el servicio inyectado desde la composición principal.
     *
     * @param servicio operaciones de inventario y validación
     */
    public MedicamentoController(MedicamentoService servicio) {
        this.servicio = servicio;
    }

    /**
     * Lista el inventario visible para la sesión.
     *
     * @param sesion cuenta autenticada
     * @return medicamentos persistidos
     * @throws IOException si falla la lectura
     * @throws SecurityException si la sesión no está autorizada
     */
    public List<Medicamento> listar(Usuario sesion) throws IOException {
        return servicio.listar(sesion);
    }

    /**
     * Busca medicamentos con el criterio unificado del servicio.
     *
     * @param sesion cuenta autenticada
     * @param consulta texto o precio que se busca
     * @return medicamentos coincidentes
     * @throws IOException si falla la lectura
     * @throws SecurityException si la sesión no está autorizada
     * @throws IllegalArgumentException si la consulta está vacía
     */
    public List<Medicamento> buscar(Usuario sesion, String consulta) throws IOException {
        return servicio.buscar(sesion, consulta);
    }

    /**
     * Consulta existencias por código.
     *
     * @param sesion cuenta autenticada
     * @param codigo código del medicamento
     * @return medicamento encontrado, o vacío
     * @throws IOException si falla la lectura
     * @throws SecurityException si la sesión no está autorizada
     */
    public Optional<Medicamento> consultarStock(Usuario sesion, String codigo)
            throws IOException {
        return servicio.consultarStock(sesion, codigo);
    }

    /**
     * Registra los valores capturados en el formulario.
     *
     * @param sesion administrador autenticado
     * @param datos datos crudos que valida el servicio
     * @throws IOException si falla la persistencia
     * @throws IllegalArgumentException si los valores no son válidos
     * @throws SecurityException si la sesión no es administradora
     */
    public void registrar(Usuario sesion, DatosMedicamento datos) throws IOException {
        servicio.registrar(sesion, datos);
    }

    /**
     * Actualiza los campos del formulario para el código original.
     *
     * @param sesion administrador autenticado
     * @param codigoOriginal código que identifica el registro actual
     * @param datos datos crudos que valida el servicio
     * @throws IOException si falla la persistencia
     * @throws IllegalArgumentException si los valores no son válidos
     * @throws SecurityException si la sesión no es administradora
     */
    public void actualizar(Usuario sesion, String codigoOriginal, DatosMedicamento datos)
            throws IOException {
        servicio.actualizar(sesion, codigoOriginal, datos);
    }

    /**
     * Elimina el medicamento por código.
     *
     * @param sesion administrador autenticado
     * @param codigo código que se elimina
     * @throws IOException si falla la persistencia
     * @throws IllegalArgumentException si el medicamento no existe
     * @throws SecurityException si la sesión no es administradora
     */
    public void eliminar(Usuario sesion, String codigo) throws IOException {
        servicio.eliminar(sesion, codigo);
    }
}

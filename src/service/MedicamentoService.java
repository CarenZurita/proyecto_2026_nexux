package service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;
import model.Medicamento;
import model.Permiso;
import model.Rol;
import model.Usuario;
import persistence.MedicamentoRepository;

/**
 * Servicio de Medicamentos: centraliza autorización, validación de formularios
 * y coordinación entre las vistas/controladores y el repositorio de inventario.
 */
public class MedicamentoService {
    private static final String MENSAJE_CAMPOS_OBLIGATORIOS =
            "Completa todos los campos obligatorios.";
    private static final String MENSAJE_NUMEROS =
            "Precio o cantidad no válidos. Usa números positivos.";
    private static final String MENSAJE_FECHA =
            "La fecha debe tener formato dd/MM/aaaa, por ejemplo 31/10/2025.";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final MedicamentoRepository repositorio;

    /**
     * Crea el servicio con el repositorio que inyecta la composición de la aplicación.
     *
     * @param repositorio persistencia de medicamentos
     */
    public MedicamentoService(MedicamentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    MedicamentoService(Path archivoMedicamentos) {
        this(new MedicamentoRepository(archivoMedicamentos));
    }

    /**
     * Lista el inventario para una sesión autenticada de administrador o encargado.
     *
     * @param sesion sesión que consulta
     * @return inventario persistido
     * @throws SecurityException si la sesión o el rol no permiten consultar
     * @throws IOException si no se puede cargar el inventario
     */
    public List<Medicamento> listar(Usuario sesion) throws IOException {
        validarSesionConsulta(sesion);
        try {
            return repositorio.listar();
        } catch (IOException e) {
            throw new IOException("No se pudo cargar el inventario: " + e.getMessage(), e);
        }
    }

    /**
     * Busca por nombre, código, marca o precio exacto según los criterios del repositorio.
     *
     * @param sesion sesión que consulta
     * @param consulta criterio no vacío
     * @return medicamentos coincidentes
     * @throws SecurityException si la sesión o el rol no permiten consultar
     * @throws IllegalArgumentException si la consulta está vacía
     * @throws IOException si no se puede cargar el inventario
     */
    public List<Medicamento> buscar(Usuario sesion, String consulta) throws IOException {
        validarSesionConsulta(sesion);
        if (consulta == null || consulta.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar un texto para buscar.");
        }
        try {
            return repositorio.buscar(consulta.trim());
        } catch (IOException e) {
            throw new IOException("No se pudo cargar el inventario: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta un medicamento por código, permitida a ambos roles de usuario.
     *
     * @param sesion sesión que consulta
     * @param codigo código del medicamento
     * @return medicamento encontrado, o vacío si no existe
     * @throws SecurityException si la sesión o el rol no permiten consultar
     * @throws IOException si falla la lectura
     */
    public Optional<Medicamento> consultarStock(Usuario sesion, String codigo)
            throws IOException {
        validarSesionConsulta(sesion);
        return repositorio.buscarPorCodigo(codigo);
    }

    /**
     * Valida los campos del formulario, exige código único y registra un medicamento.
     *
     * @param sesion sesión administradora vigente
     * @param datos valores crudos del formulario, incluidos precio y cantidad
     * @throws SecurityException si no hay sesión administradora vigente
     * @throws IllegalArgumentException si faltan datos, hay duplicado o una regla falla
     * @throws IOException si falla la persistencia o el guardado
     */
    /**
     * Valida los campos del formulario, exige código único y registra un medicamento.
     *
     * @param sesion sesión administradora vigente
     * @param datos valores crudos del formulario, incluidos precio y cantidad
     * @throws SecurityException si no hay sesión administradora vigente
     * @throws IllegalArgumentException si faltan datos, hay duplicado o una regla falla
     * @throws IOException si falla la persistencia o el guardado
     */
    public void registrar(Usuario sesion, DatosMedicamento datos) throws IOException {
        guardarMedicamento(sesion, datos, null);
    }

    /**
     * Valida y persiste cambios de formulario conservando marca y stock mínimo existentes.
     *
     * @param sesion sesión administradora vigente
     * @param codigoOriginal código que identifica el registro que se edita
     * @param datos valores crudos del formulario
     * @throws SecurityException si no hay sesión administradora vigente
     * @throws IllegalArgumentException si los campos fallan validación o el código duplica otro
     * @throws IllegalStateException si el registro dejó de existir
     * @throws IOException si falla la lectura o escritura
     */
    public void actualizar(Usuario sesion, String codigoOriginal, DatosMedicamento datos)
            throws IOException {
        guardarMedicamento(sesion, datos, codigoOriginal);
    }

    /**
     * Flujo compartido de alta o edición de medicamento desde el formulario.
     */
    private void guardarMedicamento(Usuario sesion, DatosMedicamento datos,
                                    String codigoOriginal) throws IOException {
        validarAdministradorFormulario(sesion, "Error al guardar: ");
        DatosMedicamento campos = normalizarYValidarCampos(datos);
        List<Medicamento> existentes = listarParaGuardar();
        validarCodigoUnico(campos.codigo(), codigoOriginal, existentes);
        ValoresMedicamento valores = validarValores(campos);

        String marca = "Sin marca";
        int stockMinimo = 0;
        if (codigoOriginal != null) {
            Medicamento anterior = buscarPorCodigo(existentes, codigoOriginal);
            marca = anterior == null ? "Sin marca" : anterior.getMarca();
            stockMinimo = anterior == null ? 0 : anterior.getStockMinimo();
        }
        Medicamento medicamento = crearMedicamento(campos, valores, marca, stockMinimo);

        try {
            if (codigoOriginal == null) {
                if (repositorio.buscarPorCodigo(campos.codigo()).isPresent()) {
                    throw new IllegalArgumentException(
                            "Error al guardar: Ya existe un medicamento con ese código.");
                }
                repositorio.registrar(medicamento);
            } else {
                if (!repositorio.actualizar(medicamento)) {
                    throw new IllegalStateException(
                            "Error al guardar: El medicamento ya no existe en el inventario.");
                }
            }
        } catch (IOException e) {
            throw errorAlGuardar(e);
        }
    }

    /**
     * Elimina el medicamento por código tras validar la sesión administradora.
     *
     * @param sesion sesión administradora vigente
     * @param codigo código del medicamento
     * @throws SecurityException si no hay sesión administradora vigente
     * @throws IllegalArgumentException si el registro no existe
     * @throws IllegalStateException si el registro desapareció antes de borrarse
     * @throws IOException si falla el acceso o la escritura
     */
    public void eliminar(Usuario sesion, String codigo) throws IOException {
        validarAdministradorFormulario(sesion, "No se pudo eliminar el medicamento: ");
        try {
            if (repositorio.buscarPorCodigo(codigo).isEmpty()) {
                throw new IllegalArgumentException(
                        "El medicamento seleccionado ya no está en el inventario.");
            }
            if (!repositorio.eliminar(codigo)) {
                throw new IllegalStateException(
                        "No se pudo eliminar el medicamento: "
                                + "El medicamento ya no existe en el inventario.");
            }
        } catch (IOException e) {
            throw errorAlEliminar(e);
        }
    }

    /**
     * Registra una entidad ya validada para los flujos de dominio heredados.
     *
     * @param sesion sesión con permiso de gestión
     * @param medicamento medicamento completo
     * @throws SecurityException si la sesión no posee el permiso
     * @throws IOException si falla la persistencia
     */
    public void registrar(Usuario sesion, Medicamento medicamento) throws IOException {
        validarPermisoGestionMedicamentos(sesion);
        repositorio.registrar(medicamento);
    }

    /**
     * Actualiza una entidad ya validada para los flujos de dominio heredados.
     *
     * @param sesion sesión con permiso de gestión
     * @param medicamento medicamento actualizado
     * @return {@code true} si se encontró y actualizó el código
     * @throws SecurityException si la sesión no posee el permiso
     * @throws IOException si falla la persistencia
     */
    public boolean actualizar(Usuario sesion, Medicamento medicamento) throws IOException {
        validarPermisoGestionMedicamentos(sesion);
        return repositorio.actualizar(medicamento);
    }

    /**
     * Elimina una entidad por código para los flujos de dominio heredados.
     *
     * @param sesion sesión con permiso de gestión
     * @param medicamento medicamento cuyo código se elimina
     * @return {@code true} si se encontró y eliminó
     * @throws SecurityException si la sesión no posee el permiso
     * @throws IOException si falla la persistencia
     */
    public boolean eliminar(Usuario sesion, Medicamento medicamento) throws IOException {
        validarPermisoGestionMedicamentos(sesion);
        return repositorio.eliminar(medicamento.getCodigo());
    }

    private static void validarSesionConsulta(Usuario sesion) {
        if (sesion == null || !sesion.estaActivo() || !sesion.estaEnSesion()
                || (!sesion.esAdministrador() && !Rol.ENCARGADO.equals(sesion.getRol()))) {
            throw new SecurityException(
                    "Se requiere una sesión válida para consultar medicamentos.");
        }
    }

    private static void validarAdministradorFormulario(Usuario sesion, String prefijo) {
        if (sesion == null || !sesion.estaEnSesion()
                || !UsuarioServiceSupport.administradorActivo(sesion)) {
            throw new SecurityException(
                    prefijo + "Se requiere una sesión activa de administrador.");
        }
    }

    private static void validarPermisoGestionMedicamentos(Usuario sesion) {
        if (sesion == null || !sesion.estaActivo() || !sesion.estaEnSesion()) {
            throw new SecurityException("Debe iniciar sesión para realizar esta operación.");
        }
        Permiso permiso = new Permiso(
                "GESTIONAR_MEDICAMENTOS", "Permiso requerido para la operación.");
        if (!sesion.getRol().verificarPermiso(permiso)) {
            throw new SecurityException(
                    "El rol no tiene el permiso GESTIONAR_MEDICAMENTOS.");
        }
    }

    private List<Medicamento> listarParaGuardar() throws IOException {
        try {
            return repositorio.listar();
        } catch (IOException e) {
            throw errorAlGuardar(e);
        }
    }

    private static DatosMedicamento normalizarYValidarCampos(DatosMedicamento datos) {
        // Se normalizan los datos antes de validar para que todas las rutas persistan el mismo formato.
        if (datos == null) {
            throw new IllegalArgumentException(MENSAJE_CAMPOS_OBLIGATORIOS);
        }
        DatosMedicamento normalizados = new DatosMedicamento(
                NormalizacionMedicamento.capitalizarPrimeraLetra(recortar(datos.nombre())),
                NormalizacionMedicamento.aMayusculas(recortar(datos.codigo())),
                recortar(datos.categoria()),
                NormalizacionMedicamento.capitalizarPrimeraLetra(
                        recortar(datos.presentacion())),
                recortar(datos.laboratorio()),
                recortar(datos.precio()),
                recortar(datos.cantidad()),
                recortar(datos.vencimiento()));
        if (normalizados.nombre().isEmpty()
                || normalizados.codigo().isEmpty()
                || normalizados.categoria().isEmpty()
                || normalizados.presentacion().isEmpty()
                || normalizados.laboratorio().isEmpty()
                || normalizados.vencimiento().isEmpty()) {
            throw new IllegalArgumentException(MENSAJE_CAMPOS_OBLIGATORIOS);
        }
        return normalizados;
    }

    private static String recortar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static void validarCodigoUnico(
            String codigo, String codigoOriginal, List<Medicamento> existentes) {
        // La validación previa evita duplicados de código en el inventario TXT.
        boolean duplicado = existentes.stream()
                .anyMatch(medicamento -> medicamento.getCodigo().equalsIgnoreCase(codigo)
                        && (codigoOriginal == null
                                || !medicamento.getCodigo().equalsIgnoreCase(codigoOriginal)));
        if (duplicado) {
            throw new IllegalArgumentException(
                    "El código " + codigo + " ya se encuentra registrado.");
        }
    }

    private static ValoresMedicamento validarValores(DatosMedicamento datos) {
        double precio;
        int cantidad;
        try {
            precio = Double.parseDouble(datos.precio().replace(',', '.'));
            cantidad = Integer.parseInt(datos.cantidad());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MENSAJE_NUMEROS, e);
        }
        if (precio < 0 || cantidad < 0) {
            throw new IllegalArgumentException(MENSAJE_NUMEROS);
        }
        String vencimiento = datos.vencimiento();
        try {
            if (vencimiento.matches("\\d{2}/\\d{2}/\\d{4}")) {
                LocalDate fecha = LocalDate.parse(vencimiento, FORMATO_FECHA);
                return new ValoresMedicamento(precio, cantidad, fecha.toString());
            }
            if (vencimiento.matches("\\d{2}/\\d{4}")) {
                YearMonth mes = YearMonth.parse(vencimiento, FORMATO_MES);
                return new ValoresMedicamento(precio, cantidad, mes.atEndOfMonth().toString());
            }
            throw new IllegalArgumentException(MENSAJE_FECHA);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(MENSAJE_FECHA, e);
        }
    }

    private static Medicamento crearMedicamento(
            DatosMedicamento datos, ValoresMedicamento valores, String marca, int stockMinimo) {
        try {
            return new Medicamento(datos.nombre(), datos.codigo(), datos.categoria(),
                    datos.presentacion(), datos.laboratorio(), marca, valores.cantidad(),
                    valores.vencimiento(), valores.precio(), stockMinimo);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IllegalArgumentException("Error al guardar: " + e.getMessage(), e);
        }
    }

    private static Medicamento buscarPorCodigo(List<Medicamento> existentes, String codigo) {
        if (codigo == null) {
            return null;
        }
        return existentes.stream()
                .filter(medicamento -> medicamento.getCodigo().equalsIgnoreCase(codigo))
                .findFirst()
                .orElse(null);
    }

    private static IOException errorAlGuardar(IOException causa) {
        return new IOException("Error al guardar: " + causa.getMessage(), causa);
    }

    private static IOException errorAlEliminar(IOException causa) {
        return new IOException("No se pudo eliminar el medicamento: " + causa.getMessage(),
                causa);
    }

    /** Valores numéricos ya analizados y fecha convertida al formato ISO del modelo. */
    private record ValoresMedicamento(double precio, int cantidad, String vencimiento) {
    }
}

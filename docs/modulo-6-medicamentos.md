# Diagnóstico del módulo 6 — Medicamentos

**Estado:** diagnóstico y 6a completados; 6b-1 y 6b-2 completados. 6c no iniciado.
**Alcance de este documento:** análisis estático del código real y propuesta de pruebas/subpasos. No se modificó código ni `datos/`, no se cambió el formato TXT y no se ejecutó la aplicación Swing.

## 1. Inventario

Los tamaños son líneas físicas de cada archivo. Se incluyen ventas y vencimientos como dependencias limítrofes: las ventas alteran el stock y el reporte consulta medicamentos, pero el plan asigna esas responsabilidades a los módulos 7 y 8.

| Clase / archivo | Líneas | Rol y dependencias actuales |
|---|---:|---|
| [`Medicamento.java`](../src/model/Medicamento.java) | 240 | Entidad: valida atributos, modifica stock, evalúa vencimiento y serializa/deserializa TXT. Usa `LocalDate`; además, `mostrarDatos()` imprime en consola. |
| [`MedicamentoRepository.java`](../src/persistence/MedicamentoRepository.java) | 121 | Lectura, escritura, búsqueda, alta, edición y eliminación UTF-8. Depende de `Medicamento` y NIO; el constructor por defecto apunta a `datos/medicamentos.txt`, aunque acepta `Path`. |
| [`MedicamentoService.java`](../src/service/MedicamentoService.java) | 4 | Clase vacía, sin dependencias ni referencias de producción encontradas. |
| [`MedicamentoController.java`](../src/controller/MedicamentoController.java) | 4 | Clase vacía, sin dependencias ni referencias de producción encontradas. |
| [`MedicamentoView.java`](../src/view/MedicamentoView.java) | 1092 | Vista Swing que también crea el repositorio, carga y filtra datos, valida, autoriza, persiste y presenta errores. Depende directamente de `MedicamentoRepository`, `Medicamento`, `Usuario`, `Rol` y componentes visuales. |
| [`Usuario.java`](../src/model/Usuario.java) | 422 | Modelo de cuenta que además busca medicamentos, consulta stock, registra ventas y revisa vencimientos; depende de ambos repositorios, `Medicamento`, `Venta` y `DetalleVenta`. |
| [`Administrador.java`](../src/model/Administrador.java) | 62 | Subclase de `Usuario`; registra, busca, edita y elimina medicamentos caducados. Depende directamente de los repositorios y valida permisos del rol. |
| [`Encargado.java`](../src/model/Encargado.java) | 41 | Subclase de `Usuario`; sobrescribe búsqueda, consulta de stock, ventas y vencimientos para delegar en `super`. Depende de ambos repositorios. |
| [`Main.java`](../src/Main.java) | 464 | Inicia Swing y mantiene métodos públicos de consola para medicamentos, stock, ventas y vencimientos. Tiene repositorios estáticos y llama directamente a `Usuario`/`Administrador`. |
| [`MenuAdministradorView.java`](../src/view/MenuAdministradorView.java) | 411 | Construye la pantalla y crea `MedicamentoView(sesion)`; no inyecta controlador ni servicio de medicamentos. |
| [`MenuEncargadoView.java`](../src/view/MenuEncargadoView.java) | 487 | También crea `MedicamentoView(sesion)` y `VencimientoController`; ambos roles usan la misma vista de medicamentos. |
| [`VencimientoService.java`](../src/service/VencimientoService.java) | 43 | Consulta y ordena inventario para vencimientos; depende de `MedicamentoRepository` y `Clock`. Pertenece al módulo 8 según el plan. |
| [`VencimientoController.java`](../src/controller/VencimientoController.java) | 71 | Valida sesión y carga el reporte con `SwingWorker`; recibe servicio y vista, aunque su constructor simple crea un servicio por defecto. |
| [`VencimientoView.java`](../src/view/VencimientoView.java) | 246 | Presenta tabla, resumen, errores y colores según vencimiento; recibe filas, no un repositorio. |
| [`Venta.java`](../src/model/Venta.java) | 362 | Dependencia del módulo 7: valida y reduce stock, registra la venta e intenta restaurarlo si falla la escritura. Depende de ambos repositorios. |
| [`VentaRepository.java`](../src/persistence/VentaRepository.java) | 72 | Persistencia de ventas; depende de `Venta`. |
| [`VentaService.java`](../src/service/VentaService.java) / [`VentaController.java`](../src/controller/VentaController.java) | 4 / 4 | Clases vacías, fuera del alcance de 6; el plan asigna ventas al módulo 7. |
| [`VentaView.java`](../src/view/VentaView.java) | 877 | Flujo visual de venta; crea repositorios directamente y llama a `sesion.registrarVenta`. |

## 2. Flujo actual

- **Composición:** `Main.main` inicia Swing. Los menús construyen `MedicamentoView` con la sesión, pero no inyectan `MedicamentoController` ni `MedicamentoService`; ambas clases existen vacías.
- **Alta y edición Swing:** `MedicamentoView` crea un repositorio por defecto. La vista construye el diálogo, valida campos y duplicados, convierte `MM/AAAA`, construye el modelo y persiste. La protección de administrador se realiza dentro de la vista. En consola, `Main` valida y llama a `Administrador`.
- **Búsqueda:** la vista carga todo el inventario y filtra en memoria. El repositorio permite buscar por nombre, código, marca o precio exacto. `Usuario.buscarMedicamento` utiliza ese repositorio. Los criterios de Swing y consola no coinciden y los resultados de la vista pueden quedar desactualizados hasta recargar.
- **Stock:** la vista permite editar el stock como un campo del formulario. La venta sigue otra ruta: `Usuario.registrarVenta` crea una `Venta`; `Venta.registrar` vuelve a leer los medicamentos, valida vencimiento/stock y actualiza ambos archivos. Esa operación es del módulo 7 y es un punto de integración, no una operación para rediseñar en 6.
- **Vencimientos:** `Usuario.revisarVencimientos` recorre el inventario y devuelve vencidos o medicamentos dentro de 30 días. El reporte Swing usa `VencimientoService`, que ordena y calcula días restantes; su controlador ya usa `SwingWorker`. Recomiendo conservar esa separación y reservar la lógica del reporte al módulo 8.
- **EDT:** `MedicamentoView` lee y escribe archivos directamente desde la construcción/recarga y los listeners de Swing. `cargarInventario` llama a `repositorio.listar()` y guardar/eliminar llaman a persistencia sin `SwingWorker`; esas operaciones pueden bloquear la interfaz. El flujo de vencimientos sí separa su carga.

La lógica está distribuida entre la vista (validación, duplicados, CRUD), `Usuario` (búsqueda, stock y vencimientos), `Administrador` (CRUD y permisos), `Medicamento` (validación, stock y fechas) y `Venta` (actualización de stock por venta). `Medicamento` puede conservar reglas propias de la entidad —por ejemplo, impedir cantidades inválidas—, pero la vista no debería asumir persistencia/autorización ni `Usuario` orquestar repositorios de medicamentos y ventas.

### Decisiones acordadas para el módulo 6

1. **Eliminación:** preservar el comportamiento actual de Swing: el administrador puede eliminar cualquier medicamento. Registrar la diferencia con `Administrador.eliminarMedicamentoCaducado`, que rechaza medicamentos vigentes, como hallazgo; no cambiar el comportamiento de Swing para igualarlo al método del modelo.
2. **Búsqueda:** el servicio debe unificar los criterios actuales del repositorio —nombre, código, marca y precio exacto— y la vista debe mostrar los resultados que devuelve ese servicio. No introducir criterios nuevos en este cambio.
3. **Consola de `Main`:** se buscó `Main.<método>(...)` para `registrarMedicamento`, `buscarMedicamento`, `consultarStock`, `actualizarMedicamento`, `controlarVencimientos` y `eliminarMedicamentoCaducado` en `src/` y `test/`; no se encontraron llamadores. Se proponen como código muerto para posible eliminación en 6e, que requiere aprobación separada. No caracterizarlos ni conservarlos como requisito de módulo 6 sin que se decida lo contrario.
4. **Formatos y datos:** no tocar `datos/` ni el formato TXT. Cualquier cambio de formato corresponde exclusivamente al módulo 8 con aprobación explícita y respaldo previo.

## 3. Especificación de validaciones actuales del formulario

Extraída de `MedicamentoView`; será la especificación inicial de pruebas del servicio. Los mensajes de error se conservan literalmente. `línea` referencia la condición o mensaje.

| Campo | Regla actual | Mensaje de error actual | Línea |
|---|---|---|---:|
| Nombre | Se recorta con `trim`; no puede quedar vacío. | `Completa todos los campos obligatorios.` | [MedicamentoView.java:494](../src/view/MedicamentoView.java#L494), [MedicamentoView.java:501](../src/view/MedicamentoView.java#L501) |
| Código | Se recorta con `trim`; no puede quedar vacío. La duplicidad se compara sin distinguir mayúsculas y no considera duplicado el mismo código al editar. | Vacío: `Completa todos los campos obligatorios.` Duplicado: `El código <código> ya se encuentra registrado.` | [MedicamentoView.java:495](../src/view/MedicamentoView.java#L495), [MedicamentoView.java:501](../src/view/MedicamentoView.java#L501), [MedicamentoView.java:505](../src/view/MedicamentoView.java#L505), [MedicamentoView.java:510](../src/view/MedicamentoView.java#L510) |
| Categoría | Debe haber una opción seleccionada distinta de la primera opción del combo. | `Completa todos los campos obligatorios.` | [MedicamentoView.java:502](../src/view/MedicamentoView.java#L502) |
| Presentación | Se recorta con `trim`; no puede quedar vacía. | `Completa todos los campos obligatorios.` | [MedicamentoView.java:496](../src/view/MedicamentoView.java#L496), [MedicamentoView.java:500](../src/view/MedicamentoView.java#L500) |
| Laboratorio | Debe haber una opción seleccionada distinta de la primera opción del combo. | `Completa todos los campos obligatorios.` | [MedicamentoView.java:502](../src/view/MedicamentoView.java#L502) |
| Vencimiento | Debe coincidir con `MM/AAAA`, mes `01`–`12` y cuatro dígitos de año; se convierte al último día del mes. | `La fecha debe tener formato MM/AAAA, por ejemplo 12/2027.` | [MedicamentoView.java:497](../src/view/MedicamentoView.java#L497), [MedicamentoView.java:520](../src/view/MedicamentoView.java#L520), [MedicamentoView.java:547](../src/view/MedicamentoView.java#L547), [MedicamentoView.java:524](../src/view/MedicamentoView.java#L524) |
| Precio | Se parsea como `double`; se admite coma decimal al reemplazarla por punto; no puede ser negativo. El modelo también rechaza valores no finitos. | Error de parseo o negativo: `Precio o cantidad no válidos. Usa números positivos.` Un valor no finito llega a la validación del modelo y se presenta como `Error al guardar: <mensaje de excepción>`. | [MedicamentoView.java:515](../src/view/MedicamentoView.java#L515), [MedicamentoView.java:517](../src/view/MedicamentoView.java#L517), [MedicamentoView.java:545](../src/view/MedicamentoView.java#L545), [Medicamento.java:236](../src/model/Medicamento.java#L236) |
| Cantidad | Debe ser entero y no negativo. **Cero se acepta**, aunque el mensaje diga “positivos”. | `Precio o cantidad no válidos. Usa números positivos.` | [MedicamentoView.java:516](../src/view/MedicamentoView.java#L516), [MedicamentoView.java:517](../src/view/MedicamentoView.java#L517), [MedicamentoView.java:545](../src/view/MedicamentoView.java#L545) |
| Permiso para abrir/guardar/eliminar | Se requiere sesión activa, iniciada y con rol administrador; se vuelve a comprobar al guardar y eliminar. | Al abrir/eliminar: `Se requiere una sesión activa de administrador.` Al guardar, el error se envuelve como `Error al guardar: <mensaje de excepción>`. | [MedicamentoView.java:263](../src/view/MedicamentoView.java#L263), [MedicamentoView.java:493](../src/view/MedicamentoView.java#L493), [MedicamentoView.java:555](../src/view/MedicamentoView.java#L555), [MedicamentoView.java:586](../src/view/MedicamentoView.java#L586) |

La vista no solicita marca ni stock mínimo: al crear asigna `"Sin marca"` y mínimo `0`; al editar conserva ambos valores existentes ([MedicamentoView.java:527](../src/view/MedicamentoView.java#L527)).

### Hallazgos de 6a

- **Cantidad cero:** el constructor y la actualización aceptan cantidad `0`. La vista solo rechaza valores menores que cero, aunque el mensaje diga `Precio o cantidad no válidos. Usa números positivos.`. Esta conducta se conserva.
- **Precio no finito:** `Double.parseDouble` acepta valores como `NaN`; el modelo los rechaza con `El precio no es válido.` y la vista actual muestra `Error al guardar: El precio no es válido.`. Se conserva ese mensaje.
- **Duplicados:** precisión sobre el diagnóstico anterior: en el código actual la comprobación primaria recorre una lista recién obtenida mediante `repositorio.listar()`; no usa el campo cacheado `inventario` de la vista. Al registrar también se consulta `repositorio.buscarPorCodigo()`. No existe una restricción de unicidad atómica en el repositorio, por lo que la operación sigue siendo susceptible a concurrencia. No es correcto afirmar que la vista no consulta el repositorio.
- **Actualización no atómica:** `Medicamento.actualizarInfo` cambia nombre y cantidad antes de parsear la fecha; si falla, esos cambios parciales permanecen. La prueba de 6a caracteriza este comportamiento. El caso no se alcanza desde el formulario Swing actual: este valida la fecha antes de construir un `Medicamento` nuevo y no llama a `actualizarInfo`. Por decisión del usuario, se conserva y solo se documenta.
- **Eliminación de vigente:** sí existe prueba directa: `AdministradorTest.rechazaEliminarMedicamentoVigente` verifica la excepción y que el medicamento siga en el repositorio temporal. Esta prueba caracteriza el método de `Administrador`, no el borrado general de la vista Swing.

## 4. Code smells — catálogo SourceMaking

Catálogo: [SourceMaking — Code Smells](https://sourcemaking.com/refactoring/smells). Las dependencias directas vista–repositorio y el I/O en EDT se documentan como problemas de arquitectura, no se etiquetan automáticamente con un smell del catálogo.

| Smell y grupo | Evidencia verificable | Severidad | Técnica sugerida / estado |
|---|---|---|---|
| [Large Class — Bloaters](https://sourcemaking.com/refactoring/smells/large-class) | [MedicamentoView.java:60](../src/view/MedicamentoView.java#L60): `public class MedicamentoView extends JPanel`; 1092 líneas con UI, búsqueda, validación y persistencia. | Alta: múltiples responsabilidades y motivos de cambio concentrados. | **Extract Class**, alineado con el objetivo MVC de 6; concretarlo en 6c/6d. |
| [Long Method — Bloaters](https://sourcemaking.com/refactoring/smells/long-method) | [MedicamentoView.java:261](../src/view/MedicamentoView.java#L261): `abrirFormulario(...)` crea diálogo, controles, layout y listeners hasta antes del método de línea 358. | Media: mezcla construcción de formulario y acciones. | **Extract Method** y extraer el formulario como `MedicamentoFormDialog`, ahora acordado para 6d. |
| [Long Method — Bloaters](https://sourcemaking.com/refactoring/smells/long-method) | [MedicamentoView.java:487](../src/view/MedicamentoView.java#L487): `guardarMedicamento(...)` combina validaciones, búsqueda de duplicados, conversión, creación, persistencia y errores; termina antes de `eliminarPorCodigo`, línea 553. | Alta: lógica de negocio/persistencia en listener Swing y EDT. | **Move Method / Extract Method** al servicio; planificado para 6b y 6d. |
| [Long Parameter List — Bloaters](https://sourcemaking.com/refactoring/smells/long-parameter-list) | [Medicamento.java:38](../src/model/Medicamento.java#L38): constructor de 10 parámetros; [Medicamento.java:92](../src/model/Medicamento.java#L92): actualización de 9. | Media: orden de argumentos propenso a errores. | **Introduce Parameter Object** sería una opción; no está planificado ni se debe cambiar la API sin autorización. |
| [Lazy Class — Dispensables](https://sourcemaking.com/refactoring/smells/lazy-class) | [MedicamentoService.java:3](../src/service/MedicamentoService.java#L3) y [MedicamentoController.java:3](../src/controller/MedicamentoController.java#L3): solo declaran clases vacías; no se hallaron usos de producción. | Alta como brecha MVC; baja como smell aislado porque parecen esqueletos previstos. | Implementar sus roles en 6b/6c. Si se decide no mantener las capas, SourceMaking sugiere **Inline Class**; no borrarlas antes de definirlo. |
| [Dead Code — Dispensables](https://sourcemaking.com/refactoring/smells/dead-code) | [Main.java:73](../src/Main.java#L73) y métodos de consola posteriores: búsqueda en `src/` y `test/` sin llamadores `Main.<método>(...)`; `main` inicia Swing. | Baja; **candidato**, no confirmado frente a consumidores externos. | **Remove Dead Code** solo tras aprobación separada. Propuesto para 6e. |

No se confirman **Duplicate Code**, **Divergent Change**, **Feature Envy** o **Inappropriate Intimacy** como smells adicionales: hay dependencias y responsabilidades mezcladas, pero no evidencia suficiente para afirmar esos patrones del catálogo. `Usuario` y `Administrador` dependen directamente de repositorios; se registra como problema de capas en el flujo.

## 5. Matriz de pruebas existentes y faltantes

| Comportamiento | Pruebas existentes | Cobertura faltante |
|---|---|---|
| Búsqueda | `EncargadoTest` separa búsqueda por nombre, código, marca y precio. `MedicamentoRepositorySearchTest` conserva marca insensible a mayúsculas y precio exacto con coma/punto. | Sin pruebas de filtros Swing, campo vacío, sin coincidencias ni coherencia de resultados del servicio en la vista. |
| Alta | `AdministradorTest.registraMedicamento`; `MedicamentoTest` comprueba constructor/cantidad; `MedicamentoRepositoryTest` comprueba persistencia. | Sin formulario Swing, duplicados concurrentes ni fallo de escritura desde la interfaz. |
| Edición | `AdministradorTest.actualizaMedicamento` verifica el cambio y su lectura posterior. | Sin formulario Swing, errores de fecha/cantidad, elemento inexistente ni preservación de marca/mínimo. |
| Stock | [MedicamentoTest.reducirCantidadDescuentaDelInventario](../test/model/MedicamentoTest.java#L95) verifica reducción. [VentaTest](../test/model/VentaTest.java#L40) cubre venta y actualización, con casos de exceso, vencido y fallo de persistencia en líneas 143, 151 y 207. | Sin prueba de edición de stock desde la vista ni de concurrencia/stock obsoleto; la transacción de venta es del módulo 7. |
| Autorización | `AdministradorTest` verifica operación sin sesión, antes de iniciar sesión y que el rol encargado no tiene permiso de gestión; también verifica rechazo al eliminar un medicamento vigente. `EncargadoTest` comprueba consulta sin sesión. | Sin prueba visual de restricciones por rol ni de listeners, controles deshabilitados o sesión expirada durante operaciones. |
| Vencimientos | [EncargadoTest.revisaMedicamentosVencidosYProximosAVencer](../test/model/EncargadoTest.java#L59) verifica vencido/próximo/no próximo. [VentaTest](../test/model/VentaTest.java#L151) rechaza venta vencida. | Sin prueba de `VencimientoService`, controlador o vista: límites, orden, colores, vacío y errores de lectura. |
| Persistencia TXT | [MedicamentoTest.serializacionConservaLaMarcaYLeeRegistrosAntiguos](../test/model/MedicamentoTest.java#L61) cubre round-trip y registros antiguos. | No se cambia formato en 6; cualquier modificación se reserva a 8 con aprobación y respaldo. |

## 6. API de `MedicamentoService` implementada en 6b

### Datos de entrada

Propuesta de record de formulario en `service`:

```java
public record DatosMedicamento(
        String nombre,
        String codigo,
        String categoria,
        String presentacion,
        String laboratorio,
        String precio,
        String cantidad,
        String vencimiento) { }
```

Los valores de precio, cantidad y vencimiento llegan crudos como `String` para que el servicio reproduzca la conversión y los mensajes actuales. El vencimiento se valida como `MM/AAAA`, se parsea como `YearMonth` y se convierte al último día del mes. Marca y stock mínimo no son campos del formulario: al alta se mantienen `"Sin marca"` y `0`; en edición se preservan los valores existentes.

| Operación y firma propuesta | Entrada | Excepciones y mensajes que recibirá la vista | Reglas cubiertas |
|---|---|---|---|
| `List<Medicamento> listar(Usuario sesion) throws IOException` | Sesión actual; no recibe criterios. | `SecurityException("Se requiere una sesión válida para consultar medicamentos.")`; `IOException("No se pudo cargar el inventario: " + causa)`. | Sesión válida para consulta; lista vacía si no hay archivo. |
| `List<Medicamento> buscar(Usuario sesion, String consulta) throws IOException` | Texto crudo; aplica exactamente nombre, código, marca y precio exacto del repositorio. | Sesión inválida: mensaje de `listar`; vacío si no hay coincidencias. Una consulta vacía enviada directamente produce `IllegalArgumentException("Debe ingresar un texto para buscar.")`, como el repositorio. Para conservar la vista actual —campo vacío muestra todo—, la vista/controlador llama `listar` cuando la consulta queda vacía. | Criterios de búsqueda y ausencia de resultados. |
| `Optional<Medicamento> consultarStock(Usuario sesion, String codigo) throws IOException` | Código crudo. Operación auxiliar para redirigir el método actual de `Usuario`. | Sesión inválida: `SecurityException("Se requiere una sesión válida para consultar medicamentos.")`; código inexistente: `Optional.empty()`. | Consulta de stock por código. |
| `void registrar(Usuario sesion, DatosMedicamento datos) throws IOException` | Campos crudos del formulario. | Errores de campos requeridos, duplicidad, parseo numérico y fecha: excepciones de validación con los mensajes directos de la tabla (sin prefijo). Errores del modelo o de I/O: mensaje completo `Error al guardar: <causa>`. Sin administrador: `SecurityException("Error al guardar: Se requiere una sesión activa de administrador.")`, porque la vista actual aplica ese prefijo al capturar el error. | Campos requeridos, duplicados, coma decimal, número no negativo, cantidad cero aceptada y fecha `MM/AAAA`. |
| `void actualizar(Usuario sesion, String codigoOriginal, DatosMedicamento datos) throws IOException` | Código original para excluirlo del chequeo de duplicado y datos crudos nuevos. | Mensajes directos de validación iguales a `registrar`; si ya no existe: `IllegalStateException("Error al guardar: El medicamento ya no existe en el inventario.")`; autorización: `SecurityException("Error al guardar: Se requiere una sesión activa de administrador.")`. Errores del modelo/I/O con `Error al guardar: <causa>`. | Reglas de edición, preservación de marca/mínimo y errores de persistencia. Para mantener el comportamiento actual, la actualización usa el código nuevo como clave del repositorio; un cambio de código que no encuentre registro conserva el error actual. |
| `void eliminar(Usuario sesion, String codigo) throws IOException` | Código. La confirmación destructiva sigue siendo responsabilidad de la vista. | No está en la lista mostrada: `IllegalArgumentException("El medicamento seleccionado ya no está en el inventario.")`; si desaparece al persistir: `IllegalStateException("No se pudo eliminar el medicamento: El medicamento ya no existe en el inventario.")`; I/O con prefijo `No se pudo eliminar el medicamento: <causa>`. Si falla la autorización en la operación confirmada: `SecurityException("No se pudo eliminar el medicamento: Se requiere una sesión activa de administrador.")`, según el wrapper actual de Swing. | Permiso de administrador y eliminación de cualquier medicamento, vencido o vigente, como en Swing. |

Las validaciones del formulario que hoy muestran mensajes directos deben seguir entregando esos mensajes sin prefijo. Los errores posteriores del modelo, por ejemplo precio no finito, deben llegar como `Error al guardar: El precio no es válido.`. La vista solo presentará `e.getMessage()` y no duplicará reglas ni prefijos.

**Reglas que permanecen en la vista por ser de interacción:** detectar la opción inicial/no seleccionada de los combos de Categoría y Laboratorio, mostrar/ocultar controles por rol, confirmación de borrado, apertura/cierre del diálogo y presentación de errores. El servicio también valida que los valores de categoría/laboratorio recibidos no estén vacíos, usando el mensaje general de campos obligatorios.

### Repositorio y autorización

- Constructor de producción propuesto: `public MedicamentoService(MedicamentoRepository repositorio)`.
- Constructor auxiliar de prueba propuesto, package-private en `service`: `MedicamentoService(Path archivoMedicamentos)`, que crea `MedicamentoRepository` apuntando al `@TempDir`. No usar el constructor sin argumentos/default `datos/` en pruebas.
- Para preservar la autorización local actual, comprobar sesión iniciada y reutilizar `UsuarioServiceSupport.administradorActivo(sesion)` para escrituras. Lecturas requieren sesión iniciada, activa y rol administrador o encargado. No agregar una nueva dependencia de `UsuarioRepository` en 6b: la vista y los métodos actuales autorizan con el objeto de sesión, no revalidan su estado contra el TXT de usuarios.
- El borrado general del servicio no restringe vencimiento. `Administrador.eliminarMedicamentoCaducado` conserva su guarda de medicamento vigente antes de delegar, por lo que la discrepancia con Swing no se altera.
- Para redirigir los métodos actuales de `Usuario`/`Administrador` sin romper sus firmas en 6b-2, el servicio puede ofrecer overloads para recibir un `Medicamento` ya construido; el formulario usará `DatosMedicamento`.

## 7. Ejecución de 6b

La suite de 181 pruebas es la red de seguridad. No implementar hasta la aprobación de esta API.

| Subpaso | Objetivo y archivos | Pruebas primero / exclusiones |
|---|---|---|
| **6b-1 — Servicio y pruebas directas — completado** | Se implementaron `src/service/MedicamentoService.java` y `src/service/DatosMedicamento.java`, con repositorio inyectado y constructor `Path` para pruebas. | Se añadieron 21 pruebas directas con `@TempDir`; la suite completa pasó con 202 pruebas (181 existentes + 21 nuevas). No se modificaron los modelos en esta fase. |
| **6b-2 — Delegación desde modelos — completado** | Búsqueda/stock de `Usuario` y `Encargado` y operaciones de medicamentos de `Administrador` delegan al servicio; se conservaron firmas y visibilidad. | Suite completa: 202 pruebas, 0 fallos/errores/omitidas. No se modificó `Main`, las vistas, ventas ni los reportes de vencimientos. No se tocaron formatos TXT ni `datos/`. |
| **6c — Controlador e inyección** | Completar `MedicamentoController` e inyectar servicio/controlador desde ambos menús. | Mantener sin cambios diseño y navegación; ninguna vista/controlador crea repositorios. |
| **6d — Vista delgada, formulario y concurrencia** | Extraer `MedicamentoFormDialog`; delegar operaciones y ejecutar todo I/O fuera del EDT mediante `SwingWorker`. | Mantener mensajes/reglas y borrado Swing; prueba manual del usuario al final. |
| **6e — Limpieza de consola** | Propuesta de eliminación de los métodos de consola de medicamentos sin llamadores. | Requiere aprobación separada; no incluye métodos de venta. |

El comportamiento de eliminación acordado es el de Swing: **el administrador puede eliminar cualquier medicamento**. `Administrador.eliminarMedicamentoCaducado` solo permite eliminar vencidos; registrar esta diferencia como hallazgo, no corregirla en 6.

## 8. Riesgos y prueba manual

Las vistas y sus eventos Swing carecen de pruebas. Cuando se autorice, la prueba manual del usuario debería cubrir:

1. Login de administrador y encargado; permisos visibles y efectivos.
2. Carga/recarga, búsqueda por nombre, código, marca y precio exacto, sin coincidencias y limpiar búsqueda.
3. Alta válida; campos obligatorios; código duplicado con distinta capitalización; precio/cantidad inválidos; precio con coma; fechas `MM/AAAA`, mes límite e invalidaciones.
4. Edición y persistencia tras recargar; conservación de marca y stock mínimo.
5. Eliminación confirmada y cancelada; confirmar que administrador puede borrar tanto vencidos como vigentes.
6. Encargado con consulta solamente, sin acciones de alta, edición o eliminación.
7. Venta válida, stock insuficiente y medicamento vencido; consistencia de stock/venta (módulo 7).
8. Vencimientos: inventario vacío, vencido, próximo y vigente; orden, límites, colores y actualizar (módulo 8).
9. Respuesta de la UI durante lecturas/escrituras para asegurar que el EDT no se congela.

## 9. Línea base

Al completar 6a y verificar antes de 6b, `mvn test` pasó con **181 pruebas, 0 fallos, 0 errores y 0 omitidas**. Tras 6b, la suite pasó con **202 pruebas, 0 fallos, 0 errores y 0 omitidas**: la diferencia son las 21 pruebas directas de `MedicamentoServiceTest`. No se modificó `datos/` ni los formatos TXT.

6b está completado. No se inicia 6c ni otro módulo.

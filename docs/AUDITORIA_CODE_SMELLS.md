# Auditoria de Code Smells - Sistema de Gestión de Farmacia

**Autor:** Kilo (Arquitecto de Software Senior)  
**Estándar:** Clean Code (Martin Fowler) + taxonomía SourceMaking (5 categorías)  
**Estado:** Solo inspeccion. Sin cambios aplicados al código.  
**Fecha:** 2026-11-24

---

## 1. Resumen del Diagnóstico

El proyecto tiene una arquitectura MVC por capas (model / controller / service / persistence / view) y una composición de dependencias en `Main`. En general el código compila, usa inyección de dependencias básica, separa persistencia TXT y aplica validación de entradas y seguridad (PBKDF2, sesiones, roles, permisos).

No obstante, hay **varios code smells concentrados en pocas clases grandes**. Los problemas mas críticos son:

- **Clases grandes (Large Class)** que concentran UI, negocio y persistencia: `VentaView` (877 l.), `MedicamentoView` (1248 l.), `LoginView` (592 l.), `Usuario` (524 l.), `MedicamentoService` (363 l.), `Venta` (362 l.), `Medicamento` (362 l.), `MenuAdministradorView` (486 l.), `LoginController` (257 l.), `GestionUsuariosService` (211 l.), `RecuperacionService` (192 l.).
- **Código muerto (Dead Code)**: campos, métodos y clases internas definidos pero nunca usados (ej. `MedicamentoView.ROJO`, `IconoMedicamento`, `botonIcono`, `Validaciones.formatearTexto`).
- **Duplicación de código**: `MedicamentoService.registrar` / `actualizar` comparten casi el mismo flujo; `GestionUsuariosService` repite la invariable del "ultimo administrador activo"; `Venta` duplica logica de rollback de stock en `registrar` y `cancelar`.
- **Acoplamiento indebido (Inappropriate Intimacy / Feature Envy)**: controladores llaman metodos estaticos de `LoginView`; `VentaView` conoce internos de `Venta`, `Medicamento`, `DetalleVenta`; `Usuario` conoce `MedicamentoRepository` y `VentaRepository`.
- **Secretos hardcodeados**: `AutenticacionService.CLAVE_INICIAL = "Farmacia2026!"`.
- **Primitiva obsesion**: abuso de `String`, `int`, `double`, `BigDecimal` para conceptos de dominio (precios, fechas, codigos).

Estos smells no impiden la compilacion ni el funcionamiento, pero dificultan el mantenimiento, la prueba unitaria y la evolucion del sistema.

---

## 2. Listado de Code Smells Detectados

Se presenta cada smell con categoria, ubicacion, explicacion y tecnica recomendada. Los archivos estan referenciados por ruta relativa a la raiz del proyecto.

### 2.1 Bloaters (Hinchados)

| # | Smell | Ubicacion | Explicacion | Tecnica recomendada |
|---|-------|-----------|-------------|---------------------|
| B1 | **Large Class** | `src/view/VentaView.java` (877 l.) | Gestiona UI del punto de venta, carrito, validacion de stock, registro de venta, generacion de comprobante e imprimir. Demasiadas responsabilidades en una sola vista. | Extract Class: separar `Carrito`, `ComprobanteService`, `VentaPresenter`; Extract Method para metodos largos (`registrarVenta`, `buscarMedicamento`). |
| B2 | **Large Class** | `src/view/MedicamentoView.java` (1248 l.) | Vista mas grande del proyecto. Contiene tabla, formulario, busqueda, dialogos de edicion, iconos internos, renderizadores y validacion de fechas. | Extract Class: separar `MedicamentoTableModel`, `MedicamentoFormDialog` (ya existe pero esta poco usado), `MedicamentoPresenter`. |
| B3 | **Large Class** | `src/view/LoginView.java` (592 l.) | Login + dibujo vectorial de marca + metodos estaticos de utilidad UI (`mostrarAlertaError`, `mostrarConfirmacion`, `TonoDialogo`) usados por controladores. | Extract Class para utilidades de dialogo; mover logica de dibujo a `EstiloSistema`; quitar metodos estaticos de UI de la vista. |
| B4 | **Large Class** | `src/model/Usuario.java` (524 l.) | Entidad base que hace criptografia (PBKDF2), serializacion TXT, gestion de sesion, recuperacion, validacion de identidad, busqueda de medicamentos, registro de ventas y revision de vencimientos. Viola SRP. | Extract Class: `UsuarioCredencial` (criptografia), `UsuarioSesion` (estado de sesion), `UsuarioRecuperacion` (respuestas y bloqueo), `UsuarioSerializer` (aLinea/desdeLinea). |
| B5 | **Large Class** | `src/model/Medicamento.java` (362 l.) | Entidad con 3 constructores encadenados, 3 sobrecargas de `actualizarInfo`, validacion repetida, `mostrarDatos` (consola) y serializacion. | Extract Method para validacion; unificar constructores con Builder o factory; quitar `mostrarDatos` (responsabilidad de vista). |
| B6 | **Large Class** | `src/model/Venta.java` (362 l.) | Logica de negocio, transaccion manual con rollback (suppressed exceptions), serializacion V1/V2, calculo de totales y consultas a repositorios. | Extract Class: `VentaRegistro` (persistencia), `VentaCalculo` (totales), `VentaSerializer` (desdeLinea/aLinea). |
| B7 | **Large Class** | `src/service/MedicamentoService.java` (363 l.) | Autorizacion, validacion de formulario, normalizacion, creacion de entidad y coordinacion con repositorio. | Extract Class: `MedicamentoValidador`, `MedicamentoFactory`; Extract Method para flujo comun registrar/actualizar. |
| B8 | **Large Class** | `src/controller/LoginController.java` (257 l.) | Autenticacion, SwingWorker, recuperacion, configuracion de preguntas y apertura de menus por rol. Mezcla UI e negocio. | Extract Class: `RecuperacionFlow`, `MenuFactory`; Extract Method para `ingresar` y `recuperar`. |
| B9 | **Large Class** | `src/service/GestionUsuariosService.java` (211 l.) | CRUD de usuarios, validaciones, invariables de administrador, cambio de clave propia. | Extract Method para `validarAdministrador` y `validarUltimoAdminActivo`. |
| B10 | **Large Class** | `src/service/RecuperacionService.java` (192 l.) | Configuracion, verificacion, bloqueo por intentos, restablecimiento y sincronizacion con repositorio. | Extract Class: `RecuperacionIntentos` (bloqueo), `RecuperacionValidador`. |
| B11 | **Long Method** | `src/model/Usuario.java:desdeLinea` (líneas 330-379) | Metodo de 50 lineas con parsing, validacion de longitudes, decode Base64, asignacion de campos y manejo de ID. | Extract Method: `parsearCabecera`, `validarCamposRecuperacion`, `asignarId`. |
| B12 | **Long Method** | `src/model/Venta.java:registrar` (líneas 51-125) | Valida, busca medicamentos, reduce stock, persiste, hace rollback manual con `addSuppressed`. | Extract Method: `validarDetalles`, `reducirStockConRollback`, `persistirVenta`. |
| B13 | **Long Parameter List** | `src/model/Medicamento.java` constructores (hasta 10 parametros) | Constructores con 6, 8 y 10 parametros de tipos primitivos y String. Difíciles de leer y propensos a intercambio de argumentos. | Introduce Parameter Object (ya existe `DatosMedicamento`) o Builder. |
| B14 | **Long Parameter List** | `src/service/UsuarioService.java:guardar` (7 parametros) | `sesion, id, nombre, usuario, rol, clave, confirmacion`. | Parameter Object `DatosUsuario`. |
| B15 | **Primitive Obsession** | Varias clases (`Usuario`, `Medicamento`, `Venta`, `VentaView`) | Uso extensivo de `String`, `int`, `double`, `BigDecimal` para concepto de dominio (precio, stock, fecha, codigo, usuario) sin tipos de valor. | Introduce Value Objects: `Precio`, `CodigoMedicamento`, `Cantidad`, `FechaVencimiento`. |
| B16 | **Data Clumps** | `src/model/Usuario.java` | Campos `sal`, `contraseña`, `salColor`, `respuestaColor`, `salComida`, `respuestaComida`, `intentosRecuperacion`, `recuperacionBloqueadaHasta` siempre viajan juntos. | Extract Class: `Credenciales`, `DatosRecuperacion`. |

### 2.2 Object-Orientation Abusers (Abusadores POO)

| # | Smell | Ubicacion | Explicacion | Tecnica recomendada |
|---|-------|-----------|-------------|---------------------|
| O1 | **Switch Statements** | `src/model/Usuario.java:crearDesdeHash` (líneas 94-103) | Switch por nombre de rol para instanciar `Administrador` o `Encargado`. | Replace Conditional with Polymorphism: factory por rol o `Rol.crearUsuario(...)`. |
| O2 | **Switch Statements** | `src/model/Rol.java:porNombre` (líneas 59-65) | Switch por nombre para devolver instancia de rol. | Enum o Map estatico; polimorfismo. |
| O3 | **Temporary Field** | `src/controller/LoginController.java` | `boolean[] guardado = {false}` y `PermisoRecuperacion[] permiso = {null}` se usan como hack para capturar variables en lambdas internas. | Replace Temporary Field with Query o extraer a clase interna `RecuperacionEstado`. |
| O4 | **Refused Bequest** | `src/view/VentaView.java` (línea 125, 820) | `sesion instanceof Administrador` para mostrar historial. `VentaView` depende de tipo concreto. | Polimorfismo: `sesion.puedeConsultarHistorial()` en `Usuario`. |
| O5 | **Refused Bequest** | `src/service/ReporteVentasService.java:consultar` (línea 42) | `sesion instanceof Administrador` para autorizar. | Polimorfismo en `Usuario` o `Rol`. |
| O6 | **Alternative Classes with Different Interfaces** | `src/controller/VencimientoController.java` | Dos constructores: uno por defecto con `new VencimientoService()` y otro con inyeccion. Genera dos caminos de creacion. | Quitar constructor por defecto; exigir inyeccion (o usar factory). |
| O7 | **Alternative Classes with Different Interfaces** | `src/model/Venta.java` | Dos constructores con semantica distinta (`Usuario` vs `codigo+cantidad`). | Factory metodos con nombre (`Venta.nueva(usuario)`, `Venta.resumen(codigo, cantidad)`). |

### 2.3 Change Preventers (Impedimentos al Cambio)

| # | Smell | Ubicacion | Explicacion | Tecnica recomendada |
|---|-------|-----------|-------------|---------------------|
| C1 | **Divergent Change** | `src/Main.java` | Clase unica que hace composicion de dependencias, consola (Scanner), seguridad (`permitirSesion`, `permitirAdministrador`) y lanzamiento Swing. Cambiar UI o consola obliga a tocar `Main`. | Extract Class: `AppComposition` (dependencias), `ConsolaApp`, `SwingApp`. |
| C2 | **Shotgun Surgery** | `src/service/GestionUsuariosService.java`, `src/model/Rol.java`, `src/model/Permiso.java` | Cambiar un rol o permiso toca `Rol`, `Permiso`, `GestionUsuariosService.validarDatos`, `Administrador.validarPermiso`, `MedicamentoService.validarPermisoGestionMedicamentos`. | Centralizar autorizacion en un servicio `AutorizacionService` o `PermissionEvaluator`. |
| C3 | **Shotgun Surgery** | `src/model/Usuario.java` | Cambiar formato de archivo de usuarios toca `Usuario.aLinea`, `Usuario.desdeLinea`, `UsuarioRepository`, y posiblemente `MedicamentoService`. | Extract Class: `UsuarioSerializer`; repositorio solo llama al serializer. |
| C4 | **Parallel Inheritance Hierarchies** | `src/model/Administrador.java`, `src/model/Encargado.java` | Ambas heredan de `Usuario` pero replican validacion de sesion y delegacion a `MedicamentoService`. Si se añade un tercer rol, hay que replicar en tres sitios. | Push Down / Template Method en `Usuario`; evitar subclases por rol si solo difieren en permisos (usar `Rol`). |

### 2.4 Dispensables (Prescindibles)

| # | Smell | Ubicacion | Explicacion | Tecnica recomendada |
|---|-------|-----------|-------------|---------------------|
| D1 | **Dead Code** | `src/view/MedicamentoView.java` | Campos `ROJO` (línea 68), `AccionesEditor.editarAccion`/`eliminarAccion` (544-545), metodo `botonIcono` (607), clases internas `IconoMedicamento` (723), `IconoAlerta` (757), `IconoAdvertencia` (792) y constructores nunca usados. | Eliminar codigo muerto. |
| D2 | **Dead Code** | `src/util/Validaciones.java` | `formatearTexto` definido pero prácticamente sin uso real en produccion (solo 1 referencia dubiosa). | Eliminar o mover a `EstiloSistema` si se usa. |
| D3 | **Duplicate Code** | `src/service/MedicamentoService.java` | `registrar` (líneas 108-125) y `actualizar` (líneas 138-158) comparten: validar sesion, normalizar campos, validar codigo unico, validar valores, crear medicamento, try/catch con repositorio. | Extract Method: `guardarMedicamento(sesion, datos, codigoOriginal)` con bandera de alta/edicion. |
| D4 | **Duplicate Code** | `src/service/GestionUsuariosService.java` | Invariable "debe quedar al menos un administrador activo" repetida en `actualizarUsuario` (líneas 105-108) y `cambiarEstadoUsuario` (líneas 147-150). | Extract Method privado `validarUltimoAdministradorActivo(cuentas, id, rol, activo)`. |
| D5 | **Duplicate Code** | `src/model/Venta.java` | Logica de reduccion/reposicion de stock con try/catch, rollback y `addSuppressed` duplicada en `registrar` (líneas 96-124) y `cancelar` (líneas 180-206). | Extract Class: `StockManager` o `TransaccionStock`. |
| D6 | **Duplicate Code** | `src/view/VentaView.java` | `dinero`, `totalCarrito`, `leerEfectivo`, `calcularCambio` logica de dinero repetida y mezclada con UI. | Extract Class: `MoneyFormatter`, `CalculoVenta`. |
| D7 | **Lazy Class** | `src/util/Validaciones.java` | Clase con un unico metodo de utilidad poco usado. | Eliminar o fusionar con `EstiloSistema`. |
| D8 | **Lazy Class** | `src/persistence/ArchivoTXT.java` *(ya eliminado)* | Clase vacia marcador legacy. Eliminada en revision previa. | — |
| D9 | **Lazy Class** | `src/controller/VentaController.java` *(ya eliminado)* | Clase vacia sin referencias. Eliminada en revision previa. | — |
| D10 | **Lazy Class** | `src/service/VentaService.java` *(ya eliminado)* | Clase vacia sin referencias. Eliminada en revision previa. | — |
| D11 | **Data Class** | `src/model/FichaUsuario.java`, `src/service/DatosMedicamento.java`, `src/model/DetalleVenta.java`, `src/model/Permiso.java`, `src/model/PermisoRecuperacion.java` | Clases que solo contienen campos y getters sin logica. No es necesariamente un problema si son DTO/Value Object, pero indican modelo anemico en partes. | Acceptable como DTO; mover logica relacionada a servicios si crecen. |
| D12 | **Comments** | Varios | Comentarios que justifican codigo complejo o explican lo obvio (ej. `// El trabajo criptografico y de archivo va fuera del EDT`). No es grave, pero indica codigo que podria ser mas expresivo por si mismo. | Renombrar metodos/variables para que el comentario sobrese. |

### 2.5 Couplers (Acopladores)

| # | Smell | Ubicacion | Explicacion | Tecnica recomendada |
|---|-------|-----------|-------------|---------------------|
| K1 | **Feature Envy** | `src/model/Usuario.java:registrarVenta`, `buscarMedicamento`, `consultarStock`, `revisarVencimientos` | `Usuario` conoce `MedicamentoRepository`, `VentaRepository`, `MedicamentoService` y construye `Venta`/`DetalleVenta`. Pertenece a servicio o controlador, no a entidad de cuenta. | Move Method a `VentaService` / `MedicamentoService`; `Usuario` solo expone identidad y permiso. |
| K2 | **Feature Envy** | `src/view/VentaView.java` | Accede intensivamente a `item.medicamento.getPrecio()`, `detalle.getMedicamento().getNombre()`, `venta.calcularTotal()`. | Move Method a `Venta` o `DetalleVenta` (`subtotal`, `totalConDetalles`); view solo pide datos ya calculados. |
| K3 | **Feature Envy** | `src/service/ReporteVentasService.java` | Recorre `venta.obtenerDetalle()` y accede a `detalle.getMedicamento().getCodigo()`. | Move Method: `venta.resumenDetalles()` o `DetalleVenta` exponga datos. |
| K4 | **Inappropriate Intimacy** | `src/controller/LoginController.java`, `src/view/VentaView.java`, `src/controller/UsuarioController.java` | Controladores llaman `LoginView.mostrarAlertaError(...)`, `LoginView.mostrarConfirmacion(...)`, `LoginView.TonoDialogo` (metodos estaticos de UI). Los controladores conocen detalles internos de la vista. | Los metodos de UI deben ir en la vista (`vista.mostrarError`, `vista.preguntarConfirmacion`) y no ser estaticos en `LoginView`. |
| K5 | **Inappropriate Intimacy** | `src/main/Main.java` | Accede a campos privados de `MedicamentoController` y construye todo el grafo de dependencias acoplado a implementaciones concretas (`new MedicamentoRepository()`, `new VentaRepository()`). | Factory o `DependencyInjector` ligero; `Main` solo ensambla, no conoce implementaciones concretas profundas. |
| K6 | **Message Chains** | `src/view/VentaView.java` | `item.medicamento.getPrecio()`, `detalle.getMedicamento().getNombre()`, `venta.getUsuarioRegistrado()`. | Hide Delegate: `Venta.getDetallesResumidos()`, `DetalleVenta.getNombreMedicamento()`. |
| K7 | **Middle Man** | `src/controller/MedicamentoController.java`, `src/service/UsuarioService.java`, `src/controller/SeguridadCuentaController.java` | Controladores y servicios que solo delegan al siguiente capa sin logica propia. | Aceptable en MVC, pero revisar si aportan valor (validacion, transformacion de DTO). Si no, eliminar o fusionar. |
| K8 | **Hardcoded Secret** (seguridad) | `src/service/AutenticacionService.java` líneas 17-18 | `CLAVE_INICIAL = "Farmacia2026!"` en texto plano en codigo. | Variable de entorno o archivo de configuracion protegido; no hardcodear credenciales. |

---

## 3. Plan de Refactorización Sugerido (orden de prioridad)

1. **Eliminar Dead Code** (`MedicamentoView` campos/metodos/clases internas no usados; `Validaciones` si no se usa). Riesgo bajo, impacto inmediato en las alertas amarillas de VS Code.
2. **Extraer utilidades de UI de `LoginView`** (`mostrarAlertaError`, `mostrarConfirmacion`, `TonoDialogo`) a una clase de dialogos o a la propia vista instancia. Corrige Inappropriate Intimacy con controladores.
3. **Unificar flujos duplicados** en `MedicamentoService` (`registrar`/`actualizar`) y `GestionUsuariosService` (validacion ultimo admin).
4. **Extraer `UsuarioSerializer`** de `Usuario` para quitar serializacion TXT de la entidad.
5. **Extraer `StockManager` / transaccion de stock** de `Venta` para eliminar duplicacion `registrar`/`cancelar`.
6. **Reemplazar `instanceof Administrador`** por polimorfismo en `Usuario`/`Rol` (ReporteVentasService, VentaView).
7. **Extraer Value Objects** (`Precio`, `CodigoMedicamento`) para reducir Primitive Obsession.
8. **Revisar secreto hardcodeado** en `AutenticacionService.CLAVE_INICIAL`.

---

## 4. Archivos relacionados creados

- `docs/AUDITORIA_CODE_SMELLS.md` (este archivo)
- `docs/DOCUMENTACION.md` (documentacion general de estructura, ejecucion e historial Git)

**No se ha modificado ningun archivo de codigo fuente.** Cuando confirme, puedo aplicar los cambios de limpieza (eliminar dead code, unificar duplicados, extraer metodos) y/o subir a GitHub.

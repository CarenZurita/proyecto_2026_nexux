# Plan de refactorización de Salud Vida

## Estado y alcance

No se encontró un plan aprobado guardado en el workspace ni en los artefactos de la sesión. Este documento reconstruye el plan a partir de las decisiones del usuario ya registradas y del código actual. El plan actualizado fue aprobado por el usuario el 2026-11-22 y 2d-1 está autorizado. Los módulos 6, 7 y 8 se identifican a partir de los módulos del proyecto y quedan sujetos a aprobación antes de ejecutarse.

Restricciones transversales:

- Preservar el comportamiento y las reglas de negocio actuales; no alterar archivos de `datos/`. Los formatos TXT solo podrán modificarse en el módulo 8, con aprobación explícita y respaldo previo.
- No cambiar seguridad de cuentas: PBKDF2, sal, iteraciones, bloqueo, expiración ni uso único de permisos.
- Mantener I/O, lectura/escritura de archivos y PBKDF2 fuera del EDT mediante `SwingWorker`.
- Conservar UTF-8 y CRLF. No ampliar la visibilidad de `Usuario` sin aprobación expresa.
- Ejecutar primero pruebas de caracterización contra producción sin cambios; mover producción solo con la suite verde.
- Verificar las referencias antes de eliminar clases o métodos. No ejecutar ni visualizar la aplicación Swing durante esta refactorización.

## Subpaso 2d-1 — Record y listado de usuarios

### Objetivo

Mover `FichaUsuario` a `model.FichaUsuario` e incorporar el listado a `GestionUsuariosService`. No cambiar la inyección, composición, controladores ni vistas. Mantener el facade `GestorUsuarios` y sus APIs durante este subpaso.

### Archivos previstos

**Nuevos**

- `src/model/FichaUsuario.java`: record público de transferencia, trasladado desde `GestorUsuarios.FichaUsuario`; sin secretos ni datos internos.

**Modificar**

- `src/model/GestorUsuarios.java`: mantener el facade, delegar el listado y adaptar el tipo de retorno al record `model.FichaUsuario`; conservar el mecanismo de configuración/restauración del repositorio mientras exista el facade.
- `src/service/GestionUsuariosService.java`: incorporar el listado de usuarios con validación y sincronización sobre el repositorio.
- `src/view/UsuarioView.java` y `src/view/UsuarioFormDialog.java`: cambiar únicamente el import a `model.FichaUsuario`; no modificar lógica, diseño ni validaciones.
- `src/service/UsuarioService.java` y `src/controller/UsuarioController.java` (referencias adicionales a las previstas): cambiar únicamente el import/tipo de `GestorUsuarios.FichaUsuario` a `model.FichaUsuario`; no modificar nada más en estos archivos.
- `test/model/GestorUsuariosRepositoryTest.java`: cambiar únicamente las referencias de tipo al nuevo `model.FichaUsuario`.
- `test/model/GestorUsuariosCharacterizationTest.java`: añadir las pruebas de caracterización del listado, sin cambios de producción en la fase test-first.

### Orden de trabajo y verificación

1. Añadir a las pruebas de caracterización del facade casos independientes de listado válido —incluyendo los campos públicos del record— y rechazos con sesión ausente, rol no administrador y sesión desactualizada.
2. Ejecutar `mvn test` contra producción sin cambios. Si falla, corregir las pruebas para reflejar el comportamiento actual; no tocar producción.
3. Solo con la suite verde, extraer `FichaUsuario`, actualizar las referencias e incorporar el listado a `GestionUsuariosService`, manteniendo mensajes, validaciones y sincronización.
4. Ejecutar `mvn test` completo y comprobar que la inyección permanece sin cambios, que `GestorUsuarios` conserva su API salvo el traslado aprobado del tipo `FichaUsuario`, y que no hubo cambios en `datos/`.
5. Informar el desglose de pruebas, SHA-256, `git status --short` y `git diff --stat`, explicando archivos sin seguimiento.

**DETENTE al terminar 2d-1.** No iniciar 2d-2 sin aprobación expresa.

## Subpaso 2d-2 — Inyección y composición

### Objetivo

Componer los servicios de usuarios una sola vez en `Main.main`, inyectarlos por controladores y eliminar las llamadas directas de las vistas/controladores al facade estático `GestorUsuarios`. Mantener el facade y sus APIs durante 2d-2. No rediseñar la interfaz.

### Archivos previstos

**Nuevos**

- `src/controller/SeguridadCuentaController.java`: mediador entre el diálogo de seguridad y los servicios de gestión/recuperación.

**Modificar**

- `src/Main.java`: crear un `UsuarioRepository` y las instancias compartidas de `AutenticacionService`, `RecuperacionService`, `GestionUsuariosService` y los adaptadores/controladores necesarios; construir la UI en el EDT e inyectar dependencias.
- `src/model/GestorUsuarios.java`: conservar el mecanismo de configuración/restauración del repositorio mientras exista el facade.
- `src/service/UsuarioService.java`: recibir la instancia de `GestionUsuariosService` y delegar en ella, sin invocar el facade.
- `src/controller/LoginController.java`: recibir servicios por constructor; migrar autenticación y recuperación al servicio correspondiente; propagar las dependencias al menú mediante callbacks que preserven la sesión de login.
- `src/controller/UsuarioController.java`: recibir `UsuarioService` por constructor y dejar de instanciarlo internamente; conservar `SwingWorker`.
- `src/view/MenuAdministradorView.java` y `src/view/MenuEncargadoView.java`: recibir solo controladores/servicios de aplicación necesarios, no repositorios; pasar el controlador de seguridad a `ConfiguracionView`.
- `src/view/ConfiguracionView.java` y `src/view/SeguridadCuentaDialog.java`: delegar las operaciones en `SeguridadCuentaController`; eliminar llamadas estáticas a `GestorUsuarios`, sin cambios visuales.
- Pruebas de servicios y facade: conservar la cobertura y actualizar únicamente los consumidores afectados por la inyección.

### Puntos de construcción y cadena de inyección

1. `Main.main` crea el repositorio único y los servicios; no se crean repositorios desde vistas ni controladores.
2. `Main.main` crea/invoca `LoginController` con autenticación, recuperación, gestión de usuarios y mediador de seguridad.
3. `LoginController` construye `LoginView` y, al autenticar, pasa los servicios y el callback de reingreso a `MenuAdministradorView` o `MenuEncargadoView`.
4. El menú administrador construye `UsuarioView`, `UsuarioController` y `ConfiguracionView`; el controlador de usuarios recibe `UsuarioService`, que recibe `GestionUsuariosService`.
5. Ambos menús pasan el controlador de seguridad a `ConfiguracionView`; esta crea `SeguridadCuentaDialog` con el controlador, no con repositorios ni con `GestorUsuarios`.
6. El callback para volver al login debe cerrar sobre las mismas dependencias de `Main`; no debe volver a un método estático sin inyección.

### Orden de trabajo y verificación

1. Añadir pruebas temporales de la nueva ruta de inyección/mediación que no requieran lanzar la aplicación ni acceder a `datos/`.
2. Ejecutar `mvn test` contra producción sin cambios. Si falla, detener la refactorización de producción y corregir primero las pruebas.
3. Solo con la suite verde, modificar servicios, controladores, menús y vistas para inyección; preservar callbacks, navegación y operaciones asíncronas.
4. Componer en `Main.main`; confirmar por búsqueda que vistas/controladores ya no llaman `GestorUsuarios` directamente ni crean repositorios.
5. Ejecutar `mvn test`, revisar CRLF/UTF-8, hashes y estado del worktree. Explicar el conteo del diffstat y los archivos sin seguimiento.

### Prueba manual

Al recibir autorización y abrir la aplicación, probar:

- Inicio de sesión de administrador.
- Inicio de sesión de encargado.
- Menú de usuarios: listar, registrar, actualizar y cambiar estado.
- Cambio de mi clave.
- Configuración de preguntas de recuperación.
- Recuperación de contraseña.
- Cierre de sesión.

El usuario realiza esta prueba manual. Copilot no ejecuta ni abre la aplicación Swing.

**DETENTE al terminar 2d-2.** No iniciar 2e ni el módulo 6 sin aprobación expresa.

### Fuera de alcance de 2d

- No tocar `datos/`, archivos TXT/backup ni sus formatos.
- No tocar reglas de autenticación/recuperación, medicamentos, ventas, reportes ni sus repositorios/servicios.
- No cambiar aspecto, componentes visuales, validaciones de formularios ni navegación visible.
- No eliminar `GestorUsuarios`, código de consola, métodos públicos ni servicios hasta el paso que lo autorice.

## Subpaso 2e — Limpieza, con aprobación expresa

El subpaso 2e-A se completó con aprobación del usuario el 2026-11-22: se eliminó únicamente el código de consola de gestión de usuarios sin referencias.

El subpaso 2e-B se completó con aprobación del usuario el 2026-11-22:

1. Buscar y listar todas las referencias a métodos de consola, `GestorUsuarios` y sus tipos anidados, separando producción y pruebas.
2. Confirmar que ninguna vista/controlador depende del facade y que las pruebas del facade se mantienen mientras este exista.
3. Las aprobaciones para eliminar el código de consola y retirar el facade se solicitaron por separado; ambas fueron concedidas.
4. Se eliminó `src/model/GestorUsuarios.java` y se corrigió el comentario obsoleto de `src/model/Usuario.java`. No se amplió la visibilidad de métodos.
5. Las pruebas `GestorUsuariosCharacterizationTest` y `GestorUsuariosRepositoryTest` se migraron a `UsuariosCharacterizationTest` y `UsuarioServicesRepositoryTest`. Las pruebas crean su propio `UsuarioRepository` temporal y usan directamente `AutenticacionService`, `GestionUsuariosService` y `RecuperacionService`. Las dependencias restantes de `GestionUsuariosServiceTest` también se migraron a servicios directos.
6. La cobertura de comportamiento se conserva en las pruebas de servicios. Se dejan de probar deliberadamente la delegación estática del facade y el adaptador anidado `GestorUsuarios.Recuperacion`; no eran comportamiento de negocio.
7. La suite pasó con 146 pruebas antes y después de retirar el facade. No se inició el módulo 6.

## Módulo 6 — Medicamentos (borrador, sujeto a aprobación antes de ejecutar)

Diagnóstico completo: [`docs/modulo-6-medicamentos.md`](./modulo-6-medicamentos.md). El objetivo es implementar MVC y separar la lógica de negocio de la interfaz para medicamentos e inventario, sin tocar `datos/` ni formatos TXT.

Decisiones registradas para revisión:

- Preservar el borrado Swing: un administrador puede eliminar cualquier medicamento. La diferencia con `Administrador.eliminarMedicamentoCaducado`, que rechaza medicamentos vigentes, se conserva como hallazgo.
- Unificar en `MedicamentoService` la búsqueda actual del repositorio —nombre, código, marca y precio exacto— y hacer que la vista use el resultado del servicio.
- No se encontraron llamadores `Main.<método>(...)` para los métodos de consola de medicamentos. Proponer su eliminación como código muerto en 6e, con aprobación separada; no caracterizarlos como requisito.
- Extraer las validaciones del formulario a pruebas del servicio. Mantener mensajes y permitir cero en precio/cantidad tal como hoy; la discrepancia del mensaje “positivos” para cantidad cero queda documentada.

Subpasos propuestos, cada uno condicionado a pruebas verdes:

1. **6a — Caracterización:** solo pruebas con `@TempDir`; caracterizar búsqueda, alta, edición, stock, permisos y eliminación contra la producción actual. No modificar código. Las pruebas de 6a no se duplican al iniciar 6b.
2. **6b — Servicio:** completar `MedicamentoService`, extraer validación y operaciones de negocio de `Usuario`/`Administrador`/`Encargado`; no mover ventas ni reportes de vencimientos.
3. **6c — Controlador e inyección:** completar `MedicamentoController` e inyectar servicio/controlador desde ambos menús, sin repositorios en vistas/controladores.
4. **6d — Vista delgada:** extraer `MedicamentoFormDialog`, mantener la presentación y delegar operaciones; todo I/O debe ejecutarse fuera del EDT mediante `SwingWorker`.
5. **6e — Limpieza de consola:** retirar únicamente los métodos de consola de medicamentos de `Main` tras aprobación separada; no incluye métodos de venta.

**No aprobado todavía:** no iniciar 6a ni producción hasta recibir aprobación expresa de la matriz y de los subpasos.

## Módulo 7 — Ventas (borrador, sujeto a aprobación antes de ejecutar)

Objetivo previsto: separar carrito, validaciones, registro de ventas y actualización del inventario. Revisar `Venta`, `DetalleVenta`, `VentaRepository`, `VentaService`, `VentaController`, `VentaView`, `Usuario` y `Main`. Preservar cálculos, stock, manejo de errores y formato actual. Antes de producción, caracterizar venta válida, cantidades/stock insuficientes, persistencia y consistencia entre inventario y venta con datos temporales. No ejecutar sin aprobación del módulo y su matriz de comportamiento.

## Módulo 8 — Reportes y Persistencia (borrador, sujeto a aprobación antes de ejecutar)

Objetivo previsto: mejorar consultas, archivos TXT y consistencia de datos. Revisar persistencia, consultas y reportes de vencimiento/ventas, incluyendo `VencimientoService`, `VencimientoController`, `VencimientoView`, `ReporteVentasService`, `ReporteVentasController`, `ReporteVentasView` y `ReportesView`. Los formatos TXT solo se pueden cambiar en este módulo, con aprobación explícita y respaldo previo. Antes de producción, caracterizar criterios, cálculos, permisos, datos vacíos, límites de fechas y consistencia entre archivos. No ejecutar sin aprobación del módulo y su matriz de comportamiento.

## Deuda de code smells

Las siguientes decisiones quedan registradas tras la auditoría del módulo Usuarios y no modifican código todavía:

- **2e-B:** corregir el comentario obsoleto de `Usuario.java:135`, que afirma que solo `GestorUsuarios` puede configurar las respuestas aunque `RecuperacionService` ya invoca el método directamente.
- **Antes del módulo 8:** aplicar **Extract Method** a `Usuario.desdeLinea` para separar parsing, validación y restauración de campos sin cambiar el formato TXT. Usar las pruebas existentes de `UsuarioTest` y del repositorio como red de seguridad.
- **Módulo 6:** aplicar **Extract Class** para sacar de `Usuario`, `Administrador` y `Encargado` las operaciones de medicamentos e inventario.
- **Módulo 7:** aplicar **Extract Class** para separar de `Usuario`, `Administrador` y `Encargado` el carrito, las validaciones de venta, el registro de ventas y la actualización del inventario.
- **Opcional:** aplicar **Extract Method** a `LoginController.recuperar` solo con aprobación explícita y prueba manual adicional.
- **Aceptados y documentados, sin cambios:** mantener los parámetros largos y los Data Clumps de credenciales `char[]`, sin empaquetar ni prolongar la vida de las claves; mantener `UsuarioService` como frontera de aplicación; mantener `FichaUsuario` como DTO.
- **Transversal de interfaz:** el tamaño de los menús y las tarjetas duplicadas quedan fuera de alcance por ahora.

## Verificación y límites

Cada subpaso requiere pruebas primero y suite verde antes de modificar producción. Al finalizar cada fase: `mvn test` con desglose por clase, comparación SHA-256 de los TXT/backup con `sha256-inicial.csv`, inventario de `datos/`, `git status --short` y `git diff --stat` explicando los archivos no rastreados. Detenerse en la fase solicitada; no comenzar la siguiente sin aprobación.

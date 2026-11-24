# Registro de cambios - Refactorización de limpieza

**Autor:** Kilo (Arquitecto de Software Senior)  
**Fecha:** 2026-11-24  
**Rama:** `refactor/mvc-srp-tests`  
**Estado:** Cambios aplicados localmente. **NO subidos a GitHub** (pendiente de confirmación del usuario).

---

## Resumen

Se aplicaron cambios de limpieza basados en la auditoría de code smells (`docs/AUDITORIA_CODE_SMELLS.md`) y en las alertas del servidor de lenguaje Java de VS Code (código muerto: campos, métodos y clases internas sin uso). El objetivo fue eliminar dead code, reducir duplicación y mejorar la legibilidad sin alterar la funcionalidad.

## Archivos modificados

| Archivo | Cambio | Motivo |
|---------|--------|--------|
| `src/view/MedicamentoView.java` | Eliminado campo estático `ROJO` | Campo privado sin uso (dead code) |
| `src/view/MedicamentoView.java` | Eliminado método estático `botonIcono(AccionIcono.Tipo, Color)` | Método definido y nunca invocado (dead code) |
| `src/view/MedicamentoView.java` | Eliminada clase interna `IconoMedicamento` | Clase interna nunca instanciada (dead code) |
| `src/view/MedicamentoView.java` | Eliminada clase interna `IconoAlerta` | Clase interna nunca instanciada (dead code) |
| `src/view/MedicamentoView.java` | Eliminada clase interna `IconoAdvertencia` | Clase interna nunca instanciada (dead code) |
| `src/service/MedicamentoService.java` | Unificados `registrar` y `actualizar` en `guardarMedicamento(...)` | Duplicate Code: ambos compartían validación, normalización, cálculo de valores y persistencia (Smell D3 - Dispensables/Duplicate Code) |
| `src/service/GestionUsuariosService.java` | Extraído método privado `ultimoAdministradorActivo(List<Usuario>)` | Duplicate Code: la invariable "quede al menos un administrador activo" estaba repetida en `actualizarUsuario` y `cambiarEstadoUsuario` (Smell D3) |
| `src/util/Validaciones.java` | Eliminado archivo completo | Clase utilitaria con un único método `formatearTexto` sin uso real en el proyecto (Smell D7 - Dispensables/Lazy Class) |

## Archivos eliminados previamente (en sesión anterior, ya sin referencias)

| Archivo | Motivo |
|---------|--------|
| `src/persistence/ArchivoTXT.java` | Clase vacía marcador legacy, 0 referencias |
| `src/controller/VentaController.java` | Clase vacía, 0 referencias |
| `src/service/VentaService.java` | Clase vacía, 0 referencias |

## Detalle de cada cambio

### 1. MedicamentoView — eliminación de código muerto

Se removieron los siguientes miembros que el servidor de lenguaje Java de VS Code reportaba como "never used locally" y que confirmé con búsqueda de referencias (cero instanciaciones/invocaciones):

- `private static final Color ROJO` — declarado pero nunca leído.
- `private static JButton botonIcono(AccionIcono.Tipo tipo, Color color)` — declarado pero nunca llamado.
- `private static class IconoMedicamento implements Icon` — nunca instanciado (`new IconoMedicamento` = 0 ocurrencias).
- `private static class IconoAlerta implements Icon` — nunca instanciado.
- `private static class IconoAdvertencia implements Icon` — nunca instanciado.

La clase `AccionIcono` se conservó porque sí se usa en los botones de acción de la tabla.

**Técnica de refactor:** Eliminación de código muerto (Dead Code removal).

### 2. MedicamentoService — unificación de registrar y actualizar

Antes, `registrar` y `actualizar` duplicaban casi por completo el flujo:
1. `validarAdministradorFormulario(...)`
2. `normalizarYValidarCampos(datos)`
3. `listarParaGuardar()`
4. `validarCodigoUnico(...)`
5. `validarValores(campos)`
6. `crearMedicamento(...)`
7. `try/catch` con `errorAlGuardar(e)`

Ahora existe un método privado `guardarMedicamento(Usuario, DatosMedicamento, String codigoOriginal)` que contiene el flujo común. Los dos métodos públicos delegan en él pasando `null` (alta) o el `codigoOriginal` (edición). Las diferencias (código único vs exclusión del original, marca/stock mínimo conservados, registrar vs actualizar en el repositorio) se mantienen dentro del método compartido mediante la rama `if (codigoOriginal == null)`.

**Técnica de refactor:** Extract Method + unificación de flujo duplicado (elimina Duplicate Code).

### 3. GestionUsuariosService — extracción de invariable del último administrador

La condición `cuentas.stream().filter(u -> u.esAdministrador() && u.estaActivo()).count() <= 1` estaba repetida literalmente en `actualizarUsuario` (línea 106) y `cambiarEstadoUsuario` (línea 148). Se extrajo a:

```java
private static boolean ultimoAdministradorActivo(List<Usuario> cuentas) {
    return cuentas.stream().filter(u -> u.esAdministrador() && u.estaActivo()).count() <= 1;
}
```

**Técnica de refactor:** Extract Method (elimina Duplicate Code).

### 4. Validaciones — eliminación de claseLazy

`src/util/Validaciones.java` contenía un único método público estático `formatearTexto` que no es invocado desde ningún archivo de producción (la única mención es un comentario de Javadoc en `UsuarioServiceSupport`, no una referencia de código). Se eliminó el archivo.

**Técnica de refactor:** Eliminación de clase Lazy / Dead Code.

## Verificación

- `mvn compile` finaliza sin errores de compilación (solo warnings de entorno de Maven, no del proyecto).
- No se modificó el comportamiento de negocio: mismas validaciones, mismos mensajes de error, mismas reglas de sesión y permisos.

## Pendiente (no aplicado)

- No se subió a GitHub. Ejecutar `git add`, `git commit` y `git push` solo cuando el usuario lo confirme.
- Quedan code smells de mayor complejidad (clases grandes `VentaView`, `MedicamentoView`, `Usuario`, `Venta`; acoplamiento `Feature Envy`; secretos hardcodeados) que requieren refactorización más profunda y se dejaron para una siguiente iteración.

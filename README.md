# GestorFarmacia

Aplicación de consola para gestionar medicamentos, ventas, vencimientos y usuarios. Requiere JDK 17 o superior y Maven.

## Compilar y probar

Desde la raíz del repositorio:

```bash
mvn clean test
mvn package
java -cp target/classes Main
```

Ejecuta el programa desde la raíz para que las rutas relativas de `datos/` funcionen. En el primer inicio, si no existe `datos/usuarios.txt`, se crea la cuenta inicial `admin` con contraseña `Farmacia2026!`. El archivo de usuarios se genera localmente y no debe subirse al repositorio.

`datos/medicamentos.txt` incluye registros de demostración. El historial `datos/ventas.txt` se crea al registrar la primera venta y se mantiene local.
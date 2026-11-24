# Ejecución

## Requisitos

Se requiere Java 25 (JDK) y Maven.

## Construir

Desde la raíz del proyecto:

```text
mvn clean package
```

El archivo ejecutable se genera en `target\SaludVida.jar`.

## Ejecutar

Desde la raíz del proyecto, ejecuta `ejecutar.bat` o usa:

```text
java -jar target\SaludVida.jar
```

Los archivos de datos se resuelven desde el directorio de trabajo. Mantén `datos\` junto al directorio desde el que se ejecuta la aplicación; el JAR no incluye esa carpeta. Los repositorios crean el directorio al guardar si aún no existe.

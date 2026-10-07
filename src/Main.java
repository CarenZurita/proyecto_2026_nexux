import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        int opcion;

        do {

            System.out.println("1. Registrar medicamento");
System.out.println("2. Buscar medicamento");
System.out.println("3. Consultar stock");
System.out.println("4. Registrar venta");
System.out.println("5. Actualizar medicamento");
System.out.println("6. Control de vencimientos");
System.out.println("7. Eliminar medicamento caducado");
System.out.println("8. Salir");

            String entrada = teclado.nextLine().trim();

            try {
                opcion = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                opcion = -1;
            }

            switch (opcion) {

                case 1:
                    registrarMedicamento(teclado);
                    break;

                case 2:
                    buscarMedicamento(teclado);
                    break;

                case 3:
                    consultarStock (teclado);
                    break;
                case 4:
                    registrarVenta (teclado);
                    break;
                case 5:
                    actualizarMedicamento(teclado);
                    break;
                case 6:
                    controlarVencimientos();
                    break;
                case 7:
                   eliminarMedicamentoCaducado(teclado);
                    break;
                case 8:
                    System.out.println();
                    System.out.println ("programa finalizado");
                    break;
                default:
                    System.out.println ("opcion invalida, intente de nuevo");
                    break;
            }

        } while (opcion != 8);

        teclado.close();
    }


    public static void registrarMedicamento(Scanner teclado) {

        System.out.println();
        System.out.println("=================================");
        System.out.println("     REGISTRAR MEDICAMENTO");
        System.out.println("=================================");

        System.out.print("Nombre: ");
        String nombre = formatearTexto(teclado.nextLine());

        System.out.print("Código: ");
        String codigo = teclado.nextLine()
                .trim()
                .toUpperCase(Locale.ROOT);

        String categoria = seleccionarCategoria(teclado);

        String presentacion = seleccionarPresentacion(teclado);

        String laboratorio = seleccionarLaboratorio(teclado);



        System.out.print("Cantidad disponible: ");
        int cantidadDisponible =
                Integer.parseInt(teclado.nextLine());

        String fechaVencimiento = leerFechaValida(teclado);

        Medicamento medicamento = new Medicamento(
                nombre,
                codigo,
                categoria,
                presentacion,
                laboratorio,
                cantidadDisponible,
                fechaVencimiento
        );

        guardarMedicamento(
                nombre,
                codigo,
                categoria,
                presentacion,
                laboratorio,
                cantidadDisponible,
                fechaVencimiento
        );

        System.out.println();
        System.out.println("=================================");
        System.out.println("MEDICAMENTO REGISTRADO");
        System.out.println("=================================");

        medicamento.mostrarDatos();
    }


    
    public static void buscarMedicamento(Scanner teclado) {

    System.out.println();
    System.out.println("=================================");
    System.out.println("     BUSCAR MEDICAMENTO");
    System.out.println("=================================");
    System.out.print("Ingrese nombre o código a buscar: ");

    String busqueda = teclado.nextLine()
            .trim()
            .toLowerCase(Locale.ROOT);

    // Si no escribe nada, termina la búsqueda
    if (busqueda.isEmpty()) {
        System.out.println("Debe ingresar un texto para buscar.");
        return;
    }

    try (BufferedReader archivo =
                 new BufferedReader(new FileReader("medicamentos.txt"))) {

        String linea;
        int encontrados = 0;

        while ((linea = archivo.readLine()) != null) {

            if (linea.isBlank()) {
                continue;
            }

            String[] datos = linea.split(";");

            if (datos.length < 7) {
                continue;
            }

            String nombre = datos[0];
            String codigo = datos[1];
            String categoria = datos[2];
            String presentacion = datos[3];
            String laboratorio = datos[4];
            String fechaVencimiento = datos[6];

            int cantidadDisponible;

            try {
                cantidadDisponible = Integer.parseInt(datos[5]);
            } catch (NumberFormatException e) {
                cantidadDisponible = 0;
            }

            boolean coincide =
                    nombre.toLowerCase(Locale.ROOT).contains(busqueda)
                    || codigo.toLowerCase(Locale.ROOT).contains(busqueda);

            if (coincide) {

                encontrados++;

                Medicamento medicamento = new Medicamento(
                        nombre,
                        codigo,
                        categoria,
                        presentacion,
                        laboratorio,
                        cantidadDisponible,
                        fechaVencimiento
                );

                System.out.println();
                System.out.println("---------------------------------");
                System.out.println("Medicamento encontrado");
                System.out.println("---------------------------------");

                medicamento.mostrarDatos();
            }
        }

        System.out.println();

        if (encontrados == 0) {
            System.out.println("No se encontraron coincidencias.");
        } else {
            System.out.println(
                    encontrados + " medicamento(s) encontrado(s)."
            );
        }

    } catch (IOException e) {
        System.out.println(
                "Aún no hay medicamentos registrados o no se pudo "
                + "leer el archivo."
        );
    }
}


    public static String seleccionarCategoria(Scanner teclado) {

        String[] categorias = {
                "Analgésico",
                "Antibiótico",
                "Antiinflamatorio",
                "Antialérgico",
                "Antigripal",
                "Vitaminas y suplementos",
                "Antiácido / digestivo",
                "Otro"
        };

        String categoria = null;

        while (categoria == null) {

            System.out.println();
            System.out.println("Seleccione la categoría:");

            for (int i = 0; i < categorias.length; i++) {
                System.out.println((i + 1) + ". " + categorias[i]);
            }

            System.out.print("Opción: ");
            String entrada = teclado.nextLine().trim();

            try {

                int opcion = Integer.parseInt(entrada);

                if (opcion >= 1 && opcion <= categorias.length) {

                    categoria = categorias[opcion - 1];

                    if (categoria.equals("Otro")) {
                        System.out.print("Especifique la categoría: ");
                        categoria = formatearTexto(teclado.nextLine());
                    }

                } else {
                    System.out.println(
                            "Opción fuera de rango. Intente de nuevo."
                    );
                }

            } catch (NumberFormatException e) {
                System.out.println(
                        "Debe ingresar un número. Intente de nuevo."
                );
            }
        }

        return categoria;
    }


    public static String seleccionarPresentacion(Scanner teclado) {

        String[] presentaciones = {
                "Tabletas",
                "Cápsulas",
                "Jarabe",
                "Inyección",
                "Crema / pomada",
                "Gotas",
                "Suspensión",
                "Supositorio",
                "Otro"
        };

        String presentacion = null;

        while (presentacion == null) {

            System.out.println();
            System.out.println("Seleccione la presentación:");

            for (int i = 0; i < presentaciones.length; i++) {
                System.out.println((i + 1) + ". " + presentaciones[i]);
            }

            System.out.print("Opción: ");
            String entrada = teclado.nextLine().trim();

            try {

                int opcion = Integer.parseInt(entrada);

                if (opcion >= 1 && opcion <= presentaciones.length) {

                    presentacion = presentaciones[opcion - 1];

                    if (presentacion.equals("Otro")) {
                        System.out.print("Especifique la presentación: ");
                        presentacion = formatearTexto(teclado.nextLine());
                    }

                } else {
                    System.out.println(
                            "Opción fuera de rango. Intente de nuevo."
                    );
                }

            } catch (NumberFormatException e) {
                System.out.println(
                        "Debe ingresar un número. Intente de nuevo."
                );
            }
        }

        return presentacion;
    }


    
    public static String leerFechaValida(Scanner teclado) {

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate fecha = null;

        while (fecha == null) {

            System.out.print(
                    "Fecha de vencimiento (AAAA-MM-DD): "
            );

            String entrada = teclado.nextLine().trim();

            try {

                fecha = LocalDate.parse(entrada, formato);

                if (fecha.isBefore(LocalDate.now())) {
                    System.out.println(
                            "La fecha ya venció. Ingrese una fecha futura."
                    );
                    fecha = null;
                }

            } catch (DateTimeParseException e) {
                System.out.println(
                        "Fecha inválida. Use el formato AAAA-MM-DD "
                        + "(ej: 2026-12-31)."
                );
            }
        }

        return fecha.format(formato);
    }


    public static void guardarMedicamento(
            String nombre,
            String codigo,
            String categoria,
            String presentacion,
            String laboratorio,
            int cantidadDisponible,
            String fechaVencimiento) {

        try {

            BufferedWriter archivo =
                    new BufferedWriter(
                            new FileWriter(
                                    "medicamentos.txt",
                                    true
                            )
                    );

            archivo.write(
                    nombre + ";" +
                    codigo + ";" +
                    categoria + ";" +
                    presentacion + ";" +
                    laboratorio + ";" +
                    cantidadDisponible + ";" +
                    fechaVencimiento
            );

            archivo.newLine();

            archivo.close();

            System.out.println(
                    "Medicamento guardado correctamente."
            );

        } catch (IOException e) {

            System.out.println(
                    "Error al guardar el medicamento."
            );
        }
    }
    
    public static void consultarStock(Scanner teclado){


    System.out.println();
    System.out.println("===============================");
    System.out.println("      CONSULTAR STOCK");
    System.out.println("===============================");


    System.out.print(
        "Ingrese código del medicamento: "
    );


    String codigo = teclado.nextLine()
            .trim()
            .toUpperCase();



    try(
        BufferedReader archivo =
        new BufferedReader(
        new FileReader("medicamentos.txt"))
    ){


        String linea;
        boolean encontrado = false;



        while((linea = archivo.readLine()) != null){


            String datos[] = linea.split(";");


            if(datos.length < 7){

                continue;
            }



            if(datos[1].equals(codigo)){


                encontrado = true;


                System.out.println();
                System.out.println(
                "Medicamento encontrado"
                );


                System.out.println(
                "Nombre: "
                + datos[0]
                );


                System.out.println(
                "Código: "
                + datos[1]
                );


                System.out.println(
                "Cantidad disponible: "
                + datos[5]
                );



                int stock =
                Integer.parseInt(datos[5]);



                if(stock == 0){

                    System.out.println(
                    "Estado: SIN STOCK"
                    );

                }else{

                    System.out.println(
                    "Estado: DISPONIBLE"
                    );
                }


                break;

            }

        }



        if(!encontrado){

            System.out.println(
            "Medicamento no encontrado."
            );

        }



    }catch(IOException e){


        System.out.println(
        "Error al leer medicamentos.txt"
        );

    }

}


public static void registrarVenta(Scanner teclado){


    System.out.println();
    System.out.println("===============================");
    System.out.println("       REGISTRAR VENTA");
    System.out.println("===============================");



    System.out.print(
    "Código medicamento: "
    );


    String codigo =
    teclado.nextLine()
    .trim()
    .toUpperCase();



    System.out.print(
    "Cantidad vendida: "
    );


    int cantidadVenta =
    Integer.parseInt(
    teclado.nextLine()
    );



    try{


        BufferedReader archivo =
        new BufferedReader(
        new FileReader("medicamentos.txt")
        );


        String linea;

        StringBuilder nuevoArchivo =
        new StringBuilder();


        boolean vendido=false;



        while((linea = archivo.readLine()) != null){


            String datos[] =
            linea.split(";");



            if(datos.length < 7){

                continue;

            }



            if(datos[1].equals(codigo)){



                int stock =
                Integer.parseInt(datos[5]);
                

                LocalDate fechaVencimiento;

try {

    fechaVencimiento =
            LocalDate.parse(
                    datos[6],
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd"
                    )
            );

} catch (DateTimeParseException e) {

    System.out.println(
            "La fecha de vencimiento del medicamento "
                    + "no es válida."
    );

    archivo.close();

    return;
}


if (fechaVencimiento.isBefore(LocalDate.now())) {

    System.out.println();
    System.out.println(
            "No se puede realizar la venta."
    );

    System.out.println(
            "El medicamento se encuentra vencido."
    );

    archivo.close();

    return;
}

                if(cantidadVenta <=0){


                    System.out.println(
                    "Cantidad inválida."
                    );

                    archivo.close();
                    return;

                }



                if(cantidadVenta > stock){


                    System.out.println(
                    "Stock insuficiente."
                    );

                    archivo.close();
                    return;

                }



                int nuevoStock =
                stock - cantidadVenta;



                datos[5] =
                String.valueOf(nuevoStock);



                vendido=true;


                guardarVenta(
                datos[1],
                datos[0],
                cantidadVenta
                );


            }



            nuevoArchivo.append(
            String.join(";", datos)
            );

            nuevoArchivo.append("\n");


        }



        archivo.close();



        if(vendido){


            BufferedWriter escritor =
            new BufferedWriter(
            new FileWriter(
            "medicamentos.txt"
            ));



            escritor.write(
            nuevoArchivo.toString()
            );


            escritor.close();



            System.out.println(
            "Venta registrada correctamente."
            );


        }else{


            System.out.println(
            "Medicamento no encontrado."
            );

        }



    }catch(Exception e){


        System.out.println(
        "Error registrando venta."
        );

    }


}

public static void guardarVenta(
String codigo,
String nombre,
int cantidad){


    try{


        BufferedWriter archivo =
        new BufferedWriter(
        new FileWriter(
        "ventas.txt",
        true
        ));



        archivo.write(
        codigo+";"+
        nombre+";"+
        cantidad
        );


        archivo.newLine();


        archivo.close();



    }catch(IOException e){


        System.out.println(
        "Error guardando venta."
        );

    }


}

// ======================================================
// HU-05 ACTUALIZAR MEDICAMENTO
// ======================================================

public static void actualizarMedicamento(Scanner teclado) {

    System.out.println();
    System.out.println("=================================");
    System.out.println("     ACTUALIZAR MEDICAMENTO");
    System.out.println("=================================");

    System.out.print("Ingrese código del medicamento: ");

    String codigoBuscado = teclado.nextLine()
            .trim()
            .toUpperCase(Locale.ROOT);

    if (codigoBuscado.isEmpty()) {

        System.out.println(
                "El código no puede estar vacío."
        );

        return;
    }

    try {

        BufferedReader archivo =
                new BufferedReader(
                        new FileReader("medicamentos.txt")
                );

        StringBuilder nuevoContenido =
                new StringBuilder();

        String linea;

        boolean encontrado = false;

        while ((linea = archivo.readLine()) != null) {

            if (linea.isBlank()) {
                continue;
            }

            String[] datos = linea.split(";");

            if (datos.length < 7) {
                continue;
            }

            if (datos[1].equalsIgnoreCase(codigoBuscado)) {

                encontrado = true;

                System.out.println();
                System.out.println("Medicamento encontrado.");
                System.out.println("Nombre actual: " + datos[0]);
                System.out.println("Categoría actual: " + datos[2]);
                System.out.println("Presentación actual: " + datos[3]);
                System.out.println("Laboratorio actual: " + datos[4]);
                System.out.println("Cantidad actual: " + datos[5]);
                System.out.println("Vencimiento actual: " + datos[6]);

                System.out.println();
                System.out.println("Ingrese los nuevos datos.");

                // Nombre
                String nombre;

                do {

                    System.out.print("Nuevo nombre: ");

                    nombre = formatearTexto(
                            teclado.nextLine()
                    );

                    if (nombre.isEmpty()) {

                        System.out.println(
                                "El nombre no puede estar vacío."
                        );
                    }

                } while (nombre.isEmpty());


                // Categoría
                String categoria =
                        seleccionarCategoria(teclado);


                // Presentación
                String presentacion =
                        seleccionarPresentacion(teclado);


                // Laboratorio
                String laboratorio =
                        seleccionarLaboratorio(teclado);


                // Cantidad
                int cantidadDisponible;

                while (true) {

                    System.out.print(
                            "Nueva cantidad disponible: "
                    );

                    String entrada =
                            teclado.nextLine().trim();

                    try {

                        cantidadDisponible =
                                Integer.parseInt(entrada);

                        if (cantidadDisponible < 0) {

                            System.out.println(
                                    "La cantidad no puede ser negativa."
                            );

                            continue;
                        }

                        break;

                    } catch (NumberFormatException e) {

                        System.out.println(
                                "Debe ingresar un número entero válido."
                        );
                    }
                }


                // Fecha de vencimiento
                String fechaVencimiento =
                        leerFechaValida(teclado);


                datos[0] = nombre;
                datos[2] = categoria;
                datos[3] = presentacion;
                datos[4] = laboratorio;
                datos[5] =
                        String.valueOf(cantidadDisponible);
                datos[6] = fechaVencimiento;
            }

            nuevoContenido.append(
                    String.join(";", datos)
            );

            nuevoContenido.append("\n");
        }

        archivo.close();


        if (!encontrado) {

            System.out.println(
                    "No existe un medicamento con ese código."
            );

            return;
        }


        BufferedWriter escritor =
                new BufferedWriter(
                        new FileWriter("medicamentos.txt")
                );

        escritor.write(
                nuevoContenido.toString()
        );

        escritor.close();


        System.out.println();
        System.out.println(
                "Medicamento actualizado correctamente."
        );

    } catch (IOException e) {

        System.out.println(
                "Error al actualizar el medicamento."
        );
    }
}

public static String seleccionarLaboratorio(Scanner teclado) {

    String laboratorio = null;

    while (laboratorio == null) {

        System.out.println();
        System.out.println("Seleccione el laboratorio:");

        System.out.println("1. INTI");
        System.out.println("2. Bagó");
        System.out.println("3. COFAR");
        System.out.println("4. LAFAR");
        System.out.println("5. VITA");

        System.out.print("Opción: ");

        String entrada =
                teclado.nextLine().trim();

        try {

            int opcion =
                    Integer.parseInt(entrada);

            switch (opcion) {

                case 1:
                    laboratorio = "INTI";
                    break;

                case 2:
                    laboratorio = "Bagó";
                    break;

                case 3:
                    laboratorio = "COFAR";
                    break;

                case 4:
                    laboratorio = "LAFAR";
                    break;

                case 5:
                    laboratorio = "VITA";
                    break;

                default:

                    System.out.println(
                            "Opción fuera de rango."
                    );

                    break;
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "Debe ingresar un número."
            );
        }
    }

    return laboratorio;
}

// ======================================================
// HU-06 CONTROL DE VENCIMIENTOS
// ======================================================

public static void controlarVencimientos() {

    System.out.println();
    System.out.println("=================================");
    System.out.println("     CONTROL DE VENCIMIENTOS");
    System.out.println("=================================");

    LocalDate hoy = LocalDate.now();

    LocalDate limite =
            hoy.plusDays(30);

    boolean encontrado = false;

    try (
        BufferedReader archivo =
                new BufferedReader(
                        new FileReader("medicamentos.txt")
                )
    ) {

        String linea;

        while ((linea = archivo.readLine()) != null) {

            if (linea.isBlank()) {
                continue;
            }

            String[] datos =
                    linea.split(";");

            if (datos.length < 7) {
                continue;
            }

            String nombre = datos[0];
            String codigo = datos[1];
            String fechaTexto = datos[6];

            try {

                LocalDate fechaVencimiento =
                        LocalDate.parse(
                                fechaTexto,
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd"
                                )
                        );

                if (!fechaVencimiento.isAfter(limite)) {

                    encontrado = true;

                    System.out.println();
                    System.out.println(
                            "---------------------------------"
                    );

                    System.out.println(
                            "Medicamento: " + nombre
                    );

                    System.out.println(
                            "Código: " + codigo
                    );

                    System.out.println(
                            "Fecha de vencimiento: "
                                    + fechaTexto
                    );


                    if (fechaVencimiento.isBefore(hoy)) {

                        System.out.println(
                                "Estado: VENCIDO"
                        );

                    } else {

                        System.out.println(
                                "Estado: PRÓXIMO A VENCER"
                        );
                    }
                }

            } catch (DateTimeParseException e) {

                System.out.println(
                        "Fecha inválida en medicamento: "
                                + codigo
                );
            }
        }


        if (!encontrado) {

            System.out.println();
            System.out.println(
                    "No existen medicamentos próximos "
                            + "a vencer o vencidos."
            );
        }

    } catch (IOException e) {

        System.out.println(
                "No se pudo leer medicamentos.txt."
        );
    }
}

// ======================================================
// HU-07 ELIMINAR MEDICAMENTO CADUCADO
// ======================================================

public static void eliminarMedicamentoCaducado(
        Scanner teclado) {

    System.out.println();
    System.out.println("=================================");
    System.out.println(" ELIMINAR MEDICAMENTO CADUCADO");
    System.out.println("=================================");

    System.out.print(
            "Ingrese código del medicamento: "
    );

    String codigoBuscado =
            teclado.nextLine()
                    .trim()
                    .toUpperCase(Locale.ROOT);


    if (codigoBuscado.isEmpty()) {

        System.out.println(
                "El código no puede estar vacío."
        );

        return;
    }


    try {

        BufferedReader archivo =
                new BufferedReader(
                        new FileReader("medicamentos.txt")
                );

        StringBuilder nuevoContenido =
                new StringBuilder();

        String linea;

        boolean encontrado = false;
        boolean eliminado = false;

        LocalDate hoy = LocalDate.now();


        while ((linea = archivo.readLine()) != null) {

            if (linea.isBlank()) {
                continue;
            }

            String[] datos =
                    linea.split(";");


            if (datos.length < 7) {
                continue;
            }


            if (datos[1].equalsIgnoreCase(
                    codigoBuscado)) {

                encontrado = true;


                LocalDate fechaVencimiento;

                try {

                    fechaVencimiento =
                            LocalDate.parse(
                                    datos[6],
                                    DateTimeFormatter.ofPattern(
                                            "yyyy-MM-dd"
                                    )
                            );

                } catch (DateTimeParseException e) {

                    System.out.println(
                            "La fecha del medicamento "
                                    + "no es válida."
                    );

                    archivo.close();

                    return;
                }


                if (!fechaVencimiento.isBefore(hoy)) {

                    System.out.println();
                    System.out.println(
                            "El medicamento todavía está vigente."
                    );

                    System.out.println(
                            "No puede ser eliminado."
                    );

                    archivo.close();

                    return;
                }


                System.out.println();
                System.out.println(
                        "Medicamento caducado encontrado:"
                );

                System.out.println(
                        "Nombre: " + datos[0]
                );

                System.out.println(
                        "Código: " + datos[1]
                );

                System.out.println(
                        "Fecha de vencimiento: "
                                + datos[6]
                );


                System.out.print(
                        "¿Desea eliminarlo? (S/N): "
                );


                String confirmacion =
                        teclado.nextLine()
                                .trim()
                                .toUpperCase();


                if (confirmacion.equals("S")) {

                    eliminado = true;

                    continue;

                } else {

                    System.out.println(
                            "Eliminación cancelada."
                    );
                }
            }


            nuevoContenido.append(linea);
            nuevoContenido.append("\n");
        }


        archivo.close();


        if (!encontrado) {

            System.out.println(
                    "Medicamento no encontrado."
            );

            return;
        }


        if (!eliminado) {

            return;
        }


        BufferedWriter escritor =
                new BufferedWriter(
                        new FileWriter(
                                "medicamentos.txt"
                        )
                );


        escritor.write(
                nuevoContenido.toString()
        );


        escritor.close();


        System.out.println();
        System.out.println(
                "Medicamento eliminado correctamente."
        );


    } catch (IOException e) {

        System.out.println(
                "Error al eliminar el medicamento."
        );
    }
}



    public static String formatearTexto(String texto) {

        texto = texto.trim().toLowerCase();

        if (texto.isEmpty()) {
            return texto;
        }

        String[] palabras = texto.split("\\s+");

        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {

            String palabraFormateada =
                    palabra.substring(0, 1).toUpperCase()
                    + palabra.substring(1);

            resultado.append(palabraFormateada).append(" ");
        }

        return resultado.toString().trim();
    }
}

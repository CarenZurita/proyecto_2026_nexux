import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        String continuar;

        do {

            System.out.println();
            System.out.println("=================================");
            System.out.println("     REGISTRAR MEDICAMENTO");
            System.out.println("=================================");

            System.out.print("Nombre: ");
            String nombre = teclado.nextLine();

            System.out.print("Código: ");
            String codigo = teclado.nextLine();

            System.out.print("Categoría: ");
            String categoria = teclado.nextLine();

            System.out.println();
            System.out.println("Seleccione el laboratorio:");
            System.out.println("1. INTI");
            System.out.println("2. Bagó");
            System.out.println("3. COFAR");
            System.out.println("4. LAFAR");
            System.out.println("5. VITA");

            System.out.print("Opción: ");
            int opcionLaboratorio = Integer.parseInt(teclado.nextLine());

            String laboratorio;

            switch (opcionLaboratorio) {

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
                    laboratorio = "No especificado";
                    break;
            }

            System.out.print("Cantidad disponible: ");
            int cantidadDisponible = Integer.parseInt(teclado.nextLine());

            System.out.print("Fecha de vencimiento (AAAA-MM-DD): ");
            String fechaVencimiento = teclado.nextLine();

            Medicamento medicamento = new Medicamento(
                    nombre,
                    codigo,
                    categoria,
                    laboratorio,
                    cantidadDisponible,
                    fechaVencimiento
            );

            guardarMedicamento(
                    nombre,
                    codigo,
                    categoria,
                    laboratorio,
                    cantidadDisponible,
                    fechaVencimiento
            );

            System.out.println();
            System.out.println("=================================");
            System.out.println("MEDICAMENTO REGISTRADO");
            System.out.println("=================================");

            medicamento.mostrarDatos();

            System.out.println();
            System.out.print("¿Desea registrar otro medicamento? (S/N): ");
            continuar = teclado.nextLine();

        } while (continuar.equalsIgnoreCase("S"));

        System.out.println();
        System.out.println("Programa finalizado.");

        teclado.close();
    }


    public static void guardarMedicamento(
            String nombre,
            String codigo,
            String categoria,
            String laboratorio,
            int cantidadDisponible,
            String fechaVencimiento) {

        try {

            BufferedWriter archivo = new BufferedWriter(
                    new FileWriter("medicamentos.txt", true)
            );

            archivo.write(
                    codigo + ";" +
                    nombre + ";" +
                    categoria + ";" +
                    laboratorio + ";" +
                    cantidadDisponible + ";" +
                    fechaVencimiento
            );

            archivo.newLine();

            archivo.close();

            System.out.println("Medicamento guardado en el archivo correctamente.");

        } catch (IOException e) {

            System.out.println("Error al guardar el medicamento.");

        }
    }
}
package persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import model.Venta;

public class VentaRepository {
    private final Path archivo;

    public VentaRepository() {
        this(Paths.get("datos", "ventas.txt"));
    }

    public VentaRepository(Path archivo) {
        this.archivo = archivo;
    }

    public void registrar(Venta venta) throws IOException {
        crearDirectorioSiEsNecesario();
        Files.writeString(archivo, venta.aLinea() + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public List<Venta> listar() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }

        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        List<Venta> ventas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) {
                continue;
            }
            try {
                ventas.add(Venta.desdeLinea(lineas.get(i)));
            } catch (IllegalArgumentException e) {
                throw new IOException("Registro inválido en ventas.txt, línea "
                        + (i + 1) + ".", e);
            }
        }
        return ventas;
    }

    public boolean eliminar(Venta venta) throws IOException {
        List<String> lineas = Files.exists(archivo)
                ? Files.readAllLines(archivo, StandardCharsets.UTF_8)
                : new ArrayList<>();
        String registro = venta.aLinea();
        boolean eliminado = lineas.remove(registro);
        if (eliminado) {
            crearDirectorioSiEsNecesario();
            Files.write(archivo, lineas, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        }
        return eliminado;
    }

    private void crearDirectorioSiEsNecesario() throws IOException {
        Path directorio = archivo.getParent();
        if (directorio != null) {
            Files.createDirectories(directorio);
        }
    }
}

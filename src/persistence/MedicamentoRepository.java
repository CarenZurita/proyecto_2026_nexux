package persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Medicamento;

public class MedicamentoRepository {
    private final Path archivo;

    public MedicamentoRepository() {
        this(Paths.get("datos", "medicamentos.txt"));
    }

    public MedicamentoRepository(Path archivo) {
        this.archivo = archivo;
    }

    public List<Medicamento> listar() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }

        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        List<Medicamento> medicamentos = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) {
                continue;
            }
            try {
                medicamentos.add(Medicamento.desdeLinea(lineas.get(i)));
            } catch (IllegalArgumentException e) {
                throw new IOException("Registro inválido en medicamentos.txt, línea "
                        + (i + 1) + ".", e);
            }
        }
        return medicamentos;
    }

    public Optional<Medicamento> buscarPorCodigo(String codigo) throws IOException {
        return listar().stream()
                .filter(medicamento -> medicamento.getCodigo().equalsIgnoreCase(codigo))
                .findFirst();
    }

    public List<Medicamento> buscar(String texto) throws IOException {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar un texto para buscar.");
        }
        String consulta = texto.trim().toLowerCase(java.util.Locale.ROOT);
        Double precio = null;
        try {
            double precioIngresado = Double.parseDouble(consulta.replace(',', '.'));
            if (Double.isFinite(precioIngresado) && precioIngresado >= 0) {
                precio = precioIngresado;
            }
        } catch (NumberFormatException e) {
            // La búsqueda también admite consultas de texto como nombre, código o marca.
        }
        Double precioBuscado = precio;
        return listar().stream()
                .filter(medicamento -> medicamento.getNombre()
                                .toLowerCase(java.util.Locale.ROOT).contains(consulta)
                        || medicamento.getCodigo()
                                .toLowerCase(java.util.Locale.ROOT).contains(consulta)
                        || medicamento.getMarca()
                                .toLowerCase(java.util.Locale.ROOT).contains(consulta)
                        || (precioBuscado != null
                                && Double.compare(medicamento.getPrecio(), precioBuscado) == 0))
                .toList();
    }

    public void registrar(Medicamento medicamento) throws IOException {
        crearDirectorioSiEsNecesario();
        Files.writeString(archivo, medicamento.aLinea() + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public boolean actualizar(Medicamento actualizado) throws IOException {
        List<Medicamento> medicamentos = listar();
        for (int i = 0; i < medicamentos.size(); i++) {
            if (medicamentos.get(i).getCodigo().equalsIgnoreCase(actualizado.getCodigo())) {
                medicamentos.set(i, actualizado);
                guardarTodos(medicamentos);
                return true;
            }
        }
        return false;
    }

    public boolean eliminar(String codigo) throws IOException {
        List<Medicamento> medicamentos = listar();
        boolean eliminado = medicamentos.removeIf(
                medicamento -> medicamento.getCodigo().equalsIgnoreCase(codigo));
        if (eliminado) {
            guardarTodos(medicamentos);
        }
        return eliminado;
    }

    private void guardarTodos(List<Medicamento> medicamentos) throws IOException {
        crearDirectorioSiEsNecesario();
        List<String> lineas = medicamentos.stream().map(Medicamento::aLinea).toList();
        Files.write(archivo, lineas, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private void crearDirectorioSiEsNecesario() throws IOException {
        Path directorio = archivo.getParent();
        if (directorio != null) {
            Files.createDirectories(directorio);
        }
    }
}

package persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import model.Usuario;

/**
 * Repositorio TXT de Usuarios: convierte registros UTF-8 a entidades y
 * persiste las cuentas sin exponer detalles del archivo a las vistas.
 */
public class UsuarioRepository {
    private final Path archivo;

    /**
     * Usa la ruta de datos relativa al directorio de trabajo.
     */
    public UsuarioRepository() {
        this(Paths.get("datos", "usuarios.txt"));
    }

    /**
     * Permite inyectar una ruta concreta, por ejemplo un archivo temporal de pruebas.
     *
     * @param archivo archivo TXT de cuentas
     */
    public UsuarioRepository(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Indica si el archivo de cuentas existe.
     *
     * @return {@code true} si la ruta apunta a un archivo existente
     */
    public boolean existe() {
        return Files.exists(archivo);
    }

    /**
     * Lee y valida las cuentas, asignando IDs antiguos y creando respaldo si hace falta.
     *
     * @return cuentas reconstruidas
     * @throws IOException si el archivo no se puede leer o contiene registros inválidos
     */
    public List<Usuario> listar() throws IOException {
        List<Usuario> cuentas = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                try {
                    Usuario cuenta = Usuario.desdeLinea(linea);
                    if (cuentas.stream().anyMatch(
                            u -> u.getNombreUsuario().equals(cuenta.getNombreUsuario()))) {
                        throw new IllegalArgumentException("Usuario repetido");
                    }
                    if (cuenta.getId() > 0
                            && cuentas.stream().anyMatch(u -> u.getId() == cuenta.getId())) {
                        throw new IllegalArgumentException("ID repetido");
                    }
                    cuentas.add(cuenta);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Datos inválidos en usuarios.txt, línea "
                            + numeroLinea, e);
                }
            }
        }
        if (cuentas.stream().anyMatch(u -> u.getId() == 0)) {
            asignarIds(cuentas);
            Path respaldo = archivo.resolveSibling("usuarios.antes-gestion.bak");
            if (!Files.exists(respaldo)) {
                Files.copy(archivo, respaldo);
            }
            guardarTodos(cuentas);
        }
        return cuentas;
    }

    /**
     * Guarda el conjunto completo en UTF-8 mediante archivo temporal y reemplazo atómico
     * cuando el sistema de archivos lo permite.
     *
     * @param cuentas cuentas que se guardan
     * @throws IOException si falla la escritura o el reemplazo
     */
    public void guardarTodos(List<Usuario> cuentas) throws IOException {
        asignarIds(cuentas);
        Path respaldo = archivo.resolveSibling("usuarios.antes-recuperacion.bak");
        if (Files.exists(archivo) && !Files.exists(respaldo)
                && cuentas.stream().anyMatch(Usuario::tieneRecuperacion)) {
            Files.copy(archivo, respaldo);
        }
        crearDirectorioSiEsNecesario();
        Path temporal = Files.createTempFile(archivo.toAbsolutePath().getParent(),
                "usuarios-", ".tmp");
        try {
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal,
                    StandardCharsets.UTF_8)) {
                for (Usuario cuenta : cuentas) {
                    escritor.write(cuenta.aLinea());
                    escritor.newLine();
                }
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void asignarIds(List<Usuario> cuentas) throws IOException {
        long siguiente = (long) cuentas.stream().mapToInt(Usuario::getId).max().orElse(0) + 1;
        for (Usuario cuenta : cuentas) {
            if (cuenta.getId() == 0) {
                if (siguiente > Integer.MAX_VALUE) {
                    throw new IOException("Se agotaron los IDs de usuarios.");
                }
                cuenta.asignarId((int) siguiente++);
            }
        }
    }

    private void crearDirectorioSiEsNecesario() throws IOException {
        Path directorio = archivo.toAbsolutePath().getParent();
        if (directorio != null) {
            Files.createDirectories(directorio);
        }
    }

}

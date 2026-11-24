package persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UsuarioRepositoryTest {
    private static final String SAL = Base64.getEncoder().encodeToString(new byte[16]);
    private static final String HASH = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String SAL_RESPUESTA = Base64.getEncoder().encodeToString(new byte[16]);
    private static final String HASH_RESPUESTA = Base64.getEncoder().encodeToString(new byte[32]);

    @TempDir
    Path directorioTemporal;

    private Path archivo;
    private UsuarioRepository repositorio;

    @BeforeEach
    void prepararRepositorio() {
        archivo = directorioTemporal.resolve("usuarios.txt");
        repositorio = new UsuarioRepository(archivo);
    }

    @Test
    void archivoInexistenteNoExisteYListarloProduceIOException() {
        assertFalse(repositorio.existe());
        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void archivoVacioSeListaSinCuentas() throws IOException {
        Files.createFile(archivo);

        assertTrue(repositorio.existe());
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void cargaRegistrosAntiguosDeSeisCampos() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "true"));

        assertEquals(1, repositorio.listar().size());
    }

    @Test
    void cargaRegistrosDeSieteCamposConId() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "true", "7"));

        assertEquals(7, repositorio.listar().get(0).getId());
    }

    @Test
    void sieteCamposConIdVacioSeRechazan() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "true") + ";");

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void idCeroSeReasignaYConservaElRespaldoDeMigracion() throws IOException {
        String original = linea("usuario1", "Nombre", "ENCARGADO", "true", "0");
        escribir(original);

        Usuario usuario = repositorio.listar().get(0);

        assertEquals(1, usuario.getId());
        assertEquals(original, Files.readString(
                archivo.resolveSibling("usuarios.antes-gestion.bak"), StandardCharsets.UTF_8).trim());
        assertEquals(1, new UsuarioRepository(archivo).listar().get(0).getId());
    }

    @Test
    void idNegativoSeRechaza() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "true", "-1"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void idNoNumericoSeRechaza() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "true", "id"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void usuariosDuplicadosSeRechazan() throws IOException {
        escribir(linea("usuario1", "Primero", "ENCARGADO", "true"),
                linea("usuario1", "Segundo", "ENCARGADO", "true"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void idsDuplicadosSeRechazan() throws IOException {
        escribir(linea("usuario1", "Primero", "ENCARGADO", "true", "4"),
                linea("usuario2", "Segundo", "ENCARGADO", "true", "4"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void lineaEnBlancoProduceIOExceptionEnLugarDeOmitirse() throws IOException {
        escribir("");

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void lineaCorruptaProduceIOException() throws IOException {
        escribir("registro corrupto");

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void cuentaDeshabilitadaSeCargaComoDeshabilitada() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "false"));

        assertFalse(repositorio.listar().get(0).estaActivo());
    }

    @Test
    void rolInvalidoSeRechaza() throws IOException {
        escribir(linea("usuario1", "Nombre", "OTRO", "true"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void usuarioFueraDelFormatoPermitidoSeRechaza() throws IOException {
        escribir(linea("usuario con espacios", "Nombre", "ENCARGADO", "true"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void nombreConSeparadorSeRechaza() throws IOException {
        escribir(linea("usuario1", "Nombre;Extra", "ENCARGADO", "true"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void base64InvalidaSeRechaza() throws IOException {
        escribir("usuario1;Nombre;ENCARGADO;true;!;"+ HASH);

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void claveConLongitudBase64IncorrectaSeRechaza() throws IOException {
        escribir("usuario1;Nombre;ENCARGADO;true;" + SAL + ";"
                + Base64.getEncoder().encodeToString(new byte[31]));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void cuentaSinRecuperacionEsValida() throws IOException {
        escribir(linea("usuario1", "Nombre", "ENCARGADO", "true"));

        assertFalse(repositorio.listar().get(0).tieneRecuperacion());
    }

    @Test
    void formatoDeDoceCamposConRolNoAdministradorSeRechaza() throws IOException {
        escribir(lineaRecuperacion("usuario1", "Nombre", "ENCARGADO", "true", null));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void formatoDeDoceCamposConRecuperacionSeCarga() throws IOException {
        escribir(lineaRecuperacion("admin", "Admin", "ADMINISTRADOR", "true", null));

        Usuario usuario = repositorio.listar().get(0);

        assertEquals(1, usuario.getId());
        assertTrue(usuario.tieneRecuperacion());
    }

    @Test
    void intentosDeRecuperacionNegativosOMayoresADosSeRechazan() throws IOException {
        escribir(lineaRecuperacion("admin", "Admin", "ADMINISTRADOR", "true", null, "-1", "0"));
        assertThrows(IOException.class, repositorio::listar);

        escribir(lineaRecuperacion("admin", "Admin", "ADMINISTRADOR", "true", null, "3", "0"));
        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void fechaDeBloqueoNegativaSeRechaza() throws IOException {
        escribir(lineaRecuperacion("admin", "Admin", "ADMINISTRADOR", "true",
                null, "0", "-1"));

        assertThrows(IOException.class, repositorio::listar);
    }

    @Test
    void formatoDeTreceCamposConRecuperacionEIdSeCarga() throws IOException {
        escribir(lineaRecuperacion("admin", "Admin", "ADMINISTRADOR", "true", "9", "0", "0"));

        Usuario usuario = repositorio.listar().get(0);

        assertEquals(9, usuario.getId());
        assertTrue(usuario.tieneRecuperacion());
    }

    @Test
    void guardarYVolverACargarConservaTodosLosCamposYRespaldaRecuperacion()
            throws IOException {
        String original = lineaRecuperacion("admin", "Admin", "ADMINISTRADOR", "true",
                "9", "2", "123456");
        escribir(original);
        Usuario usuario = repositorio.listar().get(0);

        repositorio.guardarTodos(List.of(usuario));

        assertEquals(original, Files.readString(
                archivo.resolveSibling("usuarios.antes-recuperacion.bak"),
                StandardCharsets.UTF_8).trim());
        assertEquals(original, new UsuarioRepository(archivo).listar().get(0).aLinea());
    }

    @Test
    void asignaIdsARegistrosAntiguosSinId() throws IOException {
        String primera = linea("usuario1", "Primero", "ENCARGADO", "true");
        String segunda = linea("usuario2", "Segundo", "ENCARGADO", "true");
        escribir(primera, segunda);

        List<Usuario> usuarios = repositorio.listar();

        assertEquals(List.of(1, 2), usuarios.stream().map(Usuario::getId).toList());
        assertTrue(Files.exists(archivo.resolveSibling("usuarios.antes-gestion.bak")));
    }

    @Test
    void idsAgotadosProducenIOException() throws IOException {
        escribir(linea("usuario1", "Primero", "ENCARGADO", "true",
                        Integer.toString(Integer.MAX_VALUE)),
                linea("usuario2", "Segundo", "ENCARGADO", "true"));

        assertThrows(IOException.class, repositorio::listar);
    }

    private void escribir(String... lineas) throws IOException {
        Files.write(archivo, List.of(lineas), StandardCharsets.UTF_8);
    }

    private static String linea(String usuario, String nombre, String rol, String activo,
                                String... id) {
        String base = usuario + ";" + nombre + ";" + rol + ";" + activo + ";" + SAL + ";" + HASH;
        return id.length == 0 ? base : base + ";" + id[0];
    }

    private static String lineaRecuperacion(String usuario, String nombre, String rol,
                                            String activo, String id) {
        return lineaRecuperacion(usuario, nombre, rol, activo, id, "0", "0");
    }

    private static String lineaRecuperacion(String usuario, String nombre, String rol,
                                            String activo, String id, String intentos,
                                            String bloqueo) {
        String base = usuario + ";" + nombre + ";" + rol + ";" + activo + ";" + SAL + ";"
                + HASH + ";" + SAL_RESPUESTA + ";" + HASH_RESPUESTA + ";"
                + SAL_RESPUESTA + ";" + HASH_RESPUESTA + ";" + intentos + ";" + bloqueo;
        return id == null ? base : base + ";" + id;
    }
}

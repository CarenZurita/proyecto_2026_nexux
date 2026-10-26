package service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import model.GestorUsuarios;
import model.GestorUsuarios.FichaUsuario;
import model.Rol;
import model.Usuario;

/** Operaciones de la pantalla, con autorización comprobada en GestorUsuarios. */
public class UsuarioService {
    public List<FichaUsuario> listar(Usuario sesion) throws IOException {
        return GestorUsuarios.listarUsuarios(sesion);
    }
    public void guardar(Usuario sesion, Integer id, String nombre, String usuario, Rol rol,
                        char[] clave, char[] confirmacion) throws IOException, GeneralSecurityException {
        if (id == null) GestorUsuarios.registrarUsuario(sesion, nombre, usuario, rol, clave, confirmacion);
        else GestorUsuarios.actualizarUsuario(sesion, id, nombre, usuario, rol, clave, confirmacion);
    }
    public void cambiarEstado(Usuario sesion, int id, boolean activo) throws IOException {
        GestorUsuarios.cambiarEstadoUsuario(sesion, id, activo);
    }
}

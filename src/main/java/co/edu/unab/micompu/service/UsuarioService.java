package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import co.edu.unab.micompu.entity.Usuario;
import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.RolRepository;
import co.edu.unab.micompu.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final ConexionBD conexion;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService(ConexionBD conexion, UsuarioRepository usuarioRepository,
                         RolRepository rolRepository) {
        this.conexion = conexion;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    /** Devuelve todos los usuarios con su rol resuelto (asociación). */
    public List<Usuario> obtenerTodos() {
        try (Connection cn = conexion.obtener()) {
            List<Usuario> usuarios = usuarioRepository.findAll(cn);
            for (Usuario usuario : usuarios) {
                completarRol(cn, usuario);
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando los usuarios: " + e.getMessage(), e);
        }
    }

    /** Devuelve un usuario con su rol resuelto (404 si no existe). */
    public Usuario obtenerPorId(Integer idUsuario) {
        validarId(idUsuario);
        try (Connection cn = conexion.obtener()) {
            Usuario usuario = usuarioRepository.findById(cn, idUsuario)
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));
            completarRol(cn, usuario);
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el usuario " + idUsuario + ": " + e.getMessage(), e);
        }
    }

    /** Crea un usuario validando que el rol exista (transacción manual). */
    public Usuario crear(Usuario usuario) {
        validar(usuario);
        try (Connection cn = conexion.obtener()) {
            // Transacción manual: la validación del rol y el insert deben ser atómicos.
            cn.setAutoCommit(false);
            try {
                if (!rolRepository.exists(cn, usuario.getRol().getIdRol())) {
                    throw new NoSuchElementException(
                            "Rol no encontrado o inactivo: " + usuario.getRol().getIdRol());
                }
                Usuario creado = usuarioRepository.insert(cn, usuario);
                completarRol(cn, creado);
                cn.commit();
                return creado;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // UNIQUE (email) o FK (rol inexistente).
                    throw new IllegalStateException(
                            "Ya existe un usuario registrado con el correo " + usuario.getEmail() + ".", e);
                }
                throw new RuntimeException("Error creando el usuario: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza un usuario (404 si no existe; transacción manual). */
    public Usuario actualizar(Integer idUsuario, Usuario usuario) {
        validarId(idUsuario);
        validar(usuario);
        usuario.setIdUsuario(idUsuario);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                if (!rolRepository.exists(cn, usuario.getRol().getIdRol())) {
                    throw new NoSuchElementException(
                            "Rol no encontrado o inactivo: " + usuario.getRol().getIdRol());
                }
                if (!usuarioRepository.update(cn, usuario)) {
                    throw new NoSuchElementException("Usuario no encontrado: " + idUsuario);
                }
                completarRol(cn, usuario);
                cn.commit();
                return usuario;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    throw new IllegalStateException(
                            "Ya existe otro usuario registrado con el correo " + usuario.getEmail() + ".", e);
                }
                throw new RuntimeException("Error actualizando el usuario " + idUsuario + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Desactiva un usuario (borrado lógico: {@code estado = FALSE}; 404 si no
     * existe). Como la fila ya no se borra físicamente, la FK
     * {@code fk_sala_responsable} (ON DELETE RESTRICT) ya no bloquea la
     * operación: el usuario puede quedar inactivo aunque sea responsable de
     * salas (que seguirán resolviéndolo por su id). Sus reservas se conservan
     * como histórico: la tabla reserva no tiene columna {@code estado} y el
     * borrado lógico del usuario no exige tocarlas.
     */
    public void eliminar(Integer idUsuario) {
        validarId(idUsuario);
        try (Connection cn = conexion.obtener()) {
            if (!usuarioRepository.delete(cn, idUsuario)) {
                throw new NoSuchElementException("Usuario no encontrado: " + idUsuario);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando el usuario " + idUsuario + ": " + e.getMessage(), e);
        }
    }

    /** Completa la asociación usuario–rol cargando el rol completo. */
    private void completarRol(Connection cn, Usuario usuario) throws SQLException {
        Integer idRol = usuario.getRol() != null ? usuario.getRol().getIdRol() : null;
        if (idRol == null) {
            throw new IllegalStateException("El usuario " + usuario.getIdUsuario() + " no tiene rol asignado.");
        }
        usuario.setRol(rolRepository.findById(cn, idRol)
                .orElseThrow(() -> new NoSuchElementException("Rol no encontrado: " + idRol)));
    }

    private void validar(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El cuerpo de la petición (usuario) es obligatorio.");
        }
        if (usuario.getRol() == null || usuario.getRol().getIdRol() == null) {
            throw new IllegalArgumentException("El usuario requiere el rol al que pertenece (rol.idRol).");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo del usuario es obligatorio.");
        }
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio.");
        }
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("El hash de la contraseña es obligatorio.");
        }
    }

    private void validarId(Integer idUsuario) {
        if (idUsuario == null || idUsuario <= 0) {
            throw new IllegalArgumentException("El id del usuario debe ser un entero positivo.");
        }
    }
}

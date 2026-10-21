package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.unab.micompu.entity.Usuario;
import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.RolRepository;
import co.edu.unab.micompu.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private static final int ER_DUP_ENTRY = 1062;

    private final ConexionBD conexion;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(ConexionBD conexion, UsuarioRepository usuarioRepository,
            RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.conexion = conexion;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
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

    /** Devuelve un usuario con su rol resuelto (404 si no existe o está inactivo). */
    public Usuario obtenerPorId(Integer idUsuario) {
        validarId(idUsuario);
        try (Connection cn = conexion.obtener()) {
            Usuario usuario = usuarioRepository.findById(cn, idUsuario)
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));
            if (!usuario.isEstado()) {
                throw new NoSuchElementException("Usuario inactivo (borrado lógico): " + idUsuario);
            }
            completarRol(cn, usuario);
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el usuario " + idUsuario + ": " + e.getMessage(), e);
        }
    }

    /**
     * Crea un usuario cifrando su contraseña con BCrypt y
     * validando que el rol exista (transacción manual).
     */
    public Usuario crear(Usuario usuario) {
        validar(usuario);
        // BCrypt: la contraseña llega en texto plano y se guarda cifrada
        // (hash con sal aleatoria); el texto plano nunca llega a la base.
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuario.setNombre(usuario.getNombre().toLowerCase(Locale.ROOT));
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
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // Solo el 1062 (ER_DUP_ENTRY) es una clave duplicada
                    // NOT NULL u otras violaciones de integridad NO se reportan como correo duplicado.
                    if (e.getErrorCode() == ER_DUP_ENTRY) {
                        throw new IllegalStateException(
                                "Ya existe un usuario registrado con el correo " + usuario.getEmail() + ".", e);
                    }
                    throw new IllegalStateException(
                            "La operación viola una restricción de integridad: " + e.getMessage(), e);
                }
                throw new RuntimeException("Error creando el usuario: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza un usuario cifrando la contraseña que trae la petición (transacción manual). */
    public Usuario actualizar(Integer idUsuario, Usuario usuario) {
        validarId(idUsuario);
        validar(usuario);
        usuario.setIdUsuario(idUsuario);
        // BCrypt: la petición trae la contraseña en texto plano y se vuelve a
        // cifrar con sal nueva (aunque el cliente reenvíe la misma contraseña).
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuario.setNombre(usuario.getNombre().toLowerCase(Locale.ROOT));
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
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // Solo el 1062 (ER_DUP_ENTRY) es una clave duplicada
                    // NOT NULL u otras violaciones de integridad NO se reportan como correo duplicado.
                    if (e.getErrorCode() == ER_DUP_ENTRY) {
                        throw new IllegalStateException(
                                "Ya existe otro usuario registrado con el correo " + usuario.getEmail() + ".", e);
                    }
                    throw new IllegalStateException(
                            "La operación viola una restricción de integridad: " + e.getMessage(), e);
                }
                throw new RuntimeException("Error actualizando el usuario " + idUsuario + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
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

    /**
     * Inicia la sesión de un usuario verificando sus credenciales
     * contra la base de datos: 404 si el correo no está registrado
     * (o el usuario está inactivo por borrado lógico) y 400 
     * si la contraseña no coincide. La comparación se hace con
     * {@link PasswordEncoder#matches} contra el hash BCrypt almacenado:
     * nunca se compara texto plano ni se expone el hash.
     */
    public Usuario autenticar(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo de la sesión es obligatorio.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña de la sesión es obligatoria.");
        }
        try (Connection cn = conexion.obtener()) {
            Optional<Usuario> hallado = usuarioRepository.findByEmail(cn, email);
            if (hallado.isEmpty() || !hallado.get().isEstado()) {
                // Borrado lógico: para quien inicia sesión, un usuario inactivo ya no existe.
                throw new NoSuchElementException(
                        "No hay ningún usuario registrado con el correo " + email + ".");
            }
            Usuario usuario = hallado.get();
            // BCrypt: matches compara la contraseña recibida contra el hash
            // (y la sal) almacenado en password_hash, sin texto plano.
            if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
                throw new IllegalArgumentException("La contraseña es incorrecta.");
            }
            completarRol(cn, usuario);
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Error autenticando el usuario: " + e.getMessage(), e);
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
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
    }

    private void validarId(Integer idUsuario) {
        if (idUsuario == null || idUsuario <= 0) {
            throw new IllegalArgumentException("El id del usuario debe ser un entero positivo.");
        }
    }
}

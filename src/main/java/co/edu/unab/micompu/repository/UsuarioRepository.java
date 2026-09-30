package co.edu.unab.micompu.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import co.edu.unab.micompu.entity.Rol;
import co.edu.unab.micompu.entity.Usuario;

@Repository
public class UsuarioRepository {

    private static final String SQL_SELECT =
            "SELECT id_usuario, id_rol, email, nombre, password_hash, estado FROM usuario";
    private static final String SQL_INSERT =
            "INSERT INTO usuario (id_rol, email, nombre, password_hash, estado) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE usuario SET id_rol = ?, email = ?, nombre = ?, password_hash = ?, estado = ?"
                    + " WHERE id_usuario = ?";
    private static final String SQL_DELETE =
            "DELETE FROM usuario WHERE id_usuario = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM usuario WHERE id_usuario = ?";

    /** Devuelve todos los usuarios. */
    public List<Usuario> findAll(Connection cn) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " ORDER BY id_usuario");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        }
        return usuarios;
    }

    /** Busca un usuario por su clave primaria {@code id_usuario}. */
    public Optional<Usuario> findById(Connection cn, int idUsuario) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Inserta un usuario y devuelve la entidad con la llave generada. */
    public Usuario insert(Connection cn, Usuario usuario) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuario.getRol().getIdRol());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getNombre());
            ps.setString(4, usuario.getPasswordHash());
            ps.setBoolean(5, usuario.isEstado());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    usuario.setIdUsuario(llaves.getInt(1));
                }
            }
        }
        return usuario;
    }

    /** Actualiza un usuario por su {@code id_usuario}; indica si la fila existía. */
    public boolean update(Connection cn, Usuario usuario) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, usuario.getRol().getIdRol());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getNombre());
            ps.setString(4, usuario.getPasswordHash());
            ps.setBoolean(5, usuario.isEstado());
            ps.setInt(6, usuario.getIdUsuario());
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina un usuario por su {@code id_usuario}; indica si la fila existía. */
    public boolean delete(Connection cn, int idUsuario) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    /** Indica si existe un usuario con el id dado. */
    public boolean exists(Connection cn, int idUsuario) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mapeo manual de una fila del {@link ResultSet} al POJO {@link Usuario}. */
    private Usuario mapear(ResultSet rs) throws SQLException {
        return Usuario.builder()
                .idUsuario(rs.getInt("id_usuario"))
                // FK id_rol -> referencia mínima al rol (la asocia el servicio).
                .rol(Rol.builder().idRol(rs.getInt("id_rol")).build())
                .email(rs.getString("email"))
                .nombre(rs.getString("nombre"))
                .passwordHash(rs.getString("password_hash"))
                .estado(rs.getBoolean("estado"))
                .build();
    }
}

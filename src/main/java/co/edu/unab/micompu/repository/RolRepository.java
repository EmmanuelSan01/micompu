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

@Repository
public class RolRepository {

    private static final String SQL_SELECT =
            "SELECT id_rol, nombre, estado FROM rol";
    private static final String SQL_INSERT =
            "INSERT INTO rol (nombre, estado) VALUES (?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE rol SET nombre = ?, estado = ? WHERE id_rol = ?";
    private static final String SQL_DELETE =
            "DELETE FROM rol WHERE id_rol = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM rol WHERE id_rol = ?";

    /** Devuelve todos los roles. */
    public List<Rol> findAll(Connection cn) throws SQLException {
        List<Rol> roles = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " ORDER BY id_rol");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapear(rs));
            }
        }
        return roles;
    }

    /** Busca un rol por su clave primaria {@code id_rol}. */
    public Optional<Rol> findById(Connection cn, int idRol) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_rol = ?")) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Inserta un rol y devuelve la entidad con la llave generada. */
    public Rol insert(Connection cn, Rol rol) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rol.getNombre());
            ps.setBoolean(2, rol.isEstado());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    rol.setIdRol(llaves.getInt(1));
                }
            }
        }
        return rol;
    }

    /** Actualiza un rol por su {@code id_rol}; indica si la fila existía. */
    public boolean update(Connection cn, Rol rol) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, rol.getNombre());
            ps.setBoolean(2, rol.isEstado());
            ps.setInt(3, rol.getIdRol());
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina un rol por su {@code id_rol}; indica si la fila existía. */
    public boolean delete(Connection cn, int idRol) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idRol);
            return ps.executeUpdate() > 0;
        }
    }

    /** Indica si existe un rol con el id dado. */
    public boolean exists(Connection cn, int idRol) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mapeo manual de una fila del {@link ResultSet} al POJO {@link Rol}. */
    private Rol mapear(ResultSet rs) throws SQLException {
        return Rol.builder()
                .idRol(rs.getInt("id_rol"))
                .nombre(rs.getString("nombre"))
                .estado(rs.getBoolean("estado"))
                .build();
    }
}

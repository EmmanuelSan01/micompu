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
import co.edu.unab.micompu.entity.Sala;
import co.edu.unab.micompu.entity.Usuario;

@Repository
public class SalaRepository {

    private static final String SQL_SELECT =
            "SELECT id_sala, id_responsable, nombre, capacidad, estado FROM sala";
    private static final String SQL_INSERT =
            "INSERT INTO sala (id_responsable, nombre, capacidad, estado) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE sala SET id_responsable = ?, nombre = ?, capacidad = ?, estado = ?"
                    + " WHERE id_sala = ?";
    private static final String SQL_DELETE =
            "DELETE FROM sala WHERE id_sala = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM sala WHERE id_sala = ?";

    /** Devuelve todas las salas. */
    public List<Sala> findAll(Connection cn) throws SQLException {
        List<Sala> salas = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " ORDER BY id_sala");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                salas.add(mapear(rs));
            }
        }
        return salas;
    }

    /** Busca una sala por su clave primaria {@code id_sala}. */
    public Optional<Sala> findById(Connection cn, int idSala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_sala = ?")) {
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Inserta una sala y devuelve la entidad con la llave generada. */
    public Sala insert(Connection cn, Sala sala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sala.getResponsable().getIdUsuario());
            ps.setString(2, sala.getNombre());
            ps.setByte(3, sala.getCapacidad());
            ps.setBoolean(4, sala.isEstado());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    sala.setIdSala(llaves.getInt(1));
                }
            }
        }
        return sala;
    }

    /** Actualiza una sala por su {@code id_sala}; indica si la fila existía. */
    public boolean update(Connection cn, Sala sala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, sala.getResponsable().getIdUsuario());
            ps.setString(2, sala.getNombre());
            ps.setByte(3, sala.getCapacidad());
            ps.setBoolean(4, sala.isEstado());
            ps.setInt(5, sala.getIdSala());
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina una sala por su {@code id_sala}; indica si la fila existía. */
    public boolean delete(Connection cn, int idSala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idSala);
            return ps.executeUpdate() > 0;
        }
    }

    /** Indica si existe una sala con el id dado. */
    public boolean exists(Connection cn, int idSala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mapeo manual de una fila del {@link ResultSet} al POJO {@link Sala}. */
    private Sala mapear(ResultSet rs) throws SQLException {
        return Sala.builder()
                .idSala(rs.getInt("id_sala"))
                // FK id_responsable -> referencia mínima (la asocia el servicio).
                .responsable(Usuario.builder().idUsuario(rs.getInt("id_responsable")).build())
                .nombre(rs.getString("nombre"))
                .capacidad(rs.getByte("capacidad"))
                .estado(rs.getBoolean("estado"))
                .build();
    }
}

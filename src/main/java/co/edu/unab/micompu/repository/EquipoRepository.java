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
import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.Sala;

@Repository
public class EquipoRepository {

    private static final String SQL_SELECT =
            "SELECT id_equipo, id_sala, estado FROM equipo";
    private static final String SQL_INSERT =
            "INSERT INTO equipo (id_sala, estado) VALUES (?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE equipo SET id_sala = ?, estado = ? WHERE id_equipo = ?";
    private static final String SQL_DELETE =
            "DELETE FROM equipo WHERE id_equipo = ?";
    private static final String SQL_DELETE_BY_SALA =
            "DELETE FROM equipo WHERE id_sala = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM equipo WHERE id_equipo = ?";

    /** Devuelve todos los equipos. */
    public List<Equipo> findAll(Connection cn) throws SQLException {
        List<Equipo> equipos = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " ORDER BY id_equipo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                equipos.add(mapear(rs));
            }
        }
        return equipos;
    }

    /** Busca un equipo por su clave primaria {@code id_equipo}. */
    public Optional<Equipo> findById(Connection cn, int idEquipo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_equipo = ?")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Devuelve los equipos (partes) de una sala (todo). */
    public List<Equipo> findBySala(Connection cn, int idSala) throws SQLException {
        List<Equipo> equipos = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_sala = ? ORDER BY id_equipo")) {
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipos.add(mapear(rs));
                }
            }
        }
        return equipos;
    }

    /** Inserta un equipo y devuelve la entidad con la llave generada. */
    public Equipo insert(Connection cn, Equipo equipo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, equipo.getSala().getIdSala());
            ps.setBoolean(2, equipo.isEstado());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    equipo.setIdEquipo(llaves.getInt(1));
                }
            }
        }
        return equipo;
    }

    /** Actualiza un equipo por su {@code id_equipo}; indica si la fila existía. */
    public boolean update(Connection cn, Equipo equipo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, equipo.getSala().getIdSala());
            ps.setBoolean(2, equipo.isEstado());
            ps.setInt(3, equipo.getIdEquipo());
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina un equipo por su {@code id_equipo}; indica si la fila existía. */
    public boolean delete(Connection cn, int idEquipo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idEquipo);
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina todos los equipos de una sala (composición); devuelve las filas borradas. */
    public int deleteBySala(Connection cn, int idSala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE_BY_SALA)) {
            ps.setInt(1, idSala);
            return ps.executeUpdate();
        }
    }

    /** Indica si existe un equipo con el id dado. */
    public boolean exists(Connection cn, int idEquipo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mapeo manual de una fila del {@link ResultSet} al POJO {@link Equipo}. */
    private Equipo mapear(ResultSet rs) throws SQLException {
        return Equipo.builder()
                .idEquipo(rs.getInt("id_equipo"))
                // FK id_sala -> referencia mínima a la sala (la asocia el servicio).
                .sala(Sala.builder().idSala(rs.getInt("id_sala")).build())
                .estado(rs.getBoolean("estado"))
                .build();
    }
}

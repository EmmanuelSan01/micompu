package co.edu.unab.micompu.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import co.edu.unab.micompu.entity.FranjaClase;
import co.edu.unab.micompu.entity.Sala;

@Repository
public class FranjaClaseRepository {

    private static final String SQL_SELECT =
            "SELECT id_franja_clase, id_sala, dia_semana, hora_inicio, hora_fin, motivo, estado"
                    + " FROM franja_clase";
    private static final String SQL_INSERT =
            "INSERT INTO franja_clase (id_sala, dia_semana, hora_inicio, hora_fin, motivo, estado)"
                    + " VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE franja_clase SET id_sala = ?, dia_semana = ?, hora_inicio = ?, hora_fin = ?,"
                    + " motivo = ?, estado = ? WHERE id_franja_clase = ?";
    private static final String SQL_DELETE =
            "DELETE FROM franja_clase WHERE id_franja_clase = ?";
    private static final String SQL_DELETE_BY_SALA =
            "DELETE FROM franja_clase WHERE id_sala = ?";
    private static final String SQL_EXISTS =
            "SELECT 1 FROM franja_clase WHERE id_franja_clase = ?";
    private static final String SQL_SOLAPADA =
            "SELECT 1 FROM franja_clase WHERE id_sala = ? AND dia_semana = ? AND estado = 1"
                    + " AND hora_inicio < ? AND hora_fin > ?";

    /** Devuelve todas las franjas de clase. */
    public List<FranjaClase> findAll(Connection cn) throws SQLException {
        List<FranjaClase> franjas = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " ORDER BY id_sala, dia_semana, hora_inicio");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                franjas.add(mapear(rs));
            }
        }
        return franjas;
    }

    /** Busca una franja por su clave primaria {@code id_franja_clase}. */
    public Optional<FranjaClase> findById(Connection cn, int idFranjaClase) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_franja_clase = ?")) {
            ps.setInt(1, idFranjaClase);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Devuelve las franjas (partes) del horario de una sala (todo). */
    public List<FranjaClase> findBySala(Connection cn, int idSala) throws SQLException {
        List<FranjaClase> franjas = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_sala = ?"
                + " ORDER BY dia_semana, hora_inicio")) {
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    franjas.add(mapear(rs));
                }
            }
        }
        return franjas;
    }

    /** Inserta una franja y devuelve la entidad con la llave generada. */
    public FranjaClase insert(Connection cn, FranjaClase franja) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, franja.getSala().getIdSala());
            ps.setByte(2, franja.getDiaSemana());
            ps.setTime(3, Time.valueOf(franja.getHoraInicio()));
            ps.setTime(4, Time.valueOf(franja.getHoraFin()));
            ps.setString(5, franja.getMotivo());
            ps.setBoolean(6, franja.isEstado());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    franja.setIdFranjaClase(llaves.getInt(1));
                }
            }
        }
        return franja;
    }

    /** Actualiza una franja por su {@code id_franja_clase}; indica si la fila existía. */
    public boolean update(Connection cn, FranjaClase franja) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, franja.getSala().getIdSala());
            ps.setByte(2, franja.getDiaSemana());
            ps.setTime(3, Time.valueOf(franja.getHoraInicio()));
            ps.setTime(4, Time.valueOf(franja.getHoraFin()));
            ps.setString(5, franja.getMotivo());
            ps.setBoolean(6, franja.isEstado());
            ps.setInt(7, franja.getIdFranjaClase());
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina una franja por su {@code id_franja_clase}; indica si la fila existía. */
    public boolean delete(Connection cn, int idFranjaClase) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idFranjaClase);
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina todas las franjas de una sala (composición); devuelve las filas borradas. */
    public int deleteBySala(Connection cn, int idSala) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE_BY_SALA)) {
            ps.setInt(1, idSala);
            return ps.executeUpdate();
        }
    }

    /** Indica si existe una franja con el id dado. */
    public boolean exists(Connection cn, int idFranjaClase) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, idFranjaClase);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Indica si la sala ya tiene una franja de clase activa que se cruza con el horario dado. */
    public boolean existsSolapada(Connection cn, int idSala, int diaSemana,
                                  LocalTime horaInicio, LocalTime horaFin,
                                  Integer excluirIdFranja) throws SQLException {
        String sql = SQL_SOLAPADA;
        boolean excluir = excluirIdFranja != null;
        if (excluir) {
            sql = sql + " AND id_franja_clase <> ?";
        }
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ps.setInt(2, diaSemana);
            ps.setTime(3, Time.valueOf(horaFin));      // hora_inicio existente < horaFin nueva
            ps.setTime(4, Time.valueOf(horaInicio));   // hora_fin existente > horaInicio nueva
            if (excluir) {
                ps.setInt(5, excluirIdFranja);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mapeo manual de una fila del {@link ResultSet} al POJO {@link FranjaClase}. */
    private FranjaClase mapear(ResultSet rs) throws SQLException {
        return FranjaClase.builder()
                .idFranjaClase(rs.getInt("id_franja_clase"))
                // FK id_sala -> referencia mínima a la sala (la asocia el servicio).
                .sala(Sala.builder().idSala(rs.getInt("id_sala")).build())
                .diaSemana(rs.getByte("dia_semana"))
                .horaInicio(rs.getTime("hora_inicio").toLocalTime())
                .horaFin(rs.getTime("hora_fin").toLocalTime())
                .motivo(rs.getString("motivo"))
                .estado(rs.getBoolean("estado"))
                .build();
    }
}

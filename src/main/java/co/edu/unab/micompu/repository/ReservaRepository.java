package co.edu.unab.micompu.repository;

import java.sql.*;

import java.time.LocalDate;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.Reserva;
import co.edu.unab.micompu.entity.Usuario;

@Repository
public class ReservaRepository {

    private static final String SQL_SELECT
            = "SELECT id_reserva, id_usuario, id_equipo, fecha, hora_inicio, hora_fin FROM reserva";
    private static final String SQL_INSERT
            = "INSERT INTO reserva (id_usuario, id_equipo, fecha, hora_inicio, hora_fin)"
            + " VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE
            = "UPDATE reserva SET id_usuario = ?, id_equipo = ?, fecha = ?, hora_inicio = ?,"
            + " hora_fin = ? WHERE id_reserva = ?";
    /**
     * Borrado físico de reservas: la tabla {@code reserva} no tiene columna
     * {@code estado} en el DDL, por lo que su eliminación no puede ser lógica.
     */
    private static final String SQL_DELETE
            = "DELETE FROM reserva WHERE id_reserva = ?";
    private static final String SQL_EXISTS
            = "SELECT 1 FROM reserva WHERE id_reserva = ?";
    private static final String SQL_SOLAPADA
            = "SELECT 1 FROM reserva WHERE id_equipo = ? AND fecha = ?"
            + " AND hora_inicio < ? AND hora_fin > ?";

    /** Devuelve todas las reservas. */
    public List<Reserva> findAll(Connection cn) throws SQLException {
        List<Reserva> reservas = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " ORDER BY fecha, hora_inicio"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reservas.add(mapear(rs));
            }
        }
        return reservas;
    }

    /** Busca una reserva por su clave primaria {@code id_reserva}. */
    public Optional<Reserva> findById(Connection cn, int idReserva) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE id_reserva = ?")) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Inserta una reserva y devuelve la entidad con la llave generada. */
    public Reserva insert(Connection cn, Reserva reserva) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reserva.getUsuario().getIdUsuario());
            ps.setInt(2, reserva.getEquipo().getIdEquipo());
            ps.setDate(3, java.sql.Date.valueOf(reserva.getFecha()));
            ps.setTime(4, Time.valueOf(reserva.getHoraInicio()));
            ps.setTime(5, Time.valueOf(reserva.getHoraFin()));
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    reserva.setIdReserva(llaves.getInt(1));
                }
            }
        }
        return reserva;
    }

    /** Actualiza una reserva por su {@code id_reserva}; indica si la fila existía. */
    public boolean update(Connection cn, Reserva reserva) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, reserva.getUsuario().getIdUsuario());
            ps.setInt(2, reserva.getEquipo().getIdEquipo());
            ps.setDate(3, java.sql.Date.valueOf(reserva.getFecha()));
            ps.setTime(4, Time.valueOf(reserva.getHoraInicio()));
            ps.setTime(5, Time.valueOf(reserva.getHoraFin()));
            ps.setInt(6, reserva.getIdReserva());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Borrado físico de una reserva por su {@code id_reserva}; indica si la
     * fila existía. A diferencia del resto de tablas, {@code reserva} no
     * tiene columna {@code estado} en el DDL, por lo que su eliminación no
     * puede ser lógica.
     */
    public boolean delete(Connection cn, int idReserva) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idReserva);
            return ps.executeUpdate() > 0;
        }
    }

    /** Indica si existe una reserva con el id dado. */
    public boolean exists(Connection cn, int idReserva) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Indica si el equipo ya tiene una reserva que se cruza con el horario. */
    public boolean existsSolapada(Connection cn, int idEquipo, LocalDate fecha,
                                  LocalTime horaInicio, LocalTime horaFin,
                                  Integer excluirIdReserva) throws SQLException {
        String sql = SQL_SOLAPADA;
        boolean excluir = excluirIdReserva != null;
        if (excluir) {
            sql = sql + " AND id_reserva <> ?";
        }
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            ps.setDate(2, java.sql.Date.valueOf(fecha));
            ps.setTime(3, Time.valueOf(horaFin));      // hora_inicio existente < horaFin nueva
            ps.setTime(4, Time.valueOf(horaInicio));   // hora_fin existente > horaInicio nueva
            if (excluir) {
                ps.setInt(5, excluirIdReserva);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mapeo manual de una fila del {@link ResultSet} al POJO {@link Reserva}. */
    private Reserva mapear(ResultSet rs) throws SQLException {
        return Reserva.builder()
                .idReserva(rs.getInt("id_reserva"))
                // FK id_usuario / id_equipo -> referencias mínimas (las asocia el servicio).
                .usuario(Usuario.builder().idUsuario(rs.getInt("id_usuario")).build())
                .equipo(Equipo.builder().idEquipo(rs.getInt("id_equipo")).build())
                .fecha(rs.getDate("fecha").toLocalDate())
                .horaInicio(rs.getTime("hora_inicio").toLocalTime())
                .horaFin(rs.getTime("hora_fin").toLocalTime())
                .build();
    }
}

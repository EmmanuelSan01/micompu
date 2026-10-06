package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.Reserva;
import co.edu.unab.micompu.entity.Sala;
import co.edu.unab.micompu.entity.Usuario;
import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.EquipoRepository;
import co.edu.unab.micompu.repository.FranjaClaseRepository;
import co.edu.unab.micompu.repository.ReservaRepository;
import co.edu.unab.micompu.repository.RolRepository;
import co.edu.unab.micompu.repository.SalaRepository;
import co.edu.unab.micompu.repository.UsuarioRepository;

@Service
public class ReservaService {

    private final ConexionBD conexion;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EquipoRepository equipoRepository;
    private final SalaRepository salaRepository;
    private final FranjaClaseRepository franjaClaseRepository;

    public ReservaService(ConexionBD conexion, ReservaRepository reservaRepository,
                          UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          EquipoRepository equipoRepository, SalaRepository salaRepository,
                          FranjaClaseRepository franjaClaseRepository) {
        this.conexion = conexion;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.equipoRepository = equipoRepository;
        this.salaRepository = salaRepository;
        this.franjaClaseRepository = franjaClaseRepository;
    }

    /** Devuelve todas las reservas con sus relaciones completas. */
    public List<Reserva> obtenerTodos() {
        try (Connection cn = conexion.obtener()) {
            List<Reserva> reservas = reservaRepository.findAll(cn);
            for (Reserva reserva : reservas) {
                completarRelaciones(cn, reserva);
            }
            return reservas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando las reservas: " + e.getMessage(), e);
        }
    }

    /** Devuelve una reserva con sus relaciones completas (404 si no existe). */
    public Reserva obtenerPorId(Integer idReserva) {
        validarId(idReserva);
        try (Connection cn = conexion.obtener()) {
            Reserva reserva = reservaRepository.findById(cn, idReserva)
                    .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada: " + idReserva));
            completarRelaciones(cn, reserva);
            return reserva;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la reserva " + idReserva + ": " + e.getMessage(), e);
        }
    }

    /**
     * Crea una reserva validando todas las reglas de negocio dentro de una
     * transacción manual (commit si todo sale bien; rollback si algo falla).
     */
    public Reserva crear(Reserva reserva) {
        validar(reserva);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                int idUsuario = reserva.getUsuario().getIdUsuario();
                int idEquipo = reserva.getEquipo().getIdEquipo();

                Usuario usuario = usuarioRepository.findById(cn, idUsuario)
                        .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));
                if (!usuario.isEstado()) {
                    // Regla de negocio ante el borrado lógico: un usuario inactivo no puede reservar.
                    throw new IllegalStateException("El usuario " + idUsuario + " está inactivo.");
                }
                Equipo equipo = equipoRepository.findById(cn, idEquipo)
                        .orElseThrow(() -> new NoSuchElementException("Equipo no encontrado: " + idEquipo));
                if (!equipo.isEstado()) {
                    throw new IllegalStateException("El equipo " + idEquipo + " está inactivo.");
                }
                if (reservaRepository.existsSolapada(cn, idEquipo, reserva.getFecha(),
                        reserva.getHoraInicio(), reserva.getHoraFin(), null)) {
                    throw new IllegalStateException("El equipo ya tiene una reserva para ese día y horario.");
                }
                int diaSemana = reserva.getFecha().getDayOfWeek().getValue(); // 1 = lunes … 7 = domingo
                int idSala = equipo.getSala().getIdSala();
                if (franjaClaseRepository.existsSolapada(cn, idSala, diaSemana,
                        reserva.getHoraInicio(), reserva.getHoraFin(), null)) {
                    throw new IllegalStateException(
                            "El horario solicitado se cruza con una franja de clase de la sala.");
                }

                Reserva creada = reservaRepository.insert(cn, reserva);
                completarRelaciones(cn, creada);
                cn.commit();
                return creada;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // FK fk_reserva_usuario / fk_reserva_equipo.
                    throw new IllegalStateException("La reserva referencia un usuario o equipo inexistente.", e);
                }
                throw new RuntimeException("Error creando la reserva: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza una reserva revalidando las reglas de negocio (transacción manual). */
    public Reserva actualizar(Integer idReserva, Reserva reserva) {
        validarId(idReserva);
        validar(reserva);
        reserva.setIdReserva(idReserva);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                int idUsuario = reserva.getUsuario().getIdUsuario();
                int idEquipo = reserva.getEquipo().getIdEquipo();

                Usuario usuario = usuarioRepository.findById(cn, idUsuario)
                        .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));
                if (!usuario.isEstado()) {
                    // Regla de negocio ante el borrado lógico: un usuario inactivo no puede reservar.
                    throw new IllegalStateException("El usuario " + idUsuario + " está inactivo.");
                }
                Equipo equipo = equipoRepository.findById(cn, idEquipo)
                        .orElseThrow(() -> new NoSuchElementException("Equipo no encontrado: " + idEquipo));
                if (!equipo.isEstado()) {
                    throw new IllegalStateException("El equipo " + idEquipo + " está inactivo.");
                }
                if (reservaRepository.existsSolapada(cn, idEquipo, reserva.getFecha(),
                        reserva.getHoraInicio(), reserva.getHoraFin(), idReserva)) {
                    throw new IllegalStateException("El equipo ya tiene una reserva para ese día y horario.");
                }
                int diaSemana = reserva.getFecha().getDayOfWeek().getValue();
                int idSala = equipo.getSala().getIdSala();
                if (franjaClaseRepository.existsSolapada(cn, idSala, diaSemana,
                        reserva.getHoraInicio(), reserva.getHoraFin(), null)) {
                    throw new IllegalStateException(
                            "El horario solicitado se cruza con una franja de clase de la sala.");
                }
                if (!reservaRepository.update(cn, reserva)) {
                    throw new NoSuchElementException("Reserva no encontrada: " + idReserva);
                }
                completarRelaciones(cn, reserva);
                cn.commit();
                return reserva;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    throw new IllegalStateException("La reserva referencia un usuario o equipo inexistente.", e);
                }
                throw new RuntimeException("Error actualizando la reserva " + idReserva + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina una reserva (404 si no existe). Su borrado es físico —no
     * lógico— porque la tabla {@code reserva} no tiene columna {@code estado}
     * en el DDL. El equipo reservado permanece (agregación).
     */
    public void eliminar(Integer idReserva) {
        validarId(idReserva);
        try (Connection cn = conexion.obtener()) {
            if (!reservaRepository.delete(cn, idReserva)) {
                throw new NoSuchElementException("Reserva no encontrada: " + idReserva);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando la reserva " + idReserva + ": " + e.getMessage(), e);
        }
    }

    /**
     * Completa el mapeo de las relaciones de la reserva: la asociación con el
     * usuario (y su rol) y la agregación con el equipo (y su sala completa).
     */
    private void completarRelaciones(Connection cn, Reserva reserva) throws SQLException {
        Usuario usuario = usuarioRepository.findById(cn, reserva.getUsuario().getIdUsuario())
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuario de la reserva no encontrado: " + reserva.getUsuario().getIdUsuario()));
        usuario.setRol(rolRepository.findById(cn, usuario.getRol().getIdRol())
                .orElseThrow(() -> new NoSuchElementException(
                        "Rol no encontrado: " + usuario.getRol().getIdRol())));
        reserva.setUsuario(usuario);

        Equipo equipo = equipoRepository.findById(cn, reserva.getEquipo().getIdEquipo())
                .orElseThrow(() -> new NoSuchElementException(
                        "Equipo de la reserva no encontrado: " + reserva.getEquipo().getIdEquipo()));
        completarSalaDelEquipo(cn, equipo);
        reserva.setEquipo(equipo);
    }

    /** Completa la composición equipo–sala (sala + responsable + rol del responsable). */
    private void completarSalaDelEquipo(Connection cn, Equipo equipo) throws SQLException {
        Sala sala = salaRepository.findById(cn, equipo.getSala().getIdSala())
                .orElseThrow(() -> new NoSuchElementException(
                        "Sala no encontrada: " + equipo.getSala().getIdSala()));
        Usuario responsable = usuarioRepository.findById(cn, sala.getResponsable().getIdUsuario())
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuario responsable no encontrado: " + sala.getResponsable().getIdUsuario()));
        responsable.setRol(rolRepository.findById(cn, responsable.getRol().getIdRol())
                .orElseThrow(() -> new NoSuchElementException(
                        "Rol no encontrado: " + responsable.getRol().getIdRol())));
        sala.setResponsable(responsable);
        equipo.setSala(sala);
    }

    private void validar(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("El cuerpo de la petición (reserva) es obligatorio.");
        }
        if (reserva.getUsuario() == null || reserva.getUsuario().getIdUsuario() == null) {
            throw new IllegalArgumentException("La reserva requiere el usuario que la realiza (usuario.idUsuario).");
        }
        if (reserva.getEquipo() == null || reserva.getEquipo().getIdEquipo() == null) {
            throw new IllegalArgumentException("La reserva requiere el equipo a reservar (equipo.idEquipo).");
        }
        if (reserva.getFecha() == null) {
            throw new IllegalArgumentException("La fecha de la reserva es obligatoria.");
        }
        if (reserva.getHoraInicio() == null || reserva.getHoraFin() == null) {
            throw new IllegalArgumentException("Las horas de inicio y fin de la reserva son obligatorias.");
        }
        if (!reserva.getHoraFin().isAfter(reserva.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin de la reserva debe ser posterior a la de inicio.");
        }
    }

    private void validarId(Integer idReserva) {
        if (idReserva == null || idReserva <= 0) {
            throw new IllegalArgumentException("El id de la reserva debe ser un entero positivo.");
        }
    }
}

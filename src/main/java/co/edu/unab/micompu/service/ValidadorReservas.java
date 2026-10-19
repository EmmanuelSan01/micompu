package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.stereotype.Component;

import co.edu.unab.micompu.config.ConfiguracionReservas;
import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.Reserva;
import co.edu.unab.micompu.repository.FranjaClaseRepository;
import co.edu.unab.micompu.repository.ReservaRepository;

/**
 * Las 15 validaciones de negocio del proceso de reservas, en su orden oficial.
 */
@Component
public class ValidadorReservas {

    /** Inicio del bloque de almuerzo, fijo en la regla de negocio (no forma parte de la configuración). */
    private static final LocalTime ALMUERZO_INICIO = LocalTime.parse("13:00");

    /** Fin del bloque de almuerzo: la sala vuelve a estar disponible exactamente a las 14:00. */
    private static final LocalTime ALMUERZO_FIN = LocalTime.parse("14:00");

    private final ConfiguracionReservas configuracion;
    private final FranjaClaseRepository franjaClaseRepository;
    private final ReservaRepository reservaRepository;

    public ValidadorReservas(ConfiguracionReservas configuracion,
            FranjaClaseRepository franjaClaseRepository,
            ReservaRepository reservaRepository) {
        this.configuracion = configuracion;
        this.franjaClaseRepository = franjaClaseRepository;
        this.reservaRepository = reservaRepository;
    }

    /**
     * Validaciones 1 a 11 (locales, sin base de datos). Requiere que
     * {@code ReservaService.validar} ya haya comprobado la forma de la
     * reserva (campos obligatorios y hora de fin posterior a la de inicio).
     */
    public void validarLocales(Reserva reserva) {
        LocalDate fecha = reserva.getFecha();
        LocalTime horaInicio = reserva.getHoraInicio();
        LocalTime horaFin = reserva.getHoraFin();
        LocalDate hoy = LocalDate.now();
        Duration duracion = Duration.between(horaInicio, horaFin);

        validarFechaInicioSemestre(fecha);          // 01
        validarFechaNoPasada(fecha, hoy);           // 02
        validarDiasAnticipacion(fecha, hoy);        // 03
        validarFechaCierreSemestre(fecha);          // 04
        validarDiaNoBloqueado(fecha);               // 05
        validarFechaNoBloqueada(fecha);             // 06
        validarHoraApertura(horaInicio);            // 07
        validarHoraCierre(horaFin);                 // 08
        validarDuracionMinima(duracion);            // 09
        validarDuracionMaxima(duracion);            // 10
        validarBloqueAlmuerzo(horaInicio, horaFin); // 11
    }

    /**
     * Validaciones 12 a 15 (contra la base de datos). Debe ejecutarse
     * dentro de la transacción de la reserva, después de que el
     * service haya bloqueado y leído la fila del equipo.
     */
    public void validarContraBaseDeDatos(Connection cn, Reserva reserva, Equipo equipo,
            Integer idReservaExcluida) throws SQLException {
        validarFranjaDeClase(cn, reserva, equipo);                    // 12
        validarEquipoActivo(equipo);                                  // 13
        validarMaximoReservasActivas(cn, reserva, idReservaExcluida); // 14
        validarReservaSolapada(cn, reserva, idReservaExcluida);       // 15
    }

    /** 1. La fecha no es anterior al inicio del semestre. */
    private void validarFechaInicioSemestre(LocalDate fecha) {
        if (fecha.isBefore(configuracion.getFechaInicioSemestre())) {
            throw new IllegalArgumentException("La fecha de la reserva no puede ser anterior al inicio del semestre ("
                    + configuracion.getFechaInicioSemestre() + ").");
        }
    }

    /** 2. La fecha no está en el pasado. */
    private void validarFechaNoPasada(LocalDate fecha, LocalDate hoy) {
        if (fecha.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de la reserva no puede estar en el pasado.");
        }
    }

    /** 3. La fecha no excede la anticipación máxima. */
    private void validarDiasAnticipacion(LocalDate fecha, LocalDate hoy) {
        LocalDate fechaLimite = hoy.plusDays(configuracion.getMaxDiasAnticipacion());
        if (fecha.isAfter(fechaLimite)) {
            throw new IllegalArgumentException("La reserva debe hacerse con máximo "
                    + configuracion.getMaxDiasAnticipacion() + " días de anticipación (hasta el " + fechaLimite + ").");
        }
    }

    /** 4. La fecha no es posterior al cierre del semestre. */
    private void validarFechaCierreSemestre(LocalDate fecha) {
        if (fecha.isAfter(configuracion.getFechaCierreSemestre())) {
            throw new IllegalArgumentException("La fecha de la reserva no puede ser posterior al cierre del semestre ("
                    + configuracion.getFechaCierreSemestre() + ").");
        }
    }

    /** 5. El día de la semana de la fecha no está en los días bloqueados. */
    private void validarDiaNoBloqueado(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (configuracion.getDiasBloqueados().contains(dia.getValue())) {
            throw new IllegalArgumentException("No se permiten reservas el día "
                    + dia.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es")) + ".");
        }
    }

    /** 6. La fecha no es una de las fechas bloqueadas (días festivos). */
    private void validarFechaNoBloqueada(LocalDate fecha) {
        if (configuracion.getFechasBloqueadas().contains(fecha)) {
            throw new IllegalArgumentException("La fecha " + fecha + " está bloqueada (día festivo).");
        }
    }

    /** 7. La hora de inicio no es anterior a la hora de apertura. */
    private void validarHoraApertura(LocalTime horaInicio) {
        if (horaInicio.isBefore(configuracion.getHoraApertura())) {
            throw new IllegalArgumentException("La hora de inicio de la reserva no puede ser anterior a la hora de"
                    + " apertura (" + configuracion.getHoraApertura() + ").");
        }
    }

    /** 8. La hora de fin no es posterior a la hora de cierre. */
    private void validarHoraCierre(LocalTime horaFin) {
        if (horaFin.isAfter(configuracion.getHoraCierre())) {
            throw new IllegalArgumentException("La hora de fin de la reserva no puede ser posterior a la hora de"
                    + " cierre (" + configuracion.getHoraCierre() + ").");
        }
    }

    /** 9. La duración percibida (hora de fin menos hora de inicio) alcanza el mínimo. */
    private void validarDuracionMinima(Duration duracion) {
        if (duracion.compareTo(Duration.ofMinutes(configuracion.getMinDuracionMinutos())) < 0) {
            throw new IllegalArgumentException("La reserva debe durar al menos "
                    + configuracion.getMinDuracionMinutos() + " minutos.");
        }
    }

    /** 10. La duración percibida no excede el máximo. */
    private void validarDuracionMaxima(Duration duracion) {
        if (duracion.compareTo(Duration.ofHours(configuracion.getMaxDuracionHoras())) > 0) {
            throw new IllegalArgumentException("La reserva no puede durar más de "
                    + configuracion.getMaxDuracionHoras() + " horas.");
        }
    }

    /** 11. La franja no se cruza con el bloque de almuerzo {@code [13:00, 14:00)}. */
    private void validarBloqueAlmuerzo(LocalTime horaInicio, LocalTime horaFin) {
        boolean seCruza = horaInicio.isBefore(ALMUERZO_FIN) && horaFin.isAfter(ALMUERZO_INICIO);
        if (seCruza) {
            throw new IllegalArgumentException("El horario de la reserva se cruza con el bloque de almuerzo ("
                    + ALMUERZO_INICIO + " a " + ALMUERZO_FIN + ").");
        }
    }

    /** 12. La franja no se cruza con franjas de clase activas de la sala del equipo. */
    private void validarFranjaDeClase(Connection cn, Reserva reserva, Equipo equipo) throws SQLException {
        int idSala = equipo.getSala().getIdSala();
        int diaSemana = reserva.getFecha().getDayOfWeek().getValue(); // 1 = lunes … 7 = domingo
        if (franjaClaseRepository.existsSolapada(cn, idSala, diaSemana,
                reserva.getHoraInicio(), reserva.getHoraFin(), null)) {
            throw new IllegalStateException("El horario solicitado se cruza con una franja de clase de la sala.");
        }
    }

    /** 13. El equipo está activo (la existencia ya la comprobó el service al bloquearlo). */
    private void validarEquipoActivo(Equipo equipo) {
        if (!equipo.isEstado()) {
            throw new IllegalStateException("El equipo " + equipo.getIdEquipo() + " está inactivo.");
        }
    }

    /**
     * 14. La cantidad de reservas activas del usuario es menor que el máximo.
     * Se consideran activas las reservas del usuario con fecha igual o posterior a hoy.
     */
    private void validarMaximoReservasActivas(Connection cn, Reserva reserva,
            Integer idReservaExcluida) throws SQLException {
        int idUsuario = reserva.getUsuario().getIdUsuario();
        int activas = reservaRepository.countByUsuarioDesdeFecha(cn, idUsuario, LocalDate.now(), idReservaExcluida);
        if (activas >= configuracion.getMaxReservasActivas()) {
            throw new IllegalStateException("El usuario " + idUsuario + " ya tiene el máximo de "
                    + configuracion.getMaxReservasActivas() + " reservas activas.");
        }
    }

    /** 15. El equipo no tiene otra reserva que se cruce con la fecha y el horario solicitados. */
    private void validarReservaSolapada(Connection cn, Reserva reserva,
            Integer idReservaExcluida) throws SQLException {
        int idEquipo = reserva.getEquipo().getIdEquipo();
        if (reservaRepository.existsSolapada(cn, idEquipo, reserva.getFecha(),
                reserva.getHoraInicio(), reserva.getHoraFin(), idReservaExcluida)) {
            throw new IllegalStateException("El equipo ya tiene una reserva para ese día y horario.");
        }
    }
}

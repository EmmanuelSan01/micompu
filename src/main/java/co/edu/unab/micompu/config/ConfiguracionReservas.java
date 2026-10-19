package co.edu.unab.micompu.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.stereotype.Component;

/**
 * Parámetros que controlan las validaciones de las reservas
 * (ver {@code service/ValidadorReservas}).
 */
@Component
@Getter
@Setter
public class ConfiguracionReservas {

    /** Primer día del semestre admitido para reservar. */
    private LocalDate fechaInicioSemestre = LocalDate.parse("2026-07-06");

    /** Último día del semestre admitido para reservar. */
    private LocalDate fechaCierreSemestre = LocalDate.parse("2026-10-31");

    /** Hora a la que abren las salas (primera hora de inicio válida). */
    private LocalTime horaApertura = LocalTime.parse("08:00");

    /** Hora a la que cierran las salas (última hora de fin válida). */
    private LocalTime horaCierre = LocalTime.parse("20:00");

    /** Máximo de días entre hoy y la fecha de la reserva. */
    private int maxDiasAnticipacion = 7;

    /** Máximo de reservas activas simultáneas por usuario. */
    private int maxReservasActivas = 2;

    /** Duración mínima de una reserva, en minutos. */
    private int minDuracionMinutos = 30;

    /** Duración máxima de una reserva, en horas. */
    private int maxDuracionHoras = 4;

    /** Días de la semana sin reservas (ISO-8601). */
    private List<Integer> diasBloqueados = List.of(6, 7);

    /** Fechas bloqueadas del calendario (días festivos). */
    private List<LocalDate> fechasBloqueadas = List.of(
        LocalDate.parse("2026-07-13"),
        LocalDate.parse("2026-07-20"),
        LocalDate.parse("2026-08-07"),
        LocalDate.parse("2026-08-17"),
        LocalDate.parse("2026-10-12")
    );
}

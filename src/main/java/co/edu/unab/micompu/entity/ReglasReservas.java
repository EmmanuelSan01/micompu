package co.edu.unab.micompu.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de las reglas de reservas que la interfaz web refleja en su
 * asistente ({@code GET /api/configuracion-reservas}): los parámetros
 * configurables de {@code config/ConfiguracionReservas} más el bloque
 * fijo de almuerzo de {@code service/ValidadorReservas}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglasReservas {

    /** Primer día del semestre admitido para reservar. */
    private LocalDate fechaInicioSemestre;

    /** Último día del semestre admitido para reservar. */
    private LocalDate fechaCierreSemestre;

    /** Máximo de días entre hoy y la fecha de la reserva. */
    private int maxDiasAnticipacion;

    /** Máximo de reservas activas simultáneas por usuario. */
    private int maxReservasActivas;

    /** Duración mínima de una reserva, en minutos. */
    private int minDuracionMinutos;

    /** Duración máxima de una reserva, en horas. */
    private int maxDuracionHoras;

    /** Días de la semana sin reservas (ISO-8601: 1 = lunes … 7 = domingo). */
    private List<Integer> diasBloqueados;

    /** Fechas bloqueadas del calendario (días festivos). */
    private List<LocalDate> fechasBloqueadas;

    /** Hora a la que abren las salas (primera hora de inicio válida). */
    private LocalTime horaApertura;

    /** Hora a la que cierran las salas (última hora de fin válida). */
    private LocalTime horaCierre;

    /** Inicio del bloque fijo de almuerzo (regla del validador, no configurable). */
    private LocalTime horaInicioAlmuerzo;

    /** Fin del bloque fijo de almuerzo (regla del validador, no configurable). */
    private LocalTime horaFinAlmuerzo;

}

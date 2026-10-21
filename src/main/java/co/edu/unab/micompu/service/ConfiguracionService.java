package co.edu.unab.micompu.service;

import org.springframework.stereotype.Service;

import co.edu.unab.micompu.config.ConfiguracionReservas;
import co.edu.unab.micompu.entity.ReglasReservas;

/**
 * Expone las reglas de reservas que la interfaz web refleja en su
 * asistente ({@code GET /api/configuracion-reservas}): los parámetros
 * configurables de {@code config/ConfiguracionReservas} más el bloque
 * fijo de almuerzo de {@code ValidadorReservas}.
 */
@Service
public class ConfiguracionService {

    private final ConfiguracionReservas configuracion;
    private final ValidadorReservas validadorReservas;

    public ConfiguracionService(ConfiguracionReservas configuracion, ValidadorReservas validadorReservas) {
        this.configuracion = configuracion;
        this.validadorReservas = validadorReservas;
    }

    /** Devuelve las reglas vigentes de las reservas: lo configurable más el almuerzo fijo. */
    public ReglasReservas obtenerReglas() {
        return ReglasReservas.builder()
                .fechaInicioSemestre(configuracion.getFechaInicioSemestre())
                .fechaCierreSemestre(configuracion.getFechaCierreSemestre())
                .maxDiasAnticipacion(configuracion.getMaxDiasAnticipacion())
                .maxReservasActivas(configuracion.getMaxReservasActivas())
                .minDuracionMinutos(configuracion.getMinDuracionMinutos())
                .maxDuracionHoras(configuracion.getMaxDuracionHoras())
                .diasBloqueados(configuracion.getDiasBloqueados())
                .fechasBloqueadas(configuracion.getFechasBloqueadas())
                .horaApertura(configuracion.getHoraApertura())
                .horaCierre(configuracion.getHoraCierre())
                .horaInicioAlmuerzo(validadorReservas.getAlmuerzoInicio())
                .horaFinAlmuerzo(validadorReservas.getAlmuerzoFin())
                .build();
    }
}

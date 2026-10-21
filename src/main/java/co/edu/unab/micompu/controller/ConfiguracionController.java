package co.edu.unab.micompu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unab.micompu.entity.ReglasReservas;
import co.edu.unab.micompu.service.ConfiguracionService;

/**
 * Reglas de reservas que la interfaz web refleja en su asistente: los
 * parámetros configurables de {@code config/ConfiguracionReservas} más
 * el bloque fijo de almuerzo de {@code service/ValidadorReservas},
 * leídos en cada petición para responder los valores vigentes. La
 * modificación (PUT) llega con el futuro panel del rol administrador.
 */
@RestController
@RequestMapping("/api/configuracion-reservas")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    /** Devuelve las reglas vigentes de las reservas (200). */
    @GetMapping
    public ResponseEntity<ReglasReservas> obtener() {
        return ResponseEntity.ok(configuracionService.obtenerReglas());
    }
}

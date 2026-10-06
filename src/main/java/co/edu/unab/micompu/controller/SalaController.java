package co.edu.unab.micompu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.FranjaClase;
import co.edu.unab.micompu.entity.Sala;
import co.edu.unab.micompu.service.SalaService;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    /** Lista todas las salas con su responsable (200). */
    @GetMapping
    public ResponseEntity<List<Sala>> listar() {
        return ResponseEntity.ok(salaService.obtenerTodos());
    }

    /** Obtiene una sala por su id, con su responsable (200 ó 404). */
    @GetMapping("/{id}")
    public ResponseEntity<Sala> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(salaService.obtenerPorId(id));
    }

    /** Equipos de la sala: las partes de la composición (200 ó 404). */
    @GetMapping("/{id}/equipos")
    public ResponseEntity<List<Equipo>> equipos(@PathVariable Integer id) {
        return ResponseEntity.ok(salaService.obtenerEquipos(id));
    }

    /** Franjas de clase de la sala: las partes de su horario (200 ó 404). */
    @GetMapping("/{id}/franjas")
    public ResponseEntity<List<FranjaClase>> franjas(@PathVariable Integer id) {
        return ResponseEntity.ok(salaService.obtenerFranjas(id));
    }

    /** Crea una sala (201, 400, 404 si el responsable no existe ó 409 si el nombre ya existe). */
    @PostMapping
    public ResponseEntity<Sala> crear(@RequestBody Sala sala) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaService.crear(sala));
    }

    /** Actualiza una sala (200, 400, 404 ó 409). */
    @PutMapping("/{id}")
    public ResponseEntity<Sala> actualizar(@PathVariable Integer id, @RequestBody Sala sala) {
        return ResponseEntity.ok(salaService.actualizar(id, sala));
    }

    /**
     * Elimina una sala y, en cascada, las reservas de sus equipos, sus equipos
     * y sus franjas de clase (204, 404 ó 500).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        salaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

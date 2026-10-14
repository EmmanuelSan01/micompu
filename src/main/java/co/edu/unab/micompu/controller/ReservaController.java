package co.edu.unab.micompu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.unab.micompu.entity.Reserva;
import co.edu.unab.micompu.service.ReservaService;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /** Lista todas las reservas con usuario y equipo (200). */
    @GetMapping
    public ResponseEntity<List<Reserva>> listar() {
        return ResponseEntity.ok(reservaService.obtenerTodos());
    }

    /** Obtiene una reserva por su id, con sus relaciones (200 ó 404). */
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /**
     * Crea una reserva (201, 400 si los datos son inválidos, 404 si el
     * usuario o el equipo no existen, 409 si el equipo está inactivo u
     * otro horario se cruza con la reserva o con una franja de clase).
     */
    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody Reserva reserva) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(reserva));
    }

    /** Actualiza una reserva revalidando las reglas (200, 400, 404 ó 409). */
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> actualizar(@PathVariable Integer id, @RequestBody Reserva reserva) {
        return ResponseEntity.ok(reservaService.actualizar(id, reserva));
    }

    /** Elimina una reserva; el equipo reservado permanece (204 ó 404). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        reservaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

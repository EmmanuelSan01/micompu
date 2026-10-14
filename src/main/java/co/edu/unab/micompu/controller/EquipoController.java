package co.edu.unab.micompu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.service.EquipoService;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    /** Lista todos los equipos con su sala (200). */
    @GetMapping
    public ResponseEntity<List<Equipo>> listar() {
        return ResponseEntity.ok(equipoService.obtenerTodos());
    }

    /** Obtiene un equipo por su id, con su sala (200 ó 404). */
    @GetMapping("/{id}")
    public ResponseEntity<Equipo> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(equipoService.obtenerPorId(id));
    }

    /** Crea un equipo en una sala (201, 400, 404 si la sala no existe ó 409). */
    @PostMapping
    public ResponseEntity<Equipo> crear(@RequestBody Equipo equipo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipoService.crear(equipo));
    }

    /** Actualiza un equipo, incluso su sala (200, 400, 404 ó 409). */
    @PutMapping("/{id}")
    public ResponseEntity<Equipo> actualizar(@PathVariable Integer id, @RequestBody Equipo equipo) {
        return ResponseEntity.ok(equipoService.actualizar(id, equipo));
    }

    /** Elimina un equipo y sus reservas (204, 404 ó 500). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

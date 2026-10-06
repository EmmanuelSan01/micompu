package co.edu.unab.micompu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.unab.micompu.entity.FranjaClase;
import co.edu.unab.micompu.service.FranjaClaseService;

@RestController
@RequestMapping("/api/franjas-clase")
public class FranjaClaseController {

    private final FranjaClaseService franjaClaseService;

    public FranjaClaseController(FranjaClaseService franjaClaseService) {
        this.franjaClaseService = franjaClaseService;
    }

    /** Lista todas las franjas con su sala (200). */
    @GetMapping
    public ResponseEntity<List<FranjaClase>> listar() {
        return ResponseEntity.ok(franjaClaseService.obtenerTodos());
    }

    /** Obtiene una franja por su id, con su sala (200 ó 404). */
    @GetMapping("/{id}")
    public ResponseEntity<FranjaClase> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(franjaClaseService.obtenerPorId(id));
    }

    /** Crea una franja (201, 400, 404 si la sala no existe ó 409 si cruza otra franja). */
    @PostMapping
    public ResponseEntity<FranjaClase> crear(@RequestBody FranjaClase franjaClase) {
        return ResponseEntity.status(HttpStatus.CREATED).body(franjaClaseService.crear(franjaClase));
    }

    /** Actualiza una franja (200, 400, 404 ó 409). */
    @PutMapping("/{id}")
    public ResponseEntity<FranjaClase> actualizar(@PathVariable Integer id, @RequestBody FranjaClase franjaClase) {
        return ResponseEntity.ok(franjaClaseService.actualizar(id, franjaClase));
    }

    /** Elimina una franja (204 ó 404). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        franjaClaseService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

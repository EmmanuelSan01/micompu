package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

public abstract class CrudController {

    protected final JdbcCrudService service;

    protected CrudController(JdbcCrudService service) {
        this.service = service;
    }

    protected abstract String table();

    protected abstract String id();

    @GetMapping
    public ResponseEntity<?> all() {
        try {
            return ResponseEntity.ok(service.all(table()));
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error de base de datos"));
        }
    }

    @GetMapping("/{value}")
    public ResponseEntity<?> one(@PathVariable int value) {
        try {
            return service.one(table(), id(), value).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (SQLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            service.create(table(), body);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            return ResponseEntity.status(409).body(Map.of("error", "No se pudo crear el registro"));
        }
    }

    @PutMapping("/{value}")
    public ResponseEntity<?> update(@PathVariable int value, @RequestBody Map<String, Object> body) {
        try {
            return service.update(table(), id(), value, body) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SQLException e) {
            return ResponseEntity.status(409).build();
        }
    }

    @DeleteMapping("/{value}")
    public ResponseEntity<?> delete(@PathVariable int value) {
        try {
            return service.delete(table(), id(), value) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SQLException e) {
            return ResponseEntity.status(409).build();
        }
    }
}

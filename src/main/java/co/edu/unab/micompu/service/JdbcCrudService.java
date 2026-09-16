package co.edu.unab.micompu.service;

import co.edu.unab.micompu.repository.JdbcCrudRepository;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

@Service
public class JdbcCrudService {

    private final JdbcCrudRepository repo;

    public JdbcCrudService(JdbcCrudRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> all(String table) throws SQLException {
        return repo.findAll(table);
    }

    public Optional<Map<String, Object>> one(String table, String id, int value) throws SQLException {
        return repo.findById(table, id, value);
    }

    public void create(String table, Map<String, Object> data) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("El cuerpo no puede estar vacío");
        }
        repo.insert(table, data);
    }

    public boolean update(String table, String id, int value, Map<String, Object> data) throws SQLException {
        return repo.update(table, id, value, data) > 0;
    }

    public boolean delete(String table, String id, int value) throws SQLException {
        return repo.delete(table, id, value) > 0;
    }
}

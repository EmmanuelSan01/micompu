package co.edu.unab.micompu.repository;

import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
public class JdbcCrudRepository {

    private final String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/mi_compu_unab?serverTimezone=UTC"), user = System.getenv().getOrDefault("DB_USER", "root"), password = System.getenv().getOrDefault("DB_PASSWORD", "");

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public List<Map<String, Object>> findAll(String table) throws SQLException {
        return query("SELECT * FROM " + safe(table));
    }

    public Optional<Map<String, Object>> findById(String table, String id, int value) throws SQLException {
        List<Map<String, Object>> r = query("SELECT * FROM " + safe(table) + " WHERE " + safe(id) + " = ?", value);
        return r.stream().findFirst();
    }

    public int insert(String table, Map<String, Object> fields) throws SQLException {
        return mutate("INSERT INTO " + safe(table) + " (" + columns(fields) + ") VALUES (" + marks(fields.size()) + ")", fields.values().toArray());
    }

    public int update(String table, String id, int value, Map<String, Object> fields) throws SQLException {
        List<Object> args = new ArrayList<>(fields.values());
        args.add(value);
        return mutate("UPDATE " + safe(table) + " SET " + assignments(fields) + " WHERE " + safe(id) + " = ?", args.toArray());
    }

    public int delete(String table, String id, int value) throws SQLException {
        return mutate("DELETE FROM " + safe(table) + " WHERE " + safe(id) + " = ?", value);
    }

    private List<Map<String, Object>> query(String sql, Object... args) throws SQLException {
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            try (ResultSet rs = p.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                ResultSetMetaData m = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) {
                        row.put(m.getColumnLabel(i), rs.getObject(i));
                    }
                    out.add(row);
                }
                return out;
            }
        }
    }

    private int mutate(String sql, Object... args) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(p, args);
                int n = p.executeUpdate();
                c.commit();
                return n;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void bind(PreparedStatement p, Object[] a) throws SQLException {
        for (int i = 0; i < a.length; i++) {
            p.setObject(i + 1, a[i]);
        }
    }

    private String safe(String s) {
        if (!s.matches("[a-z_]+")) {
            throw new IllegalArgumentException("Identificador inválido");
        }
        return s;
    }

    private String columns(Map<String, Object> m) {
        return String.join(",", m.keySet().stream().map(this::safe).toList());
    }

    private String assignments(Map<String, Object> m) {
        return String.join(",", m.keySet().stream().map(k -> safe(k) + "=?").toList());
    }

    private String marks(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }
}

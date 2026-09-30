package co.edu.unab.micompu.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConexionBD {

    private final String url;
    private final String usuario;
    private final String clave;

    public ConexionBD(@Value("${db.url}") String url,
                     @Value("${db.usuario}") String usuario,
                     @Value("${db.clave}") String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    /** Abre una nueva conexión JDBC contra la base de datos {@code mi_compu_unab}. */
    public Connection obtener() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }
}

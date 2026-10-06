package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.FranjaClase;
import co.edu.unab.micompu.entity.Sala;
import co.edu.unab.micompu.entity.Usuario;
import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.EquipoRepository;
import co.edu.unab.micompu.repository.FranjaClaseRepository;
import co.edu.unab.micompu.repository.RolRepository;
import co.edu.unab.micompu.repository.SalaRepository;
import co.edu.unab.micompu.repository.UsuarioRepository;

@Service
public class SalaService {

    private final ConexionBD conexion;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EquipoRepository equipoRepository;
    private final FranjaClaseRepository franjaClaseRepository;

    public SalaService(ConexionBD conexion, SalaRepository salaRepository,
                      UsuarioRepository usuarioRepository, RolRepository rolRepository,
                      EquipoRepository equipoRepository, FranjaClaseRepository franjaClaseRepository) {
        this.conexion = conexion;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.equipoRepository = equipoRepository;
        this.franjaClaseRepository = franjaClaseRepository;
    }

    /** Devuelve todas las salas con su responsable resuelto (asociación). */
    public List<Sala> obtenerTodos() {
        try (Connection cn = conexion.obtener()) {
            List<Sala> salas = salaRepository.findAll(cn);
            for (Sala sala : salas) {
                completarResponsable(cn, sala);
            }
            return salas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando las salas: " + e.getMessage(), e);
        }
    }

    /** Devuelve una sala con su responsable resuelto (404 si no existe). */
    public Sala obtenerPorId(Integer idSala) {
        validarId(idSala);
        try (Connection cn = conexion.obtener()) {
            Sala sala = salaRepository.findById(cn, idSala)
                    .orElseThrow(() -> new NoSuchElementException("Sala no encontrada: " + idSala));
            completarResponsable(cn, sala);
            return sala;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la sala " + idSala + ": " + e.getMessage(), e);
        }
    }

    /** Devuelve los equipos de la sala. */
    public List<Equipo> obtenerEquipos(Integer idSala) {
        validarId(idSala);
        try (Connection cn = conexion.obtener()) {
            Sala sala = salaRepository.findById(cn, idSala)
                    .orElseThrow(() -> new NoSuchElementException("Sala no encontrada: " + idSala));
            completarResponsable(cn, sala);
            List<Equipo> equipos = equipoRepository.findBySala(cn, idSala);
            for (Equipo equipo : equipos) {
                // Composición: cada parte conoce su todo.
                equipo.setSala(sala);
            }
            return equipos;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando los equipos de la sala " + idSala + ": " + e.getMessage(), e);
        }
    }

    /** Devuelve las franjas de clase de la sala. */
    public List<FranjaClase> obtenerFranjas(Integer idSala) {
        validarId(idSala);
        try (Connection cn = conexion.obtener()) {
            Sala sala = salaRepository.findById(cn, idSala)
                    .orElseThrow(() -> new NoSuchElementException("Sala no encontrada: " + idSala));
            completarResponsable(cn, sala);
            List<FranjaClase> franjas = franjaClaseRepository.findBySala(cn, idSala);
            for (FranjaClase franja : franjas) {
                // Composición: cada parte conoce su todo.
                franja.setSala(sala);
            }
            return franjas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando las franjas de la sala " + idSala + ": " + e.getMessage(), e);
        }
    }

    /** Crea una sala validando que el responsable exista (transacción manual). */
    public Sala crear(Sala sala) {
        validar(sala);
        try (Connection cn = conexion.obtener()) {
            // Transacción manual: la validación del responsable y el insert deben ser atómicos.
            cn.setAutoCommit(false);
            try {
                if (!usuarioRepository.exists(cn, sala.getResponsable().getIdUsuario())) {
                    throw new NoSuchElementException(
                            "Usuario responsable no encontrado o inactivo: " + sala.getResponsable().getIdUsuario());
                }
                Sala creada = salaRepository.insert(cn, sala);
                completarResponsable(cn, creada);
                cn.commit();
                return creada;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // UNIQUE (nombre) o FK (responsable inexistente).
                    throw new IllegalStateException(
                            "Ya existe una sala con el nombre " + sala.getNombre() + ".", e);
                }
                throw new RuntimeException("Error creando la sala: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza una sala (404 si no existe; transacción manual). */
    public Sala actualizar(Integer idSala, Sala sala) {
        validarId(idSala);
        validar(sala);
        sala.setIdSala(idSala);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                if (!usuarioRepository.exists(cn, sala.getResponsable().getIdUsuario())) {
                    throw new NoSuchElementException(
                            "Usuario responsable no encontrado o inactivo: " + sala.getResponsable().getIdUsuario());
                }
                if (!salaRepository.update(cn, sala)) {
                    throw new NoSuchElementException("Sala no encontrada: " + idSala);
                }
                completarResponsable(cn, sala);
                cn.commit();
                return sala;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    throw new IllegalStateException(
                            "Ya existe otra sala con el nombre " + sala.getNombre() + ".", e);
                }
                throw new RuntimeException("Error actualizando la sala " + idSala + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina la sala (el "todo") arrastrando el ciclo de vida de sus partes
     * (composición): desactiva sus franjas, sus equipos y la propia sala
     * (borrado lógico: {@code estado = FALSE}) dentro de una transacción
     * manual (commit si todo sale bien; rollback y estado consistente si
     * algo falla). Las reservas de los equipos no se tocan: como el borrado
     * es lógico, las filas de los equipos siguen existiendo y la FK
     * {@code fk_reserva_equipo} se conserva — las reservas quedan como
     * histórico de equipos inactivos.
     */
    public void eliminar(Integer idSala) {
        validarId(idSala);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                franjaClaseRepository.deleteBySala(cn, idSala);
                equipoRepository.deleteBySala(cn, idSala);
                if (!salaRepository.delete(cn, idSala)) {
                    throw new NoSuchElementException("Sala no encontrada: " + idSala);
                }
                cn.commit();
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw new RuntimeException("Error eliminando la sala " + idSala + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Completa la asociación sala–responsable (y la del responsable con su rol). */
    private void completarResponsable(Connection cn, Sala sala) throws SQLException {
        Integer idResponsable = sala.getResponsable() != null ? sala.getResponsable().getIdUsuario() : null;
        if (idResponsable == null) {
            throw new IllegalStateException("La sala " + sala.getIdSala() + " no tiene responsable.");
        }
        Usuario responsable = usuarioRepository.findById(cn, idResponsable)
                .orElseThrow(() -> new NoSuchElementException("Usuario responsable no encontrado: " + idResponsable));
        responsable.setRol(rolRepository.findById(cn, responsable.getRol().getIdRol())
                .orElseThrow(() -> new NoSuchElementException(
                        "Rol no encontrado: " + responsable.getRol().getIdRol())));
        sala.setResponsable(responsable);
    }

    private void validar(Sala sala) {
        if (sala == null) {
            throw new IllegalArgumentException("El cuerpo de la petición (sala) es obligatorio.");
        }
        if (sala.getResponsable() == null || sala.getResponsable().getIdUsuario() == null) {
            throw new IllegalArgumentException("La sala requiere su usuario responsable (responsable.idUsuario).");
        }
        if (sala.getNombre() == null || sala.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la sala es obligatorio.");
        }
        if (sala.getCapacidad() < 1) {
            throw new IllegalArgumentException("La capacidad de la sala debe ser mayor que cero.");
        }
    }

    private void validarId(Integer idSala) {
        if (idSala == null || idSala <= 0) {
            throw new IllegalArgumentException("El id de la sala debe ser un entero positivo.");
        }
    }
}

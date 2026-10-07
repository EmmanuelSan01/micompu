package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import co.edu.unab.micompu.entity.Equipo;
import co.edu.unab.micompu.entity.Sala;
import co.edu.unab.micompu.entity.Usuario;

import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.EquipoRepository;
import co.edu.unab.micompu.repository.RolRepository;
import co.edu.unab.micompu.repository.SalaRepository;
import co.edu.unab.micompu.repository.UsuarioRepository;

@Service
public class EquipoService {

    private final ConexionBD conexion;
    private final EquipoRepository equipoRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public EquipoService(ConexionBD conexion, EquipoRepository equipoRepository,
            SalaRepository salaRepository, UsuarioRepository usuarioRepository,
            RolRepository rolRepository) {
        this.conexion = conexion;
        this.equipoRepository = equipoRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    /** Devuelve todos los equipos con su sala completa (composición). */
    public List<Equipo> obtenerTodos() {
        try (Connection cn = conexion.obtener()) {
            List<Equipo> equipos = equipoRepository.findAll(cn);
            for (Equipo equipo : equipos) {
                completarSala(cn, equipo);
            }
            return equipos;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando los equipos: " + e.getMessage(), e);
        }
    }

    /** Devuelve un equipo con su sala completa (404 si no existe). */
    public Equipo obtenerPorId(Integer idEquipo) {
        validarId(idEquipo);
        try (Connection cn = conexion.obtener()) {
            Equipo equipo = equipoRepository.findById(cn, idEquipo)
                    .orElseThrow(() -> new NoSuchElementException("Equipo no encontrado: " + idEquipo));
            completarSala(cn, equipo);
            return equipo;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el equipo " + idEquipo + ": " + e.getMessage(), e);
        }
    }

    /** Crea un equipo validando que su sala exista (transacción manual). */
    public Equipo crear(Equipo equipo) {
        validar(equipo);
        try (Connection cn = conexion.obtener()) {
            // Transacción manual: la validación de la sala y el insert deben ser atómicos.
            cn.setAutoCommit(false);
            try {
                if (!salaRepository.exists(cn, equipo.getSala().getIdSala())) {
                    throw new NoSuchElementException(
                            "Sala no encontrada o inactiva: " + equipo.getSala().getIdSala());
                }
                Equipo creado = equipoRepository.insert(cn, equipo);
                completarSala(cn, creado);
                cn.commit();
                return creado;
            } catch (SQLException e) {
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // FK fk_equipo_sala: la sala indicada no existe.
                    throw new IllegalStateException("La sala indicada no existe.", e);
                }
                throw new RuntimeException("Error creando el equipo: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza un equipo (404 si no existe; transacción manual). */
    public Equipo actualizar(Integer idEquipo, Equipo equipo) {
        validarId(idEquipo);
        validar(equipo);
        equipo.setIdEquipo(idEquipo);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                if (!salaRepository.exists(cn, equipo.getSala().getIdSala())) {
                    throw new NoSuchElementException(
                            "Sala no encontrada o inactiva: " + equipo.getSala().getIdSala());
                }
                Equipo actual = equipoRepository.findByIdForUpdate(cn, idEquipo)
                        .orElseThrow(() -> new NoSuchElementException("Equipo no encontrado: " + idEquipo));
                if (!actual.isEstado()) {
                    throw new IllegalStateException(
                            "El equipo " + idEquipo + " está inactivo (borrado lógico) y no puede actualizarse.");
                }
                if (!equipoRepository.update(cn, equipo)) {
                    throw new NoSuchElementException("Equipo no encontrado: " + idEquipo);
                }
                equipo.setEstado(actual.isEstado());
                completarSala(cn, equipo);
                cn.commit();
                return equipo;
            } catch (SQLException e) {
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    throw new IllegalStateException("La sala indicada no existe.", e);
                }
                throw new RuntimeException("Error actualizando el equipo " + idEquipo + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException sup) {
                    e.addSuppressed(sup);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Desactiva un equipo (borrado lógico: {@code estado = FALSE}; 404 si no
     * existe). Como la fila ya no se borra físicamente, la FK
     * {@code fk_reserva_equipo} (ON DELETE RESTRICT) ya no exige borrar sus
     * reservas: el equipo sigue existiendo (inactivo) y sus reservas se
     * conservan como histórico.
     */
    public void eliminar(Integer idEquipo) {
        validarId(idEquipo);
        try (Connection cn = conexion.obtener()) {
            if (!equipoRepository.delete(cn, idEquipo)) {
                throw new NoSuchElementException("Equipo no encontrado: " + idEquipo);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando el equipo " + idEquipo + ": " + e.getMessage(), e);
        }
    }

    /** Completa la composición equipo–sala cargando la sala con su responsable y su rol. */
    private void completarSala(Connection cn, Equipo equipo) throws SQLException {
        Sala sala = salaRepository.findById(cn, equipo.getSala().getIdSala())
                .orElseThrow(() -> new NoSuchElementException(
                "Sala no encontrada: " + equipo.getSala().getIdSala()));
        Usuario responsable = usuarioRepository.findById(cn, sala.getResponsable().getIdUsuario())
                .orElseThrow(() -> new NoSuchElementException(
                "Usuario responsable no encontrado: " + sala.getResponsable().getIdUsuario()));
        responsable.setRol(rolRepository.findById(cn, responsable.getRol().getIdRol())
                .orElseThrow(() -> new NoSuchElementException(
                "Rol no encontrado: " + responsable.getRol().getIdRol())));
        sala.setResponsable(responsable);
        equipo.setSala(sala);
    }

    private void validar(Equipo equipo) {
        if (equipo == null) {
            throw new IllegalArgumentException("El cuerpo de la petición (equipo) es obligatorio.");
        }
        if (equipo.getSala() == null || equipo.getSala().getIdSala() == null) {
            throw new IllegalArgumentException("El equipo requiere la sala a la que pertenece (sala.idSala).");
        }
    }

    private void validarId(Integer idEquipo) {
        if (idEquipo == null || idEquipo <= 0) {
            throw new IllegalArgumentException("El id del equipo debe ser un entero positivo.");
        }
    }
}

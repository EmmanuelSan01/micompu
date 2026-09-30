package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import co.edu.unab.micompu.entity.FranjaClase;
import co.edu.unab.micompu.entity.Sala;
import co.edu.unab.micompu.entity.Usuario;
import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.FranjaClaseRepository;
import co.edu.unab.micompu.repository.RolRepository;
import co.edu.unab.micompu.repository.SalaRepository;
import co.edu.unab.micompu.repository.UsuarioRepository;

@Service
public class FranjaClaseService {

    private final ConexionBD conexion;
    private final FranjaClaseRepository franjaClaseRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public FranjaClaseService(ConexionBD conexion, FranjaClaseRepository franjaClaseRepository,
                             SalaRepository salaRepository, UsuarioRepository usuarioRepository,
                             RolRepository rolRepository) {
        this.conexion = conexion;
        this.franjaClaseRepository = franjaClaseRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    /** Devuelve todas las franjas con su sala completa (composición). */
    public List<FranjaClase> obtenerTodos() {
        try (Connection cn = conexion.obtener()) {
            List<FranjaClase> franjas = franjaClaseRepository.findAll(cn);
            for (FranjaClase franja : franjas) {
                completarSala(cn, franja);
            }
            return franjas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando las franjas de clase: " + e.getMessage(), e);
        }
    }

    /** Devuelve una franja con su sala completa (404 si no existe). */
    public FranjaClase obtenerPorId(Integer idFranjaClase) {
        validarId(idFranjaClase);
        try (Connection cn = conexion.obtener()) {
            FranjaClase franja = franjaClaseRepository.findById(cn, idFranjaClase)
                    .orElseThrow(() -> new NoSuchElementException("Franja de clase no encontrada: " + idFranjaClase));
            completarSala(cn, franja);
            return franja;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error consultando la franja de clase " + idFranjaClase + ": " + e.getMessage(), e);
        }
    }

    /** Crea una franja validando la sala y los cruces de horario (transacción manual). */
    public FranjaClase crear(FranjaClase franja) {
        validar(franja);
        try (Connection cn = conexion.obtener()) {
            // Transacción manual: validaciones y insert atómicos.
            cn.setAutoCommit(false);
            try {
                int idSala = franja.getSala().getIdSala();
                if (!salaRepository.exists(cn, idSala)) {
                    throw new NoSuchElementException("Sala no encontrada: " + idSala);
                }
                if (franjaClaseRepository.existsSolapada(cn, idSala, franja.getDiaSemana(),
                        franja.getHoraInicio(), franja.getHoraFin(), null)) {
                    throw new IllegalStateException(
                            "La sala ya tiene una franja de clase activa que se cruza con ese horario.");
                }
                FranjaClase creada = franjaClaseRepository.insert(cn, franja);
                completarSala(cn, creada);
                cn.commit();
                return creada;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // FK fk_franja_clase_sala.
                    throw new IllegalStateException("La sala indicada no existe.", e);
                }
                throw new RuntimeException("Error creando la franja de clase: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza una franja (404 si no existe; transacción manual). */
    public FranjaClase actualizar(Integer idFranjaClase, FranjaClase franja) {
        validarId(idFranjaClase);
        validar(franja);
        franja.setIdFranjaClase(idFranjaClase);
        try (Connection cn = conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                int idSala = franja.getSala().getIdSala();
                if (!salaRepository.exists(cn, idSala)) {
                    throw new NoSuchElementException("Sala no encontrada: " + idSala);
                }
                if (franjaClaseRepository.existsSolapada(cn, idSala, franja.getDiaSemana(),
                        franja.getHoraInicio(), franja.getHoraFin(), idFranjaClase)) {
                    throw new IllegalStateException(
                            "La sala ya tiene una franja de clase activa que se cruza con ese horario.");
                }
                if (!franjaClaseRepository.update(cn, franja)) {
                    throw new NoSuchElementException("Franja de clase no encontrada: " + idFranjaClase);
                }
                completarSala(cn, franja);
                cn.commit();
                return franja;
            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    throw new IllegalStateException("La sala indicada no existe.", e);
                }
                throw new RuntimeException(
                        "Error actualizando la franja de clase " + idFranjaClase + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                try { cn.rollback(); } catch (SQLException sup) { e.addSuppressed(sup); }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No fue posible conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /** Elimina una franja (404 si no existe). */
    public void eliminar(Integer idFranjaClase) {
        validarId(idFranjaClase);
        try (Connection cn = conexion.obtener()) {
            if (!franjaClaseRepository.delete(cn, idFranjaClase)) {
                throw new NoSuchElementException("Franja de clase no encontrada: " + idFranjaClase);
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error eliminando la franja de clase " + idFranjaClase + ": " + e.getMessage(), e);
        }
    }

    /** Completa la composición franja–sala cargando la sala con su responsable y su rol. */
    private void completarSala(Connection cn, FranjaClase franja) throws SQLException {
        Sala sala = salaRepository.findById(cn, franja.getSala().getIdSala())
                .orElseThrow(() -> new NoSuchElementException(
                        "Sala no encontrada: " + franja.getSala().getIdSala()));
        Usuario responsable = usuarioRepository.findById(cn, sala.getResponsable().getIdUsuario())
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuario responsable no encontrado: " + sala.getResponsable().getIdUsuario()));
        responsable.setRol(rolRepository.findById(cn, responsable.getRol().getIdRol())
                .orElseThrow(() -> new NoSuchElementException(
                        "Rol no encontrado: " + responsable.getRol().getIdRol())));
        sala.setResponsable(responsable);
        franja.setSala(sala);
    }

    private void validar(FranjaClase franja) {
        if (franja == null) {
            throw new IllegalArgumentException("El cuerpo de la petición (franja de clase) es obligatorio.");
        }
        if (franja.getSala() == null || franja.getSala().getIdSala() == null) {
            throw new IllegalArgumentException("La franja requiere la sala a la que pertenece (sala.idSala).");
        }
        if (franja.getDiaSemana() < 1 || franja.getDiaSemana() > 7) {
            throw new IllegalArgumentException("El día de la semana debe estar entre 1 (lunes) y 7 (domingo).");
        }
        if (franja.getHoraInicio() == null || franja.getHoraFin() == null) {
            throw new IllegalArgumentException("Las horas de inicio y fin de la franja son obligatorias.");
        }
        if (!franja.getHoraFin().isAfter(franja.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin de la franja debe ser posterior a la de inicio.");
        }
        if (franja.getMotivo() == null || franja.getMotivo().isBlank()) {
            throw new IllegalArgumentException("El motivo de la franja es obligatorio.");
        }
    }

    private void validarId(Integer idFranjaClase) {
        if (idFranjaClase == null || idFranjaClase <= 0) {
            throw new IllegalArgumentException("El id de la franja de clase debe ser un entero positivo.");
        }
    }
}

package co.edu.unab.micompu.service;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import co.edu.unab.micompu.entity.Rol;
import co.edu.unab.micompu.repository.ConexionBD;
import co.edu.unab.micompu.repository.RolRepository;

@Service
public class RolService {

    private final ConexionBD conexion;
    private final RolRepository rolRepository;

    public RolService(ConexionBD conexion, RolRepository rolRepository) {
        this.conexion = conexion;
        this.rolRepository = rolRepository;
    }

    /** Devuelve todos los roles. */
    public List<Rol> obtenerTodos() {
        try (Connection cn = conexion.obtener()) {
            return rolRepository.findAll(cn);
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando los roles: " + e.getMessage(), e);
        }
    }

    /** Devuelve un rol por su id (404 si no existe). */
    public Rol obtenerPorId(Integer idRol) {
        validarId(idRol);
        try (Connection cn = conexion.obtener()) {
            return rolRepository.findById(cn, idRol)
                    .orElseThrow(() -> new NoSuchElementException("Rol no encontrado: " + idRol));
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el rol " + idRol + ": " + e.getMessage(), e);
        }
    }

    /** Crea un rol. */
    public Rol crear(Rol rol) {
        validar(rol);
        try (Connection cn = conexion.obtener()) {
            return rolRepository.insert(cn, rol);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando el rol: " + e.getMessage(), e);
        }
    }

    /** Actualiza un rol (404 si no existe). */
    public Rol actualizar(Integer idRol, Rol rol) {
        validarId(idRol);
        validar(rol);
        rol.setIdRol(idRol);
        try (Connection cn = conexion.obtener()) {
            if (!rolRepository.update(cn, rol)) {
                throw new NoSuchElementException("Rol no encontrado: " + idRol);
            }
            return rol;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando el rol " + idRol + ": " + e.getMessage(), e);
        }
    }

    /**
     * Desactiva un rol (borrado lógico: {@code estado = FALSE}; 404 si no
     * existe). Como la fila ya no se borra físicamente, la FK
     * {@code fk_usuario_rol} (ON DELETE RESTRICT) ya no bloquea la operación:
     * el rol puede quedar inactivo aunque tenga usuarios asignados, que
     * seguirán referenciándolo y resolviéndolo por su id.
     */
    public void eliminar(Integer idRol) {
        validarId(idRol);
        try (Connection cn = conexion.obtener()) {
            if (!rolRepository.delete(cn, idRol)) {
                throw new NoSuchElementException("Rol no encontrado: " + idRol);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando el rol " + idRol + ": " + e.getMessage(), e);
        }
    }

    private void validar(Rol rol) {
        if (rol == null) {
            throw new IllegalArgumentException("El cuerpo de la petición (rol) es obligatorio.");
        }
        if (rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }
    }

    private void validarId(Integer idRol) {
        if (idRol == null || idRol <= 0) {
            throw new IllegalArgumentException("El id del rol debe ser un entero positivo.");
        }
    }
}

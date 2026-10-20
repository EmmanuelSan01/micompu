package co.edu.unab.micompu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unab.micompu.entity.Usuario;
import co.edu.unab.micompu.service.UsuarioService;

/**
 * Inicio de sesión de la interfaz web ({@code static/}): la página de login
 * envía el correo y la contraseña y guarda el usuario que responde este
 * endpoint como sesión local del navegador.
 */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final UsuarioService usuarioService;

    public SesionController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Inicia sesión (200 con el usuario autenticado, 400 si la contraseña no
     * coincide con la registrada y 404 si el correo no está registrado o el
     * usuario está inactivo).
     */
    @PostMapping
    public ResponseEntity<Usuario> iniciar(@RequestBody Usuario credenciales) {
        return ResponseEntity.ok(usuarioService.autenticar(
                credenciales.getEmail(), credenciales.getPasswordHash()));
    }
}

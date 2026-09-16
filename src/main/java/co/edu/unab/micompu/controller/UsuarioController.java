package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends CrudController {

    public UsuarioController(JdbcCrudService s) {
        super(s);
    }

    protected String table() {
        return "usuario";
    }

    protected String id() {
        return "id_usuario";
    }
}

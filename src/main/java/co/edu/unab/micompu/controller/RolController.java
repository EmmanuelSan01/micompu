package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RolController extends CrudController {

    public RolController(JdbcCrudService s) {
        super(s);
    }

    protected String table() {
        return "rol";
    }

    protected String id() {
        return "id_rol";
    }
}

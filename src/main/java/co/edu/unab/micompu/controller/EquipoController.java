package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController extends CrudController {

    public EquipoController(JdbcCrudService s) {
        super(s);
    }

    protected String table() {
        return "equipo";
    }

    protected String id() {
        return "id_equipo";
    }
}

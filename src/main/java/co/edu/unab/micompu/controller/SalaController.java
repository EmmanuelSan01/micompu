package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salas")
public class SalaController extends CrudController {

    public SalaController(JdbcCrudService s) {
        super(s);
    }

    protected String table() {
        return "sala";
    }

    protected String id() {
        return "id_sala";
    }
}

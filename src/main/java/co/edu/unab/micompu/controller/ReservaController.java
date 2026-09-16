package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController extends CrudController {

    public ReservaController(JdbcCrudService s) {
        super(s);
    }

    protected String table() {
        return "reserva";
    }

    protected String id() {
        return "id_reserva";
    }
}

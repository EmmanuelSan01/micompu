package co.edu.unab.micompu.controller;

import co.edu.unab.micompu.service.JdbcCrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/franjas-clase")
public class FranjaClaseController extends CrudController {

    public FranjaClaseController(JdbcCrudService s) {
        super(s);
    }

    protected String table() {
        return "franja_clase";
    }

    protected String id() {
        return "id_franja_clase";
    }
}
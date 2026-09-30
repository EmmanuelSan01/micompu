package co.edu.unab.micompu.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    private Integer idRol;
    private String nombre;
    private boolean estado;

}

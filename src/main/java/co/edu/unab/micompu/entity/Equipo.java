package co.edu.unab.micompu.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

    private Integer idEquipo;
    private Sala sala;
    private boolean estado;

}

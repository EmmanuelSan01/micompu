package co.edu.unab.micompu.entity;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FranjaClase {

    private Integer idFranjaClase;
    private Sala sala;
    private byte diaSemana;
    private LocalTime horaInicio, horaFin;
    private String motivo;
    private boolean estado;

}

package co.edu.unab.micompu.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sala {

    private Integer idSala;
    private Usuario responsable;
    private String nombre;
    private byte capacidad;
    private boolean estado;

}

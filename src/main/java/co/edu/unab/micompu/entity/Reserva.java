package co.edu.unab.micompu.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    private Integer idReserva;
    private Usuario usuario;
    private Equipo equipo;
    private LocalDate fecha;
    private LocalTime horaInicio, horaFin;

}

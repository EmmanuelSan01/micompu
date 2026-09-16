package co.edu.unab.micompu.entity;

import java.time.LocalTime;

public class FranjaClase {

    private Integer idFranjaClase;
    private Sala sala;
    private short diaSemana;
    private LocalTime horaInicio, horaFin;
    private String motivo;
    private boolean estado;

    public FranjaClase() {
    }

    public Integer getIdFranjaClase() {
        return idFranjaClase;
    }

    public void setIdFranjaClase(Integer idFranjaClase) {
        this.idFranjaClase = idFranjaClase;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public short getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(short diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}

package co.edu.unab.micompu.entity;

public class Equipo {

    private Integer idEquipo;
    private Sala sala;
    private boolean estado;

    public Equipo() {
    }

    public Equipo(Integer idEquipo, Sala sala, boolean estado) {
        this.idEquipo = idEquipo;
        this.sala = sala;
        this.estado = estado;
    }

    public Integer getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Integer idEquipo) {
        this.idEquipo = idEquipo;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}

package co.edu.unab.micompu.entity;

public class Sala {

    private Integer idSala;
    private Usuario responsable;
    private String nombre;
    private int capacidad;
    private boolean estado;

    public Sala() {
    }

    public Sala(Integer idSala, Usuario responsable, String nombre, int capacidad, boolean estado) {
        this.idSala = idSala;
        this.responsable = responsable;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.estado = estado;
    }

    public void setIdSala(Integer idSala) {
        this.idSala = idSala;
    }

    public void setResponsable(Usuario responsable) {
        this.responsable = responsable;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}

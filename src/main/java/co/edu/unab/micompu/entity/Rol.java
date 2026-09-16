package co.edu.unab.micompu.entity;

public class Rol {

    private Integer idRol;
    private String nombre;
    private boolean estado;

    public Rol() {
    }

    public Rol(Integer idRol, String nombre, boolean estado) {
        this.idRol = idRol;
        this.nombre = nombre;
        this.estado = estado;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}

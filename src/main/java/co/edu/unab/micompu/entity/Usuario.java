package co.edu.unab.micompu.entity;

public class Usuario {

    private Integer idUsuario;
    private Rol rol;
    private String email;
    private String nombre;
    private String passwordHash;
    private boolean estado;

    public Usuario() {
    }

    public Usuario(Integer idUsuario, Rol rol, String email, String nombre, String passwordHash, boolean estado) {
        this.idUsuario = idUsuario;
        this.rol = rol;
        this.email = email;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.estado = estado;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}

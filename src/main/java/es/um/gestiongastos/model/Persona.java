package es.um.gestiongastos.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Persona {
    private final String id;
    private String nombreCompleto;
    private String nombreUsuario;
    private String contraseña; // podrías cifrarla después
    private final List<Gasto> gastos; //lista de gastos

    public Persona(String id, String nombreCompleto, String nombreUsuario, String contraseña) {
        if (id == null || nombreCompleto == null || nombreUsuario == null || contraseña == null) {
            throw new IllegalArgumentException("Ningún campo de Persona puede ser nulo");
        }
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
        this.contraseña = contraseña;
        this.gastos = new ArrayList<>(); // Inicializar la lista
    }

    public String getId() { return id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getContraseña() { return contraseña; }
    public void setContraseña(String contraseña) { this.contraseña = contraseña; }
    
    // Método para que el Controlador pueda agregar un Gasto
    public void agregarGasto(Gasto gasto) {
        this.gastos.add(gasto);
    }
    
    // Getter para que la Vista pueda leer la lista
    public List<Gasto> getGastos() {
        return gastos;
    }
    @Override
    public String toString() {
        return nombreCompleto + " (@" + nombreUsuario + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Persona)) return false;
        Persona p = (Persona) o;
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

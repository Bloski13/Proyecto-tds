package es.um.gestiongastos.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Persona {
    private final String id;
    private String nombreCompleto;
    private String nombreUsuario;
    private String contraseña;
    
    private final List<GastosCompartidos> misCuentas; 
    
    private final List<Alerta> alertasConfiguradas;
    private final List<Notificacion> historialNotificaciones;

    public Persona(String id, String nombreCompleto, String nombreUsuario, String contraseña) {
        if (id == null || nombreCompleto == null || nombreUsuario == null || contraseña == null) {
            throw new IllegalArgumentException("Ningún campo de Persona puede ser nulo");
        }
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
        this.contraseña = contraseña;
        this.misCuentas = new ArrayList<>(); 
        this.alertasConfiguradas = new ArrayList<>();
        this.historialNotificaciones = new ArrayList<>();
    }

    public String getId() { return id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getContraseña() { return contraseña; }
    public void setContraseña(String contraseña) { this.contraseña = contraseña; }
    
    public void agregarCuenta(GastosCompartidos cuenta) {
        if (!misCuentas.contains(cuenta)) {
            this.misCuentas.add(cuenta);
        }
    }
    
    public List<GastosCompartidos> getCuentas() {
        return Collections.unmodifiableList(misCuentas);
    }
    
    public void agregarAlerta(Alerta alerta) {
        this.alertasConfiguradas.add(alerta);
    }

    public void eliminarAlerta(Alerta alerta) {
        this.alertasConfiguradas.remove(alerta);
    }

    public List<Alerta> getAlertas() {
        return Collections.unmodifiableList(alertasConfiguradas);
    }

    public void agregarNotificacion(Notificacion n) {
        // Añadimos al principio para que salgan las más nuevas primero
        this.historialNotificaciones.add(0, n);
    }

    public List<Notificacion> getNotificaciones() {
        return Collections.unmodifiableList(historialNotificaciones);
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
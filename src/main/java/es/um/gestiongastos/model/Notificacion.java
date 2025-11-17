
package es.um.gestiongastos.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Notificacion {
    private final String id;
    private final LocalDateTime fechaHora;
    private final String mensaje;

    public Notificacion(String id, LocalDateTime fechaHora, String mensaje) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.mensaje = mensaje;
    }

    public String getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getMensaje() { return mensaje; }

    @Override
    public String toString() {
        return fechaHora + " - " + mensaje;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notificacion)) return false;
        Notificacion n = (Notificacion) o;
        return Objects.equals(id, n.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

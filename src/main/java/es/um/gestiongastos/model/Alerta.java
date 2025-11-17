
package es.um.gestiongastos.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase Alerta: tiene un umbral, periodicidad y opcionalmente una categoría asociada.
 * Mantiene historial de notificaciones.
 */
public class Alerta {
    private final String id;
    private String nombre;
    private final Periodicidad periodicidad;
    private final Categoria categoriaOpcional;
    private final EstrategiaAlerta estrategia;
    private final List<Notificacion> historial = new ArrayList<>();

    public Alerta(String id, String nombre, Periodicidad periodicidad, Categoria categoriaOpcional, EstrategiaAlerta estrategia) {
        this.id = id;
        this.nombre = nombre;
        this.periodicidad = periodicidad;
        this.categoriaOpcional = categoriaOpcional;
        this.estrategia = estrategia;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Periodicidad getPeriodicidad() { return periodicidad; }
    public Optional<Categoria> getCategoriaOpcional() { return Optional.ofNullable(categoriaOpcional); }
    public List<Notificacion> getHistorial() { return Collections.unmodifiableList(historial); }

    public boolean evaluar(double importeTotal) {
        boolean disparo = estrategia.comprobar(importeTotal, periodicidad);
        if (disparo) {
            Notificacion n = new Notificacion(UUID.randomUUID().toString(), java.time.LocalDateTime.now(),
                    String.format("Alerta '%s' disparada: %.2f > umbral", nombre, importeTotal));
            historial.add(n);
        }
        return disparo;
    }

    @Override
    public String toString() {
        return nombre + " (" + periodicidad + ") - " + estrategia;
    }
}

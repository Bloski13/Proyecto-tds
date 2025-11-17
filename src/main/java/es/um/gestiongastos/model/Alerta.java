package es.um.gestiongastos.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase Alerta: tiene un umbral, periodicidad y opcionalmente una categoría asociada.
 * Mantiene historial de notificaciones. Soporta listeners y límite de historial.
 */
public class Alerta {
    private final String id;
    private String nombre;
    private final Periodicidad periodicidad;
    private final Categoria categoriaOpcional;
    private final EstrategiaAlerta estrategia;
    private final LinkedList<Notificacion> historial = new LinkedList<>();

    // listeners para notificaciones (observer)
    private final List<NotificacionListener> listeners = new ArrayList<>();

    // tamaño máximo del historial (si <=0 -> sin límite)
    private final int maxHistorial;

    public Alerta(String id, String nombre, Periodicidad periodicidad, Categoria categoriaOpcional,
                  EstrategiaAlerta estrategia) {
        this(id, nombre, periodicidad, categoriaOpcional, estrategia, 0);
    }

    public Alerta(String id, String nombre, Periodicidad periodicidad, Categoria categoriaOpcional,
                  EstrategiaAlerta estrategia, int maxHistorial) {
        if (id == null || nombre == null || periodicidad == null || estrategia == null) {
            throw new IllegalArgumentException("id, nombre, periodicidad y estrategia no pueden ser nulos");
        }
        this.id = id;
        this.nombre = nombre;
        this.periodicidad = periodicidad;
        this.categoriaOpcional = categoriaOpcional;
        this.estrategia = estrategia;
        this.maxHistorial = maxHistorial;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Periodicidad getPeriodicidad() { return periodicidad; }
    public Optional<Categoria> getCategoriaOpcional() { return Optional.ofNullable(categoriaOpcional); }

    /**
     * Historial inmutable desde fuera.
     */
    public List<Notificacion> getHistorial() {
        return Collections.unmodifiableList(new ArrayList<>(historial));
    }

    /**
     * Registra un listener que será notificado cuando se cree una Notificacion.
     */
    public void registerListener(NotificacionListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void unregisterListener(NotificacionListener l) {
        listeners.remove(l);
    }

    /**
     * Borra el historial.
     */
    public void clearHistorial() {
        historial.clear();
    }

    /**
     * Devuelve las últimas n notificaciones (hasta el tamaño del historial).
     */
    public List<Notificacion> getUltimasNotificaciones(int n) {
        if (n <= 0) return Collections.emptyList();
        int size = historial.size();
        int fromIndex = Math.max(0, size - n);
        return Collections.unmodifiableList(new ArrayList<>(historial.subList(fromIndex, size)));
    }

    /**
     * Evalúa la alerta y, si corresponde, crea una notificación y la añade al historial
     * (aplicando el límite si procede). Devuelve true si se disparó.
     *
     * @param importeTotal importe calculado de los gastos
     * @return true si la alerta se disparó
     */
    public boolean evaluar(double importeTotal) {
        boolean disparo = estrategia.comprobar(importeTotal, periodicidad, categoriaOpcional);
        if (disparo) {
            Notificacion n = new Notificacion(UUID.randomUUID().toString(), LocalDateTime.now(),
                    String.format("Alerta '%s' disparada: %.2f > umbral", nombre, importeTotal));
            // mantener límite tamaño historial
            if (maxHistorial > 0 && historial.size() >= maxHistorial) {
                historial.removeFirst();
            }
            historial.add(n);
            // notificar listeners (hacer copia para evitar CME si listener modifica lista)
            List<NotificacionListener> copy = new ArrayList<>(listeners);
            for (NotificacionListener l : copy) {
                try {
                    l.onNotificacion(n, this);
                } catch (Exception ex) {
                    // no dejamos que un listener rompa el flujo de notificación
                    ex.printStackTrace();
                }
            }
        }
        return disparo;
    }

    @Override
    public String toString() {
        return nombre + " (" + periodicidad + ")" + (categoriaOpcional != null ? " [" + categoriaOpcional + "]" : "");
    }
}

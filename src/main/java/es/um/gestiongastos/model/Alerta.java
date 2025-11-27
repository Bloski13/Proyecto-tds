package es.um.gestiongastos.model;

import java.util.Optional;

public class Alerta {
    private final String id;
    private String nombre;
    private final Periodicidad periodicidad;
    private final Categoria categoriaOpcional; // Puede ser null
    private final EstrategiaAlerta estrategia; // Patrón Estrategia

    public Alerta(String id, String nombre, Periodicidad periodicidad, Categoria categoriaOpcional, EstrategiaAlerta estrategia) {
        this.id = id;
        this.nombre = nombre;
        this.periodicidad = periodicidad;
        this.categoriaOpcional = categoriaOpcional;
        this.estrategia = estrategia;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Periodicidad getPeriodicidad() { return periodicidad; }
    public Optional<Categoria> getCategoriaOpcional() { return Optional.ofNullable(categoriaOpcional); }
    public EstrategiaAlerta getEstrategia() { return estrategia; }

    /**
     * Delega en la estrategia para comprobar si salta la alerta.
     */
    public boolean comprobar(double importeTotalCalculado) {
        return estrategia.comprobar(importeTotalCalculado, periodicidad, categoriaOpcional);
    }

    @Override
    public String toString() {
        String catStr = (categoriaOpcional != null) ? " (" + categoriaOpcional.getNombre() + ")" : " (Global)";
        return nombre + " [" + periodicidad + "]" + catStr + " -> " + estrategia.toString();
    }
}
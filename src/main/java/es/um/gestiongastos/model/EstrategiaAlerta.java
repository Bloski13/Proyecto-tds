
package es.um.gestiongastos.model;

import java.util.List;

/**
 * Interfaz de la estrategia que define cuándo debe lanzarse una alerta.
 * Implementaciones concretas pueden comprobar periodos, categorías, etc.
 */
public interface EstrategiaAlerta {
    /**
     * Dada la lista de gastos relevantes, devuelve true si la alerta debe dispararse.
     * Por simplicidad la implementación recibe el importe total de los gastos ya calculado.
     */
    boolean comprobar(double importeTotal, Periodicidad period);
}

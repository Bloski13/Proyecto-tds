
package es.um.gestiongastos.model;

/**
 * Estrategia simple: dispara la alerta cuando el importe total supera un umbral.
 */
public class EstrategiaPorUmbral implements EstrategiaAlerta {
    private final double umbral;

    public EstrategiaPorUmbral(double umbral) {
        this.umbral = umbral;
    }

    @Override
    public boolean comprobar(double importeTotal, Periodicidad period) {
        return importeTotal > umbral;
    }

    @Override
    public String toString() {
        return String.format("EstrategiaPorUmbral(%.2f)", umbral);
    }
}

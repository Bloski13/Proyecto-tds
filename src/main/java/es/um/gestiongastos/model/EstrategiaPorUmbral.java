package es.um.gestiongastos.model;

/**
 * Estrategia sencilla: dispara si importeTotal > umbral.
 * Si la alerta tiene categoría, esta estrategia ignora la categoría (pero podría ampliarse).
 */
public class EstrategiaPorUmbral implements EstrategiaAlerta {
    private final double umbral;

    public EstrategiaPorUmbral(double umbral) {
        this.umbral = umbral;
    }

    @Override
    public boolean comprobar(double importeTotal, Periodicidad period, Categoria categoriaOpcional) {
        return importeTotal > umbral;
    }

    @Override
    public String toString() {
        return "Umbral(" + umbral + ")";
    }
}

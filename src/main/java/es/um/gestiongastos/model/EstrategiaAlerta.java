package es.um.gestiongastos.model;

public interface EstrategiaAlerta {
    /**
     * Comprueba si la alerta debe dispararse.
     *
     * @param importeTotal importe total calculado de los gastos relevantes
     * @param period periodicidad que aplica en la alerta
     * @param categoriaOpcional categoría relacionada (puede ser null si la alerta no tiene categoría)
     * @return true si debe dispararse la alerta
     */
    boolean comprobar(double importeTotal, Periodicidad period, Categoria categoriaOpcional);
}

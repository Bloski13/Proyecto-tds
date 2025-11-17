
package es.um.gestiongastos.model;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cuenta de gasto compartida. Una vez creada la lista de participantes no puede modificarse.
 * Permite distribución equitativa o porcentaje por participante.
 */
public class CuentaGasto {
    private final String id;
    private String nombre;
    private final List<Participante> participantes = new ArrayList<>();
    private final boolean porcentajesFijos; // si true, cada participante tiene un porcentaje definido; si false: reparto equitativo

    public CuentaGasto(String id, String nombre, Map<Persona, Double> porcentajes) {
        this.id = id;
        this.nombre = nombre;
        if (porcentajes == null || porcentajes.isEmpty()) {
            this.porcentajesFijos = false;
        } else {
            this.porcentajesFijos = true;
            double sum = porcentajes.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 100.0) > 0.001) {
                throw new IllegalArgumentException("Los porcentajes deben sumar 100"); 
            }
            porcentajes.forEach((p, pr) -> participantes.add(new Participante(p, pr)));
        }
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<Participante> getParticipantes() { return Collections.unmodifiableList(participantes); }

    public void addParticipanteEquitativo(Persona p) {
        if (porcentajesFijos) throw new UnsupportedOperationException("Cuenta creada con porcentajes fijos: no se puede modificar la lista");
        participantes.add(new Participante(p, -1.0)); // -1 indica equitativo por calcular
    }

    /**
     * Calcula el reparto de un gasto (importe) entre participantes, devolviendo un mapa persona -> cantidad que debe suponer para esa persona.
     * El pagador recibirá el importe prorrateado como saldo positivo frente al resto.
     */
    public Map<Persona, BigDecimal> repartirGasto(BigDecimal importe) {
        Map<Persona, BigDecimal> res = new LinkedHashMap<>();
        int n = participantes.size();
        if (n == 0) return res;
        if (porcentajesFijos) {
            for (Participante par : participantes) {
                BigDecimal share = importe.multiply(BigDecimal.valueOf(par.getPorcentaje())).divide(BigDecimal.valueOf(100.0));
                res.put(par.getPersona(), share);
            }
        } else {
            BigDecimal share = importe.divide(BigDecimal.valueOf(n), 2, BigDecimal.ROUND_HALF_UP);
            for (Participante par : participantes) {
                res.put(par.getPersona(), share);
            }
        }
        return res;
    }

    public static class Participante {
        private final Persona persona;
        private final double porcentaje; // porcentaje 0..100, o -1 si equitativo

        public Participante(Persona persona, double porcentaje) {
            this.persona = persona;
            this.porcentaje = porcentaje;
        }

        public Persona getPersona() { return persona; }
        public double getPorcentaje() { return porcentaje; }

        @Override
        public String toString() {
            return persona + (porcentaje >= 0 ? String.format(" (%.1f%%)", porcentaje) : " (equitat.)");
        }
    }

    @Override
    public String toString() {
        return nombre + " [" + participantes.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "]";
    }
}

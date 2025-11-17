package es.um.gestiongastos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cuenta de gasto compartida. Una vez creada la lista de participantes no puede modificarse.
 * Permite distribución equitativa o porcentaje por participante. Guarda saldo y porcentaje por participante.
 */
public class CuentaGasto {
    private final String id;
    private String nombre;
    private final List<Participante> participantes; // lista inmutable externamente
    private boolean porcentajesFijos; // si true, cada participante tiene un porcentaje definido; si false: reparto equitativo

    private static final double EPS = 0.001;

    /**
     * Crea una cuenta con participantes y opcionalmente porcentajes.
     * Si porcentajes == null o vacío -> reparto equitativo (se asigna 100/n a cada participante).
     *
     * @param id id de la cuenta
     * @param nombre nombre de la cuenta
     * @param participantes lista de participantes (no nula, tamaño > 0)
     * @param porcentajes mapa Persona->Double con porcentajes (suma 100). Las claves deben coincidir con la lista de participantes si no es nulo.
     */
    public CuentaGasto(String id, String nombre, Collection<Persona> participantes, Map<Persona, Double> porcentajes) {
        if (id == null || nombre == null) throw new IllegalArgumentException("id y nombre no pueden ser nulos");
        if (participantes == null || participantes.isEmpty()) throw new IllegalArgumentException("debe haber al menos un participante al crear la cuenta");
        this.id = id;
        this.nombre = nombre;

        // Construimos la lista de Participante con porcentaje y saldo inicial 0
        List<Participante> list = new ArrayList<>();
        if (porcentajes == null || porcentajes.isEmpty()) {
            this.porcentajesFijos = false;
            int n = participantes.size();
            double equal = 100.0 / n;
            for (Persona p : participantes) {
                list.add(new Participante(p, equal, BigDecimal.ZERO));
            }
        } else {
            // validación: las claves del mapa deben coincidir con los participantes (misma cantidad y mismos elementos)
            Set<Persona> keys = new HashSet<>(porcentajes.keySet());
            Set<Persona> partsSet = new HashSet<>(participantes);
            if (!keys.equals(partsSet)) {
                throw new IllegalArgumentException("Las claves del mapa de porcentajes deben coincidir exactamente con los participantes proporcionados");
            }
            double sum = porcentajes.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 100.0) > EPS) {
                throw new IllegalArgumentException("Los porcentajes deben sumar 100");
            }
            this.porcentajesFijos = true;
            for (Persona p : participantes) {
                Double pr = porcentajes.get(p);
                if (pr == null) throw new IllegalArgumentException("Falta porcentaje para participante " + p);
                list.add(new Participante(p, pr, BigDecimal.ZERO));
            }
        }
        // Mantener orden predecible: usar lista inmutable
        this.participantes = Collections.unmodifiableList(list);
    }

    // Getter / Setter básicos
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<Participante> getParticipantes() { return participantes; }

    /**
     * Devuelve el saldo actual de un participante (0 si no existe).
     */
    public BigDecimal getSaldo(Persona persona) {
        Participante p = findParticipante(persona);
        if (p == null) return BigDecimal.ZERO;
        return p.getSaldo();
    }

    /**
     * Devuelve el porcentaje actual de un participante (0 si no existe).
     */
    public double getPorcentaje(Persona persona) {
        Participante p = findParticipante(persona);
        if (p == null) return 0.0;
        return p.getPorcentaje();
    }

    private Participante findParticipante(Persona persona) {
        for (Participante p : participantes) {
            if (p.getPersona().equals(persona)) return p;
        }
        return null;
    }

    /**
     * Redefine los porcentajes de los participantes. El mapa debe contener exactamente los mismos participantes
     * y los porcentajes deben sumar 100 (tolerancia EPS).
     * Una vez redefinidos, porcentajesFijos pasa a true.
     *
     * @param nuevosPorcentajes mapa Persona->Double con los nuevos porcentajes
     */
    public void redefinirPorcentajes(Map<Persona, Double> nuevosPorcentajes) {
        if (nuevosPorcentajes == null) throw new IllegalArgumentException("nuevosPorcentajes no puede ser nulo");
        Set<Persona> keys = new HashSet<>(nuevosPorcentajes.keySet());
        Set<Persona> parts = participantes.stream().map(Participante::getPersona).collect(Collectors.toSet());
        if (!keys.equals(parts)) {
            throw new IllegalArgumentException("Las claves del mapa deben coincidir exactamente con los participantes de la cuenta");
        }
        double sum = nuevosPorcentajes.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 100.0) > EPS) {
            throw new IllegalArgumentException("Los porcentajes deben sumar 100");
        }
        // Actualizamos porcentajes: Participante tiene setter package-private
        for (Participante par : participantes) {
            double pr = nuevosPorcentajes.get(par.getPersona());
            par.setPorcentaje(pr);
        }
        this.porcentajesFijos = true;
    }

    /**
     * Reparte un gasto entre los participantes según sus porcentajes actuales o reparto equitativo.
     * Actualiza los saldos internamente: cada participante tendrá su saldo reducido en su cuota
     * y el pagador incrementará su saldo en la suma de las cuotas de los demás (ajuste por redondeo incluido).
     *
     * Devuelve un mapa Persona -> cuota (cantidad que corresponde a cada persona).
     *
     * @param importe importe total (debe ser >= 0)
     * @param pagador persona que ha pagado (debe ser uno de los participantes)
     * @return mapa con la cuota de cada participante (sumará aproximadamente el importe, con ajuste en el pagador)
     */
    public Map<Persona, BigDecimal> repartirGasto(BigDecimal importe, Persona pagador) {
        if (importe == null) throw new IllegalArgumentException("importe no puede ser nulo");
        if (importe.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("importe debe ser >= 0");
        Participante pag = findParticipante(pagador);
        if (pag == null) throw new IllegalArgumentException("El pagador no es participante de esta cuenta");

        Map<Persona, BigDecimal> res = new LinkedHashMap<>();
        BigDecimal totalAsignado = BigDecimal.ZERO;
        // Calcular shares por participante según porcentaje
        for (Participante par : participantes) {
            BigDecimal porcentajeBD = BigDecimal.valueOf(par.getPorcentaje());
            BigDecimal share = importe.multiply(porcentajeBD).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP);
            res.put(par.getPersona(), share);
            totalAsignado = totalAsignado.add(share);
        }
        // Ajuste por redondeo: diferencia = importe - totalAsignado
        BigDecimal diferencia = importe.subtract(totalAsignado);
        if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
            // aplicamos la diferencia al pagador para que la suma total sea exactamente el importe
            res.put(pagador, res.get(pagador).add(diferencia));
        }

        // Actualizar saldos: cada participante pierde su cuota; pagador recibe las cuotas de los demás
        BigDecimal sumaCuotasOtros = BigDecimal.ZERO;
        for (Map.Entry<Persona, BigDecimal> e : res.entrySet()) {
            Persona persona = e.getKey();
            BigDecimal cuota = e.getValue();
            if (!persona.equals(pagador)) {
                // esa persona debe pagar cuota -> su saldo disminuye
                Participante par = findParticipante(persona);
                par.setSaldo(par.getSaldo().subtract(cuota));
                sumaCuotasOtros = sumaCuotasOtros.add(cuota);
            } else {
                // pagador: su saldo aumentará con lo que le deben los demás
                // (lo calculamos después)
            }
        }
        // incrementar saldo del pagador por la suma de las cuotas de los demás
        pag.setSaldo(pag.getSaldo().add(sumaCuotasOtros));

        return res;
    }

    /**
     * No se permiten añadir participantes después de crear la cuenta.
     * (Método presente para compatibilidad pero lanza excepción.)
     */
    public void addParticipanteEquitativo(Persona p) {
        throw new UnsupportedOperationException("No se pueden añadir participantes después de crear la cuenta");
    }

    @Override
    public String toString() {
        return nombre + " [" + participantes.stream().map(Participante::toString).collect(Collectors.joining(", ")) + "]";
    }

    /**
     * Clase interna que contiene persona, porcentaje y saldo.
     */
    public static class Participante {
        private final Persona persona;
        private double porcentaje; // mutable mediante redefinirPorcentajes
        private BigDecimal saldo; // saldo acumulado, positivo = le deben, negativo = debe

        public Participante(Persona persona, double porcentaje, BigDecimal saldoInicial) {
            if (persona == null) throw new IllegalArgumentException("persona no puede ser nula");
            this.persona = persona;
            this.porcentaje = porcentaje;
            this.saldo = saldoInicial != null ? saldoInicial : BigDecimal.ZERO;
        }

        public Persona getPersona() { return persona; }
        public double getPorcentaje() { return porcentaje; }
        public BigDecimal getSaldo() { return saldo; }

        /* package-private setters usados por la clase envolvente */
        void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
        void setSaldo(BigDecimal nuevoSaldo) { this.saldo = nuevoSaldo; }

        @Override
        public String toString() {
            return persona.toString() + (porcentaje >= 0 ? String.format(" (%.1f%%, saldo=%s)", porcentaje, saldo.setScale(2, RoundingMode.HALF_UP).toPlainString()) : " (equitat.)");
        }
    }
}

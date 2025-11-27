package es.um.gestiongastos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class GastosCompartidos {
    private final String id;
    private String nombre;
    private final List<Participante> participantes;
    private boolean porcentajesFijos;
    
    private final List<Gasto> historialGastos = new ArrayList<>();

    private static final double EPS = 0.001;

    public GastosCompartidos(String id, String nombre, Collection<Persona> participantes, Map<Persona, Double> porcentajes) {
        if (id == null || nombre == null) throw new IllegalArgumentException("id y nombre no pueden ser nulos");
        if (participantes == null || participantes.isEmpty()) throw new IllegalArgumentException("Debe haber al menos un participante");
        this.id = id;
        this.nombre = nombre;

        List<Participante> list = new ArrayList<>();
        if (porcentajes == null || porcentajes.isEmpty()) {
            this.porcentajesFijos = false;
            int n = participantes.size();
            double equal = 100.0 / n;
            for (Persona p : participantes) {
                list.add(new Participante(p, equal, BigDecimal.ZERO));
            }
        } else {
            Set<Persona> keys = new HashSet<>(porcentajes.keySet());
            Set<Persona> partsSet = new HashSet<>(participantes);
            if (!keys.equals(partsSet)) {
                throw new IllegalArgumentException("Las claves de porcentajes deben coincidir con los participantes");
            }
            double sum = porcentajes.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 100.0) > EPS) throw new IllegalArgumentException("Los porcentajes deben sumar 100");
            
            this.porcentajesFijos = true;
            for (Persona p : participantes) {
                list.add(new Participante(p, porcentajes.get(p), BigDecimal.ZERO));
            }
        }
        this.participantes = Collections.unmodifiableList(list);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<Participante> getParticipantes() { return participantes; }

    public BigDecimal getSaldo(Persona persona) {
        Participante p = findParticipante(persona);
        return (p == null) ? BigDecimal.ZERO : p.getSaldo();
    }
    
    public double getPorcentaje(Persona persona) {
        Participante p = findParticipante(persona);
        return (p == null) ? 0.0 : p.getPorcentaje();
    }

    private Participante findParticipante(Persona persona) {
        for (Participante p : participantes) {
            if (p.getPersona().equals(persona)) return p;
        }
        return null;
    }

    public List<Gasto> getGastos() {
        return Collections.unmodifiableList(historialGastos);
    }

    public void agregarGasto(Gasto gasto) {
        if (!historialGastos.contains(gasto)) {
            historialGastos.add(gasto);
            recalcularSaldos();
        }
    }

    public void eliminarGasto(Gasto gasto) {
        if (historialGastos.remove(gasto)) {
            recalcularSaldos();
        }
    }

    private void recalcularSaldos() {
        // 1. Resetear saldos a 0
        for (Participante p : participantes) {
            p.setSaldo(BigDecimal.ZERO);
        }
        // 2. Reaplicar todos los gastos
        for (Gasto g : historialGastos) {
            aplicarReparto(g.getImporte(), g.getPagador());
        }
    }

    private void aplicarReparto(BigDecimal importe, Persona pagador) {
        Participante pag = findParticipante(pagador);
        if (pag == null) return; 

        BigDecimal totalAsignado = BigDecimal.ZERO;
        
        for (Participante par : participantes) {
            BigDecimal porcentajeBD = BigDecimal.valueOf(par.getPorcentaje());
            // Calcula cuánto le toca pagar a este participante
            BigDecimal share = importe.multiply(porcentajeBD).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP);
            
            // Si NO es el pagador, su saldo baja (debe dinero)
            if (!par.getPersona().equals(pagador)) {
                par.setSaldo(par.getSaldo().subtract(share));
            } else {
                // Si ES el pagador, su saldo subirá por la parte que pagan los OTROS.
                // Lo gestionamos sumando todo lo que "deben los otros" al final o restando su propia parte del total pagado?
                // En el modelo anterior: pagador sube saldo por la suma de shares de los otros.
                // Equivalente matemático: Saldo += Pagado - SuParte.
            }
            
            // Para el cálculo de ajuste de céntimos, sumamos lo asignado
            totalAsignado = totalAsignado.add(share);
        }

        // Ajuste de céntimos (diferencia) se lo asignamos al pagador para cuadrar
        BigDecimal diferencia = importe.subtract(totalAsignado);
        
        BigDecimal suParteTeorica = importe.multiply(BigDecimal.valueOf(pag.getPorcentaje())).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP);
        BigDecimal incrementoPagador = importe.subtract(suParteTeorica).add(diferencia);
        pag.setSaldo(pag.getSaldo().add(incrementoPagador));
    }

    @Override
    public String toString() {
        return nombre;
    }

    // Clase interna Participante (sin cambios mayores, solo getters/setters package-private)
    public static class Participante {
        private final Persona persona;
        private double porcentaje; 
        private BigDecimal saldo; 

        public Participante(Persona persona, double porcentaje, BigDecimal saldoInicial) {
            this.persona = persona;
            this.porcentaje = porcentaje;
            this.saldo = saldoInicial != null ? saldoInicial : BigDecimal.ZERO;
        }
        public Persona getPersona() { return persona; }
        public double getPorcentaje() { return porcentaje; }
        public BigDecimal getSaldo() { return saldo; }
        void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
        void setSaldo(BigDecimal nuevoSaldo) { this.saldo = nuevoSaldo; }
        
        @Override
        public String toString() {
            return persona.getNombreUsuario();
        }
    }
}
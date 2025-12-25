package es.um.gestiongastos.model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Gasto {
    private String id;
    private BigDecimal importe;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    private Categoria categoria;
    private Persona pagador;
    private String descripcion;    
    private GastosCompartidos cuenta;

    public Gasto(String id, BigDecimal importe, LocalDate fecha, Categoria categoria, 
                 Persona pagador, String descripcion, GastosCompartidos cuenta) {
        if (cuenta == null) throw new IllegalArgumentException("El gasto debe pertenecer a una cuenta.");
        this.id = id;
        this.importe = importe;
        this.fecha = fecha;
        this.categoria = categoria;
        this.pagador = pagador;
        this.descripcion = descripcion;
        this.cuenta = cuenta;
    }
    public Gasto() {};

    public String getId() { return id; }
    
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
    
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    
    public Persona getPagador() { return pagador; }
    public void setPagador(Persona pagador) { this.pagador = pagador; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public GastosCompartidos getCuenta() { return cuenta; }
    public void setCuenta(GastosCompartidos cuenta) {
        if (cuenta == null) throw new IllegalArgumentException("La cuenta no puede ser nula");
        this.cuenta = cuenta;
    }

    /**
     * Calcula cuánto le corresponde pagar a una persona específica de este gasto
     * basándose en su porcentaje de participación en la cuenta.
     */
    public BigDecimal getCostePara(Persona p) {
        double porcentaje = cuenta.getPorcentaje(p);
        // Si no está en la cuenta o porcentaje 0, retorna 0
        if (porcentaje <= 0.001) return BigDecimal.ZERO;
        
        // Cálculo: Importe * (Porcentaje / 100)
        return importe.multiply(BigDecimal.valueOf(porcentaje))
                      .divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format("Gasto[%s] %s: %s € (%s) - %s", id, fecha, importe, categoria, pagador.getNombreUsuario());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Gasto)) return false;
        Gasto g = (Gasto) o;
        return Objects.equals(id, g.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
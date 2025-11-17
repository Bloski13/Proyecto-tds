
package es.um.gestiongastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Gasto {
    private final String id;
    private BigDecimal importe;
    private LocalDate fecha;
    private Categoria categoria;
    private Persona pagador; // persona que paga el gasto
    private String descripcion;

    public Gasto(String id, BigDecimal importe, LocalDate fecha, Categoria categoria, Persona pagador, String descripcion) {
        this.id = id;
        this.importe = importe;
        this.fecha = fecha;
        this.categoria = categoria;
        this.pagador = pagador;
        this.descripcion = descripcion;
    }

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

    @Override
    public String toString() {
        return String.format("Gasto[%s] %s: %s € (%s) - %s", id, fecha, importe, categoria, pagador);
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

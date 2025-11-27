package es.um.gestiongastos.importer;

public class GastoDTO {
    public String descripcion;
    public double importe;
    // El adaptador se encargará de parsear la fecha a LocalDate
    public String fecha; 
    public String categoria;
    public String nombreUsuarioPagador;
}
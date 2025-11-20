package es.um.gestiongastos.model;

import java.util.Objects;

public class Categoria {
    private String nombre;

    /**
     * Constructor.
     * @param nombre El nombre de la categoría (ej: "Transporte")
     */
    public Categoria(String nombre) {
        // Es buena práctica evitar nombres nulos
        if (nombre == null) {
            throw new IllegalArgumentException("El nombre de la categoría no puede ser nulo");
        }
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }
    
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() {
        // Al imprimir la categoría, ahora solo muestra el nombre limpio
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categoria)) return false;
        Categoria c = (Categoria) o;
        // La igualdad ahora depende del nombre
        return Objects.equals(nombre, c.nombre);
    }

    @Override
    public int hashCode() {
        // El hash también se genera a partir del nombre
        return Objects.hash(nombre);
    }
}


package es.um.gestiongastos.importer;

/**
 * Interfaz Adaptador: transforma datos externos al modelo de la aplicación.
 */
public interface AdaptadorImportador<T> {
    T adapt(String linea);
}

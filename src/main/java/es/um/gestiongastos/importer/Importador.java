
package es.um.gestiongastos.importer;

import java.io.InputStream;
import java.util.List;

/**
 * Interfaz para importadores de ficheros externos que devuelven una lista de Strings (por ejemplo, líneas)
 * o directamente objetos de dominio en implementaciones futuras.
 */
public interface Importador {
    List<String> importar(InputStream in) throws Exception;
}

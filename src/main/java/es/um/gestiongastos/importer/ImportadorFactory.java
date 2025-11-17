
package es.um.gestiongastos.importer;

/**
 * Factoría simple para obtener importadores según tipo. Implementación mínima por ahora.
 */
public class ImportadorFactory {
    public static Importador getImportadorCSV() {
        return in -> {
            java.util.List<String> lines = new java.util.ArrayList<>();
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) lines.add(line);
            }
            return lines;
        };
    }
}

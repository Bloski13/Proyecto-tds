package es.um.gestiongastos.importer;

import java.io.File;

public class ImportadorFactory {
    public static IImportadorCuenta getImportador(File archivo) {
        String nombre = archivo.getName().toLowerCase();
        
        if (nombre.endsWith(".json")) {
            return new AdaptadorJSON();
        } else if (nombre.endsWith(".yaml") || nombre.endsWith(".yml")) {
            return new AdaptadorYAML();
        } else if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) {
            return new AdaptadorCSV();
        }
        
        throw new IllegalArgumentException("Formato no soportado: " + nombre);
    }
}
package es.um.gestiongastos.importer;

import java.io.File;

/**
 * Target del patrón Adaptador.
 * Define la interfaz común que espera el Controlador.
 */
public interface IImportadorCuenta {
    CuentaDTO importar(File archivo) throws Exception;
}
package es.um.gestiongastos.importer;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.io.FileInputStream;
import java.io.File;

public class AdaptadorYAML implements IImportadorCuenta {
    @Override
    public CuentaDTO importar(File archivo) throws Exception {
        // Configuramos SnakeYAML para que confíe en nuestras clases
        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(CuentaDTO.class, options);
        Yaml yaml = new Yaml(constructor);
        
        try (FileInputStream fis = new FileInputStream(archivo)) {
            return yaml.load(fis);
        }
    }
}
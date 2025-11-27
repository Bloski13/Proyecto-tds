package es.um.gestiongastos.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;

public class AdaptadorJSON implements IImportadorCuenta {
    @Override
    public CuentaDTO importar(File archivo) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Jackson mapea automáticamente si los nombres coinciden
        return mapper.readValue(archivo, CuentaDTO.class);
    }
}
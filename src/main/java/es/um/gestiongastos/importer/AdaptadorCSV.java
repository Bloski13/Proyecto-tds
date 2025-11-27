package es.um.gestiongastos.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class AdaptadorCSV implements IImportadorCuenta {
    @Override
    public CuentaDTO importar(File archivo) throws Exception {
        CuentaDTO cuenta = new CuentaDTO();
        cuenta.participantes = new ArrayList<>();
        cuenta.gastos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;
                
                String[] partes = linea.split(";");
                String tipo = partes[0].toUpperCase().trim();

                switch (tipo) {
                    case "CUENTA":
                        if (partes.length > 1) cuenta.nombre = partes[1].trim();
                        break;
                    case "PARTICIPANTE":
                        // PARTICIPANTE;Nombre;User;Porcentaje
                        if (partes.length >= 4) {
                            ParticipanteDTO p = new ParticipanteDTO();
                            p.nombreCompleto = partes[1].trim();
                            p.nombreUsuario = partes[2].trim();
                            p.porcentaje = Double.parseDouble(partes[3].trim());
                            cuenta.participantes.add(p);
                        }
                        break;
                    case "GASTO":
                        // GASTO;Desc;Importe;Fecha;Categoria;Pagador
                        if (partes.length >= 6) {
                            GastoDTO g = new GastoDTO();
                            g.descripcion = partes[1].trim();
                            g.importe = Double.parseDouble(partes[2].trim());
                            g.fecha = partes[3].trim();
                            g.categoria = partes[4].trim();
                            g.nombreUsuarioPagador = partes[5].trim();
                            cuenta.gastos.add(g);
                        }
                        break;
                }
            }
        }
        return cuenta;
    }
}
package es.um.gestiongastos;

import es.um.gestiongastos.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AppMain {
    public static void main(String[] args) {
        System.out.println("--- Prueba de AppGastos ---");

        Categoria c1 = new Categoria("c1", "Explosivos");
        Categoria c2 = new Categoria("c2", "Miscelánea");

        Persona p1 = new Persona("p1", "Patri");
        Persona p2 = new Persona("p2", "Álvaro");
        Persona p3 = new Persona("p3", "Pablo");

        // Cuenta equitativa
        CuentaGasto cuenta = new CuentaGasto("ct1", "Bombas", null);
        cuenta.addParticipanteEquitativo(p1);
        cuenta.addParticipanteEquitativo(p2);
        cuenta.addParticipanteEquitativo(p3);

        System.out.println("Cuenta creada: " + cuenta);

        Gasto g1 = new Gasto("g1", new BigDecimal("5"), LocalDate.of(2025,7,1), c2, p1, "Pólvora");
        Gasto g2 = new Gasto("g2", new BigDecimal("20"), LocalDate.of(2025,7,2), c1, p2, "TNT");

        System.out.println(g1);
        System.out.println(g2);

        System.out.println("Reparto " + g1.getDescripcion() + ":"); 
        Map<Persona, BigDecimal> reparto1 = cuenta.repartirGasto(g1.getImporte());
        reparto1.forEach((p, v) -> System.out.println("  " + p + " -> " + v + " €"));

        System.out.println("Reparto " + g2.getDescripcion() + ":"); 
        Map<Persona, BigDecimal> reparto2 = cuenta.repartirGasto(g2.getImporte());
        reparto2.forEach((p, v) -> System.out.println("  " + p + " -> " + v + " €"));

        // Alerta ejemplo
        Alerta alertaSem = new Alerta("a1", "Límite semanal general", Periodicidad.SEMANAL, null, new EstrategiaPorUmbral(10));
        boolean disparada = alertaSem.evaluar(10);
        System.out.println("Alerta disparada? " + disparada);
        alertaSem.getHistorial().forEach(System.out::println);

        System.out.println("--- Fin prueba ---");
    }
}

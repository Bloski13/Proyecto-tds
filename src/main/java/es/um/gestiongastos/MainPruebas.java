package es.um.gestiongastos;

import es.um.gestiongastos.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public class MainPruebas {
    public static void main(String[] args) {
        System.out.println("--- Prueba de AppGastos (cuentas y alertas) ---");

        // --- Preparación básica (categorías, personas) ---
        Categoria c1 = new Categoria("c1", "Explosivos");
        Categoria c2 = new Categoria("c2", "Miscelánea");

        Persona p1 = new Persona("p1", "Patricia Conesa", "patri", "123");
        Persona p2 = new Persona("p2", "Álvaro Sancho", "alvaro", "123");
        Persona p3 = new Persona("p3", "Pablo Asensio", "pablo", "123");

        // --- Prueba de CuentaGasto (como antes) ---
        List<Persona> participantes = Arrays.asList(p1, p2, p3);
        CuentaGasto cuenta = new CuentaGasto("ct1", "Bombas", participantes, null);

        System.out.println("Cuenta creada: " + cuenta);
        System.out.println("Porcentajes iniciales:");
        for (Persona p : participantes) {
            System.out.printf("  %s -> %.2f%%%n", p, cuenta.getPorcentaje(p));
        }

        Gasto g1 = new Gasto("g1", new BigDecimal("5.00"), LocalDate.of(2025,7,1), c2, p1, "Pólvora");
        System.out.println("\nReparto " + g1.getDescripcion() + " (pagador: " + g1.getPagador() + "):");
        Map<Persona, BigDecimal> reparto1 = cuenta.repartirGasto(g1.getImporte(), g1.getPagador());
        reparto1.forEach((p, v) -> System.out.println("  " + p + " -> " + v.setScale(2, RoundingMode.HALF_UP).toPlainString() + " €"));

        System.out.println("\nSaldos tras gasto 1:");
        for (Persona p : participantes) {
            System.out.println("  " + p + " saldo=" + cuenta.getSaldo(p).setScale(2, RoundingMode.HALF_UP).toPlainString() + " €");
        }

        // --- PRUEBAS DE ALERTAS / NOTIFICACIONES ---
        System.out.println("\n--- Pruebas de alertas ---");

        // Creamos una alerta con umbral 10 (disparará si suma>10). Límite historial = 3.
        Alerta alertaUmbral = new Alerta("a1", "Límite semanal general", Periodicidad.SEMANAL, null,
                new EstrategiaPorUmbral(10.0), 3);

        // Listener simple que imprime la notificación (simula envío por email/push)
        NotificacionListener impresora = new NotificacionListener() {
            @Override
            public void onNotificacion(Notificacion n, Alerta fuente) {
                System.out.println("[LISTENER] Nueva notificación de alerta '" + fuente.getNombre() + "': " + n.getMensaje());
            }
        };

        alertaUmbral.registerListener(impresora);

        // Evaluación 1: importe 11 -> debe disparar
        boolean d1 = alertaUmbral.evaluar(11.0);
        System.out.println("Alerta disparada (importe 11)? " + d1);

        // Evaluación 2: importe 5 -> no dispara
        boolean d2 = alertaUmbral.evaluar(5.0);
        System.out.println("Alerta disparada (importe 5)? " + d2);

        // Forzamos varias notificaciones para comprobar límite de historial
        alertaUmbral.evaluar(12.0); // 2ª notificación
        alertaUmbral.evaluar(13.0); // 3ª notificación
        alertaUmbral.evaluar(14.0); // 4ª notificación -> al tener límite=3 la más antigua se elimina

        System.out.println("\nHistorial completo (de mayor antigüedad a más reciente):");
        alertaUmbral.getHistorial().forEach(System.out::println);

        System.out.println("\nÚltimas 2 notificaciones:");
        alertaUmbral.getUltimasNotificaciones(2).forEach(System.out::println);

        // Probamos clearHistorial
        alertaUmbral.clearHistorial();
        System.out.println("\nHistorial limpiado. Tamaño ahora: " + alertaUmbral.getHistorial().size());

        // --- Alerta ligada a categoría (ejemplo) ---
        Alerta alertaExplosivos = new Alerta("a2", "Gastos explosivos", Periodicidad.MENSUAL, c1,
                new EstrategiaPorUmbral(50.0)); // estrategia actual ignora categoría pero la alerta la contiene

        alertaExplosivos.registerListener((n, fuente) ->
            System.out.println("[LISTENER-EXP] Notificación de " + fuente.getNombre() + ": " + n.getMensaje())
        );

        System.out.println("\nEvaluando alerta de explosivos con importe 60.0 -> debería disparar:");
        System.out.println("  Disparada? " + alertaExplosivos.evaluar(60.0));

        System.out.println("\nEvaluando alerta de explosivos con importe 40.0 -> no debería disparar:");
        System.out.println("  Disparada? " + alertaExplosivos.evaluar(40.0));

        System.out.println("\n--- Fin pruebas ---");
    }
}

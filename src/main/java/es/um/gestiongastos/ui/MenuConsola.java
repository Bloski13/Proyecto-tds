package es.um.gestiongastos.ui;

import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.stream.Collectors; 

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Gasto;
import es.um.gestiongastos.model.GastosCompartidos;
import es.um.gestiongastos.model.GastosCompartidos.Participante;
import es.um.gestiongastos.model.Persona;

public class MenuConsola {

    private Scanner scanner;
    private DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MenuConsola() {
        this.scanner = new Scanner(System.in);
    }

    public void iniciar() {
        Persona usuario = Controlador.getInstancia().getUsuarioAutenticado();
        
        if (usuario == null) {
            System.err.println("ERROR: La Consola no se puede iniciar sin un usuario. Saliendo.");
            return;
        }

        System.out.println("\n=== GESTIÓN DE GASTOS (MODO CONSOLA) ===");
        System.out.println("Sesión activa para: " + usuario.getNombreCompleto());
        System.out.println("----------------------------------------");
        System.out.println("Sistema listo. Esperando instrucciones...");

        boolean salir = false;
        imprimirOpciones();
        
        // Bucle principal
        while (!salir) {
            // Callback para refrescar menú si hay cambios externos
            Controlador.getInstancia().setOnConsolaRefrescar(this::imprimirOpciones);
            
            if (scanner.hasNextLine()) {
                String entrada = scanner.nextLine();

                switch (entrada) {
                    case "1": 
                        opcionRegistrarGasto(); 
                        break;
                    case "2": 
                        opcionModificarGasto(); 
                        break;
                    case "3": 
                        opcionBorrarGasto(); 
                        break;
                    case "4": 
                        opcionListarGastos(); 
                        break;
                    case "0":
                        salir = true; // Rompemos el bucle para salir ordenadamente
                        break;
                    default:
                        System.out.println("❌ Opción no reconocida. Intente de nuevo.");
                        imprimirOpciones();
                }
            }
        }
        
        // Mensaje de despedida al salir del bucle
        System.out.println("\nCerrando aplicación... ¡Hasta pronto, " + usuario.getNombreUsuario() + "!");
        
        // Cierre ordenado
        javafx.application.Platform.exit(); 
        System.exit(0); 
    }
    
    private void imprimirOpciones() {
        System.out.println("\n--- MENÚ PRINCIPAL ---");
        System.out.println("1. Registrar Gasto");
        System.out.println("2. Modificar Gasto (Editar, Mover o cambiar Pagador)");
        System.out.println("3. Borrar Gasto");
        System.out.println("4. Listar Todos (Detallado con ID Completo)");
        System.out.println("0. Salir");
        System.out.print("Elija una opción: ");
    }

    // --- MÉTODOS AUXILIARES ---

    private String elegirCategoria() {
        List<String> categorias = Controlador.getInstancia().getNombresCategorias();
        
        if (categorias.isEmpty()) {
            System.out.println("No hay categorías registradas.");
            return pedirNombreNuevaCategoria();
        }

        while (true) {
            System.out.println("\n--- Seleccione Categoría ---");
            for (int i = 0; i < categorias.size(); i++) {
                System.out.printf("%d. %s%n", (i + 1), categorias.get(i));
            }
            int opcionNueva = categorias.size() + 1;
            System.out.printf("%d. Nueva Categoría%n", opcionNueva);
            System.out.print("Opción: ");

            String entrada = scanner.nextLine().trim();
            try {
                int opcion = Integer.parseInt(entrada);
                if (opcion >= 1 && opcion <= categorias.size()) {
                    return categorias.get(opcion - 1);
                } else if (opcion == opcionNueva) {
                    return pedirNombreNuevaCategoria();
                } else {
                    System.out.println("❌ Opción inválida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor, introduzca un número.");
            }
        }
    }

    private String pedirNombreNuevaCategoria() {
        while (true) {
            System.out.print("Escriba el nombre de la nueva Categoría: ");
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) return nombre;
            System.out.println("❌ El nombre no puede estar vacío.");
        }
    }

    // --- ACCIONES --- //
    
    private void opcionRegistrarGasto() {
        try {
            System.out.println("\n--- NUEVO GASTO ---");
            
            Persona usuario = Controlador.getInstancia().getUsuarioAutenticado();
            List<GastosCompartidos> cuentas = usuario.getCuentas();
            
            if (cuentas.isEmpty()) {
                System.out.println("❌ Error: No tienes cuentas asociadas.");
                return;
            }

            System.out.println("Seleccione la cuenta para imputar el gasto:");
            for (int i = 0; i < cuentas.size(); i++) {
                System.out.printf("%d. %s%n", (i + 1), cuentas.get(i).getNombre());
            }
            System.out.print("Número de cuenta: ");
            int indiceCuenta = Integer.parseInt(scanner.nextLine()) - 1;

            if (indiceCuenta < 0 || indiceCuenta >= cuentas.size()) {
                System.out.println("❌ Selección inválida.");
                return;
            }
            GastosCompartidos cuentaDestino = cuentas.get(indiceCuenta);

            System.out.print("Concepto: ");
            String concepto = scanner.nextLine();

            System.out.print("Importe (€): ");
            double importe = Double.parseDouble(scanner.nextLine());

            System.out.print("Fecha (dd/MM/yyyy): ");
            LocalDate fecha = LocalDate.parse(scanner.nextLine(), formatoFecha);

            String nombreCategoria = elegirCategoria();

            Controlador.getInstancia().registrarGasto(concepto, importe, fecha, nombreCategoria, cuentaDestino);

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void opcionModificarGasto() {
        System.out.println("\n--- MODIFICAR GASTO ---");
        System.out.print("ID del gasto (copie y pegue el ID completo): ");
        String idGasto = scanner.nextLine().trim();

        Gasto gastoOriginal = Controlador.getInstancia().obtenerGastoPorId(idGasto);
        if (gastoOriginal == null) {
            System.out.println("❌ Error: No se encontró el gasto con ID: " + idGasto);
            return;
        }

        try {
            System.out.println("Editando: " + gastoOriginal.getDescripcion() + " (" + gastoOriginal.getImporte() + "€)");
            System.out.println("(Deje vacío para mantener el valor actual)");
            
            System.out.print("Nuevo Concepto: ");
            String nuevoConcepto = scanner.nextLine();
            if (nuevoConcepto.isEmpty()) nuevoConcepto = null;

            System.out.print("Nuevo Importe: ");
            String impStr = scanner.nextLine();
            Double nuevoImporte = impStr.isEmpty() ? null : Double.parseDouble(impStr);
            
            System.out.print("Nueva Fecha (dd/MM/yyyy): ");
            String fecStr = scanner.nextLine();
            LocalDate nuevaFecha = fecStr.isEmpty() ? null : LocalDate.parse(fecStr, formatoFecha);

            // Categoría
            String nuevaCat = null;
            System.out.println("Categoría actual: " + gastoOriginal.getCategoria().getNombre());
            System.out.print("¿Desea cambiar la categoría? (s/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                nuevaCat = elegirCategoria();
            }
            
            // Cuenta
            GastosCompartidos nuevaCuenta = gastoOriginal.getCuenta(); 
            System.out.println("Cuenta actual: " + gastoOriginal.getCuenta().getNombre());
            System.out.print("¿Desea mover el gasto a otra cuenta? (s/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                List<GastosCompartidos> cuentas = Controlador.getInstancia().getUsuarioAutenticado().getCuentas();
                for (int i = 0; i < cuentas.size(); i++) {
                    System.out.printf("%d. %s%n", (i + 1), cuentas.get(i).getNombre());
                }
                System.out.print("Seleccione nueva cuenta: ");
                try {
                    int idx = Integer.parseInt(scanner.nextLine()) - 1;
                    if (idx >= 0 && idx < cuentas.size()) {
                        nuevaCuenta = cuentas.get(idx);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Inválido. Se mantiene la original.");
                }
            }
            
            // Pagador
            Persona nuevoPagador = null;
            System.out.println("Pagador actual: " + gastoOriginal.getPagador().getNombreUsuario());
            System.out.print("¿Desea cambiar el pagador? (s/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                List<Persona> participantes = nuevaCuenta.getParticipantes().stream()
                        .map(Participante::getPersona)
                        .collect(Collectors.toList());
                
                System.out.println("Participantes en cuenta destino:");
                for (int i = 0; i < participantes.size(); i++) {
                    System.out.printf("%d. %s%n", (i+1), participantes.get(i).getNombreCompleto());
                }
                
                System.out.print("Seleccione pagador: ");
                try {
                    int idx = Integer.parseInt(scanner.nextLine()) - 1;
                    if (idx >= 0 && idx < participantes.size()) {
                        nuevoPagador = participantes.get(idx);
                    }
                } catch (Exception e) {
                    System.out.println("Inválido. Se mantiene el original.");
                }
            }
            
            Controlador.getInstancia().modificarGasto(
                idGasto, nuevoConcepto, nuevoImporte, nuevaFecha, nuevaCat, nuevaCuenta, nuevoPagador
            );

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        imprimirOpciones();
    }

    private void opcionBorrarGasto() {
        System.out.print("ID del gasto a borrar: ");
        String id = scanner.nextLine().trim();
        Controlador.getInstancia().borrarGasto(id);
    }

    private void opcionListarGastos() {
        List<Gasto> gastos = Controlador.getInstancia().getGastosUsuarioActual();
        Persona yo = Controlador.getInstancia().getUsuarioAutenticado();
        
        System.out.println("\n--- LISTADO DE GASTOS ---");
        if (gastos.isEmpty()) {
            System.out.println("No hay gastos.");
            imprimirOpciones();
            return;
        }
        
        String format = "%-37s | %-20s | %-10s | %-12s | %-15s | %-12s | %-10s%n";
        
        System.out.printf(format, "ID Completo", "Concepto", "Total", "Fecha", "Cuenta", "Pagado Por", "Mi Coste");
        System.out.println("-".repeat(130));

        for (Gasto g : gastos) {
            String idCompleto = g.getId();
            String desc = g.getDescripcion().length() > 20 ? g.getDescripcion().substring(0, 17)+"..." : g.getDescripcion();
            String cta = g.getCuenta().getNombre().length() > 15 ? g.getCuenta().getNombre().substring(0, 12)+"..." : g.getCuenta().getNombre();
            String pagador = g.getPagador().equals(yo) ? "Mí" : g.getPagador().getNombreUsuario();
            if (pagador.length() > 12) pagador = pagador.substring(0, 9) + "...";
            BigDecimal miParte = g.getCostePara(yo);
            
            System.out.printf(format, 
                idCompleto, 
                desc, 
                g.getImporte() + "€", 
                g.getFecha().format(formatoFecha), 
                cta, 
                pagador, 
                miParte + "€");
        }
        System.out.println("-".repeat(130));
        imprimirOpciones();
    }
}
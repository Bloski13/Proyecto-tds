package es.um.gestiongastos.ui;

import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Gasto;
import es.um.gestiongastos.model.Persona;

public class MenuConsola {

    private Scanner scanner;
    // Formato para leer fechas (ej: 29/09/2025)
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
        System.out.println("✅ Sesión activa para: " + usuario.getNombreCompleto());
        System.out.println("----------------------------------------");

        boolean salir = false;
        imprimirOpciones();
        while (!salir) {
        	// Registrar el callback de refresco
            Controlador.getInstancia().setOnConsolaRefrescar(this::imprimirOpciones);
            String entrada = scanner.nextLine();

            switch (entrada) {
                case "1":
                    opcionRegistrarGasto();
                    break;
                case "2":
                    opcionModificarGasto(); // Incompleto
                    break;
                case "3":
                    opcionBorrarGasto();
                    break;
                case "4":
                    opcionListarGastos(); 
                    break;
                case "0":
                	salir = true;
                    System.out.println("Saliendo del sistema...");
                    // Forzar el cierre de la JVM
                    javafx.application.Platform.exit(); // Cierra JavaFX ordenadamente
                    System.exit(0); // Mata todos los hilos inmediatamente
                    break;
                    
                default:
                    System.out.println("Opción no reconocida. Intente de nuevo.");
                    imprimirOpciones();
            }
        }
    }
    private void imprimirOpciones() {
        System.out.println("\n--- MENÚ PRINCIPAL ---");
        System.out.println("1. Registrar Gasto");
        System.out.println("2. Modificar Gasto");
        System.out.println("3. Borrar Gasto");
        System.out.println("4. Listar Todos (del usuario actual)");
        System.out.println("0. Salir");
        System.out.print("Elija una opción: ");
        
    }

    // --- ACCIONES --- //
    
    // Registar gasto
    private void opcionRegistrarGasto() {
        try {
            System.out.println("\n--- NUEVO GASTO ---");
            
            System.out.print("Concepto/Descripción: ");
            String concepto = scanner.nextLine();

            System.out.print("Importe (use punto para decimales, ej: 10.50): ");
            double importe = Double.parseDouble(scanner.nextLine());

            System.out.print("Fecha (dd/MM/yyyy): ");
            String fechaStr = scanner.nextLine();
            LocalDate fecha = LocalDate.parse(fechaStr, formatoFecha);

            // 🔴 NUEVA LÓGICA DE VISUALIZACIÓN DE CATEGORÍAS
            List<String> categorias = Controlador.getInstancia().getNombresCategorias();
            
            System.out.println("------------------------------------------------");
            if (categorias.isEmpty()) {
                System.out.println("No hay categorías registradas. La que escribas será la primera.");
            } else {
                // Usamos String.join para mostrarlas bonitas separadas por comas
                System.out.println("Categorías existentes: [" + String.join(", ", categorias) + "]");
            }
            System.out.println("ℹ️  NOTA: Escribe una de la lista para seleccionarla.");
            System.out.println("          Si escribes una palabra distinta, se CREARÁ una nueva categoría.");
            System.out.println("------------------------------------------------");
            
            System.out.print("Nombre de la Categoría: ");
            String nombreCategoria = scanner.nextLine();

            // El controlador ya sabe qué hacer: busca si existe o crea si no existe
            Controlador.getInstancia().registrarGasto(concepto, importe, fecha, nombreCategoria);
            
            // (El mensaje de éxito lo imprime el Controlador)

        } catch (NumberFormatException e) {
            System.out.println("❌ Error: El importe debe ser un número válido (ej: 15.50).");
        } catch (DateTimeParseException e) {
            System.out.println("❌ Error: Fecha inválida. Asegúrese de usar el formato dd/MM/yyyy.");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Error de validación: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error inesperado: " + e.getMessage());
        }
    }

    //  MODIFICAR GASTO
    private void opcionModificarGasto() {
    	
        System.out.println("\n--- MODIFICAR GASTO ---");
        System.out.print("Introduzca el ID del gasto a modificar: ");
        String idGasto = scanner.nextLine();

        // 1. Intentamos obtener el gasto para ver si existe
        Gasto gastoOriginal = Controlador.getInstancia().obtenerGastoPorId(idGasto);

        if (gastoOriginal == null) {
            System.out.println("❌ Error: No se encontró ningún gasto con el ID " + idGasto + ".");
            return;
        }

        try {
            System.out.println("\n--- Gasto encontrado ---");
            System.out.println("Original: " + gastoOriginal.toString());
            System.out.println("-------------------------");
            
            // 2. Pedimos los nuevos valores, permitiendo dejar vacío para no modificar
            
            System.out.print("Nuevo Concepto/Descripción (dejar vacío para mantener '" + gastoOriginal.getDescripcion() + "'): ");
            String nuevoConcepto = scanner.nextLine();
            if (nuevoConcepto.isEmpty()) nuevoConcepto = gastoOriginal.getDescripcion();

            System.out.print("Nuevo Importe (€) (dejar vacío para mantener '" + gastoOriginal.getImporte() + "'): ");
            String nuevoImporteStr = scanner.nextLine();
            Double nuevoImporte = nuevoImporteStr.isEmpty() ? null : Double.parseDouble(nuevoImporteStr);
            
            System.out.print("Nueva Fecha (dd/MM/yyyy) (dejar vacío para mantener '" + gastoOriginal.getFecha().format(formatoFecha) + "'): ");
            String nuevaFechaStr = scanner.nextLine();
            LocalDate nuevaFecha = nuevaFechaStr.isEmpty() ? null : LocalDate.parse(nuevaFechaStr, formatoFecha);

            System.out.print("Nueva Categoría (dejar vacío para mantener '" + gastoOriginal.getCategoria().getNombre() + "'): ");
            String nuevaCategoria = scanner.nextLine();
            if (nuevaCategoria.isEmpty()) nuevaCategoria = gastoOriginal.getCategoria().getNombre();
            
            // 3. LLAMADA REAL AL CONTROLADOR para aplicar los cambios
            Controlador.getInstancia().modificarGasto(
                idGasto, 
                nuevoConcepto, 
                nuevoImporte, // Nullable
                nuevaFecha,   // Nullable
                nuevaCategoria
            );

            System.out.println("✅ Gasto modificado con éxito.");

        } catch (NumberFormatException e) {
            System.out.println("❌ Error: El nuevo importe introducido no es un número válido.");
        } catch (DateTimeParseException e) {
            System.out.println("❌ Error: La nueva fecha es inválida. Formato requerido: dd/MM/yyyy.");
        } catch (Exception e) {
            System.out.println("❌ Error inesperado al modificar: " + e.getMessage());
        }
    	imprimirOpciones();
    }
    // BORRAR GASTO
    private void opcionBorrarGasto() {
        System.out.print("Introduzca el ID del gasto a borrar: ");
        String id = scanner.nextLine();
        Controlador.getInstancia().borrarGasto(id);
    }

    // LISTAR GASTOS
    private void opcionListarGastos() {
        List<Gasto> gastos = Controlador.getInstancia().getGastosUsuarioActual();
        
        System.out.println("\n--- LISTADO DE GASTOS ---");
        
        if (gastos.isEmpty()) {
            System.out.println("No hay gastos registrados para el usuario actual.");
            imprimirOpciones();
            return;
        }
        
        // Formato de cabecera
        String format = "%-12s | %-40s | %-10s | %-15s | %-20s%n";
        System.out.printf(format, "ID (Corto)", "Concepto", "Importe (€)", "Fecha", "Categoría");
        System.out.println("-----------------------------------------------------------------------------------------------------------------");

        for (Gasto gasto : gastos) {
            // Mostramos solo los primeros 8 caracteres del ID para que la tabla sea legible
            String idCorto = gasto.getId().substring(0, 8) + "...";
            String importeStr = String.format("%.2f", gasto.getImporte().doubleValue());
            
            System.out.printf(format, 
                idCorto, 
                gasto.getDescripcion(), 
                importeStr, 
                gasto.getFecha().format(formatoFecha), 
                gasto.getCategoria().getNombre());
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        System.out.printf("Total de gastos listados: %d%n", gastos.size());
        imprimirOpciones();
    }
}
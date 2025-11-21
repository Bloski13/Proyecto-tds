package es.um.gestiongastos.controlador;

import es.um.gestiongastos.model.*;
import es.um.gestiongastos.ui.*;
import javafx.application.Platform;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
// Importación necesaria para el dinero
import java.math.BigDecimal; 

public class Controlador {

    private static Controlador instancia;

    private final Map<String, Persona> usuariosPorNombre;
    private final Map<String, Categoria> categoriasExistentes; 
    private Persona usuarioAutenticado;
    // Guardará la función de refresco de la GUI
    private Runnable onModeloCambiado; // Runnable es perfecto para esto, no necesita argumentos
    // Guardará la función de refresco de la Consola
    private Runnable onConsolaRefrescar;
    

    // Constructor privado (Singleton)
    private Controlador() {
        this.usuariosPorNombre = new LinkedHashMap<>();
        this.categoriasExistentes = new HashMap<>();
        inicializarDatosEjemplo();
        
    }

    public static synchronized Controlador getInstancia() {
        if (instancia == null) {
            instancia = new Controlador();
        }
        return instancia;
    }

    private void inicializarDatosEjemplo() {
        Persona p1 = new Persona("p1", "Patricia Conesa", "patri", "pass1");
        Persona p2 = new Persona("p2", "Álvaro Sancho", "alvaro", "pass2");
        Persona p3 = new Persona("p3", "Pablo Asensio", "pablo", "pass3");

        usuariosPorNombre.put(p1.getNombreUsuario(), p1);
        usuariosPorNombre.put(p2.getNombreUsuario(), p2);
        usuariosPorNombre.put(p3.getNombreUsuario(), p3);

        crearCategoriaSiNoExiste("Alimentación");
        crearCategoriaSiNoExiste("Transporte");
        crearCategoriaSiNoExiste("Entretenimiento");
    }
    
    // --- GESTIÓN DE USUARIOS ---

    public Optional<Persona> autenticar(String nombreUsuario, String contraseña) {
        if (nombreUsuario == null || contraseña == null) return Optional.empty();
        Persona p = usuariosPorNombre.get(nombreUsuario);
        if (p != null && contraseña.equals(p.getContraseña())) {
            this.usuarioAutenticado = p; 
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public Persona registrarUsuario(String nombreCompleto, String nombreUsuario, String contraseña) {
    	 if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
             throw new IllegalArgumentException("El nombre completo no puede estar vacío");
         }
         if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
             throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
         }
         if (contraseña == null || contraseña.isEmpty()) {
             throw new IllegalArgumentException("La contraseña no puede estar vacía");
         }
         if (usuariosPorNombre.containsKey(nombreUsuario)) {
             throw new IllegalArgumentException("El nombre de usuario ya existe: " + nombreUsuario);
         }
        String id = UUID.randomUUID().toString();
        Persona nueva = new Persona(id, nombreCompleto, nombreUsuario, contraseña);
        usuariosPorNombre.put(nueva.getNombreUsuario(), nueva);
        return nueva;
    }

    // --- GESTIÓN DE GASTOS ---

    /**
     * Registra un gasto adaptándose al constructor de tu clase Gasto.
     */
    public void registrarGasto(String concepto, double importe, LocalDate fecha, String nombreCategoria) {
        if (usuarioAutenticado == null) {
            System.err.println("Error: No hay usuario identificado.");
            return; 
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del gasto no puede ser futura.");
        }

        // 1. Obtener Categoría
        Categoria categoria = crearCategoriaSiNoExiste(nombreCategoria);

        // 2. Preparar datos para tu constructor de Gasto
        String idGasto = UUID.randomUUID().toString(); // Generamos ID único
        BigDecimal importeBD = BigDecimal.valueOf(importe); // Convertimos double a BigDecimal

        // 3. Crear el Gasto (Usando tu constructor exacto)
        // Orden: id, importe, fecha, categoria, pagador, descripcion
        Gasto nuevoGasto = new Gasto(
            idGasto, 
            importeBD, 
            fecha, 
            categoria, 
            usuarioAutenticado, // El pagador es el usuario actual
            concepto            // La descripción es el concepto
        );
        usuarioAutenticado.agregarGasto(nuevoGasto); // Guardamos el gasto en la persona
        // 4. Guardar/Asignar
        
        System.out.println("\n>> [Controlador] Gasto creado con éxito:");
        System.out.println("   " + nuevoGasto);
        notificarModeloCambiado();
        
    }
    
    /**
     * Busca y elimina un Gasto de la lista del usuario autenticado por su ID.
     * @param idGasto El ID único del gasto a borrar.
     */
    public void borrarGasto(String idGasto) {
        if (usuarioAutenticado == null) {
            System.err.println("Error: No hay usuario identificado para borrar el gasto.");
            return; 
        }

        // Usamos removeIf para iterar sobre la lista de gastos del usuario autenticado y eliminar el que coincida con el ID.
        boolean eliminado = usuarioAutenticado.getGastos().removeIf(gasto -> gasto.getId().equals(idGasto));

        if (eliminado) {
            
            System.out.println("\n>> [Controlador] Gasto con ID " + idGasto + " eliminado con éxito.");
            notificarModeloCambiado();
        } else {
            // Esto solo ocurre si el ID era correcto pero el gasto ya no estaba (raro)
            System.out.println("\n>> [Controlador] Advertencia: No se encontró ningún gasto con ID " + idGasto + ".");
        }
    }

    public List<Gasto> getGastosUsuarioActual() {
        if (usuarioAutenticado == null) return Collections.emptyList();
        return usuarioAutenticado.getGastos();
    }
    
    // --- MÉTODOS PARA MODIFICACIÓN DE GASTOS ---
    /**
     * Busca un gasto por su ID dentro de la lista del usuario autenticado.
     * @return El objeto Gasto si existe, o null si no se encuentra.
     */
    public Gasto obtenerGastoPorId(String idGasto) {
        if (usuarioAutenticado == null || idGasto == null) return null;
        
        return usuarioAutenticado.getGastos().stream()
                .filter(g -> g.getId().equals(idGasto))
                .findFirst()
                .orElse(null);
    }

    /**
     * Modifica los datos de un gasto existente.
     * Si un parámetro es null, se ignora (mantiene el valor original).
     */
    public void modificarGasto(String idGasto, String nuevoConcepto, Double nuevoImporte, LocalDate nuevaFecha, String nombreCategoria) {
        Gasto gasto = obtenerGastoPorId(idGasto);
        
        if (gasto == null) {
            // Puedes lanzar excepción o imprimir error, depende de tu gusto
            throw new IllegalArgumentException("No se encontró el gasto para modificar.");
        }

        // 🔴 NUEVA VALIDACIÓN: Si hay nueva fecha, comprobar que no sea futura
        if (nuevaFecha != null && nuevaFecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del gasto no puede ser futura.");
        }

        // 1. Actualizar Concepto
        if (nuevoConcepto != null && !nuevoConcepto.isEmpty()) {
            gasto.setDescripcion(nuevoConcepto);
        }

        // 2. Actualizar Importe
        if (nuevoImporte != null) {
            gasto.setImporte(BigDecimal.valueOf(nuevoImporte));
        }

        // 3. Actualizar Fecha (Ya validada arriba)
        if (nuevaFecha != null) {
            gasto.setFecha(nuevaFecha);
        }

        // 4. Actualizar Categoría
        if (nombreCategoria != null && !nombreCategoria.isEmpty()) {
            Categoria nuevaCat = crearCategoriaSiNoExiste(nombreCategoria);
            gasto.setCategoria(nuevaCat);
        }

        // 5. Notificar cambios
        System.out.println(">> [Controlador] Gasto modificado correctamente.");
        System.out.flush();
        
        notificarModeloCambiado();
    }
    
    
    // GESTIÓN DE CATEGORÍAS //
    
    private Categoria crearCategoriaSiNoExiste(String nombre) {
        String key = nombre.toLowerCase().trim();
        if (!categoriasExistentes.containsKey(key)) {
            // Usamos el constructor de Categoria(nombre) que arreglamos antes
            Categoria nueva = new Categoria(nombre); 
            categoriasExistentes.put(key, nueva);
        }
        return categoriasExistentes.get(key);
    }
    /**
     * Devuelve una lista con los nombres de todas las categorías registradas en el sistema.
     * Útil para llenar el ComboBox de la interfaz gráfica.
     */
    public List<String> getNombresCategorias() {
        // Convertimos los valores del mapa a una lista de Strings (nombres)
        return categoriasExistentes.values().stream()
                .map(Categoria::getNombre)
                .sorted() // Orden alfabético para que quede bonito
                .collect(Collectors.toList());
    }
    // ================================================================= //
    // Métodos actualizar ventana por acciones de consola y viceversa	 //
    // ================================================================= //
    
    // Para que la GUI se registre
    public void setOnModeloCambiado(Runnable callback) {
        this.onModeloCambiado = callback;
    }
    
    // Para notificar a la GUI
    private void notificarModeloCambiado() {
    	// Notificar a la GUI (CRÍTICO: debe ir en Platform.runLater)
        if (onModeloCambiado != null) {
            // Ejecutamos en el hilo de JavaFX
            Platform.runLater(onModeloCambiado); 
        }
        
        // Notificar a la Consola (puede ir directamente aquí)
        if (onConsolaRefrescar != null) {
            onConsolaRefrescar.run(); // Ejecutamos repintado del menú
        }
    }
    
    // Para que el MenuConsola se registre
    public void setOnConsolaRefrescar(Runnable callback) {
        this.onConsolaRefrescar = callback;
    }
    
    // --- UI JAVAFX ---
    public void abrirVentanaPrincipalPersona(Persona autenticado) {
        this.usuarioAutenticado = autenticado;
        List<Persona> lista = Collections.unmodifiableList(new ArrayList<>(usuariosPorNombre.values()));
        Platform.runLater(() -> VentanaPrincipalPersona.mostrar(lista, autenticado));
        lanzarMenuConsola();
    }
    /**
     * Lanza la interfaz de línea de comandos en un hilo separado 
     * para que no bloquee la interfaz gráfica de JavaFX.
     */
    private void lanzarMenuConsola() {
        // En este punto, usuarioAutenticado ya está establecido por autenticar()
        if (usuarioAutenticado == null) return;
        
        // Creamos y lanzamos un nuevo hilo (Thread)
        new Thread(() -> {
            MenuConsola menu = new MenuConsola();
            menu.iniciar();
        }, "CLI-Thread").start();
        
        System.out.println("\n>> [Controlador] Menú de consola lanzado. "
                         + "Puede interactuar con él en la pestaña 'Console' de Eclipse.");
    }
    public Persona getUsuarioAutenticado() {
        return this.usuarioAutenticado;
    }
}
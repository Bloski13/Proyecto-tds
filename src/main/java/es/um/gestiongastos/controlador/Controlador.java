package es.um.gestiongastos.controlador;

import es.um.gestiongastos.model.*;
import es.um.gestiongastos.ui.*;
import es.um.gestiongastos.importer.*;
import javafx.application.Platform;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal; 

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;

public class Controlador {

    private static Controlador instancia;

    private final Map<String, Persona> usuariosPorNombre;
    private final Map<String, Categoria> categoriasExistentes; 
    private Persona usuarioAutenticado;
    
    private Runnable onModeloCambiado;
    private Runnable onConsolaRefrescar;

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
        registrarUsuario("Patricia Conesa", "patri", "pass1");
        registrarUsuario("Álvaro Sancho", "alvaro", "pass2");
        registrarUsuario("Pablo Asensio", "pablo", "pass3");

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
        
        // AUTOMÁTICO: Crear "Cuenta Personal" para este usuario
        crearCuentaCompartida("Cuenta Personal (" + nueva.getNombreUsuario() + ")", List.of(nueva), null);
        
        return nueva;
    }
    
    /**
     * Devuelve lista de usuarios para poder agregarlos a grupos (excluyendo al actual si se desea).
     */
    public List<Persona> getTodosLosUsuarios() {
        return new ArrayList<>(usuariosPorNombre.values());
    }

    // --- GESTIÓN DE CUENTAS ---

    public void crearCuentaCompartida(String nombre, List<Persona> participantes, Map<Persona, Double> porcentajes) {
        String id = UUID.randomUUID().toString();
        GastosCompartidos nuevaCuenta = new GastosCompartidos(id, nombre, participantes, porcentajes);
        
        // Vincular la cuenta a TODOS los participantes
        for (Persona p : participantes) {
            p.agregarCuenta(nuevaCuenta);
        }
        notificarModeloCambiado();
    }

    // --- GESTIÓN DE GASTOS ---

    /**
     * Registra un gasto en una cuenta específica.
     */
    public void registrarGasto(String concepto, double importe, LocalDate fecha, String nombreCategoria, GastosCompartidos cuentaDestino) {
        if (usuarioAutenticado == null) {
            System.err.println("Error: No hay usuario identificado.");
            return; 
        }
        if (cuentaDestino == null) {
             throw new IllegalArgumentException("Debe seleccionar una cuenta para el gasto.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del gasto no puede ser futura.");
        }

        Categoria categoria = crearCategoriaSiNoExiste(nombreCategoria);
        String idGasto = UUID.randomUUID().toString(); 
        BigDecimal importeBD = BigDecimal.valueOf(importe); 

        Gasto nuevoGasto = new Gasto(
            idGasto, 
            importeBD, 
            fecha, 
            categoria, 
            usuarioAutenticado, 
            concepto,
            cuentaDestino // Asignamos la cuenta
        );
        
        // Delegamos en la cuenta la gestión del gasto y saldos
        cuentaDestino.agregarGasto(nuevoGasto);
        
        System.out.println(">> [Controlador] Gasto creado en cuenta '" + cuentaDestino.getNombre() + "': " + nuevoGasto);
        notificarModeloCambiado();
        comprobarAlertas();
    }
    
    public void borrarGasto(String idGasto) {
        if (usuarioAutenticado == null) return;

        // Buscar el gasto en todas las cuentas del usuario
        // Como el ID es único, paramos al encontrarlo
        for (GastosCompartidos cuenta : usuarioAutenticado.getCuentas()) {
            Optional<Gasto> target = cuenta.getGastos().stream()
                    .filter(g -> g.getId().equals(idGasto))
                    .findFirst();
            
            if (target.isPresent()) {
                cuenta.eliminarGasto(target.get());
                System.out.println(">> [Controlador] Gasto eliminado de la cuenta " + cuenta.getNombre());
                notificarModeloCambiado();
                return;
            }
        }
        System.out.println(">> [Controlador] No se encontró gasto con ID " + idGasto);
    }

    /**
     * Devuelve TODOS los gastos visibles para el usuario (la suma de todas sus cuentas).
     */
    public List<Gasto> getGastosUsuarioActual() {
        if (usuarioAutenticado == null) return Collections.emptyList();
        
        return usuarioAutenticado.getCuentas().stream()
                .flatMap(c -> c.getGastos().stream())
                .collect(Collectors.toList());
    }
    
    public Gasto obtenerGastoPorId(String idGasto) {
        return getGastosUsuarioActual().stream()
                .filter(g -> g.getId().equals(idGasto))
                .findFirst()
                .orElse(null);
    }

    public void modificarGasto(String idGasto, String nuevoConcepto, Double nuevoImporte, 
            LocalDate nuevaFecha, String nombreCategoria, 
            GastosCompartidos nuevaCuenta, Persona nuevoPagador) { // <--- NUEVO ARGUMENTO

		Gasto gasto = obtenerGastoPorId(idGasto);
		if (gasto == null) throw new IllegalArgumentException("No se encontró el gasto.");
		
		GastosCompartidos cuentaOriginal = gasto.getCuenta();
		
		// 1. Sacamos el gasto de su cuenta actual (importante para que se recalculen saldos sin él)
		cuentaOriginal.eliminarGasto(gasto);
		
		// 2. Aplicamos cambios básicos
		if (nuevoConcepto != null && !nuevoConcepto.isEmpty()) gasto.setDescripcion(nuevoConcepto);
		if (nuevoImporte != null) gasto.setImporte(java.math.BigDecimal.valueOf(nuevoImporte));
		if (nuevaFecha != null) {
			if (nuevaFecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("Fecha futura no permitida");
			gasto.setFecha(nuevaFecha);
		}
		
		if (nombreCategoria != null && !nombreCategoria.isEmpty()) {
			gasto.setCategoria(crearCategoriaSiNoExiste(nombreCategoria));
		}
		
		// 3. CAMBIO DE PAGADOR
		if (nuevoPagador != null) {
			gasto.setPagador(nuevoPagador);
		}
		
		// 4. Gestionamos el cambio de cuenta
		if (nuevaCuenta != null && !nuevaCuenta.equals(cuentaOriginal)) {
			// Validar que el pagador pertenece a la nueva cuenta
			// (Si cambiamos de cuenta, el pagador actual o nuevo debe ser miembro de esa nueva cuenta)
			Persona pagadorFinal = (nuevoPagador != null) ? nuevoPagador : gasto.getPagador();
			
			// Verificación simple: buscamos si el pagador está en la lista de participantes de la nueva cuenta
			boolean esMiembro = nuevaCuenta.getParticipantes().stream()
			 .anyMatch(p -> p.getPersona().equals(pagadorFinal));
			
			if (!esMiembro) {
				// Si el pagador no está en la nueva cuenta, reasignamos al usuario actual por defecto o lanzamos error.
				// Para ser seguros, lanzamos excepción.
				// Pero como hemos sacado el gasto, debemos meterlo de nuevo en la original antes de fallar para no perderlo.
				cuentaOriginal.agregarGasto(gasto); 
				throw new IllegalArgumentException("El pagador " + pagadorFinal.getNombreUsuario() + " no pertenece a la cuenta destino.");
			}
			
			gasto.setCuenta(nuevaCuenta);
			nuevaCuenta.agregarGasto(gasto); // Añadir y recalcular en la nueva
			System.out.println(">> [Controlador] Gasto MOVIDO a cuenta '" + nuevaCuenta.getNombre() + "'");
		} else {
			// Si es la misma cuenta, lo volvemos a meter (trigger recalculo con nuevo pagador/importe)
			cuentaOriginal.agregarGasto(gasto);
		}
		
		notificarModeloCambiado();
		comprobarAlertas();
	}
    
    // GESTIÓN DE CATEGORÍAS //
    
    private Categoria crearCategoriaSiNoExiste(String nombre) {
        String key = nombre.toLowerCase().trim();
        if (!categoriasExistentes.containsKey(key)) {
            Categoria nueva = new Categoria(nombre); 
            categoriasExistentes.put(key, nueva);
        }
        return categoriasExistentes.get(key);
    }

    public List<String> getNombresCategorias() {
        return categoriasExistentes.values().stream()
                .map(Categoria::getNombre)
                .sorted()
                .collect(Collectors.toList());
    }

    // EVENTOS UI //
    
    public void setOnModeloCambiado(Runnable callback) {
        this.onModeloCambiado = callback;
    }
    
    private void notificarModeloCambiado() {
        if (onModeloCambiado != null) Platform.runLater(onModeloCambiado); 
        if (onConsolaRefrescar != null) onConsolaRefrescar.run(); 
    }
    
    public void setOnConsolaRefrescar(Runnable callback) {
        this.onConsolaRefrescar = callback;
    }
    
    // UI JAVAFX //
    public void abrirVentanaPrincipalPersona(Persona autenticado) {
        this.usuarioAutenticado = autenticado;
        List<Persona> lista = new ArrayList<>(usuariosPorNombre.values());
        Platform.runLater(() -> VentanaPrincipalPersona.mostrar(lista, autenticado));
        lanzarMenuConsola();
    }

    private void lanzarMenuConsola() {
        if (usuarioAutenticado == null) return;
        new Thread(() -> {
            MenuConsola menu = new MenuConsola();
            menu.iniciar();
        }, "CLI-Thread").start();
    }
    
    public Persona getUsuarioAutenticado() {
        return this.usuarioAutenticado;
    }
    
    // IMPORTACIÓN DE ARCHIVOS

    public void importarCuenta(File archivo) {
        try {
            // 1. Obtener el adaptador adecuado usando la Factoría
            IImportadorCuenta importador = ImportadorFactory.getImportador(archivo);
            
            // 2. Usar el adaptador para obtener el DTO (Patrón Adaptador en acción)
            CuentaDTO dto = importador.importar(archivo);

            System.out.println(">> [Controlador] Importando cuenta: " + dto.nombre);

            // 3. (El resto de la lógica de procesamiento del DTO es IDÉNTICA a la anterior)
            procesarDTOImportado(dto); // He extraído la lógica a un método privado para limpieza
            
            notificarModeloCambiado();
            System.out.println(">> [Controlador] Cuenta importada con éxito.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al importar: " + e.getMessage());
        }
    }

    // Método auxiliar privado con la lógica de negocio (Crear usuarios, cuentas, gastos)
    // Es el mismo código que tenías dentro del try de Jackson, movido aquí.
    private void procesarDTOImportado(CuentaDTO dto) {
        Map<Persona, Double> mapaPorcentajes = new HashMap<>();
        List<Persona> listaParticipantes = new ArrayList<>();

        if (dto.participantes != null) {
            for (ParticipanteDTO pDto : dto.participantes) {
                Persona persona = usuariosPorNombre.get(pDto.nombreUsuario);
                if (persona == null) {
                    System.out.println("   -> Creando usuario: " + pDto.nombreUsuario);
                    persona = registrarUsuario(pDto.nombreCompleto, pDto.nombreUsuario, "1234");
                }
                listaParticipantes.add(persona);
                mapaPorcentajes.put(persona, pDto.porcentaje); 
            }
        }

        double suma = mapaPorcentajes.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(suma - 100.0) > 0.1) mapaPorcentajes = null;

        String idCuenta = UUID.randomUUID().toString();
        GastosCompartidos nuevaCuenta = new GastosCompartidos(idCuenta, dto.nombre, listaParticipantes, mapaPorcentajes);
        
        for (Persona p : listaParticipantes) p.agregarCuenta(nuevaCuenta);

        if (dto.gastos != null) {
            for (GastoDTO gDto : dto.gastos) {
                Categoria cat = crearCategoriaSiNoExiste(gDto.categoria);
                Persona pagador = usuariosPorNombre.get(gDto.nombreUsuarioPagador);
                if (pagador == null && !listaParticipantes.isEmpty()) pagador = listaParticipantes.get(0);
                
                // Parseo de fecha flexible (el DTO trae String)
                LocalDate fecha = LocalDate.parse(gDto.fecha); // Asume yyyy-MM-dd estándar

                String idGasto = UUID.randomUUID().toString();
                Gasto nuevoGasto = new Gasto(idGasto, BigDecimal.valueOf(gDto.importe), fecha, cat, pagador, gDto.descripcion, nuevaCuenta);
                nuevaCuenta.agregarGasto(nuevoGasto);
            }
        }
        
        // Importante: comprobar alertas tras importación masiva
        comprobarAlertas();
    }
    
    // --- GESTIÓN DE ALERTAS Y NOTIFICACIONES ---

    public void crearAlerta(String nombre, Periodicidad periodicidad, String nombreCategoria, double umbralMaximo) {
        if (usuarioAutenticado == null) return;
        
        Categoria cat = null;
        if (nombreCategoria != null && !nombreCategoria.equals("Todas") && !nombreCategoria.isEmpty()) {
            cat = crearCategoriaSiNoExiste(nombreCategoria);
        }

        String id = UUID.randomUUID().toString();
        // Usamos el Patrón Estrategia: "EstrategiaPorUmbral"
        Alerta alerta = new Alerta(id, nombre, periodicidad, cat, new EstrategiaPorUmbral(umbralMaximo));
        
        usuarioAutenticado.agregarAlerta(alerta);
        
        System.out.println(">> [Controlador] Alerta creada: " + alerta);
        
        // Comprobamos inmediatamente por si ya se ha pasado
        comprobarAlertas();
        notificarModeloCambiado();
    }

    public void borrarAlerta(Alerta alerta) {
        if (usuarioAutenticado != null) {
            usuarioAutenticado.eliminarAlerta(alerta);
            notificarModeloCambiado();
        }
    }

    /**
     * Recorre todas las alertas del usuario, calcula el gasto acumulado relevante
     * y genera notificaciones si corresponde.
     */
    private void comprobarAlertas() {
        if (usuarioAutenticado == null) return;

        List<Gasto> todosLosGastos = getGastosUsuarioActual(); // Obtiene gastos de todas las cuentas
        LocalDate hoy = LocalDate.now();

        for (Alerta alerta : usuarioAutenticado.getAlertas()) {
            
            // 1. Filtrar gastos que aplican a esta alerta (Fecha y Categoría)
            double totalAcumulado = todosLosGastos.stream()
                .filter(g -> {
                    // A. Filtro de Categoría
                    if (alerta.getCategoriaOpcional().isPresent()) {
                        if (!g.getCategoria().equals(alerta.getCategoriaOpcional().get())) return false;
                    }
                    
                    // B. Filtro de Periodicidad
                    if (alerta.getPeriodicidad() == Periodicidad.MENSUAL) {
                        // Mismo mes y año
                        return g.getFecha().getMonth() == hoy.getMonth() && g.getFecha().getYear() == hoy.getYear();
                    } else {
                        WeekFields weekFields = WeekFields.of(Locale.getDefault());
                        int semanaGasto = g.getFecha().get(weekFields.weekOfWeekBasedYear());
                        int semanaHoy = hoy.get(weekFields.weekOfWeekBasedYear());
                        return semanaGasto == semanaHoy && g.getFecha().getYear() == hoy.getYear();
                    }
                })
                .mapToDouble(g -> g.getCostePara(usuarioAutenticado).doubleValue()) // Importante: Usamos MI coste
                .sum();

            // 2. Preguntar a la alerta (Estrategia) si se dispara
            if (alerta.comprobar(totalAcumulado)) {
                generarNotificacion(alerta, totalAcumulado);
            }
        }
    }

    private void generarNotificacion(Alerta alerta, double totalActual) {
        String mensaje = String.format("¡Cuidado! Has superado tu límite en '%s'. Llevas %.2f €.", 
                                       alerta.getNombre(), totalActual);
        
        Notificacion notif = new Notificacion(UUID.randomUUID().toString(), LocalDateTime.now(), mensaje);
        usuarioAutenticado.agregarNotificacion(notif);
    }
}
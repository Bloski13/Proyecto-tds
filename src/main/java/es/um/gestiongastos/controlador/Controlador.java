package es.um.gestiongastos.controlador;

import es.um.gestiongastos.model.*;
import es.um.gestiongastos.ui.*;
import es.um.gestiongastos.importer.*;
// Importamos directamente la clase concreta, sin interfaz
import es.um.gestiongastos.persistencia.RepositorioJSON;

import javafx.application.Platform;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;

public class Controlador {

    private static Controlador instancia;

    // Referencia directa a la clase concreta
    private final RepositorioJSON repositorio;
    
    private Persona usuarioAutenticado;
    
    private Runnable onModeloCambiado;
    private Runnable onConsolaRefrescar;

    private Controlador() {
        this.repositorio = new RepositorioJSON();
        
    }

    public static synchronized Controlador getInstancia() {
        if (instancia == null) {
            instancia = new Controlador();
        }
        return instancia;
    }


    
    // --- GESTIÓN DE USUARIOS ---

    public Optional<Persona> autenticar(String nombreUsuario, String contraseña) {
        if (nombreUsuario == null || contraseña == null) return Optional.empty();
        
        // Buscamos directamente en el repositorio
        Persona p = repositorio.buscarUsuarioPorNombre(nombreUsuario);
        
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
         
         // Validación contra el repo
         if (repositorio.buscarUsuarioPorNombre(nombreUsuario) != null) {
             throw new IllegalArgumentException("El nombre de usuario ya existe: " + nombreUsuario);
         }

        String id = UUID.randomUUID().toString();
        Persona nueva = new Persona(id, nombreCompleto, nombreUsuario, contraseña);
        
        // Guardamos en persistencia
        repositorio.registrarUsuario(nueva);
        
        // Creamos su cuenta personal (esto modifica al usuario 'nueva', así que habrá que actualizarlo)
        crearCuentaCompartida("Cuenta Personal (" + nueva.getNombreUsuario() + ")", List.of(nueva), null);
        
        return nueva;
    }
    
    public List<Persona> getTodosLosUsuarios() {
        return repositorio.getUsuarios();
    }

    // --- GESTIÓN DE CUENTAS ---

    public void crearCuentaCompartida(String nombre, List<Persona> participantes, Map<Persona, Double> porcentajes) {
        String id = UUID.randomUUID().toString();
        GastosCompartidos nuevaCuenta = new GastosCompartidos(id, nombre, participantes, porcentajes);
        
        // Vinculamos la cuenta a los participantes y guardamos los cambios de cada persona
        for (Persona p : participantes) {
            p.agregarCuenta(nuevaCuenta);
            repositorio.actualizarUsuario(p); // Forzamos guardado del usuario modificado
        }
        notificarModeloCambiado();
    }

    // --- GESTIÓN DE GASTOS ---

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

        // Recuperamos la categoría del repo (o la creamos y guardamos)
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
            cuentaDestino
        );
        
        // Modificamos el modelo en memoria
        cuentaDestino.agregarGasto(nuevoGasto);
        
        System.out.println(">> [Controlador] Gasto creado en cuenta '" + cuentaDestino.getNombre() + "': " + nuevoGasto);
        
        // PERSISTENCIA: Al añadir un gasto, cambia el estado de la Cuenta y del Usuario.
        // Guardamos el usuario actual (que contiene la cuenta) para persistir todo el árbol.
        repositorio.actualizarUsuario(usuarioAutenticado);
        
        notificarModeloCambiado();
        comprobarAlertas();
    }
    
    public void borrarGasto(String idGasto) {
        if (usuarioAutenticado == null) return;

        for (GastosCompartidos cuenta : usuarioAutenticado.getCuentas()) {
            Optional<Gasto> target = cuenta.getGastos().stream()
                    .filter(g -> g.getId().equals(idGasto))
                    .findFirst();
            
            if (target.isPresent()) {
                cuenta.eliminarGasto(target.get());
                System.out.println(">> [Controlador] Gasto eliminado de la cuenta " + cuenta.getNombre());
                
                // Guardamos cambios
                repositorio.actualizarUsuario(usuarioAutenticado);
                
                notificarModeloCambiado();
                return;
            }
        }
        System.out.println(">> [Controlador] No se encontró gasto con ID " + idGasto);
    }

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
            GastosCompartidos nuevaCuenta, Persona nuevoPagador) {

        Gasto gasto = obtenerGastoPorId(idGasto);
        if (gasto == null) throw new IllegalArgumentException("No se encontró el gasto.");
        
        GastosCompartidos cuentaOriginal = gasto.getCuenta();
        
        // 1. Sacamos el gasto temporalmente
        cuentaOriginal.eliminarGasto(gasto);
        
        // 2. Aplicamos cambios
        if (nuevoConcepto != null && !nuevoConcepto.isEmpty()) gasto.setDescripcion(nuevoConcepto);
        if (nuevoImporte != null) gasto.setImporte(BigDecimal.valueOf(nuevoImporte));
        if (nuevaFecha != null) {
            if (nuevaFecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("Fecha futura no permitida");
            gasto.setFecha(nuevaFecha);
        }
        
        if (nombreCategoria != null && !nombreCategoria.isEmpty()) {
            gasto.setCategoria(crearCategoriaSiNoExiste(nombreCategoria));
        }
        
        if (nuevoPagador != null) {
            gasto.setPagador(nuevoPagador);
        }
        
        // 3. Gestionamos cambio de cuenta
        if (nuevaCuenta != null && !nuevaCuenta.equals(cuentaOriginal)) {
            Persona pagadorFinal = (nuevoPagador != null) ? nuevoPagador : gasto.getPagador();
            boolean esMiembro = nuevaCuenta.getParticipantes().stream()
             .anyMatch(p -> p.getPersona().equals(pagadorFinal));
            
            if (!esMiembro) {
                cuentaOriginal.agregarGasto(gasto); // Restaurar si falla
                throw new IllegalArgumentException("El pagador no pertenece a la cuenta destino.");
            }
            
            gasto.setCuenta(nuevaCuenta);
            nuevaCuenta.agregarGasto(gasto);
        } else {
            cuentaOriginal.agregarGasto(gasto);
        }
        
        // Guardamos cambios
        repositorio.actualizarUsuario(usuarioAutenticado);
        
        notificarModeloCambiado();
        comprobarAlertas();
    }
    
    // --- GESTIÓN DE CATEGORÍAS ---
    
    private Categoria crearCategoriaSiNoExiste(String nombre) {
        String key = nombre.trim();
        // Buscamos en el repo
        Categoria existente = repositorio.buscarCategoriaPorNombre(key);
        
        if (existente == null) {
            Categoria nueva = new Categoria(key);
            repositorio.registrarCategoria(nueva); // Persistir
            return nueva;
        }
        return existente;
    }

    public List<String> getNombresCategorias() {
        return repositorio.getCategorias().stream()
                .map(Categoria::getNombre)
                .sorted()
                .collect(Collectors.toList());
    }

    // --- EVENTOS UI ---
    
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
    
    // --- UI Y APP ---
    
    public void abrirVentanaPrincipalPersona(Persona autenticado) {
        this.usuarioAutenticado = autenticado;
        // Obtenemos lista actualizada del repo
        List<Persona> lista = repositorio.getUsuarios();
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
    
    // --- IMPORTACIÓN ---

    public void importarCuenta(File archivo) {
        try {
            IImportadorCuenta importador = ImportadorFactory.getImportador(archivo);
            CuentaDTO dto = importador.importar(archivo);
            System.out.println(">> [Controlador] Importando cuenta: " + dto.nombre);

            procesarDTOImportado(dto);
            
            // Persistir tras importar
            if (usuarioAutenticado != null) {
                repositorio.actualizarUsuario(usuarioAutenticado);
            }
            
            notificarModeloCambiado();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al importar: " + e.getMessage());
        }
    }

    private void procesarDTOImportado(CuentaDTO dto) {
        Map<Persona, Double> mapaPorcentajes = new HashMap<>();
        List<Persona> listaParticipantes = new ArrayList<>();

        if (dto.participantes != null) {
            for (ParticipanteDTO pDto : dto.participantes) {
                // Buscamos en el repo
                Persona persona = repositorio.buscarUsuarioPorNombre(pDto.nombreUsuario);
                
                if (persona == null) {
                    System.out.println("   -> Creando usuario importado: " + pDto.nombreUsuario);
                    // Esto ya guarda automáticamente dentro de registrarUsuario
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
        
        for (Persona p : listaParticipantes) {
            p.agregarCuenta(nuevaCuenta);
            repositorio.actualizarUsuario(p); // Importante: guardar cada participante modificado
        }

        if (dto.gastos != null) {
            for (GastoDTO gDto : dto.gastos) {
                Categoria cat = crearCategoriaSiNoExiste(gDto.categoria);
                Persona pagador = repositorio.buscarUsuarioPorNombre(gDto.nombreUsuarioPagador);
                if (pagador == null && !listaParticipantes.isEmpty()) pagador = listaParticipantes.get(0);
                
                LocalDate fecha = LocalDate.parse(gDto.fecha);
                String idGasto = UUID.randomUUID().toString();
                Gasto nuevoGasto = new Gasto(idGasto, BigDecimal.valueOf(gDto.importe), fecha, cat, pagador, gDto.descripcion, nuevaCuenta);
                nuevaCuenta.agregarGasto(nuevoGasto);
            }
        }
        comprobarAlertas();
    }
    
    // --- ALERTAS ---

    public void crearAlerta(String nombre, Periodicidad periodicidad, String nombreCategoria, double umbralMaximo) {
        if (usuarioAutenticado == null) return;
        
        Categoria cat = null;
        if (nombreCategoria != null && !nombreCategoria.equals("Todas") && !nombreCategoria.isEmpty()) {
            cat = crearCategoriaSiNoExiste(nombreCategoria);
        }

        String id = UUID.randomUUID().toString();
        Alerta alerta = new Alerta(id, nombre, periodicidad, cat, new EstrategiaPorUmbral(umbralMaximo));
        
        usuarioAutenticado.agregarAlerta(alerta);
        
        // Guardamos cambios
        repositorio.actualizarUsuario(usuarioAutenticado);
        
        System.out.println(">> [Controlador] Alerta creada y guardada: " + alerta);
        comprobarAlertas();
        notificarModeloCambiado();
    }

    public void borrarAlerta(Alerta alerta) {
        if (usuarioAutenticado != null) {
            usuarioAutenticado.eliminarAlerta(alerta);
            repositorio.actualizarUsuario(usuarioAutenticado); // Guardamos cambios
            notificarModeloCambiado();
        }
    }

    private void comprobarAlertas() {
        if (usuarioAutenticado == null) return;

        List<Gasto> todosLosGastos = getGastosUsuarioActual();
        LocalDate hoy = LocalDate.now();
        boolean algunaDisparada = false;

        for (Alerta alerta : usuarioAutenticado.getAlertas()) {
            double totalAcumulado = todosLosGastos.stream()
                .filter(g -> {
                    if (alerta.getCategoriaOpcional().isPresent()) {
                        if (!g.getCategoria().equals(alerta.getCategoriaOpcional().get())) return false;
                    }
                    if (alerta.getPeriodicidad() == Periodicidad.MENSUAL) {
                        return g.getFecha().getMonth() == hoy.getMonth() && g.getFecha().getYear() == hoy.getYear();
                    } else {
                        WeekFields weekFields = WeekFields.of(Locale.getDefault());
                        int semanaGasto = g.getFecha().get(weekFields.weekOfWeekBasedYear());
                        int semanaHoy = hoy.get(weekFields.weekOfWeekBasedYear());
                        return semanaGasto == semanaHoy && g.getFecha().getYear() == hoy.getYear();
                    }
                })
                .mapToDouble(g -> g.getCostePara(usuarioAutenticado).doubleValue())
                .sum();

            if (alerta.comprobar(totalAcumulado)) {
                generarNotificacion(alerta, totalAcumulado);
                algunaDisparada = true;
            }
        }
        
        if (algunaDisparada) {
            repositorio.actualizarUsuario(usuarioAutenticado); // Guardamos las nuevas notificaciones
        }
    }

    private void generarNotificacion(Alerta alerta, double totalActual) {
        String mensaje = String.format("¡Cuidado! Has superado tu límite en '%s'. Llevas %.2f €.", 
                                       alerta.getNombre(), totalActual);
        
        Notificacion notif = new Notificacion(UUID.randomUUID().toString(), LocalDateTime.now(), mensaje);
        usuarioAutenticado.agregarNotificacion(notif);
    }
}

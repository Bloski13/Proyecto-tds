package es.um.gestiongastos.persistencia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.type.TypeReference;
import es.um.gestiongastos.model.Persona;
import es.um.gestiongastos.model.Categoria;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RepositorioJSON {

    private static final String FICHERO_USUARIOS = "datos_usuarios.json";
    private static final String FICHERO_CATEGORIAS = "datos_categorias.json";
    
    private final ObjectMapper mapper;
    
    // CACHÉ EN MEMORIA (Para no leer el fichero 100 veces por segundo)
    private Map<String, Persona> personas;
    private Map<String, Categoria> categorias;

    public RepositorioJSON() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        this.personas = new HashMap<>();
        this.categorias = new HashMap<>();
        
        // Cargar datos al arrancar la aplicación
        cargarTodo();
    }

    // --- MÉTODOS QUE LLAMA EL CONTROLADOR ---

    /**
     * Devuelve todos los usuarios (para listados o login).
     */
    public List<Persona> getUsuarios() {
        return new ArrayList<>(personas.values());
    }

    /**
     * Busca un usuario por su nick.
     * Retorna null si no existe.
     */
    public Persona buscarUsuarioPorNombre(String nombre) {
        if (nombre == null) return null;
        return personas.get(nombre); // Búsqueda rápida en memoria
    }

    /**
     * Registra un nuevo usuario y guarda en disco inmediatamente.
     */
    public void registrarUsuario(Persona persona) {
        personas.put(persona.getNombreUsuario(), persona);
        guardarUsuarios(); 
    }

    /**
     * Se llama cuando modificamos un usuario (añadir gasto, cuenta, alerta...).
     * Fuerza el volcado a disco de los cambios.
     */
    public void actualizarUsuario(Persona persona) {
        // Como en Java los objetos van por referencia, el objeto 'persona'
        // ya está actualizado dentro del mapa 'cacheUsuarios'.
        // Solo necesitamos guardar el estado actual del mapa en el fichero.
        guardarUsuarios();
    }

    // --- GESTIÓN DE CATEGORÍAS ---

    public List<Categoria> getCategorias() {
        return new ArrayList<>(categorias.values());
    }

    public Categoria buscarCategoriaPorNombre(String nombre) {
        if (nombre == null) return null;
        return categorias.get(nombre.toLowerCase());
    }

    public void registrarCategoria(Categoria categoria) {
        categorias.put(categoria.getNombre().toLowerCase(), categoria);
        guardarCategorias();
    }

    // --- MÉTODOS PRIVADOS DE PERSISTENCIA (Lectura/Escritura Real) ---

    private void guardarUsuarios() {
        try {
            // Convertimos el Mapa a Lista para guardarlo
            List<Persona> listaAGuardar = new ArrayList<>(personas.values());
            
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(FICHERO_USUARIOS), listaAGuardar);
                  
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error grave: No se han podido guardar los usuarios.");
        }
    }
    
    private void guardarCategorias() {
        try {
            List<Categoria> listaAGuardar = new ArrayList<>(categorias.values());
            
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(FICHERO_CATEGORIAS), listaAGuardar);
                  
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarTodo() {
        // 1. Cargar Categorías
        File fCat = new File(FICHERO_CATEGORIAS);
        if (fCat.exists()) {
            try {
                List<Categoria> lista = mapper.readValue(fCat, new TypeReference<List<Categoria>>(){});
                for (Categoria c : lista) {
                    categorias.put(c.getNombre().toLowerCase(), c);
                }
            } catch (IOException e) { 
                e.printStackTrace(); 
            }
        }

        // 2. Cargar Usuarios
        File fUser = new File(FICHERO_USUARIOS);
        if (fUser.exists()) {
            try {
                List<Persona> lista = mapper.readValue(fUser, new TypeReference<List<Persona>>(){});
                for (Persona p : lista) {
                    personas.put(p.getNombreUsuario(), p);
                }
            } catch (IOException e) { 
                e.printStackTrace(); 
            }
        }
    }
}

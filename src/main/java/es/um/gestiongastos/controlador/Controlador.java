package es.um.gestiongastos.controlador;

import es.um.gestiongastos.model.*;
import es.um.gestiongastos.ui.*;
import javafx.application.Platform;

import java.util.*;

/**
 * Controlador: lógica de autenticación, registro y apertura de ventanas.
 * Actualmente contiene un repositorio simple en memoria de usuarios de ejemplo.
 */
public class Controlador {

    private final Map<String, Persona> usuariosPorNombre; // clave: nombreUsuario

    public Controlador() {
        this.usuariosPorNombre = new LinkedHashMap<>();
        inicializarDatosEjemplo();
    }

    private void inicializarDatosEjemplo() {
        Persona p1 = new Persona("p1", "Patricia Conesa", "patri", "pass1");
        Persona p2 = new Persona("p2", "Álvaro Sancho", "alvaro", "pass2");
        Persona p3 = new Persona("p3", "Pablo Asensio", "pablo", "pass3");

        usuariosPorNombre.put(p1.getNombreUsuario(), p1);
        usuariosPorNombre.put(p2.getNombreUsuario(), p2);
        usuariosPorNombre.put(p3.getNombreUsuario(), p3);
    }

    /**
     * Intenta autenticar un usuario por nombreUsuario y contraseña.
     * Devuelve Optional<Persona> si OK, vacío si falla.
     */
    public Optional<Persona> autenticar(String nombreUsuario, String contraseña) {
        if (nombreUsuario == null || contraseña == null) return Optional.empty();
        Persona p = usuariosPorNombre.get(nombreUsuario);
        if (p == null) return Optional.empty();
        if (contraseña.equals(p.getContraseña())) {
            return Optional.of(p);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Registra un nuevo usuario. Lanza IllegalArgumentException si los datos son inválidos
     * o si el nombre de usuario ya existe.
     *
     * @return Persona creada
     */
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
        Persona nueva = new Persona(id, nombreCompleto.trim(), nombreUsuario.trim(), contraseña);
        usuariosPorNombre.put(nueva.getNombreUsuario(), nueva);
        return nueva;
    }

    /**
     * Abre una ventana provisional que muestra la lista de usuarios y los datos del usuario autenticado.
     */
    public void abrirVentanaPrincipalPersona(Persona autenticado) {
        List<Persona> lista = Collections.unmodifiableList(new ArrayList<>(usuariosPorNombre.values()));
        javafx.application.Platform.runLater(() -> VentanaPrincipalPersona.mostrar(lista, autenticado));
    }

    /**
     * Devuelve lista inmutable de usuarios (para mostrar en UI).
     */
    public List<Persona> getUsuarios() {
        return Collections.unmodifiableList(new ArrayList<>(usuariosPorNombre.values()));
    }
}

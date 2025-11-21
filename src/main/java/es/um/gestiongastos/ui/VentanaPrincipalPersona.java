package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Persona;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Ventana Principal de la aplicación.
 * Actúa como CONTENEDOR y COORDINADOR.
 * Delega la lógica visual específica a PanelGestionGastos y PanelInformes.
 */
public class VentanaPrincipalPersona {

    // Acceso al Singleton
    private static final Controlador controlador = Controlador.getInstancia();

    /**
     * Muestra la ventana principal.
     */
    public static void mostrar(List<Persona> usuarios, Persona autenticado) {
        Stage stage = new Stage();
        stage.setTitle("Gestión de Gastos - Usuario: " + autenticado.getNombreCompleto());

        BorderPane root = new BorderPane();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- PESTAÑA 1: GESTIÓN DE GASTOS ---
        // Delegamos toda la complejidad a la clase PanelGestionGastos
        Tab tabGastos = new Tab("Gestión de Gastos");
        tabGastos.setContent(PanelGestionGastos.crearVista());

        // --- PESTAÑA 2: INFORMES Y GRÁFICOS ---
        // Delegamos toda la complejidad a la clase PanelInformes
        Tab tabInformes = new Tab("Informes y Gráficos");
        tabInformes.setContent(PanelInformes.crearVista());

        // --- PESTAÑA 3: CUENTAS Y ALERTAS ---
        // Esta aún la mantenemos aquí hasta que la implementemos
        Tab tabCuentasAlertas = new Tab("Cuentas y Alertas");
        tabCuentasAlertas.setContent(crearPanelCuentasAlertas(autenticado));

        tabPane.getTabs().addAll(tabGastos, tabInformes, tabCuentasAlertas);

        root.setCenter(tabPane);

        // Barra de estado inferior
        Label status = new Label("Sesión iniciada. Su ID: " + autenticado.getId());
        status.setPadding(new Insets(4, 8, 4, 8));
        root.setBottom(status);

        Scene scene = new Scene(root, 950, 700);
        stage.setScene(scene);

        // 🔴 REGISTRO DEL CALLBACK CENTRALIZADO
        // Cuando el modelo cambie, esta ventana ordena a los paneles hijos que se refresquen
        controlador.setOnModeloCambiado(VentanaPrincipalPersona::refrescarTodo);
        
        // Carga inicial de datos en todos los paneles
        refrescarTodo();
        
        // Cierre total de la aplicación al pulsar la X
        stage.setOnCloseRequest(event -> {
            System.out.println("\nSaliendo del sistema...");
            javafx.application.Platform.exit();
            System.exit(0);
        });
        
        stage.show();
    }

    /**
     * Método coordinador que ordena refrescar a todos los sub-paneles.
     */
    private static void refrescarTodo() {
        // 1. Actualizar tabla y filtros
        PanelGestionGastos.refrescarDatos();
        
        // 2. Actualizar gráficos
        PanelInformes.refrescarDatos();
        
        // (Aquí añadiremos el refresco de alertas en el futuro)
    }

    // =========================================================
    // PESTAÑA CUENTAS Y ALERTAS (Pendiente de crear clase)
    // =========================================================

    private static VBox crearPanelCuentasAlertas(Persona autenticado) {
        Label titulo = new Label("Cuentas Compartidas y Alertas");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label nota = new Label("¡Aquí irá la lógica de las cuentas compartidas y el Patrón Estrategia para las Alertas!");
        
        VBox panel = new VBox(15, titulo, nota);
        panel.setPadding(new Insets(15));
        return panel;
    }
}

package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Persona;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class VentanaPrincipalPersona {

    private static final Controlador controlador = Controlador.getInstancia();

    public static void mostrar(List<Persona> usuarios, Persona autenticado) {
        Stage stage = new Stage();
        stage.setTitle("Gestión de Gastos - " + autenticado.getNombreCompleto());

        BorderPane root = new BorderPane();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- PESTAÑA 1: GESTIÓN ---
        Tab tabGastos = new Tab("Gestión de Gastos");
        tabGastos.setContent(PanelGestionGastos.crearVista());

        // --- PESTAÑA 2: INFORMES ---
        Tab tabInformes = new Tab("Informes y Gráficos");
        tabInformes.setContent(PanelInformes.crearVista());

        // --- PESTAÑA 3: CUENTAS Y ALERTAS ---
        Tab tabCuentasAlertas = new Tab("Cuentas y Alertas");
        
        SplitPane split = new SplitPane();
        
        // Izquierda: Panel de Cuentas
        VBox panelCuentas = PanelCuentas.crearVista();
        
        // Derecha: Panel de Alertas
        VBox panelAlertas = PanelAlertas.crearVista();
        
        split.getItems().addAll(panelCuentas, panelAlertas);
        split.setDividerPositions(0.4); 
        
        tabCuentasAlertas.setContent(split);

        tabPane.getTabs().addAll(tabGastos, tabInformes, tabCuentasAlertas);
        root.setCenter(tabPane);

        Label status = new Label("Usuario: " + autenticado.getNombreUsuario());
        status.setPadding(new Insets(5));
        root.setBottom(status);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);

        // Callback global de refresco
        controlador.setOnModeloCambiado(VentanaPrincipalPersona::refrescarTodo);
        
        // Carga inicial
        refrescarTodo();
        
        stage.setOnCloseRequest(event -> {
            System.out.println("\nCerrando aplicación... ¡Hasta pronto, " + autenticado.getNombreUsuario() + "!");
            javafx.application.Platform.exit();
            System.exit(0);
        });
        
        stage.show();
    }

    private static void refrescarTodo() {
        PanelGestionGastos.refrescarDatos();
        PanelInformes.refrescarDatos();
        PanelCuentas.refrescarDatos();
        PanelAlertas.refrescarDatos();
    }
}
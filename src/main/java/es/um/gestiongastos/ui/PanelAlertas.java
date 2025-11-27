package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Alerta;
import es.um.gestiongastos.model.Notificacion;
import es.um.gestiongastos.model.Periodicidad;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;

public class PanelAlertas {

    private static final Controlador controlador = Controlador.getInstancia();
    
    private static ListView<Alerta> listaAlertas;
    private static ListView<Notificacion> listaNotificaciones;

    public static VBox crearVista() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        // TÍTULO
        Label lblTitulo = new Label("Configuración de Alertas y Notificaciones");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 18));

        // --- ZONA SUPERIOR: CREAR ALERTA ---
        TitledPane paneCrear = new TitledPane("Nueva Alerta", crearFormularioAlerta());
        paneCrear.setCollapsible(false);

        // --- ZONA CENTRAL: DOS COLUMNAS ---
        HBox split = new HBox(20);
        VBox.setVgrow(split, Priority.ALWAYS);

        // COLUMNA IZQUIERDA: ALERTAS ACTIVAS
        VBox boxAlertas = new VBox(5);
        Label lblAlertas = new Label("Mis Alertas Activas");
        lblAlertas.setStyle("-fx-font-weight: bold;");
        
        listaAlertas = new ListView<>();
        listaAlertas.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Alerta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    // Botón borrar pequeñito
                    Button btnDel = new Button("🗑");
                    btnDel.setStyle("-fx-text-fill: red; -fx-background-color: transparent; -fx-border-color: #ddd;");
                    btnDel.setOnAction(e -> controlador.borrarAlerta(item));
                    
                    HBox cell = new HBox(10, new Label(item.toString()), new Region(), btnDel);
                    HBox.setHgrow(cell.getChildren().get(1), Priority.ALWAYS);
                    setGraphic(cell);
                }
            }
        });
        VBox.setVgrow(listaAlertas, Priority.ALWAYS);
        boxAlertas.getChildren().addAll(lblAlertas, listaAlertas);
        HBox.setHgrow(boxAlertas, Priority.ALWAYS);

        // COLUMNA DERECHA: HISTORIAL NOTIFICACIONES
        VBox boxNotif = new VBox(5);
        Label lblNotif = new Label("Historial de Notificaciones");
        lblNotif.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;");
        
        listaNotificaciones = new ListView<>();
        listaNotificaciones.setCellFactory(param -> new ListCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
            @Override
            protected void updateItem(Notificacion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("[" + item.getFechaHora().format(fmt) + "] " + item.getMensaje());
                    setStyle("-fx-text-fill: #c0392b;"); // Color rojizo para alertas
                    setWrapText(true);
                }
            }
        });
        VBox.setVgrow(listaNotificaciones, Priority.ALWAYS);
        boxNotif.getChildren().addAll(lblNotif, listaNotificaciones);
        HBox.setHgrow(boxNotif, Priority.ALWAYS);

        split.getChildren().addAll(boxAlertas, boxNotif);

        root.getChildren().addAll(lblTitulo, paneCrear, split);
        return root;
    }

    private static GridPane crearFormularioAlerta() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));

        TextField tfNombre = new TextField(); tfNombre.setPromptText("Ej: Límite Comida Semanal");
        
        ComboBox<Periodicidad> cbPeriodo = new ComboBox<>();
        cbPeriodo.setItems(FXCollections.observableArrayList(Periodicidad.values()));
        cbPeriodo.setValue(Periodicidad.MENSUAL);

        ComboBox<String> cbCategoria = new ComboBox<>();
        cbCategoria.getItems().add("Todas");
        cbCategoria.getItems().addAll(controlador.getNombresCategorias());
        cbCategoria.setValue("Todas");

        TextField tfUmbral = new TextField(); tfUmbral.setPromptText("Ej: 150.00");

        Button btnGuardar = new Button("Crear Alerta");
        btnGuardar.setOnAction(e -> {
            try {
                String nombre = tfNombre.getText();
                double umbral = Double.parseDouble(tfUmbral.getText());
                String cat = cbCategoria.getValue();
                Periodicidad per = cbPeriodo.getValue();

                if (nombre.isEmpty() || umbral <= 0) throw new IllegalArgumentException("Datos inválidos");

                controlador.crearAlerta(nombre, per, cat, umbral);
                
                tfNombre.clear(); tfUmbral.clear();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                a.show();
            }
        });

        grid.add(new Label("Nombre:"), 0, 0); grid.add(tfNombre, 1, 0);
        grid.add(new Label("Periodo:"), 2, 0); grid.add(cbPeriodo, 3, 0);
        
        grid.add(new Label("Límite (€):"), 0, 1); grid.add(tfUmbral, 1, 1);
        grid.add(new Label("Categoría:"), 2, 1); grid.add(cbCategoria, 3, 1);
        
        grid.add(btnGuardar, 4, 1);

        return grid;
    }

    public static void refrescarDatos() {
        if (controlador.getUsuarioAutenticado() != null) {
            if (listaAlertas != null)
                listaAlertas.setItems(FXCollections.observableArrayList(controlador.getUsuarioAutenticado().getAlertas()));
            
            if (listaNotificaciones != null)
                listaNotificaciones.setItems(FXCollections.observableArrayList(controlador.getUsuarioAutenticado().getNotificaciones()));
        }
    }
}
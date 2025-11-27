package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.GastosCompartidos;
import es.um.gestiongastos.model.Persona;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.stage.FileChooser; // Necesario
import java.io.File;

public class PanelCuentas {

    private static final Controlador controlador = Controlador.getInstancia();
    private static ListView<GastosCompartidos> listaCuentas;

    public static VBox crearVista() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        Label lblTitulo = new Label("Mis Cuentas");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        listaCuentas = new ListView<>();
        listaCuentas.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(GastosCompartidos item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // INFO DE TEXTO
                    Persona yo = controlador.getUsuarioAutenticado();
                    String info = String.format("%s (%d personas)", item.getNombre(), item.getParticipantes().size());
                    String saldoStr = String.format("Mi Saldo: %.2f €", item.getSaldo(yo));
                    
                    VBox vText = new VBox(2, new Label(info), new Label(saldoStr));
                    if (item.getSaldo(yo).doubleValue() < 0) vText.getChildren().get(1).setStyle("-fx-text-fill: red;");
                    else vText.getChildren().get(1).setStyle("-fx-text-fill: green;");
                    
                    // ESPACIADOR
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    // BOTÓN INFO
                    Button btnInfo = new Button("ℹ");
                    btnInfo.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
                    btnInfo.setTooltip(new Tooltip("Consultar información detallada"));
                    btnInfo.setOnAction(e -> DialogoDetalleCuenta.mostrar(item));

                    HBox rootCell = new HBox(10, vText, spacer, btnInfo);
                    rootCell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(rootCell);
                }
            }
        });
        VBox.setVgrow(listaCuentas, Priority.ALWAYS);

        // BOTONERA
        Button btnNueva = new Button("Nueva Cuenta");
        btnNueva.setMaxWidth(Double.MAX_VALUE);
        btnNueva.setOnAction(e -> mostrarPaso1SeleccionUsuarios()); // Tu método anterior

        // BOTÓN IMPORTAR 
        Button btnImportar = new Button("Importar Archivo");
        btnImportar.setMaxWidth(Double.MAX_VALUE);
        btnImportar.setStyle("-fx-base: #f39c12;"); 
        btnImportar.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar archivo de Cuenta");
            
            // Filtros para todos los formatos soportados
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Todos los soportados", "*.json", "*.yaml", "*.yml", "*.csv"),
                new FileChooser.ExtensionFilter("JSON", "*.json"),
                new FileChooser.ExtensionFilter("YAML", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter("CSV", "*.csv")
            );
            
            File archivo = fileChooser.showOpenDialog(btnImportar.getScene().getWindow());
            
            if (archivo != null) {
                try {
                    // Llamada al método genérico
                    controlador.importarCuenta(archivo);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cuenta importada correctamente.");
                    alert.show();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error al importar: " + ex.getMessage());
                    alert.show();
                }
            }
        });

        HBox botonera = new HBox(10, btnNueva, btnImportar);
        HBox.setHgrow(btnNueva, Priority.ALWAYS);
        HBox.setHgrow(btnImportar, Priority.ALWAYS);

        root.getChildren().addAll(lblTitulo, listaCuentas, botonera);
        return root;
    }

    public static void refrescarDatos() {
        if (listaCuentas != null && controlador.getUsuarioAutenticado() != null) {
            listaCuentas.setItems(FXCollections.observableArrayList(
                controlador.getUsuarioAutenticado().getCuentas()
            ));
        }
    }

    private static void mostrarPaso1SeleccionUsuarios() {
        Stage stage1 = new Stage();
        stage1.initModality(Modality.APPLICATION_MODAL);
        stage1.setTitle("Nueva Cuenta - Paso 1/2");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre del grupo (ej: Viaje Madrid)");
        
        Label lblPart = new Label("Selecciona a los otros participantes:");
        
        TableView<FilaCandidato> tabla = new TableView<>();
        tabla.setEditable(true);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<FilaCandidato, Boolean> colSel = new TableColumn<>("Incluir");
        colSel.setCellValueFactory(p -> p.getValue().seleccionado);
        colSel.setCellFactory(CheckBoxTableCell.forTableColumn(colSel));
        colSel.setMaxWidth(60);
        
        TableColumn<FilaCandidato, String> colNombre = new TableColumn<>("Usuario");
        colNombre.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().persona.getNombreCompleto()));

        tabla.getColumns().addAll(colSel, colNombre);
        
        Persona yo = controlador.getUsuarioAutenticado();
        List<FilaCandidato> candidatos = controlador.getTodosLosUsuarios().stream()
                .filter(p -> !p.equals(yo))
                .map(FilaCandidato::new)
                .collect(Collectors.toList());
        tabla.setItems(FXCollections.observableArrayList(candidatos));

        Button btnSiguiente = new Button("Siguiente ->");
        btnSiguiente.setOnAction(e -> {
            String nombre = tfNombre.getText().trim();
            if (nombre.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Escribe un nombre para la cuenta.");
                a.show();
                return;
            }
            
            List<Persona> seleccionados = tabla.getItems().stream()
                    .filter(f -> f.seleccionado.get())
                    .map(f -> f.persona)
                    .collect(Collectors.toList());
            
            if (seleccionados.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Selecciona al menos otro usuario.");
                a.show();
                return;
            }

            // Añadirme a mí mismo a la lista final
            seleccionados.add(0, yo);
            
            // Cerrar paso 1 y abrir paso 2
            stage1.close();
            mostrarPaso2Porcentajes(nombre, seleccionados);
        });

        layout.getChildren().addAll(new Label("Nombre de la Cuenta:"), tfNombre, lblPart, tabla, btnSiguiente);
        stage1.setScene(new Scene(layout, 400, 450));
        stage1.show();
    }

    private static void mostrarPaso2Porcentajes(String nombreCuenta, List<Persona> participantes) {
        Stage stage2 = new Stage();
        stage2.initModality(Modality.APPLICATION_MODAL);
        stage2.setTitle("Nueva Cuenta - Paso 2/2: Definir Reparto");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        
        Label lblInfo = new Label("Define el porcentaje de gasto para cada miembro.\n(La suma debe ser 100%)");
        
        TableView<FilaPorcentaje> tabla = new TableView<>();
        tabla.setEditable(true);

        TableColumn<FilaPorcentaje, String> colUser = new TableColumn<>("Participante");
        colUser.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().persona.getNombreCompleto()));
        
        TableColumn<FilaPorcentaje, String> colPorc = new TableColumn<>("Porcentaje %");
        colPorc.setCellValueFactory(p -> p.getValue().porcentajeTexto);
        colPorc.setCellFactory(TextFieldTableCell.forTableColumn());
        colPorc.setOnEditCommit(e -> e.getRowValue().porcentajeTexto.set(e.getNewValue()));

        tabla.getColumns().addAll(colUser, colPorc);
        
        // Inicializar datos (por defecto equitativo)
        double equitativo = 100.0 / participantes.size();
        List<FilaPorcentaje> filas = participantes.stream()
                .map(p -> new FilaPorcentaje(p, String.format("%.2f", equitativo).replace(",", ".")))
                .collect(Collectors.toList());
        
        tabla.setItems(FXCollections.observableArrayList(filas));

        // Botón Equitativo
        Button btnEquitativo = new Button("Restablecer Equitativo");
        btnEquitativo.setOnAction(e -> {
            for (FilaPorcentaje f : filas) {
                f.porcentajeTexto.set(String.format("%.2f", equitativo).replace(",", "."));
            }
            tabla.refresh();
        });
        
        Label lblTotal = new Label("Suma actual: Calculando...");

        Button btnCrear = new Button("Finalizar y Crear");
        btnCrear.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");
        btnCrear.setOnAction(e -> {
            try {
                Map<Persona, Double> mapa = new HashMap<>();
                double suma = 0.0;
                
                for (FilaPorcentaje f : filas) {
                    double val = Double.parseDouble(f.porcentajeTexto.get());
                    if (val < 0) throw new IllegalArgumentException("No puede haber porcentajes negativos.");
                    mapa.put(f.persona, val);
                    suma += val;
                }
                
                // Validación estricta antes de enviar al modelo
                if (Math.abs(suma - 100.0) > 0.1) {
                    throw new IllegalArgumentException("La suma de porcentajes es " + String.format("%.2f", suma) + "%. Debe ser 100%.");
                }

                controlador.crearCuentaCompartida(nombreCuenta, participantes, mapa);
                stage2.close();
                
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error en formato numérico. Usa el punto como decimal.");
                a.show();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                a.show();
            }
        });

        HBox botones = new HBox(10, btnEquitativo, btnCrear);
        botones.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(lblInfo, tabla, botones);
        stage2.setScene(new Scene(layout, 450, 400));
        stage2.show();
    }

    // Clases auxiliares para las tablas
    public static class FilaCandidato {
        Persona persona;
        BooleanProperty seleccionado = new SimpleBooleanProperty(false);
        public FilaCandidato(Persona p) { this.persona = p; }
        public BooleanProperty seleccionadoProperty() { return seleccionado; }
    }
    
    public static class FilaPorcentaje {
        Persona persona;
        SimpleStringProperty porcentajeTexto;
        public FilaPorcentaje(Persona p, String inicial) {
            this.persona = p;
            this.porcentajeTexto = new SimpleStringProperty(inicial);
        }
    }
}
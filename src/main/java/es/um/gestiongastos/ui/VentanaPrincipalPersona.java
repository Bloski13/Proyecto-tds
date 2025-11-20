package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Categoria;
import es.um.gestiongastos.model.Gasto;
import es.um.gestiongastos.model.Persona;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ventana Principal de la aplicación de Gestión de Gastos.
 * Contiene la estructura de pestañas para gestionar Gastos, Cuentas y Alertas.
 */
public class VentanaPrincipalPersona {

    // Acceso al Singleton
    private static final Controlador controlador = Controlador.getInstancia();
    // LISTA OBSERVABLE: Enlaza los datos del Controlador con la TableView de la GUI
    private static ObservableList<Gasto> datosGastos = FXCollections.observableArrayList();
    
    /**
     * Muestra la ventana principal de gestión de gastos del usuario autenticado.
     */
    public static void mostrar(List<Persona> usuarios, Persona autenticado) {
        Stage stage = new Stage();
        stage.setTitle("Gestión de Gastos - Usuario: " + autenticado.getNombreCompleto());

        BorderPane root = new BorderPane();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // PESTAÑA 1: GESTIÓN DE GASTOS
        Tab tabGastos = new Tab("Gestión de Gastos");
        tabGastos.setContent(crearPanelGestionGastos(autenticado));

        // PESTAÑA 2: INFORMES Y GRÁFICOS
        Tab tabInformes = new Tab("Informes y Gráficos");
        tabInformes.setContent(crearPanelInformes());

        // PESTAÑA 3: CUENTAS Y ALERTAS
        Tab tabCuentasAlertas = new Tab("Cuentas y Alertas");
        tabCuentasAlertas.setContent(crearPanelCuentasAlertas(autenticado));

        tabPane.getTabs().addAll(tabGastos, tabInformes, tabCuentasAlertas);

        root.setCenter(tabPane);

        Label status = new Label("Sesión iniciada. Su ID: " + autenticado.getId());
        status.setPadding(new Insets(4, 8, 4, 8));
        root.setBottom(status);

        Scene scene = new Scene(root, 900, 700);
        stage.setScene(scene);
        // Así, cuando la consola cambie algo, el controlador ejecutará recargarTablaGastos
        controlador.setOnModeloCambiado(VentanaPrincipalPersona::recargarTablaGastos);
        // Carga los gastos existentes
        recargarTablaGastos();
        
        stage.show();
    }

    /**
     * Método para recargar la tabla de gastos del usuario actual.
     * Se llama al inicio y después de cada registro/borrado.
     */
    private static void recargarTablaGastos() {
        // 1. Limpiamos la lista actual
        datosGastos.clear();
        
        // 2. Obtenemos los gastos del Controlador
        List<Gasto> listaGastos = controlador.getGastosUsuarioActual();
        
        // 3. Añadimos los nuevos gastos a la ObservableList
        datosGastos.addAll(listaGastos);
        
        //System.out.println(">> [GUI] Tabla recargada. Total de gastos: " + listaGastos.size());
    }
    
   
    
    // =========================================================
    // PESTAÑA DE GESTIÓN DE GASTOS
    // =========================================================

    private static VBox crearPanelGestionGastos(Persona autenticado) {
        VBox panelPrincipal = new VBox(15);
        panelPrincipal.setPadding(new Insets(15));
        
        GridPane formulario = crearFormularioRegistro();
        TableView<Gasto> tablaGastos = crearTablaGastos();
        
        panelPrincipal.getChildren().addAll(formulario, new Separator(), tablaGastos);
        
        return panelPrincipal;
    }

    private static GridPane crearFormularioRegistro() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 10;");
        
        Label lblTitulo = new Label("REGISTRAR NUEVO GASTO");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(lblTitulo, 0, 0, 4, 1);
        GridPane.setHalignment(lblTitulo, HPos.LEFT);
        
        // Campos del formulario
        Label lblDescripcion = new Label("Descripción:");
        TextField tfDescripcion = new TextField();
        tfDescripcion.setPromptText("Ej: Café, Gasolina, Cena con amigos");
        grid.add(lblDescripcion, 0, 1);
        grid.add(tfDescripcion, 1, 1);

        Label lblImporte = new Label("Importe (€):");
        TextField tfImporte = new TextField();
        tfImporte.setPromptText("Ej: 15.50");
        grid.add(lblImporte, 2, 1);
        grid.add(tfImporte, 3, 1);

        Label lblFecha = new Label("Fecha:");
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        grid.add(lblFecha, 0, 2);
        grid.add(dpFecha, 1, 2);

        Label lblCategoria = new Label("Categoría:");
        ObservableList<String> opcionesCategoria = FXCollections.observableArrayList(
                "Alimentación", "Transporte", "Entretenimiento", "Otras"
        );
        ComboBox<String> cbCategoria = new ComboBox<>(opcionesCategoria);
        cbCategoria.setValue("Alimentación");
        grid.add(lblCategoria, 2, 2);
        grid.add(cbCategoria, 3, 2);
        
        Button btnRegistrar = new Button("Registrar Gasto");
        Label lblStatus = new Label("");
        lblStatus.setStyle("-fx-text-fill: green;");
        
        HBox botones = new HBox(10, btnRegistrar, lblStatus);
        grid.add(botones, 3, 3);
        GridPane.setHalignment(botones, HPos.RIGHT);

        // LÓGICA DEL BOTÓN REGISTRAR
        btnRegistrar.setOnAction(e -> {
            try {
                String descripcion = tfDescripcion.getText();
                double importe = Double.parseDouble(tfImporte.getText());
                LocalDate fecha = dpFecha.getValue();
                String categoria = cbCategoria.getValue();
                
                if (descripcion.trim().isEmpty() || importe <= 0 || fecha == null) {
                     throw new IllegalArgumentException("Datos incompletos o incorrectos.");
                }

                // LLAMADA AL CONTROLADOR (Guarda el gasto en la Persona)
                controlador.registrarGasto(descripcion, importe, fecha, categoria);
                
                lblStatus.setText("Gasto registrado con éxito.");
                lblStatus.setStyle("-fx-text-fill: green;");
                
                // Limpiar formulario
                tfDescripcion.clear();
                tfImporte.clear();
                dpFecha.setValue(LocalDate.now());
                
                // LLAMADA DE RECARGA TRAS REGISTRO: Refrescar la tabla
                recargarTablaGastos(); 

            } catch (NumberFormatException ex) {
                lblStatus.setText("Error: El importe debe ser un número válido (ej: 15.50).");
                lblStatus.setStyle("-fx-text-fill: red;");
            } catch (IllegalArgumentException ex) {
                lblStatus.setText("Error: " + ex.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
            }
        });

        return grid;
    }

    @SuppressWarnings("unchecked")
	private static TableView<Gasto> crearTablaGastos() {
        TableView<Gasto> table = new TableView<>();
        table.setPlaceholder(new Label("No hay gastos registrados."));
        
        // Enlazar la tabla a la ObservableList que controlamos
        table.setItems(datosGastos); 
        
        // Columna de la Descripción
        TableColumn<Gasto, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(250);

        // Columna del Importe
        TableColumn<Gasto, BigDecimal> colImporte = new TableColumn<>("Importe (€)");
        colImporte.setCellValueFactory(new PropertyValueFactory<>("importe"));
        colImporte.setPrefWidth(100);
        
        // Columna de la Fecha
        TableColumn<Gasto, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(120);
        
        // Columna de la Categoría
        TableColumn<Gasto, Categoria> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setPrefWidth(150);

        // Columna de Acciones (Editar/Borrar)
        TableColumn<Gasto, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellFactory(param -> new TableCell<Gasto, Void>() {
        private final Button btnBorrar = new Button("Borrar");
        {   
         // Borrado de gasto
        btnBorrar.setOnAction(e -> {
                Gasto gastoABorrar = getTableView().getItems().get(getIndex());
                controlador.borrarGasto(gastoABorrar.getId());
                recargarTablaGastos(); // Recargar después de borrar
            });
        }

		@Override
		public void updateItem(Void item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
			} else {
				setGraphic(btnBorrar);
			}
		}
        });
        
        colAcciones.setPrefWidth(80);

        table.getColumns().addAll(colDescripcion, colImporte, colFecha, colCategoria, colAcciones);

        return table;
    }
    
    // =========================================================
    // OTRAS PESTAÑAS (Provisionales)
    // =========================================================

    private static VBox crearPanelInformes() {
        Label titulo = new Label("Visualización de Gastos");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label nota = new Label("¡Aquí irán los diagramas de barras/circulares y el calendario!");
        return new VBox(15, titulo, nota);
    }

    private static VBox crearPanelCuentasAlertas(Persona autenticado) {
        Label titulo = new Label("Cuentas Compartidas y Alertas");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label nota = new Label("¡Aquí irá la lógica de las cuentas compartidas y el Patrón Estrategia para las Alertas!");
        return new VBox(15, titulo, nota);
    }
}

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
    
    // Elementos de la UI que necesitan actualizarse externamente (STATIC para acceso global en la clase)
    private static ObservableList<Gasto> datosGastos = FXCollections.observableArrayList();
    private static ComboBox<String> cbCategoriaRegistro; // Ahora es propiedad de la clase para poder refrescarlo

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

        Scene scene = new Scene(root, 950, 700);
        stage.setScene(scene);

        // registro del callback...
        controlador.setOnModeloCambiado(VentanaPrincipalPersona::refrescarInterfazGlobal);
        refrescarInterfazGlobal();
        
        // Cierre total al pulsar la "X" de la ventana
        stage.setOnCloseRequest(event -> {
            System.out.println("\nSaliendo del sistema...");
            // Platform.exit() cierra JavaFX, System.exit(0) mata el hilo de la consola también
            javafx.application.Platform.exit();
            System.exit(0);
        });
        
        stage.show();
    }

    /**
     * Método maestro que actualiza toda la interfaz gráfica
     * cuando hay cambios en los datos (Tabla de gastos y ComboBoxes).
     */
    private static void refrescarInterfazGlobal() {
        // 1. Refrescar Tabla
        datosGastos.clear();
        datosGastos.addAll(controlador.getGastosUsuarioActual());
        
        // 2. Refrescar ComboBox de Registro (si ya fue creado)
        if (cbCategoriaRegistro != null) {
            String seleccionPrevia = cbCategoriaRegistro.getValue();
            cargarCategoriasEnCombo(cbCategoriaRegistro);
            // Intentar mantener la selección si aún es válida
            if (seleccionPrevia != null && cbCategoriaRegistro.getItems().contains(seleccionPrevia)) {
                cbCategoriaRegistro.setValue(seleccionPrevia);
            }
        }
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
        
        // --- CAMPOS ESTÁNDAR ---
        Label lblDescripcion = new Label("Descripción:");
        TextField tfDescripcion = new TextField();
        tfDescripcion.setPromptText("Ej: Café, Gasolina...");
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

        // --- LÓGICA DE CATEGORÍA DINÁMICA ---
        Label lblCategoria = new Label("Categoría:");
        
        // Inicializamos la variable estática
        cbCategoriaRegistro = new ComboBox<>();
        
        TextField tfNuevaCategoria = new TextField();
        tfNuevaCategoria.setPromptText("Nombre nueva categoría...");
        tfNuevaCategoria.setVisible(false); 
        tfNuevaCategoria.setManaged(false); 

        // Configurar lógica y cargar datos iniciales
        configurarLogicaComboCategoria(cbCategoriaRegistro, tfNuevaCategoria, null); 
        cargarCategoriasEnCombo(cbCategoriaRegistro);

        VBox boxCategoria = new VBox(5, cbCategoriaRegistro, tfNuevaCategoria);
        grid.add(lblCategoria, 2, 2);
        grid.add(boxCategoria, 3, 2);
        
        // --- BOTONES Y ESTADO ---
        Button btnRegistrar = new Button("Registrar Gasto");
        Label lblStatus = new Label("");
        
        HBox botones = new HBox(10, btnRegistrar, lblStatus);
        grid.add(botones, 3, 3);
        GridPane.setHalignment(botones, HPos.RIGHT);

        // --- ACCIÓN REGISTRAR ---
        btnRegistrar.setOnAction(e -> {
            try {
                String descripcion = tfDescripcion.getText();
                double importe = Double.parseDouble(tfImporte.getText());
                LocalDate fecha = dpFecha.getValue();
                
                // Obtener categoría usando método auxiliar
                String categoriaFinal = obtenerCategoriaSeleccionada(cbCategoriaRegistro, tfNuevaCategoria);
                
                if (descripcion.trim().isEmpty() || importe <= 0 || fecha == null) {
                     throw new IllegalArgumentException("Datos incompletos.");
                }

                controlador.registrarGasto(descripcion, importe, fecha, categoriaFinal);
                
                lblStatus.setText("Gasto guardado.");
                lblStatus.setStyle("-fx-text-fill: green;");
                
                // LIMPIEZA
                tfDescripcion.clear();
                tfImporte.clear();
                dpFecha.setValue(LocalDate.now());
                tfNuevaCategoria.clear();
                tfNuevaCategoria.setVisible(false);
                tfNuevaCategoria.setManaged(false);
                cbCategoriaRegistro.getSelectionModel().selectFirst();
                
                // NOTA: No hace falta llamar a refrescarInterfazGlobal() aquí manualmente
                // porque el controlador dispara el callback setOnModeloCambiado.

            } catch (NumberFormatException ex) {
                lblStatus.setText("Error: Importe inválido.");
                lblStatus.setStyle("-fx-text-fill: red;");
            } catch (IllegalArgumentException ex) {
                lblStatus.setText(ex.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
            }
        });

        return grid;
    }

    private static TableView<Gasto> crearTablaGastos() {
        TableView<Gasto> table = new TableView<>();
        table.setPlaceholder(new Label("No hay gastos registrados."));
        table.setItems(datosGastos); 
        
        // Columnas
        TableColumn<Gasto, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(250);

        TableColumn<Gasto, BigDecimal> colImporte = new TableColumn<>("Importe (€)");
        colImporte.setCellValueFactory(new PropertyValueFactory<>("importe"));
        colImporte.setPrefWidth(100);
        
        TableColumn<Gasto, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(120);
        
        TableColumn<Gasto, Categoria> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setPrefWidth(150);

        TableColumn<Gasto, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellFactory(param -> new TableCell<Gasto, Void>() {
            private final Button btnBorrar = new Button("Borrar");
            private final Button btnEditar = new Button("Editar");
            private final HBox pane = new HBox(5, btnEditar, btnBorrar);

            {
                btnBorrar.setOnAction(e -> {
                    Gasto gasto = (Gasto) getTableRow().getItem();
                    if (gasto != null) controlador.borrarGasto(gasto.getId());
                });

                btnEditar.setOnAction(e -> {
                    Gasto gasto = (Gasto) getTableRow().getItem();
                    if (gasto != null) mostrarDialogoEdicion(gasto);
                });
                
                btnBorrar.setStyle("-fx-background-color: #ffcccc; -fx-text-fill: darkred;");
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        colAcciones.setPrefWidth(140);
        table.getColumns().addAll(colDescripcion, colImporte, colFecha, colCategoria, colAcciones);

        return table;
    }

    // =========================================================
    // LÓGICA DE EDICIÓN (Con Categoría Dinámica)
    // =========================================================

    private static void mostrarDialogoEdicion(Gasto gasto) {
        Stage dialog = new Stage();
        dialog.setTitle("Editar Gasto");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField tfDesc = new TextField(gasto.getDescripcion());
        TextField tfImp = new TextField(gasto.getImporte().toString());
        DatePicker dpFec = new DatePicker(gasto.getFecha());

        // LÓGICA CATEGORIA EDICIÓN (IGUAL QUE REGISTRO)
        ComboBox<String> cbCatEdit = new ComboBox<>();
        TextField tfNuevaCatEdit = new TextField();
        tfNuevaCatEdit.setPromptText("Nombre nueva categoría...");
        tfNuevaCatEdit.setVisible(false); tfNuevaCatEdit.setManaged(false);
        
        // Pasamos 'dialog' como tercer parámetro
        configurarLogicaComboCategoria(cbCatEdit, tfNuevaCatEdit, dialog);
        
        cargarCategoriasEnCombo(cbCatEdit);
        
        // Seleccionar la categoría actual del gasto
        String catActual = gasto.getCategoria().getNombre();
        if (cbCatEdit.getItems().contains(catActual)) {
            cbCatEdit.setValue(catActual);
        } else {
             // Si por alguna razón no está en la lista, seleccionamos el primero
             cbCatEdit.getSelectionModel().selectFirst();
        }

        VBox boxCat = new VBox(5, cbCatEdit, tfNuevaCatEdit);

        grid.add(new Label("Descripción:"), 0, 0); grid.add(tfDesc, 1, 0);
        grid.add(new Label("Importe (€):"), 0, 1); grid.add(tfImp, 1, 1);
        grid.add(new Label("Fecha:"), 0, 2);       grid.add(dpFec, 1, 2);
        grid.add(new Label("Categoría:"), 0, 3);   grid.add(boxCat, 1, 3);

        Button btnGuardar = new Button("Guardar Cambios");
        Button btnCancelar = new Button("Cancelar");
        HBox botones = new HBox(10, btnGuardar, btnCancelar);
        grid.add(botones, 1, 4);

        btnGuardar.setOnAction(e -> {
            try {
                String desc = tfDesc.getText();
                Double imp = Double.parseDouble(tfImp.getText());
                LocalDate fec = dpFec.getValue();
                
                String catFinal = obtenerCategoriaSeleccionada(cbCatEdit, tfNuevaCatEdit);

                controlador.modificarGasto(gasto.getId(), desc, imp, fec, catFinal);
                dialog.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        btnCancelar.setOnAction(e -> dialog.close());
        dialog.setScene(new Scene(grid));
        dialog.showAndWait();
    }

    // =========================================================
    // MÉTODOS AUXILIARES (Para no repetir código)
    // =========================================================

    private static void configurarLogicaComboCategoria(ComboBox<String> cb, TextField tfOculto, Stage stageARedimensionar) {
        cb.setOnAction(e -> {
            if ("Nueva categoría...".equals(cb.getValue())) {
                tfOculto.setVisible(true);
                tfOculto.setManaged(true);
                tfOculto.requestFocus();
            } else {
                tfOculto.setVisible(false);
                tfOculto.setManaged(false);
            }
            
            // 🔴 FIX: Si nos han pasado una ventana, recalculamos su tamaño
            if (stageARedimensionar != null) {
                stageARedimensionar.sizeToScene();
            }
        });
    }

    private static String obtenerCategoriaSeleccionada(ComboBox<String> cb, TextField tfOculto) {
        String seleccion = cb.getValue();
        if ("Nueva categoría...".equals(seleccion)) {
            String nueva = tfOculto.getText().trim();
            if (nueva.isEmpty()) throw new IllegalArgumentException("Escriba un nombre para la categoría.");
            return nueva;
        }
        return seleccion;
    }

    private static void cargarCategoriasEnCombo(ComboBox<String> cb) {
        ObservableList<String> items = FXCollections.observableArrayList(controlador.getNombresCategorias());
        items.add("Nueva categoría...");
        cb.setItems(items);
        
        // Seleccionar por defecto
        if (cb.getValue() == null && !items.isEmpty()) {
             if (items.contains("Alimentación")) cb.setValue("Alimentación");
             else cb.getSelectionModel().selectFirst();
        }
    }

    // =========================================================
    // OTRAS PESTAÑAS
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

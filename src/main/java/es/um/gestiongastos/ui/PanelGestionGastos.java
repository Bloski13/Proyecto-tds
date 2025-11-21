package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Categoria;
import es.um.gestiongastos.model.Gasto;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PanelGestionGastos {

    private static final Controlador controlador = Controlador.getInstancia();
    
    // Variables de estado estáticas para mantener los datos
    private static ObservableList<Gasto> datosGastos = FXCollections.observableArrayList();
    private static ComboBox<String> cbCategoriaRegistro; 
    private static ComboBox<String> cbFiltroCategoria;
    private static Label lblTotalGastado; // Etiqueta para el total
    private static FilteredList<Gasto> listaFiltrada;
    /**
     * Crea la vista completa de la pestaña de gestión (Formulario + Filtros + Tabla).
     */
    public static VBox crearVista() {
        VBox panelPrincipal = new VBox(15);
        panelPrincipal.setPadding(new Insets(15));
        
        // 🔴 CORRECCIÓN: Inicializamos la etiqueta PRIMERO para que exista cuando la tabla intente escribir en ella
        lblTotalGastado = new Label("Total listado: 0.00 €");
        lblTotalGastado.setFont(Font.font("System", FontWeight.BOLD, 14));

        // 1. Formulario
        GridPane formulario = crearFormularioRegistro();
        
        // 2. Filtros
        Label lblFiltrar = new Label("FILTROS:");
        lblFiltrar.setStyle("-fx-font-weight: bold;");
        
        cbFiltroCategoria = new ComboBox<>();
        cbFiltroCategoria.setPromptText("Todas las categorías");
        cargarCategoriasEnFiltro(cbFiltroCategoria);

        DatePicker dpDesde = new DatePicker();
        dpDesde.setPromptText("Desde...");
        dpDesde.setDayCellFactory(param -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });
        
        DatePicker dpHasta = new DatePicker();
        dpHasta.setPromptText("Hasta...");
        dpHasta.setDayCellFactory(param -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });
        
        Button btnLimpiarFiltros = new Button("Limpiar");
        HBox barraFiltros = new HBox(10, lblFiltrar, cbFiltroCategoria, dpDesde, dpHasta, btnLimpiarFiltros);
        barraFiltros.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-background-radius: 5;");
        barraFiltros.setAlignment(Pos.CENTER_LEFT);

        // 3. Tabla (Ahora que lblTotalGastado existe, la tabla ya puede actualizarlo)
        TableView<Gasto> tablaGastos = crearTablaGastosConFiltro(cbFiltroCategoria, dpDesde, dpHasta);
        
        // 4. Barra de Totales
        HBox barraTotal = new HBox(lblTotalGastado);
        barraTotal.setAlignment(Pos.CENTER_RIGHT); 
        barraTotal.setPadding(new Insets(5, 20, 0, 0));

        // Lógica del botón Limpiar
        btnLimpiarFiltros.setOnAction(e -> {
            cbFiltroCategoria.getSelectionModel().select("Todas");
            dpDesde.setValue(null);
            dpHasta.setValue(null);
        });

        panelPrincipal.getChildren().addAll(formulario, new Separator(), barraFiltros, tablaGastos, barraTotal);
        
        return panelPrincipal;
    }

    /**
     * Método público para refrescar los datos desde fuera (llamado por VentanaPrincipal).
    */
    public static void refrescarDatos() {
    	// 1. Refrescar Tabla
    	datosGastos.clear();
        datosGastos.addAll(controlador.getGastosUsuarioActual());
        
        // 2. Refrescar ComboBox de Registro
        if (cbCategoriaRegistro != null) {
            String prev = cbCategoriaRegistro.getValue();
            cargarCategoriasEnCombo(cbCategoriaRegistro);
            if (prev != null && cbCategoriaRegistro.getItems().contains(prev)) cbCategoriaRegistro.setValue(prev);
        }
        // 3. Refrescar ComboBox de Filtro
        if (cbFiltroCategoria != null) {
            String prev = cbFiltroCategoria.getValue();
            cargarCategoriasEnFiltro(cbFiltroCategoria);
            if (prev != null && cbFiltroCategoria.getItems().contains(prev)) cbFiltroCategoria.setValue(prev);
            else cbFiltroCategoria.getSelectionModel().select("Todas");
        }
    }
    
    /**
     * Permite a PanelInformes obtener los datos que se están viendo actualmente
     */
    public static List<Gasto> getGastosVisibles() {
        if (listaFiltrada != null) {
            return listaFiltrada;
        }
        return datosGastos; // Si aún no hay filtro, devolvemos todo
    }

    // =========================================================
    // MÉTODOS PRIVADOS DE CONSTRUCCIÓN DE UI
    // =========================================================

    private static GridPane crearFormularioRegistro() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 10;");
        
        Label lblTitulo = new Label("REGISTRAR NUEVO GASTO");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(lblTitulo, 0, 0, 4, 1);
        GridPane.setHalignment(lblTitulo, HPos.LEFT);
        
        // Campos
        Label lblDescripcion = new Label("Descripción:");
        TextField tfDescripcion = new TextField();
        tfDescripcion.setPromptText("Ej: Café, Gasolina...");
        grid.add(lblDescripcion, 0, 1); grid.add(tfDescripcion, 1, 1);

        Label lblImporte = new Label("Importe (€):");
        TextField tfImporte = new TextField();
        tfImporte.setPromptText("Ej: 15.50");
        grid.add(lblImporte, 2, 1); grid.add(tfImporte, 3, 1);

        Label lblFecha = new Label("Fecha:");
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });
        grid.add(lblFecha, 0, 2); grid.add(dpFecha, 1, 2);

        // Categoría Dinámica
        Label lblCategoria = new Label("Categoría:");
        cbCategoriaRegistro = new ComboBox<>();
        TextField tfNuevaCategoria = new TextField();
        tfNuevaCategoria.setPromptText("Nombre nueva categoría...");
        tfNuevaCategoria.setVisible(false); tfNuevaCategoria.setManaged(false); 

        configurarLogicaComboCategoria(cbCategoriaRegistro, tfNuevaCategoria, null); 
        cargarCategoriasEnCombo(cbCategoriaRegistro);

        VBox boxCategoria = new VBox(5, cbCategoriaRegistro, tfNuevaCategoria);
        grid.add(lblCategoria, 2, 2); grid.add(boxCategoria, 3, 2);
        
        Button btnRegistrar = new Button("Registrar Gasto");
        Label lblStatus = new Label("");
        HBox botones = new HBox(10, btnRegistrar, lblStatus);
        grid.add(botones, 3, 3);
        GridPane.setHalignment(botones, HPos.RIGHT);

        btnRegistrar.setOnAction(e -> {
            try {
                String descripcion = tfDescripcion.getText();
                double importe = Double.parseDouble(tfImporte.getText());
                LocalDate fecha = dpFecha.getValue();
                String categoriaFinal = obtenerCategoriaSeleccionada(cbCategoriaRegistro, tfNuevaCategoria);
                
                if (descripcion.trim().isEmpty() || importe <= 0 || fecha == null) {
                     throw new IllegalArgumentException("Datos incompletos.");
                }

                controlador.registrarGasto(descripcion, importe, fecha, categoriaFinal);
                
                lblStatus.setText("Gasto guardado.");
                lblStatus.setStyle("-fx-text-fill: green;");
                
                // Limpieza
                tfDescripcion.clear(); tfImporte.clear(); dpFecha.setValue(LocalDate.now());
                tfNuevaCategoria.clear(); tfNuevaCategoria.setVisible(false); tfNuevaCategoria.setManaged(false);
                cbCategoriaRegistro.getSelectionModel().selectFirst();

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

    private static TableView<Gasto> crearTablaGastosConFiltro(ComboBox<String> cbCat, DatePicker dpDesde, DatePicker dpHasta) {
        TableView<Gasto> table = new TableView<>();
        table.setPlaceholder(new Label("No hay gastos que coincidan con el filtro."));
        
        listaFiltrada = new FilteredList<>(datosGastos, p -> true);
        
        Runnable actualizarFiltro = () -> {
            listaFiltrada.setPredicate(gasto -> {
                String catSeleccionada = cbCat.getValue();
                boolean coincideCategoria = true;
                if (catSeleccionada != null && !"Todas".equals(catSeleccionada)) {
                    coincideCategoria = gasto.getCategoria().getNombre().equalsIgnoreCase(catSeleccionada);
                }
                boolean coincideDesde = true;
                if (dpDesde.getValue() != null) coincideDesde = !gasto.getFecha().isBefore(dpDesde.getValue());
                boolean coincideHasta = true;
                if (dpHasta.getValue() != null) coincideHasta = !gasto.getFecha().isAfter(dpHasta.getValue());
                
                return coincideCategoria && coincideDesde && coincideHasta;
            });
           // Cuando cambia el filtro, avisamos a los gráficos para que se actualicen
            PanelInformes.refrescarDatos();
        };
        
        cbCat.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFiltro.run());
        dpDesde.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFiltro.run());
        dpHasta.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFiltro.run());
        
        SortedList<Gasto> listaOrdenada = new SortedList<>(listaFiltrada);
        listaOrdenada.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(listaOrdenada); 
        
        // Listener para recalcular el total automáticamente
        // Cada vez que la lista cambie (filtro aplicado, borrado, añadido), recalculamos.
        listaOrdenada.addListener((ListChangeListener<Gasto>) c -> {
            actualizarEtiquetaTotal(listaOrdenada);
        });
        
        // Cálculo inicial
        actualizarEtiquetaTotal(listaOrdenada);

        // --- COLUMNAS (Sin cambios) ---
        TableColumn<Gasto, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(200);

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
    // MÉTODO AUXILIAR PARA SUMAR EL TOTAL
    private static void actualizarEtiquetaTotal(ObservableList<Gasto> listaVisible) {
        if (lblTotalGastado == null) return;
        
        BigDecimal total = BigDecimal.ZERO;
        for (Gasto g : listaVisible) {
            total = total.add(g.getImporte());
        }
        
        lblTotalGastado.setText("Total listado: " + total.toString() + " €");
    }

    private static void mostrarDialogoEdicion(Gasto gasto) {
        Stage dialog = new Stage();
        dialog.setTitle("Editar Gasto");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField tfDesc = new TextField(gasto.getDescripcion());
        TextField tfImp = new TextField(gasto.getImporte().toString());
        DatePicker dpFec = new DatePicker(gasto.getFecha());
        dpFec.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });

        ComboBox<String> cbCatEdit = new ComboBox<>();
        TextField tfNuevaCatEdit = new TextField();
        tfNuevaCatEdit.setPromptText("Nombre nueva categoría...");
        tfNuevaCatEdit.setVisible(false); tfNuevaCatEdit.setManaged(false);
        
        configurarLogicaComboCategoria(cbCatEdit, tfNuevaCatEdit, dialog);
        cargarCategoriasEnCombo(cbCatEdit);
        
        String catActual = gasto.getCategoria().getNombre();
        if (cbCatEdit.getItems().contains(catActual)) {
            cbCatEdit.setValue(catActual);
        } else {
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
    // MÉTODOS AUXILIARES DE LÓGICA
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

    private static void cargarCategoriasEnFiltro(ComboBox<String> cb) {
        ObservableList<String> items = FXCollections.observableArrayList(controlador.getNombresCategorias());
        items.add(0, "Todas"); 
        cb.setItems(items);
        if (cb.getValue() == null) cb.getSelectionModel().select("Todas");
    }

    private static void cargarCategoriasEnCombo(ComboBox<String> cb) {
        ObservableList<String> items = FXCollections.observableArrayList(controlador.getNombresCategorias());
        items.add("Nueva categoría...");
        cb.setItems(items);
        
        if (cb.getValue() == null && !items.isEmpty()) {
             if (items.contains("Alimentación")) cb.setValue("Alimentación");
             else cb.getSelectionModel().selectFirst();
        }
    }
}





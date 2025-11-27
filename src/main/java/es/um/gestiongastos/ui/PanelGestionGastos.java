package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Categoria;
import es.um.gestiongastos.model.Gasto;
import es.um.gestiongastos.model.GastosCompartidos;
import es.um.gestiongastos.model.Persona;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PanelGestionGastos {

    private static final Controlador controlador = Controlador.getInstancia();
    
    // Datos
    private static ObservableList<Gasto> datosGastos = FXCollections.observableArrayList();
    private static FilteredList<Gasto> listaFiltrada;
    
    // Componentes UI que necesitan actualización
    private static ComboBox<String> cbCategoriaRegistro; 
    private static ComboBox<GastosCompartidos> cbCuentaRegistro; // NUEVO
    
    private static ComboBox<String> cbFiltroCategoria;
    private static ComboBox<GastosCompartidos> cbFiltroCuenta; // NUEVO
    private static Label lblTotalGastado;

    public static VBox crearVista() {
        VBox panelPrincipal = new VBox(15);
        panelPrincipal.setPadding(new Insets(15));
        
        lblTotalGastado = new Label("Mi gasto total: 0.00 €");
        lblTotalGastado.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblTotalGastado.setStyle("-fx-text-fill: #2c3e50;");

        // 1. Formulario
        GridPane formulario = crearFormularioRegistro();
        
        // 2. Filtros
        HBox barraFiltros = crearBarraFiltros();

        // 3. Tabla (MODIFICADA)
        TableView<Gasto> tablaGastos = crearTablaGastos();
        
        // 4. Totales
        HBox barraTotal = new HBox(lblTotalGastado);
        barraTotal.setAlignment(Pos.CENTER_RIGHT); 
        barraTotal.setPadding(new Insets(5, 20, 0, 0));

        panelPrincipal.getChildren().addAll(formulario, new Separator(), barraFiltros, tablaGastos, barraTotal);
        return panelPrincipal;
    }

    public static void refrescarDatos() {
        // 1. Recargar lista de gastos
        datosGastos.clear();
        datosGastos.addAll(controlador.getGastosUsuarioActual());
        
        // 2. Recargar Categorías
        if (cbCategoriaRegistro != null) cargarCategoriasEnCombo(cbCategoriaRegistro);
        if (cbFiltroCategoria != null) {
            String prev = cbFiltroCategoria.getValue();
            cargarCategoriasEnFiltro(cbFiltroCategoria);
            if (prev != null && cbFiltroCategoria.getItems().contains(prev)) cbFiltroCategoria.setValue(prev);
            else cbFiltroCategoria.getSelectionModel().select("Todas");
        }
        
        // 3. Recargar Cuentas (NUEVO)
        refrescarCombosCuentas();
    }
    
    public static List<Gasto> getGastosVisibles() {
        if (listaFiltrada != null) {
            return listaFiltrada;
        }
        return datosGastos; 
    }

    // --- CREACIÓN DE UI ---

    private static GridPane crearFormularioRegistro() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 10;");
        
        Label lblTitulo = new Label("REGISTRAR NUEVO GASTO");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(lblTitulo, 0, 0, 4, 1);
        
        // Campos existentes
        TextField tfDescripcion = new TextField(); tfDescripcion.setPromptText("Ej: Café...");
        TextField tfImporte = new TextField(); tfImporte.setPromptText("0.00");
        DatePicker dpFecha = new DatePicker(LocalDate.now());

        grid.add(new Label("Descripción:"), 0, 1); grid.add(tfDescripcion, 1, 1);
        grid.add(new Label("Importe (€):"), 2, 1); grid.add(tfImporte, 3, 1);
        grid.add(new Label("Fecha:"), 0, 2);       grid.add(dpFecha, 1, 2);

        // Selector Categoría
        cbCategoriaRegistro = new ComboBox<>();
        TextField tfNuevaCategoria = new TextField();
        tfNuevaCategoria.setPromptText("Nueva categoría...");
        tfNuevaCategoria.setVisible(false); tfNuevaCategoria.setManaged(false);
        configurarLogicaComboCategoria(cbCategoriaRegistro, tfNuevaCategoria, null);
        cargarCategoriasEnCombo(cbCategoriaRegistro);
        
        VBox boxCat = new VBox(5, cbCategoriaRegistro, tfNuevaCategoria);
        grid.add(new Label("Categoría:"), 2, 2); grid.add(boxCat, 3, 2);

        // NUEVO: Selector de Cuenta
        cbCuentaRegistro = new ComboBox<>();
        cbCuentaRegistro.setPromptText("Selecciona cuenta...");
        cbCuentaRegistro.setPrefWidth(200);
        // Converter para mostrar el nombre
        StringConverter<GastosCompartidos> converter = new StringConverter<>() {
            @Override public String toString(GastosCompartidos c) { return c == null ? "" : c.getNombre(); }
            @Override public GastosCompartidos fromString(String s) { return null; }
        };
        cbCuentaRegistro.setConverter(converter);
        
        grid.add(new Label("Cuenta:"), 0, 3); grid.add(cbCuentaRegistro, 1, 3);

        Button btnRegistrar = new Button("Registrar Gasto");
        Label lblStatus = new Label("");
        HBox botones = new HBox(10, btnRegistrar, lblStatus);
        grid.add(botones, 2, 3, 2, 1);
        GridPane.setHalignment(botones, HPos.RIGHT);

        btnRegistrar.setOnAction(e -> {
            try {
                String desc = tfDescripcion.getText();
                double imp = Double.parseDouble(tfImporte.getText());
                LocalDate fec = dpFecha.getValue();
                String cat = obtenerCategoriaSeleccionada(cbCategoriaRegistro, tfNuevaCategoria);
                GastosCompartidos cuenta = cbCuentaRegistro.getValue(); // Obtener cuenta
                
                if (desc.isEmpty() || imp <= 0 || fec == null || cuenta == null) {
                     throw new IllegalArgumentException("Datos incompletos (Verifique cuenta).");
                }
                
                // Llamada al controlador actualizado
                controlador.registrarGasto(desc, imp, fec, cat, cuenta);
                
                lblStatus.setText("Guardado."); lblStatus.setStyle("-fx-text-fill: green;");
                tfDescripcion.clear(); tfImporte.clear(); dpFecha.setValue(LocalDate.now());
                tfNuevaCategoria.clear(); tfNuevaCategoria.setVisible(false); tfNuevaCategoria.setManaged(false);
                cbCategoriaRegistro.getSelectionModel().selectFirst();
            } catch (Exception ex) {
                lblStatus.setText("Error: " + ex.getMessage()); lblStatus.setStyle("-fx-text-fill: red;");
            }
        });

        return grid;
    }

    private static HBox crearBarraFiltros() {
        Label lblFiltrar = new Label("FILTROS:");
        lblFiltrar.setStyle("-fx-font-weight: bold;");
        
        // Inicialización de componentes (igual que antes)
        cbFiltroCategoria = new ComboBox<>();
        cargarCategoriasEnFiltro(cbFiltroCategoria);
        
        cbFiltroCuenta = new ComboBox<>();
        cbFiltroCuenta.setPromptText("Todas las cuentas");
        cbFiltroCuenta.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(GastosCompartidos o) { return o == null ? "Todas las cuentas" : o.getNombre(); }
            @Override public GastosCompartidos fromString(String s) { return null; }
        });

        DatePicker dpDesde = new DatePicker(); dpDesde.setPromptText("Desde...");
        DatePicker dpHasta = new DatePicker(); dpHasta.setPromptText("Hasta...");
        
        // --- NUEVO BOTÓN APLICAR ---
        Button btnAplicar = new Button("Aplicar filtros");
        btnAplicar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAplicar.setOnAction(e -> aplicarLogicaFiltros(dpDesde, dpHasta));

        // --- BOTÓN LIMPIAR MODIFICADO ---
        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setOnAction(e -> {
            // 1. Resetear campos visuales
            cbFiltroCategoria.getSelectionModel().select("Todas");
            cbFiltroCuenta.setValue(null);
            dpDesde.setValue(null);
            dpHasta.setValue(null);
            
            // 2. Resetear lógica (mostrar todo)
            listaFiltrada.setPredicate(p -> true);
            
            // 3. Actualizar gráficos
            PanelInformes.refrescarDatos();
        });

        HBox barra = new HBox(10, lblFiltrar, cbFiltroCategoria, cbFiltroCuenta, dpDesde, dpHasta, btnAplicar, btnLimpiar);
        barra.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-background-radius: 5;");
        barra.setAlignment(Pos.CENTER_LEFT);
        
        return barra;
    }
    
    private static void aplicarLogicaFiltros(DatePicker dpDesde, DatePicker dpHasta) {
        if (listaFiltrada == null) return;

        listaFiltrada.setPredicate(g -> {
            // 1. Filtro Categoría
            String catSel = cbFiltroCategoria.getValue();
            boolean okCat = (catSel == null || "Todas".equals(catSel)) || g.getCategoria().getNombre().equals(catSel);
            
            // 2. Filtro Cuenta
            GastosCompartidos ctaSel = cbFiltroCuenta.getValue();
            boolean okCta = (ctaSel == null) || g.getCuenta().equals(ctaSel);
            
            // 3. Filtro Fechas
            boolean okDesde = (dpDesde.getValue() == null) || !g.getFecha().isBefore(dpDesde.getValue());
            boolean okHasta = (dpHasta.getValue() == null) || !g.getFecha().isAfter(dpHasta.getValue());
            
            return okCat && okCta && okDesde && okHasta;
        });

        // IMPORTANTE: Tras filtrar la tabla, ordenamos a los informes que se repinten
        // usando los datos filtrados (PanelInformes usa getGastosVisibles())
        PanelInformes.refrescarDatos();
    }

    private static TableView<Gasto> crearTablaGastos() {
        TableView<Gasto> table = new TableView<>();
        
        // AJUSTE: Eliminar columna vacía estirando las columnas automáticamente
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        listaFiltrada = new FilteredList<>(datosGastos, p -> true);
        SortedList<Gasto> sortedData = new SortedList<>(listaFiltrada);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
        sortedData.addListener((ListChangeListener<Gasto>) c -> actualizarEtiquetaTotal(sortedData));

        // --- COLUMNAS ---

        // 1. Fecha
        TableColumn<Gasto, LocalDate> colFec = new TableColumn<>("Fecha");
        colFec.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFec.setMinWidth(90);
        colFec.setMaxWidth(100); // Fijamos máximo para que no crezca
        colFec.setPrefWidth(95);

        // 2. Descripción (Flexible)
        TableColumn<Gasto, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        // Esta dejamos que crezca

        // 3. Categoría (Tamaño medio fijo)
        TableColumn<Gasto, String> colCat = new TableColumn<>("Categoría");
        colCat.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategoria().getNombre()));
        colCat.setPrefWidth(110);
        colCat.setMinWidth(100);
        colCat.setMaxWidth(150);

        // 4. Cuenta
        TableColumn<Gasto, String> colCta = new TableColumn<>("Cuenta");
        colCta.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCuenta().getNombre()));
        colCta.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
        colCta.setPrefWidth(140); 
        colCta.setMinWidth(120);

        // 5. Pagador
        TableColumn<Gasto, String> colPagador = new TableColumn<>("Pagado por");
        colPagador.setCellValueFactory(cell -> {
            if (cell.getValue().getPagador().equals(controlador.getUsuarioAutenticado())) return new SimpleStringProperty("Mí");
            return new SimpleStringProperty(cell.getValue().getPagador().getNombreUsuario());
        });
        colPagador.setMaxWidth(100);
        colPagador.setMinWidth(80);

        // 6. Total
        TableColumn<Gasto, BigDecimal> colImpTotal = new TableColumn<>("Total");
        colImpTotal.setCellValueFactory(new PropertyValueFactory<>("importe"));
        colImpTotal.setMaxWidth(80);
        colImpTotal.setMinWidth(60);
        
        // 7. Mi Parte
        TableColumn<Gasto, String> colMiParte = new TableColumn<>("Mi Parte");
        colMiParte.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCostePara(controlador.getUsuarioAutenticado()) + " €"));
        colMiParte.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        colMiParte.setMaxWidth(80);
        colMiParte.setMinWidth(60);

        // 8. Acciones (Modificar + Eliminar)
        TableColumn<Gasto, Void> colAx = new TableColumn<>("Acciones");
        colAx.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modificar");
            private final Button btnDel = new Button("Eliminar");
            private final HBox pane = new HBox(5, btnEdit, btnDel);
            {
                // Estilo botón Modificar
                btnEdit.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11px;");
                btnEdit.setOnAction(e -> {
                    Gasto g = getTableRow().getItem();
                    if (g != null) mostrarDialogoEdicion(g);
                });

                // Estilo botón Eliminar
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px;");
                btnDel.setOnAction(e -> {
                    Gasto g = getTableRow().getItem();
                    if (g != null) controlador.borrarGasto(g.getId());
                });
                
                pane.setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        colAx.setMinWidth(160);
        colAx.setPrefWidth(170);

        table.getColumns().addAll(colFec, colDesc, colCat, colCta, colPagador, colImpTotal, colMiParte, colAx);
        return table;
    }

    private static void mostrarDialogoEdicion(Gasto gasto) {
        Stage dialog = new Stage();
        dialog.setTitle("Modificar Gasto");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField tfDesc = new TextField(gasto.getDescripcion());
        TextField tfImp = new TextField(gasto.getImporte().toString());
        DatePicker dpFec = new DatePicker(gasto.getFecha());
        
        // Categoría
        ComboBox<String> cbCat = new ComboBox<>();
        TextField tfNuevaCat = new TextField();
        tfNuevaCat.setVisible(false);
        configurarLogicaComboCategoria(cbCat, tfNuevaCat, dialog);
        cargarCategoriasEnCombo(cbCat);
        cbCat.setValue(gasto.getCategoria().getNombre());
        VBox boxCat = new VBox(5, cbCat, tfNuevaCat);

        // Cuenta
        ComboBox<GastosCompartidos> cbCta = new ComboBox<>();
        cbCta.setConverter(new StringConverter<>() {
            @Override public String toString(GastosCompartidos c) { return c == null ? "" : c.getNombre(); }
            @Override public GastosCompartidos fromString(String s) { return null; }
        });
        cbCta.setItems(FXCollections.observableArrayList(controlador.getUsuarioAutenticado().getCuentas()));
        
        // Pagador 
        ComboBox<Persona> cbPagador = new ComboBox<>();
        cbPagador.setConverter(new StringConverter<>() {
            @Override public String toString(Persona p) { return p == null ? "" : p.getNombreUsuario(); }
            @Override public Persona fromString(String s) { return null; }
        });

        // Lógica de actualización dinámica: Cuenta -> Pagadores
        cbCta.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Cargar participantes de la cuenta seleccionada
                List<Persona> participantes = newVal.getParticipantes().stream()
                        .map(GastosCompartidos.Participante::getPersona)
                        .collect(java.util.stream.Collectors.toList());
                cbPagador.setItems(FXCollections.observableArrayList(participantes));
                
                // Intentar mantener el pagador seleccionado si existe en la nueva lista, si no, seleccionar al usuario actual
                Persona seleccionActual = cbPagador.getValue();
                if (seleccionActual != null && participantes.contains(seleccionActual)) {
                    cbPagador.setValue(seleccionActual);
                } else {
                    cbPagador.setValue(controlador.getUsuarioAutenticado());
                }
            }
        });

        // Inicializar valores
        cbCta.setValue(gasto.getCuenta()); 
        cbPagador.setValue(gasto.getPagador()); 

        grid.add(new Label("Descripción:"), 0, 0); grid.add(tfDesc, 1, 0);
        grid.add(new Label("Importe (€):"), 0, 1); grid.add(tfImp, 1, 1);
        grid.add(new Label("Fecha:"), 0, 2);       grid.add(dpFec, 1, 2);
        grid.add(new Label("Categoría:"), 0, 3);   grid.add(boxCat, 1, 3);
        grid.add(new Label("Cuenta:"), 0, 4);      grid.add(cbCta, 1, 4);
        grid.add(new Label("Pagado por:"), 0, 5);  grid.add(cbPagador, 1, 5); // NUEVO CAMPO

        Button btnGuardar = new Button("Guardar Cambios");
        btnGuardar.setOnAction(e -> {
            try {
                String desc = tfDesc.getText();
                Double imp = Double.parseDouble(tfImp.getText());
                LocalDate fec = dpFec.getValue();
                String cat = obtenerCategoriaSeleccionada(cbCat, tfNuevaCat);
                GastosCompartidos cta = cbCta.getValue();
                Persona pagador = cbPagador.getValue();
                
                controlador.modificarGasto(gasto.getId(), desc, imp, fec, cat, cta, pagador);
                dialog.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                alert.show();
            }
        });

        grid.add(btnGuardar, 1, 6);
        
        Scene scene = new Scene(grid);
        dialog.setScene(scene);
        dialog.show();
    }

    // --- AUXILIARES ---
    
    private static void refrescarCombosCuentas() {
        if (controlador.getUsuarioAutenticado() == null) return;
        List<GastosCompartidos> cuentas = controlador.getUsuarioAutenticado().getCuentas();
        
        if (cbCuentaRegistro != null) {
            GastosCompartidos prev = cbCuentaRegistro.getValue();
            cbCuentaRegistro.setItems(FXCollections.observableArrayList(cuentas));
            if (prev != null && cuentas.contains(prev)) cbCuentaRegistro.setValue(prev);
            else if (!cuentas.isEmpty()) cbCuentaRegistro.getSelectionModel().selectFirst();
        }
        
        if (cbFiltroCuenta != null) {
            GastosCompartidos prev = cbFiltroCuenta.getValue();
            ObservableList<GastosCompartidos> itemsFiltro = FXCollections.observableArrayList(cuentas);
            cbFiltroCuenta.setItems(itemsFiltro);
            if (prev != null && cuentas.contains(prev)) cbFiltroCuenta.setValue(prev);
        }
    }

    private static void actualizarEtiquetaTotal(ObservableList<Gasto> lista) {
        if (lblTotalGastado == null || controlador.getUsuarioAutenticado() == null) return;
        
        BigDecimal totalMio = BigDecimal.ZERO;
        for (Gasto g : lista) {
            totalMio = totalMio.add(g.getCostePara(controlador.getUsuarioAutenticado()));
        }
        
        lblTotalGastado.setText("Mi gasto total (filtrado): " + totalMio + " €");
    }
    
    // Métodos de Categorías y Configuración reutilizados 
    private static void configurarLogicaComboCategoria(ComboBox<String> cb, TextField tf, Stage st) {
        cb.setOnAction(e -> {
            boolean nueva = "Nueva categoría...".equals(cb.getValue());
            tf.setVisible(nueva); tf.setManaged(nueva);
            if (nueva) tf.requestFocus();
            if (st != null) st.sizeToScene();
        });
    }
    
    private static String obtenerCategoriaSeleccionada(ComboBox<String> cb, TextField tf) {
        if ("Nueva categoría...".equals(cb.getValue())) return tf.getText().trim();
        return cb.getValue();
    }
    
    private static void cargarCategoriasEnFiltro(ComboBox<String> cb) {
        ObservableList<String> it = FXCollections.observableArrayList(controlador.getNombresCategorias());
        it.add(0, "Todas"); cb.setItems(it); cb.getSelectionModel().selectFirst();
    }
    
    private static void cargarCategoriasEnCombo(ComboBox<String> cb) {
        ObservableList<String> it = FXCollections.observableArrayList(controlador.getNombresCategorias());
        it.add("Nueva categoría..."); cb.setItems(it); 
        if(cb.getValue()==null && !it.isEmpty()) cb.getSelectionModel().selectFirst();
    }
    
}
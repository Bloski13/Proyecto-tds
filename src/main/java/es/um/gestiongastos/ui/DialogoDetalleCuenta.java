package es.um.gestiongastos.ui;

import es.um.gestiongastos.model.Gasto;
import es.um.gestiongastos.model.GastosCompartidos;
import es.um.gestiongastos.model.Persona;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class DialogoDetalleCuenta {

    public static void mostrar(GastosCompartidos cuenta) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Detalles: " + cuenta.getNombre());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // 1. Cabecera con Total Cuenta
        BigDecimal totalCuenta = cuenta.getGastos().stream()
                .map(Gasto::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Label lblNombre = new Label("Cuenta: " + cuenta.getNombre());
        lblNombre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label lblTotal = new Label("Gasto Total Acumulado: " + totalCuenta + " €");
        lblTotal.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        // 2. Tabla de Participantes
        TableView<FilaDetalle> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FilaDetalle, String> colUser = new TableColumn<>("Participante");
        colUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().persona.getNombreCompleto()));

        TableColumn<FilaDetalle, String> colPorc = new TableColumn<>("Porcentaje");
        colPorc.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f %%", data.getValue().porcentaje)));

        TableColumn<FilaDetalle, String> colGastoPropio = new TableColumn<>("Gasto Asumido");
        colGastoPropio.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().gastoAsumido + " €"));

        TableColumn<FilaDetalle, String> colSaldo = new TableColumn<>("Saldo Actual");
        colSaldo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().saldo + " €"));
        colSaldo.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("-")) setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    else if (item.equals("0.00 €")) setStyle("-fx-text-fill: black;");
                    else setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }
        });

        tabla.getColumns().addAll(colUser, colPorc, colGastoPropio, colSaldo);

        // Calcular datos para la tabla
        List<FilaDetalle> filas = cuenta.getParticipantes().stream().map(p -> {
            Persona persona = p.getPersona();
            
            // Calculamos el Gasto Asumido sumando la parte proporcional de cada gasto del historial
            BigDecimal gastoAsumido = cuenta.getGastos().stream()
                    .map(g -> g.getCostePara(persona))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new FilaDetalle(
                    persona,
                    p.getPorcentaje(),
                    gastoAsumido,
                    p.getSaldo()
            );
        }).collect(Collectors.toList());

        tabla.setItems(FXCollections.observableArrayList(filas));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> dialog.close());
        btnCerrar.setAlignment(Pos.CENTER);

        root.getChildren().addAll(lblNombre, lblTotal, tabla, btnCerrar);
        
        Scene scene = new Scene(root, 600, 400);
        dialog.setScene(scene);
        dialog.show();
    }

    // Clase auxiliar interna para mostrar datos
    private static class FilaDetalle {
        Persona persona;
        double porcentaje;
        BigDecimal gastoAsumido;
        BigDecimal saldo;

        public FilaDetalle(Persona p, double porc, BigDecimal gasto, BigDecimal saldo) {
            this.persona = p;
            this.porcentaje = porc;
            this.gastoAsumido = gasto;
            this.saldo = saldo;
        }
    }
}
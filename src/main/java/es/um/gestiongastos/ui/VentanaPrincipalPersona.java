package es.um.gestiongastos.ui;

import es.um.gestiongastos.model.Persona;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Ventana provisional que muestra la lista de usuarios en una TableView
 * y los datos del usuario que ha iniciado sesión.
 */
public class VentanaPrincipalPersona {

    /**
     * Muestra la ventana con la lista de usuarios y detalle del usuario actualmente autenticado.
     */
    public static void mostrar(List<Persona> usuarios, Persona autenticado) {
        Stage stage = new Stage();
        stage.setTitle("Usuarios - Sesión de " + autenticado.getNombreUsuario());

        // TableView con columnas: ID, Nombre completo, Nombre de usuario
        TableView<Persona> table = new TableView<>();
        table.setPrefWidth(520);
        table.setPrefHeight(240);

        TableColumn<Persona, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));
        colId.setPrefWidth(100);

        TableColumn<Persona, String> colNombre = new TableColumn<>("Nombre completo");
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombreCompleto()));
        colNombre.setPrefWidth(260);

        TableColumn<Persona, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombreUsuario()));
        colUsuario.setPrefWidth(140);

        table.getColumns().addAll(colId, colNombre, colUsuario);
        table.getItems().addAll(usuarios);

        // Panel derecho: detalles del usuario autenticado
        Label lblDetalle = new Label("Usuario autenticado:");
        Label lblId = new Label("ID: " + autenticado.getId());
        Label lblNombre = new Label("Nombre: " + autenticado.getNombreCompleto());
        Label lblUsuario = new Label("Usuario: " + autenticado.getNombreUsuario());
        Label lblPwd = new Label("Contraseña (no recomendado mostrar): " + autenticado.getContraseña());
        VBox detalle = new VBox(8, lblDetalle, lblId, lblNombre, lblUsuario, lblPwd);
        detalle.setPadding(new Insets(6));
        detalle.setAlignment(Pos.TOP_LEFT);
        detalle.setStyle("-fx-border-color: #ccc; -fx-border-radius: 4; -fx-padding: 6;");

        // Botón para cerrar
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(ev -> stage.close());

        HBox bottom = new HBox(btnCerrar);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setCenter(table);
        root.setRight(detalle);
        root.setBottom(bottom);
        BorderPane.setMargin(detalle, new Insets(12));
        BorderPane.setMargin(table, new Insets(12));

        Scene scene = new Scene(root, 760, 320);
        stage.setScene(scene);
        stage.show();
    }
}

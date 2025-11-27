package es.um.gestiongastos.ui;

import es.um.gestiongastos.model.Persona;
import es.um.gestiongastos.controlador.Controlador;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class VentanaRegistro {

    public static void mostrar(Stage owner, Controlador controlador, Consumer<Persona> onCreated) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Registrar nuevo usuario");

        Label lblTitulo = new Label("Registro de usuario");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblNombre = new Label("Nombre completo:");
        TextField tfNombre = new TextField();

        Label lblUsuario = new Label("Nombre de usuario:");
        TextField tfUsuario = new TextField();

        Label lblPwd = new Label("Contraseña:");
        PasswordField pfPwd = new PasswordField();

        Label lblPwd2 = new Label("Confirmar contraseña:");
        PasswordField pfPwd2 = new PasswordField();

        Label lblStatus = new Label();
        lblStatus.setStyle("-fx-text-fill: red;");

        Button btnRegistrar = new Button("Registrar");
        Button btnCancelar = new Button("Cancelar");

        HBox botones = new HBox(8, btnRegistrar, btnCancelar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(lblNombre, 0, 0);
        grid.add(tfNombre, 1, 0);
        grid.add(lblUsuario, 0, 1);
        grid.add(tfUsuario, 1, 1);
        grid.add(lblPwd, 0, 2);
        grid.add(pfPwd, 1, 2);
        grid.add(lblPwd2, 0, 3);
        grid.add(pfPwd2, 1, 3);
        grid.add(lblStatus, 0, 4, 2, 1);
        grid.add(botones, 1, 5);
        GridPane.setMargin(botones, new Insets(10, 0, 0, 0));

        VBox root = new VBox(10, lblTitulo, grid);
        root.setPadding(new Insets(12));
        root.setPrefWidth(480);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Acciones
        btnCancelar.setOnAction(ev -> stage.close());

        btnRegistrar.setOnAction(ev -> {
            lblStatus.setText("");
            String nombre = tfNombre.getText() == null ? "" : tfNombre.getText().trim();
            String usuario = tfUsuario.getText() == null ? "" : tfUsuario.getText().trim();
            String pwd = pfPwd.getText() == null ? "" : pfPwd.getText();
            String pwd2 = pfPwd2.getText() == null ? "" : pfPwd2.getText();

            if (nombre.isEmpty() || usuario.isEmpty() || pwd.isEmpty() || pwd2.isEmpty()) {
                lblStatus.setText("Rellena todos los campos.");
                return;
            }
            if (!pwd.equals(pwd2)) {
                lblStatus.setText("Las contraseñas no coinciden.");
                return;
            }

            try {
                Persona creada = controlador.registrarUsuario(nombre, usuario, pwd);
                lblStatus.setStyle("-fx-text-fill: green;");
                lblStatus.setText("Usuario registrado correctamente: " + creada.getNombreUsuario());
                if (onCreated != null) {
                    onCreated.accept(creada);
                }
                stage.close();
            } catch (IllegalArgumentException ex) {
                lblStatus.setStyle("-fx-text-fill: red;");
                lblStatus.setText(ex.getMessage());
            } catch (Exception ex) {
                lblStatus.setStyle("-fx-text-fill: red;");
                lblStatus.setText("Error al registrar usuario.");
                ex.printStackTrace();
            }
        });

        stage.show();
    }
}

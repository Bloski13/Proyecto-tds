package es.um.gestiongastos.ui;

import es.um.gestiongastos.model.Persona;
import es.um.gestiongastos.controlador.Controlador;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

public class VentanaInicioSesion extends Application {

    private Controlador controlador;

    @Override
    public void start(Stage primaryStage) {
        controlador = new Controlador(); // controlador con repositorio en memoria

        Label titulo = new Label("Iniciar sesión");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblUsuario = new Label("Usuario:");
        TextField tfUsuario = new TextField();
        tfUsuario.setPromptText("nombreUsuario");

        Label lblPassword = new Label("Contraseña:");
        PasswordField pfPassword = new PasswordField();
        pfPassword.setPromptText("contraseña");

        Label lblStatus = new Label();
        lblStatus.setStyle("-fx-text-fill: red;");

        Button btnAceptar = new Button("Aceptar");
        Button btnCancelar = new Button("Cancelar");
        Button btnRegistrar = new Button("Registrar");

        HBox botones = new HBox(8, btnAceptar, btnRegistrar, btnCancelar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(lblUsuario, 0, 0);
        grid.add(tfUsuario, 1, 0);
        grid.add(lblPassword, 0, 1);
        grid.add(pfPassword, 1, 1);
        grid.add(lblStatus, 0, 2, 2, 1);
        grid.add(botones, 1, 3);
        GridPane.setMargin(botones, new Insets(10, 0, 0, 0));

        VBox root = new VBox(12, titulo, grid);
        root.setPadding(new Insets(18));
        root.setPrefWidth(420);

        Scene scene = new Scene(root);

        // Aceptar: autenticar
        btnAceptar.setOnAction(ev -> {
            lblStatus.setText("");
            String user = tfUsuario.getText() == null ? "" : tfUsuario.getText().trim();
            String pwd = pfPassword.getText() == null ? "" : pfPassword.getText();

            Optional<Persona> autenticado = controlador.autenticar(user, pwd);
            if (autenticado.isPresent()) {
                controlador.abrirVentanaPrincipalPersona(autenticado.get());
                primaryStage.close();
            } else {
                lblStatus.setText("Usuario o contraseña incorrectos.");
            }
        });

        // Cancelar: cerrar app
        btnCancelar.setOnAction(ev -> primaryStage.close());

        // Registrar: abrir ventana de registro; callback rellena user y pone foco en contraseña
        btnRegistrar.setOnAction(ev -> {
            Consumer<Persona> onCreated = personaCreada -> {
                // rellenar nombre de usuario en el formulario e indicar registro OK
                tfUsuario.setText(personaCreada.getNombreUsuario());
                pfPassword.requestFocus();
            };
            VentanaRegistro.mostrar(primaryStage, controlador, onCreated);
        });

        pfPassword.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) btnAceptar.fire();
        });

        tfUsuario.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) pfPassword.requestFocus();
        });

        primaryStage.setTitle("Inicio de sesión - GestionGastos");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

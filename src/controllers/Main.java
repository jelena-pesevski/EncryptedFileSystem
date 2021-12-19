package controllers;

import classes.EncryptedFileSystem;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String LOGIN_PAGE="/view/LoginPage.fxml";
    public static Stage window;
    @Override
    public void start(Stage primaryStage) throws Exception{
        EncryptedFileSystem.loadUsers();
        EncryptedFileSystem.checkEnvironment();
        window=primaryStage;
        Parent root= FXMLLoader.load(getClass().getResource(LOGIN_PAGE));
        Scene scene = new Scene(root);
        window.setScene(scene);
        window.setTitle("Welcome");
        window.setResizable(false);
        window.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        EncryptedFileSystem.saveUsers();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

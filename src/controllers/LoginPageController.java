package controllers;

import classes.EncryptedFileSystem;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;

public class LoginPageController {

    private static final String SIGN_UP_PAGE="/view/SignUpPage.fxml";
    private static final String MAIN_PAGE="/view/MainPage.fxml";
    private File selectedCert;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label passwordLabel;

    @FXML
    private TextField username;

    @FXML
    private PasswordField password;

    @FXML
    private Button selectButton;

    @FXML
    private Button loginButton;

    @FXML
    private Button signUpButton;

    @FXML
    void certificateSelected(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose certificate");
        File selectedFile = fileChooser.showOpenDialog(Main.window);
        if (selectedFile != null) {
            this.selectedCert=selectedFile;
        }
    }

    @FXML
    void login(ActionEvent event) {
        if(selectedCert==null){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Certificate not selected");
            alert.setHeaderText(null);

            alert.setContentText("Please select certificate.");

            alert.showAndWait();
            return;
        }
        else if(EncryptedFileSystem.login(username.getText(), password.getText(),selectedCert)){
            try {
                Main.window.close();
                FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_PAGE));
                Parent root = loader.load();
                Scene mainScene = new Scene(root);
                Main.window.setScene(mainScene);
                Main.window.setTitle("EFS");
                Main.window.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.setContentText(" Something is not valid. Please try again");

            alert.showAndWait();
        }
    }

    @FXML
    void toSignUp(ActionEvent event) {
        try{
            Main.window.close();
            FXMLLoader loader=new FXMLLoader(getClass().getResource(SIGN_UP_PAGE));
            Parent root= loader.load();
            Scene signUpScene=new Scene(root);
            Main.window.setScene(signUpScene);
            Main.window.setTitle("Sign up");
            Main.window.show();
        }catch (Exception e){
           // e.printStackTrace();
        }
    }

}



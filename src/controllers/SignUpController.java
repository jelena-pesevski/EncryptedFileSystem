package controllers;

import classes.EncryptedFileSystem;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

public class SignUpController {
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
    void toSignUp(ActionEvent event) {
        if(selectedCert==null){
            showError("Please select your certificate.");
            return;
        }
        if(username.getText().length()==0 || password.getText().length()==0){
            showError("Username or password missing.");
            return;
        }
        if(username.getText().length()<3 || password.getText().length()<3){
            showError("Username or password are too short.");
            return;
        }
        if(EncryptedFileSystem.trySigningUp(username.getText(), password.getText(), selectedCert)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Congratulations");
            alert.setHeaderText(null);
            alert.setContentText("You have created your account!");
            alert.showAndWait();

            try {
                Main.window.close();
                FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_PAGE));
                Parent root = loader.load();
                Scene mainScene = new Scene(root);
                Main.window.setScene(mainScene);
                Main.window.setTitle("EFS");
                Main.window.show();
            } catch (Exception e) {
             //   e.printStackTrace();
            }
        }else{
            showError("Something went wrong. Please try again.");
        }
    }

    private void showError(String content){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Dialog");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}

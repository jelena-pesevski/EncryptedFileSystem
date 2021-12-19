package controllers;

import classes.EncryptedFileSystem;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;


public class AddTxtFile {
    private static final String MAIN_PAGE="/view/MainPage.fxml";

    @FXML
    private TextField textArea;

    @FXML
    private TextField filenameField;

    @FXML
    private Button createButton;

    @FXML
    void createFile(ActionEvent evencreateTxtFilet) {
        EncryptedFileSystem.createTxt(filenameField.getText(),textArea.getText());
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

    }

}

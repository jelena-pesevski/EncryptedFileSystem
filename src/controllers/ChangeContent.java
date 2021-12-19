package controllers;

import classes.Cryptography;
import classes.EncryptedFileSystem;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class ChangeContent implements Initializable {
    public static final String MAIN_PAGE="/view/MainPage.fxml";

    @FXML
    private TextArea textArea;

    @FXML
    private Button saveButton;

    @FXML
    void saveChanges(ActionEvent event) {
        String newContent=textArea.getText();

        //upisati novi sadrzaj u fileForChanging, append false
        //upisujemo nekriptovano u fajl
        try{
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileForChanging, false)));
            out.println(newContent);
            out.close();

            //kriptovati fileForChanging
            Cryptography.encryptFile(fileForChanging, EncryptedFileSystem.currentUser.getUsername());
        }catch (Exception e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Something went wrong.");
            alert.showAndWait();

        }

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

    @FXML
    private Label fileName;

    private File fileForChanging;
    private String tempFile="./temp/tempFile.txt";

    public void setFileForChanging(File fileForChanging) {
        this.fileForChanging = fileForChanging;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileForChanging=MainPageController.getSelectedFile();

        fileName.setText(fileForChanging.getName());

        //dekriptujemo u temp
        File temp=new File(tempFile);
        byte[] content= Cryptography.decryptFileUserDir(fileForChanging,temp);
        if(content==null)return;

        //prikazemo sadrzaj tempa
        textArea.setText(new String(content));
    }
}
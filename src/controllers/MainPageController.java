package controllers;

import classes.Cryptography;
import classes.EncryptedFileSystem;
import classes.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;


import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainPageController implements Initializable {

    private static File selectedFile;
    private static final String ADDTXT_PAGE="/view/AddTxtFile.fxml";
    private static final String CHANGE_CONTENT_PAGE ="/view/ChangeContent.fxml";
    private static final String SHARED_DIR_PAGE="/view/SharedDir.fxml";
    private static final String LOGIN_PAGE="/view/LoginPage.fxml";

    public static File getSelectedFile() {
        return selectedFile;
    }

    @FXML
    private TreeView<File> efsTree;

    @FXML
    private Button addFileButton;

    @FXML
    private Button directoryAddButton;

    @FXML
    private Button uploadButton;

    @FXML
    private Button openButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button changeContentButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button sharedDirButton;

    @FXML
    private Button sendButton;

    @FXML
    private Button logOutButton;

    @FXML
    void logOut(ActionEvent event) {
        try {
            Main.window.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_PAGE));
            Parent root = loader.load();
            Scene addTxtScene = new Scene(root);
            Main.window.setScene(addTxtScene);
            Main.window.setTitle("Welcome");
            Main.window.show();

            EncryptedFileSystem.currentUser=null;
        } catch (Exception e) {
            //   e.printStackTrace();
        }

    }

    @FXML
    void addDirectory(ActionEvent event) {
        if(selectedFile==null){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Nothing is selected");

            alert.showAndWait();
            return;
        }

        if(!selectedFile.isDirectory()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Selected file is not a directory.");

            alert.showAndWait();
            return;
        }
        String dirName="";
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Add directory");
        dialog.setHeaderText(null);
        dialog.setContentText("Name of a directory:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            dirName= result.get();
           // System.out.println("Name of a directory: " + result.get());
        }

        File newDir=new File(selectedFile.getAbsolutePath()+File.separator+dirName);
        if(!newDir.exists()){
            newDir.mkdir();
        }
        efsTree.setRoot(createNode(EncryptedFileSystem.currentUser.getUserDir()));
        manageView();
    }

    @FXML
    void addTxtFile(ActionEvent event) {
        if(selectedFile==null || !selectedFile.isDirectory()){
            showError("Select destination directory.");
            return;
        }
        try {
            Main.window.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ADDTXT_PAGE));
            Parent root = loader.load();
            Scene addTxtScene = new Scene(root);
            Main.window.setScene(addTxtScene);
            Main.window.setTitle("Add txt file");
            Main.window.show();
        } catch (Exception e) {
          //  e.printStackTrace();
        }
    }

    @FXML
    void changeContent(ActionEvent event) {
        if(selectedFile==null){
            showError("Destination is not selected");
            return;
        }

        if(selectedFile.isDirectory() || !selectedFile.getName().endsWith(".txt")) {
            showError("Selected file is not a txt file.");
            return;
        }

        try {
            Main.window.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(CHANGE_CONTENT_PAGE));
            Parent root = loader.load();
            Scene addTxtScene = new Scene(root);
            Main.window.setScene(addTxtScene);
            Main.window.setTitle("Change content");
            Main.window.show();
        } catch (Exception e) {
           // e.printStackTrace();
        }


    }

    @FXML
    void delete(ActionEvent event) {
        if(selectedFile.equals(EncryptedFileSystem.currentUser.getUserDir()))return;

        boolean result=true;

        if(!selectedFile.isDirectory()){
            EncryptedFileSystem.deleteConnectedFiles(selectedFile.getName());
            result=selectedFile.delete();
        }else {
            result=deleteDirRecursively(selectedFile);
        }
        efsTree.setRoot(createNode(EncryptedFileSystem.currentUser.getUserDir()));
        manageView();
        if(!result){
            showError("Error while deleting.");
            return;
        }
    }

    private boolean deleteDirRecursively(File dirToDelete) {
        File[] allContents = dirToDelete.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirRecursively(file);
            }
        }
        if(!dirToDelete.isDirectory())
            EncryptedFileSystem.deleteConnectedFiles(dirToDelete.getName());
        return dirToDelete.delete();
    }

    @FXML
    void downloadAFile(ActionEvent event) {
        if(selectedFile.isDirectory()){

            showError("Only a file can be downloaded.");
            return;
        }
        File source=selectedFile;
        File destination=null;
        //kopiramo file source na destinaciju gdje izabere

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose where to save file");
        File selectedDirectory = directoryChooser.showDialog(Main.window);

        if(selectedDirectory != null){
            destination=new File(selectedDirectory.getAbsolutePath()+File.separator+source.getName());

            //dekripcija, destination je umjesto temp
            Cryptography.decryptFileUserDir(source, destination);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText("You have successfully downloaded "+ source.getName());
            alert.showAndWait();
        }

    }

    @FXML
    void uploadAFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Word Files", "*.docx"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File fileToUpload = fileChooser.showOpenDialog(Main.window);
        if (fileToUpload != null) {
            //kopiraj na fajl sistem u direktorijum koji je izabran
            String newFile=fileToUpload.getName();

            File destination=null;
            //ako nije selektovao fajl onda u root
            if(!selectedFile.isDirectory())
                destination=new File(EncryptedFileSystem.currentUser.getUserDir().getPath()+File.separator+newFile);
            else
                destination=new File(selectedFile.getPath()+File.separator+newFile);
            try{
                Files.copy(fileToUpload.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Cryptography.encryptFile(destination, EncryptedFileSystem.currentUser.getUsername());
            }catch (Exception e){
                System.out.println("Greska pri upload-u.");
                return;
            }
            //azuriraj tree
            efsTree.setRoot(createNode(EncryptedFileSystem.currentUser.getUserDir()));
            manageView();
        }
    }

    @FXML
    void sendFile(ActionEvent event) {
        if(selectedFile==null || selectedFile.isDirectory()){
            showError("Select file for sending.");
            return;
        }
        String reciever=showRecieverChooser();
        if(reciever==null)return;

        //dekriptujemo selektovani fajl u fajl koji saljemo
        File sendingFile=new File(EncryptedFileSystem.sharedDir.getPath()+File.separator+"from#"+EncryptedFileSystem.currentUser.getUsername()+
                "#to#"+reciever+"#"+selectedFile.getName());
        Cryptography.decryptFileUserDir(selectedFile, sendingFile);
        Cryptography.encryptFile(sendingFile, reciever);

    }

    private String showRecieverChooser() {
        List<String> choices = new ArrayList<>();
        for(int i=0; i<EncryptedFileSystem.users.size(); i++){
            User u=EncryptedFileSystem.users.get(i);
            if(!u.getUsername().equals(EncryptedFileSystem.currentUser.getUsername()))
                choices.add(u.getUsername());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Choose reciever");
        dialog.setHeaderText(null);
        dialog.setContentText("Reciever:");

        Optional<String> result = dialog.showAndWait();

        if(result.isPresent())
            return result.get();
        else
            return null;
    }

    @FXML
    void showShared(ActionEvent event) {
        try {
            Main.window.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(SHARED_DIR_PAGE));
            Parent root = loader.load();
            Scene addTxtScene = new Scene(root);
            Main.window.setScene(addTxtScene);
            Main.window.setTitle("Shared directory");
            Main.window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void open(ActionEvent event) {
        if(selectedFile!=null && !selectedFile.isDirectory()){
            if(!EncryptedFileSystem.openFile(selectedFile)){
                showError("Something is wrong with selected file.");
            }
        }else{
            showError("Select file for opening.");
            return;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        efsTree.setEditable(true);
        efsTree.setRoot(createNode(EncryptedFileSystem.currentUser.getUserDir()));
        manageView();
    }

    private void manageView(){
        efsTree.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {

            public TreeCell<File> call(TreeView<File> tv) {
                return new TreeCell<File>() {

                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);

                        setText((empty || item == null) ? "" : item.getName());
                    }

                };
            }
        });
    }

    private TreeItem<File> createNode(final File f) {
        return new TreeItem<File>(f){
            private boolean isLeaf;

            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;

                    // First getChildren() call, so we actually go off and
                    // determine the children of the File contained in this TreeItem.
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    File f = (File) getValue();
                    isLeaf = f.isFile();
                }

                return isLeaf;
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
                File f = TreeItem.getValue();
                if (f != null && f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

                        for (File childFile : files) {
                            children.add(createNode(childFile));
                        }

                        return children;
                    }
                }

                return FXCollections.emptyObservableList();
            }

        };
    }

    @FXML
    public void mouseClick(javafx.scene.input.MouseEvent mouseEvent) {
        TreeItem<File> selected=efsTree.getSelectionModel().getSelectedItem();
        if(selected!=null)
             selectedFile=selected.getValue();

    }

    private void showError(String message){
       Alert alert = new Alert(Alert.AlertType.ERROR);
       alert.setTitle("Error Dialog");
       alert.setHeaderText(null);
       alert.setContentText(message);
       alert.showAndWait();
    }

}

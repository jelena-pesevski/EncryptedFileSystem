package controllers;

import classes.Cryptography;
import classes.EncryptedFileSystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class SharedDir implements Initializable {
    private static final String MAIN_PAGE="/view/MainPage.fxml";

    @FXML
    private TreeView<File> treeView;

    @FXML
    private Button downloadButton;

    @FXML
    private Button returnButton;

    private static File selectedFile;

    @FXML
    void download(ActionEvent event) {
        if(selectedFile.isDirectory())return;
        String infoString=selectedFile.getName();
        String[] info=infoString.split("#");
        String sender=info[1].trim();
        String reciever=info[3].trim();
        String fileName=info[4].trim();

        //prvo treba dekriptovati fajl prije provjere integriteta
        File destination=new File(EncryptedFileSystem.currentUser.getUserDir().getPath()+File.separator+fileName);

        //sada dekripcija fajla
        boolean isOk=Cryptography.decryptFileFromSharedDir(selectedFile, destination, sender);

        //ako je uspjesno preuzeto sada ga kriptujemo na nasem fajl sistemu
        if(!isOk){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText(null);
            alert.setContentText("This file can't be downloaded.");
            alert.showAndWait();
        }else {
            Cryptography.encryptFile(destination, EncryptedFileSystem.currentUser.getUsername());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText("You have successfully downloaded file which " + sender + " sent you.");
            alert.showAndWait();

            //na kraju brisanje fajla iz zajednickog direktorijuma
            deleteRecievedFile(selectedFile);

            //azuriramo tree view
            treeView.setRoot(createNode(EncryptedFileSystem.sharedDir));
            manageView();
        }
    }

    @FXML
    void returnMainPage(ActionEvent event) {
        goToMain();
    }

    private void goToMain(){
        //vracanje na formu
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

    private void deleteRecievedFile(File selectedFile) {
        String fileName=selectedFile.getName();
        selectedFile.delete();

        File f1=new File(EncryptedFileSystem.sharedDirInfo.getPath()+File.separator+"hash_"+fileName+".txt");
        f1.delete();

        File f2=new File(EncryptedFileSystem.sharedDirInfo.getPath()+File.separator+"encryptInfo_"+fileName+".txt");
        f2.delete();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        treeView.setEditable(true);
        treeView.setRoot(createNode(EncryptedFileSystem.sharedDir));
        manageView();
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

    private void manageView(){
        treeView.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {

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

    @FXML
    void mouseClick(MouseEvent event) {
        TreeItem<File> selected=treeView.getSelectionModel().getSelectedItem();
        if(selected!=null)
            selectedFile=selected.getValue();

    }
}

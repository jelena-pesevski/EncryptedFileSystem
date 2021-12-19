package classes;

import controllers.MainPageController;
import javafx.scene.control.Alert;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EncryptedFileSystem {
    public static File efsRoot=new File("./efsRoot");
    public static File sharedDir=new File("./efsRoot/sharedDir");
    public static File encryptInfo= new File("./encryptInfo");
    public static File sharedDirInfo= new File("./sharedDirInfo");
    public static File certs= new File("./certificates");


    private static String temp="./temp/tempFile.";
    private static String crlListPath="./caDir/lista.crl";
    private static String caCert="./caDir/cacert.pem";


    public static File getEfsRoot() {
        return efsRoot;
    }

    public static ArrayList<User> users;

    public static User currentUser;


    public static boolean login(String username, String password, File selectedCert){
        for(User user : users){

            if(username.equals(user.getUsername())){
                if(user.checkPassword(password) && checkIfRightCertificate(selectedCert, username)){
                    currentUser=user;
                    return true;
                }else break;
            }
        }
        return false;
    }


    public static void loadUsers(){
        File dataFile=new File("."+File.separator+"data");
        if(!dataFile.exists()){
            users=new ArrayList<>();
            return;
        }
        try
        {
            FileInputStream fis = new FileInputStream("data");
            ObjectInputStream ois = new ObjectInputStream(fis);

            users = (ArrayList) ois.readObject();

            ois.close();
            fis.close();
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();

            return;
        }
    }

    public static void saveUsers(){
        try
        {
            FileOutputStream fos = new FileOutputStream("data");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);
            oos.close();
            fos.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }

    }


    public static boolean trySigningUp(String username, String password, File selectedCert){
        //postoji korisnik sa tim imenom
        if(users.stream().filter(u->u.getUsername().equals(username)).collect(Collectors.toList()).size()!=0)return false;

        //ako ne postoji sa tim imenom provjeri sert da li je dobar
        File userCert=setUserCert(selectedCert, username);
        if(userCert==null) return false;

        //ako je sve dobro kreiraj korisnika
        User newUser=new User(username, password, userCert);
        users.add(newUser);

        try{
            File newUserFile = new File(efsRoot.getPath()+File.separator+username);
            if(!newUserFile.exists())
                newUserFile.mkdir();
            newUser.setUserDir(newUserFile);

            //kreiramo folder za sesijske kljuceve ovog usera
            File newEncryptInfo=new File(encryptInfo+File.separator+username);
            if(!newEncryptInfo.exists())
                newEncryptInfo.mkdir();
            newUser.setEncryptInfoDir(newEncryptInfo);

        }catch (Exception e){
            e.printStackTrace();
        }
        currentUser=newUser;
        return true;
    }

    public static void createTxt(String filename, String content){
        File newTxt= new File(MainPageController.getSelectedFile().getPath()+File.separator+filename+".txt");
        // if file doesnt exists, then create it
        try{
            if (!newTxt.exists()) {
                newTxt.createNewFile();
            }
        }catch (Exception e){
           // e.printStackTrace();
            System.out.println("Greska kreiranje fajla.");
            return;
        }
        try{
            //upisujemo nekriptovano u fajl
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(newTxt, false)));
            out.println(content);
            out.close();

            //kriptovanje
            Cryptography.encryptFile(newTxt, currentUser.getUsername());
        }
        catch (Exception e){
          //  e.printStackTrace();
        }

    }

    //provjera da li je validan sertifikat
    public static boolean checkCertificate(File certFile){
        try{
            //provjera keyUsage
            if(!Cryptography.checkKeyUsage(certFile, Cryptography.DIGITAL_SIGNATURE) || !Cryptography.checkKeyUsage(certFile,Cryptography.DATA_ENCIPHERMENT))
                return false;

            FileInputStream fis = new FileInputStream(certFile.getPath());
            CertificateFactory f = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate)f.generateCertificate(fis);

            //provjera validnost sertifikata
            certificate.checkValidity();

            //provjera da li ga je izdao cacert.pem
            FileInputStream fisCA = new FileInputStream(caCert);
            CertificateFactory fCA = CertificateFactory.getInstance("X.509");
            X509Certificate certificateCA = (X509Certificate)fCA.generateCertificate(fisCA);
            PublicKey publicKeyCA = certificateCA.getPublicKey();

            certificate.verify(publicKeyCA);

            //provjera da li se nalazi na crl listi
            InputStream inStream = new FileInputStream(crlListPath);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509CRL crl = (X509CRL)cf.generateCRL(inStream);

            //X509CRL crlList;
            if (crl.isRevoked(certificate)) {
                throw new CertificateException("Certificate revoked");
            }

            //sve je dobro
            return true;
        }catch (Exception e){
            System.out.println(e.getMessage());
          //  e.printStackTrace();
            return false;
        }
    }

    //kad se prvi put loguje ja zapamtim koji je njegov sertifikat i smjestim ga u
    //folder certificates da bi ga drugi koristili
    //i onda svaki out poredim sa njegovim
    public static File setUserCert(File selectedCert, String username){
        if(!checkCertificate(selectedCert))return null;

        Path destination = Paths.get(certs+File.separator+username+".crt");

        try{
            Files.copy(Paths.get(selectedCert.getPath()), destination, StandardCopyOption.REPLACE_EXISTING);
            return new File(destination.toString());
        }
        catch(Exception ex){
            return null;
        }

    }

    //poziva se svaki naredni put kada se korisnik loguje, da provjeri da li je to njegov
    //potencijalno samo provjera getName
    public static boolean checkIfRightCertificate(File selectedCert, String username) {
        try{
            File realCert=new File(certs+File.separator+username+".crt");

            byte[] f1 = Files.readAllBytes(Paths.get(selectedCert.getPath()));
            byte[] f2 = Files.readAllBytes(Paths.get(realCert.getPath()));
            if(Arrays.equals(f1, f2))
                return checkCertificate(selectedCert);
            //else nije izabrao svoj sertifikat kojim se prvi put prijavio
            else return false;
        }
        catch(Exception ex){
            return false;
        }

    }

    //provjera okruzenja
    public static void checkEnvironment() {
        if(!encryptInfo.exists()){
            encryptInfo.mkdir();
        }
        if(!efsRoot.exists()){
            efsRoot.mkdir();
        }
        if(!sharedDir.exists()){
            sharedDir.mkdir();
        }
        if(!sharedDirInfo.exists()){
            sharedDirInfo.mkdir();
        }
    }

    //obrisi i hash_ in encryptInfo_ fajlove za fajl koji se brise
    public static void deleteConnectedFiles(String fileName){
        File f1=new File(currentUser.getEncryptInfoDir().getPath()+File.separator+"hash_"+fileName+".txt");
        f1.delete();

        File f2=new File(currentUser.getEncryptInfoDir().getPath()+File.separator+"encryptInfo_"+fileName+".txt");
        f2.delete();

    }

    public static boolean openFile(File file){
        //fajl dekriptujemo u temp fajl i njega otvaramo
        String extension=null;
        if(file.getName().endsWith(".txt"))extension="txt";
        else if(file.getName().endsWith(".docx"))extension="docx";
        else if(file.getName().endsWith(".jpeg"))extension="jpeg";
        else if(file.getName().endsWith(".jpg"))extension="jpg";
        else if(file.getName().endsWith(".png"))extension="png";
        else if(file.getName().endsWith(".pdf"))extension="pdf";

        File tempFile=new File(temp+extension);

        byte[] bytes=Cryptography.decryptFileUserDir(file, tempFile);
        //nije dobro dekriptovano
        if(bytes==null)return false;

        Desktop desktop=Desktop.getDesktop();
        try{
            desktop.open(tempFile);

        }catch(Exception e){
            e.printStackTrace();
        }

        return true;
    }

}

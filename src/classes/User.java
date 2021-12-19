package classes;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class User implements Serializable {
    private String username;
    private byte[] password;
    private File userDir;
    private File certificate;
    private File encryptInfoDir;

    public File getUserDir() {
        return userDir;
    }

    public void setUserDir(File userDir) {
        this.userDir = userDir;
    }

    public void setEncryptInfoDir(File encryptInfoDir) {
        this.encryptInfoDir = encryptInfoDir;
    }

    public File getEncryptInfoDir() {
        return encryptInfoDir;
    }

    public User(String username, String password, File certificate){
        this.username=username;
        this.certificate=certificate;

        //ovdje cemo sada da sacuvamo hash otisak lozinke
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(password.getBytes());
        this.password=md.digest();
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(password.getBytes());
        if(Arrays.equals(md.digest(), this.password))return true;
        else
            return false;

    }

    public File getCertificate() {
        return certificate;
    }
}

package classes;

import javafx.scene.shape.Path;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;

public class Cryptography {
    public static final int DATA_ENCIPHERMENT=3;
    public static final int DIGITAL_SIGNATURE=0;

    //u ovoj metodi biramo algoritam, i pripremamo podatke za kriptovanje
    //zatim pozivamo encryption, te nakon toga pravimo dodatni fajl sa informacijama
    //o algoritmu i kljucu za kriptovanje, te ga potpisujemo javnim kljucem onoga cija je datoteka
    public static void encryptFile(File fileForEncryption, String reciever){

        //moguci algoritmi des
        //aes128 i DESede(des3)
        String algName="AES/CBC/PKCS5Padding";
        String algKey="AES";
        //iv se koristi da se smanji mogucnost pronalaska plain teksta
        int IVlength=16;
        int keySize=16;
        String encodedKey="abcdefghijklmop";

        Random random=new Random();
        int num=random.nextInt(3);

        try{
            if(num==0){
                algName = "AES/CBC/PKCS5Padding";
                algKey = "AES";
                IVlength = 16;
                keySize = 16;

                KeyGenerator keyGen = KeyGenerator.getInstance(algKey);
                keyGen.init(128);
                SecretKey secretKey = keyGen.generateKey();
                encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            }else if(num==2){
                algName = "DES/CBC/PKCS5Padding";
                algKey = "DES";
                IVlength = 8;
                keySize = 8;

                KeyGenerator keyGen = KeyGenerator.getInstance(algKey);
                keyGen.init(56);
                SecretKey secretKey = keyGen.generateKey();
                encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            }else{
                algName = "DESede/CBC/PKCS5Padding";
                algKey = "DESede";
                IVlength = 8;
                keySize = 24;

                KeyGenerator keyGen = KeyGenerator.getInstance(algKey);
                keyGen.init(168);
                SecretKey secretKey = keyGen.generateKey();
                encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            }
        }catch(Exception e){
            System.out.println("Greska kod generisanja kljuca.");
        }

        File hashFile=null;
        try{
            //kreiraj hash_dokument
            //lokaciju hash fajla razlikujemo u zavisnosti od toga da li je fajl u
            //direktorijumu trenutnog korisnika, ili se salje drugom korisniku
            hashFile=signFile(fileForEncryption, reciever);

            //enkriptuj fajl
            encryption(fileForEncryption, fileForEncryption, encodedKey, algName, algKey, IVlength, keySize);

            //enkriptuj hashFile istim kljucem
            encryption(hashFile, hashFile, encodedKey, algName, algKey, IVlength, keySize);

        }catch (Exception e){
            System.out.println("Greska enkripcija.");
            return;
        }

        //sada smo sacuvali info o hashu i enkriptovan fajl
        //sada sacuvati info o algoritmu

        String info="algoritam#"+algName+"#algKey#"+algKey+"#key#"+encodedKey+"#";
        File destInfoFile=null;

        //razlikujemo slucaj kada je samo ocuvanje integriteta i kada je slanje
        //iskoristicemo funkciju publicKeyEncryption, ali prije toga moramo kreirati nekriptovano
        if(EncryptedFileSystem.currentUser.getUsername().equals(reciever)){
            //dest je ecnrypt info
            destInfoFile=new File(EncryptedFileSystem.currentUser.getEncryptInfoDir().getPath()+File.separator+"encryptInfo_"+fileForEncryption.getName()+".txt");
        }else{
            destInfoFile=new File(EncryptedFileSystem.sharedDirInfo.getPath()+File.separator+"encryptInfo_"+fileForEncryption.getName()+".txt");
        }

        //sada upisemo info u destInfoFile
        try{
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(destInfoFile, false)));
            out.println(info);
            out.close();
        }catch(Exception e){
            e.printStackTrace();
            return;
        }

        //sada enkripcija javnim kljucem primaoca
        try{
            publicKeyEncryption(destInfoFile, reciever);
        }catch (Exception e){
            //e.printStackTrace();
            System.out.println("Greska enkripcija javnim kljucem.");
        }
    }

    //za dekripciju na svom
    public static byte[] decryptFileUserDir(File fileForDecryption, File tempFile){
        File infoFile=new File(EncryptedFileSystem.currentUser.getEncryptInfoDir().getPath()+File.separator+"encryptInfo_"+fileForDecryption.getName()+".txt");
        String encryptInfo=null;
        try{
            encryptInfo=privateKeyDecryption(infoFile);
        }catch (Exception e){
            System.out.println("Private key decryption error");
        }

        String[] info=encryptInfo.split("#");
        //"algoritam#"+algName+"#algKey#"+algKey+"#key#"+encodedKey+"#";
        String algName=info[1].trim();
        String algKey=info[3].trim();
        String encodedKey=info[5].trim();

        int IVlength=0, keySize=0;
        if("DESede".equals(algKey)){
            IVlength=8;
            keySize=24;
        }else if("DES".equals(algKey)){
            IVlength = 8;
            keySize = 8;
        }else{
            IVlength = 16;
            keySize = 16;
        }

        //dekriptujemo sadrzaj
        byte[] decryptedContent=null;
        try{
            decryptedContent=decryption(fileForDecryption, encodedKey,algName, algKey, IVlength, keySize);
        }catch(Exception e){
            System.out.println("Dekripcija problem");
        }

        //dekriptujemo fajl sa info o potpisu
        File hashFile=new File(EncryptedFileSystem.currentUser.getEncryptInfoDir().getPath()+File.separator+"hash_"+fileForDecryption.getName()+".txt");
        byte[] signatureInfo=null;
        try{
            signatureInfo=decryption(hashFile, encodedKey,algName, algKey, IVlength, keySize );
        }catch(Exception e){
            System.out.println("Dekripcija hash file problem");
        }

        String hashFileContent=new String(signatureInfo);


        //verifikovacemo bajtove koje smo dekriptovali
        boolean good=checkIntegrity(decryptedContent, hashFileContent);
        if(!good){
            System.out.println("Integritet narusen "+fileForDecryption.getName());
            return null;
        }

        //verifikovali smo da integritet nije narusen
        //sada upisemo dekriptovani sadrzaj u tempFile
        try{
            FileOutputStream fos=new FileOutputStream(tempFile.getPath());
            fos.write(decryptedContent);
            fos.close();
        }catch (Exception e){
            System.out.println("Greska pri upisu u temp.");
        }

        return decryptedContent;
    }

    //za dekripciju primljenog
    public static boolean decryptFileFromSharedDir(File fileForDecryption, File destinationFile, String sender){
        File infoFile=new File(EncryptedFileSystem.sharedDirInfo.getPath()+File.separator+"encryptInfo_"+fileForDecryption.getName()+".txt");
        String encryptInfo=null;

        try{
            encryptInfo=privateKeyDecryption(infoFile);
        }catch (Exception e){
            System.out.println("Private key decryption error");
            return false;
        }

        String[] info=encryptInfo.split("#");
        //"algoritam#"+algName+"#algKey#"+algKey+"#key#"+encodedKey+"#";
        String algName=info[1].trim();
        String algKey=info[3].trim();
        String encodedKey=info[5].trim();

        int IVlength=0, keySize=0;
        if("DESede".equals(algKey)){
            IVlength=8;
            keySize=24;
        }else if("DES".equals(algKey)){
            IVlength = 8;
            keySize = 8;
        }else{
            IVlength = 16;
            keySize = 16;
        }

        //dekriptujemo sadrzaj
        byte[] decryptedContent=null;
        try{
            decryptedContent=decryption(fileForDecryption, encodedKey,algName, algKey, IVlength, keySize);
        }catch(Exception e){
            System.out.println("Dekripcija problem");
            return false;
        }


        //dekriptujemo file sa info o potpisu
        File hashFile=new File(EncryptedFileSystem.sharedDirInfo.getPath()+File.separator+"hash_"+fileForDecryption.getName()+".txt");
        byte[] signatureInfo=null;
        try{
            signatureInfo=decryption(hashFile, encodedKey,algName, algKey, IVlength, keySize );
        }catch(Exception e){
            System.out.println("Dekripcija hash file problem");
            return false;
        }

        //verifikacija potpisa
        String hashFileContent=new String(signatureInfo);
        boolean good=verifySignature(decryptedContent, hashFileContent, sender);
        if(!good){
            System.out.println("Integritet narusen "+fileForDecryption.getName());
            return false;
        }

        //verifikovali smo da je integritet nije narusen
        //sada upisemo dekriptovani sadrzaj u tempFile
        try{
            FileOutputStream fos=new FileOutputStream(destinationFile.getPath());
            fos.write(decryptedContent);
            fos.close();
        }catch (Exception e){
            System.out.println("Greska pri upisu u temp.");
            return false;
        }

        return true;
    }

    public static byte[] encryption(File fileForEncryption, File encryptedFile, String key, String algName, String algKey, int IVlength, int keySize){
        try{
            byte[] fileBytes=Files.readAllBytes(fileForEncryption.toPath());

            //generisemo IV
            byte[] iv=new byte[IVlength];
            SecureRandom random=new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec= new IvParameterSpec(iv);

            //hesiramo kljuc
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            digest.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes=new byte[keySize];
            System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
            SecretKeySpec secretKeySpec=new SecretKeySpec(keyBytes, algKey);

            //enkripcija
            Cipher cipher=Cipher.getInstance(algName);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encrypted=cipher.doFinal(fileBytes);

            //kombinujemo IV i enkriptovani tekst
            byte[] encryptedIVAndText = new byte[IVlength + encrypted.length];
            System.arraycopy(iv, 0, encryptedIVAndText, 0, IVlength);
            System.arraycopy(encrypted, 0, encryptedIVAndText, IVlength, encrypted.length);

            OutputStream outputStream = new FileOutputStream(encryptedFile.getPath());
            outputStream.write(encryptedIVAndText);
            outputStream.close();
            return encryptedIVAndText;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public static byte[] decryption(File encryptedFile, String key, String algName, String algKey, int IVlength, int keySize){
        try{
            byte[] encryptedIvTextBytes = Files.readAllBytes(Paths.get(encryptedFile.getPath().toString()));

            //extract iv
            byte[] iv = new byte[IVlength];
            System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            //ektraktujemo enkriptovani tekts
            int encryptedSize = encryptedIvTextBytes.length - IVlength;
            byte[] encryptedBytes = new byte[encryptedSize];
            System.arraycopy(encryptedIvTextBytes, IVlength, encryptedBytes, 0, encryptedSize);

            // Hash key
            byte[] keyBytes = new byte[keySize];
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(key.getBytes());
            System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);

            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, algKey);

            // dekriptujemo
            Cipher cipherDecrypt = Cipher.getInstance(algName);
            cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

            return decrypted;

        }catch(Exception e){
            //  System.out.println("Losa dekripcija.");

            return null;
        }
    }

    public static void publicKeyEncryption(File fileToEncrypt, String reciever){
        try{
            String certPath=EncryptedFileSystem.certs.getPath()+File.separator+reciever+".crt";

            //uzimamo sertifikat od recievera
            //reciever moze biti i trenutni korisnik
            FileInputStream fis=new FileInputStream(certPath);
            CertificateFactory f=CertificateFactory.getInstance("X.509");
            X509Certificate certificate=(X509Certificate) f.generateCertificate(fis);
            PublicKey publicKey=certificate.getPublicKey();

            //koristicemo sertifikat pa provjera da li je validan
            if(EncryptedFileSystem.checkCertificate(new File(certPath))){
                Cipher cipher=Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] input=Files.readAllBytes(Paths.get(fileToEncrypt.getPath()));
                byte[] encryptedInput= cipher.doFinal(input);

                FileOutputStream fos = new FileOutputStream(fileToEncrypt);
                fos.write(encryptedInput);
                fos.close();
            }else{
                System.out.println("Sertifikat nije validan, ne moze se iskoristiti javni kljuc");
            }

        }catch (Exception e){
            System.out.println("Greska pri potpisivanju javnim kljucem.");
        }
    }

    //uvijek dekriptujemo privatnim kljucem trenutnog korisnika
    public static String privateKeyDecryption(File encryptedFile) throws Exception{
        //dekriptujemo privatnim kljucem iz ks
        KeyStore ks = KeyStore.getInstance("Windows-MY",  "SunMSCAPI");
        ks.load(null, null) ;
        PrivateKey privateKey=(PrivateKey) ks.getKey(EncryptedFileSystem.currentUser.getUsername(), null );

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedBytes= Files.readAllBytes(Paths.get(encryptedFile.getPath()));
        byte[] decryptedBytes=cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);

    }

    //ovu metodu cemo dvostruko koristiti, jedan slucaj je za ocuvanje integriteta
    //sopstvenih fajlova
    public static File signFile(File fileForSigning, String reciever) throws Exception{
        //prvo da odlucimo koji algoritam koristimo za hesiranje
        String shaAlg="";
        Random rand=new Random();

        int num=rand.nextInt(3);

        switch(num){
            case 0:
                shaAlg="SHA256withRSA";
                break;
            case 1:
                shaAlg="SHA384withRSA";
                break;
            case 2:
                shaAlg="SHA512withRSA";
                break;
        }

        //sada treba dohvatiti privatni kljuc iz keystora
        //jer fajl potpisujemo privatnim kljucem trenutnog korisnika
        KeyStore ks = KeyStore.getInstance("Windows-MY",  "SunMSCAPI");
        ks.load(null, null) ;
        Certificate c = ks.getCertificate(EncryptedFileSystem.currentUser.getUsername()) ;
        X509Certificate x509 = (X509Certificate) c;

        PrivateKey privateKey=(PrivateKey) ks.getKey(EncryptedFileSystem.currentUser.getUsername(), null );

        Signature signature=Signature.getInstance(shaAlg);
        signature.initSign(privateKey);

        byte[] fileBytes= Files.readAllBytes(fileForSigning.toPath());
        signature.update(fileBytes);
        byte[] digitalSignature= signature.sign();

        //koji algoritam je koristen i sami bajtovi potpisa
        String info="algoritam#"+shaAlg+"#potpis#"+ Base64.getEncoder().encodeToString(digitalSignature);

        //sada pravimo hash_ fajl
        //i taj fajl vracamo
        return createHashFile(fileForSigning, info, reciever);
    }

    private static File createHashFile(File fileForSigning, String content, String reciever) throws Exception{
        String destDir=null;
        File file=null;
        if(EncryptedFileSystem.currentUser.getUsername().equals(reciever)){
            //rijec je samo o fajlu za tog korisnika da sacuva integritet
            destDir=EncryptedFileSystem.encryptInfo.getPath()+File.separator+EncryptedFileSystem.currentUser.getUsername();
            file=new File(destDir+File.separator+"hash_"+fileForSigning.getName()+".txt");
        }else{
            destDir=EncryptedFileSystem.sharedDirInfo.getPath();
            file=new File(destDir+File.separator+"hash_"+fileForSigning.getName()+".txt");
        }
        FileWriter fw=new FileWriter(file);
        fw.write(content);
        fw.close();

        return file;

    }

    //za provjeru integriteta, isto je kad neko drugi posalje koristimo njegov kljuc, a kad mi onda nas
    public static boolean verifySignature(byte[] content, String hashFileContent, String sender){
        String[] hashInfo=hashFileContent.split("#");
        //"algoritam#"+shaAlg+"#potpis#"+ Base64.getEncoder().encodeToString(digitalSignature)

        File senderCertificate=new File(EncryptedFileSystem.certs+File.separator+sender+".crt");

        try{
            FileInputStream fin = new FileInputStream(senderCertificate.getPath());
            CertificateFactory f = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate)f.generateCertificate(fin);
            //javni kljuc kojim se provjerava potpis
            PublicKey publicKey = certificate.getPublicKey();

            Signature signature = Signature.getInstance(hashInfo[1]);
            signature.initVerify(publicKey);
            signature.update(content);

            String recievedSignature=hashInfo[3];
            byte[] signatureBytes=Base64.getDecoder().decode(recievedSignature.trim());

            return signature.verify(signatureBytes);

        }catch (Exception e){
           // e.printStackTrace();
            System.out.println("Verifikacija nije prosla.");
            return false;
        }

    }

    public static boolean checkIntegrity(byte[] content, String hashFileContent){
        String[] hashInfo=hashFileContent.split("#");
        //"algoritam#"+shaAlg+"#potpis#"+ Base64.getEncoder().encodeToString(digitalSignature)

        //sertifikat za verifikaciju nije validan
        // if(!EncryptedFileSystem.checkCertificate(EncryptedFileSystem.currentUser.getCertificate()))return false;

        try{

            FileInputStream fin = new FileInputStream(EncryptedFileSystem.currentUser.getCertificate().getPath());
            CertificateFactory f = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate)f.generateCertificate(fin);
            //javni kljuc kojim se provjerava potpis
            PublicKey publicKey = certificate.getPublicKey();

            Signature signature = Signature.getInstance(hashInfo[1]);
            signature.initVerify(publicKey);
            signature.update(content);

            String recievedSignature=hashInfo[3];
            byte[] signatureBytes=Base64.getDecoder().decode(recievedSignature.trim());

            return signature.verify(signatureBytes);

        }catch (Exception e){
         //   e.printStackTrace();
            System.out.println("Verifikacija nije prosla.");
            return false;
        }

    }

    public static boolean checkKeyUsage(File certificate, int requiredKeyUsage){
            /*
            KeyUsage ::= BIT STRING {
            digitalSignature        (0),
            nonRepudiation          (1),
            keyEncipherment         (2),
            dataEncipherment        (3),
            keyAgreement            (4),
            keyCertSign             (5),
            cRLSign                 (6),
            encipherOnly            (7),
            decipherOnly            (8) }
            */

        try{
            FileInputStream fis=new FileInputStream(certificate.getPath());
            CertificateFactory f = CertificateFactory.getInstance("X.509");
            X509Certificate cert=(X509Certificate) f.generateCertificate(fis);

            boolean[] keyUsage=cert.getKeyUsage();

            if(keyUsage[requiredKeyUsage])
                return true;
            else
                return false;
        }catch (Exception e){
            //  e.printStackTrace();
            return false;
        }
        // return true;

    }


}

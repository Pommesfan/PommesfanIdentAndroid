package com.example.controller;

import com.example.model.Personal_ID;
import com.example.model.PrivateProfile;
import com.example.model.PublicProfile;
import com.example.utils.OutputEvent;
import com.example.utils.Utils;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Observable;

public class Controller extends Observable {
    public static Controller controller = null;
    public final String appDataLocation;
    public Controller(String appDataLocation) {
        this.appDataLocation = appDataLocation;
    }

    public void generateKeyPair(String profileName, String[] dynamicAttributes) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator gpk = KeyPairGenerator.getInstance("RSA");
        gpk.initialize(2048);
        KeyPair keyPair = gpk.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        PrivateProfile privateProfile = new PrivateProfile(
                profileName, dynamicAttributes, publicKey.getEncoded(), privateKey.getEncoded());
        privateProfile.saveInternal(appDataLocation + "MyPublicProfiles/" + profileName);
    }

    private byte[] sign_id(byte[] personalIdB, PrivateProfile privateProfile) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateProfile.privateKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        //sign message
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyFactory.generatePrivate(spec));
        signature.update(personalIdB);
        return signature.sign();
    }

    public void generateID(String publicProfileName, String name, String surname, Date date, String address, String[] dynamicAttributeValues, File personalPicture, File handSignature) throws Exception {
        //load public profile
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                appDataLocation + "MyPublicProfiles/", publicProfileName);
        if(privateProfile.dynamicAttributes.length != dynamicAttributeValues.length) {
            throw new Exception("Anzahl dynamischer Attribute unpassend");
        }

        String ID_number = Utils.getAlphanumeric(8);
        // get personal image
        String personalPictureFileName = personalPicture.getName();
        String internalPath1 = appDataLocation + "PersonalImages/" + personalPictureFileName;
        File imageDir1 = new File(appDataLocation + "PersonalImages/");
        imageDir1.mkdirs();
        Files.copy(Paths.get(personalPicture.toURI()), Paths.get(internalPath1));
        // get hand signature
        String handSignatureFileName = personalPicture.getName();
        String internalPath2 = appDataLocation + "HandSignatures/" + personalPictureFileName;
        File imageDir2 = new File(appDataLocation + "HandSignatures/");
        imageDir2.mkdirs();
        Files.copy(Paths.get(handSignature.toURI()), Paths.get(internalPath2));

        Personal_ID personalId = new Personal_ID(ID_number, privateProfile.name, name, surname, date, address,
                privateProfile.dynamicAttributes, dynamicAttributeValues, personalPictureFileName, handSignatureFileName);
        byte[] personalId_b = personalId.toByte(true);

        //Create signature
        byte[] personalId_with_personal_image_b = Utils.concat_bytes(
                personalId_b, Files.readAllBytes(personalPicture.toPath()), Files.readAllBytes(handSignature.toPath()));
        byte[] signature_b = sign_id(personalId_with_personal_image_b, privateProfile);

        String distPath = appDataLocation + "CreatedPersonalIDs/" + ID_number;

        //Save ID
        File f = Utils.createFileAndSubfolder(distPath);
        FileOutputStream fos = new FileOutputStream(f);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> fos.write(data));
        sliceWriter.write(personalId_b);
        sliceWriter.write(signature_b);
        fos.close();
    }

    private boolean validateSignature(byte[] personal_id_b, byte[] publicKey, byte[] signature_b) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(keyFactory.generatePublic(spec));
        publicSignature.update(personal_id_b);
        return publicSignature.verify(signature_b);
    }

    public void checkPersonalID(String id_number) throws Exception {
        String distPath = appDataLocation + "ImportedPersonalIDs/" + id_number.toUpperCase();

        //load personal id
        File f = new File(distPath);
        FileInputStream fis = new FileInputStream(f);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        byte[] personal_id_b = sliceReader.next();
        byte[] signature_b = sliceReader.next();
        fis.close();
        String[] personal_id_s = Utils.bytesToStringArray(personal_id_b);

        // load public profile
        PublicProfile publicProfile = PublicProfile.loadInternal(appDataLocation + "ImportedPublicProfiles/", personal_id_s[1]);

        Personal_ID personalId = new Personal_ID(personal_id_s, publicProfile.dynamicAttributes);
        String personalImage = appDataLocation + "PersonalImages/" + personalId.personalImagePath;
        byte[] personalImage_b = Files.readAllBytes(Paths.get(personalImage));
        String handSignature = appDataLocation + "HandSignatures/" + personalId.personalImagePath;
        byte[] handSignature_b = Files.readAllBytes(Paths.get(handSignature));

        setChanged();
        if (validateSignature(Utils.concat_bytes(personal_id_b, personalImage_b, handSignature_b), publicProfile.publicKey, signature_b)) {
            notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
        } else {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
        }
    }

    public void exportPublicProfile(String profileName, File destination) throws IOException {
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(appDataLocation + "MyPublicProfiles/", profileName);
        PublicProfile publicProfile = new PublicProfile(privateProfile.name, privateProfile.dynamicAttributes, privateProfile.publicKey);
        publicProfile.saveExternal(destination);
    }

    public void importPublicProfile(File publicProfileFile) throws IOException {
        PublicProfile publicProfile = PublicProfile.fromExternal(publicProfileFile);
        publicProfile.saveInternal(appDataLocation + "ImportedPublicProfiles/");
    }

    public void exportPersonalID(String personal_id, File destination) throws Exception {
        String distPath = appDataLocation + "CreatedPersonalIDs/" + personal_id.toUpperCase();

        //load personal id
        File f = new File(distPath);
        FileInputStream fis = new FileInputStream(f);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        byte[] personal_id_b = sliceReader.next();
        byte[] signature_b = sliceReader.next();
        fis.close();

        String[] personal_id_s = Utils.bytesToStringArray(personal_id_b);
        PublicProfile publicProfile = PublicProfile.loadInternal(appDataLocation + "MyPublicProfiles/", personal_id_s[1]);

        Personal_ID personalId = new Personal_ID(personal_id_s, publicProfile.dynamicAttributes);
        // load personal image
        byte[] personalImage_b = Files.readAllBytes(Paths.get(appDataLocation + "PersonalImages/" + personalId.personalImagePath));
        // load hand signature
        byte[] handSignature_b = Files.readAllBytes(Paths.get(appDataLocation + "HandSignatures/" + personalId.handSignaturePath));

        FileOutputStream fos = new FileOutputStream(destination);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> fos.write(data));
        sliceWriter.write(personal_id_b);
        sliceWriter.write(signature_b);
        sliceWriter.write(personalImage_b);
        sliceWriter.write(handSignature_b);
        fos.close();
    }

    public void importPersonalID(File source) throws Exception {
        FileInputStream fis = new FileInputStream(source);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        // read personal id
        byte[] personal_id_b = sliceReader.next();
        // read signature
        byte[] signature_b = sliceReader.next();
        // read personal image and hand signature
        byte[] personalImage_b = sliceReader.next();
        byte[] handSignature_b = sliceReader.next();
        fis.close();
        String[] personal_id_s = Utils.bytesToStringArray(personal_id_b);

        PublicProfile publicProfile = PublicProfile.loadInternal(appDataLocation + "ImportedPublicProfiles/", personal_id_s[1]);

        Personal_ID personalId = new Personal_ID(personal_id_s, publicProfile.dynamicAttributes);
        // extract id number and image name
        String id_number = personalId.ID_number;
        String imageName = personalId.personalImagePath;
        String handSignatureName = personalId.handSignaturePath;

        if(!validateSignature(
                Utils.concat_bytes(personal_id_b, personalImage_b, handSignature_b),
                publicProfile.publicKey, signature_b)) {
            setChanged();
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            return;
        }

        // save imported data
        String id_path = appDataLocation +  "ImportedPersonalIDs/" + id_number;
        File f_personal_id = Utils.createFileAndSubfolder(id_path);
        FileOutputStream fos1 = new FileOutputStream(f_personal_id);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> fos1.write(data));
        sliceWriter.write(personal_id_b);
        sliceWriter.write(signature_b);
        fos1.close();


        File f_personal_image = Utils.createFileAndSubfolder(appDataLocation + "PersonalImages/" + imageName);
        FileOutputStream fos2 = new FileOutputStream(f_personal_image);
        fos2.write(personalImage_b);
        fos2.close();

        File f_hand_signature = Utils.createFileAndSubfolder(appDataLocation + "HandSignatures/" + handSignatureName);
        FileOutputStream fos3 = new FileOutputStream(f_hand_signature);
        fos3.write(handSignature_b);
        fos3.close();
    }

    public void checkPersonalIDFromRemote() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        ServerSocket serverSocket = new ServerSocket(0);
        setChanged();
        notifyObservers(new OutputEvent.ServerStartedEvent(ip, serverSocket.getLocalPort()));
        Socket s = serverSocket.accept();
        InputStream inputStream = new BufferedInputStream(s.getInputStream());

        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> inputStream.read(data, 0, length));
        byte[] personal_id_b = sliceReader.next();
        byte[] personal_image_b = sliceReader.next();
        byte[] handSignature_b = sliceReader.next();
        byte[] signature_b = sliceReader.next();
        inputStream.close();
        serverSocket.close();
        String[] personal_id_s = Utils.bytesToStringArray(personal_id_b);

        PublicProfile publicProfile = PublicProfile.loadInternal(appDataLocation + "ImportedPublicProfiles/", personal_id_s[1]);
        setChanged();
        if (validateSignature(Utils.concat_bytes(personal_id_b, personal_image_b, handSignature_b), publicProfile.publicKey, signature_b)) {
            Personal_ID personalId = new Personal_ID(personal_id_s, publicProfile.dynamicAttributes);
            notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
        } else {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
        }
    }

    public void handInPersonalIDtoRemote(String id_number, String ip, int port) throws Exception {
        Socket s = new Socket(ip, port);
        //load personal id
        String distPath = appDataLocation + "ImportedPersonalIDs/" + id_number.toUpperCase();
        FileInputStream fis = new FileInputStream(distPath);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        byte[] personal_id_b = sliceReader.next();
        byte[] signature_b = sliceReader.next();
        // load personal image
        String[] personal_id_s = Utils.bytesToStringArray(personal_id_b);

        // load public profile
        PublicProfile publicProfile = PublicProfile.loadInternal(appDataLocation + "ImportedPublicProfiles/", personal_id_s[1]);
        Personal_ID personalId = new Personal_ID(personal_id_s, publicProfile.dynamicAttributes);

        byte[] personalImage_b = Files.readAllBytes(Paths.get(appDataLocation + "PersonalImages/" + personalId.personalImagePath));
        byte[] handSignature_b = Files.readAllBytes(Paths.get(appDataLocation + "HandSignatures/" + personalId.handSignaturePath));
        fis.close();
        OutputStream outputStream = new BufferedOutputStream(s.getOutputStream());

        //hand in
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> outputStream.write(data));
        sliceWriter.write(personal_id_b);
        sliceWriter.write(personalImage_b);
        sliceWriter.write(handSignature_b);
        sliceWriter.write(signature_b);
        outputStream.close();
    }
}

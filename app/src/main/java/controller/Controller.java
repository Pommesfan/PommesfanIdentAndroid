package controller;

import model.Personal_ID;
import model.PrivateProfile;
import model.PublicProfile;
import utils.Observable;
import utils.OutputEvent;
import utils.Utils;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Controller extends Observable<OutputEvent> {
    public static final int LOAD_FROM_CREATED = 1;
    public static final int LOAD_FROM_IMPORTED = 2;
    public static final String strCreatedProfiles = "CreatedProfiles/";
    public static final String strImportedPublicProfiles = "ImportedPublicProfiles/";
    public static final String strPersonalImages = "PersonalImages/";
    public static final String strHandSignatures = "HandSignatures/";
    public static final String strCreatedPersonalIDs = "CreatedPersonalIDs/";
    public static final String strImportedPersonalIDs = "ImportedPersonalIDs/";
    public static final String encryptionAlgorithm = "RSA";
    public static final String hashAllgorithm = "SHA256withRSA";
    public final String appDataLocation;
    public static Controller controller;
    public Controller(String appDataLocation) {
        this.appDataLocation = appDataLocation;
    }

    public void generateKeyPair(String profileName, int sequence_number, PublicProfile.ValidityPeriod validityPeriod, String[] dynamicAttributes) throws NoSuchAlgorithmException, IOException, ParseException {
        if(!Utils.validateStringDate(validityPeriod.validFrom) || !Utils.validateStringDate(validityPeriod.validUntilForCreation)
                || !Utils.validateStringDate(validityPeriod.validUntilForCreated)) {
            notifyObservers(new OutputEvent.InvalidDateEvent());
            return;
        }

        String todayDate = Utils.today();
        if(!validateValidityPeriod(validityPeriod, todayDate)) {
            notifyObservers(new OutputEvent.InvalidDateSequenceEvent());
            return;
        }

        KeyPairGenerator gpk = KeyPairGenerator.getInstance(encryptionAlgorithm);
        gpk.initialize(2048);
        KeyPair keyPair = gpk.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        PrivateProfile privateProfile = new PrivateProfile(
                profileName, sequence_number, todayDate, validityPeriod, dynamicAttributes, publicKey.getEncoded(), privateKey.getEncoded());
        privateProfile.saveInternal(this, appDataLocation + strCreatedProfiles + profileName + "/" + sequence_number);
    }

    private boolean validateValidityPeriod(PublicProfile.ValidityPeriod v, String todayDate) throws ParseException {
        return Utils.dateAfter(todayDate, v.validFrom, true) &&
                Utils.dateAfter(v.validFrom, v.validUntilForCreation, false) &&
                Utils.dateAfter(v.validUntilForCreation, v.validUntilForCreated, false);
    }

    private byte[] sign_id(Personal_ID personalId, PrivateProfile privateProfile) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException, IOException {
        if(personalId.blob.isEmpty())
            throw new NoSuchAlgorithmException("Optional of BLOB is empty");
        Personal_ID.BLOB blob = personalId.blob.get();
        byte[] personalId_with_blob_b = Utils.concat_bytes(
                personalId.toByte(false), blob.personal_image, blob.hand_signature);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateProfile.privateKey);
        KeyFactory keyFactory = KeyFactory.getInstance(encryptionAlgorithm);
        //sign message
        Signature signature = Signature.getInstance(hashAllgorithm);
        signature.initSign(keyFactory.generatePrivate(spec));
        signature.update(personalId_with_blob_b);
        return signature.sign();
    }

    public void generateID(Controller controller, String publicProfileName, int sequence_number, String validUntil, String name, String surname, String birthdate, String address, String[] dynamicAttributeValues, File personalPicture, File handSignature) throws Exception {
        if(!Utils.validateStringDate(validUntil)) {
            notifyObservers(new OutputEvent.InvalidDateEvent());
            return;
        }

        //load public profile
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                this, appDataLocation + strCreatedProfiles, publicProfileName, sequence_number);
        if(privateProfile == null) {
            return;
        }

        // check validity period
        String today = Utils.today();
        if(!checkPersonalIDvalidDate(privateProfile.validityPeriod, today, validUntil)) {
            notifyObservers(new OutputEvent.PersonalIDoutOfValidityPeriod());
            return;
        }

        int nDynamicAttributes = privateProfile.dynamicAttributes.length;
        if(nDynamicAttributes != dynamicAttributeValues.length) {
            controller.notifyObservers(new OutputEvent.DynamicAttributesDoesntFitEvent(nDynamicAttributes));
            return;
        }

        String ID_number = Utils.getAlphanumeric(8);
        byte[] personalImage_b = Files.readAllBytes(personalPicture.toPath());
        byte[] handSignature_b = Files.readAllBytes(handSignature.toPath());

        Personal_ID personalId = new Personal_ID(ID_number, privateProfile, today, validUntil, name, surname, birthdate,
                address, dynamicAttributeValues, personalPicture.getName(), handSignature.getName());
        personalId.blob = Optional.of(new Personal_ID.BLOB(personalImage_b, handSignature_b));
        //Create signature
        byte[] signature_b = sign_id(personalId, privateProfile);
        personalId.signature = Optional.of(signature_b);
        //Save ID
        personalId.saveInternal(this, LOAD_FROM_CREATED);
    }

    public boolean checkPersonalIDvalidDate(PublicProfile.ValidityPeriod v, String today, String validUntil) throws ParseException {
        return Utils.dateAfter(v.validFrom, today, true) && Utils.dateAfter(today, v.validUntilForCreation, true) &&
                Utils.dateAfter(validUntil, v.validUntilForCreated, true) && Utils.daysBetween(today, validUntil) <= v.maxValidDays;
    }

    private boolean validateSignature(Personal_ID personalId) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
        if(personalId.blob.isEmpty())
            throw new NoSuchElementException("Option of BLOB is empty");
        if(personalId.signature.isEmpty())
            throw new NoSuchElementException("Option of signature is empty");
        Personal_ID.BLOB blob = personalId.blob.get();
        byte[] personal_id_b = Utils.concat_bytes(personalId.toByte(false), blob.personal_image, blob.hand_signature);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(personalId.publicProfile.publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(encryptionAlgorithm);
        Signature publicSignature = Signature.getInstance(hashAllgorithm);
        publicSignature.initVerify(keyFactory.generatePublic(spec));
        publicSignature.update(personal_id_b);
        return publicSignature.verify(personalId.signature.get());
    }

    public void checkPersonalID(String id_number) throws Exception {
        Personal_ID personalId = Personal_ID.loadInternal(this, LOAD_FROM_IMPORTED, id_number.toUpperCase());
        if(personalId == null) {
            return;
        }
        if (validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
        } else {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
        }
    }

    public void exportPublicProfile(String profileName, int sequence_number, File destination) throws IOException {
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                this, appDataLocation + strCreatedProfiles, profileName, sequence_number);
        if(privateProfile == null)
            return;
        PublicProfile publicProfile = new PublicProfile(privateProfile.name, privateProfile.sequence_number,
                privateProfile.created, privateProfile.validityPeriod, privateProfile.dynamicAttributes, privateProfile.publicKey);
        publicProfile.saveExternal(destination);
    }

    public void importPublicProfile(InputStream inputStream) throws IOException {
        PublicProfile publicProfile = PublicProfile.fromExternal(inputStream);
        publicProfile.saveInternal(this, appDataLocation + strImportedPublicProfiles);
    }

    public void exportPersonalID(String personalID_s, File destination) throws Exception {
        Personal_ID personalId = Personal_ID.loadInternal(this, LOAD_FROM_CREATED, personalID_s.toUpperCase());
        if (personalId == null) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(destination);
        personalId.toOutputStream(fos, true);
    }

    public void importPersonalID(InputStream inputStream) throws Exception {
        Personal_ID personalId = Personal_ID.fromInputStream(this, LOAD_FROM_IMPORTED, inputStream, true);
        if (personalId == null) {
            return;
        }

        if(!validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            return;
        }

        personalId.saveInternal(this, LOAD_FROM_IMPORTED);
    }

    public void checkPersonalIDFromRemote() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        ServerSocket serverSocket = new ServerSocket(0);
        notifyObservers(new OutputEvent.ServerStartedEvent(ip, serverSocket.getLocalPort()));
        Socket s = serverSocket.accept();
        InputStream inputStream = new BufferedInputStream(s.getInputStream());
        Personal_ID personalId = Personal_ID.fromInputStream(this, LOAD_FROM_IMPORTED, inputStream, true);
        serverSocket.close();

        if (personalId == null) {
            return;
        }

        if (validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
        } else {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
        }
    }

    public void handInPersonalIDtoRemote(String id_number, String ip, int port) throws Exception {
        Socket s = new Socket(ip, port);
        //load personal id
        Personal_ID personalId = Personal_ID.loadInternal(this, LOAD_FROM_IMPORTED, id_number.toUpperCase());
        if (personalId == null) {
            return;
        }
        //hand in
        OutputStream outputStream = new BufferedOutputStream(s.getOutputStream());
        personalId.toOutputStream(outputStream, true);
    }

    public void showPublicProfile(String profileName, int sequence) throws IOException {
        PublicProfile profile = PublicProfile.loadInternal(this, appDataLocation + Controller.strImportedPublicProfiles, profileName, sequence);
        if(profile == null) {
            return;
        }
        notifyObservers(new OutputEvent.ShowProfileEvent(profile.toString()));
    }

    public void saveAttachedData(String url, byte[] data) throws IOException {
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(data);
        fos.close();
    }
}

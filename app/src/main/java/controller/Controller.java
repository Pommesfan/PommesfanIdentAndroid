package controller;

import android.os.Looper;
import model.Personal_ID;
import model.PrivateProfile;
import model.PublicProfile;
import utils.*;
import javax.crypto.NoSuchPaddingException;
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
import java.text.ParseException;
import java.util.Arrays;
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
    public static final String strProgramPassword = "ProgramPassword";
    public static final String encryptionAlgorithm = "RSA";
    public static final String hashAllgorithm = "SHA256withRSA";
    public static final byte[] PROGRAM_WATERMARK = new byte[]{-87, 105, -121, -73, 46, -16, 16, -12, -54, 16, 81, 127, 85, 10, -35, -67};
    public static final int FILE_TYPE_PROFILE = 1;
    public static final int FILE_TYPE_ID = 2;
    public static final int AES_BUFFER_SIZE = 1024;
    public final String appDataLocation;
    private static byte[] programPasswordHash = null;
    public static Controller controller;

    public Controller(String appDataLocation) {
        this.appDataLocation = appDataLocation;
    }

    public void generateKeyPair(String profileName, int sequence_number, PublicProfile.ValidityPeriod validityPeriod, String[] dynamicAttributes) throws NoSuchAlgorithmException, IOException, ParseException, NoSuchPaddingException, InvalidKeyException {
        if (!Utils.validateStringDate(validityPeriod.validFrom) || !Utils.validateStringDate(validityPeriod.validUntilForCreation)
                || !Utils.validateStringDate(validityPeriod.validUntilForCreated)) {
            notifyObservers(new OutputEvent.InvalidDateEvent());
            return;
        }

        String todayDate = Utils.today();
        if (!validateValidityPeriod(validityPeriod, todayDate)) {
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
        notifyObservers(new OutputEvent.ActionCompletedEvent());
    }

    public boolean validateValidityPeriod(PublicProfile.ValidityPeriod v, String todayDate) throws ParseException {
        return Utils.dateAfter(todayDate, v.validFrom, true) &&
                Utils.dateAfter(v.validFrom, v.validUntilForCreation, false) &&
                Utils.dateAfter(v.validUntilForCreation, v.validUntilForCreated, false);
    }

    private byte[] sign_id(Personal_ID personalId, PrivateProfile privateProfile) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException, IOException {
        if (personalId.blob.isEmpty())
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
        if (!Utils.validateStringDate(validUntil)) {
            notifyObservers(new OutputEvent.InvalidDateEvent());
            return;
        }

        //load public profile
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                this, appDataLocation + strCreatedProfiles, publicProfileName, sequence_number);
        if (privateProfile == null) {
            return;
        }

        // check validity period
        String today = Utils.today();
        if (!checkPersonalIDvalidDate(privateProfile.validityPeriod, today, validUntil)) {
            notifyObservers(new OutputEvent.PersonalIDoutOfValidityPeriodEvent());
            return;
        }

        int nDynamicAttributes = privateProfile.dynamicAttributes.length;
        if (nDynamicAttributes != dynamicAttributeValues.length) {
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
        notifyObservers(new OutputEvent.ActionCompletedEvent());
    }

    public boolean checkPersonalIDvalidDate(PublicProfile.ValidityPeriod v, String today, String validUntil) throws ParseException {
        return Utils.dateAfter(v.validFrom, today, true) && Utils.dateAfter(today, v.validUntilForCreation, true) &&
                Utils.dateAfter(validUntil, v.validUntilForCreated, true) && Utils.daysBetween(today, validUntil) <= v.maxValidDays;
    }

    private boolean validateSignature(Personal_ID personalId) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
        if (personalId.blob.isEmpty())
            throw new NoSuchElementException("Option of BLOB is empty");
        if (personalId.signature.isEmpty())
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
        if (personalId == null) {
            return;
        }
        if (validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
        } else {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
        }
    }

    public void exportPublicProfile(String profileName, int sequence_number, File destination, String password) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                this, appDataLocation + strCreatedProfiles, profileName, sequence_number);
        if (privateProfile == null)
            return;
        PublicProfile publicProfile = new PublicProfile(privateProfile.name, privateProfile.sequence_number,
                privateProfile.created, privateProfile.validityPeriod, privateProfile.dynamicAttributes, privateProfile.publicKey);
        publicProfile.saveExternal(destination, password);
        notifyObservers(new OutputEvent.ActionCompletedEvent());
    }

    public void importPublicProfile(InputStream inputStream, String password) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (!checkProgramWatermark(inputStream)) {
            return;
        }
        if (!checkFileType(inputStream, FILE_TYPE_PROFILE))
            return;

        byte[] password_hash = Utils.passwordHash(password);
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, password_hash);
        PublicProfile publicProfile = PublicProfile.fromExternal(aesis, this, password_hash);
        if (publicProfile != null)
            publicProfile.saveInternal(this, appDataLocation + strImportedPublicProfiles);
        notifyObservers(new OutputEvent.ActionCompletedEvent());
    }

    public void exportPersonalID(String personalID_s, File destination, String password) throws Exception {
        Personal_ID personalId = Personal_ID.loadInternal(this, LOAD_FROM_CREATED, personalID_s.toUpperCase());
        if (personalId == null) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(destination);
        fos.write(PROGRAM_WATERMARK);
        fos.write(Utils.int_to_bytes(FILE_TYPE_ID));
        byte[] password_hash = Utils.passwordHash(password);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, password_hash);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        aesos.write(password_hash);
        personalId.toSliceWriter(sliceWriter, true);
        aesos.close();
        notifyObservers(new OutputEvent.ActionCompletedEvent());
    }

    public void importPersonalID(InputStream inputStream, Controller controller, String password) throws Exception {
        if (!checkProgramWatermark(inputStream))
            return;
        if (!checkFileType(inputStream, FILE_TYPE_ID))
            return;

        byte[] password_hash = Utils.passwordHash(password);
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, password_hash);
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);

        if (!controller.validateCryptoPassword(aesis, password_hash))
            return;

        Personal_ID personalId = Personal_ID.fromSliceReader(this, LOAD_FROM_IMPORTED, sliceReader, true);
        if (personalId == null) {
            return;
        }

        if (!validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            return;
        }

        personalId.saveInternal(this, LOAD_FROM_IMPORTED);
        notifyObservers(new OutputEvent.ActionCompletedEvent());
    }

    private CheckIDrunner checkIDrunner;
    private Personal_ID checkIDrunnerRes;

    public Personal_ID getCheckIDrunnerRes() {
        Personal_ID res = checkIDrunnerRes;
        checkIDrunnerRes = null;
        return res;
    }

    private class CheckIDrunner {
        final private Thread t;
        final private ServerSocket serverSocket;
        final private byte[] password_hash;

        public CheckIDrunner(ServerSocket serverSocket, byte[] password_hash) {
            t = new Thread(() -> {
                try {
                    Looper.prepare();
                    routine();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            this.serverSocket = serverSocket;
            this.password_hash = password_hash;
        }

        private void routine() throws Exception {
            checkIDrunner = null;
            // check crypto-password
            Socket s;
            s = serverSocket.accept();
            InputStream inputStream = s.getInputStream();
            if (inputStream.read() == 1) {
                s.close();
                serverSocket.close();
                checkIDrunner = null;
                notifyObservers(new OutputEvent.CheckIDcancelled());
                return;
            }

            OutputStream o = s.getOutputStream();
            AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, password_hash);

            if (!validateCryptoPassword(aesis, password_hash)) {
                o.write(1);
                o.close();
                aesis.close();
                s.close();
                checkIDrunner = null;
                return;
            }
            o.write(2);
            o.flush();

            Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
            Personal_ID personalId = Personal_ID.fromSliceReader(Controller.this, LOAD_FROM_IMPORTED, sliceReader, true);
            aesis.close();
            o.close();
            s.close();
            serverSocket.close();

            if (personalId == null) {
                return;
            }

            checkIDrunnerRes = personalId;

            if (validateSignature(personalId)) {
                notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
            } else {
                notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            }
        }

        public void start() {
            t.start();
        }
    }

    public void checkPersonalIDFromRemote() throws Exception {
        String password = Utils.getAlphanumeric(16);
        ServerSocket serverSocket = new ServerSocket(0);

        notifyObservers(new OutputEvent.ServerStartedEvent("127.0.0.1", serverSocket.getLocalPort(), password));
        byte[] password_hash = Utils.passwordHash(password);
        checkIDrunner = new CheckIDrunner(serverSocket, password_hash);
        checkIDrunner.start();
    }

    public void stopCheckIDrunner() throws IOException {
        if (checkIDrunner == null)
            return;
        Socket s = new Socket(InetAddress.getLocalHost().getHostAddress(), checkIDrunner.serverSocket.getLocalPort());
        s.getOutputStream().write(1);
        checkIDrunner = null;
    }


    public void handInPersonalIDtoRemote(String id_number, String ip, int port, String password) throws Exception {
        Socket s = new Socket(ip, port);
        // load personal id
        Personal_ID personalId = Personal_ID.loadInternal(this, LOAD_FROM_IMPORTED, id_number.toUpperCase());
        if (personalId == null) {
            return;
        }
        //hand in
        byte[] password_hash = Utils.passwordHash(password);
        OutputStream os = s.getOutputStream();
        AES_OutputStream aesos = AES_OutputStream.from_ecb(os, AES_BUFFER_SIZE, password_hash);
        os.write(0);
        aesos.write(password_hash);
        aesos.flush();
        InputStream i = s.getInputStream();
        if (i.read() == 1) {
            notifyObservers(new OutputEvent.CryptoPasswordInvalidEvent());
            i.close();
            aesos.close();
            s.close();
            return;
        }
        personalId.toSliceWriter(new Utils.SliceWriter(aesos), true);
        aesos.close();
        i.close();
        s.close();
        notifyObservers(new OutputEvent.IDhandedInSuccessEvent());
    }

    public void showPublicProfile(String profileName, int sequence) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        PublicProfile profile = PublicProfile.loadInternal(this, appDataLocation + Controller.strImportedPublicProfiles, profileName, sequence);
        if (profile == null) {
            return;
        }
        notifyObservers(new OutputEvent.ShowProfileEvent(profile.toString()));
    }

    public void saveAttachedData(String url, byte[] data) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, programPasswordHash);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        sliceWriter.write(data);
        aesos.close();
    }

    public byte[] readAttachedData(String url) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        FileInputStream fis = new FileInputStream(url);
        AES_InputStream aesis = AES_InputStream.from_ecb(fis, AES_BUFFER_SIZE, programPasswordHash);
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        byte[] res = sliceReader.next();
        aesis.close();
        return res;
    }

    public byte[] getProgramPasswordHash() {
        return programPasswordHash;
    }

    public boolean setProgramPasswordHash(String password) throws NoSuchPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        this.programPasswordHash = Utils.passwordHash(password);
        byte[] passwordHash = Utils.passwordHash(password);
        String url = appDataLocation + strProgramPassword;
        if (Files.exists(Paths.get(url))) {
            FileInputStream fis = new FileInputStream(url);
            AES_InputStream aesis = AES_InputStream.from_ecb(fis, 32, passwordHash);
            byte[] savedPasswordHash = new byte[32];
            aesis.read(savedPasswordHash);
            aesis.close();
            return Arrays.equals(savedPasswordHash, passwordHash);
        } else {
            FileOutputStream fos = new FileOutputStream(Utils.createFileAndSubfolder(url));
            AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, 32, passwordHash);
            aesos.write(passwordHash);
            aesos.close();
            return true;
        }
    }

    public boolean checkProgramWatermark(InputStream inputStream) throws IOException {
        byte[] readedWatermark_b = new byte[16];
        inputStream.read(readedWatermark_b);
        if (!Arrays.equals(readedWatermark_b, PROGRAM_WATERMARK)) {
            notifyObservers(new OutputEvent.FileNotFromHereEvent());
            return false;
        }
        return true;
    }

    public boolean checkFileType(InputStream inputStream, int type) throws IOException {
        byte[] readedType_b = new byte[4];
        inputStream.read(readedType_b);
        int readedType = Utils.bytes_to_int(readedType_b);
        if (readedType != type) {
            notifyObservers(new OutputEvent.WrongFileTypeEvent(readedType));
            return false;
        }
        return true;
    }

    public boolean validateCryptoPassword(InputStream inputStream, byte[] password_hash) throws IOException {
        byte[] savedPasswordHash = new byte[32];
        inputStream.read(savedPasswordHash);
        if (!Arrays.equals(savedPasswordHash, password_hash)) {
            notifyObservers(new OutputEvent.CryptoPasswordInvalidEvent());
            return false;
        }
        return true;
    }
}
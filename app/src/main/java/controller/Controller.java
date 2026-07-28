package controller;

import model.AttachmentRelation;
import model.Personal_ID;
import model.PrivateProfile;
import model.PublicProfile;
import utils.*;
import utils.Observable;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.*;

public class Controller extends Observable<OutputEvent> {
    public static final int LOAD_FROM_CREATED = 1;
    public static final int LOAD_FROM_IMPORTED = 2;
    public static final String strPrivateProfiles = "PrivateProfiles/";
    public static final String strPublicProfiles = "PublicProfiles/";
    public static final String strPersonalImages = "PersonalImages/";
    public static final String strHandSignatures = "HandSignatures/";
    public static final String strCreatedPersonalIDs = "CreatedPersonalIDs/";
    public static final String strImportedPersonalIDs = "ImportedPersonalIDs/";
    public static final String strProgramPassword = "ProgramPassword";
    public static final String strPersonalImageRelations = "PersonalImagesRelations";
    public static final String strHandSignaturesRelations = "HandSignaturesRelations";
    public static final String encryptionAlgorithm = "RSA";
    public static final String hashAllgorithm = "SHA256withRSA";
    public static final byte[]PROGRAM_WATERMARK = new byte[]{-87, 105, -121, -73, 46, -16, 16, -12, -54, 16, 81, 127, 85, 10, -35, -67};
    public static final int FILE_TYPE_PUBLIC_PROFILE = 1;
    public static final int FILE_TYPE_PRIVATE_PROFILE = 3;
    public static final int FILE_TYPE_ID = 2;
    public static final int CON_PURPOSE_IMPORT = 4;
    public static final int CON_PURPOSE_CHECK_ID = 5;
    public static final int AES_BUFFER_SIZE = 1024;
    public static final int ATTACHMENT_PERSONAL_IMAGE = 1;
    public static final int ATTACHMENT_HAND_SIGNATURE = 2;
    public final String appDataLocation;
    private static byte[] programPasswordHash = null;
    public static Controller controller; // for Android

    public Controller(String appDataLocation) {
        this.appDataLocation = appDataLocation;
    }

    public void generateKeyPair(String profileName, int sequence_number, PublicProfile.ValidityPeriod validityPeriod, String[] dynamicAttributes) throws NoSuchAlgorithmException, IOException, ParseException, NoSuchPaddingException, InvalidKeyException {
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
        privateProfile.saveInternal(appDataLocation + strPrivateProfiles + profileName + "/" + sequence_number);
        notifyObservers(new OutputEvent.CreationSuccessEvent());
    }

    public boolean validateValidityPeriod(PublicProfile.ValidityPeriod v, String todayDate) throws ParseException {
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

    public void generateID(String publicProfileName, int sequence_number, String validUntil, String name, String surname,
                           String birthdate, String address, String[] dynamicAttributeValues, byte[]personalImage,
                           String personalImageName, byte[] handSignature, String handSignatureName) throws Exception {
        if(!Utils.validateStringDate(validUntil)) {
            notifyObservers(new OutputEvent.InvalidDateEvent());
            return;
        }

        //load public profile
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                appDataLocation + strPrivateProfiles, publicProfileName, sequence_number, true);
        if(privateProfile == null) {
            return;
        }

        // check validity period
        String today = Utils.today();
        if(!checkPersonalIDvalidDate(privateProfile.validityPeriod, today, validUntil)) {
            notifyObservers(new OutputEvent.PersonalIDoutOfValidityPeriodEvent());
            return;
        }

        int nDynamicAttributes = privateProfile.dynamicAttributes.length;
        if(nDynamicAttributes != dynamicAttributeValues.length) {
            controller.notifyObservers(new OutputEvent.DynamicAttributesDoesntFitEvent(nDynamicAttributes));
            return;
        }

        String ID_number = Utils.getAlphanumeric(8);

        Personal_ID personalId = new Personal_ID(ID_number, privateProfile, today, validUntil, name, surname, birthdate,
                address, dynamicAttributeValues, personalImageName, handSignatureName);
        personalId.blob = Optional.of(new Personal_ID.BLOB(personalImage, handSignature));
        //Create signature
        byte[] signature_b = sign_id(personalId, privateProfile);
        personalId.signature = Optional.of(signature_b);
        //Save ID
        personalId.saveInternal(LOAD_FROM_CREATED);
        notifyObservers(new OutputEvent.CreationSuccessEvent());
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
        Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_IMPORTED, id_number.toUpperCase(), true, true);
        if(personalId == null) {
            return;
        }
        if (validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
        } else {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
        }
    }

    public void exportPublicProfile(String profileName, int sequence_number, OutputStream os, String password) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                appDataLocation + strPrivateProfiles, profileName, sequence_number, true);
        if(privateProfile == null)
            return;

        PublicProfile publicProfile = privateProfile.toPublic();
        publicProfile.saveExternal(os, password, FILE_TYPE_PUBLIC_PROFILE);
        notifyObservers(new OutputEvent.DummyEvent());
    }

    public void importPublicProfile(InputStream inputStream, String password) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(!checkProgramWatermark(inputStream)) {
            return;
        }
        if (!checkFileType(inputStream, FILE_TYPE_PUBLIC_PROFILE))
            return;

        byte[]password_hash = Utils.passwordHash(password);
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, password_hash);
        PublicProfile publicProfile = PublicProfile.fromExternal(aesis, password_hash);
        if (publicProfile != null) {
            publicProfile.saveInternal(appDataLocation + strPublicProfiles);
            notifyObservers(new OutputEvent.DummyEvent());
        }
    }

    public void exportPrivateProfile(String profileName, int sequenceNumber, OutputStream os, String password) throws NoSuchPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        PrivateProfile privateProfile = PrivateProfile.fromInternalFile(
                appDataLocation + strPrivateProfiles, profileName, sequenceNumber, true);
        if(privateProfile == null)
            return;
        privateProfile.saveExternal(os, password, FILE_TYPE_PRIVATE_PROFILE);
        notifyObservers(new OutputEvent.DummyEvent());
    }

    public void importPrivateProfile(InputStream inputStream, String password) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        if(!checkProgramWatermark(inputStream)) {
            return;
        }
        if (!checkFileType(inputStream, FILE_TYPE_PRIVATE_PROFILE))
            return;

        byte[]password_hash = Utils.passwordHash(password);
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, password_hash);
        PrivateProfile privateProfile = PrivateProfile.fromExternal(aesis, password_hash);
        if (privateProfile != null) {
            privateProfile.saveInternal(appDataLocation + strPrivateProfiles + privateProfile.name +
                    "/" + privateProfile.sequence_number);
            notifyObservers(new OutputEvent.DummyEvent());
        }
    }

    public void exportPersonalID(String personalID_s, OutputStream os, String password) throws Exception {
        Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_CREATED, personalID_s.toUpperCase(), true, true);
        if (personalId == null) {
            return;
        }

        os.write(PROGRAM_WATERMARK);
        os.write(FILE_TYPE_ID);
        byte[]password_hash = Utils.passwordHash(password);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(os, AES_BUFFER_SIZE, password_hash);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        aesos.write(password_hash);
        personalId.toSliceWriter(sliceWriter, true);
        aesos.close();
        notifyObservers(new OutputEvent.DummyEvent());
    }

    public void importPersonalID(InputStream inputStream, String password) throws Exception {
        if(!checkProgramWatermark(inputStream))
            return;
        if (!checkFileType(inputStream, FILE_TYPE_ID))
            return;

        byte[]password_hash = Utils.passwordHash(password);
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, password_hash);
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);

        if(!controller.validateCryptoPassword(aesis, password_hash))
            return;

        Personal_ID personalId = Personal_ID.fromSliceReader(LOAD_FROM_IMPORTED, sliceReader, true, true);
        if (personalId == null) {
            return;
        }

        if(!validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            return;
        }

        personalId.saveInternal(LOAD_FROM_IMPORTED);
        notifyObservers(new OutputEvent.DummyEvent());
    }

    private BackgroundRunner backgroundRunner;
    private Personal_ID checkIDrunnerRes;

    public Personal_ID getCheckIDrunnerRes() {
        Personal_ID res = checkIDrunnerRes;
        checkIDrunnerRes = null;
        return res;
    }
    public void deleteProfile(String name, int sequenceNumber, int mode) throws Exception {
        boolean isAggregated;
        String url;
        if(mode == LOAD_FROM_CREATED) {
            isAggregated = PrivateProfile.isIDaggregated(name, sequenceNumber);
            url = strPrivateProfiles;
        } else if (mode == LOAD_FROM_IMPORTED) {
            isAggregated = PublicProfile.isIDaggregated(name, sequenceNumber);
            url = strPublicProfiles;
        } else
            throw new IllegalArgumentException("unvalid mode: " + mode);
        if(isAggregated)
            notifyObservers(new OutputEvent.IDaggregatedEvent());
        else {
            Files.delete(Paths.get(appDataLocation + url + name + "/" + sequenceNumber));
            notifyObservers(new OutputEvent.DummyEvent());
        }
    }

    public void deleteID(String idNumber, int mode) throws Exception {
        String urlToDelete;
        String urlOpposite;
        if(mode == LOAD_FROM_CREATED) {
            urlToDelete = strCreatedPersonalIDs;
            urlOpposite = strImportedPersonalIDs;
        } else if (mode == LOAD_FROM_IMPORTED) {
            urlToDelete = strImportedPersonalIDs;
            urlOpposite = strCreatedPersonalIDs;
        } else
            throw new IllegalArgumentException("unvalid mode: " + mode);
        Personal_ID id = Personal_ID.loadInternal(mode, idNumber, true, true);
        if(id == null)
            return;

        // if id is present in created, don't delete relation
        if(!Files.exists(Paths.get(appDataLocation + urlOpposite + idNumber))) {
            AttachmentRelation relationPersonalImage = AttachmentRelation.getRelation(ATTACHMENT_PERSONAL_IMAGE);
            String personalImageFile = relationPersonalImage.removeID(id.ID_number);
            if(!relationPersonalImage.hasImage(personalImageFile)) {
                Files.delete(Paths.get(appDataLocation + strPersonalImages + personalImageFile));
            }
            relationPersonalImage.save();

            AttachmentRelation relationHandSignature = AttachmentRelation.getRelation(ATTACHMENT_HAND_SIGNATURE);
            String handSignatureFile = relationHandSignature.removeID(id.ID_number);
            if(!relationHandSignature.hasImage(handSignatureFile)) {
                Files.delete(Paths.get(appDataLocation + strHandSignatures + handSignatureFile));
            }
            relationHandSignature.save();
        }

        Files.delete(Paths.get(appDataLocation + urlToDelete + idNumber));
        notifyObservers(new OutputEvent.DummyEvent());
    }

    private class CheckIDrunner extends BackgroundRunner {
        public CheckIDrunner()  {
            super();
        }

        @Override
        protected void routine() throws Exception {
            init();
            Socket s = serverSocket.accept();
            InputStream inputStream = s.getInputStream();
            if(inputStream.read() == 1) {
                s.close();
                serverSocket.close();
                backgroundRunner = null;
                notifyObservers(new OutputEvent.CheckIDcancelled());
                return;
            }

            OutputStream o = s.getOutputStream();
            o.write(PROGRAM_WATERMARK);
            o.write(CON_PURPOSE_CHECK_ID);
            AES_OutputStream aesos = AES_OutputStream.from_ecb(o, AES_BUFFER_SIZE, crypto_hash);
            aesos.write(crypto_hash);
            aesos.flush();

            if(inputStream.read() == 1) {
                s.close();
                serverSocket.close();
                backgroundRunner = null;
                notifyObservers(new OutputEvent.CheckIDcancelled());
                return;
            }

            AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, crypto_hash);
            Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
            Personal_ID personalId = Personal_ID.fromSliceReader(LOAD_FROM_IMPORTED, sliceReader, true, true);
            aesos.close();
            aesis.close();
            s.close();
            serverSocket.close();
            backgroundRunner = null;

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
    }

    public void checkPersonalIDFromRemote() {
        backgroundRunner = new CheckIDrunner();
        backgroundRunner.start();
    }

    public void handInPersonalIDtoRemote(String id_number, String ip, int port, String password) throws Exception {
        Socket s = new Socket(ip, port);
        // load personal id
        Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_IMPORTED, id_number.toUpperCase(), true, true);
        if (personalId == null) {
            return;
        }
        //hand in
        byte[]password_hash = Utils.passwordHash(password);
        OutputStream os = s.getOutputStream();
        AES_OutputStream aesos = AES_OutputStream.from_ecb(os, AES_BUFFER_SIZE, password_hash);
        os.write(0);
        InputStream inputStream = s.getInputStream();
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, 1024, password_hash);
        if(!checkProgramWatermark(inputStream) || !checkConnectionPurposeType(inputStream, CON_PURPOSE_CHECK_ID)
                || !validateCryptoPassword(aesis, password_hash)) {
            os.write(1);
            aesos.close();
            inputStream.close();
            s.close();
            return;
        }

        os.write(0);
        personalId.toSliceWriter(new Utils.SliceWriter(aesos), true);
        aesos.close();
        inputStream.close();
        s.close();
        notifyObservers(new OutputEvent.IDhandedInSuccessEvent());
    }

    private class ExportIDrunner extends BackgroundRunner {
        private final String idNumber;
        public ExportIDrunner(String idNumber) {
            super();
            this.idNumber = idNumber.toUpperCase();
        }

        @Override
        protected void routine() throws Exception {
            init();
            Socket s = serverSocket.accept();
            InputStream inputStream = s.getInputStream();
            if(inputStream.read() == 1) {
                s.close();
                serverSocket.close();
                backgroundRunner = null;
                notifyObservers(new OutputEvent.DummyEvent());
                return;
            }

            OutputStream outputStream = s.getOutputStream();
            AES_OutputStream aesos = AES_OutputStream.from_ecb(outputStream, AES_BUFFER_SIZE, crypto_hash);
            outputStream.write(PROGRAM_WATERMARK);
            outputStream.write(CON_PURPOSE_IMPORT);
            aesos.write(crypto_hash);
            aesos.flush();

            Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_CREATED, idNumber, true, true);
            if (personalId == null) {
                return;
            }

            if(inputStream.read() == 1) {
                s.close();
                serverSocket.close();
                backgroundRunner = null;
                notifyObservers(new OutputEvent.DummyEvent());
                return;
            }
            personalId.publicProfile.toSliceWriter(new Utils.SliceWriter(aesos));
            personalId.toSliceWriter(new Utils.SliceWriter(aesos), true);
            aesos.close();
            notifyObservers(new OutputEvent.DummyEvent());
        }
    }

    public void importOverNetwork(String ip, int port, String crypto) throws Exception {
        Socket s = new Socket(ip, port);
        s.getOutputStream().write(0);
        byte[]cryptoHash = Utils.passwordHash(crypto);
        InputStream inputStream = s.getInputStream();
        AES_InputStream aesis = AES_InputStream.from_ecb(inputStream, AES_BUFFER_SIZE, cryptoHash);
        OutputStream outputStream = s.getOutputStream();

        if(!checkProgramWatermark(inputStream) || !checkConnectionPurposeType(inputStream, CON_PURPOSE_IMPORT)
                || !validateCryptoPassword(aesis, cryptoHash)) {
            outputStream.write(1);
            outputStream.flush();
            aesis.close();
            return;
        }
        outputStream.write(0);
        PublicProfile publicProfile = PublicProfile.fromSliceReader(new Utils.SliceReader(aesis));
        if(Files.exists(Paths.get(appDataLocation + strPublicProfiles + publicProfile.name + "/" + publicProfile.sequence_number))) {
            PublicProfile saved = PublicProfile.loadInternal(
                    appDataLocation + strPublicProfiles, publicProfile.name, publicProfile.sequence_number, true);
            if(!saved.equals(publicProfile)) {
                notifyObservers(new OutputEvent.OtherProfileFoundEvent());
                return;
            }
        } else {
            publicProfile.saveInternal(appDataLocation + strPublicProfiles + "/");
        }

        Personal_ID personalId = Personal_ID.fromSliceReader(LOAD_FROM_IMPORTED, new Utils.SliceReader(aesis), true, true);
        if(personalId == null) {
            aesis.close();
            return;
        }

        if(!validateSignature(personalId)) {
            notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            aesis.close();
            return;
        }
        personalId.saveInternal(LOAD_FROM_IMPORTED);
        notifyObservers(new OutputEvent.DummyEvent());
        aesis.close();
    }

    public void exportOverNetwork(String idNumber) throws Exception {
        backgroundRunner = new ExportIDrunner(idNumber);
        backgroundRunner.start();
    }

    public void stopBackgroundRunner() throws IOException {
        if(backgroundRunner == null)
            return;
        Socket s = new Socket(InetAddress.getLocalHost().getHostAddress(), backgroundRunner.getPort());
        s.getOutputStream().write(1);
        backgroundRunner = null;
    }

    public void showPublicProfile(String profileName, int sequence) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        PublicProfile profile = PublicProfile.loadInternal(appDataLocation + Controller.strPublicProfiles, profileName, sequence, true);
        if(profile == null) {
            return;
        }
        notifyObservers(new OutputEvent.ShowProfileEvent(profile.toString()));
    }

    public void saveAttachedData(String originalFileName, int attachmentMode, String idNumber, byte[] image) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        AttachmentRelation relation = AttachmentRelation.getRelation(attachmentMode);
        String url = AttachmentRelation.attachmentPath(attachmentMode);
        Set<String> sameOriginalFileNames = relation.getSameFileNames(originalFileName);
        String imageFileName;
        if(sameOriginalFileNames.isEmpty()) {
            imageFileName = Utils.getAlphanumeric(8);
            saveAttachmentFile(url + imageFileName, image);
        } else {
            Optional<String> sameImage = getSameImage(url, image, sameOriginalFileNames);
            if(sameImage.isEmpty()) {
                imageFileName = Utils.getAlphanumeric(8);
                saveAttachmentFile(url + imageFileName, image);
            } else {
                imageFileName = sameImage.get();
            }
        }
        if(!relation.hasID(idNumber))
            relation.insertRelation(imageFileName, originalFileName, idNumber);
        relation.save();
    }

    private void saveAttachmentFile(String url, byte[]attachment) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, programPasswordHash);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        sliceWriter.write(attachment);
        aesos.close();
    }

    public byte[]readAttachedData(String idNumber, int attachmentMode) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        AttachmentRelation relation = AttachmentRelation.getRelation(attachmentMode);
        String url = AttachmentRelation.attachmentPath(attachmentMode);
        String imageFileName = relation.getImageFileName(idNumber);
        FileInputStream fis = new FileInputStream(url + imageFileName);
        AES_InputStream aesis = AES_InputStream.from_ecb(fis, AES_BUFFER_SIZE, programPasswordHash);
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        byte[]res = sliceReader.next();
        aesis.close();
        return res;
    }

    public byte[] getProgramPasswordHash() {
        return programPasswordHash;
    }

    public boolean setProgramPasswordHash(String password) throws NoSuchPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        programPasswordHash = Utils.passwordHash(password);
        byte[]passwordHash = Utils.passwordHash(password);
        String url = appDataLocation + strProgramPassword;
        if(Files.exists(Paths.get(url))) {
            FileInputStream fis = new FileInputStream(url);
            AES_InputStream aesis = AES_InputStream.from_ecb(fis, 32, passwordHash);
            byte[]savedPasswordHash = new byte[32];
            aesis.read(savedPasswordHash);
            aesis.close();
            return Arrays.equals(savedPasswordHash, passwordHash);
        } else {
            FileOutputStream fos = new FileOutputStream(Utils.createFileAndSubfolder(url));
            AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, 32, passwordHash);
            aesos.write(passwordHash);
            aesos.close();
            // create attachment relation files
            Files.createFile(Paths.get(appDataLocation + strPersonalImageRelations));
            Files.createFile(Paths.get(appDataLocation + strHandSignaturesRelations));
            return true;
        }
    }

    private Optional<String> getSameImage(String url, byte[] image, Set<String> sameOriginalFileNames) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        for (String fileName: sameOriginalFileNames) {
            if(isSameImage(url + fileName, image)) {
                return Optional.of(fileName);
            }
        }
        return Optional.empty();
    }

    private boolean isSameImage(String url, byte[]image) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        FileInputStream fis = new FileInputStream(url);
        AES_InputStream aesis = AES_InputStream.from_ecb(fis, AES_BUFFER_SIZE, programPasswordHash);
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        byte[]res = sliceReader.next();
        return Arrays.equals(image, res);
    }

    public boolean checkProgramWatermark(InputStream inputStream) throws IOException {
        byte[]readedWatermark_b = new byte[16];
        inputStream.read(readedWatermark_b);
        if(!Arrays.equals(readedWatermark_b, PROGRAM_WATERMARK)) {
            notifyObservers(new OutputEvent.FileNotFromHereEvent());
            return false;
        }
        return true;
    }

    public boolean checkFileType(InputStream inputStream, int type) throws IOException {
        int readedType = inputStream.read();
        if(readedType != type) {
            notifyObservers(new OutputEvent.WrongFileTypeEvent(readedType));
            return false;
        }
        return true;
    }

    public boolean checkConnectionPurposeType(InputStream inputStream, int type) throws IOException {
        int readedType = inputStream.read();
        if(readedType != type) {
            notifyObservers(new OutputEvent.WrongConnectionPurposeTypeEvent(readedType));
            return false;
        }
        return true;
    }

    public boolean validateCryptoPassword(InputStream inputStream, byte[]password_hash) throws IOException {
        byte[]savedPasswordHash = new byte[32];
        inputStream.read(savedPasswordHash);
        if(!Arrays.equals(savedPasswordHash, password_hash)) {
            notifyObservers(new OutputEvent.CryptoPasswordInvalidEvent());
            return false;
        }
        return true;
    }
}

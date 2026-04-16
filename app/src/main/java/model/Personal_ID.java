package model;

import controller.Controller;
import utils.AES_InputStream;
import utils.AES_OutputStream;
import utils.OutputEvent;
import utils.Utils;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Optional;

import static controller.Controller.*;

public class Personal_ID {
    public final String ID_number;
    public final PublicProfile publicProfile;
    public final String created;
    public final String validUntil;
    public final String name;
    public final String surname;
    public final String birthdate;
    public final String address;
    public final String[] dynamicAttributesValues;
    public final String personalImagePath;
    public final String handSignaturePath;
    public Optional<byte[]> signature = Optional.empty();
    public Optional<BLOB> blob = Optional.empty();

    public Personal_ID(String pIDnumber, PublicProfile pPublicProfile, String pCreated, String pValidUntil, String pName,
                       String pSurname, String pBirthDate, String pAddress, String[] pDynamicAttributesValues,
                       String pPersonalImagePath, String pHandSignaturePath) {
        ID_number = pIDnumber;
        publicProfile = pPublicProfile;
        created = pCreated;
        validUntil = pValidUntil;
        name = pName;
        surname = pSurname;
        birthdate = pBirthDate;
        address = pAddress;
        dynamicAttributesValues = pDynamicAttributesValues;
        personalImagePath = pPersonalImagePath;
        handSignaturePath = pHandSignaturePath;
    }

    public static Personal_ID fromString(Controller controller, int created_or_imported_profile, String[] attributes) throws Exception {
        String ID_number = attributes[0];
        PublicProfile publicProfile;
        final String profileName = attributes[1];
        final int sequence_number = Integer.parseInt(attributes[2]);
        if (created_or_imported_profile == Controller.LOAD_FROM_CREATED) {
            publicProfile = PrivateProfile.loadInternal(controller, controller.appDataLocation + Controller.strPrivateProfiles, profileName, sequence_number);
        } else if(created_or_imported_profile == Controller.LOAD_FROM_IMPORTED) {
            publicProfile = PublicProfile.loadInternal(controller, controller.appDataLocation + Controller.strPublicProfiles, profileName, sequence_number);
        } else {
            throw new NoSuchMethodException("created_or_imported must be 1 or 2");
        }
        if(publicProfile == null) {
            return null;
        }
        String created = attributes[3];
        String validUntil = attributes[4];

        if(!Utils.dateAfter(Utils.today(), validUntil, true)) {
            controller.notifyObservers(new OutputEvent.PersonalIDoutdatedEvent(ID_number));
            return null;
        }

        if(!controller.validateValidityPeriod(publicProfile.validityPeriod, publicProfile.validityPeriod.validFrom)) {
            controller.notifyObservers(new OutputEvent.InvalidDateSequenceEvent());
            return null;
        }

        if(!controller.checkPersonalIDvalidDate(publicProfile.validityPeriod, Utils.today(), validUntil)) {
            controller.notifyObservers(new OutputEvent.PersonalIDoutOfValidityPeriodEvent());
            return null;
        }

        String name = attributes[5];
        String surname = attributes[6];
        String birthdate = attributes[7];
        String address = attributes[8];

        int nDynamicAttributes = publicProfile.dynamicAttributes.length;
        if(attributes.length != 11 + nDynamicAttributes) {
            controller.notifyObservers(new OutputEvent.DynamicAttributesDoesntFitEvent(nDynamicAttributes));
            return null;
        }

        String[] dynamicAttributesValues = Utils.sliceStringArray(attributes, 9, 9 + nDynamicAttributes);

        String personalImagePath = attributes[9 + nDynamicAttributes];
        String handSignaturePath = attributes[10 + nDynamicAttributes];
        return new Personal_ID(ID_number, publicProfile, created, validUntil, name, surname, birthdate, address, dynamicAttributesValues, personalImagePath, handSignaturePath);
    }

    public static Personal_ID fromSliceReader(Controller controller, int created_or_imported_profile, Utils.SliceReader sliceReader, boolean withBlob) throws Exception {
        String[] attributes = Utils.bytesToStringArray(sliceReader.next());
        if(attributes.length == 0)
            return null;
        Personal_ID personalId = Personal_ID.fromString(controller, created_or_imported_profile, attributes);
        if(personalId == null)
            return null;
        personalId.signature = Optional.of(sliceReader.next());

        if(withBlob) {
            byte[] personal_image = sliceReader.next();
            byte[] hand_signature = sliceReader.next();
            personalId.blob = Optional.of(new BLOB(personal_image, hand_signature));
        }
        return personalId;
    }

    public static Personal_ID loadInternal(Controller controller, int created_or_imported, String name, boolean loadBlob) throws Exception {
        String location;
        if (created_or_imported == Controller.LOAD_FROM_CREATED) {
            location = controller.appDataLocation + Controller.strCreatedPersonalIDs;
        } else if(created_or_imported == Controller.LOAD_FROM_IMPORTED) {
            location = controller.appDataLocation + Controller.strImportedPersonalIDs;
        } else {
            throw new NoSuchMethodException("created_or_imported must be 1 or 2");
        }
        if(!Utils.exists(location + name)) {
            controller.notifyObservers(new OutputEvent.NoSuchPersonalIDevent(name));
            return null;
        }
        FileInputStream fis = new FileInputStream(location + name);
        AES_InputStream aesis = AES_InputStream.from_ecb(fis, AES_BUFFER_SIZE, controller.getProgramPasswordHash());
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        Personal_ID personalId = fromSliceReader(controller, created_or_imported, sliceReader, false);
        if(personalId == null)
            return null;

        if(loadBlob) {
            byte[] personalImage_b = controller.readAttachedData(controller.appDataLocation + Controller.strPersonalImages + personalId.personalImagePath);
            byte[] handSignature_b = controller.readAttachedData(controller.appDataLocation + Controller.strHandSignatures + personalId.handSignaturePath);
            personalId.blob = Optional.of(new BLOB(personalImage_b, handSignature_b));
        }
        aesis.close();
        return personalId;
    }

    public void toSliceWriter(Utils.SliceWriter sliceWriter, boolean withBlob) throws IOException {
        sliceWriter.write(toByte(true));

        if(signature.isEmpty())
            throw new NoSuchElementException("Optional of signature is empty");

        sliceWriter.write(signature.get());
        if(withBlob) {

            if (blob.isEmpty())
                throw new NoSuchElementException("Optional of BLOB is empty");

            BLOB blob_unwrapped = blob.get();
            sliceWriter.write(blob_unwrapped.personal_image);
            sliceWriter.write(blob_unwrapped.hand_signature);
        }
    }

    public void saveInternal(Controller controller, int created_or_imported) throws IOException, NoSuchMethodException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (blob.isEmpty())
            throw new NoSuchElementException("Optional of BLOB is empty");
        BLOB blob_unwrapped = blob.get();

        String location;
        if (created_or_imported == Controller.LOAD_FROM_CREATED) {
            location = controller.appDataLocation + Controller.strCreatedPersonalIDs;
        } else if(created_or_imported == Controller.LOAD_FROM_IMPORTED) {
            location = controller.appDataLocation + Controller.strImportedPersonalIDs;
        } else {
            throw new NoSuchMethodException("created_or_imported must be 1 or 2");
        }

        if(Utils.exists(location + ID_number)) {
            controller.notifyObservers(new OutputEvent.IDalreadyExistsEvent());
            return;
        }
        File f = Utils.createFileAndSubfolder(location + ID_number);
        FileOutputStream fos = new FileOutputStream(f);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, controller.getProgramPasswordHash());
        toSliceWriter(new Utils.SliceWriter(aesos), false);
        aesos.close();
        controller.saveAttachedData(controller.appDataLocation + Controller.strPersonalImages + personalImagePath, blob_unwrapped.personal_image);
        controller.saveAttachedData(controller.appDataLocation + Controller.strHandSignatures + handSignaturePath, blob_unwrapped.hand_signature);
    }

    public byte[] toByte(boolean withPaths) throws IOException {
        Utils.LineWriter lineWriter = new Utils.LineWriter();
        lineWriter.write(ID_number);
        lineWriter.write(publicProfile.name);
        lineWriter.write(Integer.toString(publicProfile.sequence_number));
        lineWriter.write(created);
        lineWriter.write(validUntil);
        lineWriter.write(name);
        lineWriter.write(surname);
        lineWriter.write(birthdate);
        lineWriter.write(address);

        for (String attribute : dynamicAttributesValues) {
            lineWriter.write(attribute);
        }

        if(withPaths) {
            lineWriter.write(personalImagePath);
            lineWriter.write(handSignaturePath);
        }
        return lineWriter.get_bytes();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ausweisnummer:\n");
        sb.append(ID_number);
        sb.append("\nÖffentliches Profil:\n");
        sb.append(publicProfile.name);
        sb.append("\nÖffentliches Profil Folgenummer:\n");
        sb.append(publicProfile.sequence_number);
        sb.append("\nErstellt:\n");
        sb.append(created);
        sb.append("\nGültig bis:\n");
        sb.append(validUntil);
        sb.append("\nVorname:\n");
        sb.append(name);
        sb.append("\nNachname:\n");
        sb.append(surname);
        sb.append("\nGeburtsdatum\n");
        sb.append(birthdate);
        sb.append("\nAdresse:\n");
        sb.append(address);

        for (int i = 0; i < publicProfile.dynamicAttributes.length; i++) {
            sb.append('\n');
            sb.append(publicProfile.dynamicAttributes[i]);
            sb.append(":\n");
            sb.append(dynamicAttributesValues[i]);
        }

        sb.append("\nPfad Passbild:\n");
        sb.append(personalImagePath);
        sb.append("\nPfad händische Signatur:\n");
        sb.append(handSignaturePath);
        sb.append('\n');
        return sb.toString();
    }

    public static class BLOB {
        public final byte[] personal_image;
        public final byte[] hand_signature;
        public BLOB(byte[] personal_image, byte[] hand_signature) {
            this.personal_image = personal_image;
            this.hand_signature = hand_signature;
        }
    }
}

package model;

import utils.AES_InputStream;
import utils.AES_OutputStream;
import utils.OutputEvent;
import utils.Utils;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

import static controller.Controller.*;

public class PublicProfile {
    public final String name;
    public final int sequence_number;
    public final String created;
    public final ValidityPeriod validityPeriod;
    public final String[] dynamicAttributes;
    public final byte[] publicKey;

    public PublicProfile(String name, int sequence_number, String created, ValidityPeriod validityPeriod, String[] dynamicAttributes, byte[] publicKey) {
        this.name = name;
        this.sequence_number = sequence_number;
        this.created = created;
        this.validityPeriod = validityPeriod;
        this.dynamicAttributes = dynamicAttributes;
        this.publicKey = publicKey;
    }

    public static PublicProfile loadInternal(String path, String profileName, int sequence_number) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(!Utils.exists(path + profileName)) {
            controller.notifyObservers(new OutputEvent.NoSuchProfileEvent(profileName, sequence_number, false));
            return null;
        }

        if(!Utils.exists(path + profileName + "/" + sequence_number)) {
            controller.notifyObservers(new OutputEvent.NoSuchProfileEvent(profileName, sequence_number, true));
            return null;
        }

        FileInputStream fis = new FileInputStream(path + profileName + "/" + sequence_number);
        AES_InputStream aesis = AES_InputStream.from_ecb(fis, AES_BUFFER_SIZE, controller.getProgramPasswordHash());
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        String[] profileParams = Utils.bytesToStringArray(sliceReader.next());

        String creationDate = profileParams[0];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 1);
        String[] dynamicAttributes = Utils.sliceStringArray(profileParams, 5, profileParams.length);

        byte[] publicKey = sliceReader.next();
        aesis.close();
        return new PublicProfile(profileName, sequence_number, creationDate, validityPeriod, dynamicAttributes, publicKey);
    }

    public static PublicProfile fromSliceReader(Utils.SliceReader sliceReader) throws IOException, NoSuchAlgorithmException {
        String[] profileParams = Utils.bytesToStringArray(sliceReader.next());
        String public_profile_name = profileParams[0];
        int sequence_number = Integer.parseInt(profileParams[1]);
        String creationDate = profileParams[2];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 3);
        String[] dynamic_attributes = Utils.sliceStringArray(profileParams, 7, profileParams.length);
        byte[] public_profile_b = sliceReader.next();
        return new PublicProfile(public_profile_name, sequence_number, creationDate, validityPeriod, dynamic_attributes, public_profile_b);
    }

    public static PublicProfile fromExternal(InputStream inputStream, byte[]password_hash) throws IOException, NoSuchAlgorithmException {
        if(!controller.validateCryptoPassword(inputStream, password_hash))
            return null;
        Utils.SliceReader sliceReader = new Utils.SliceReader(inputStream);
        return fromSliceReader(sliceReader);
    }

    protected static boolean isIDaggregated(String profileName, int sequenceNumber, int mode, String url) throws Exception {
        File folder = new File(controller.appDataLocation + url);
        for(String idNumber: Objects.requireNonNull(folder.list())) {
            Personal_ID personalId = Personal_ID.loadInternal(mode, idNumber, false);
            assert personalId != null;
            PublicProfile profile = personalId.publicProfile;
            if(profile.name.equals(profileName) && profile.sequence_number == sequenceNumber)
                return true;
        }
        return false;
    }

    public static boolean isIDaggregated(String profileName, int sequenceNumber) throws Exception {
        return isIDaggregated(profileName, sequenceNumber, LOAD_FROM_IMPORTED, strImportedPersonalIDs);
    }

    public void saveExternal(OutputStream os, String password, int type) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        os.write(PROGRAM_WATERMARK);
        os.write(type);
        byte[]password_hash = Utils.passwordHash(password);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(os, AES_BUFFER_SIZE, password_hash);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        aesos.write(password_hash);
        toSliceWriter(sliceWriter);
        aesos.close();
    }

    public void toSliceWriter(Utils.SliceWriter sliceWriter) throws IOException {
        sliceWriter.write(toByteArray(true));
        sliceWriter.write(publicKey);
    }

    public void saveInternal(String url) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        String path = url + name + "/" + sequence_number;
        if(Utils.exists(path + name) && Utils.exists(path)) {
            controller.notifyObservers(new OutputEvent.ProfileAlreadyExistsEvent());
            return;
        }
        File destination = Utils.createFileAndSubfolder(path);
        FileOutputStream fos = new FileOutputStream(destination);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, controller.getProgramPasswordHash());
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        sliceWriter.write(toByteArray(false));
        sliceWriter.write(publicKey);
        aesos.close();
    }

    @Override
    public String toString() {
        return "Profilname:\n" +
                name +
                "\nFolgenummer:\n" +
                sequence_number +
                "\nErstellt\n" +
                created +
                '\n' +
                validityPeriod +
                "Dynamische Attribute:\n" +
                Utils.stringArrayToLines(dynamicAttributes);
    }

    public byte[] toByteArray(boolean addNameAndSequence) throws IOException {
        Utils.LineWriter lineWriter = new Utils.LineWriter();
        if(addNameAndSequence) {
            lineWriter.write(name);
            lineWriter.write(String.valueOf(sequence_number));
        }
        lineWriter.write(created);
        lineWriter.write_byte(validityPeriod.toByteArray());
        lineWriter.write(Utils.stringArrayToLines(dynamicAttributes));
        return lineWriter.get_bytes();
    }

    public static class ValidityPeriod {
        public final String validFrom;
        public final String validUntilForCreation;
        public final String validUntilForCreated;
        public final int maxValidDays;
        public ValidityPeriod(String validFrom, String validUntilForCreation, String validUntilForCreated, int maxValidDays) {
            this.validFrom = validFrom;
            this.validUntilForCreation = validUntilForCreation;
            this.validUntilForCreated =validUntilForCreated;
            this.maxValidDays = maxValidDays;
        }

        @Override
        public String toString() {
            return "Gültig ab:\n" +
                    validFrom +
                    "\nGültig bis, bezüglich Erstellung:\n" +
                    validUntilForCreation +
                    "\nGültig bis, bezüglich Gültigkeit erstellter Ausweise:\n" +
                    validUntilForCreated +
                    "\nMaximale Gültigkeit Tage:\n" +
                    maxValidDays +
                    '\n';
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof ValidityPeriod))
                return false;
            ValidityPeriod v = (ValidityPeriod) obj;
            return validFrom.equals(v.validFrom) && validUntilForCreation.equals(v.validUntilForCreation) &&
                    validUntilForCreated.equals(v.validUntilForCreated) && maxValidDays == v.maxValidDays;
        }

        public byte[] toByteArray() throws IOException {
            Utils.LineWriter lineWriter = new Utils.LineWriter();
            lineWriter.write(validFrom);
            lineWriter.write(validUntilForCreation);
            lineWriter.write(validUntilForCreated);
            lineWriter.write(String.valueOf(maxValidDays));
            return lineWriter.get_bytes();
        }

        public static ValidityPeriod fromStringArray(String[]s, int start) {
            return new ValidityPeriod(s[start], s[1 + start], s[2 + start], Integer.parseInt(s[3 + start]));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PublicProfile))
            return false;
        PublicProfile p = (PublicProfile) obj;
        return name.equals(p.name) && sequence_number == p.sequence_number && created.equals(p.created) &&
                validityPeriod.equals(p.validityPeriod) && Arrays.equals(dynamicAttributes, p.dynamicAttributes) &&
                Arrays.equals(publicKey, p.publicKey);
    }
}

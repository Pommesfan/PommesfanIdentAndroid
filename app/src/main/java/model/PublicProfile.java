package model;

import controller.Controller;
import utils.OutputEvent;
import utils.Utils;
import java.io.*;

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

    public static PublicProfile loadInternal(Controller controller, String path, String profileName, int sequence_number) throws IOException {
        if(!Utils.exists(path + profileName)) {
            controller.notifyObservers(new OutputEvent.NoSuchProfileEvent(profileName, sequence_number, false));
            return null;
        }

        if(!Utils.exists(path + profileName + "/" + sequence_number)) {
            controller.notifyObservers(new OutputEvent.NoSuchProfileEvent(profileName, sequence_number, true));
            return null;
        }

        FileInputStream fis = new FileInputStream(path + profileName + "/" + sequence_number);
        Utils.SliceReader sliceReader = new Utils.SliceReader(fis);
        String[] profileParams = Utils.bytesToStringArray(sliceReader.next());

        String creationDate = profileParams[0];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 1);
        String[] dynamicAttributes = Utils.sliceStringArray(profileParams, 5, profileParams.length);

        byte[] publicKey = sliceReader.next();
        fis.close();
        return new PublicProfile(profileName, sequence_number, creationDate, validityPeriod, dynamicAttributes, publicKey);
    }

    public static PublicProfile fromExternal(InputStream inputStream) throws IOException {
        Utils.SliceReader sliceReader = new Utils.SliceReader(inputStream);
        String[] profileParams = Utils.bytesToStringArray(sliceReader.next());
        String public_profile_name = profileParams[0];
        int sequence_number = Integer.parseInt(profileParams[1]);
        String creationDate = profileParams[2];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 3);
        String[] dynamic_attributes = Utils.sliceStringArray(profileParams, 7, profileParams.length);
        byte[] public_profile_b = sliceReader.next();
        inputStream.close();
        return new PublicProfile(public_profile_name, sequence_number, creationDate, validityPeriod, dynamic_attributes, public_profile_b);
    }

    public void saveExternal(File destination) throws IOException {
        FileOutputStream fos = new FileOutputStream(destination);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(fos);
        sliceWriter.write(toByteArray(true));
        sliceWriter.write(publicKey);
    }

    public void saveInternal(Controller controller, String path) throws IOException {
        if(Utils.exists(path + name) && Utils.exists(path + name + "/" + sequence_number)) {
            controller.notifyObservers(new OutputEvent.ProfileAlreadyExistsEvent());
            return;
        }
        File destination = Utils.createFileAndSubfolder(path + name + "/" + sequence_number);
        FileOutputStream fos = new FileOutputStream(destination);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(fos);
        sliceWriter.write(toByteArray(false));
        sliceWriter.write(publicKey);
        fos.close();
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
}

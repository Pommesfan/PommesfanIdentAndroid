package model;

import controller.Controller;
import utils.OutputEvent;
import utils.Utils;
import java.io.*;

public class PrivateProfile extends PublicProfile{
    public final byte[] privateKey;
    public PrivateProfile(String name, int sequence_number, String created, ValidityPeriod validityPeriod, String[] dynamicAttributes, byte[] publicKey, byte[] privateKey) {
        super(name, sequence_number, created, validityPeriod, dynamicAttributes, publicKey);
        this.privateKey = privateKey;
    }

    public void saveInternal(Controller controller, String url) throws IOException {
        if(Utils.exists(url)) {
            controller.notifyObservers(new OutputEvent.ProfileAlreadyExistsEvent());
            return;
        }
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(fos);
        sliceWriter.write(toByteArray(false));
        sliceWriter.write(privateKey);
        sliceWriter.write(publicKey);
        fos.close();
    }

    public static PrivateProfile fromInternalFile(Controller controller, String path, String profileName, int sequence_number) throws IOException {
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
        String created = profileParams[0];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 1);
        String[] dynamicAttributes = Utils.sliceStringArray(profileParams, 5, profileParams.length);
        byte[] privateKey = sliceReader.next();
        byte[] publicKey = sliceReader.next();
        fis.close();
        return new PrivateProfile(profileName, sequence_number, created, validityPeriod, dynamicAttributes, publicKey, privateKey);
    }
}

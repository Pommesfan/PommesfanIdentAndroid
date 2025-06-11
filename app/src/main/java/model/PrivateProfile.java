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

import static controller.Controller.AES_BUFFER_SIZE;

public class PrivateProfile extends PublicProfile{
    public final byte[] privateKey;
    public PrivateProfile(String name, int sequence_number, String created, ValidityPeriod validityPeriod, String[] dynamicAttributes, byte[] publicKey, byte[] privateKey) {
        super(name, sequence_number, created, validityPeriod, dynamicAttributes, publicKey);
        this.privateKey = privateKey;
    }

    public void saveInternal(Controller controller, String url) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(Utils.exists(url)) {
            controller.notifyObservers(new OutputEvent.ProfileAlreadyExistsEvent());
            return;
        }
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        AES_OutputStream aesos = new AES_OutputStream(fos, AES_BUFFER_SIZE, controller.getProgrammPassword());
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        sliceWriter.write(toByteArray(false));
        sliceWriter.write(privateKey);
        sliceWriter.write(publicKey);
        aesos.close();
    }

    public static PrivateProfile fromInternalFile(Controller controller, String path, String profileName, int sequence_number) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(!Utils.exists(path + profileName)) {
            controller.notifyObservers(new OutputEvent.NoSuchProfileEvent(profileName, sequence_number, false));
            return null;
        }
        if(!Utils.exists(path + profileName + "/" + sequence_number)) {
            controller.notifyObservers(new OutputEvent.NoSuchProfileEvent(profileName, sequence_number, true));
            return null;
        }
        FileInputStream fis = new FileInputStream(path + profileName + "/" + sequence_number);
        AES_InputStream aesis = new AES_InputStream(fis, AES_BUFFER_SIZE, controller.getProgrammPassword());
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        String[] profileParams = Utils.bytesToStringArray(sliceReader.next());
        String created = profileParams[0];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 1);
        String[] dynamicAttributes = Utils.sliceStringArray(profileParams, 5, profileParams.length);
        byte[] privateKey = sliceReader.next();
        byte[] publicKey = sliceReader.next();
        aesis.close();
        return new PrivateProfile(profileName, sequence_number, created, validityPeriod, dynamicAttributes, publicKey, privateKey);
    }
}

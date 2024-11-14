package model;

import controller.Controller;
import utils.OutputEvent;
import utils.Utils;

import java.io.*;

public class PrivateProfile extends PublicProfile{
    public final byte[] privateKey;
    public PrivateProfile(String name, String[] dynamicAttributes, byte[] publicKey, byte[] privateKey) {
        super(name, dynamicAttributes, publicKey);
        this.privateKey = privateKey;
    }

    public void saveInternal(String url) throws IOException {
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> fos.write(data));
        sliceWriter.write(Utils.stringArrayToLines(dynamicAttributes).getBytes());
        sliceWriter.write(privateKey);
        sliceWriter.write(publicKey);
        fos.close();
    }

    public static PrivateProfile fromInternalFile(Controller controller, String path, String profileName) throws IOException {
        File f = new File(path + profileName);
        if(!f.exists()) {
            controller.notifyObservers(new OutputEvent.NoSuchPublicProfileEvent(profileName));
            return null;
        }
        FileInputStream fis = new FileInputStream(f);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        byte[] dynamicAttributes_b = sliceReader.next();
        byte[] privateKey = sliceReader.next();
        byte[] publicKey = sliceReader.next();
        String[] dynamicAttributes = Utils.bytesToStringArray(dynamicAttributes_b);
        fis.close();
        return new PrivateProfile(profileName, dynamicAttributes, publicKey, privateKey);
    }
}

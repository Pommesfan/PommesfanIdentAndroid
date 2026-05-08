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

import static controller.Controller.*;

public class PrivateProfile extends PublicProfile {
    public final byte[] privateKey;
    public PrivateProfile(String name, int sequence_number, String created, ValidityPeriod validityPeriod, String[] dynamicAttributes, byte[] publicKey, byte[] privateKey) {
        super(name, sequence_number, created, validityPeriod, dynamicAttributes, publicKey);
        this.privateKey = privateKey;
    }

    public void saveInternal(String url) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(Utils.exists(url)) {
            controller.notifyObservers(new OutputEvent.ProfileAlreadyExistsEvent());
            return;
        }
        File f = Utils.createFileAndSubfolder(url);
        FileOutputStream fos = new FileOutputStream(f);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, controller.getProgramPasswordHash());
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        sliceWriter.write(toByteArray(false));
        sliceWriter.write(publicKey);
        sliceWriter.write(privateKey);
        aesos.close();
    }

    public static PrivateProfile fromInternalFile(String path, String profileName, int sequence_number) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
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
        String created = profileParams[0];
        ValidityPeriod validityPeriod = ValidityPeriod.fromStringArray(profileParams, 1);
        String[] dynamicAttributes = Utils.sliceStringArray(profileParams, 5, profileParams.length);
        byte[] publicKey = sliceReader.next();
        byte[] privateKey = sliceReader.next();
        aesis.close();
        return new PrivateProfile(profileName, sequence_number, created, validityPeriod, dynamicAttributes, publicKey, privateKey);
    }

    public static PrivateProfile fromExternal(InputStream inputStream, byte[]password_hash) throws IOException, NoSuchAlgorithmException {
        if(!controller.validateCryptoPassword(inputStream, password_hash))
            return null;
        Utils.SliceReader sliceReader = new Utils.SliceReader(inputStream);
        PublicProfile p = PublicProfile.fromSliceReader(sliceReader);
        byte[]private_key_b = sliceReader.next();
        return new PrivateProfile(p.name, p.sequence_number, p.created, p.validityPeriod, p.dynamicAttributes, p.publicKey, private_key_b);
    }

    public void saveExternal(File destination, String password) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        FileOutputStream fos = new FileOutputStream(destination);
        fos.write(PROGRAM_WATERMARK);
        fos.write(Utils.int_to_bytes(FILE_TYPE_PRIVATE_PROFILE));
        byte[]password_hash = Utils.passwordHash(password);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE, password_hash);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        aesos.write(password_hash);
        sliceWriter.write(toByteArray(true));
        sliceWriter.write(publicKey);
        sliceWriter.write(privateKey);
        aesos.close();
    }

    public PublicProfile toPublic() {
        return new PublicProfile(name, sequence_number, created, validityPeriod, dynamicAttributes, publicKey);
    }
}

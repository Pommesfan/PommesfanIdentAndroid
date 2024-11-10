package com.example.model;

import com.example.utils.Utils;

import java.io.*;

public class PublicProfile {
    public final String name;
    public final String[] dynamicAttributes;
    public final byte[] publicKey;
    public PublicProfile(String name, String[] dynamicAttributes, byte[] publicKey) {
        this.name = name;
        this.dynamicAttributes = dynamicAttributes;
        this.publicKey = publicKey;
    }

    public static PublicProfile loadInternal(String path, String fileName) throws IOException {
        FileInputStream fis2 = new FileInputStream(path + fileName);
        Utils.SliceReader sliceReader2 = new Utils.SliceReader((data, length) -> fis2.read(data, 0, length));
        byte[] dynamicAttributes_b = sliceReader2.next();
        byte[] publicKey = sliceReader2.next();
        String[] dynamicAttributes = Utils.bytesToStringArray(dynamicAttributes_b);
        fis2.close();
        return new PublicProfile(fileName, dynamicAttributes, publicKey);
    }

    public static PublicProfile fromExternal(File publicProfileFile) throws IOException {
        FileInputStream fis = new FileInputStream(publicProfileFile);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        String public_profile_name = new String(sliceReader.next());
        byte[] dynamic_attributes_b = sliceReader.next();
        byte[] public_profile_b = sliceReader.next();
        fis.close();
        return new PublicProfile(public_profile_name, Utils.bytesToStringArray(dynamic_attributes_b), public_profile_b);
    }

    public void saveExternal(File destination) throws IOException {
        FileOutputStream fos = new FileOutputStream(destination);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> fos.write(data));
        sliceWriter.write(name.getBytes());
        sliceWriter.write(Utils.stringArrayToLines(dynamicAttributes).getBytes());
        sliceWriter.write(publicKey);
    }

    public void saveInternal(String path) throws IOException {
        File destination = Utils.createFileAndSubfolder(path + name);
        FileOutputStream fos = new FileOutputStream(destination);
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(data -> fos.write(data));
        sliceWriter.write(Utils.stringArrayToLines(dynamicAttributes).getBytes());
        sliceWriter.write(publicKey);
        fos.close();
    }
}

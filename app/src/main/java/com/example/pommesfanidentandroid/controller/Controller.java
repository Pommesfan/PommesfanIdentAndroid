package com.example.pommesfanidentandroid.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

public class Controller {
    public static void generateKeyPair(String path) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator gpk = KeyPairGenerator.getInstance("RSA");
        gpk.initialize(2048);
        KeyPair keyPair = gpk.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        File f = new File(path+ "/private");
        f.getParentFile().mkdirs();
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(privateKey.getEncoded());
        fos.close();

        f = new File(path + "/public");
        f.getParentFile().mkdirs();
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(publicKey.getEncoded());
        fos.close();
    }
}

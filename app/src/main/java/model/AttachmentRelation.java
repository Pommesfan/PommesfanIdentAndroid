package model;

import utils.AES_InputStream;
import utils.AES_OutputStream;
import utils.Utils;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static controller.Controller.*;

public class AttachmentRelation {
    public static final int AES_BUFFER_SIZE_RELATION = 256;
    private final List<String[]> data; //
    public final String filePath;

    public AttachmentRelation(String filePath, List<String[]> data) {
        this.filePath = filePath;
        this.data = data;
    }

    public void insertRelation(String imageFileName, String originalFileName, String idNumber) {
        data.add(new String[]{imageFileName, originalFileName, idNumber});
    }

    public String getImageFileName(String idNumber) {
        for(String[] attributes: data) {
            if(attributes[2].equals(idNumber))
                return attributes[0];
        }
        throw new NoSuchElementException("idNumber not found");
    }

    public Set<String> getSameFileNames(String originalFileName) {
        TreeSet<String> res = new TreeSet<>();
        for(String[] attributes: data) {
            if(attributes[1].equals(originalFileName))
                res.add(attributes[0]);
        }
        return res;
    }

    public void save() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        FileOutputStream fos = new FileOutputStream(filePath);
        AES_OutputStream aesos = AES_OutputStream.from_ecb(fos, AES_BUFFER_SIZE_RELATION, controller.getProgramPasswordHash());
        aesos.write(data.size());
        Utils.SliceWriter sliceWriter = new Utils.SliceWriter(aesos);
        for (String[]arguments: data) {
            sliceWriter.writeLine(arguments[0] + ":" + arguments[1] + ":" + arguments[2]);
        }
        aesos.close();
    }

    public String removeID(String idNumber) {
        for (String[]arguments: data) {
            if(arguments[2].equals(idNumber)) {
                data.remove(arguments);
                return arguments[0];
            }
        }
        throw new NoSuchElementException("no such id number");
    }

    public boolean hasImage(String imageID) {
        for (String[]arguments: data) {
            if(arguments[0].equals(imageID))
                return true;
        }
        return false;
    }

    public boolean hasID(String idNumber) {
        for (String[]arguments: data) {
            if(arguments[2].equals(idNumber))
                return true;
        }
        return false;
    }

    public static AttachmentRelation getRelation(int attachmentMode) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        String filePath;
        if(attachmentMode == ATTACHMENT_PERSONAL_IMAGE) {
            filePath = controller.appDataLocation + strPersonalImageRelations;
        } else if(attachmentMode == ATTACHMENT_HAND_SIGNATURE) {
            filePath = controller.appDataLocation + strHandSignaturesRelations;
        } else {
            throw new IllegalArgumentException("not such relation type");
        }
        FileInputStream fis = new FileInputStream(filePath);
        AES_InputStream aesis = AES_InputStream.from_ecb(fis, AES_BUFFER_SIZE_RELATION, controller.getProgramPasswordHash());
        int numberOfLines = aesis.read();
        Utils.SliceReader sliceReader = new Utils.SliceReader(aesis);
        List<String[]> data = new LinkedList<>();
        for (int i = 0; i < numberOfLines; i++) {
            String[]arguments = sliceReader.readLine().split(":");
            if(arguments.length != 3)
                continue;
            data.add(arguments);
        }
        aesis.close();
        return new AttachmentRelation(filePath, data);
    }

    public static String attachmentPath(int attachmentMode) {
        if(attachmentMode == ATTACHMENT_PERSONAL_IMAGE)
            return controller.appDataLocation + strPersonalImages;
        else if(attachmentMode == ATTACHMENT_HAND_SIGNATURE)
            return controller.appDataLocation + strHandSignatures;
        else
            throw new IllegalArgumentException("not such relation type");
    }
}

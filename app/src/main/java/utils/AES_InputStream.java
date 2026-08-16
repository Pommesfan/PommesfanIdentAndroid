package utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AES_InputStream extends InputStream {
    private final InputStream inputStream;
    public final int buf_len;
    private byte[] buf;
    private int received_size;
    private int buf_position = 0;
    private final Cipher cipher;
    public AES_InputStream(InputStream inputStream, int buf_len, Cipher cipher) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (buf_len % 16 != 0)
            throw new IllegalArgumentException("buf_size of AES_InputStream must be multiple of 16");
        this.inputStream = inputStream;
        this.buf_len = buf_len;
        this.cipher = cipher;
    }

    public static AES_InputStream from_ecb(InputStream inputStream, int buf_size, byte[] passwordHash) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        SecretKeySpec sks = new SecretKeySpec(passwordHash, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, sks);
        return new AES_InputStream(inputStream, buf_size, cipher);
    }

    @Override
    public int read() throws IOException {
        byte[]b = new byte[4];
        read(b, 0, 4);
        return Utils.bytes_to_int(b);
    }

    private void from_inputstream() {
        try {
            received_size = inputStream.read(buf, 0, buf_len);
            buf = cipher.doFinal(buf);
            buf_position = 0;
        } catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if(buf == null) {
            buf = new byte[buf_len];
            from_inputstream();
        }
        if(received_size == -1)
            return - 1;

        int b_position = 0;

        while(b_position < b.length) {
            int buf_remaining = received_size - buf_position;
            int b_remaining = b.length - b_position;

            if(b_remaining < buf_remaining) {
                System.arraycopy(buf, buf_position, b, b_position, b_remaining);
                b_position += b_remaining;
                buf_position += b_remaining;
            } else if (b_remaining == buf_remaining) {
                System.arraycopy(buf, buf_position, b, b_position, b_remaining);
                b_position += b_remaining;
                buf = null; // after flush on other side, receive new data on calling this read()
            } else {
                System.arraycopy(buf, buf_position, b, b_position, buf_remaining);
                from_inputstream();
                if(received_size == -1)
                    return - 1;
                b_position += buf_remaining;
            }
        }
        return b_position;
    }
    @Override
    public void close() throws IOException {
        inputStream.close();
        super.close();
    }
}
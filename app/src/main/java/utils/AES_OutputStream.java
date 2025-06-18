package utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AES_OutputStream extends OutputStream {
    private final OutputStream outputStream;
    private byte[]buf;
    private int buf_position = 0;
    private final Cipher cipher;
    public AES_OutputStream(OutputStream outputStream, int buf_size, Cipher cipher) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (buf_size % 16 != 0)
            throw new IllegalArgumentException("buf_size of AES_InputStream must be multiple of 16");
        this.outputStream = outputStream;
        this.buf = new byte[buf_size];
        this.cipher = cipher;
    }

    public static AES_OutputStream from_ecb(OutputStream outputStream, int buf_size, byte[] passwordHash) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        SecretKeySpec sks = new SecretKeySpec(passwordHash, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, sks);
        return new AES_OutputStream(outputStream, buf_size, cipher);
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(new byte[]{Integer.valueOf(b).byteValue()});
    }

    @Override
    public void write(byte[]b) {
        System.gc();
        int buf_len = buf.length;
        int start = 0;
        while(start < b.length) {
            int end;
            if(b.length - start > buf_len) {
                end = start + buf_len;
            } else {
                end = b.length;
            }

            int chunk_len = end - start;
            int remaining_size = buf_len - buf_position;
            if(remaining_size > chunk_len) {
                System.arraycopy(b, start, buf, buf_position, chunk_len);
                buf_position += chunk_len;
                start += chunk_len;
            } else if (remaining_size == chunk_len) {
                System.arraycopy(b, start, buf, buf_position, chunk_len);
                toOutputStream(buf_len);
                start += chunk_len;
            } else {
                int overflow_pos = start + remaining_size;
                System.arraycopy(b, start, buf, buf_position, overflow_pos);
                toOutputStream(buf_len);
                System.arraycopy(b, overflow_pos, buf, buf_position, chunk_len - overflow_pos);
                start += overflow_pos;
            }
        }
        System.gc();
    }

    private void toOutputStream(int len) {
        try {
            if(len == buf.length)
                outputStream.write(cipher.doFinal(buf));
            else {
                // add zeros to last package, so it becomes multiple of 16 in length
                int output_size = buf_position;
                int rest = 16 - (output_size % 16);
                if(rest != 16)
                    output_size += rest;
                byte[] tmp = new byte[output_size];
                System.arraycopy(buf, 0, tmp, 0, len);
                outputStream.write(cipher.doFinal(tmp));
            }
            buf_position = 0;
        } catch (IllegalBlockSizeException | IOException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        toOutputStream(buf_position);
        super.flush();
    }

    @Override
    public void close() throws IOException {
        toOutputStream(buf_position);
        outputStream.close();
        super.close();
    }
}
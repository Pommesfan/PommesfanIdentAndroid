package utils;

import android.os.Looper;
import controller.Controller;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;

public abstract class BackgroundRunner {
    private Thread t;
    protected ServerSocket serverSocket;
    protected byte[] crypto_hash;

    protected abstract void routine() throws Exception;

    public BackgroundRunner() {
        t = new Thread(() -> {
            try {
                // for Android
                Looper.prepare();
                routine();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void init() throws NoSuchAlgorithmException, IOException {
        serverSocket = new ServerSocket(0);
        String crypto = Utils.getAlphanumeric(16);
        crypto_hash = Utils.passwordHash(crypto);
        String ip = InetAddress.getLocalHost().getHostAddress();
        Controller.controller.notifyObservers(new OutputEvent.ServerStartedEvent(ip, serverSocket.getLocalPort(), crypto));
    }

    public void start() {
        t.start();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }
}
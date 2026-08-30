package utils;

import android.os.Looper;
import controller.Controller;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public abstract class BackgroundRunner {
    private Thread t;
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

    public void start() {
        t.start();
    }
    public abstract void stop() throws IOException;

    public abstract static class NetworkBackgroundRunner extends BackgroundRunner {
        protected ServerSocket serverSocket;

        public NetworkBackgroundRunner() {
            super();
        }
        public void init() throws NoSuchAlgorithmException, IOException {
            serverSocket = new ServerSocket(0);
            String crypto = Utils.getAlphanumeric(16);
            crypto_hash = Utils.passwordHash(crypto);
            String ip = InetAddress.getLocalHost().getHostAddress();
            Controller.controller.notifyObservers(new OutputEvent.NetworkServerStartedEvent(ip, serverSocket.getLocalPort(), crypto));
        }
        @Override
        public void stop() throws IOException {
            Socket s = new Socket(InetAddress.getLocalHost().getHostAddress(), getPort());
            s.getOutputStream().write(1);
        }
        public int getPort() {
            return serverSocket.getLocalPort();
        }
    }
}
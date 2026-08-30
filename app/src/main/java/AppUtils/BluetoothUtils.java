package AppUtils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import controller.Controller;
import model.Personal_ID;
import model.PublicProfile;
import utils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import static controller.Controller.*;
import static controller.Controller.strPublicProfiles;

public class BluetoothUtils {
    public final static String BLUETOOTH_UUID = "3df1b6dd-1e4e-4dcb-9d6d-7e455b6f08fa";
    public static class BluetoothServerStartedEvent implements OutputEvent {
        public final String deviceName;
        public final String password;
        public BluetoothServerStartedEvent(String pDeviceName, String pPassword) {
            deviceName = pDeviceName;
            password = pPassword;
        }
    }
    public static abstract class BluetoothBackroundRunner extends BackgroundRunner {
        private final Activity activity;
        private BluetoothServerSocket mmServerSocket;
        private boolean cancelled = false;
        public BluetoothBackroundRunner(Activity pActivity) {
            activity = pActivity;
        }
        protected BluetoothSocket init() throws IOException, NoSuchAlgorithmException {
            BluetoothManager bluetoothManager = activity.getSystemService(BluetoothManager.class);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                // Toast.makeText(this, "Bluetooth auf diesem Gerät nicht unterstützt", Toast.LENGTH_LONG).show();
                return null;
            }
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                activity.startActivityForResult(enableBtIntent, 0);
            }

            // https://stackoverflow.com/questions/70245463/java-lang-securityexception-need-android-permission-bluetooth-connect-permissio
            int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        1
                );
            }
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                mmServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("PommesFanIdent", UUID.fromString(BLUETOOTH_UUID));
            } catch (Exception e) {
                throw new RuntimeException();
            }
            String crypto = Utils.getAlphanumeric(16);
            crypto_hash = Utils.passwordHash(crypto);
            controller.notifyObservers(new BluetoothServerStartedEvent(bluetoothAdapter.getName(), crypto));
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned.
            try {
                socket = mmServerSocket.accept();
                System.out.println();
            } catch (IOException e) {
                if(!cancelled)
                    throw new RuntimeException();
                else
                    return null;
            }
            mmServerSocket.close();
            return socket;
        }
        @Override
        public void stop() throws IOException {
            mmServerSocket.close();
            cancelled = true;
        }
    }
    public static class ImportOverBluetoothRunner extends BluetoothBackroundRunner {
        public final String idNumber;
        public ImportOverBluetoothRunner(String pIdNumber, Activity pActivity) {
            super(pActivity);
            idNumber = pIdNumber;
        }
        @Override
        protected void routine() throws Exception {
            BluetoothSocket socket = init();
            if(socket == null)
                return;
            Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_CREATED, idNumber, true, true);
            if(personalId == null)
                return;
            OutputStream os = AES_OutputStream.from_ecb(socket.getOutputStream(), AES_BUFFER_SIZE, crypto_hash);
            personalId.publicProfile.toSliceWriter(new Utils.SliceWriter(os));
            personalId.toSliceWriter(new Utils.SliceWriter(os), true);
            os.close();
        }
    }
    public static class CheckIDoverBluetoothRunner extends BluetoothBackroundRunner {
        public CheckIDoverBluetoothRunner(Activity pActivity) {
            super(pActivity);
        }
        @Override
        protected void routine() throws Exception {
            BluetoothSocket socket = init();
            if(socket == null)
                return;
            InputStream inputStream = AES_InputStream.from_ecb(socket.getInputStream(), AES_BUFFER_SIZE, crypto_hash);
            Utils.SliceReader sliceReader = new Utils.SliceReader(inputStream);
            Personal_ID personalId = Personal_ID.fromSliceReader(LOAD_FROM_IMPORTED, sliceReader, true, true);
            socket.close();
            if (personalId == null)
                return;

            Controller c = controller;
            controller.checkIDrunnerRes = personalId;

            if (c.validateSignature(personalId)) {
                c.notifyObservers(new OutputEvent.PersonalIDValidEvent(personalId.toString()));
            } else {
                c.notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            }
        }
    }

    private static BluetoothSocket connectClient(Activity activity, String deviceName) {
        BluetoothManager bluetoothManager = activity.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, "Bluetooth auf diesem Gerät nicht unterstützt", Toast.LENGTH_LONG).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            activity.startActivityForResult(enableBtIntent, 0);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        BluetoothDevice bluetoothDevice = null;
        if (!pairedDevices.isEmpty()) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if(name.equals(deviceName)) {
                    bluetoothDevice = device;
                    break;
                }
            }
        }
        if(bluetoothDevice == null)
            return null;
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket socket;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_UUID));
        } catch (Exception e) {
            throw new RuntimeException();
        }

        // bluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                socket.close();
            } catch (IOException closeException) {
                throw new RuntimeException();
            }
            return null;
        }
        return socket;
    }
    public static void importFromBluetooth(Controller controller, String deviveName, String crypto, Activity activity) throws Exception {
        BluetoothSocket socket = connectClient(activity, deviveName);
        InputStream inputStream = AES_InputStream.from_ecb(socket.getInputStream(), AES_BUFFER_SIZE, Utils.passwordHash(crypto));
        PublicProfile publicProfile = PublicProfile.fromSliceReader(new Utils.SliceReader(inputStream));
        if(Files.exists(Paths.get(controller.appDataLocation + strPublicProfiles + publicProfile.name + "/" + publicProfile.sequence_number))) {
            PublicProfile saved = PublicProfile.loadInternal(
                    controller.appDataLocation + strPublicProfiles, publicProfile.name, publicProfile.sequence_number, true);
            if(!saved.equals(publicProfile)) {
                controller.notifyObservers(new OutputEvent.OtherProfileFoundEvent());
                return;
            }
        } else {
            publicProfile.saveInternal(controller.appDataLocation + strPublicProfiles + "/");
        }

        Personal_ID personalId = Personal_ID.fromSliceReader(LOAD_FROM_IMPORTED, new Utils.SliceReader(inputStream), true, true);
        socket.close();
        if(personalId == null) {
            return;
        }
        Controller c = Controller.controller;
        if(!c.validateSignature(personalId)) {
            c.notifyObservers(new OutputEvent.PersonalIDInvalidEvent());
            return;
        }
        personalId.saveInternal(LOAD_FROM_IMPORTED);
        c.notifyObservers(new OutputEvent.DummyEvent());
    }

    public static void handInOverBluetooth(Activity activity, String idNumber, String deviceName, String crypto) throws Exception {
        BluetoothSocket socket = connectClient(activity, deviceName);
        Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_IMPORTED, idNumber.toUpperCase(), true, true);
        if (personalId == null) {
            return;
        }
        OutputStream os = AES_OutputStream.from_ecb(socket.getOutputStream(), AES_BUFFER_SIZE, Utils.passwordHash(crypto));
        personalId.toSliceWriter(new Utils.SliceWriter(os), true);
        os.close();
        controller.notifyObservers(new OutputEvent.IDhandedInSuccessEvent());
    }
}

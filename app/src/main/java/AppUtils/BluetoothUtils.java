package AppUtils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import controller.Controller;
import model.Personal_ID;
import utils.BackgroundRunner;
import utils.OutputEvent;
import utils.Utils;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import static androidx.fragment.app.FragmentManager.TAG;
import static controller.Controller.LOAD_FROM_CREATED;

public class BluetoothUtils {
    public static class BluetoothServerStartedEvent implements OutputEvent {
        public final String mac;
        public final String password;
        public BluetoothServerStartedEvent(String pMac, String pPassword) {
            mac = pMac;
            password = pPassword;
        }
    }
    public static class BluetoothBackroundRunner extends BackgroundRunner {
        public final String idNumber;
        private final Activity activity;
        public BluetoothBackroundRunner(String pIdNumber, Activity pActivity) {
            idNumber = pIdNumber;
            activity = pActivity;
        }
        @Override
        protected void routine() throws Exception {
            BluetoothManager bluetoothManager = activity.getSystemService(BluetoothManager.class);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                // Toast.makeText(this, "Bluetooth auf diesem Gerät icht unterstützt", Toast.LENGTH_LONG).show();
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
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

            BluetoothServerSocket mmServerSocket = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                mmServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("PommesFanIdent", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
            } catch (Exception e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            Controller.controller.notifyObservers(new BluetoothUtils.BluetoothServerStartedEvent(bluetoothAdapter.getAddress(), "fhfnflmnztoidhfg"));
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    mmServerSocket.close();
                    break;
                }
            }

            Personal_ID personalId = Personal_ID.loadInternal(LOAD_FROM_CREATED, idNumber, true, true);
            OutputStream os = new BufferedOutputStream(socket.getOutputStream());
            personalId.publicProfile.toSliceWriter(new Utils.SliceWriter(os));
            personalId.toSliceWriter(new Utils.SliceWriter(os), true);
            os.close();
            mmServerSocket.close();
        }
    }
}

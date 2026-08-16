package com.example.pommesfanidentandroid;

import AppUtils.AppGUIUtils;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import controller.Controller;
import model.Personal_ID;
import model.PublicProfile;
import utils.Observer;
import utils.OutputEvent;
import utils.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import static androidx.fragment.app.FragmentManager.TAG;
import static controller.Controller.LOAD_FROM_IMPORTED;
import static controller.Controller.strPublicProfiles;

public class MainMenu extends Activity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Controller.controller == null) {
            Controller.controller = new Controller(getFilesDir().toString() + "/");
        }
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.ownProfiles).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfilesListView.class);
            intent.putExtra("mode", AppGUIUtils.PRIVATE);
            startActivity(intent);
        });
        findViewById(R.id.importedProfiles).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfilesListView.class);
            intent.putExtra("mode", AppGUIUtils.PUBLIC);
            startActivity(intent);
        });
        findViewById(R.id.createdIDs).setOnClickListener(v -> {
            Intent intent = new Intent(this, PersonalIDsListView.class);
            intent.putExtra("mode", AppGUIUtils.CREATED);
            startActivity(intent);
        });
        findViewById(R.id.importedPersonalIDs).setOnClickListener(v -> {
            Intent intent = new Intent(this, PersonalIDsListView.class);
            intent.putExtra("mode", AppGUIUtils.IMPORTED);
            startActivity(intent);
        });
        findViewById(R.id.checkPersonalIDoverNetwork).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProvideServiceView.class);
            intent.putExtra("mode", AppGUIUtils.CHECK);
            intent.putExtra("medium", AppGUIUtils.NETWORK);
            startActivity(intent);
        });
        findViewById(R.id.checkPersonalIDoverBluetooth).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProvideServiceView.class);
            intent.putExtra("mode", AppGUIUtils.CHECK);
            intent.putExtra("medium", AppGUIUtils.BLUETOOTH);
            startActivity(intent);
        });

        findViewById(R.id.importFromNetwork).setOnClickListener(v -> showImportDialog());
        findViewById(R.id.importFromBluetooth).setOnClickListener(v -> {
            try {
                importFromBluetooth(Controller.controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        if(Controller.controller.getProgramPasswordHash() == null) {
            new PasswordDialog(this, "App-Passwort") {
                @Override
                public void onOk(String crypto_password) throws Exception {
                    if(!Controller.controller.setProgramPasswordHash(crypto_password)) {
                        System.exit(0);
                    }
                }

                @Override
                public void onCancel() {
                    System.exit(0);
                }
            };
        }
    }

    private void importFromBluetooth(Controller controller) throws Exception {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth auf diesem Gerät nicht unterstützt", Toast.LENGTH_LONG).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        BluetoothDevice bluetoothDevice = null;
        if (!pairedDevices.isEmpty()) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                bluetoothDevice = device;
                break;
            }
        }
        if(bluetoothDevice == null)
            return;
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket socket = null;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        } catch (Exception e) {
            Log.e(TAG, "Socket's create() method failed", e);
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
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
        InputStream inputStream = socket.getInputStream();
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
        personalId.saveInternal(LOAD_FROM_IMPORTED);
        inputStream.close();
    }

    public final int REQUEST_ENABLE_BT = 0;

    public void showImportDialog() {
        new NetworkDialog(this, "Über Netzwerk importieren") {
            @Override
            public void onOk(String ip, int port, String crypto) throws Exception {
                Controller.controller.importOverNetwork(ip, port, crypto);
            }
            @Override
            public void onCancel() {
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        Controller.controller.deleteObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Controller.controller.addObserver(this);
    }

    @Override
    public void update(OutputEvent e) {
        Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }

    private static final int READ_QR_CODE = 49374;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case READ_QR_CODE:
                // https://www.geeksforgeeks.org/android/how-to-read-qr-code-using-zxing-library-in-android/
                super.onActivityResult(requestCode, resultCode, data);
                IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                // if the intentResult is null then
                // toast a message as "cancelled"
                if (intentResult != null) {
                    String res = intentResult.getContents();
                    if (intentResult.getContents() != null) {
                        String[]resArray = res.split("\n");
                        if (resArray.length != 4 || !resArray[0].equals("PommesfanIdent")) {
                            Toast.makeText(this, "QR-Code wird nicht unterstützt", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new Thread(() -> {
                            Looper.prepare();
                            try {
                                Controller.controller.importOverNetwork(resArray[1], Integer.parseInt(resArray[2]), resArray[3]);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
        }
    }
}
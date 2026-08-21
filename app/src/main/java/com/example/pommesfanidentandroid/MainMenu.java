package com.example.pommesfanidentandroid;

import AppUtils.AppGUIUtils;
import AppUtils.BluetoothUtils;
import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;

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

        findViewById(R.id.importFromNetwork).setOnClickListener(v -> importOverNetwork());
        findViewById(R.id.importFromBluetooth).setOnClickListener(v -> importOverBluetooth());

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
    public void importOverNetwork() {
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
    public void importOverBluetooth() {
        new BluetoothDialog(this, "Über Bluetooth importieren") {
            @Override
            public void onOk(String mac, String crypto) throws Exception {
                BluetoothUtils.importFromBluetooth(Controller.controller, MainMenu.this);
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
package com.example.pommesfanidentandroid;

import AppUtils.AppGUIUtils;
import AppUtils.BluetoothUtils;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;
import java.io.IOException;
import java.util.concurrent.Semaphore;

public class ProvideServiceView extends AppCompatActivity implements Observer<OutputEvent> {
    private String ipAddress;
    private final Semaphore semaphore = new Semaphore(0);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provide_service_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewProvideService), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void setQRcode(String qrTxt) throws InterruptedException, WriterException {
        ImageView qrCode = findViewById(R.id.qrCode);
        // https://www.geeksforgeeks.org/android/how-to-generate-qr-code-in-android/
        BarcodeEncoder encoder = new BarcodeEncoder();
        Bitmap bitmap = encoder.encodeBitmap(qrTxt, BarcodeFormat.QR_CODE, 600, 600);
        qrCode.setImageBitmap(bitmap);
    }

    public void updateFields(OutputEvent e) {
        String qr = "PommesfanIdent\n";
        TextView password = findViewById(R.id.crypto_password);
        if(e instanceof OutputEvent.NetworkServerStartedEvent) {
            OutputEvent.NetworkServerStartedEvent evt = (OutputEvent.NetworkServerStartedEvent)e;
            ((TextView)findViewById(R.id.ip_address)).setText(ipAddress);
            ((TextView)findViewById(R.id.port)).setText(String.valueOf(evt.port));
            password.setText(evt.password);
            qr += ipAddress + "\n" + evt.port + "\n" + evt.password;
        } else if(e instanceof BluetoothUtils.BluetoothServerStartedEvent) {
            BluetoothUtils.BluetoothServerStartedEvent evt = (BluetoothUtils.BluetoothServerStartedEvent)e;
            ((TextView)findViewById(R.id.mac_address)).setText(evt.mac);
            password.setText(evt.password);
            qr += evt.mac + "\n" + evt.password;
        } else {
            return;
        }
        try {
            setQRcode(qr);
        } catch (InterruptedException | WriterException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void update(OutputEvent e) {
        if(e instanceof OutputEvent.NetworkServerStartedEvent || e instanceof BluetoothUtils.BluetoothServerStartedEvent) {
            updateFields(e);
            semaphore.release();
        } else if (e instanceof OutputEvent.PersonalIDValidEvent) {
            finish();
            Intent intent = new Intent(this, PersonalIDdetailView.class);
            intent.putExtra("mode", AppGUIUtils.RECEIVED);
            startActivity(intent);
        }  else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Controller.controller.deleteObserver(this);
        new Thread(() -> {
            try {
                Controller.controller.stopBackgroundRunner();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Controller.controller.addObserver(this);
        LinearLayout viewProvideService = findViewById(R.id.viewProvideService);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Intent intent = getIntent();
        int mode = intent.getIntExtra("mode", 0);
        int medium = intent.getIntExtra("medium", 0);
        try {
            if(medium == AppGUIUtils.NETWORK) {
                viewProvideService.removeView(findViewById(R.id.layoutBluetoothConnection));
                if(mode == AppGUIUtils.CHECK)
                    Controller.controller.checkPersonalIDFromRemote();
                else if (mode == AppGUIUtils.EXPORT)
                    Controller.controller.exportOverNetwork(intent.getStringExtra("idNumber"));
            } else if(medium == AppGUIUtils.BLUETOOTH) {
                viewProvideService.removeView(findViewById(R.id.layoutNetworkConnection));
                Controller.controller.startBackGroundRunner(new BluetoothUtils.BluetoothBackroundRunner(intent.getStringExtra("idNumber"), this));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Einlesen", Toast.LENGTH_SHORT).show();
            finish();
        }
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
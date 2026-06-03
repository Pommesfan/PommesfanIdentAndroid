package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.ImageView;
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
    private OutputEvent.ServerStartedEvent serverStartedEvent = null;
    private String ipAddress;
    private final Semaphore semaphore = new Semaphore(0);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provide_service_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewCheckPersonsalID), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void setQRcode() throws InterruptedException, WriterException {
        semaphore.acquire();
        String qrCodeTxt = "PommesfanIdent\n" + ipAddress + "\n" + serverStartedEvent.port + "\n" + serverStartedEvent.password;
        ImageView qrCode = findViewById(R.id.qrCode);
        // https://www.geeksforgeeks.org/android/how-to-generate-qr-code-in-android/
        BarcodeEncoder encoder = new BarcodeEncoder();
        Bitmap bitmap = encoder.encodeBitmap(qrCodeTxt, BarcodeFormat.QR_CODE, 600, 600);
        qrCode.setImageBitmap(bitmap);
    }

    public void updateFields(OutputEvent.ServerStartedEvent evt) {
        serverStartedEvent = evt;
        ((TextView)findViewById(R.id.ip_address)).setText(ipAddress);
        ((TextView)findViewById(R.id.port)).setText(String.valueOf(evt.port));
        ((TextView)findViewById(R.id.crypto_password)).setText(evt.password);
    }

    @Override
    public void update(OutputEvent e) {
        if(e instanceof OutputEvent.ServerStartedEvent) {
            updateFields((OutputEvent.ServerStartedEvent) e);
            semaphore.release();
        } else if (e instanceof OutputEvent.PersonalIDValidEvent) {
            finish();
            Intent intent = new Intent(this, PersonalIDdetailView.class);
            intent.putExtra("mode", "received");
            startActivity(intent);
        }  else {
            if(!(e instanceof OutputEvent.DummyEvent))
                Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
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
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        try {
            if(mode.equals("check"))
                Controller.controller.checkPersonalIDFromRemote();
            else if (mode.equals("export"))
                Controller.controller.exportOverNetwork(intent.getStringExtra("idNumber"));
        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Einlesen", Toast.LENGTH_SHORT).show();
            finish();
        }
        try {
            setQRcode();
        } catch (InterruptedException | WriterException e) {
            throw new RuntimeException(e);
        }
    }
}
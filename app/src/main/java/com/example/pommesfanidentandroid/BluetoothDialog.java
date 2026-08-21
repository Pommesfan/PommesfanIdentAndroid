package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.zxing.integration.android.IntentIntegrator;

public abstract class BluetoothDialog {
    public BluetoothDialog(Activity activity, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.bluetooth_dialog, null);
        builder.setView(view);
        builder.setPositiveButton("Ok", (dialog, which) -> {
            EditText mac = view.findViewById(R.id.mac);
            EditText crypto = view.findViewById(R.id.crypto);
            try {
                onOk(mac.toString(), crypto.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        builder.setNeutralButton("QR-Code", ((dialogInterface, id) -> scanQRcode(activity)));
        builder.setNegativeButton("Abbrechen", (dialog, id) -> onCancel());
        builder.setCancelable(true);
        builder.create().show();
    }

    private void scanQRcode(Activity activity) {
        // https://www.geeksforgeeks.org/android/how-to-read-qr-code-using-zxing-library-in-android/
        IntentIntegrator intentIntegrator = new IntentIntegrator(activity);
        intentIntegrator.setPrompt("Scan a barcode or QR Code");
        intentIntegrator.setOrientationLocked(false);
        intentIntegrator.initiateScan();
    }

    public abstract void onOk(String mac, String crypto) throws Exception;
    public abstract void onCancel();
}
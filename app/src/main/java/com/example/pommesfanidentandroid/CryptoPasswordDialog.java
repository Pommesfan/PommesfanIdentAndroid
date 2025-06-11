package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;

public abstract class CryptoPasswordDialog {
    public CryptoPasswordDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Krypto-Passwort");
        LinearLayout layout = new LinearLayout(activity);
        EditText input = new EditText(activity);
        layout.addView(input);
        builder.setPositiveButton("Ok", (dialog, id) -> {
            try {
                onOk(input.getText().toString().toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        builder.setNegativeButton("Abbrechen", (dialog, id) -> {
        });
        builder.setView(layout);
        builder.create().show();
    }

    public abstract void onOk(String crypto_password) throws Exception;
}
package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

public abstract class NetworkDialog {
    public NetworkDialog(Activity activity, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.network_dialog, null);
        builder.setView(view);
        builder.setPositiveButton("Ok", (dialog, which) -> {
            Thread t = new Thread(() -> {
                Looper.prepare();
                try {
                    EditText ip = view.findViewById(R.id.ip);
                    EditText port = view.findViewById(R.id.port);
                    EditText crypto = view.findViewById(R.id.crypto);
                    onOk(ip.getText().toString(), Integer.parseInt(port.getText().toString()), crypto.getText().toString().toUpperCase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        builder.setNegativeButton("Abbrechen", (dialog, id) -> onCancel());
        builder.setCancelable(true);
        builder.create().show();
    }

    public abstract void onOk(String ip, int port, String crypto) throws Exception;
    public abstract void onCancel();
}
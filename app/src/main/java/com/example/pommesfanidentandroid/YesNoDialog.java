package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;

public abstract class YesNoDialog {
    public YesNoDialog(Activity activity, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        LinearLayout layout = new LinearLayout(activity);
        builder.setPositiveButton("Ja", (dialog, id) -> onOk());
        builder.setCancelable(false);
        builder.setNegativeButton("Nein", null);
        builder.setView(layout);
        builder.create().show();
    }

    public abstract void onOk();
}
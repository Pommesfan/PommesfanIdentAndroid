package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

public abstract class PasswordDialog {
    public PasswordDialog(Activity activity, String message, boolean hidePassword) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.password_dialog, null);
        EditText passwordTextBox = view.findViewById(R.id.password);
        if(hidePassword)
            passwordTextBox.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(view);
        builder.setPositiveButton("Ok", (dialog, id) -> {
            try {
                onOk(passwordTextBox.getText().toString().toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        builder.setCancelable(false);
        builder.setNegativeButton("Abbrechen", (dialog, id) -> onCancel());
        builder.create().show();
    }

    public abstract void onOk(String crypto_password) throws Exception;
    public abstract void onCancel();
}
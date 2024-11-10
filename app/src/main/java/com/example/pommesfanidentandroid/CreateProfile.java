package com.example.pommesfanidentandroid;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.controller.Controller;
import java.io.File;
import java.io.IOException;
import java.security.*;


public class CreateProfile extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_profile);
        LinearLayout myLayout = findViewById(R.id.createProfiles);

        File appDir = getFilesDir();
        int i = 0;
        for(File f : appDir.listFiles()) {
            TextView t = new TextView(this);
            t.setText(f.getName());
            t.setX(20);
            t.setY(60 * i + 160);
            myLayout.addView(t);
            i += 1;
        }

        ViewCompat.setOnApplyWindowInsetsListener(myLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button newKeyPair = findViewById(R.id.btnNewKeyPair);
        newKeyPair.setOnClickListener(v -> {
            newPublicProfileDialog();
        });
    }

    private void newPublicProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Neues öffentliches Profil");
        LinearLayout layout = new LinearLayout(this);
        EditText input = new EditText(this);
        layout.addView(input);
        builder.setPositiveButton("Ok", (dialog, id) -> {
            try {
                Controller.controller.generateKeyPair(getFilesDir() + input.getText().toString(), new String[0]);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            recreate();
        });
        builder.setNegativeButton("Abbrechen", (dialog, id) -> {
        });

        builder.setView(layout);
        builder.create().show();
    }
}
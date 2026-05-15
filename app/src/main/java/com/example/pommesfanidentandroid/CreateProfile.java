package com.example.pommesfanidentandroid;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.PublicProfile;
import utils.Observer;
import utils.OutputEvent;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.text.ParseException;


public class CreateProfile extends Activity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_profile_dialog);
        LinearLayout layout = findViewById(R.id.createProfile);
        loadPublicProfiles(layout);
        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button newKeyPair = findViewById(R.id.btnNewKeyPair);
        newKeyPair.setOnClickListener(v -> {
            newPublicProfileDialog();
        });

        Controller.controller.addObserver(this);
    }

    private void newPublicProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Neues öffentliches Profil");
        LinearLayout layout = new LinearLayout(this);

        TextView l_name = new TextView(this);
        l_name.setText("Name");
        EditText name_input = new EditText(this);
        layout.addView(l_name);
        layout.addView(name_input);

        TextView l_sequence = new TextView(this);
        l_sequence.setText("Sequenznummer");
        EditText sequence_input = new EditText(this);
        layout.addView(l_sequence);
        layout.addView(sequence_input);

        TextView l_valid_from = new TextView(this);
        l_valid_from.setText("Gültig ab");
        EditText valid_from = new EditText(this);
        layout.addView(l_valid_from);
        layout.addView(valid_from);

        TextView l_valid_until_creation = new TextView(this);
        l_valid_until_creation.setText("Gültig bis für Ausstellung");
        EditText valid_until_creation = new EditText(this);
        layout.addView(l_valid_until_creation);
        layout.addView(valid_until_creation);

        TextView l_valid_until_created = new TextView(this);
        l_valid_until_created.setText("Ausgestellte max gültig bis");
        EditText valid_until_created = new EditText(this);
        layout.addView(l_valid_until_created);
        layout.addView(valid_until_created);

        TextView l_max_days_valid = new TextView(this);
        l_max_days_valid.setText("Tage maximale Gültigkeit");
        EditText max_days_valid = new EditText(this);
        layout.addView(l_max_days_valid);
        layout.addView(max_days_valid);

        PublicProfile.ValidityPeriod v = new PublicProfile.ValidityPeriod(
                valid_from.getText().toString(),
                valid_until_creation.getText().toString(),
                valid_until_created.getText().toString(),
                Integer.parseInt(max_days_valid.getText().toString()));
        builder.setPositiveButton("Ok", (dialog, id) -> {
            try {
                String name = name_input.getText().toString();
                int sequence_number = Integer.parseInt(sequence_input.getText().toString());
                controller.Controller.controller.generateKeyPair(name, sequence_number, v, new String[0]);
            } catch (NoSuchAlgorithmException | IOException | NoSuchPaddingException | ParseException |
                     InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            recreate();
        });
        builder.setNegativeButton("Abbrechen", (dialog, id) -> {
        });

        builder.setView(layout);
        builder.create().show();
    }

    private void loadPublicProfiles(LinearLayout layout) {
        File appDir = new File(Controller.controller.appDataLocation + Controller.strPrivateProfiles);
        if(!appDir.exists()) {
            return;
        }
        int i = 0;
        for(File f : appDir.listFiles()) {
            TextView t = new TextView(this);
            t.setText(f.getName());
            t.setX(20);
            t.setY(60 * i + 0);
            layout.addView(t);
            i += 1;
        }
    }

    @Override
    protected void onDestroy() {
        Controller.controller.deleteObserver(this);
        super.onDestroy();
    }

    @Override
    public void update(OutputEvent e) {
        Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }
}
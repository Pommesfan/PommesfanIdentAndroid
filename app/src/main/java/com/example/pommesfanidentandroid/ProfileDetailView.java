package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.PublicProfile;
import utils.Observer;
import utils.OutputEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import android.widget.LinearLayout.LayoutParams;
import javax.crypto.NoSuchPaddingException;

public class ProfileDetailView extends AppCompatActivity implements Observer<OutputEvent> {
    private String profileName;
    private int sequenceNumber = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.publicProfileDetailView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        profileName = intent.getStringExtra("profileName");
        sequenceNumber = intent.getIntExtra("sequenceNumber", -1);
        String mode = intent.getStringExtra("mode");
        Button newIdbtn = findViewById(R.id.btnNewID);
        Button exportProfile = findViewById(R.id.btnExportProfile);
        if(mode.equals("public")) {
            newIdbtn.setEnabled(false);
            exportProfile.setEnabled(false);
        } else {
            newIdbtn.setOnClickListener(v -> newID(profileName, sequenceNumber));
            exportProfile.setOnClickListener(v -> saveFile());
        }
        try {
            loadData(profileName, sequenceNumber, mode);
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        findViewById(R.id.btnDelete).setOnClickListener(v -> delete(profileName, sequenceNumber));
        Controller.controller.addObserver(this);
    }

    private void loadData(String profileName, int sequenceNumber, String mode) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Controller controller = Controller.controller;
        String url;
        if(mode.equals("private"))
            url = controller.appDataLocation + Controller.strPrivateProfiles;
        else if(mode.equals("public"))
            url = controller.appDataLocation + Controller.strPublicProfiles;
        else
            throw new IllegalArgumentException("mode '" + mode + "' not valid");
        PublicProfile profile = PublicProfile.loadInternal(url, profileName, sequenceNumber);
        if(profile == null) {
            return;
        }
        TextView profileNameView = findViewById(R.id.fieldprofileName);
        profileNameView.setText(profile.name);

        TextView viewCreated = findViewById(R.id.created);
        TextView sequence_number = findViewById(R.id.sequence_number);
        TextView viewValidFrom = findViewById(R.id.valid_from);
        TextView viewValidForCreation = findViewById(R.id.valid_for_creation);
        TextView viewValidForCreated = findViewById(R.id.valid_for_created);
        TextView viewMaxValidDays = findViewById(R.id.max_valid_days);

        viewCreated.setText(profile.created);
        sequence_number.setText(String.valueOf(profile.sequence_number));
        viewValidFrom.setText(profile.validityPeriod.validFrom);
        viewValidForCreation.setText(profile.validityPeriod.validUntilForCreation);
        viewValidForCreated.setText(profile.validityPeriod.validUntilForCreated);
        viewMaxValidDays.setText(String.valueOf(profile.validityPeriod.maxValidDays));

        LinearLayout attributes_layout = findViewById(R.id.publicProfileAttributes);

        if(profile.dynamicAttributes.length == 0) {
            LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            TextView t = new TextView(this);
            t.setText("Keine");
            t.setTextSize(20);
            t.setTextColor(Color.parseColor("red"));
            t.setTypeface(t.getTypeface(), Typeface.BOLD_ITALIC);
            t.setLayoutParams(lparams);
            attributes_layout.addView(t);
        }
        for (int i = 0; i < profile.dynamicAttributes.length; i++) {
           LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            String dynamicAttribute = profile.dynamicAttributes[i];
            TextView t = new TextView(this);
            t.setLayoutParams(lparams);
            t.setTextSize(20);
            t.setText(dynamicAttribute);
            attributes_layout.addView(t);
        }
    }
    private void newID(String profileName, int sequenceNumber) {
        Intent intent = new Intent(this, IDeditor.class);
        intent.putExtra("profileName", profileName);
        intent.putExtra("sequenceNumber", sequenceNumber);
        startActivity(intent);
    }
    private static final int SAVE_FILE_CODE = 1;
    private void saveFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, profileName + ":" + sequenceNumber);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    1);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SAVE_FILE_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        // https://stackoverflow.com/questions/44530136/read-failed-ebadf-bad-file-descriptor-while-reading-from-inputstream-nougat
                        OutputStream outputStream = getContentResolver().openOutputStream(uri);
                        new PasswordDialog(this, "Krypto-Passwort") {
                            @Override
                            public void onOk(String crypto_password) throws Exception {
                                Controller.controller.exportPublicProfile(profileName, sequenceNumber, outputStream, crypto_password);
                            }
                            @Override
                            public void onCancel() {
                            }
                        };
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void delete(String profileName, int sequenceNumber) {
        new YesNoDialog(this, "Profil wirklich löschen") {
            @Override
            public void onOk() {
                try {
                    Controller.controller.deletePublicProfile(profileName, sequenceNumber);
                    finish();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
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
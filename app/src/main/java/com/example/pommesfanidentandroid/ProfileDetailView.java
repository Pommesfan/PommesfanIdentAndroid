package com.example.pommesfanidentandroid;

import android.content.Intent;
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
import javax.crypto.NoSuchPaddingException;

public class ProfileDetailView extends AppCompatActivity implements Observer<OutputEvent> {
    private String profileName;
    private int sequenceNumber = -1;
    private int saveMode = -1;
    private final int SAVE_PRIVATE = 1;
    private final int SAVE_PUBLIC = 2;
    private String[]dynamicAttributes;
    private int mode;
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
        mode = intent.getIntExtra("mode", 0);
        Button newIdbtn = findViewById(R.id.btnNewID);
        Button exportPrivateProfile = findViewById(R.id.btnExportPrivateProfile);
        Button exportPublicProfile = findViewById(R.id.btnExportPublicProfile);
        LinearLayout layoutDeleteAndNew = findViewById(R.id.layoutDeleteAndNew);
        LinearLayout layoutProfileAttributes = findViewById(R.id.layoutProfileAttributes);
        if(mode == AppGUIUtils.PUBLIC) {
            layoutDeleteAndNew.removeView(newIdbtn);
            layoutProfileAttributes.removeView(exportPrivateProfile);
            layoutProfileAttributes.removeView(exportPublicProfile);
        } else {
            newIdbtn.setOnClickListener(v -> newID(profileName, sequenceNumber));
            exportPrivateProfile.setOnClickListener(v -> saveFile(SAVE_PRIVATE));
            exportPublicProfile.setOnClickListener(v -> saveFile(SAVE_PUBLIC));
        }
        try {
            loadData(profileName, sequenceNumber);
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        findViewById(R.id.btnDelete).setOnClickListener(v -> delete(profileName, sequenceNumber));
    }

    private void loadData(String profileName, int sequenceNumber) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Controller controller = Controller.controller;
        String url;
        if(mode == AppGUIUtils.PRIVATE)
            url = controller.appDataLocation + Controller.strPrivateProfiles;
        else if(mode == AppGUIUtils.PUBLIC)
            url = controller.appDataLocation + Controller.strPublicProfiles;
        else
            throw new IllegalArgumentException("mode '" + mode + "' not valid");
        PublicProfile profile = PublicProfile.loadInternal(url, profileName, sequenceNumber, false);
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

        LinearLayout attributes_layout = findViewById(R.id.layoutProfileAttributes);
        dynamicAttributes = profile.dynamicAttributes;
        if(dynamicAttributes.length == 0) {
            attributes_layout.addView(AppGUIUtils.getNoneTextView(this));
        }
        for (int i = 0; i < profile.dynamicAttributes.length; i++) {
            attributes_layout.addView(AppGUIUtils.getDynamicParamValueTag(this, profile.dynamicAttributes[i]));
        }
    }
    private void newID(String profileName, int sequenceNumber) {
        Intent intent = new Intent(this, IDeditor.class);
        intent.putExtra("profileName", profileName);
        intent.putExtra("sequenceNumber", sequenceNumber);
        intent.putExtra("dynamicAttributes", dynamicAttributes);
        startActivity(intent);
    }
    private static final int SAVE_FILE_CODE = 1;
    private void saveFile(int mode) {
        saveMode = mode;
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
                                if(saveMode == SAVE_PRIVATE)
                                    Controller.controller.exportPrivateProfile(profileName, sequenceNumber, outputStream, crypto_password);
                                else if(saveMode == SAVE_PUBLIC)
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
                    if(mode == AppGUIUtils.PRIVATE)
                        Controller.controller.deleteProfile(profileName, sequenceNumber, Controller.LOAD_FROM_CREATED);
                    else if (mode == AppGUIUtils.PUBLIC) {
                        Controller.controller.deleteProfile(profileName, sequenceNumber, Controller.LOAD_FROM_IMPORTED);
                    }
                    finish();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        Controller.controller.deleteObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Controller.controller.addObserver(this);
    }

    @Override
    public void update(OutputEvent e) {
        if(!(e instanceof OutputEvent.DummyEvent))
            Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }
}
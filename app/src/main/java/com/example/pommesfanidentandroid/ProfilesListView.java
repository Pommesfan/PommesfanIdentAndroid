package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import android.widget.LinearLayout.LayoutParams;
import javax.crypto.NoSuchPaddingException;

import static android.view.View.INVISIBLE;

public class ProfilesListView extends Activity implements Observer<OutputEvent> {
    private String mode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles_list_view);
        mode = getIntent().getStringExtra("mode");
        loadImportedProfiles();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.importedProfiles), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.btnImport).setOnClickListener(v -> openFile());
        Button newButton = findViewById(R.id.newButton);
        if(mode.equals("public"))
            newButton.setVisibility(INVISIBLE);
        else
            newButton.setOnClickListener(v -> newPrivateProfile());
        Controller.controller.addObserver(this);
    }

    private static final int FILE_SELECT_CODE = 0;

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void newPrivateProfile() {
        Intent intent = new Intent(this, ProfileEditor.class);
        startActivity(intent);
    }
    private void loadImportedProfiles() {
        LinearLayout listView = findViewById(R.id.listViewPublicprofiles);
        listView.removeAllViews();
        String url;
        if(mode.equals("private")) {
            url = Controller.controller.appDataLocation + Controller.strPrivateProfiles;
        } else if(mode.equals("public")) {
            url = Controller.controller.appDataLocation + Controller.strPublicProfiles;
        } else {
            throw new IllegalArgumentException("mode '" + mode + "' not valid");
        }
        File appDir = new File(url);
        if(!appDir.exists()) {
            return;
        }

        for(File f : Objects.requireNonNull(appDir.listFiles())) {
            String name = f.getName();
            for(File fs : Objects.requireNonNull(f.listFiles())) {
                LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                int sequence = Integer.parseInt(fs.getName());
                Button b = new Button(this);
                b.setLayoutParams(lparams);
                b.setText(name + " : " + sequence);
                b.setBackgroundColor(Color.GREEN);
                listView.addView(b);
                b.setOnClickListener(v -> startDetailView(name, sequence));
            }
        }
    }

    private void startDetailView(String profileName, int sequence_number) {
        Intent intent = new Intent(this, ProfileDetailView.class);
        intent.putExtra("profileName", profileName);
        intent.putExtra("sequenceNumber", sequence_number);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        // https://stackoverflow.com/questions/44530136/read-failed-ebadf-bad-file-descriptor-while-reading-from-inputstream-nougat
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        new PasswordDialog(this, "Krypto-Passwort") {
                            @Override
                            public void onOk(String crypto_password) throws NoSuchPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException {
                                if(mode.equals("private"))
                                    Controller.controller.importPrivateProfile(inputStream, crypto_password);
                                else if(mode.equals("public"))
                                    Controller.controller.importPublicProfile(inputStream, crypto_password);
                                ProfilesListView.this.recreate();
                            }

                            @Override
                            public void onCancel() {
                            }
                        };
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        Controller.controller.deleteObserver(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImportedProfiles();
    }

    @Override
    public void update(OutputEvent e) {
        Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }
}
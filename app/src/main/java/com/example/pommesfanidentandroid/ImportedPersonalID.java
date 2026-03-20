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
import java.io.File;
import java.io.InputStream;
import java.util.Objects;

import android.widget.LinearLayout.LayoutParams;

public class ImportedPersonalID extends Activity implements Observer<OutputEvent> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imported_personal_ids);
        LinearLayout layout = findViewById(R.id.viewImportedPersonalID);
        loadImportedProfiles(layout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewImportedPersonalID), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnImportID = findViewById(R.id.addButton);
        btnImportID.setOnClickListener(v -> {
            openFile();
        });

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

    private void loadImportedProfiles(LinearLayout layout) {
        File appDir = new File(Controller.controller.appDataLocation + Controller.strImportedPersonalIDs);
        if(!appDir.exists()) {
            return;
        }
        for(File f : Objects.requireNonNull(appDir.listFiles())) {
            LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            Button b = new Button(this);
            b.setLayoutParams(lparams);
            b.setText(f.getName());
            b.setBackgroundColor(Color.GREEN);
            layout.addView(b);
            b.setOnClickListener(v -> startDetailView(f.getName()));
        }
    }

    private void startDetailView(String idNumber) {
        Intent intent = new Intent(this, PersonalIDdetailView.class);
        intent.putExtra("mode", "saved");
        intent.putExtra("idNumber", idNumber);
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
                        new CryptoPasswordDialog(this) {
                            @Override
                            public void onOk(String crypto_password) throws Exception {
                                Controller.controller.importPersonalID(inputStream, Controller.controller, crypto_password);
                                ImportedPersonalID.this.recreate();
                            }

                            @Override
                            public void onCancel() {
                            }
                        };
                    } catch (Exception e) {
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
    public void update(OutputEvent e) {
        Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }
}
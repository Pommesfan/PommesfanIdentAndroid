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
import model.Personal_ID;
import utils.Observer;
import utils.OutputEvent;
import java.io.File;
import java.io.InputStream;
import java.util.Objects;

public class PersonalIDsListView extends Activity implements Observer<OutputEvent> {
    private int mode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_ids_list_view);
        Intent intent = getIntent();
        mode = intent.getIntExtra("mode", 0);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewImportedPersonalID), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button btnImportID = findViewById(R.id.btnImport);
        if(mode == AppGUIUtils.IMPORTED)
            btnImportID.setOnClickListener(v -> openFile());
        else
            btnImportID.setEnabled(false);
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

    private void loadImportedIDs() throws Exception {
        LinearLayout listView = findViewById(R.id.listViewIDs);
        listView.removeAllViews();
        String url;
        if(mode == AppGUIUtils.CREATED)
            url = Controller.controller.appDataLocation + Controller.strCreatedPersonalIDs;
        else if(mode == AppGUIUtils.IMPORTED)
            url = Controller.controller.appDataLocation + Controller.strImportedPersonalIDs;
        else
            throw new IllegalArgumentException("mode '" + mode + "' not valid");

        File appDir = new File(url);
        if(!appDir.exists()) {
            return;
        }
        for(File f : Objects.requireNonNull(appDir.listFiles())) {
            String idNumber = f.getName();

            int mode_int;
            if(mode == AppGUIUtils.CREATED)
                mode_int = Controller.LOAD_FROM_CREATED;
            else if(mode == AppGUIUtils.IMPORTED)
                mode_int = Controller.LOAD_FROM_IMPORTED;
            else
                throw new IllegalArgumentException("mode '" + mode + "' not valid");

            Personal_ID personalId = Personal_ID.loadInternal(mode_int, idNumber, false, false);
            if(personalId == null)
                continue;

            IdCardView cardView = new IdCardView(this, f.getName(), personalId.publicProfile.name,
                    personalId.publicProfile.sequence_number);
            cardView.setCardBackgroundColor(Color.blue(25));
            listView.addView(cardView);
            cardView.setOnClickListener(v -> startDetailView(idNumber, mode));
        }
    }

    private void startDetailView(String idNumber, int mode) {
        Intent intent = new Intent(this, PersonalIDdetailView.class);
        intent.putExtra("mode", mode);
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
                        new PasswordDialog(this, "Krypto-Passwort") {
                            @Override
                            public void onOk(String crypto_password) throws Exception {
                                Controller.controller.importPersonalID(inputStream, crypto_password);
                                PersonalIDsListView.this.recreate();
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
    protected void onPause() {
        super.onPause();
        Controller.controller.deleteObserver(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Controller.controller.addObserver(this);
        try {
            loadImportedIDs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(OutputEvent e) {
        if(!(e instanceof OutputEvent.DummyEvent))
            Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }
}
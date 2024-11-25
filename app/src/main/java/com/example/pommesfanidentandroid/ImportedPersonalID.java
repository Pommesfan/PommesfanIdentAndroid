package com.example.pommesfanidentandroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

public class ImportedPersonalID extends Activity implements Observer {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imported_personal_ids);
        LinearLayout layout = findViewById(R.id.viewImportedPersonalIDs);
        loadImportedProfiles(layout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewImportedPersonalIDs), (v, insets) -> {
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
        File appDir = new File(Controller.controller.appDataLocation + "ImportedPersonalIDs/");
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                        InputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
                        Controller.controller.importPersonalID(inputStream);
                        recreate();
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
    public void update(Observable o, Object arg) {

    }
}
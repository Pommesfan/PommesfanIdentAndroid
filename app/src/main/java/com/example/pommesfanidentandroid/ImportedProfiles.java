package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.controller.Controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ImportedProfiles extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imported_profiles);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.importedProfiles), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.addButton).setOnClickListener(v -> openFile());
    }

    private void openFile() {

    }
}
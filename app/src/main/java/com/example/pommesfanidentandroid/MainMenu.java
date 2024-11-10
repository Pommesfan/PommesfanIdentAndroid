package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.controller.Controller;

public class MainMenu extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Controller.controller == null) {
            Controller.controller = new Controller(getFilesDir().toString());
        }
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.ownProfiles).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateProfile.class);
            startActivity(intent);
        });
        findViewById(R.id.importedProfiles).setOnClickListener(v -> {
            Intent intent = new Intent(this, ImportedProfiles.class);
            startActivity(intent);
        });
        findViewById(R.id.importedPersonalIDs).setOnClickListener(v -> {
            Intent intent = new Intent(this, ImportedPersonalID.class);
            startActivity(intent);
        });
    }
}
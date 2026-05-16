package com.example.pommesfanidentandroid;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;
import java.io.File;


public class PrivateProfiles extends Activity implements Observer<OutputEvent> {

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
        newKeyPair.setOnClickListener(v -> newPrivateProfile());

        Controller.controller.addObserver(this);
    }

    private void newPrivateProfile() {
        Intent intent = new Intent(this, ProfileEditor.class);
        startActivity(intent);
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
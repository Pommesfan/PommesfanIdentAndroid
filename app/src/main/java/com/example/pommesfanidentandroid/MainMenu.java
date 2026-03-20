package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;

public class MainMenu extends Activity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Controller.controller == null) {
            Controller.controller = new Controller(getFilesDir().toString() + "/");
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
        findViewById(R.id.checkPersonalID).setOnClickListener(v -> {
            Intent intent = new Intent(this, Check_ID_Activity.class);
            startActivity(intent);
        });

        Controller.controller.addObserver(this);

        if(Controller.controller.getProgramPasswordHash() == null) {
            new CryptoPasswordDialog(this) {
                @Override
                public void onOk(String crypto_password) throws Exception {
                    Controller.controller.setProgramPasswordHash(crypto_password);
                }

                @Override
                public void onCancel() {
                    System.exit(0);
                }
            };
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
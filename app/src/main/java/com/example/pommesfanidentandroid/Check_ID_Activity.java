package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;

import java.io.IOException;

public class Check_ID_Activity extends AppCompatActivity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_id);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewCheckPersonsalID), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Controller.controller.addObserver(this);

        try {
            Controller.controller.checkPersonalIDFromRemote();
        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Einlesen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void update(OutputEvent e) {
        if(e instanceof OutputEvent.ServerStartedEvent) {
            OutputEvent.ServerStartedEvent evt = (OutputEvent.ServerStartedEvent) e;
            ((TextView)findViewById(R.id.ip_address)).setText(evt.ip);
            ((TextView)findViewById(R.id.port)).setText(String.valueOf(evt.port));
            ((TextView)findViewById(R.id.crypto_password)).setText(evt.password);
        } else if (e instanceof OutputEvent.PersonalIDValidEvent) {
            finish();
            Intent intent = new Intent(this, PersonalIDdetailView.class);
            intent.putExtra("mode", "received");
            startActivity(intent);
        }  else {
            Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Controller.controller.stopBackgroundRunner();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
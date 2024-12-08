package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.PublicProfile;
import utils.Observer;
import utils.OutputEvent;
import java.io.IOException;

public class PublicProfileDetailView extends AppCompatActivity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile_detail_view);
        LinearLayout layout = findViewById(R.id.publicProfileDetailView);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.publicProfileDetailView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        String profileName = intent.getStringExtra("profileName");
        int sequenceNumber = intent.getIntExtra("sequenceNumber", -1);
        try {
            loadData(profileName, layout, sequenceNumber);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Controller.controller.addObserver(this);
    }

    private void loadData(String profileName, LinearLayout layout, int sequenceNumber) throws IOException {
        Controller controller = Controller.controller;
        PublicProfile profile = PublicProfile.loadInternal(controller, controller.appDataLocation + Controller.strImportedPublicProfiles, profileName, sequenceNumber);
        if(profile == null) {
            return;
        }
        TextView profileNameView = findViewById(R.id.fieldprofileName);
        profileNameView.setText(profile.name);

        for (int i = 0; i < profile.dynamicAttributes.length; i++) {
            String dynamicAttribute = profile.dynamicAttributes[i];
            TextView t = new TextView(this);
            t.setTextSize(20);
            t.setText(dynamicAttribute);
            t.setX(20);
            t.setY(40 * i + 50);
            layout.addView(t);
        }
    }

    @Override
    protected void onDestroy() {
        Controller.controller.deleteObserver(this);
        super.onDestroy();
    }

    @Override
    public void update(OutputEvent e) {

    }
}
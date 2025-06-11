package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.PublicProfile;
import utils.Observer;
import utils.OutputEvent;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import android.widget.LinearLayout.LayoutParams;
import javax.crypto.NoSuchPaddingException;

public class PublicProfileDetailView extends AppCompatActivity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile_detail_view);
        ScrollView layout = findViewById(R.id.publicProfileDetailView);
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
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        Controller.controller.addObserver(this);
    }

    private void loadData(String profileName, ScrollView layout, int sequenceNumber) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Controller controller = Controller.controller;
        PublicProfile profile = PublicProfile.loadInternal(controller, controller.appDataLocation + Controller.strImportedPublicProfiles, profileName, sequenceNumber);
        if(profile == null) {
            return;
        }
        TextView profileNameView = findViewById(R.id.fieldprofileName);
        profileNameView.setText(profile.name);

        LinearLayout attributes_layout = findViewById(R.id.publicProfileAttributes);

        for (int i = 0; i < profile.dynamicAttributes.length; i++) {
           LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            String dynamicAttribute = profile.dynamicAttributes[i];
            TextView t = new TextView(this);
            t.setLayoutParams(lparams);
            t.setTextSize(20);
            t.setText(dynamicAttribute);
            attributes_layout.addView(t);
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
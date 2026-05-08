package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            try {
                Controller.controller.deletePublicProfile(profileName, sequenceNumber);
                finish();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Controller.controller.addObserver(this);
    }

    private void loadData(String profileName, ScrollView layout, int sequenceNumber) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Controller controller = Controller.controller;
        PublicProfile profile = PublicProfile.loadInternal(controller.appDataLocation + Controller.strPublicProfiles, profileName, sequenceNumber);
        if(profile == null) {
            return;
        }
        TextView profileNameView = findViewById(R.id.fieldprofileName);
        profileNameView.setText(profile.name);

        TextView viewCreated = findViewById(R.id.created);
        TextView sequence_number = findViewById(R.id.sequence_number);
        TextView viewValidFrom = findViewById(R.id.valid_from);
        TextView viewValidForCreation = findViewById(R.id.valid_for_creation);
        TextView viewValidForCreated = findViewById(R.id.valid_for_created);
        TextView viewMaxValidDays = findViewById(R.id.max_valid_days);

        viewCreated.setText(profile.created);
        sequence_number.setText(String.valueOf(profile.sequence_number));
        viewValidFrom.setText(profile.validityPeriod.validFrom);
        viewValidForCreation.setText(profile.validityPeriod.validUntilForCreation);
        viewValidForCreated.setText(profile.validityPeriod.validUntilForCreated);
        viewMaxValidDays.setText(String.valueOf(profile.validityPeriod.maxValidDays));

        LinearLayout attributes_layout = findViewById(R.id.publicProfileAttributes);

        if(profile.dynamicAttributes.length == 0) {
            LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            TextView t = new TextView(this);
            t.setText("Keine");
            t.setTextSize(20);
            t.setTextColor(Color.parseColor("red"));
            t.setTypeface(t.getTypeface(), Typeface.BOLD_ITALIC);
            t.setLayoutParams(lparams);
            attributes_layout.addView(t);
        }
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
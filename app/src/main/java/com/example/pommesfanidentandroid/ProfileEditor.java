package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.PublicProfile;
import utils.Observer;
import utils.OutputEvent;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

public class ProfileEditor extends Activity implements Observer<OutputEvent> {
    private LinearLayout dynamicAttributes;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newProfileLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnAddAttribute).setOnClickListener(v -> addDynamicAttribute());
        dynamicAttributes = findViewById(R.id.dynamicAttributes);
    }

    private void addDynamicAttribute() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dynamic_attribute, null);
        Button removeButton = (Button) ((ViewGroup)view).getChildAt(0);
        removeButton.setOnClickListener(v -> dynamicAttributes.removeView(view));
        dynamicAttributes.addView(view);
    }

    private void save() {
        EditText profileName = findViewById(R.id.profileName);
        EditText sequenceNumber = findViewById(R.id.sequence_number);
        EditText validFrom = findViewById(R.id.valid_from);
        EditText validUntilForCreation = findViewById(R.id.valid_until_for_creation);
        EditText validUntilForCreated = findViewById(R.id.valid_until_for_created);
        EditText maxValidDays = findViewById(R.id.max_valid_days);
        PublicProfile.ValidityPeriod period = new PublicProfile.ValidityPeriod(
                validFrom.getText().toString(),
                validUntilForCreation.getText().toString(),
                validUntilForCreated.getText().toString(),
                Integer.parseInt(maxValidDays.getText().toString()));
        String[]dynamicAttributesList = new String[dynamicAttributes.getChildCount()];
        for (int i = 0; i < dynamicAttributesList.length; i++) {
            EditText attribute = (EditText)(((ViewGroup)dynamicAttributes.getChildAt(i)).getChildAt(1));
            dynamicAttributesList[i] = attribute.getText().toString();
        }
        try {
            Controller.controller.generateKeyPair(
                    profileName.getText().toString(),
                    Integer.parseInt(sequenceNumber.getText().toString()),
                    period, dynamicAttributesList);
        } catch (NoSuchAlgorithmException | IOException | ParseException | NoSuchPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        finish();
    }

    @Override
    public void update(OutputEvent outputEvent) {

    }
}
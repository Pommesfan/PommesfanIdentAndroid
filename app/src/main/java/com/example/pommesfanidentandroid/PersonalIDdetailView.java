package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.Personal_ID;
import utils.Observer;
import utils.OutputEvent;
import java.io.ByteArrayInputStream;
import android.widget.LinearLayout.LayoutParams;

import static controller.Controller.LOAD_FROM_CREATED;
import static controller.Controller.LOAD_FROM_IMPORTED;

public class PersonalIDdetailView extends AppCompatActivity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_iddetails_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.personalIDdetailView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            loadData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Controller.controller.addObserver(this);
    }

    private void loadData() throws Exception {
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        //load personal id
        Personal_ID personalId;
        assert mode != null;
        if(mode.equals("created")) {
            String idNumber = intent.getStringExtra("idNumber");
            personalId = Personal_ID.loadInternal(LOAD_FROM_CREATED, idNumber, true);
        } else if(mode.equals("imported")) {
            String idNumber = intent.getStringExtra("idNumber");
            personalId = Personal_ID.loadInternal(LOAD_FROM_IMPORTED, idNumber, true);
        } else if(mode.equals("received")) {
            personalId = Controller.controller.getCheckIDrunnerRes();
        } else {
            personalId = null;
        }

        if (personalId == null) {
            return;
        }

        TextView viewIDnumber = findViewById(R.id.fieldIDnumber);
        TextView viewProfileName = findViewById(R.id.fieldprofileName);
        TextView viewProfileSequenceNumber = findViewById(R.id.profile_sequence_number);
        TextView viewCreated = findViewById(R.id.created);
        TextView viewValidUntil = findViewById(R.id.valid_until);
        TextView viewName = findViewById(R.id.fieldName);
        TextView viewSurname = findViewById(R.id.fieldSurname);
        TextView viewBirthdate = findViewById(R.id.fieldBirthdate);
        TextView viewAdress = findViewById(R.id.fieldAdress);
        ImageView personalImage = findViewById(R.id.viewPersonalImage);
        ImageView handSignature = findViewById(R.id.viewHandSignature);

        viewIDnumber.setText(personalId.ID_number);
        viewProfileName.setText(personalId.publicProfile.name);
        viewProfileSequenceNumber.setText(String.valueOf(personalId.publicProfile.sequence_number));
        viewCreated.setText(personalId.created);
        viewValidUntil.setText(personalId.validUntil);
        viewName.setText(personalId.name);
        viewSurname.setText(personalId.surname);
        String birthdate = personalId.birthdate;
        viewBirthdate.setText(birthdate);
        viewAdress.setText(personalId.address);

        //set dynamic attributes
        String[]dynamic_attributes_names = personalId.publicProfile.dynamicAttributes;
        LinearLayout attributes_layout = findViewById(R.id.personal_id_attributes);
        for (int i = 0; i < dynamic_attributes_names.length; i++) {
            LayoutParams lparams1 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            LayoutParams lparams2 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            TextView t1 = new TextView(this);
            t1.setText(dynamic_attributes_names[i]);
            t1.setTextSize(20);
            t1.setTextColor(Color.parseColor("magenta"));
            t1.setTypeface(t1.getTypeface(), Typeface.BOLD);
            t1.setLayoutParams(lparams1);
            attributes_layout.addView(t1);

            TextView t2 = new TextView(this);
            t2.setText(personalId.dynamicAttributesValues[i]);
            t2.setTextSize(20);
            t2.setLayoutParams(lparams2);
            attributes_layout.addView(t2);
        }

        if(personalId.blob.isEmpty())
            return;
        Personal_ID.BLOB blob = personalId.blob.get();

        personalImage.setImageDrawable(new BitmapDrawable(new ByteArrayInputStream(blob.personal_image)));
        handSignature.setImageDrawable(new BitmapDrawable(new ByteArrayInputStream(blob.hand_signature)));

        Button btnHandIn = findViewById(R.id.btnHandIn);
        btnHandIn.setEnabled(!mode.equals("received"));
        btnHandIn.setOnClickListener(v -> {
            handIn(personalId.ID_number);
        });
        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setEnabled(!mode.equals("received"));
        btnDelete.setOnClickListener(v -> {
            delete(personalId.ID_number);
        });
    }

    private void handIn(String id_number) {
        getHandInDialog(id_number);
    }

    private void delete(String id_number) {
        new YesNoDialog(this, "Ausweis wirklich löschen?") {
            @Override
            public void onOk() {
                try {
                    Controller.controller.deleteID(id_number);
                    finish();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void getHandInDialog(String id_number) {
        new NetworkDialog(this, "Einreichen") {
            @Override
            public void onOk(String ip, int port, String crypto) throws Exception {
                Controller.controller.handInPersonalIDtoRemote(id_number, ip, port, crypto);
            }

            @Override
            public void onCancel() {
            }
        };
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
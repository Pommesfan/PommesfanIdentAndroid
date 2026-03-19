package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
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

import static controller.Controller.LOAD_FROM_IMPORTED;

public class PersonalIDdetailView extends AppCompatActivity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_iddetails_view);
        ScrollView layout = findViewById(R.id.personalIDdetailView);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.personalIDdetailView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        String idNumber = intent.getStringExtra("idNumber");
        try {
            loadData(idNumber, layout);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Controller.controller.addObserver(this);
    }

    private void loadData(String id_number, ScrollView layout) throws Exception {
        //load personal id
        Personal_ID personalId = Personal_ID.loadInternal(Controller.controller, LOAD_FROM_IMPORTED, id_number);
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

        findViewById(R.id.btnHandIn).setOnClickListener(v -> {
            handIn(personalId.ID_number);
        });
    }

    private void handIn(String id_number) {
        AlertDialog dialog = getHandInDialog(id_number);
        dialog.show();
    }

    public AlertDialog getHandInDialog(String id_number) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Einreichen");
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.hand_in_dialog, null);
        builder.setView(view);
        builder.setPositiveButton("Ok", (dialog, which) -> {
            EditText ip = view.findViewById(R.id.ip);
            EditText port = view.findViewById(R.id.port);
            EditText crypto = view.findViewById(R.id.crypto);
            try {
                Thread t = new Thread(() -> {
                    Looper.prepare();
                    try {
                        Controller.controller.handInPersonalIDtoRemote(
                                id_number, ip.getText().toString(),
                                Integer.parseInt(port.getText().toString()),
                                crypto.getText().toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                t.start();
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.setCancelable(true);
        return builder.create();
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
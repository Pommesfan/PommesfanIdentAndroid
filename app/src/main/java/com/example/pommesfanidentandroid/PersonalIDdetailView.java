package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.Personal_ID;
import utils.Observer;
import utils.OutputEvent;
import java.io.ByteArrayInputStream;

import static controller.Controller.LOAD_FROM_IMPORTED;

public class PersonalIDdetailView extends AppCompatActivity implements Observer<OutputEvent> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_iddetails_view);
        LinearLayout layout = findViewById(R.id.personalIDdetailView);
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

    private void loadData(String id_number, LinearLayout layout) throws Exception {
        //load personal id
        Personal_ID personalId = Personal_ID.loadInternal(Controller.controller, LOAD_FROM_IMPORTED, id_number);
        if (personalId == null) {
            return;
        }

        TextView viewIDnumber = findViewById(R.id.fieldIDnumber);
        TextView viewProfileName = findViewById(R.id.fieldprofileName);
        TextView viewName = findViewById(R.id.fieldName);
        TextView viewSurname = findViewById(R.id.fieldSurname);
        TextView viewBirthdate = findViewById(R.id.fieldBirthdate);
        TextView viewAdress = findViewById(R.id.fieldAdress);
        ImageView personalImage = findViewById(R.id.viewPersonalImage);
        ImageView handSignature = findViewById(R.id.viewHandSignature);

        viewIDnumber.setText(personalId.ID_number);
        viewProfileName.setText(personalId.publicProfile.name);
        viewName.setText(personalId.name);
        viewSurname.setText(personalId.surname);
        String birthdate = personalId.birthdate;
        viewBirthdate.setText(birthdate);
        viewAdress.setText(personalId.address);

        if(personalId.blob.isEmpty())
            return;
        Personal_ID.BLOB blob = personalId.blob.get();

        personalImage.setImageDrawable(new BitmapDrawable(new ByteArrayInputStream(blob.personal_image)));
        handSignature.setImageDrawable(new BitmapDrawable(new ByteArrayInputStream(blob.hand_signature)));
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
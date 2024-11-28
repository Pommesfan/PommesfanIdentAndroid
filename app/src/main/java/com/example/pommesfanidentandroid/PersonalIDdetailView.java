package com.example.pommesfanidentandroid;

import android.content.Intent;
import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import model.Personal_ID;
import utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import static controller.Controller.LOAD_PROFILE_FROM_IMPORTED;

public class PersonalIDdetailView extends AppCompatActivity implements Observer {

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
        Controller controller = Controller.controller;
        String distPath = controller.appDataLocation + "ImportedPersonalIDs/" + id_number;
        File f = new File(distPath);
        FileInputStream fis = new FileInputStream(f);
        Utils.SliceReader sliceReader = new Utils.SliceReader((data, length) -> fis.read(data, 0, length));
        byte[] personal_id_b = sliceReader.next();
        String[] personal_id_s = Utils.bytesToStringArray(personal_id_b);
        Personal_ID personalId = Personal_ID.fromString(controller, LOAD_PROFILE_FROM_IMPORTED, personal_id_s);
        if (personalId == null) {
            return;
        }

        TextView viewIDnumber = findViewById(R.id.fieldIDnumber);
        TextView viewProfileName = findViewById(R.id.fieldprofileName);
        TextView viewName = findViewById(R.id.fieldName);
        TextView viewSurname = findViewById(R.id.fieldSurname);
        TextView viewBirthdate = findViewById(R.id.fieldBirthdate);
        TextView viewAdress = findViewById(R.id.fieldAdress);

        viewIDnumber.setText(personalId.ID_number);
        viewProfileName.setText(personalId.publicProfile.name);
        viewName.setText(personalId.name);
        viewSurname.setText(personalId.surname);
        String birthdate = personalId.birthdate_day + "." + personalId.birthdate_month + "." + personalId.birthdate_year;
        viewBirthdate.setText(birthdate);
        viewAdress.setText(personalId.address);
    }

    @Override
    protected void onDestroy() {
        Controller.controller.deleteObserver(this);
        super.onDestroy();
    }

    @Override
    public void update(Observable o, Object arg) {

    }
}
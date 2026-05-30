package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import controller.Controller;
import utils.Observer;
import utils.OutputEvent;
import java.io.*;
import java.util.Objects;

public class IDeditor extends Activity implements Observer<OutputEvent> {
    private byte[]personalimage;
    private String personalImageUrl;
    private String handSignatureUrl;
    private byte[]handsignature;
    private final int PERSONAL_IMAGE = 1;
    private final int HAND_SIGNATURE = 2;
    private int selectedImage = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newIDlayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        String profileName = intent.getStringExtra("profileName");
        int sequenceNumber = intent.getIntExtra("sequenceNumber", 0);
        setDynamicAttributes(Objects.requireNonNull(intent.getStringArrayExtra("dynamicAttributes")));
        findViewById(R.id.btnAddPersonalimage).setOnClickListener(v -> openFile(PERSONAL_IMAGE));
        findViewById(R.id.btnAddHandSignature).setOnClickListener(v -> openFile(HAND_SIGNATURE));
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            try {
                createID(profileName, sequenceNumber);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Controller.controller.addObserver(this);
    }

    private void setDynamicAttributes(String[] dynamicAttributes) {
        LinearLayout layout = findViewById(R.id.dynamicAttributes);
        if(dynamicAttributes.length == 0) {
            layout.addView(AppGUIUtils.getNoneTextView(this));
        }
        for (int i = 0; i < dynamicAttributes.length; i++) {
            // add label for dynamic attribute
            layout.addView(AppGUIUtils.getDynamicParamTag(this, dynamicAttributes[i]));
            // add edit text for value of for dynamic attribute
            EditText editText = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            editText.setLayoutParams(params);
            layout.addView(editText);
        }
    }

    private void createID(String profileName, int sequenceNumber) throws Exception {
        EditText validUntil = findViewById(R.id.valid_until);
        EditText firstName = findViewById(R.id.firstName);
        EditText surname = findViewById(R.id.surname);
        EditText birthdate = findViewById(R.id.birthdate);
        EditText address = findViewById(R.id.address);

        LinearLayout layout = findViewById(R.id.dynamicAttributes);
        final int dynamicAttributesCount = layout.getChildCount() / 2;
        String[]dynamicAttributesValues = new String[dynamicAttributesCount];
        for (int i = 0; i < dynamicAttributesCount; i++) {
            dynamicAttributesValues[i] = ((EditText)layout.getChildAt(i * 2 + 1)).getText().toString();
        }

        Controller.controller.generateID(profileName, sequenceNumber,
                validUntil.getText().toString(),
                firstName.getText().toString(),
                surname.getText().toString(),
                birthdate.getText().toString(),
                address.getText().toString(),
                dynamicAttributesValues,
                personalimage, AppGUIUtils.nameFromURL(personalImageUrl),
                handsignature, AppGUIUtils.nameFromURL(handSignatureUrl));
    }

    private static final int FILE_SELECT_CODE = 0;
    private void openFile(int mode) {
        selectedImage = mode;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        // https://stackoverflow.com/questions/44530136/read-failed-ebadf-bad-file-descriptor-while-reading-from-inputstream-nougat
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            assert inputStream != null;
                            byte[]file = inputStream.readAllBytes();
                            if(selectedImage == PERSONAL_IMAGE) {
                                personalImageUrl = uri.toString();
                                personalimage = file;
                                AppGUIUtils.bytesToImageView(this, personalimage, findViewById(R.id.viewPersonalImage));
                            } else if(selectedImage == HAND_SIGNATURE) {
                                handSignatureUrl = uri.toString();
                                handsignature = file;
                                AppGUIUtils.bytesToImageView(this, handsignature, findViewById(R.id.viewHandSignature));
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void update(OutputEvent e) {
        if(e instanceof OutputEvent.CreationSuccessEvent)
            finish();
    }
}
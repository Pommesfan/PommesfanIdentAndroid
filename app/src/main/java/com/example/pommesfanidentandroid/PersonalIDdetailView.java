package com.example.pommesfanidentandroid;

import AppUtils.AppGUIUtils;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import controller.Controller;
import model.Personal_ID;
import utils.Observer;
import utils.OutputEvent;
import java.io.IOException;
import java.io.OutputStream;
import static controller.Controller.LOAD_FROM_CREATED;
import static controller.Controller.LOAD_FROM_IMPORTED;

public class PersonalIDdetailView extends AppCompatActivity implements Observer<OutputEvent> {
    private String idNumber;
    int mode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_iddetails_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.personalIDdetailView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        mode = intent.getIntExtra("mode", 0);

        LinearLayout layoutHandInDelete = findViewById(R.id.layoutHandInDelete);
        Button btnExport = findViewById(R.id.btnExportID);
        LinearLayout personalIdAttributes = findViewById(R.id.personal_id_attributes);
        Button btnHandIn = findViewById(R.id.btnHandIn);
        Button btnDelete = findViewById(R.id.btnDelete);
        if(mode == AppGUIUtils.CREATED)
            btnExport.setOnClickListener(v -> exportId(v));
        else
            personalIdAttributes.removeView(btnExport);

        if(mode == AppGUIUtils.IMPORTED)
            btnHandIn.setOnClickListener(v -> handIn(idNumber));
        else
            layoutHandInDelete.removeView(btnHandIn);

        if(mode == AppGUIUtils.RECEIVED)
            layoutHandInDelete.removeView(btnDelete);
        else
            btnDelete.setOnClickListener(v -> delete(idNumber));

        try {
            loadData(intent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportId(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.export_id_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if(itemId == R.id.exportToFile)
                saveFile();
            else if(itemId == R.id.exportOverNetwork)
                exportOverConnection(AppGUIUtils.NETWORK);
            else if(itemId == R.id.exportOverBluetooth) {
                try {
                    exportOverConnection(AppGUIUtils.BLUETOOTH);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            else
                return false;
            return true;
        });
        popup.show();
    }

    private void loadData(Intent intent) throws Exception {
        //load personal id
        Personal_ID personalId;
        if(mode == AppGUIUtils.CREATED) {
            String idNumber = intent.getStringExtra("idNumber");
            personalId = Personal_ID.loadInternal(LOAD_FROM_CREATED, idNumber, true, true);
        } else if(mode == AppGUIUtils.IMPORTED) {
            String idNumber = intent.getStringExtra("idNumber");
            personalId = Personal_ID.loadInternal(LOAD_FROM_IMPORTED, idNumber, true, true);
        } else if(mode == AppGUIUtils.RECEIVED) {
            personalId = Controller.controller.getCheckIDrunnerRes();
        } else {
            personalId = null;
        }

        if (personalId == null)
            return;

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

        idNumber = personalId.ID_number;
        viewIDnumber.setText(idNumber);
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
        if(dynamic_attributes_names.length == 0) {
            attributes_layout.addView(AppGUIUtils.getNoneTextView(this));
        }
        for (int i = 0; i < dynamic_attributes_names.length; i++) {
            attributes_layout.addView(AppGUIUtils.getDynamicParamTag(this, dynamic_attributes_names[i]));
            attributes_layout.addView(AppGUIUtils.getDynamicParamValueTag(this, personalId.dynamicAttributesValues[i]));
        }

        if(personalId.blob.isEmpty())
            return;
        Personal_ID.BLOB blob = personalId.blob.get();
        AppGUIUtils.bytesToImageView(this, blob.personal_image, personalImage);
        AppGUIUtils.bytesToImageView(this, blob.hand_signature, handSignature);
    }

    private void handIn(String id_number) {
        getHandInDialog(id_number);
    }
    private void exportOverConnection(int medium) {
        Intent intent = new Intent(this, ProvideServiceView.class);
        intent.putExtra("mode", AppGUIUtils.EXPORT);
        intent.putExtra("medium", medium);
        intent.putExtra("idNumber", idNumber);
        startActivity(intent);
    }
    private static final int SAVE_FILE_CODE = 1;
    private static final int READ_QR_CODE = 49374;
    private void saveFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, idNumber);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    1);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SAVE_FILE_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        // https://stackoverflow.com/questions/44530136/read-failed-ebadf-bad-file-descriptor-while-reading-from-inputstream-nougat
                        OutputStream outputStream = getContentResolver().openOutputStream(uri);
                        new PasswordDialog(this, "Krypto-Passwort") {
                            @Override
                            public void onOk(String crypto_password) throws Exception {
                                Controller.controller.exportPersonalID(idNumber, outputStream, crypto_password);
                            }
                            @Override
                            public void onCancel() {
                            }
                        };
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            case READ_QR_CODE:
                super.onActivityResult(requestCode, resultCode, data);
                IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (intentResult != null) {
                    String res = intentResult.getContents();
                    if (intentResult.getContents() != null) {
                        String[]resArray = res.split("\n");
                        if (resArray.length != 4 || !resArray[0].equals("PommesfanIdent")) {
                            Toast.makeText(this, "QR-Code wird nicht unterstützt", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new Thread(() -> {
                            Looper.prepare();
                            try {
                                Controller.controller.handInPersonalIDtoRemote(idNumber, resArray[1],
                                        Integer.parseInt(resArray[2]), resArray[3]);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }
    private void delete(String id_number) {
        new YesNoDialog(this, "Ausweis wirklich löschen?") {
            @Override
            public void onOk() {
                try {
                    if(mode == AppGUIUtils.CREATED)
                        Controller.controller.deleteID(id_number, LOAD_FROM_CREATED);
                    else if(mode == AppGUIUtils.IMPORTED)
                        Controller.controller.deleteID(id_number, LOAD_FROM_IMPORTED);
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
    protected void onPause() {
        super.onPause();
        Controller.controller.deleteObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Controller.controller.addObserver(this);
    }

    @Override
    public void update(OutputEvent e) {
        if(!(e instanceof OutputEvent.DummyEvent))
            Toast.makeText(this, AppGUIUtils.handleMsg(e), Toast.LENGTH_SHORT).show();
    }
}
package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

public class IdCardView extends CardView {
    public IdCardView(@NonNull Activity activity, String idNumber, String profileName, int sequenceNumber) {
        super(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.cardview_personal_id, null);
        ((TextView)view.findViewById(R.id.idNumber)).setText(idNumber);
        ((TextView)view.findViewById(R.id.profileName)).setText(profileName);
        ((TextView)view.findViewById(R.id.sequence_number)).setText(String.valueOf(sequenceNumber));
        addView(view);
    }
}

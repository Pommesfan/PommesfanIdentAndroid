package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

public class ProfileCardView extends CardView {
    public ProfileCardView(@NonNull Activity activity, String name, int sequence) {
        super(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.cardview_profile, null);
        ((TextView)view.findViewById(R.id.profileName)).setText(name);
        ((TextView)view.findViewById(R.id.sequence_number)).setText(String.valueOf(sequence));
        addView(view);
    }
}

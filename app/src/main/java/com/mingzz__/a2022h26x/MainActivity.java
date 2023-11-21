package com.mingzz__.a2022h26x;


import android.os.Bundle;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h26x_play_activity);
        SwitchCompat viewById = findViewById(R.id.mingzz);
        viewById.setShowText(true);
        viewById.setSelected(false);
        viewById.setChecked(true);
        viewById.setEnabled(true);
        SwitchPreference
    }
}
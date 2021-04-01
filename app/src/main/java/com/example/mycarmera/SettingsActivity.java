package com.example.mycarmera;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private ImageView mBack;
    private Switch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreenActivity();
        setContentView(R.layout.activity_settings);
        initView();
    }

    private void initView() {
        mBack = findViewById(R.id.back);
        mSwitch = findViewById(R.id.water_mark_switch);

        mBack.setOnClickListener(this);
        mSwitch.setOnCheckedChangeListener(this);

        boolean waterMark = SharedPreferencesController.getInstance(this).spGetBoolean("add_water_mark");

        mSwitch.setChecked(waterMark);

    }


    private void requestFullScreenActivity() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                this.finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferencesController.getInstance(this).spPutBoolean("add_water_mark", isChecked);
    }
}
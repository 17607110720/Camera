package com.example.mycarmera;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import static com.example.mycarmera.CameraConstant.ADD_WATER_MARK;
import static com.example.mycarmera.CameraConstant.COUNTDOWN_PHOTO;
import static com.example.mycarmera.CameraConstant.FOCUS_TAKE_PICTURE;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private ImageView mBack;
    private Switch mWaterMarkSwitch;
    private Switch mCountDownPhotoSwitch;
    private Switch mFocusSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreenActivity();
        setContentView(R.layout.activity_settings);
        initView();
    }

    private void initView() {
        mBack = findViewById(R.id.back);
        mWaterMarkSwitch = findViewById(R.id.water_mark_switch);
        mCountDownPhotoSwitch = findViewById(R.id.countdown_photo_switch);
        mFocusSwitch = findViewById(R.id.focus_switch);

        mBack.setOnClickListener(this);
        mWaterMarkSwitch.setOnCheckedChangeListener(this);
        mCountDownPhotoSwitch.setOnCheckedChangeListener(this);
        mFocusSwitch.setOnCheckedChangeListener(this);

        boolean waterMark = SharedPreferencesController.getInstance(this).spGetBoolean(ADD_WATER_MARK);

        boolean countdown_photo = SharedPreferencesController.getInstance(this).spGetBoolean(COUNTDOWN_PHOTO);

        boolean focus = SharedPreferencesController.getInstance(this).spGetBoolean(FOCUS_TAKE_PICTURE);

        mWaterMarkSwitch.setChecked(waterMark);
        mCountDownPhotoSwitch.setChecked(countdown_photo);
        mFocusSwitch.setChecked(focus);

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
        switch (buttonView.getId()) {
            case R.id.water_mark_switch:
                SharedPreferencesController.getInstance(this).spPutBoolean(ADD_WATER_MARK, isChecked);//水印
                break;
            case R.id.countdown_photo_switch:
                SharedPreferencesController.getInstance(this).spPutBoolean(COUNTDOWN_PHOTO, isChecked);//倒计时拍照
                break;
            case R.id.focus_switch:
                SharedPreferencesController.getInstance(this).spPutBoolean(FOCUS_TAKE_PICTURE, isChecked);//对焦
        }


    }
}
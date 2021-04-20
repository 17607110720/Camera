package com.example.mycarmera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, CameraController.CameraControllerInterFaceCallback, MyButton.MyCameraButtonClickListener, TwoStateSwitch.CustomCheckBoxChangeListener {

    private AutoFitTextureView mPreviewView;

    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CameraCaptureSession mCaptureSession;
    private CameraController mCameraController;
    private boolean mIsRecordingVideo;
    private File mFile;
    private TextView picModeTextView;
    private TextView videoModeTextView;
    private TextView ratio_4_3;
    private TextView ratio_16_9;
    private MyButton myButton;
    //    private MyButton myVideoTakePicButton;
    private LinearLayout ll_switch_ratio;
    private LinearLayout ll_switch_mode;
    private ImageButton switch_camera_id;
    private ImageView settings;
    private RoundImageView goto_gallery;
    private MyOrientationEventListener mOrientationListener;
    private int mPhoneOrientation;
    public static final int ORIENTATION_HYSTERESIS = 5;


    private int mCurrentMode = CameraConstant.PHOTO_MODE;
    private TextView slowMotionModeTextView;
    private TwoStateSwitch mFlashSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreenActivity();
        setContentView(R.layout.activity_main);
        initView();
        registerOrientationLister();

    }

    private void registerOrientationLister() {
        mOrientationListener = new MyOrientationEventListener(this);//方向旋转监听

    }

    private void initOrientationSensor() {
        mOrientationListener.enable();//开始方向监听
    }

    private void initView() {
        mPreviewView = findViewById(R.id.preview_view);
        myButton = findViewById(R.id.myButton);
        picModeTextView = findViewById(R.id.switch_mode_pic);
        videoModeTextView = findViewById(R.id.switch_mode_video);
        slowMotionModeTextView = findViewById(R.id.switch_mode_slow_motion);

        ratio_4_3 = findViewById(R.id.switch_ratio4_3);
        ratio_16_9 = findViewById(R.id.switch_ratio16_9);
        ll_switch_ratio = findViewById(R.id.ll_switch_ratio);
        ll_switch_mode = findViewById(R.id.ll_switch_mode);
        switch_camera_id = findViewById(R.id.switch_camera_id);
        settings = findViewById(R.id.settings);
        goto_gallery = findViewById(R.id.goto_gallery);
//        myVideoTakePicButton = findViewById(R.id.myVideoTakePicButton);
        mFlashSwitch = findViewById(R.id.flash_switch);

        goto_gallery.setBackground(getDrawable(R.drawable.drawable_shape));

        picModeTextView.setOnClickListener(this);
        videoModeTextView.setOnClickListener(this);
        slowMotionModeTextView.setOnClickListener(this);
        myButton.setOnBaseViewClickListener(this);
//        myVideoTakePicButton.setOnBaseViewClickListener(this);
        ratio_4_3.setOnClickListener(this);
        ratio_16_9.setOnClickListener(this);
        switch_camera_id.setOnClickListener(this);
        settings.setOnClickListener(this);
        goto_gallery.setOnClickListener(this);
        mFlashSwitch.setCustomCheckBoxChangeListener(this);
        mCameraController = new CameraController(this, mPreviewView);
        mCameraController.setCameraControllerInterFaceCallback(this);
    }

    //拍照模式
    private void picMode() {
        if (mCurrentMode == CameraConstant.PHOTO_MODE) return;
        ll_switch_ratio.setVisibility(View.VISIBLE);
        ratio_4_3.setTextColor(Color.YELLOW);
        ratio_16_9.setTextColor(Color.WHITE);
        videoModeTextView.setTextColor(Color.WHITE);
        slowMotionModeTextView.setTextColor(Color.WHITE);
        picModeTextView.setTextColor(Color.YELLOW);

        mCurrentMode = CameraConstant.PHOTO_MODE;
        myButton.setCurrentMode(mCurrentMode);

        mCameraController.closeCamera();
        mCameraController.closeMediaRecorder();
        mCameraController.setCurrentMode(mCurrentMode);
        mCameraController.setTargetRatio(CameraConstant.RATIO_FOUR_THREE);
        mCameraController.openCamera();

    }

    //录像模式
    private void videoMode() {
        if (mCurrentMode == CameraConstant.VIDEO_MODE) return;
        ll_switch_ratio.setVisibility(View.GONE);//慢动作/录像模式下默认16：9，不允许切换比例
        picModeTextView.setTextColor(Color.WHITE);
        videoModeTextView.setTextColor(Color.YELLOW);
        slowMotionModeTextView.setTextColor(Color.WHITE);

        mCurrentMode = CameraConstant.VIDEO_MODE;
        myButton.setCurrentMode(mCurrentMode);

        myButton.setVideoRecordingState(false);
        mCameraController.closeCamera();
        mCameraController.setCurrentMode(mCurrentMode);
        mCameraController.setTargetRatio(CameraConstant.RATIO_SIXTEEN_NINE);
        mCameraController.openCamera();
    }

    //慢动作模式
    private void slowMotionMode() {
        if (mCurrentMode == CameraConstant.SLOW_MOTION_MODE) return;
        ll_switch_ratio.setVisibility(View.GONE);//慢动作/录像模式下默认16：9，不允许切换比例
        picModeTextView.setTextColor(Color.WHITE);
        videoModeTextView.setTextColor(Color.WHITE);
        slowMotionModeTextView.setTextColor(Color.YELLOW);

        mCurrentMode = CameraConstant.SLOW_MOTION_MODE;
        myButton.setCurrentMode(mCurrentMode);

        myButton.setVideoRecordingState(false);
        mCameraController.closeCamera();
        mCameraController.setCurrentMode(mCurrentMode);
        mCameraController.setTargetRatio(CameraConstant.RATIO_SIXTEEN_NINE);
        mCameraController.openCamera();


    }

    //4：3
    private void ratio_4_3() {
        ratio_4_3.setTextColor(Color.YELLOW);
        ratio_16_9.setTextColor(Color.WHITE);
        mCameraController.switch_4_3();
    }

    //16：9
    private void ratio_16_9() {
        ratio_16_9.setTextColor(Color.YELLOW);
        ratio_4_3.setTextColor(Color.WHITE);
        mCameraController.switch_16_9();
    }

    //前后置切换
    private void switch_camera_id() {
        mCameraController.switch_camera_id();
    }

    private void gotoSrttingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_mode_pic:
                picMode();
                break;
            case R.id.switch_mode_video:
                videoMode();
                break;
            case R.id.switch_mode_slow_motion:
                slowMotionMode();
                break;
            case R.id.switch_ratio4_3:
                ratio_4_3();
                break;
            case R.id.switch_ratio16_9:
                ratio_16_9();
                break;
            case R.id.switch_camera_id:
                switch_camera_id();
                break;
            case R.id.settings:
                gotoSrttingsActivity();
                break;
            case R.id.goto_gallery:
                gotoGallery();
                break;
        }
    }


    //进入相册
    private void gotoGallery() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        if (null == mFile) return;
        Uri uri = Uri.fromFile(mFile);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, "image/jpeg");
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraController.startBackgroundThread();//开启一个后台线程处理相机数据

        initOrientationSensor();//开始方向监听
        //判断TextureView是否有效，有效就直接openCamera()，
        if (mPreviewView.isAvailable()) {
            //mTextureView已经创建，SurfaceTexture已经有效，则直接openCamera，用于屏幕熄灭等情况，这时onSurfaceTextureAvailable不会回调。
            mCameraController.openCamera();
            //SurfaceTexture处于无效状态中，则通过SurfaceTextureListener确保surface准备好。
        } else {//无效就加入一个监听SufaceTextureListener，通过回调确保surfaceTexture有效，然后同样openCamera()。
            mPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }


    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {//TextureView回调

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mCameraController.openCamera();//SurfaceTexture有效即可openCamera,宽高是控件宽高
        }

        //配置transformation，主要是矩阵旋转相关
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };


    @Override
    protected void onPause() {
        super.onPause();
        mOrientationListener.disable();//禁用方向监听
        if (mIsRecordingVideo) {
            mIsRecordingVideo = false;
            myButton.setVideoRecordingState(mIsRecordingVideo);
            stopRecordVideo();
        }
        mCameraController.closeCamera();
        mCameraController.closeMediaRecorder();
        mCameraController.stopBackgroundThread();
    }

//    @Override
//    protected void onDestroy() {
//        if (mHandler != null) {
//            mHandler.removeCallbacksAndMessages(null);
//        }
//        super.onDestroy();
//    }


    public void startRecordVideo() {

        mFile = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".mp4");
        mCameraController.setPath(mFile);

        ll_switch_mode.setVisibility(View.GONE);//录像时不能切换模式
        switch_camera_id.setVisibility(View.GONE);//录像时不能前后置切换
        settings.setVisibility(View.GONE);//录像时不能设置水印
//        myVideoTakePicButton.setVisibility(View.VISIBLE);//录像时可以进行拍照
        goto_gallery.setVisibility(View.GONE);//录像时不能进入相册查看缩略图
        mFlashSwitch.setVisibility(View.GONE);//录像时不能控制闪光灯
        mCameraController.startRecordingVideo();

    }

    public void stopRecordVideo() {

        ll_switch_mode.setVisibility(View.VISIBLE);//录像结束才能切换模式
        switch_camera_id.setVisibility(View.VISIBLE);//录像结束后才能前后置切换
        settings.setVisibility(View.VISIBLE);//录像结束后才能设置水印
//        myVideoTakePicButton.setVisibility(View.GONE);//录像结束关闭录像时拍照按钮
        goto_gallery.setVisibility(View.VISIBLE);//录像结束才能进入相册查看缩略图
        mFlashSwitch.setVisibility(View.VISIBLE);//录像结束才能控制闪光灯
        mCameraController.stopRecordingVideo();
        Toast.makeText(this, "录像储存位置：" + mFile, Toast.LENGTH_LONG).show();

    }

    //添加缩略图
    @Override
    public void onThumbnailCreated(final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                goto_gallery.setBitmap(bitmap);
            }
        });
    }


    @Override
    public void onMyCameraButtonClick(int mode) {
        switch (mode) {
            case CameraConstant.PHOTO_MODE:
                takePicture();
                break;
            case CameraConstant.VIDEO_MODE:
            case CameraConstant.SLOW_MOTION_MODE:
                takeVideo();
                break;

        }
    }

    @Override
    public void customCheckBoxOn(int flashSwitch) {
        switch (flashSwitch) {
            case R.id.flash_switch:
                mCameraController.openFlashMode();
                break;
        }
    }

    @Override
    public void customCheckBoxOff(int flashSwitch) {
        switch (flashSwitch) {
            case R.id.flash_switch:
                mCameraController.closeFlashMode();
                break;
        }
    }


    private void takePicture() {
        mFile = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
        mCameraController.setPath(mFile);
        mCameraController.takepicture();
        myButton.startPictureAnimator();
        myButton.setEnabled(false);
    }

    private void takeVideo() {
        if (mIsRecordingVideo) {
            mIsRecordingVideo = false;
            myButton.setVideoRecordingState(mIsRecordingVideo);
            stopRecordVideo();
        } else {
            mIsRecordingVideo = true;
            myButton.setVideoRecordingState(mIsRecordingVideo);
            startRecordVideo();
        }
    }


    private class MyOrientationEventListener extends OrientationEventListener {

        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            mPhoneOrientation = roundOrientation(orientation, mPhoneOrientation);
            mCameraController.setPhoneDeviceDegree(mPhoneOrientation);
        }
    }

    private int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }


    private void requestFullScreenActivity() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
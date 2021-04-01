package com.example.mycarmera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, CameraController.CameraControllerInterFaceCallback {
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

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
    private LinearLayout ll_switch_ratio;
    private LinearLayout ll_switch_mode;
    private View switch_camera_id;
    private ImageView settings;
    private ImageView goto_photo;
    private View myVideoTakePicButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreenActivity();
        setContentView(R.layout.activity_main);
        initView();
        mCameraController = new CameraController(this, mPreviewView);
        mCameraController.setCameraControllerInterFaceCallback(this);


    }

    private void initView() {
        mPreviewView = findViewById(R.id.preview_view);
        myButton = findViewById(R.id.myButton);
        picModeTextView = findViewById(R.id.switch_mode_4_3);
        videoModeTextView = findViewById(R.id.switch_mode_16_9);
        ratio_4_3 = findViewById(R.id.switch_ratio4_3);
        ratio_16_9 = findViewById(R.id.switch_ratio16_9);
        ll_switch_ratio = findViewById(R.id.ll_switch_ratio);
        ll_switch_mode = findViewById(R.id.ll_switch_mode);
        switch_camera_id = findViewById(R.id.switch_camera_id);
        settings = findViewById(R.id.settings);
        goto_photo = findViewById(R.id.goto_photo);
        myVideoTakePicButton = findViewById(R.id.myVideoTakePicButton);

        goto_photo.setBackground(getDrawable(R.drawable.drawable_shape));

        picModeTextView.setOnClickListener(this);
        videoModeTextView.setOnClickListener(this);
        myButton.setOnClickListener(this);
        ratio_4_3.setOnClickListener(this);
        ratio_16_9.setOnClickListener(this);
        switch_camera_id.setOnClickListener(this);
        settings.setOnClickListener(this);
        goto_photo.setOnClickListener(this);
        myVideoTakePicButton.setOnClickListener(this);


    }

    //拍照模式
    private void picMode() {
        ll_switch_ratio.setVisibility(View.VISIBLE);
        ratio_4_3.setTextColor(Color.YELLOW);
        ratio_16_9.setTextColor(Color.WHITE);
        videoModeTextView.setTextColor(Color.WHITE);
        picModeTextView.setTextColor(Color.YELLOW);
        //拍照模式设置监听拍照方法
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraController.takepicture();
            }
        });
        mCameraController.switch_4_3();
    }

    //录像模式
    private void videoMode() {
        ll_switch_ratio.setVisibility(View.GONE);//录像模式下默认16：9，不允许切换比例
        picModeTextView.setTextColor(Color.WHITE);
        videoModeTextView.setTextColor(Color.YELLOW);
        //录像模式设置监听录像方法
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecordingVideo) {
//                            mTakeVideo.setText("开始");
//                    Log.d("123123", "stopRecordingVideo");
                    mIsRecordingVideo = false;
                    mCameraController.stopRecordingVideo();


                } else {
                    mFile = new File(MainActivity.this.getExternalFilesDir(null), "test.mp4");
                    mCameraController.setVideoPath(mFile);
                    mIsRecordingVideo = true;
                    mCameraController.startRecordingVideo();
//                    ll_switch_mode.setVisibility(View.GONE);//录像时不能切换模式
//                    switch_camera_id.setVisibility(View.GONE);//录像时不能前后置切换
                }
            }
        });
        mCameraController.switch_16_9();
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
            case R.id.myButton:
            case R.id.myVideoTakePicButton:
                mCameraController.takepicture();
                break;
            case R.id.switch_mode_4_3:
                picMode();
                break;
            case R.id.switch_mode_16_9:
                videoMode();
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
            case R.id.goto_photo:
                gotoPhoto();
                break;
        }
    }

    //进入相册
    private void gotoPhoto() {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType("image/*");
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();//开启一个后台线程处理相机数据
        //判断TextureView是否有效，有效就直接openCamera()，
        if (mPreviewView.isAvailable()) {
            try {
                //mTextureView已经创建，SurfaceTexture已经有效，则直接openCamera，用于屏幕熄灭等情况，这时onSurfaceTextureAvailable不会回调。
                mCameraController.openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            //SurfaceTexture处于无效状态中，则通过SurfaceTextureListener确保surface准备好。
        } else {//无效就加入一个监听SufaceTextureListener，通过回调确保surfaceTexture有效，然后同样openCamera()。
            mPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }


    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {//TextureView回调

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            try {
                mCameraController.openCamera();//SurfaceTexture有效即可openCamera,宽高是控件宽高
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
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
        if (mIsRecordingVideo) {
//            mTakeVideo.setText("开始");
            mIsRecordingVideo = false;
            mCameraController.stopRecordingVideo();
//            ll_switch_mode.setVisibility(View.VISIBLE);//录像结束才能切换模式
//            switch_camera_id.setVisibility(View.VISIBLE);//录像结束后才能前后置切换

        }

        if (mImageReader != null) {//关闭ImageReader
            mImageReader.close();
            mImageReader = null;
        }

        if (mCaptureSession != null) {//关闭CameraCaptureSession
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (mCameraDevice != null) {//关闭相机
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    @Override
    public void startRecordVideo() {
//                mTakeVideo.setText("停止");
        ll_switch_mode.setVisibility(View.GONE);//录像时不能切换模式
        switch_camera_id.setVisibility(View.GONE);//录像时不能前后置切换
        settings.setVisibility(View.GONE);//录像时不能设置水印
        myVideoTakePicButton.setVisibility(View.VISIBLE);//录像时可以进行拍照
        goto_photo.setVisibility(View.GONE);//录像时不能进入相册查看缩略图
//        onMyCameraButtonClick();


    }

    @Override
    public void stopRecordVideo() {
        ll_switch_mode.setVisibility(View.VISIBLE);//录像结束才能切换模式
        switch_camera_id.setVisibility(View.VISIBLE);//录像结束后才能前后置切换
        settings.setVisibility(View.VISIBLE);//录像结束后才能设置水印
        myVideoTakePicButton.setVisibility(View.GONE);//录像结束关闭录像时拍照按钮
        goto_photo.setVisibility(View.VISIBLE);//录像结束才能进入相册查看缩略图
        Toast.makeText(this, "录像储存位置：" + getExternalFilesDir(null) + "test.mp4", Toast.LENGTH_LONG).show();


    }

    //添加缩略图
    @Override
    public void onThumbnailCreated(final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                goto_photo.setImageBitmap(bitmap);
                goto_photo.invalidate();
            }
        });
    }


//    @Override
//    public void onMyCameraButtonClick() {
//        myButton.mPaint.setColor(Color.RED);
//        Canvas canvas=new Canvas();
//        canvas.drawCircle(88, 88, 60, myButton.mPaint);
//    }



    private void requestFullScreenActivity() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
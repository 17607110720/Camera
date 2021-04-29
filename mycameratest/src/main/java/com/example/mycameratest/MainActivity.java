package com.example.mycameratest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button pictureBtn;
    private AutoFitTextureView mPreviewTexture;
    private CameraManager manager;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize = new Size(1440, 1080);//预览尺寸
    private Size mCaptureSize = new Size(4000, 3000);//拍照尺寸
    private Size mVideoSize = new Size(1920, 1080);//录像尺寸
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest previewRequest;
    private File mFile;
    private Button videoBtn;
    private boolean isRecording;
    private MediaRecorder mediaRecorder;
    private HandlerThread backgroundThread;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreenActivity();
        setContentView(R.layout.activity_main);

        pictureBtn = findViewById(R.id.btn1);
        videoBtn = findViewById(R.id.btn2);
        mPreviewTexture = findViewById(R.id.autoFitTexture_view);

        pictureBtn.setOnClickListener(this);
        videoBtn.setOnClickListener(this);
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mediaRecorder = new MediaRecorder();

        if (mPreviewTexture.isAvailable()) {

            openCamera();

        } else {//无效就加入一个监听SufaceTextureListener，通过回调确保surfaceTexture有效，然后同样openCamera()。
            mPreviewTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                }
            });
        }


    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        handler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                handler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            manager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    try {
                        createCameraPreviewSession();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createCameraPreviewSession() throws CameraAccessException {
        createImagerReader();
        Log.d("mPreviewTexture", "mPreviewTexture=" + mPreviewTexture);

        SurfaceTexture surfaceTexture = mPreviewTexture.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewRequestBuilder.addTarget(surface);

        mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (null == mCameraDevice) {
                    return;
                }
                cameraCaptureSession = session;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        }, handler);

    }

    private void updatePreview() throws CameraAccessException {
        previewRequest = mPreviewRequestBuilder.build();
        cameraCaptureSession.setRepeatingRequest(previewRequest, new CameraCaptureSession.CaptureCallback() {

        }, handler);
    }

    private void createImagerReader() {
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                saveImage(reader);
            }
        }, handler);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreviewTexture.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
        });
    }

    private void saveImage(ImageReader reader) {
        mFile = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
        Image mImage = reader.acquireNextImage();
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);
//            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private void requestFullScreenActivity() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                try {
                    takePicture();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn2:
                if (!isRecording) {
                    isRecording = true;
                    startRecordingVideo();
                } else {
                    isRecording = false;
                    stopRecordingVideo();
                }
                break;
        }
    }

    private void stopRecordingVideo() {
        mediaRecorder.stop();
        mediaRecorder.reset();

        closeSessionAndImageReader();
        try {
            createCameraPreviewSession();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void startRecordingVideo() {
        //TODO 1 关闭预览session
        closeSession();
        choosePreviewAndCaptureSize();//TODO 2 设置大小
        setUpMediaRecoder();//TODO 3 设置录像参数 MediaRecord

        SurfaceTexture surfaceTexture = mPreviewTexture.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mediaRecorder.getSurface());
//            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //TODO 4 创建录像Session
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mediaRecorder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        updatePreview();
                        mediaRecorder.start();
                        Log.d("mediaRecorder.start();", "" + mediaRecorder);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void choosePreviewAndCaptureSize() {

    }

    private void setUpMediaRecoder() {
        mFile = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".mp4");
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(mFile.getPath());
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void closeCamera() {
        closeSessionAndImageReader();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void closeSessionAndImageReader() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void closeSession() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    private void takePicture() throws CameraAccessException {
        CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(mImageReader.getSurface());
        cameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Toast.makeText(MainActivity.this, "Saved: " + mFile + ".jpg", Toast.LENGTH_LONG).show();
            }
        }, handler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBackgroundThread();
    }
}
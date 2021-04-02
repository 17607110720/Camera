package com.example.mycarmera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.example.mycarmera.CameraConstant.ADD_WATER_MARK;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraController {
    private AutoFitTextureView mPreviewTexture;
    private String mCameraId = "0";
    private Size mPreviewSize = new Size(1440, 1080);//预览尺寸
    private Size mCaptureSize = new Size(1440, 1080);//拍照尺寸
    private Size mVideoSize = new Size(1920, 1080);//录像尺寸


    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private File mFile;


    private CameraManager manager;

    private Activity mActivity;

    public static final float PREVIEW_SIZE_RATIO_OFFSET = 0.01f;
    private float mTargetRatio = 1.333f;
    private MediaRecorder mMediaRecorder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private int mCurrentMode = CameraConstant.PHOTO_MODE;


    public CameraController(Activity activity, AutoFitTextureView textureView) {
        this.mPreviewTexture = textureView;
        this.mActivity = activity;
        manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
    }

    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    public void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    //mStateCallback是相机状态回调
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            Log.d("mCameraDevice","mCameraDevice:::"+mCameraDevice);
//            createImagerReader();
            choosePreviewAndCaptureSize();
            createCameraPreviewSession();//打开相机成功的话，获取CameraDevice，然后创建会话--createCameraPreviewSession()
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
            mActivity.finish();
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {//图片有效回调
            saveImage(reader);//通知ImageSaver线程保存图片，//reader.acquireNextImage()获取图片image
        }

    };


    public void openCamera() throws CameraAccessException {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
//            requestCameraPermission();
            return;
        }
//        Log.d("mCurrentMode",""+mCurrentMode);
//        if (mCurrentMode == CameraConstant.VIDEO_MODE) {
            mMediaRecorder = new MediaRecorder();
//        }

        //获取CameraManager对象，然后真正打开相机
        //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
        manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
    }


    private void createImagerReader() {
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                ImageFormat.JPEG, /*maxImages*/2);
        mImageReader.setOnImageAvailableListener(
                mOnImageAvailableListener, mBackgroundHandler);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreviewTexture.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
        });

    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {//预览回调
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

        }

    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mPreviewTexture.getSurfaceTexture();//通过mTextureView获取SurfaceTexture
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());////设置TextureView的缓冲区大小
            //获取Surface显示预览数据
            Surface surface = new Surface(texture);

//            Log.d("mCameraDevice","mCameraDevice:"+mCameraDevice);

            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);////创建TEMPLATE_PREVIEW预览CaptureRequest.Builder
            mPreviewRequestBuilder.addTarget(surface);//CaptureRequest.Builder中添加Surface，即mTextureView获取创建的Surface
            //创建会话，获得CaptureRequest对象，通过CaptureRequest发送重复请求捕捉画面，开启预览。
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {//创建会话

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {//创建会话成功
                            if (null == mCameraDevice) {
                                return;
                            }
                            mCaptureSession = cameraCaptureSession;//从onConfigured参数获取mCaptureSession

                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(//创建会话失败
                                                      @NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void updatePreview() {
        try {
            // Finally, we start displaying the camera preview.
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(mPreviewRequest,
                    mCaptureCallback, mBackgroundHandler);//设置预览,setRepeatingRequest不断的重复mPreviewRequest请求捕捉画面，常用于预览或者连拍场景。

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void saveImage(ImageReader reader) {
        Image mImage = reader.acquireNextImage();
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        boolean waterMark = SharedPreferencesController.getInstance(mActivity).spGetBoolean(ADD_WATER_MARK);

        if (waterMark) {
            saveWithWaterMark(bytes, mImage);
        } else {
            saveNoWaterMark(bytes, mImage);
        }


    }

    private void saveWithWaterMark(byte[] bytes, Image mImage) {
        Bitmap bitmapStart = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

//        Matrix matrix = new Matrix();
//        matrix.postRotate(getJpegRotation(mCameraId, mPhoneOrientation));
//        if (lenFaceFront()) {
//            matrix.postScale(-1, 1);
//        }

        Bitmap bitmapSrc = Bitmap.createBitmap(bitmapStart, 0, 0, bitmapStart.getWidth(), bitmapStart.getHeight(), null, true);
        mCameraCallback.onThumbnailCreated(bitmapSrc);//添加缩略图
        Bitmap bitmapNew = Bitmap.createBitmap(bitmapSrc.getWidth(), bitmapSrc.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasNew = new Canvas(bitmapNew);
        canvasNew.drawBitmap(bitmapSrc, 0, 0, null);

        Paint paintText = new Paint();
        paintText.setColor(Color.argb(80, 255, 255, 255));

        paintText.setTextSize(500);
//        if (lenFaceFront()) {
//            paintText.setTextSize(60);
//        } else {
//            paintText.setTextSize(150);
//        }


        paintText.setDither(true);
        paintText.setFilterBitmap(true);
        Rect rectText = new Rect();
        String drawTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        paintText.getTextBounds(drawTime, 0, drawTime.length(), rectText);
        int beginX = bitmapNew.getWidth() - rectText.width() - 100;
        int beginY = bitmapNew.getHeight() - rectText.height();
        canvasNew.drawText(drawTime, beginX, beginY, paintText);

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.flush();
            Uri photouri = Uri.fromFile(mFile);
            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
//        mCameraCallback.onTakePictureFinished();

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

    private void saveNoWaterMark(byte[] bytes, Image mImage) {
        BitmapRegionDecoder decoder = null;

        try {
            decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.length, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);

        int w = opt.outWidth;
        int h = opt.outHeight;
        int d = w > h ? h : w;

        final int target = 40;
        int sample = 1;
        if (d > target) {
            while (d / sample / 2 > target) {
                sample *= 2;
            }
        }
        int st = sample * target;
        final Rect rect = new Rect((w - st) / 2, (h - st) / 2, (w + st) / 2, (h + st) / 2);

        Bitmap showThumbnail = decoder.decodeRegion(rect, opt);
//        Matrix matrix = new Matrix();
//        matrix.postRotate(getJpegRotation(mCameraId, mPhoneOrientation));
//        if (lenFaceFront()) {
//            matrix.postScale(-1, 1);
//        }
        Bitmap srcBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        Bitmap bitmapThumbnail = Bitmap.createBitmap(showThumbnail, 0, 0, showThumbnail.getWidth(), showThumbnail.getHeight(), null, true);
        mCameraCallback.onThumbnailCreated(bitmapThumbnail);//添加缩略图

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);//保存图片到文件

            Uri photouri = Uri.fromFile(mFile);

            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
//            mCameraCallback.onTakePictureFinished();
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


    public void takepicture() {
//        //拍照数据会由imageSaver处理，保存到文件，
//        mFile = new File(Environment.getExternalStorageDirectory(),  System.currentTimeMillis() + ".jpg");

        CaptureRequest.Builder captureBuilder = null;
        try {
            //设置TEMPLATE_STILL_CAPTURE拍照CaptureRequest.Builder
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //添加拍照mImageReader为Surface
            captureBuilder.addTarget(mImageReader.getSurface());
//
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        //拍照流程执行完成回调
        //然后通过CameraCaptureSession.CaptureCallback回调解除锁定，回复预览界面
        CameraCaptureSession.CaptureCallback CaptureCallback
                = new CameraCaptureSession.CaptureCallback() {

            // 拍照完成时激发该方法
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                //提示拍照图片已经保存
                Toast.makeText(mActivity, "Saved: " + mFile, Toast.LENGTH_LONG).show();
                Log.d(TAG, mFile.toString());
            }
        };

        try {
//            mCaptureSession.stopRepeating();//停止预览,停止任何一个正常进行的重复请求。
//            mCaptureSession.abortCaptures();//中断Capture,尽可能快的取消当前队列中或正在处理中的所有捕捉请求。
            //重新Capture进行拍照，这时mImageReader的回调会执行并保存图片
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void switch_16_9() {
        mTargetRatio = 1.777f;
        closeSessionAndImageReader();
//        closeCamera();
        //重新选择大小
        choosePreviewAndCaptureSize();
        //起预览
        createCameraPreviewSession();

    }

    public void switch_4_3() {
        mTargetRatio = 1.333f;
        closeSessionAndImageReader();
//        closeCamera();
        //重新选择大小
        choosePreviewAndCaptureSize();
        //起预览
        createCameraPreviewSession();
    }


    public void closeCamera() {
        closeSessionAndImageReader();
//        closeMediaRecorder();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public void closeSession() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    public void closeSessionAndImageReader() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }


    private void choosePreviewAndCaptureSize() {
        CameraCharacteristics characteristics
                = null;
        try {
            characteristics = manager.getCameraCharacteristics(String.valueOf(mCameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

//        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);//方向

        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] previewSizeMap = map.getOutputSizes(SurfaceTexture.class);
        for (int i = 0; i < previewSizeMap.length; i++) {
            Log.d("yanweitim", "testing = " + previewSizeMap[i].getWidth() + "," + previewSizeMap[i].getHeight());
        }
        Size[] captureSizeMap = map.getOutputSizes(ImageFormat.JPEG);
        int screenWidth = getScreenWidth(mActivity.getApplicationContext());
        mPreviewSize = getPreviewSize(previewSizeMap, mTargetRatio, screenWidth);
//        if (mCurrentMode == CameraConstant.VIDEO_MODE) {
        mVideoSize = getVideoSize(mTargetRatio, previewSizeMap);
        ;
//        }


        mCaptureSize = getPictureSize(mTargetRatio, captureSizeMap);

        createImagerReader();

    }


    public Size getVideoSize(float targetRatio, Size[] mapVideo) {
        Size maxVideoPicSize = new Size(0, 0);
        for (Size size : mapVideo) {
            float ratio = size.getWidth() / (float) size.getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            if (size.getWidth() * size.getHeight() >= maxVideoPicSize.getWidth() * maxVideoPicSize.getHeight()) {
                maxVideoPicSize = size;
            }
        }
        return maxVideoPicSize;
    }

    public Size getPreviewSize(Size[] mapPreview, float targetRatio, int screenWidth) {
        Size previewSize = null;
        int minOffSize = Integer.MAX_VALUE;
        for (int i = 0; i < mapPreview.length; i++) {
            float ratio = mapPreview[i].getWidth() / (float) mapPreview[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            int diff = Math.abs(mapPreview[i].getHeight() - screenWidth);
            if (diff < minOffSize) {
                previewSize = mapPreview[i];
                minOffSize = Math.abs(mapPreview[i].getHeight() - screenWidth);
            } else if ((diff == minOffSize) && (mapPreview[i].getHeight() > screenWidth)) {
                previewSize = mapPreview[i];
            }
        }
        return previewSize;
    }

    public Size getPictureSize(float targetRatio, Size[] mapPicture) {
        Size maxPicSize = new Size(0, 0);
        for (int i = 0; i < mapPicture.length; i++) {
            float ratio = mapPicture[i].getWidth() / (float) mapPicture[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            if (mapPicture[i].getWidth() * mapPicture[i].getHeight() >= maxPicSize.getWidth() * maxPicSize.getHeight()) {
                maxPicSize = mapPicture[i];
            }
        }
        return maxPicSize;
    }


    public int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    //前后置镜头转换
    public void switch_camera_id() {
        closeCamera();
        updateCameraId();
        try {
            openCamera();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void updateCameraId() {
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(String.valueOf(mCameraId));

            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                mCameraId = "0";//后置
            } else {
                mCameraId = "1";//前置
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopRecordingVideo() {
        // UI
        // Stop recording
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mCameraCallback.stopRecordVideo();


        //mNextVideoAbsolutePath = null;
//        startPreview();
        closeSession();
        choosePreviewAndCaptureSize();
        createCameraPreviewSession();

    }


    public void startRecordingVideo() {
        try {
            closeSession();//TODO 1 关闭预览session
            choosePreviewAndCaptureSize();//TODO 2 设置大小
            setUpMediaRecorder();//TODO 3 设置录像参数 MediaRecord
            Log.d("yanweitim", "setUpMediaRecorder startRecordingVideo");

            SurfaceTexture texture = mPreviewTexture.getSurfaceTexture();
            assert texture != null;//如果为真那程序继续执行，如果为假就终止程序的执行
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);//创建视频录制的请求
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewRequestBuilder.addTarget(recorderSurface);

//            //录像时的拍照
//            Surface picSurface = mImageReader.getSurface();
//            surfaces.add(picSurface);
//            mPreviewRequestBuilder.addTarget(picSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            //TODO 4 创建录像Session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                    mCaptureSession = cameraCaptureSession;
                    updatePreview();
                    mCameraCallback.startRecordVideo();
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void setUpMediaRecorder() throws IOException {
        Log.d("yanweitim", "setUpMediaRecorder");

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mFile.getPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.prepare();
        Log.d("yanweitim", "setUpMediaRecorder prepare");
    }


    public void setCurrentMode(int currentMode) {
        mCurrentMode = currentMode;
    }
    public void setTargetRatio(float ratio) {
        mTargetRatio = ratio;
    }

    public void setPath(File file) {
        mFile = file;
    }

    private CameraControllerInterFaceCallback mCameraCallback;


    interface CameraControllerInterFaceCallback {
        void startRecordVideo();

        void stopRecordVideo();

        void onThumbnailCreated(Bitmap bitmap);
    }

    public void setCameraControllerInterFaceCallback(CameraControllerInterFaceCallback cameraCallback) {
        mCameraCallback = cameraCallback;
    }
}

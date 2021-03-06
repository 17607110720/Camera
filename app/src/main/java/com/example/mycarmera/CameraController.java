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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.util.RangeValueIterator;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.accessibility.AccessibilityNodeInfo;
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

import static com.example.mycarmera.CameraConstant.ADD_WATER_MARK;
import static com.example.mycarmera.DensityUtils.dip2px;
import static com.example.mycarmera.Utils.getScreenWidth;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraController {
    private AutoFitTextureView mPreviewTexture;
    private int mCameraId = 0;
    private static final int BACK_CAMERA_ID = 0;
    private static final int FRONT_CAMERA_ID = 1;
    private Size mPreviewSize = new Size(1440, 1080);//????????????
    private Size mCaptureSize = new Size(4000, 3000);//????????????
    private Size mVideoSize = new Size(1920, 1080);//????????????


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
    private int mPhoneOrientation;
    private int mSensorOrientation;
    private boolean mStartTapFocus = false;
    private boolean mFocusTakePicture = false;
    public int progress;


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

    //mStateCallback?????????????????????
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            choosePreviewAndCaptureSize();
            createCameraPreviewSession();//?????????????????????????????????CameraDevice?????????????????????--createCameraPreviewSession()
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
        public void onImageAvailable(ImageReader reader) {//??????????????????
            saveImage(reader);//??????ImageSaver?????????????????????//reader.acquireNextImage()????????????image
        }

    };


    public void openCamera() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
//            requestCameraPermission();
            return;
        }
        if (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_MOTION_MODE) {
            mMediaRecorder = new MediaRecorder();
        }

        try {
            //???????????????????????????????????????????????????????????????????????????stateCallback????????????????????????????????????????????????????????????Callback???????????????????????????null??????????????????????????????
            manager.openCamera(String.valueOf(mCameraId), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createImageReader() {
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                ImageFormat.JPEG, /*maxImages*/1);//RAW_SENSOR YUV_420_888
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
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    public void process(CaptureResult result) {
        Integer integer = result.get(CaptureResult.SENSOR_SENSITIVITY);
        Log.d("process", "" + integer);

        //Face[] faces = result.get(CaptureResult.STATISTICS_FACES);

        /*FLASH_STATE_UNAVAILABLE--0
        FLASH_STATE_CHARGING--1
        FLASH_STATE_READY--2
        FLASH_STATE_FIRED--3
        FLASH_STATE_PARTIAL--4*/
        /*Integer integer = result.get(CaptureResult.FLASH_STATE);//???????????????
        System.out.println("integer = " + integer);*/


        if (mFocusTakePicture) {
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);

            //AF_STATE_FOCUSED_LOCKED	AF ??????????????????????????????????????????
            //AF_STATE_NOT_FOCUSED_LOCKED	AF ???????????????????????????????????????
            //AF_STATE_PASSIVE_FOCUSED	????????????????????????????????????????????????????????????
            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
                mFocusTakePicture = false;
                beginCaptureStillPicture();
            }
        }

        if (mStartTapFocus) {
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);

            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
                mStartTapFocus = false;
                if (mCameraCallback != null)
                    mCameraCallback.onTapFocusFinish();
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                updateCapture();
            }
        }
    }

    public void updateCapture() {
        try {
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSession.capture(mPreviewRequest,
                    mCaptureCallback, mBackgroundHandler);
//            mCaptureSession.captureBurst();
//            mCaptureSession.setRepeatingBurst();


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mPreviewTexture.getSurfaceTexture();//??????mTextureView??????SurfaceTexture
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());//??????TextureView??????????????????
            //??????Surface??????????????????
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//??????TEMPLATE_PREVIEW??????CaptureRequest.Builder
            mPreviewRequestBuilder.addTarget(surface);//CaptureRequest.Builder?????????Surface??????mTextureView???????????????Surface
            //?????????????????????CaptureRequest???????????????CaptureRequest????????????????????????????????????????????????
            //??????????????????????????????????????????????????????????????????Surface???????????????????????????CameraCaptureSession???????????????????????????????????????????????????onConfigured????????????????????????????????????Callback???????????????????????????null??????????????????????????????
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {//????????????

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {//??????????????????
                            if (null == mCameraDevice) {
                                return;
                            }
                            mCaptureSession = cameraCaptureSession;//???onConfigured????????????mCaptureSession
                            setPreviewFrameParams();

                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(//??????????????????
                                                      @NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setPreviewFrameParams() {//???????????????
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,//iso
                progress);
        Log.d("onProgressChanged", "progress:" + progress);

        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                100l);//?????????????????? ns

        updatePreview();
//        mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectModes[faceDetectModes.length - 1]);//????????????????????????
    }


    public void updatePreview() {
        try {
            // Finally, we start displaying the camera preview.
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(mPreviewRequest,
                    mCaptureCallback, mBackgroundHandler);//????????????,setRepeatingRequest???????????????mPreviewRequest?????????????????????????????????????????????????????????
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

        Matrix matrix = new Matrix();
        matrix.postRotate(getJpegRotation(mCameraId, mPhoneOrientation));
        if (lenFaceFront()) {
            matrix.postScale(-1, 1);
        }

        Bitmap bitmapSrc = Bitmap.createBitmap(bitmapStart, 0, 0, bitmapStart.getWidth(), bitmapStart.getHeight(), matrix, true);
        mCameraCallback.onThumbnailCreated(bitmapSrc);//???????????????

        Bitmap bitmapNew = Bitmap.createBitmap(bitmapSrc.getWidth(), bitmapSrc.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasNew = new Canvas(bitmapNew);
        canvasNew.drawBitmap(bitmapSrc, 0, 0, null);

        Paint paintText = new Paint();
        paintText.setColor(Color.argb(80, 255, 255, 255));

        if (lenFaceFront()) {
            paintText.setTextSize(60);
        } else {
            paintText.setTextSize(150);
        }

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
            mCameraCallback.onTakePictureFinished();

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
        Matrix matrix = new Matrix();
        matrix.postRotate(getJpegRotation(mCameraId, mPhoneOrientation));
        if (lenFaceFront()) {
            matrix.postScale(-1, 1);
        }
//        Bitmap srcBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        Bitmap bitmapThumbnail = Bitmap.createBitmap(showThumbnail, 0, 0, showThumbnail.getWidth(), showThumbnail.getHeight(), matrix, true);
        mCameraCallback.onThumbnailCreated(bitmapThumbnail);//???????????????
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);//?????????????????????

            Uri photouri = Uri.fromFile(mFile);

            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
            mCameraCallback.onTakePictureFinished();
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

    private void simpleSaveNoWaterMark(byte[] bytes, Image image) {
        Bitmap srcBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

        int thumnailSize = (int) dip2px(mActivity, mActivity.getResources().getDimension(R.dimen.thumbnail_size));
        //BitmapFactory.decodeByteArray(bytes,)
        Bitmap thumbnailBitmap = ThumbnailUtils.extractThumbnail(srcBitmap, thumnailSize, thumnailSize, 1);
//        Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(srcBitmap, thumnailSize, thumnailSize, false);
        mCameraCallback.onThumbnailCreated(thumbnailBitmap);

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);//?????????????????????

            Uri photouri = Uri.fromFile(mFile);

            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
            mCameraCallback.onTakePictureFinished();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean lenFaceFront() {
        CameraCharacteristics cameraInfo = null;
        try {
            cameraInfo = manager.getCameraCharacteristics(String.valueOf(mCameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {//front camera
            return true;
        }

        return false;
    }

    public int getJpegRotation(int cameraId, int orientation) {
        int rotation = 0;
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = 0;
        }
        if (cameraId == -1) {
            cameraId = 0;
        }
        CameraCharacteristics cameraInfo = null;
        try {
            cameraInfo = manager.getCameraCharacteristics(String.valueOf(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {//front camera
            rotation = (mSensorOrientation - orientation + 360) % 360;
        } else {// back-facing camera
            rotation = (mSensorOrientation + orientation + 360) % 360;
        }
        return rotation;
    }

    public void beginCaptureStillPicture() {
        try {
            //??????????????????imageSaver???????????????????????????
            //??????TEMPLATE_STILL_CAPTURE??????CaptureRequest.Builder
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //????????????mImageReader???Surface
            captureBuilder.addTarget(mImageReader.getSurface());


            //??????????????????????????????
            //????????????CameraCaptureSession.CaptureCallback???????????????????????????????????????
            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                // ??????????????????????????????
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    //??????????????????????????????
                    //Toast.makeText(mActivity, "Saved: " + mFile, Toast.LENGTH_LONG).show();
                }
            };
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegRotation(mCameraId, mPhoneOrientation));//90 0 180 270

            //mCaptureSession.stopRepeating();//????????????,????????????????????????????????????????????????
            //mCaptureSession.abortCaptures();//??????Capture,??????????????????????????????????????????????????????????????????????????????
            //??????Capture?????????????????????mImageReader?????????????????????????????????
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void prepareCaptureStillPicture() {
        boolean focusPicture = SharedPreferencesController.getInstance(mActivity).spGetBoolean(CameraConstant.FOCUS_TAKE_PICTURE);
        if (focusPicture) {
            mFocusTakePicture = focusPicture;
            return;
        }
        beginCaptureStillPicture();
    }

    public void switch_16_9() {
        mTargetRatio = 1.777f;
//        closeSessionAndImageReader();
        closeCamera();
//        ??????????????????
//        choosePreviewAndCaptureSize();
//        //?????????
//        createCameraPreviewSession();
        openCamera();
    }

    public void switch_4_3() {
        mTargetRatio = 1.333f;
//        closeSessionAndImageReader();
        closeCamera();
        //??????????????????
//        choosePreviewAndCaptureSize();
//        //?????????
//        createCameraPreviewSession();
        openCamera();
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

    public void closeMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }


    private void choosePreviewAndCaptureSize() {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(String.valueOf(mCameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);//??????
        /*Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);//????????????
        for (int i = 0; i < ranges.length; i++) {
            System.out.println("ranges = " + ranges[1].getUpper());
        }*/

        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] previewSizeMap = map.getOutputSizes(SurfaceTexture.class);//preview
        Size[] captureSizeMap = map.getOutputSizes(ImageFormat.JPEG);//??????
        int screenWidth = getScreenWidth(mActivity.getApplicationContext());
        mPreviewSize = getPreviewSize(previewSizeMap, mTargetRatio, screenWidth);
        if (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_MOTION_MODE) {
            mVideoSize = mPreviewSize;
//            mVideoSize = getVideoSize(mTargetRatio, previewSizeMap);
        }
        mCaptureSize = getPictureSize(mTargetRatio, captureSizeMap);

        createImageReader();
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


    public int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    //?????????????????????
    public void switch_camera_id() {
        closeCamera();
        updateCameraId();
        openCamera();

    }

    //???????????????
    public void openFlashMode() {
        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        updatePreview();
    }

    //???????????????
    public void closeFlashMode() {
        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        updatePreview();
    }

    public void setPhoneDeviceDegree(int degree) {
        mPhoneOrientation = degree;
    }

    public void updateManualFocus(Rect rect) {
        if (mStartTapFocus) return;
        mStartTapFocus = true;
        //CONTROL_AF_REGIONS?????????????????????????????????????????????????????? (FOV) ???????????????????????????????????????????????????????????? AF ??????
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
        //CONTROL_AE_REGIONS??????????????????????????????????????????????????? FOV ??????????????????????????????????????? OFF ?????????????????? AE ?????????
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        //CONTROL_AE_PRECAPTURE_TRIGGER???????????????????????????????????????????????????????????????
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        updateCapture();
    }

    /**
     * ??????????????????
     *
     * @param x??????????????????x??????
     * @param y:         ???????????????y??????
     */
    public Rect getFocusRect(int x, int y) {
        int screenW = getScreenWidth(mActivity.getApplicationContext());//??????????????????
        int screenH = getScreenHeight(mActivity.getApplicationContext());//??????????????????

        //???????????????SCALER_CROP_REGION???????????????????????????????????????????????????????????????????????????width???height
        int realPreviewWidth = mPreviewSize.getHeight();
        int realPreviewHeight = mPreviewSize.getWidth();

        //?????????????????????????????????????????????????????????????????????????????????????????????
        float focusX = realPreviewWidth * 1.0f / screenW * x;
        float focusY = realPreviewHeight * 1.0f / screenH * y;

        //??????SCALER_CROP_REGION?????????????????????????????????Rect
        Rect totalPicSize = mPreviewRequest.get(CaptureRequest.SCALER_CROP_REGION);

        //???????????????????????????????????????
        int cutDx = (totalPicSize.height() - mPreviewSize.getHeight()) / 2;

        //??????????????????10dp???????????????????????????????????????????????????10dp???????????????????????????????????????
        float width = dip2px(mActivity, 10.0f);
        float height = dip2px(mActivity, 10.0f);

        //????????????????????????Rect
        return new Rect((int) focusY, (int) focusX + cutDx, (int) (focusY + height), (int) (focusX + cutDx + width));
    }

    private void updateCameraId() {
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(String.valueOf(mCameraId));

            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                mCameraId = BACK_CAMERA_ID;//??????
                mCameraId = BACK_CAMERA_ID;//??????
            } else {
                mCameraId = FRONT_CAMERA_ID;//??????
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopRecordingVideo() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        Uri uri = Uri.fromFile(mFile);
        //final int target = mActivity.getResources().getDimensionPixelSize(R.dimen.thumbnail_size);
        //Bitmap bitmapThumbnail = createVideoThumbnailBitmap(mFile.toString(), null, target);

        Bitmap bitmapThumbnail = ThumbnailUtils.createVideoThumbnail(mFile.toString(), MediaStore.Images.Thumbnails.MINI_KIND);
        if (mCameraCallback != null)
            mCameraCallback.onThumbnailCreated(bitmapThumbnail);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

        closeSession();
        choosePreviewAndCaptureSize();
        createCameraPreviewSession();

    }


    public void startRecordingVideo() {
        try {
            closeSession();//TODO 1 ????????????session
            choosePreviewAndCaptureSize();//TODO 2 ????????????
            setUpMediaRecorder();//TODO 3 ?????????????????? MediaRecord

            SurfaceTexture texture = mPreviewTexture.getSurfaceTexture();
            assert texture != null;//????????????????????????????????????????????????????????????????????????
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);//???????????????????????????
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewRequestBuilder.addTarget(recorderSurface);

            //??????????????????
            Surface picSurface = mImageReader.getSurface();
            surfaces.add(picSurface);
//            mPreviewRequestBuilder.addTarget(picSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            //TODO 4 ????????????Session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                    mCaptureSession = cameraCaptureSession;
                    updatePreview();
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mFile.getPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        if (mCurrentMode == CameraConstant.SLOW_MOTION_MODE) {
            mMediaRecorder.setCaptureRate(120);//?????????
            mMediaRecorder.setCaptureRate(10);//?????????//????????????
        }
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(getJpegRotation(mCameraId, mPhoneOrientation));
        mMediaRecorder.prepare();
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


        void onThumbnailCreated(Bitmap bitmap);

        void onTakePictureFinished();

        void onTapFocusFinish();

    }

    public void setCameraControllerInterFaceCallback(CameraControllerInterFaceCallback cameraCallback) {
        mCameraCallback = cameraCallback;
    }
}

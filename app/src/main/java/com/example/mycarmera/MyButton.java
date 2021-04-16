package com.example.mycarmera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class MyButton extends View {
    private Paint mPaint;
    private Paint mInnerPaint;
    private int mWidth;
    private int mHeight;
    private int mRadius;
    private boolean mVideoRecordState;
    private RectF mRectVideoRecording;


    private int mCurrentMode = CameraConstant.PHOTO_MODE;


    public MyButton(Context context) {//new
        this(context, null);
    }

    public MyButton(Context context, @Nullable AttributeSet attrs) {//xml
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(15);
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);

        mInnerPaint = new Paint();
        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setColor(Color.WHITE);
        mInnerPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        mRadius = mWidth / 2 - 40;
        mRectVideoRecording = new RectF();
        mRectVideoRecording.left = mWidth / 4 + 10;
        mRectVideoRecording.right = mWidth * 3 / 4 - 10;
        mRectVideoRecording.top = mHeight / 4 + 10;
        mRectVideoRecording.bottom = mHeight * 3 / 4 - 10;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(210, 210);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrentMode == CameraConstant.SLOW_MOTION_MODE) {//慢动作模式，设置外圆为虚线
            mPaint.setPathEffect(new DashPathEffect(new float[]{4, 4}, 0));
        } else {
            mPaint.setPathEffect(null);
        }
        //如果是录像和慢动作模式，则设置内圆为红色
        if (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_MOTION_MODE) {
            mInnerPaint.setColor(Color.RED);

            if (!mVideoRecordState) {//如果不是在录像状态，则设置内圆为圆形
                canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mInnerPaint);
            }
            //否则为方形
            canvas.drawRoundRect(mRectVideoRecording, 20.0f, 20.0f, mInnerPaint);
        } else {//拍照模式或其他模式
            mInnerPaint.setColor(Color.WHITE);
            canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mInnerPaint);
        }
        //画外圆
        canvas.drawCircle(mWidth / 2, mHeight / 2, mWidth / 2 - 25, mPaint);
    }

    public void startPictureAnimator() {
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(mWidth / 2 - 40, mWidth / 2 - 45, mWidth / 2 - 40);
        valueAnimator.setDuration(400);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mRadius = (int) value;
                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

            }
        });
        valueAnimator.start();
    }


    public void setCurrentMode(int currentMode) {
        mCurrentMode = currentMode;
        invalidate();
    }

    public void setVideoRecordingState(boolean recording) {
        mVideoRecordState = recording;
        invalidate();
    }

    public MyCameraButtonClickListener myCameraButtonClickListener;

    public interface MyCameraButtonClickListener {
        void onMyCameraButtonClick(int mode);
    }

    public void setOnBaseViewClickListener(MyCameraButtonClickListener lister) {
        myCameraButtonClickListener = lister;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (myCameraButtonClickListener != null) {
                    myCameraButtonClickListener.onMyCameraButtonClick(mCurrentMode);
                }
                break;
        }
        return true;
    }
}

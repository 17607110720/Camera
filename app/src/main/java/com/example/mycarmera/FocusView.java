package com.example.mycarmera;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

import androidx.annotation.Nullable;

public class FocusView extends View {
    private Paint mPaint;
    private float mStrokeWidth = 4.0f;
    private int mInnerRadiusDP = 7;
    public static final int mOuterRadiusDP = 40;
    private int mInnerRadiusPX;
    private int mOuterRadiusPX;
    private float mViewCenterX;
    private float mViewCenterY;
    private boolean mNeedToDrawView;
    private int animatorInnerRadius;
    private int animatorOuterRadius;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initData(context);
    }

    private void initData(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);

        mInnerRadiusPX = DensityUtils.dip2px(context, mInnerRadiusDP);
        mOuterRadiusPX = DensityUtils.dip2px(context, mOuterRadiusDP);

        animatorInnerRadius = DensityUtils.dip2px(context, mInnerRadiusDP);
        animatorOuterRadius = DensityUtils.dip2px(context, mOuterRadiusDP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mNeedToDrawView) {
            canvas.drawCircle(mViewCenterX, mViewCenterY, mOuterRadiusPX, mPaint);
            canvas.drawCircle(mViewCenterX, mViewCenterY, mInnerRadiusPX, mPaint);
        }
    }

    public void setFocusViewCenter(float x, float y) {
        mViewCenterX = x;
        mViewCenterY = y;
        invalidate();
    }

    public void setNeedToDrawView(boolean b) {
        mNeedToDrawView = b;
        invalidate();
    }

    public void playAnimation() {
        ValueAnimator animIner = ValueAnimator.ofFloat(animatorInnerRadius, animatorInnerRadius - 5, animatorInnerRadius);
        animIner.setDuration(500);
        animIner.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue();
                mInnerRadiusPX = (int) currentValue;
                invalidate();
            }
        });

        ValueAnimator animOuter = ValueAnimator.ofFloat(animatorOuterRadius, animatorOuterRadius + 10, animatorOuterRadius);
        animOuter.setDuration(500);
        animOuter.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue();
                mOuterRadiusPX = (int) currentValue;
                invalidate();
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animIner, animOuter);
        set.start();
    }
}

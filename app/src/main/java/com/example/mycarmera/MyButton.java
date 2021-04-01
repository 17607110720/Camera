package com.example.mycarmera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class MyButton extends View {
    public Paint mPaint;


    public MyButton(Context context) {//new
        this(context, null);
    }

    public MyButton(Context context, @Nullable AttributeSet attrs) {//xml
        super(context, attrs);
        mPaint = new Paint();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(176, 176);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(15);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(88, 88, 73, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(88, 88, 60, mPaint);
//        mPaint.setColor(Color.BLACK);
//        Rect rect = new Rect(23, 50, 100, 160);
//        canvas.drawRect(rect, mPaint);

    }

    public MyCameraButtonClickListener myCameraButtonClickListener;

    public interface MyCameraButtonClickListener {
        void onMyCameraButtonClick();
    }

    public void setOnBaseViewClickListener(MyCameraButtonClickListener lister) {
        myCameraButtonClickListener = lister;
    }

}

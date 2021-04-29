package com.example.mycarmera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import android.util.Log;
import android.view.View;

import static com.example.mycarmera.Utils.getScreenWidth;


public class GridLine extends View {
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private Paint mPaint;
    private int width;
    private int height;

    public GridLine(Context context) {
        super(context);
    }

    public GridLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(1);

    }

    public void setAspectRatio(int width, int heigth) {
        if (width < 0 || heigth < 0) {
            throw new IllegalArgumentException("长宽参数不能为负");
        }
        mRatioHeight = heigth;
        mRatioWidth = width;
        requestLayout();//宽高比之后重新绘制
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        if (mRatioHeight == 0 || mRatioWidth == 0) {
            setAspectRatio(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
        Log.d("onMeasure", "width=" + width + ",height=" + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawLine(mRatioWidth / 3, 0, mRatioWidth / 3, mRatioHeight, mPaint);
//        canvas.drawLine(mRatioWidth * 2 / 3, 0, mRatioWidth * 2 / 3, mRatioHeight, mPaint);
//        canvas.drawLine(0, mRatioHeight / 3 + height - mRatioHeight, mRatioWidth, mRatioHeight / 3 + height - mRatioHeight, mPaint);
//        canvas.drawLine(0, mRatioHeight*2 / 3 + height - mRatioHeight, mRatioWidth, mRatioHeight*2 / 3 + height - mRatioHeight, mPaint);
    }

}

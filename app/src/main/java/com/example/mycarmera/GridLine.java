package com.example.mycarmera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;


public class GridLine extends View {
    private int lineX = 2;
    private int lineY = 2;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private Paint mPaint;
    private int width;
    private int height;
    private int specifiedWeight;
    private int specifiedHeight;

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
        if (0 == mRatioWidth || 0 == mRatioHeight) {//初次绘制的情况
            setMeasuredDimension(width, height);
            specifiedWeight = width;//将当下绘制的SurfaceView的长宽比用于赋值，以便计算格线的位置
            specifiedHeight = height;
        } else {
            if (width < height * mRatioWidth / mRatioHeight)//哪边占比小就用它为绘制参考便，实际上是在选择同比例最大绘制范围
            {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);//设置SurfaceView的大小适应于预览流的大小
                specifiedWeight = width;//将当下绘制的SurfaceView的长宽比用于赋值，以便计算格线的位置
                specifiedHeight = width * mRatioHeight / mRatioWidth;
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                specifiedWeight = height * mRatioWidth / mRatioHeight;
                specifiedHeight = height;
            }
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawLine(350, 0, 350, height, mPaint);
//        canvas.drawLine(750, 0, 750, height, mPaint);
//        canvas.drawLine(0, 500, width, 500, mPaint);
//        canvas.drawLine(0, 900, width, 900, mPaint);

        int x = specifiedWeight / (lineX + 1);
        int y = specifiedHeight / (lineY + 1);
        for (int i = 1; i <= lineX; i++) {
            canvas.drawLine(x * i, 0, x * i, height, mPaint);//绘制直线的起始(x,y)与终止(x1,y1)与画笔。
        }

        for (int i = 1; i <= lineY; i++) {
            canvas.drawLine(0, y * i, width, y * i, mPaint);
        }
    }

}

package com.lcw.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.lcw.view.R;

/**
 * 自定义贴纸View
 * Create by: chenWei.li
 * Date: 2018/11/22
 * Time: 下午11:02
 * Email: lichenwei.me@foxmail.com
 */

/**
 * @Deprecated 基础贴纸类，废弃不再使用
 */
public class StickerView extends View implements View.OnTouchListener {

    private Bitmap mBitmap;//贴纸图片

    private Matrix mMatrix;//维护图像变化的矩阵
    private float[] mScrPoints;//矩阵变换前的点坐标
    private float[] mDstPoints;//矩阵变换后的点坐标
    private RectF mBitmapBound;//图片的外围边框的点坐标

    private boolean mCanTranslate;//标志是否可移动
    private boolean mCanScale;//标志是否可缩放
    private boolean mCanRotate;//标志是否可旋转

    private float mLastDistance;//记录上一次双指之间的距离
    private PointF mMidPoint = new PointF();//记录图片中心点
    private PointF mLastSinglePoint = new PointF();//记录上一次单指触摸屏幕的点坐标
    private PointF mLastDistancePoint = new PointF();//记录上一次双指触摸屏幕的点坐标
    private PointF mDistancePoint = new PointF();//记录当前双指触摸屏幕的点坐标

    private Paint mPaint;


    public StickerView(Context context) {
        super(context);
        init(context);
    }

    public StickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 完成一些初始化操作
     *
     * @param context
     */
    private void init(Context context) {

        //初始化画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GRAY);

        //初始化图像
        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.icon);
        //记录图像一些点位置
        mScrPoints = new float[]{
                0, 0,//左上
                mBitmap.getWidth(), 0,//右上
                mBitmap.getWidth(), mBitmap.getHeight(),//右下
                0, mBitmap.getHeight(),//左下
                mBitmap.getWidth() / 2, mBitmap.getHeight() / 2//中间点
        };
        //拷贝点位置
        mDstPoints = mScrPoints.clone();
        mBitmapBound = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());

        //初始化矩阵
        mMatrix = new Matrix();

        //移动图像到屏幕中心
//        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
//        float dx = displayMetrics.widthPixels / 2 - mBitmap.getWidth() / 2;
//        float dy = displayMetrics.heightPixels / 2 - mBitmap.getHeight() / 2;
//        translate(dx, dy);

        //设置触摸监听
        setOnTouchListener(this);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, mMatrix, null);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastSinglePoint.set(event.getX(), event.getY());
                if (isInStickerView(event)) {
                    //触摸点是否在贴纸范围内
                    mCanTranslate = true;
                }
                mCanScale = false;
                mCanRotate = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2 && isInStickerView(event)) {
                    mCanTranslate = false;
                    mCanScale = true;
                    mCanRotate = true;
                    //计算双指之间向量
                    mLastDistancePoint.set(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
                    //计算双指之间距离
                    mLastDistance = calculateDistance(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCanTranslate) {
                    translate(event.getX() - mLastSinglePoint.x, event.getY() - mLastSinglePoint.y);
                    mLastSinglePoint.set(event.getX(), event.getY());
                }
                if ((mCanScale || mCanRotate) && event.getPointerCount() == 2) {
                    //操作自由缩放
                    float distance = calculateDistance(event);
                    //根据双指移动的距离获取缩放因子
                    float scale = distance / mLastDistance;
                    scale(scale, scale, getMidPoint().x, getMidPoint().y);
                    mLastDistance = distance;
                    //操作自由旋转
                    mDistancePoint.set(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
                    rotate(calculateDegrees(mLastDistancePoint, mDistancePoint), getMidPoint().x, getMidPoint().y);
                    mLastDistancePoint.set(mDistancePoint.x, mDistancePoint.y);
                }
                break;
            case MotionEvent.ACTION_UP:
                reset();
                break;
        }
        invalidate();
        return true;
    }

    /**
     * 平移操作
     *
     * @param dx
     * @param dy
     */
    private void translate(float dx, float dy) {
        mMatrix.postTranslate(dx, dy);
        mMatrix.mapPoints(mDstPoints, mScrPoints);
    }

    /**
     * 缩放操作
     *
     * @param sx
     * @param sy
     * @param px
     * @param py
     */
    private void scale(float sx, float sy, float px, float py) {
        mMatrix.postScale(sx, sy, px, py);
        mMatrix.mapPoints(mDstPoints, mScrPoints);
    }

    /**
     * 旋转操作
     *
     * @param degrees
     * @param px
     * @param py
     */
    private void rotate(float degrees, float px, float py) {
        mMatrix.postRotate(degrees, px, py);
        mMatrix.mapPoints(mDstPoints, mScrPoints);
    }


    /**
     * 检测当前触摸是否在贴纸上
     *
     * @return
     */
    private boolean isInStickerView(MotionEvent motionEvent) {

        if (motionEvent.getPointerCount() == 1) {
            float[] dstPoints = new float[2];
            float[] srcPoints = new float[]{motionEvent.getX(), motionEvent.getY()};
            Matrix matrix = new Matrix();
            mMatrix.invert(matrix);
            matrix.mapPoints(dstPoints, srcPoints);
            if (mBitmapBound.contains(dstPoints[0], dstPoints[1])) {
                return true;
            }
        }

        if (motionEvent.getPointerCount() == 2) {
            float[] dstPoints = new float[4];
            float[] srcPoints = new float[]{motionEvent.getX(0), motionEvent.getY(0), motionEvent.getX(1), motionEvent.getY(1)};
            Matrix matrix = new Matrix();
            mMatrix.invert(matrix);
            matrix.mapPoints(dstPoints, srcPoints);
            if (mBitmapBound.contains(dstPoints[0], dstPoints[1]) || mBitmapBound.contains(dstPoints[2], dstPoints[3])) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取图像中心点
     *
     * @return
     */
    private PointF getMidPoint() {
        mMidPoint.set(mDstPoints[8], mDstPoints[9]);
        return mMidPoint;
    }

    /**
     * 计算两点之间的距离
     */
    private float calculateDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    /**
     * 计算旋转角度
     *
     * @param lastPoint
     * @param pointF
     * @return
     */
    private float calculateDegrees(PointF lastPoint, PointF pointF) {
        float lastDegrees = (float) Math.atan2(lastPoint.y, lastPoint.x);
        float currentDegrees = (float) Math.atan2(pointF.y, pointF.x);
        return (float) Math.toDegrees(currentDegrees - lastDegrees);
    }

    /**
     * 重置状态
     */
    private void reset() {
        mCanTranslate = false;
        mCanScale = false;
        mCanRotate = false;
        mLastDistance = 0f;
        mMidPoint.set(0f, 0f);
        mLastSinglePoint.set(0f, 0f);
        mLastDistancePoint.set(0f, 0f);
        mDistancePoint.set(0f, 0f);
    }

}

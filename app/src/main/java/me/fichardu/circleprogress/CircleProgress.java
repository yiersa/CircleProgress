package me.fichardu.circleprogress;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

public class CircleProgress extends View {

    private static final int RED = 0xFFE5282C;
    private static final int YELLOW = 0xFF1F909A;
    private static final int BLUE = 0xFFFC9E12;
    private static final int COLOR_NUM = 3;
    private int[] COLORS;
    private TimeInterpolator mInterpolator = new EaseInOutCubicInterpolator();

    private final double DEGREE = Math.PI / 180;
    private Paint mPaint;
    private int mViewSize;
    private int mPointRadius;
    private long mStartTime;
    private long mPlayTime;
    private boolean mStartAnim = false;
    private Point mCenter = new Point();

    private ArcPoint[] mArcPoint;
    private static final int POINT_NUM = 15;
    private static final int DELTA_ANGLE = 360 / POINT_NUM;
    private long mDuration = 3600;//一个周期

    public CircleProgress(Context context) {
        super(context);
        init(null, 0);
    }

    public CircleProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircleProgress(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        mArcPoint = new ArcPoint[POINT_NUM];

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircleProgress, defStyle, 0);
        int color1 = a.getColor(R.styleable.CircleProgress_color1, RED);
        int color2 = a.getColor(R.styleable.CircleProgress_color2, YELLOW);
        int color3 = a.getColor(R.styleable.CircleProgress_color3, BLUE);
        a.recycle();

        COLORS = new int[]{color1, color2, color3};
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = getResources().getDimensionPixelSize(R.dimen.default_circle_view_size);
        int width = getDefaultSize(defaultSize, widthMeasureSpec);
        int height = getDefaultSize(defaultSize, heightMeasureSpec);
        mViewSize = Math.min(width, height);
        setMeasuredDimension(mViewSize, mViewSize);
        mCenter.set(mViewSize / 2, mViewSize / 2);

        calPoints(1.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        //把当前画布的原点移到(mCenter.x, mCenter.y),
        // 后面的操作都以(mCenter.x, mCenter.y)作为参照点，
        //以中心为原点
        //小圆一遍旋转一遍做直线运动
        canvas.translate(mCenter.x, mCenter.y);

        float factor = getFactor();//可以简单理解为运动了多久
        canvas.rotate(36 * factor);//画布旋转即相当于小圆向反方向旋转
        //为什么是36，因为小圆是直线走过了一个直径的距离到了对面，对面刚好是两个圆的中间，
        //往前36度或往后36度刚好是跟他颜色一样的，而走完一个周期后所有的圆重新绘制，都回到了原来的位置，
        //这里就让圆旋转36度也就是小圆平移到了对面位置后同时旋转了36度，这样就到了跟他颜色一样的圆的位置，
        //看起来是从当前位置开始旋转的，其实是重新绘制了所有圆，只不过上一个周期结束时看起来跟开始时位置一样


        float x, y;
        for (int i = 0; i < POINT_NUM; ++i) {
            mPaint.setColor(mArcPoint[i].color);
            float itemFactor = getItemFactor(i, factor);
            x = mArcPoint[i].x - 2 * mArcPoint[i].x * itemFactor;
            y = mArcPoint[i].y - 2 * mArcPoint[i].y * itemFactor;
            canvas.drawCircle(x, y, mPointRadius, mPaint);
        }

        canvas.restore();

        if (mStartAnim) {
            postInvalidate();
        }
    }

    private void calPoints(float factor) {
        int radius = (int) (mViewSize / 3 * factor);
        mPointRadius = radius / 12;
        //计算每个小圆的坐标，坐标原点是画布中心
        for (int i = 0; i < POINT_NUM; ++i) {
            float x = radius * -(float) Math.sin(DEGREE * DELTA_ANGLE * i);
            float y = radius * -(float) Math.cos(DEGREE * DELTA_ANGLE * i);

            ArcPoint point = new ArcPoint(x, y, COLORS[i % COLOR_NUM]);
            mArcPoint[i] = point;
        }
    }

    /**
     * 当前运动时间占一个周期的百分比
     * @return
     */
    private float getFactor() {
        if (mStartAnim) {
            mPlayTime = AnimationUtils.currentAnimationTimeMillis() - mStartTime;
        }
        float factor = mPlayTime / (float) mDuration;
        return factor % 1f;
    }

    private float getItemFactor(int index, float factor) {
        //这里把一个周期当作1，0.66即2/3,这里用1/3个周期做直线运动，
        //也就是第一个马上运动，第二个等一会，第三个在等一会，……，最后一个等了2/3个周期
        //factor - 0.66f / POINT_NUM * index就是总运动时间减去当前那个小圆等待时间就是小圆运动了的时间
        //还可以把0.66改成0.8,3改成5,
        //至于为什么要1/3个周期做完，不用一个周期，可以想一下，第一个圆刚到位置，最后一个圆就开始动了，他们之间的圆肯定
        //都在运动中，这样所有的圆都在往圆心收缩，而想要的效果是一部分圆在收缩，大部分的还在原位置
        //当然可以改小点，用1/5个周期，但是没1/3效果好
        float itemFactor = (factor - 0.66f / POINT_NUM * index) * 3;
        if (itemFactor < 0f) {
            itemFactor = 0f;
        } else if (itemFactor > 1f) {
            itemFactor = 1f;
        }
        return mInterpolator.getInterpolation(itemFactor);
    }

    public void startAnim() {
        mPlayTime = mPlayTime % mDuration;
        mStartTime = AnimationUtils.currentAnimationTimeMillis() - mPlayTime;
        mStartAnim = true;
        postInvalidate();
    }

    public void reset() {
        stopAnim();
        mPlayTime = 0;
        postInvalidate();

    }

    public void stopAnim() {
        mStartAnim = false;
    }

    public void setInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public void setRadius(float factor) {
        stopAnim();
        calPoints(factor);
        startAnim();
    }

    static class ArcPoint {
        float x;
        float y;
        int color;

        ArcPoint(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

}

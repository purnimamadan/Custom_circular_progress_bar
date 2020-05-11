package com.example.circularpb;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class CustomProgressBar extends View {

    //region public variables for handling properties of custom bar
    public static final int DIRECTION_CLOCKWISE = 0;
    public static final int DIRECTION_COUNTERCLOCKWISE = 1;
    public static final int CAP_ROUND = 0;
    public static final int CAP_STRAIGHT = 1;
    //endregion

    //region paint
    private Paint progressPaint;
    private Paint progressBackgroundPaint;
    private Paint dotPaint;
    //endregion

    private static final String DEFAULT_PROGRESS_COLOR = "#28b51b";
    private static final String DEFAULT_PROGRESS_BACKGROUND_COLOR = "#e0e0e0";
    private static final int DEFAULT_STROKE_WIDTH_DP = 8;
    private static final int DEFAULT_PROGRESS_START_ANGLE = 270;
    private static final int ANGLE_START_PROGRESS_BACKGROUND = 0;
    private static final int ANGLE_END_PROGRESS_BACKGROUND = 360;
    private static final int DESIRED_WIDTH_DP = 150;
    private static final int DEFAULT_ANIMATION_DURATION = 1_000;
    private static final String PROPERTY_ANGLE = "angle";
    private int startAngle = DEFAULT_PROGRESS_START_ANGLE;
    private int sweepAngle = 0;
    private RectF circleBounds;
    private float radius;
    private boolean shouldDrawDot;
    private double maxProgressValue = 100.0;
    private double progressValue = 0.0;
    private boolean isAnimationEnabled;
    private boolean isFillBackgroundEnabled;
    private ValueAnimator progressAnimator;

    @Direction
    private int direction = DIRECTION_COUNTERCLOCKWISE;

    @Nullable
    private OnProgressChangeListener onProgressChangeListener;

    @NonNull
    private Interpolator animationInterpolator = new AccelerateDecelerateInterpolator();

    //region constructor
    public CustomProgressBar(Context context) {
        super(context);
        init(context, null);
    }

    public CustomProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }
    //endregion

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {

        int progressColor = Color.parseColor(DEFAULT_PROGRESS_COLOR);
        int progressBackgroundColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR);
        int progressStrokeWidth = convertdp2px(DEFAULT_STROKE_WIDTH_DP);
        int progressBackgroundStrokeWidth = progressStrokeWidth;

        shouldDrawDot = true;
        int dotColor = progressColor;
        int dotWidth = progressStrokeWidth;

        Paint.Cap progressStrokeCap = Paint.Cap.ROUND;

        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.CustomProgressBar);
            progressColor = attributes.getColor(R.styleable.CustomProgressBar_progressColor, progressColor);
            progressBackgroundColor = attributes.getColor(R.styleable.CustomProgressBar_progressBackgroundColor, progressBackgroundColor);
            progressStrokeWidth = attributes.getDimensionPixelSize(R.styleable.CustomProgressBar_progressStrokeWidth, progressStrokeWidth);
            progressBackgroundStrokeWidth = attributes.getDimensionPixelSize(R.styleable.CustomProgressBar_progressBackgroundStrokeWidth, progressStrokeWidth);
            shouldDrawDot = attributes.getBoolean(R.styleable.CustomProgressBar_drawDot, true);
            dotColor = attributes.getColor(R.styleable.CustomProgressBar_dotColor, progressColor);
            dotWidth = attributes.getDimensionPixelSize(R.styleable.CustomProgressBar_dotWidth, progressStrokeWidth);

            startAngle = attributes.getInt(R.styleable.CustomProgressBar_startAngle, DEFAULT_PROGRESS_START_ANGLE);
            if (startAngle < 0 || startAngle > 360) {
                startAngle = DEFAULT_PROGRESS_START_ANGLE;
            }

            isAnimationEnabled = attributes.getBoolean(R.styleable.CustomProgressBar_enableProgressAnimation, true);
            isFillBackgroundEnabled = attributes.getBoolean(R.styleable.CustomProgressBar_fillBackground, false);
            direction = attributes.getInt(R.styleable.CustomProgressBar_direction, DIRECTION_COUNTERCLOCKWISE);

            int cap = attributes.getInt(R.styleable.CustomProgressBar_progressCap, CAP_ROUND);
            progressStrokeCap = (cap == CAP_ROUND) ? Paint.Cap.ROUND : Paint.Cap.BUTT;
            attributes.recycle();
        }

        progressPaint = new Paint();
        progressPaint.setStrokeCap(progressStrokeCap);
        progressPaint.setStrokeWidth(progressStrokeWidth);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);

        progressBackgroundPaint = new Paint();
        Paint.Style progressBackgroundStyle = isFillBackgroundEnabled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE;
        progressBackgroundPaint.setStyle(progressBackgroundStyle);
        progressBackgroundPaint.setStrokeWidth(progressBackgroundStrokeWidth);
        progressBackgroundPaint.setColor(progressBackgroundColor);
        progressBackgroundPaint.setAntiAlias(true);

        dotPaint = new Paint();
        dotPaint.setStrokeCap(Paint.Cap.ROUND);
        dotPaint.setStrokeWidth(dotWidth);
        dotPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        dotPaint.setColor(dotColor);
        dotPaint.setAntiAlias(true);
        circleBounds = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        Rect textBoundsRect = new Rect();

        float dotWidth = dotPaint.getStrokeWidth();
        float progressWidth = progressPaint.getStrokeWidth();
        float progressBackgroundWidth = progressBackgroundPaint.getStrokeWidth();
        float strokeSizeOffset = (shouldDrawDot) ? Math.max(dotWidth, Math.max(progressWidth, progressBackgroundWidth)) : Math.max(progressWidth, progressBackgroundWidth);

        int desiredSize = ((int) strokeSizeOffset) + convertdp2px(DESIRED_WIDTH_DP) +
                Math.max(paddingBottom + paddingTop, paddingLeft + paddingRight);
        desiredSize += Math.max(textBoundsRect.width(), textBoundsRect.height()) + desiredSize * .1f;

        int finalWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                finalWidth = measuredWidth;
                break;
            case MeasureSpec.AT_MOST:
                finalWidth = Math.min(desiredSize, measuredWidth);
                break;
            default:
                finalWidth = desiredSize;
                break;
        }

        int finalHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                finalHeight = measuredHeight;
                break;
            case MeasureSpec.AT_MOST:
                finalHeight = Math.min(desiredSize, measuredHeight);
                break;
            default:
                finalHeight = desiredSize;
                break;
        }

        int widthWithoutPadding = finalWidth - paddingLeft - paddingRight;
        int heightWithoutPadding = finalHeight - paddingTop - paddingBottom;

        int smallestSide = Math.min(heightWithoutPadding, widthWithoutPadding);
        setMeasuredDimension(smallestSide, smallestSide);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        calculateBounds(w, h);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawProgressBackground(canvas);
        drawProgress(canvas);
        if (shouldDrawDot) drawDot(canvas);
    }

    //region draw methods
    private void drawProgressBackground(Canvas canvas) {
        canvas.drawArc(circleBounds, ANGLE_START_PROGRESS_BACKGROUND, ANGLE_END_PROGRESS_BACKGROUND,
                false, progressBackgroundPaint);
    }

    private void drawProgress(Canvas canvas) {
        canvas.drawArc(circleBounds, startAngle, sweepAngle, false, progressPaint);
    }

    private void drawDot(Canvas canvas) {
        double angleRadians = Math.toRadians(startAngle + sweepAngle + 180);
        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);
        float x = circleBounds.centerX() - radius * cos;
        float y = circleBounds.centerY() - radius * sin;

        canvas.drawPoint(x, y, dotPaint);
    }
    //endregion

    //region start and stop animation
    private void startProgressAnimation(double oldCurrentProgress, final double finalAngle) {
        final PropertyValuesHolder angleProperty = PropertyValuesHolder.ofInt(PROPERTY_ANGLE, sweepAngle, (int) finalAngle);

        progressAnimator = ValueAnimator.ofObject(new TypeEvaluator<Double>() {
            @Override
            public Double evaluate(float fraction, Double startValue, Double endValue) {
                return (startValue + (endValue - startValue) * fraction);
            }
        }, oldCurrentProgress, progressValue);
        progressAnimator.setDuration(DEFAULT_ANIMATION_DURATION);
        progressAnimator.setValues(angleProperty);
        progressAnimator.setInterpolator(animationInterpolator);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sweepAngle = (int) animation.getAnimatedValue(PROPERTY_ANGLE);
                invalidate();
            }
        });
        progressAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                sweepAngle = (int) finalAngle;
                invalidate();
                progressAnimator = null;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        progressAnimator.start();
    }

    private void stopProgressAnimation() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }
    //endregion

    private void calculateBounds(int w, int h) {
        radius = w / 2f;

        float dotWidth = dotPaint.getStrokeWidth();
        float progressWidth = progressPaint.getStrokeWidth();
        float progressBackgroundWidth = progressBackgroundPaint.getStrokeWidth();
        float strokeSizeOffset = (shouldDrawDot) ? Math.max(dotWidth, Math.max(progressWidth, progressBackgroundWidth)) : Math.max(progressWidth, progressBackgroundWidth); // to prevent progress or dot from drawing over the bounds
        float halfOffset = strokeSizeOffset / 2f;

        circleBounds.left = halfOffset;
        circleBounds.top = halfOffset;
        circleBounds.right = w - halfOffset;
        circleBounds.bottom = h - halfOffset;

        radius = circleBounds.width() / 2f;
    }

    private int convertdp2px(float dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    private void invalidateView() {
        calculateBounds(getWidth(), getHeight());
        requestLayout();
        invalidate();
    }

    //region get methods
    @ColorInt
    public int getProgressColor() {
        return progressPaint.getColor();
    }

    @ColorInt
    public int getProgressBackgroundColor() {
        return progressBackgroundPaint.getColor();
    }

    public float getProgressStrokeWidth() {
        return progressPaint.getStrokeWidth();
    }

    public float getProgressBackgroundStrokeWidth() {
        return progressBackgroundPaint.getStrokeWidth();
    }

    public boolean isDotEnabled() {
        return shouldDrawDot;
    }

    @ColorInt
    public int getDotColor() {
        return dotPaint.getColor();
    }

    public float getDotWidth() {
        return dotPaint.getStrokeWidth();
    }

    @Cap
    public int getProgressStrokeCap() {
        return (progressPaint.getStrokeCap() == Paint.Cap.ROUND) ? CAP_ROUND : CAP_STRAIGHT;
    }

    public double getProgress() {
        return progressValue;
    }

    public double getMaxProgress() {
        return maxProgressValue;
    }

    public int getStartAngle() {
        return startAngle;
    }

    @Direction
    public int getDirection() {
        return direction;
    }

    public boolean isAnimationEnabled() {
        return isAnimationEnabled;
    }

    public boolean isFillBackgroundEnabled() {
        return isFillBackgroundEnabled;
    }

    @NonNull
    public Interpolator getInterpolator() {
        return animationInterpolator;
    }

    @Nullable
    public OnProgressChangeListener getOnProgressChangeListener() {
        return onProgressChangeListener;
    }
    //endregion

    //region set methods
    public void setProgressColor(@ColorInt int color) {
        progressPaint.setColor(color);
        invalidate();
    }

    public void setProgressBackgroundColor(@ColorInt int color) {
        progressBackgroundPaint.setColor(color);
        invalidate();
    }

    public void setProgressStrokeWidthDp(@Dimension int strokeWidth) {
        setProgressStrokeWidthPx(convertdp2px(strokeWidth));
    }

    public void setProgressStrokeWidthPx(@Dimension int strokeWidth) {
        progressPaint.setStrokeWidth(strokeWidth);
        invalidateView();
    }

    public void setProgressBackgroundStrokeWidthDp(@Dimension int strokeWidth) {
        setProgressBackgroundStrokeWidthPx(convertdp2px(strokeWidth));
    }

    public void setProgressBackgroundStrokeWidthPx(@Dimension int strokeWidth) {
        progressBackgroundPaint.setStrokeWidth(strokeWidth);
        invalidateView();
    }

    public void setMaxProgress(double maxProgress) {
        maxProgressValue = maxProgress;
        if (maxProgressValue < progressValue) {
            setCurrentProgress(maxProgress);
        }
        invalidate();
    }

    public void setCurrentProgress(double currentProgress) {
        if (currentProgress > maxProgressValue) {
            maxProgressValue = currentProgress;
        }
        setProgress(currentProgress, maxProgressValue);
    }

    public void setProgress(double current, double max) {
        final double finalAngle;

        if (direction == DIRECTION_COUNTERCLOCKWISE) {
            finalAngle = -(current / max * 360);
        } else {
            finalAngle = current / max * 360;
        }

        double oldCurrentProgress = progressValue;

        maxProgressValue = max;
        progressValue = Math.min(current, max);

        if (onProgressChangeListener != null) {
            onProgressChangeListener.onProgressChanged(progressValue, maxProgressValue);
        }

        stopProgressAnimation();

        if (isAnimationEnabled) {
            startProgressAnimation(oldCurrentProgress, finalAngle);
        } else {
            sweepAngle = (int) finalAngle;
            invalidate();
        }
    }

    public void setShouldDrawDot(boolean shouldDrawDot) {
        this.shouldDrawDot = shouldDrawDot;

        if (dotPaint.getStrokeWidth() > progressPaint.getStrokeWidth()) {
            requestLayout();
            return;
        }

        invalidate();
    }

    public void setDotColor(@ColorInt int color) {
        dotPaint.setColor(color);
        invalidate();
    }

    public void setDotWidthDp(@Dimension int width) {
        setDotWidthPx(convertdp2px(width));
    }

    public void setDotWidthPx(@Dimension int width) {
        dotPaint.setStrokeWidth(width);
        invalidateView();
    }

    public void setStartAngle(@IntRange(from = 0, to = 360) int startAngle) {
        this.startAngle = startAngle;
        invalidate();
    }

    public void setDirection(@Direction int direction) {
        this.direction = direction;
        invalidate();
    }

    public void setProgressStrokeCap(@Cap int cap) {
        Paint.Cap paintCap = (cap == CAP_ROUND) ? Paint.Cap.ROUND : Paint.Cap.BUTT;
        if (progressPaint.getStrokeCap() != paintCap) {
            progressPaint.setStrokeCap(paintCap);
            invalidate();
        }
    }

    public void setAnimationEnabled(boolean enableAnimation) {
        isAnimationEnabled = enableAnimation;
        if (!enableAnimation) stopProgressAnimation();
    }

    public void setFillBackgroundEnabled(boolean fillBackgroundEnabled) {
        if (fillBackgroundEnabled == isFillBackgroundEnabled) return;

        isFillBackgroundEnabled = fillBackgroundEnabled;

        Paint.Style style = fillBackgroundEnabled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE;
        progressBackgroundPaint.setStyle(style);

        invalidate();
    }

    public void setInterpolator(@NonNull Interpolator interpolator) {
        animationInterpolator = interpolator;
    }

    public void setOnProgressChangeListener(@Nullable OnProgressChangeListener onProgressChangeListener) {
        this.onProgressChangeListener = onProgressChangeListener;
    }
    //endregion

    //region custom notations
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIRECTION_CLOCKWISE, DIRECTION_COUNTERCLOCKWISE})
    public @interface Direction {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CAP_ROUND, CAP_STRAIGHT})
    public @interface Cap {
    }
    //endregion

    public interface OnProgressChangeListener {
        void onProgressChanged(double progress, double maxProgress);
    }
}

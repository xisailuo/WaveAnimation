package com.jikai.waveanimation;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

/**
 * Created by jikai on 2017/6/8. 音量动画
 */

public class VolumeWaveDrawable extends Drawable implements Animatable {

    private static final int opacity[] = { 255, 204 };

    private ValueAnimator mRenderAnimator;
    private final Paint mWavePaint = new Paint();
    private RectF rectF = new RectF();
    private Matrix scaleMatrix = new Matrix();
    // 缓存的衰减系数
    private SparseArray<Double> recessionAttenuation = new SparseArray<>();

    private int frequency = 3;
    private double phase = 0;

    private int verticalSpeed = 1;
    private int verticalRestoreSpeed = 1;
    private int stroke1;
    private int stroke2;
    private int color = -1;
    private int colorFrom = -1;
    private int colorTo = -1;
    private int colors[] = null;

    private int mMaxHeight;
    private int mHalfHeight;
    private int mHalfWidth;
    private int mQuarterWidth;

    private double minAmplitude = 0.1;
    private double lastAmplitude;
    private double nextTargetAmplitude;

    private final Rect mBounds = new Rect();
    private final Path mWavePath = new Path();

    private final ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            phase = (float) animation.getAnimatedValue();
            invalidateSelf();
        }
    };

    public VolumeWaveDrawable() {
        mWavePaint.setStyle(Paint.Style.STROKE);
        mWavePaint.setAntiAlias(true);
        setupAnimators();
    }

    private void setupAnimators() {
        mRenderAnimator = ValueAnimator.ofFloat(0f, (float) Math.PI * 2);
        mRenderAnimator.setRepeatCount(Animation.INFINITE);
        mRenderAnimator.setInterpolator(new LinearInterpolator());
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        mWavePath.rewind();
        mWavePath.moveTo(0, mHalfHeight);
        mWavePaint.setStrokeWidth(stroke1);
        mWavePaint.setAlpha(opacity[0]);
        for (int i = -40; i <= 40; i ++) {
            double temp = i / 20d;
            float x = (float) getXPosition(temp);
            float y;
            if (Math.abs(i) >= 39) {
                y = mHalfHeight;
            } else {
                y = (float) getYPosition(phase, temp);
            }
            mWavePath.lineTo(x, y);
        }
        canvas.drawPath(mWavePath, mWavePaint);

        mWavePaint.setStrokeWidth(stroke2);
        mWavePaint.setAlpha(opacity[1]);
        mWavePath.computeBounds(rectF, true);
        scaleMatrix.setScale(1.3f, -0.8f, mHalfWidth, mHalfHeight);
        mWavePath.transform(scaleMatrix);
        canvas.drawPath(mWavePath, mWavePaint);
    }

    private double getYPosition(double phase, double x) {
        double att = (mMaxHeight * getCurrentAmplitude()); // 不同的线，不同的振幅
        return mHalfHeight + getAttenuationEquation(x) * att
                * Math.sin(frequency * x - phase);
    }

    private double getAttenuationEquation(double value) { // 80 个值
        int keyX = (int) (value * 20);
        double result;
        if (recessionAttenuation.indexOfKey(keyX) >= 0) {
            result = recessionAttenuation.get(keyX);
        } else {
            result = Math.pow(4 / (4 + Math.pow(value, 4)), 4);
            recessionAttenuation.put(keyX, result);
        }
        return result;
    }

    /**
     * @param value
     *            0~1
     */
    public void setAmplitude(double value) {
        nextTargetAmplitude = Math.max(Math.min(value, 1), 0.1);
    }

    private double getCurrentAmplitude() {
        if (lastAmplitude == nextTargetAmplitude) {
            return nextTargetAmplitude; // 不变
        } else if (nextTargetAmplitude > lastAmplitude) {
            // 变大，改变速度快
            double target = lastAmplitude + (verticalSpeed / 2000d);
            lastAmplitude = target;
            if (target >= nextTargetAmplitude) {
                target = nextTargetAmplitude;
                lastAmplitude = nextTargetAmplitude;
                nextTargetAmplitude = minAmplitude;
            }
            return target;
        } else { //  if (nextTargetAmplitude < lastAmplitude)
            // 变小(恢复)，改变速度慢
            double target = lastAmplitude - (verticalRestoreSpeed / 3000d);
            lastAmplitude = target;
            if (target <= nextTargetAmplitude) {
                target = nextTargetAmplitude;
                lastAmplitude = nextTargetAmplitude;
                nextTargetAmplitude = minAmplitude;
            }
            return target;
        }
    }

    private double getXPosition(double value) {
        return mHalfWidth + value * mQuarterWidth;
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mWavePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void start() {
        if (!isRunning()) {
            mRenderAnimator.addUpdateListener(mAnimatorUpdateListener);
            mRenderAnimator.start();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            mRenderAnimator.removeUpdateListener(mAnimatorUpdateListener);
            mRenderAnimator.end();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mBounds.set(bounds);
        int mWidth = bounds.width();
        int mHeight = bounds.height();
        mHalfHeight = mHeight / 2;
        mMaxHeight = mHalfHeight - 4;
        mHalfWidth = mWidth / 2;
        mQuarterWidth = mWidth / 4;
        mWidth = bounds.width();
        if (color != -1) {
            mWavePaint.setColor(color);
        } else if (colorTo != -1 && colorFrom != -1) {
            Shader shader = new LinearGradient(0, mHalfHeight, mWidth,
                    mHalfHeight, colorFrom, colorTo, Shader.TileMode.CLAMP);
            mWavePaint.setShader(shader);
        } else if (colors != null) {
            Shader shader = new LinearGradient(0, mHalfHeight, mWidth,
                    mHalfHeight, colors, null, Shader.TileMode.CLAMP);
            mWavePaint.setShader(shader);
        } else {
            mWavePaint.setColor(0x000000);
        }
        start();
    }

    @Override
    public boolean isRunning() {
        return mRenderAnimator.isRunning();
    }

    public static class Builder {

        private double speed = 1;
        private int frequency = 3;

        private int stroke1 = 3;
        private int stroke2 = 2;
        private int color = -1;
        private int colorFrom = -1;
        private int colorTo = -1;
        private int[] colors = null;
        private double minAmplitude;

        public Builder() {
        }

        public Builder speed(int speed) {
            this.speed = speed;
            return this;
        }

        public Builder frequency(int frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder stroke1(int stroke) {
            this.stroke1 = stroke;
            return this;
        }

        public Builder stroke2(int stroke) {
            this.stroke2 = stroke;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder colorFromTo(int colorFrom, int colorTo) {
            this.colorFrom = colorFrom;
            this.colorTo = colorTo;
            return this;
        }

        public Builder colors(int[] colors) {
            this.colors = colors;
            return this;
        }

        public Builder minAmplitude(double amplitude) {
            this.minAmplitude = amplitude;
            return this;
        }

        public VolumeWaveDrawable Build(Context context) {
            VolumeWaveDrawable drawable = new VolumeWaveDrawable();
            drawable.mRenderAnimator.setDuration((long) (1000 / speed));
            float scale = context.getResources().getDisplayMetrics().density;
            drawable.stroke1 = (int) (this.stroke1 * scale + 0.5f);
            drawable.stroke2 = (int) (this.stroke2 * scale + 0.5f);
            drawable.color = this.color;
            drawable.colorFrom = this.colorFrom;
            drawable.colorTo = this.colorTo;
            drawable.colors = this.colors;
            drawable.frequency = this.frequency;
            drawable.minAmplitude = this.minAmplitude;
            return drawable;
        }
    }

}
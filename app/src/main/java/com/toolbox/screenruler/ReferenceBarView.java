package com.toolbox.screenruler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public class ReferenceBarView extends View {

    public interface Listener {
        void onScaleChanged(float pxPerMm, boolean clamped);
    }

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private float pxPerMm = 16f;
    private float targetMm = 53.98f;
    private boolean dragging;
    private Listener listener;

    public ReferenceBarView(Context context) {
        super(context);
        init();
    }

    public ReferenceBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReferenceBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(0xFFDDE6F4);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.BUTT);
        tickPaint.setColor(0xFF9AA9BF);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF8C99AD);

        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(getContext().getColor(R.color.accent));

        handleDotPaint.setStyle(Paint.Style.FILL);
        handleDotPaint.setColor(getContext().getColor(R.color.accent_dark));

        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(Math.max(dp(2f), 2f));
        guidePaint.setColor(getContext().getColor(R.color.accent));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        notifyChanged();
    }

    public void setPxPerMm(float value) {
        pxPerMm = clamp(value, 2f, 100f);
        invalidate();
        notifyChanged();
    }

    public float getPxPerMm() {
        return pxPerMm;
    }

    public void setTargetMm(float value) {
        if (value <= 0f) {
            return;
        }
        targetMm = value;
        invalidate();
        notifyChanged();
    }

    public float getTargetMm() {
        return targetMm;
    }

    public void adjustBarPx(float deltaPx) {
        setPxPerMm((targetMm * pxPerMm + deltaPx) / targetMm);
    }

    public boolean isClamped() {
        return targetMm * pxPerMm > barLimit();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private float barLimit() {
        return Math.max(dp(30f), getWidth() - dp(28f));
    }

    private float barStart() {
        return dp(2f);
    }

    private float barEnd() {
        float start = barStart();
        return start + Math.min(targetMm * pxPerMm, barLimit() - start);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        notifyChanged();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0) {
            return;
        }

        float start = barStart();
        float end = barEnd();
        float barTop = dp(22f);
        float barHeight = dp(9f);
        float barBottom = barTop + barHeight;

        barRect.set(start, barTop, end, barBottom);
        canvas.drawRoundRect(barRect, dp(4.5f), dp(4.5f), barPaint);

        tickPaint.setStrokeWidth(Math.max(dp(0.8f), 1f));
        textPaint.setTextSize(sp(9f));
        int maxMm = (int) Math.floor(targetMm + 0.001f);
        for (int mm = 0; mm <= maxMm; mm++) {
            float x = start + mm * pxPerMm;
            if (x > end + 0.5f) {
                break;
            }
            float tickLength;
            if (mm % 10 == 0) {
                tickLength = dp(12f);
                tickPaint.setStrokeWidth(Math.max(dp(1.2f), 1f));
            } else if (mm % 5 == 0) {
                tickLength = dp(8f);
                tickPaint.setStrokeWidth(Math.max(dp(0.9f), 1f));
            } else {
                tickLength = dp(4f);
                tickPaint.setStrokeWidth(Math.max(dp(0.7f), 1f));
            }
            canvas.drawLine(x, barBottom, x, barBottom + tickLength, tickPaint);
            if (mm % 10 == 0) {
                if (mm == 0) {
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("0", start,
                            barBottom + dp(24f) + sp(9f) * 0.35f, textPaint);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                } else {
                    canvas.drawText(String.valueOf(mm / 10), x,
                            barBottom + dp(24f) + sp(9f) * 0.35f, textPaint);
                }
            }
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(0xFF9AA9BF);
        canvas.drawText(String.format(Locale.US, "%.2f mm", targetMm),
                start + dp(8f), barTop - dp(7f), textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF8C99AD);

        float handleX = end;
        float handleY = barTop + barHeight / 2f;
        canvas.drawLine(start, dp(6f), start, getHeight() - dp(6f), guidePaint);
        canvas.drawLine(end, dp(6f), end, getHeight() - dp(6f), guidePaint);
        canvas.drawCircle(handleX, handleY, dp(15f), handlePaint);
        canvas.drawCircle(handleX, handleY, dp(5f), handleDotPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (event.getX() < barStart()) {
                    return false;
                }
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                updateFromX(event.getX());
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (dragging) {
                    updateFromX(event.getX());
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (dragging) {
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        performClick();
                    }
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateFromX(float x) {
        float width = Math.max(dp(8f), Math.min(x - barStart(), barLimit() - barStart()));
        setPxPerMm(width / targetMm);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onScaleChanged(pxPerMm, isClamped());
        }
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}

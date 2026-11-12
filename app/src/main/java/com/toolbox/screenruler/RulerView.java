package com.toolbox.screenruler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RulerView extends View {

    public static final int ORIENT_HORIZONTAL = 0;
    public static final int ORIENT_VERTICAL = 1;

    public interface Listener {
        void onMeasurementChanged(float distanceMm, int selectedMarker);
    }

    private static final int MARKER_A = 0;
    private static final int MARKER_B = 1;

    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint washPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int colorMajor;
    private int colorMid;
    private int colorMinor;
    private int colorLabel;
    private int colorMuted;
    private int colorAccent;
    private int colorMarkerB;
    private int colorDark;

    private float pxPerMmX = 16f;
    private float pxPerMmY = 16f;
    private float posAMm = 12f;
    private float posBMm = 42f;
    private int orientation = ORIENT_HORIZONTAL;
    private int selectedMarker = MARKER_A;
    private boolean positionsInitialized;
    private boolean dragging;
    private boolean moved;
    private float downX;
    private float downY;
    private Listener listener;

    public RulerView(Context context) {
        super(context);
        setup(context);
    }

    public RulerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    private void setup(Context context) {
        colorMajor = context.getColor(R.color.text_primary);
        colorMid = colorA(context, R.color.text_secondary, 0xCC);
        colorMinor = colorA(context, R.color.text_muted, 0xDD);
        colorLabel = context.getColor(R.color.text_secondary);
        colorMuted = context.getColor(R.color.text_muted);
        colorAccent = context.getColor(R.color.accent);
        colorMarkerB = context.getColor(R.color.marker_b);
        colorDark = context.getColor(R.color.accent_dark);
        init();
    }

    private static int colorA(Context context, int colorRes, int alpha) {
        int color = context.getColor(colorRes);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.BUTT);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));

        bandPaint.setStyle(Paint.Style.FILL);
        bandPaint.setColor(0xFF141821);

        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(dp(1f));
        edgePaint.setColor(0xFF2A303B);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(0.7f));
        gridPaint.setColor(0x14FFFFFF);

        washPaint.setStyle(Paint.Style.FILL);
        washPaint.setColor(colorAccent);
        washPaint.setAlpha(16);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setPathEffect(new DashPathEffect(new float[]{dp(7f), dp(6f)}, 0f));

        handlePaint.setStyle(Paint.Style.FILL);
        handleRingPaint.setStyle(Paint.Style.STROKE);
        handleTextPaint.setStyle(Paint.Style.FILL);
        handleTextPaint.setTextAlign(Paint.Align.CENTER);
        handleTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        notifyChanged();
    }

    public void setCalibration(float pxPerMmX, float pxPerMmY) {
        if (!(pxPerMmX >= 2f && pxPerMmX <= 120f)
                || !(pxPerMmY >= 2f && pxPerMmY <= 120f)) {
            return;
        }
        this.pxPerMmX = pxPerMmX;
        this.pxPerMmY = pxPerMmY;
        clampPositions();
        invalidate();
        notifyChanged();
    }

    public void setOrientation(int orientation) {
        if (orientation != ORIENT_HORIZONTAL && orientation != ORIENT_VERTICAL) {
            return;
        }
        this.orientation = orientation;
        clampPositions();
        invalidate();
        notifyChanged();
    }

    public int getOrientation() {
        return orientation;
    }

    public int getSelectedMarker() {
        return selectedMarker;
    }

    public void setSelectedMarker(int marker) {
        if (marker != MARKER_A && marker != MARKER_B) {
            return;
        }
        selectedMarker = marker;
        invalidate();
        notifyChanged();
    }

    public void resetPositions() {
        float max = maxMm();
        posAMm = max * 0.18f;
        posBMm = max * 0.62f;
        if (posBMm - posAMm < 5f && max > 10f) {
            posBMm = Math.min(max, posAMm + 5f);
        }
        positionsInitialized = true;
        invalidate();
        notifyChanged();
    }

    public void nudgeSelected(float deltaMm) {
        float value = (selectedMarker == MARKER_A ? posAMm : posBMm) + deltaMm;
        setSelectedMm(value);
    }

    public float getDistanceMm() {
        return Math.abs(posBMm - posAMm);
    }

    public float getSelectedPositionMm() {
        return selectedMarker == MARKER_A ? posAMm : posBMm;
    }

    private float dps() {
        return getResources().getDisplayMetrics().scaledDensity;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float pxPerMm() {
        return orientation == ORIENT_HORIZONTAL ? pxPerMmX : pxPerMmY;
    }

    private float axisLength() {
        return orientation == ORIENT_HORIZONTAL ? getWidth() : getHeight();
    }

    private float maxMm() {
        float length = axisLength();
        float ppm = pxPerMm();
        if (length <= 0f || ppm <= 0f) {
            return 200f;
        }
        return length / ppm;
    }

    private void clampPositions() {
        float max = maxMm();
        posAMm = clamp(posAMm, 0f, max);
        posBMm = clamp(posBMm, 0f, max);
    }

    private void setSelectedMm(float value) {
        float mm = clamp(value, 0f, maxMm());
        if (selectedMarker == MARKER_A) {
            posAMm = mm;
        } else {
            posBMm = mm;
        }
        positionsInitialized = true;
        invalidate();
        notifyChanged();
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onMeasurementChanged(getDistanceMm(), selectedMarker);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!positionsInitialized) {
            resetPositions();
        } else {
            clampPositions();
            notifyChanged();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean horizontal = orientation == ORIENT_HORIZONTAL;
        float density = getResources().getDisplayMetrics().density;
        float sp = dps();
        float length = horizontal ? width : height;
        float band = (horizontal ? 60f : 54f) * density;
        float ppm = pxPerMm();

        clampPositions();
        float aPx = posAMm * ppm;
        float bPx = posBMm * ppm;
        float left = Math.min(aPx, bPx);
        float right = Math.max(aPx, bPx);

        if (horizontal) {
            canvas.drawRect(0f, 0f, width, band, bandPaint);
            canvas.drawLine(0f, band, width, band, edgePaint);
        } else {
            canvas.drawRect(0f, 0f, band, height, bandPaint);
            canvas.drawLine(band, 0f, band, height, edgePaint);
        }

        if (horizontal) {
            rect.set(left, band, right, height);
        } else {
            rect.set(band, left, width, right);
        }
        canvas.drawRect(rect, washPaint);

        drawGrid(canvas, horizontal, length, band, ppm);
        drawTicks(canvas, horizontal, length, band, ppm, density, sp);
        drawMarker(canvas, horizontal, band, aPx, MARKER_A, "A", density, sp);
        drawMarker(canvas, horizontal, band, bPx, MARKER_B, "B", density, sp);
    }

    private void drawGrid(Canvas canvas, boolean horizontal, float length, float band, float ppm) {
        float step = 10f * ppm;
        if (step < dp(9f)) {
            return;
        }
        for (float p = step; p < length; p += step) {
            if (horizontal) {
                canvas.drawLine(p, band, p, getHeight(), gridPaint);
            } else {
                canvas.drawLine(band, p, getWidth(), p, gridPaint);
            }
        }
    }

    private void drawTicks(Canvas canvas, boolean horizontal, float length, float band,
                           float ppm, float density, float sp) {
        int maxTick = (int) Math.floor((length - 0.2f) / ppm);
        for (int mm = 0; mm <= maxTick; mm++) {
            float p = mm * ppm;
            float tickLength;
            int color;
            float strokeWidth;
            if (mm % 10 == 0) {
                tickLength = 25f * density;
                color = colorMajor;
                strokeWidth = Math.max(density * 1.5f, 1f);
            } else if (mm % 5 == 0) {
                tickLength = 16f * density;
                color = colorMid;
                strokeWidth = Math.max(density, 1f);
            } else {
                tickLength = 8f * density;
                color = colorMinor;
                strokeWidth = Math.max(density * 0.75f, 1f);
            }
            tickPaint.setColor(color);
            tickPaint.setStrokeWidth(strokeWidth);
            if (horizontal) {
                canvas.drawLine(p, 0f, p, tickLength, tickPaint);
            } else {
                canvas.drawLine(0f, p, tickLength, p, tickPaint);
            }
        }

        textPaint.setColor(colorLabel);
        textPaint.setTextSize(10f * sp);
        textPaint.setTextAlign(horizontal ? Paint.Align.CENTER : Paint.Align.RIGHT);
        for (int mm = 0; mm <= maxTick; mm += 10) {
            String label = String.valueOf(mm / 10);
            float textWidth = textPaint.measureText(label);
            float p = mm * ppm;
            if (horizontal) {
                float x = Math.max(textWidth / 2f + dp(2f),
                        Math.min(p, length - textWidth / 2f - dp(2f)));
                canvas.drawText(label, x, 25f * density + 13f * density, textPaint);
            } else {
                float y = Math.max(p + 4f * density, 10f * sp + 2f * density);
                y = Math.min(y, length - 2f * density);
                canvas.drawText(label, band - 7f * density, y, textPaint);
            }
        }

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(9f * sp);
        textPaint.setColor(colorMuted);
        if (horizontal) {
            canvas.drawText(getContext().getString(R.string.unit_cm),
                    length - dp(6f), band - dp(6f), textPaint);
        } else {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(getContext().getString(R.string.unit_cm),
                    getWidth() - dp(8f), length - dp(6f), textPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawMarker(Canvas canvas, boolean horizontal, float band, float position,
                            int marker, String label, float density, float sp) {
        int color = marker == MARKER_A ? colorAccent : colorMarkerB;
        boolean selected = selectedMarker == marker;

        linePaint.setColor(color);
        linePaint.setStrokeWidth((selected ? 2f : 1.4f) * density);
        if (horizontal) {
            canvas.drawLine(position, band, position, getHeight(), linePaint);
        } else {
            canvas.drawLine(band, position, getWidth(), position, linePaint);
        }

        float radius = 13f * density;
        float handleX = horizontal ? position : band + 18f * density;
        float handleY = horizontal ? band + 18f * density : position;

        if (selected) {
            handleRingPaint.setColor((color & 0x00FFFFFF) | (0x66 << 24));
            handleRingPaint.setStrokeWidth(2f * density);
            canvas.drawCircle(handleX, handleY, radius + 5f * density, handleRingPaint);
        }

        handlePaint.setColor(color);
        canvas.drawCircle(handleX, handleY, radius, handlePaint);

        handleTextPaint.setColor(colorDark);
        handleTextPaint.setTextSize(10f * sp);
        Paint.FontMetrics fm = handleTextPaint.getFontMetrics();
        float baseline = handleY - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, handleX, baseline, handleTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean horizontal = orientation == ORIENT_HORIZONTAL;
        float density = getResources().getDisplayMetrics().density;
        float coord = horizontal ? event.getX() : event.getY();
        float ppm = pxPerMm();
        float band = (horizontal ? 60f : 54f) * density;
        float aPx = posAMm * ppm;
        float bPx = posBMm * ppm;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float da = Math.abs(coord - aPx);
                float db = Math.abs(coord - bPx);
                float hit = 24f * density;
                boolean inHandleZone = horizontal
                        ? event.getY() <= band + 40f * density
                        : event.getX() <= band + 40f * density;
                boolean nearLine = da <= hit || db <= hit;
                boolean inBand = horizontal ? event.getY() <= band : event.getX() <= band;

                if (inHandleZone && nearLine) {
                    selectedMarker = da <= db ? MARKER_A : MARKER_B;
                    dragging = true;
                    moved = false;
                    downX = event.getX();
                    downY = event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    invalidate();
                    notifyChanged();
                    return true;
                }
                if (inBand) {
                    dragging = true;
                    moved = false;
                    downX = event.getX();
                    downY = event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setSelectedMm(coord / ppm);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (dragging) {
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (dx * dx + dy * dy > (3f * density) * (3f * density)) {
                        moved = true;
                    }
                    setSelectedMm(coord / ppm);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (dragging) {
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (!moved) {
                        performClick();
                    }
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (dragging) {
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("super", super.onSaveInstanceState());
        bundle.putFloat("a", posAMm);
        bundle.putFloat("b", posBMm);
        bundle.putInt("selected", selectedMarker);
        bundle.putBoolean("initialized", positionsInitialized);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("super"));
            posAMm = bundle.getFloat("a", posAMm);
            posBMm = bundle.getFloat("b", posBMm);
            selectedMarker = bundle.getInt("selected", selectedMarker);
            positionsInitialized = bundle.getBoolean("initialized", positionsInitialized);
            clampPositions();
            invalidate();
            notifyChanged();
            return;
        }
        super.onRestoreInstanceState(state);
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

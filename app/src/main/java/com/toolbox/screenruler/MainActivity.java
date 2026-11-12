package com.toolbox.screenruler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity implements RulerView.Listener {

    private RulerView ruler;
    private TextView distanceValue;
    private TextView readingSecondary;
    private TextView statusText;
    private TextView statusScale;
    private View statusDot;
    private TextView markerA;
    private TextView markerB;
    private TextView modeHorizontal;
    private TextView modeVertical;
    private View calibrationChip;

    private boolean vertical;
    private CalibrationPrefs.Values values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ruler = findViewById(R.id.ruler);
        distanceValue = findViewById(R.id.distance_value);
        readingSecondary = findViewById(R.id.reading_secondary);
        statusText = findViewById(R.id.status_text);
        statusScale = findViewById(R.id.status_scale);
        statusDot = findViewById(R.id.status_dot);
        markerA = findViewById(R.id.marker_a);
        markerB = findViewById(R.id.marker_b);
        modeHorizontal = findViewById(R.id.mode_horizontal);
        modeVertical = findViewById(R.id.mode_vertical);
        calibrationChip = findViewById(R.id.status_chip);

        vertical = CalibrationPrefs.getOrientation(this) == RulerView.ORIENT_VERTICAL;
        ruler.setOrientation(vertical ? RulerView.ORIENT_VERTICAL : RulerView.ORIENT_HORIZONTAL);
        ruler.setListener(this);

        modeHorizontal.setOnClickListener(v -> setVertical(false));
        modeVertical.setOnClickListener(v -> setVertical(true));
        calibrationChip.setOnClickListener(v ->
                startActivity(new Intent(this, CalibrationActivity.class)));
        markerA.setOnClickListener(v -> ruler.setSelectedMarker(0));
        markerB.setOnClickListener(v -> ruler.setSelectedMarker(1));

        findViewById(R.id.reset).setOnClickListener(v -> ruler.resetPositions());
        findViewById(R.id.nudge_minus_1).setOnClickListener(v -> ruler.nudgeSelected(-1f));
        findViewById(R.id.nudge_minus_01).setOnClickListener(v -> ruler.nudgeSelected(-0.1f));
        findViewById(R.id.nudge_plus_01).setOnClickListener(v -> ruler.nudgeSelected(0.1f));
        findViewById(R.id.nudge_plus_1).setOnClickListener(v -> ruler.nudgeSelected(1f));

        updateModeUi();
        applyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyCalibration();
        updateModeUi();
        onMeasurementChanged(ruler.getDistanceMm(), ruler.getSelectedMarker());
    }

    private void applyCalibration() {
        values = CalibrationPrefs.resolve(this);
        ruler.setCalibration(values.pxPerMmX, values.pxPerMmY);

        if (values.mode == CalibrationPrefs.MODE_REFERENCE) {
            String label = values.label;
            if (label == null || label.isEmpty()) {
                label = getString(R.string.calibration_reference_title);
            }
            statusText.setText(getString(R.string.status_reference_fmt, label));
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else if (values.mode == CalibrationPrefs.MODE_SCREEN) {
            statusText.setText(R.string.status_screen);
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else if (values.systemAcknowledged) {
            statusText.setText(R.string.status_system);
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else {
            statusText.setText(R.string.status_uncalibrated);
            statusDot.setBackgroundResource(R.drawable.bg_dot_warning);
        }
        updateStatusScale();
    }

    private void updateStatusScale() {
        if (values == null) {
            return;
        }
        float pxPerMm = vertical ? values.pxPerMmY : values.pxPerMmX;
        statusScale.setText(getString(R.string.scale_fmt,
                String.format(Locale.US, "%.2f", pxPerMm)));
    }

    private void setVertical(boolean isVertical) {
        if (vertical == isVertical) {
            return;
        }
        vertical = isVertical;
        CalibrationPrefs.setOrientation(this,
                vertical ? RulerView.ORIENT_VERTICAL : RulerView.ORIENT_HORIZONTAL);
        ruler.setOrientation(vertical ? RulerView.ORIENT_VERTICAL : RulerView.ORIENT_HORIZONTAL);
        updateModeUi();
        updateStatusScale();
    }

    private void updateModeUi() {
        modeHorizontal.setBackgroundResource(vertical
                ? android.R.color.transparent
                : R.drawable.bg_segment_selected);
        modeVertical.setBackgroundResource(vertical
                ? R.drawable.bg_segment_selected
                : android.R.color.transparent);
        modeHorizontal.setTextColor(getColor(vertical ? R.color.text_muted : R.color.text_primary));
        modeVertical.setTextColor(getColor(vertical ? R.color.text_primary : R.color.text_muted));
    }

    @Override
    public void onMeasurementChanged(float distanceMm, int selectedMarker) {
        distanceValue.setText(String.format(Locale.US, "%.1f", distanceMm));
        readingSecondary.setText(getString(R.string.reading_full_fmt,
                String.format(Locale.US, "%.2f", distanceMm / 25.4f)));

        boolean aSelected = selectedMarker == 0;
        markerA.setBackgroundResource(aSelected
                ? R.drawable.bg_pill_selected
                : R.drawable.bg_pill);
        markerB.setBackgroundResource(aSelected
                ? R.drawable.bg_pill
                : R.drawable.bg_pill_selected);
        markerA.setTextColor(getColor(aSelected ? R.color.accent_dark : R.color.text_secondary));
        markerB.setTextColor(getColor(aSelected ? R.color.text_secondary : R.color.accent_dark));
    }

    private void applyInsets() {
        View content = findViewById(R.id.content);
        View header = findViewById(R.id.header);
        View controls = findViewById(R.id.controls);
        int headerTop = header.getPaddingTop();
        int controlsBottom = controls.getPaddingBottom();

        content.setOnApplyWindowInsetsListener((v, insets) -> {
            EdgeToEdge.Bars bars = EdgeToEdge.read(insets);
            v.setPadding(bars.left, 0, bars.right, 0);
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            controls.setPadding(controls.getPaddingLeft(), controls.getPaddingTop(),
                    controls.getPaddingRight(), controlsBottom + bars.bottom);
            return insets;
        });
        content.requestApplyInsets();
    }
}

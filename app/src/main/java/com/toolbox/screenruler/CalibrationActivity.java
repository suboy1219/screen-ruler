package com.toolbox.screenruler;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CalibrationActivity extends Activity {

    private int mode = CalibrationPrefs.MODE_SYSTEM;

    private View cardSystem;
    private View cardReference;
    private View cardScreen;
    private RadioButton radioSystem;
    private RadioButton radioReference;
    private RadioButton radioScreen;
    private View referencePanel;
    private View screenPanel;
    private ReferenceBarView refBar;
    private TextView referenceHint;
    private TextView referenceScale;
    private TextView referenceWarning;
    private TextView systemDetail;
    private TextView screenEstimate;
    private TextView presetCard;
    private TextView presetCoin;
    private EditText widthInput;
    private EditText heightInput;

    private String referenceName = "银行卡短边";
    private float referenceTargetMm = 53.98f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calibration);

        bindViews();
        applyInsets();
        fillSystemInfo();
        fillScreenInfo();
        setupReferenceBar();
        setupListeners();

        CalibrationPrefs.Values values = CalibrationPrefs.resolve(this);
        selectMode(values.mode);
    }

    private void bindViews() {
        cardSystem = findViewById(R.id.card_system);
        cardReference = findViewById(R.id.card_reference);
        cardScreen = findViewById(R.id.card_screen);
        radioSystem = findViewById(R.id.radio_system);
        radioReference = findViewById(R.id.radio_reference);
        radioScreen = findViewById(R.id.radio_screen);
        referencePanel = findViewById(R.id.reference_panel);
        screenPanel = findViewById(R.id.screen_panel);
        refBar = findViewById(R.id.ref_bar);
        referenceHint = findViewById(R.id.reference_hint);
        referenceScale = findViewById(R.id.reference_scale);
        referenceWarning = findViewById(R.id.reference_warning);
        systemDetail = findViewById(R.id.system_detail);
        screenEstimate = findViewById(R.id.screen_estimate);
        presetCard = findViewById(R.id.preset_card);
        presetCoin = findViewById(R.id.preset_coin);
        widthInput = findViewById(R.id.width_mm);
        heightInput = findViewById(R.id.height_mm);
    }

    private void fillSystemInfo() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        systemDetail.setText(getString(R.string.system_detail_fmt,
                String.format(Locale.US, "%.1f", dm.xdpi),
                String.format(Locale.US, "%.1f", dm.ydpi),
                String.format(Locale.US, "%.2f", CalibrationPrefs.systemPxPerMmX(this))));
    }

    private void fillScreenInfo() {
        float[] stored = CalibrationPrefs.storedScreenMm(this);
        float[] estimate = CalibrationPrefs.estimateScreenMm(this);
        float width = stored[0] > 0f ? stored[0] : estimate[0];
        float height = stored[1] > 0f ? stored[1] : estimate[1];
        widthInput.setText(String.format(Locale.US, "%.1f", width));
        heightInput.setText(String.format(Locale.US, "%.1f", height));
        screenEstimate.setText(getString(R.string.screen_estimate_hint,
                String.format(Locale.US, "%.1f", estimate[0]),
                String.format(Locale.US, "%.1f", estimate[1])));
    }

    private void setupReferenceBar() {
        CalibrationPrefs.Values values = CalibrationPrefs.resolve(this);
        refBar.setTargetMm(referenceTargetMm);
        refBar.setPxPerMm(values.pxPerMmX);
        refBar.setListener((pxPerMm, clamped) -> updateReferenceLabels(pxPerMm, clamped));
        updateReferenceLabels(refBar.getPxPerMm(), refBar.isClamped());
    }

    private void setupListeners() {
        findViewById(R.id.close).setOnClickListener(v -> finish());
        cardSystem.setOnClickListener(v -> selectMode(CalibrationPrefs.MODE_SYSTEM));
        cardReference.setOnClickListener(v -> selectMode(CalibrationPrefs.MODE_REFERENCE));
        cardScreen.setOnClickListener(v -> selectMode(CalibrationPrefs.MODE_SCREEN));
        radioSystem.setOnClickListener(v -> selectMode(CalibrationPrefs.MODE_SYSTEM));
        radioReference.setOnClickListener(v -> selectMode(CalibrationPrefs.MODE_REFERENCE));
        radioScreen.setOnClickListener(v -> selectMode(CalibrationPrefs.MODE_SCREEN));

        presetCard.setOnClickListener(v ->
                setReferenceTarget(53.98f, "银行卡短边", presetCard, presetCoin));
        presetCoin.setOnClickListener(v ->
                setReferenceTarget(25.00f, "1 元硬币", presetCoin, presetCard));

        findViewById(R.id.minus_px).setOnClickListener(v -> {
            refBar.adjustBarPx(-1f);
            updateReferenceLabels(refBar.getPxPerMm(), refBar.isClamped());
        });
        findViewById(R.id.plus_px).setOnClickListener(v -> {
            refBar.adjustBarPx(1f);
            updateReferenceLabels(refBar.getPxPerMm(), refBar.isClamped());
        });

        findViewById(R.id.save).setOnClickListener(v -> save());
    }

    private void selectMode(int newMode) {
        mode = newMode;
        radioSystem.setChecked(mode == CalibrationPrefs.MODE_SYSTEM);
        radioReference.setChecked(mode == CalibrationPrefs.MODE_REFERENCE);
        radioScreen.setChecked(mode == CalibrationPrefs.MODE_SCREEN);

        cardSystem.setBackgroundResource(mode == CalibrationPrefs.MODE_SYSTEM
                ? R.drawable.bg_card_selected : R.drawable.bg_card);
        cardReference.setBackgroundResource(mode == CalibrationPrefs.MODE_REFERENCE
                ? R.drawable.bg_card_selected : R.drawable.bg_card);
        cardScreen.setBackgroundResource(mode == CalibrationPrefs.MODE_SCREEN
                ? R.drawable.bg_card_selected : R.drawable.bg_card);

        referencePanel.setVisibility(mode == CalibrationPrefs.MODE_REFERENCE
                ? View.VISIBLE : View.GONE);
        screenPanel.setVisibility(mode == CalibrationPrefs.MODE_SCREEN
                ? View.VISIBLE : View.GONE);
    }

    private void setReferenceTarget(float mm, String name, TextView selected, TextView other) {
        referenceTargetMm = mm;
        referenceName = name;
        refBar.setTargetMm(mm);
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(getColor(R.color.text_primary));
        other.setBackgroundResource(R.drawable.bg_chip);
        other.setTextColor(getColor(R.color.text_secondary));
        updateReferenceLabels(refBar.getPxPerMm(), refBar.isClamped());
    }

    private void updateReferenceLabels(float pxPerMm, boolean clamped) {
        referenceHint.setText(getString(R.string.reference_hint_fmt,
                referenceName,
                String.format(Locale.US, "%.2f", referenceTargetMm)));
        referenceScale.setText(getString(R.string.reference_scale_fmt,
                String.format(Locale.US, "%.2f", pxPerMm)));
        referenceScale.setTextColor(getColor(clamped ? R.color.warning : R.color.accent));
        referenceWarning.setVisibility(clamped ? View.VISIBLE : View.GONE);
    }

    private void save() {
        if (mode == CalibrationPrefs.MODE_SYSTEM) {
            CalibrationPrefs.saveSystem(this);
        } else if (mode == CalibrationPrefs.MODE_REFERENCE) {
            float pxPerMm = refBar.getPxPerMm();
            if (pxPerMm < 2.5f || pxPerMm > 80f || refBar.isClamped()) {
                Toast.makeText(this, R.string.reference_too_wide, Toast.LENGTH_SHORT).show();
                return;
            }
            CalibrationPrefs.saveReference(this, pxPerMm, referenceName);
        } else {
            float width = parse(widthInput);
            float height = parse(heightInput);
            if (width < 30f || width > 200f || height < 50f || height > 400f) {
                Toast.makeText(this, R.string.screen_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            CalibrationPrefs.saveScreen(this, width, height);
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private float parse(EditText input) {
        String text = input.getText().toString().trim().replace(',', '.');
        if (text.isEmpty()) {
            return 0f;
        }
        try {
            float value = Float.parseFloat(text);
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return 0f;
            }
            return value;
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private void applyInsets() {
        View content = findViewById(R.id.calibration_content);
        int left = content.getPaddingLeft();
        int top = content.getPaddingTop();
        int right = content.getPaddingRight();
        int bottom = content.getPaddingBottom();

        content.setOnApplyWindowInsetsListener((v, insets) -> {
            EdgeToEdge.Bars bars = EdgeToEdge.read(insets);
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
        content.requestApplyInsets();
    }
}

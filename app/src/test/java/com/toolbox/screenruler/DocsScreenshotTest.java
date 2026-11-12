package com.toolbox.screenruler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 30, qualifiers = "w411dp-h891dp-xxhdpi")
public class DocsScreenshotTest {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 2340;

    @Test
    public void captureMain() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        capture(activity.getWindow().getDecorView(), "main-horizontal.png", HEIGHT);
    }

    @Test
    public void captureMainVertical() throws Exception {
        CalibrationPrefs.setOrientation(RuntimeEnvironment.getApplication(),
                RulerView.ORIENT_VERTICAL);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        capture(activity.getWindow().getDecorView(), "main-vertical.png", HEIGHT);
    }

    @Test
    public void captureCalibrationSystem() throws Exception {
        CalibrationActivity activity =
                Robolectric.buildActivity(CalibrationActivity.class).setup().get();
        captureFitContent(activity, "calibration-system.png");
    }

    @Test
    public void captureCalibrationReference() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        CalibrationPrefs.saveReference(app, 18.9f, "银行卡短边");
        CalibrationActivity activity =
                Robolectric.buildActivity(CalibrationActivity.class).setup().get();
        activity.findViewById(R.id.preset_coin).performClick();
        captureFitContent(activity, "calibration-reference.png");
    }

    private static void captureFitContent(Activity activity, String name) throws Exception {
        View decor = activity.getWindow().getDecorView();
        ViewGroup content = activity.findViewById(R.id.calibration_content);

        layout(decor, 3200);
        int bottom = 0;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                bottom = Math.max(bottom, child.getBottom());
            }
        }
        int height = Math.min(3200, Math.max(bottom + content.getPaddingBottom(), 1400));
        draw(decor, name, height);
    }

    private static void capture(View decor, String name, int height) throws Exception {
        layout(decor, height);
        draw(decor, name, height);
    }

    private static void layout(View decor, int height) {
        decor.measure(View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, WIDTH, height);

        WindowInsets insets = new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, statusBarHeight(), 0, 0))
                .setInsets(WindowInsets.Type.navigationBars(),
                        Insets.of(0, 0, 0, navigationBarHeight()))
                .build();
        decor.dispatchApplyWindowInsets(insets);
        decor.measure(View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, WIDTH, height);
    }

    private static int statusBarHeight() {
        return 66;
    }

    private static int navigationBarHeight() {
        return 48;
    }

    private static void draw(View decor, String name, int height) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        decor.draw(canvas);

        File dir = new File(System.getProperty("user.dir"), "../docs/screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("cannot create " + dir.getAbsolutePath());
        }
        try (FileOutputStream out = new FileOutputStream(new File(dir, name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}

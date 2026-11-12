package com.toolbox.screenruler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.WindowManager;

final class CalibrationPrefs {

    static final int MODE_SYSTEM = 0;
    static final int MODE_REFERENCE = 1;
    static final int MODE_SCREEN = 2;

    private static final String FILE = "ruler_calibration";
    private static final String KEY_MODE = "mode";
    private static final String KEY_PX_PER_MM = "px_per_mm";
    private static final String KEY_REFERENCE_LABEL = "reference_label";
    private static final String KEY_SCREEN_WIDTH_MM = "screen_width_mm";
    private static final String KEY_SCREEN_HEIGHT_MM = "screen_height_mm";
    private static final String KEY_SYSTEM_ACK = "system_ack";
    private static final String KEY_ORIENTATION = "orientation";

    private CalibrationPrefs() {
    }

    static final class Values {
        final int mode;
        final float pxPerMmX;
        final float pxPerMmY;
        final String label;
        final boolean calibrated;
        final boolean systemAcknowledged;

        Values(int mode, float pxPerMmX, float pxPerMmY, String label,
               boolean calibrated, boolean systemAcknowledged) {
            this.mode = mode;
            this.pxPerMmX = pxPerMmX;
            this.pxPerMmY = pxPerMmY;
            this.label = label;
            this.calibrated = calibrated;
            this.systemAcknowledged = systemAcknowledged;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static Values resolve(Context context) {
        SharedPreferences p = prefs(context);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float systemX = systemPxPerMmX(context);
        float systemY = systemPxPerMmY(context);
        boolean ack = p.getBoolean(KEY_SYSTEM_ACK, false);
        int mode = p.getInt(KEY_MODE, MODE_SYSTEM);

        if (mode == MODE_REFERENCE) {
            float value = p.getFloat(KEY_PX_PER_MM, 0f);
            if (isSane(value)) {
                String label = p.getString(KEY_REFERENCE_LABEL, "");
                return new Values(mode, value, value, label, true, ack);
            }
            mode = MODE_SYSTEM;
        }

        if (mode == MODE_SCREEN) {
            float widthMm = p.getFloat(KEY_SCREEN_WIDTH_MM, 0f);
            float heightMm = p.getFloat(KEY_SCREEN_HEIGHT_MM, 0f);
            if (widthMm >= 20f && widthMm <= 300f && heightMm >= 40f && heightMm <= 500f) {
                DisplayMetrics real = realMetrics(context);
                int shortPx = Math.min(real.widthPixels, real.heightPixels);
                int longPx = Math.max(real.widthPixels, real.heightPixels);
                float shortPxPerMm = shortPx / widthMm;
                float longPxPerMm = longPx / heightMm;
                boolean landscape = dm.widthPixels > dm.heightPixels;
                float x = landscape ? longPxPerMm : shortPxPerMm;
                float y = landscape ? shortPxPerMm : longPxPerMm;
                if (isSane(x) && isSane(y)) {
                    return new Values(mode, x, y, null, true, ack);
                }
            }
            mode = MODE_SYSTEM;
        }

        return new Values(MODE_SYSTEM, systemX, systemY, null, false, ack);
    }

    static float systemPxPerMmX(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return safeDpi(dm.xdpi, dm.densityDpi) / 25.4f;
    }

    static float systemPxPerMmY(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return safeDpi(dm.ydpi, dm.densityDpi) / 25.4f;
    }

    static float[] estimateScreenMm(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float pxPerMmX = systemPxPerMmX(context);
        float pxPerMmY = systemPxPerMmY(context);
        float a = dm.widthPixels / pxPerMmX;
        float b = dm.heightPixels / pxPerMmY;
        return new float[]{Math.min(a, b), Math.max(a, b)};
    }

    static float[] storedScreenMm(Context context) {
        SharedPreferences p = prefs(context);
        return new float[]{
                p.getFloat(KEY_SCREEN_WIDTH_MM, 0f),
                p.getFloat(KEY_SCREEN_HEIGHT_MM, 0f)
        };
    }

    static String referenceLabel(Context context) {
        return prefs(context).getString(KEY_REFERENCE_LABEL, "");
    }

    static void saveSystem(Context context) {
        prefs(context).edit()
                .putInt(KEY_MODE, MODE_SYSTEM)
                .putBoolean(KEY_SYSTEM_ACK, true)
                .apply();
    }

    static void saveReference(Context context, float pxPerMm, String label) {
        prefs(context).edit()
                .putInt(KEY_MODE, MODE_REFERENCE)
                .putFloat(KEY_PX_PER_MM, pxPerMm)
                .putString(KEY_REFERENCE_LABEL, label)
                .putBoolean(KEY_SYSTEM_ACK, true)
                .apply();
    }

    static void saveScreen(Context context, float widthMm, float heightMm) {
        prefs(context).edit()
                .putInt(KEY_MODE, MODE_SCREEN)
                .putFloat(KEY_SCREEN_WIDTH_MM, widthMm)
                .putFloat(KEY_SCREEN_HEIGHT_MM, heightMm)
                .putBoolean(KEY_SYSTEM_ACK, true)
                .apply();
    }

    static int getOrientation(Context context) {
        return prefs(context).getInt(KEY_ORIENTATION, RulerView.ORIENT_HORIZONTAL);
    }

    static void setOrientation(Context context, int orientation) {
        prefs(context).edit().putInt(KEY_ORIENTATION, orientation).apply();
    }

    private static DisplayMetrics realMetrics(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null && wm.getDefaultDisplay() != null) {
            wm.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            metrics.setTo(context.getResources().getDisplayMetrics());
        }
        return metrics;
    }

    private static float safeDpi(float reported, int densityDpi) {
        if (reported >= 100f && reported <= 1200f) {
            return reported;
        }
        return densityDpi > 0 ? densityDpi : 160f;
    }

    private static boolean isSane(float pxPerMm) {
        return pxPerMm >= 2f && pxPerMm <= 120f;
    }
}

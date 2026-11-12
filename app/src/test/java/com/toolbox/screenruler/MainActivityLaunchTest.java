package com.toolbox.screenruler;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class MainActivityLaunchTest {

    @Test
    public void launchesWithoutCrash() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.setup();
        assertNotNull(controller.get());
    }

    @Test
    public void calibrationLaunchesWithoutCrash() {
        ActivityController<CalibrationActivity> controller =
                Robolectric.buildActivity(CalibrationActivity.class);
        controller.setup();
        assertNotNull(controller.get());
    }
}

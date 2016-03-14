package org.robolectric.shadows;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.TestRunners;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import libcore.icu.ICU;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunners.MultiApiWithDefaults.class)
@Config(sdk = {
  Build.VERSION_CODES.LOLLIPOP,
  Build.VERSION_CODES.LOLLIPOP_MR1})
public class ShadowICUTest {
  @Test
  public void getBestDateTimePattern_returnsReasonableValue() {
    assertThat(ICU.getBestDateTimePattern("hm", null)).isEqualTo("hm");
  }

  @Test
  public void datePickerShouldNotCrashWhenAskingForBestDateTimePattern() {
    ActivityController<DatePickerActivity> activityController = Robolectric.buildActivity(DatePickerActivity.class);
    activityController.setup();
  }

  private static class DatePickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      LinearLayout view = new LinearLayout(this);
      view.setId(1);
      DatePicker datePicker = new DatePicker(this);
      view.addView(datePicker);

      setContentView(view);
    }
  }
}
package org.robolectric.integration_tests.axt;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.os.Bundle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for {@link ActivityScenario} that verify it behaves consistently on device and
 * Robolectric.
 */
@RunWith(AndroidJUnit4.class)
public class ActivityScenarioTest {

  private static final List<String> callbacks = new ArrayList<>();

  public static class TranscriptActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      callbacks.add("onCreate");
    }

    @Override
    public void onStart() {
      super.onStart();
      callbacks.add("onStart");
    }

    @Override
    public void onResume() {
      super.onResume();
      callbacks.add("onResume");
    }

    @Override
    public void onPause() {
      super.onPause();
      callbacks.add("onPause");
    }

    @Override
    public void onStop() {
      super.onStop();
      callbacks.add("onStop");
    }

    @Override
    public void onRestart() {
      super.onRestart();
      callbacks.add("onRestart");
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      callbacks.add("onDestroy");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
      super.onWindowFocusChanged(hasFocus);
      callbacks.add("onWindowFocusChanged " + hasFocus);
    }
  }

  @Before
  public void setUp() {
    callbacks.clear();
  }

  @Test
  public void launch_callbackSequence() {
    ActivityScenario<TranscriptActivity> activityScenario =
        ActivityScenario.launch(TranscriptActivity.class);
    assertThat(activityScenario).isNotNull();
    assertThat(callbacks)
        .containsExactly("onCreate", "onStart", "onResume", "onWindowFocusChanged true");
  }
}

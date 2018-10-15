package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import android.widget.RadioGroup;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(AndroidJUnit4.class)
public class ShadowRadioGroupTest {
  private static final int BUTTON_ID = 3245;

  @Test
  public void checkedRadioButtonId() throws Exception {
    RadioGroup radioGroup = new RadioGroup(RuntimeEnvironment.application);
    assertThat(radioGroup.getCheckedRadioButtonId()).isEqualTo(-1);
    radioGroup.check(99);
    assertThat(radioGroup.getCheckedRadioButtonId()).isEqualTo(99);
  }

  @Test
  public void check_shouldCallOnCheckedChangeListener() throws Exception {
    RadioGroup radioGroup = new RadioGroup(RuntimeEnvironment.application);
    TestOnCheckedChangeListener listener = new TestOnCheckedChangeListener();
    radioGroup.setOnCheckedChangeListener(listener);

    radioGroup.check(BUTTON_ID);

    assertEquals(Arrays.asList(BUTTON_ID), listener.onCheckedChangedCheckedIds);
    assertEquals(Arrays.asList(radioGroup), listener.onCheckedChangedGroups);
  }

  @Test
  public void clearCheck_shouldCallOnCheckedChangeListenerTwice() throws Exception {
    RadioGroup radioGroup = new RadioGroup(RuntimeEnvironment.application);
    TestOnCheckedChangeListener listener = new TestOnCheckedChangeListener();

    radioGroup.check(BUTTON_ID);
    radioGroup.setOnCheckedChangeListener(listener);
    radioGroup.clearCheck();

    assertEquals(Arrays.asList(-1), listener.onCheckedChangedCheckedIds);
    assertEquals(Arrays.asList(radioGroup), listener.onCheckedChangedGroups);
  }

  private static class TestOnCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {
    public ArrayList<RadioGroup> onCheckedChangedGroups = new ArrayList<>();
    public ArrayList<Integer> onCheckedChangedCheckedIds = new ArrayList<>();

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
      onCheckedChangedGroups.add(group);
      onCheckedChangedCheckedIds.add(checkedId);
    }
  }
}

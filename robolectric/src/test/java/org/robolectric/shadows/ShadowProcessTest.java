package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Test ShadowProcess */
@RunWith(RobolectricTestRunner.class)
public class ShadowProcessTest {
  @Test
  public void shouldBeZeroWhenNotSet() {
    assertThat(android.os.Process.myPid()).isEqualTo(0);
  }

  @Test
  public void shouldGetMyPidAsSet() {
    ShadowProcess.setPid(3);
    assertThat(android.os.Process.myPid()).isEqualTo(3);
  }

  @Test
  public void shouldGetMyUidAsSet() {
    ShadowProcess.setUid(123);
    assertThat(android.os.Process.myUid()).isEqualTo(123);
  }
}


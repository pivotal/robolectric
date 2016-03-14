package org.robolectric.shadows;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowSurfaceTest {
  private final SurfaceTexture texture = new SurfaceTexture(0);
  private final Surface surface = new Surface(texture);

  @Test
  public void getSurfaceTexture_returnsSurfaceTexture() throws Exception {
    assertThat(shadowOf(surface).getSurfaceTexture()).isEqualTo(texture);
  }
}

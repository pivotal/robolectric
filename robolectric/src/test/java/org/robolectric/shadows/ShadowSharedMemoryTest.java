package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.os.SharedMemory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Unit tests for {@link ShadowSharedMemory}. */
@RunWith(AndroidJUnit4.class)
public class ShadowSharedMemoryTest {

  @Test
  @Config(minSdk = Build.VERSION_CODES.O_MR1)
  public void create() throws Exception {
    SharedMemory sharedMemory = SharedMemory.create("foo", 4);
    assertThat(sharedMemory).isNotNull();
  }

  @Test
  @Config(minSdk = Build.VERSION_CODES.O_MR1)
  public void size() throws Exception {
    SharedMemory sharedMemory = SharedMemory.create("foo", 4);
    assertThat(sharedMemory.getSize()).isEqualTo(4);
  }

  @Test
  @Config(minSdk = Build.VERSION_CODES.O_MR1)
  public void mapReadWrite() throws Exception {
    SharedMemory sharedMemory = SharedMemory.create("foo", 4);
    assertThat(sharedMemory.mapReadWrite()).isNotNull();
  }
}

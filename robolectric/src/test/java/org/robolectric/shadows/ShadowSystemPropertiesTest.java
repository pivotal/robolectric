package org.robolectric.shadows;

import static org.assertj.core.api.Assertions.assertThat;

import android.os.SystemProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ShadowSystemPropertiesTest {

  @Test
  public void get() {
    assertThat(SystemProperties.get("ro.product.device")).isEqualTo("robolectric");
  }

  @Test
  public void getWithDefault() {
    assertThat(SystemProperties.get("foo", "bar")).isEqualTo("bar");
  }

  // separately test loading the sdk int level for, to ensure the correct build.prop is loaded

  @Test
  @Config(sdk = 16)
  public void getInt16() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(16);
  }

  @Test
  @Config(sdk = 17)
  public void getInt17() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(17);
  }

  @Test
  @Config(sdk = 18)
  public void getInt18() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(18);
  }

  @Test
  @Config(sdk = 19)
  public void getInt19() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(19);
  }

  @Test
  @Config(sdk = 21)
  public void getInt21() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(21);
  }

  @Test
  @Config(sdk = 22)
  public void getInt22() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(22);
  }

  @Test
  @Config(sdk = 23)
  public void getInt23() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(23);
  }

  @Test
  @Config(sdk = 24)
  public void getInt24() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(24);
  }

  @Test
  @Config(sdk = 25)
  public void getInt25() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(25);
  }

  @Test
  @Config(sdk = 26)
  public void getInt26() {
    assertThat(SystemProperties.getInt("ro.build.version.sdk", 0)).isEqualTo(26);
  }

  @Test
  public void set() {
    assertThat(SystemProperties.get("newkey")).isEqualTo("");
    SystemProperties.set("newkey", "val");
    assertThat(SystemProperties.get("newkey")).isEqualTo("val");
    SystemProperties.set("newkey", null);
    assertThat(SystemProperties.get("newkey")).isEqualTo("");
  }
}

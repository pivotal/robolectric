package org.robolectric;

import static org.assertj.core.api.Assertions.assertThat;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@Config(qualifiers = "en")
@RunWith(RobolectricTestRunner.class)
public class QualifiersTest {

  private Resources resources;

  @Before
  public void setUp() throws Exception {
    resources = RuntimeEnvironment.application.getResources();
  }

  @Test
  @Config(sdk = 26)
  public void testDefaultQualifiers() throws Exception {
    assertThat(RuntimeEnvironment.getQualifiers()).isEqualTo("en-ldltr-sw320dp-w320dp-normal-notlong-notround-port-notnight-mdpi-finger-v26");
  }

  @Test
  @Config(qualifiers = "land")
  public void orientation() throws Exception {
    assertThat(Robolectric.setupActivity(Activity.class).getResources().getConfiguration().orientation)
        .isEqualTo(Configuration.ORIENTATION_LANDSCAPE);
  }

  @Test
  public void shouldGetFromClass() throws Exception {
    assertThat(RuntimeEnvironment.getQualifiers()).contains("en");
  }

  @Test @Config(qualifiers = "fr")
  public void shouldGetFromMethod() throws Exception {
    assertThat(RuntimeEnvironment.getQualifiers()).contains("fr");
  }

  @Test @Config(qualifiers = "de")
  public void getQuantityString() throws Exception {
    assertThat(resources.getQuantityString(R.plurals.minute, 2)).isEqualTo(
        resources.getString(R.string.minute_plural));
  }

  @Test
  public void inflateLayout_defaultsTo_sw320dp() throws Exception {
    View view = Robolectric.setupActivity(Activity.class).getLayoutInflater().inflate(R.layout.layout_smallest_width, null);
    TextView textView = view.findViewById(R.id.text1);
    assertThat(textView.getText()).isEqualTo("320");

    assertThat(resources.getConfiguration().smallestScreenWidthDp).isEqualTo(320);
  }

  @Test @Config(qualifiers = "sw720dp")
  public void inflateLayout_overridesTo_sw720dp() throws Exception {
    View view = Robolectric.setupActivity(Activity.class).getLayoutInflater().inflate(R.layout.layout_smallest_width, null);
    TextView textView = view.findViewById(R.id.text1);
    assertThat(textView.getText()).isEqualTo("720");

    assertThat(resources.getConfiguration().smallestScreenWidthDp).isEqualTo(720);
  }

  @Test @Config(qualifiers = "b+sr+Latn")
  public void supportsBcp47() throws Exception {
    assertThat(resources.getString(R.string.hello)).isEqualTo("Zdravo");
  }

  @Test
  public void defaultScreenWidth() {
    assertThat(resources.getBoolean(R.bool.value_only_present_in_w320dp)).isTrue();
    assertThat(resources.getConfiguration().screenWidthDp).isEqualTo(320);
  }
}

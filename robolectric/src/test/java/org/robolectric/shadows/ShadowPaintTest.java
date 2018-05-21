package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.M;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Color;
import android.graphics.Paint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
public class ShadowPaintTest {

  @Test
  public void shouldGetIsDitherInfo() {
    Paint paint = Shadow.newInstanceOf(Paint.class);
    assertFalse(paint.isAntiAlias());
    ShadowPaint shadowPaint = shadowOf(paint);
    shadowPaint.setAntiAlias(true);
    assertTrue(paint.isAntiAlias());
  }

  @Test
  public void shouldGetIsAntiAlias() {
    Paint paint = Shadow.newInstanceOf(Paint.class);
    assertFalse(paint.isAntiAlias());
    ShadowPaint shadowPaint = shadowOf(paint);
    shadowPaint.setAntiAlias(true);
    assertTrue(paint.isAntiAlias());
    shadowPaint.setAntiAlias(false);
    assertFalse(paint.isAntiAlias());
  }

  @Test
  public void testCtor() {
    Paint paint = Shadow.newInstanceOf(Paint.class);
    assertFalse(paint.isAntiAlias());
    ShadowPaint shadowPaint = shadowOf(paint);
    shadowPaint.__constructor__( Paint.ANTI_ALIAS_FLAG );
    assertTrue(paint.isAntiAlias());
  }

  @Test
  public void testCtorWithPaint() {
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    paint.setAlpha(72);
    paint.setFlags(2345);

    Paint other = new Paint(paint);
    assertThat(other.getColor()).isEqualTo(Color.RED);
    assertThat(other.getAlpha()).isEqualTo(72);
    assertThat(other.getFlags()).isEqualTo(2345);
  }

  @Test
  public void shouldGetAndSetTextAlignment() throws Exception {
    Paint paint = Shadow.newInstanceOf(Paint.class);
    assertThat(paint.getTextAlign()).isEqualTo(Paint.Align.LEFT);
    paint.setTextAlign(Paint.Align.CENTER);
    assertThat(paint.getTextAlign()).isEqualTo(Paint.Align.CENTER);
  }

  @Test
  public void measureTextActuallyMeasuresLength() throws Exception {
    Paint paint = Shadow.newInstanceOf(Paint.class);
    assertThat(paint.measureText("Hello")).isEqualTo(5.0f);
    assertThat(paint.measureText("Hello", 1, 3)).isEqualTo(2.0f);
    assertThat(paint.measureText(new StringBuilder("Hello"), 1, 4)).isEqualTo(3.0f);
  }

  @Test
  public void createPaintFromPaint() throws Exception {
    Paint origPaint = new Paint();
    assertThat(new Paint(origPaint).getTextLocale()).isSameAs(origPaint.getTextLocale());
  }

  @Test
  @Config(minSdk = M)
  public void hasGlyph() throws Exception {
    Paint origPaint = new Paint();
    assertThat(origPaint.hasGlyph("M")).isTrue();
    assertThat(origPaint.hasGlyph("\uD83D\uDC75")).isTrue();
    assertThat(origPaint.hasGlyph("text")).isTrue();

    ShadowPaint shadowPaint = shadowOf(paint);
    shadowPaint.addStringWithoutGlyph("\uD83D\uDC75");
    assertThat(origPaint.hasGlyph("\uD83D\uDC75")).isFalse();
  }
}

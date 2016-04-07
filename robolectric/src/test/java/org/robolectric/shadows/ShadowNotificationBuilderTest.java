package org.robolectric.shadows;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.widget.ProgressBar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.R;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowNotificationBuilderTest {
  private final Notification.Builder builder = new Notification.Builder(RuntimeEnvironment.application);

  @Test
  public void build_setsContentTitleOnNotification() throws Exception {
    Notification notification = builder.setContentTitle("Hello").build();
    assertThat(shadowOf(notification).getContentTitle().toString()).isEqualTo("Hello");
  }

  @Test
  public void build_whenSetOngoingNotSet_leavesSetOngoingAsFalse() {
    Notification notification = builder.build();
    assertThat(shadowOf(notification).isOngoing()).isFalse();
  }

  @Test
  public void build_whenSetOngoing_setsOngoingToTrue() {
    Notification notification = builder.setOngoing(true).build();
    assertThat(shadowOf(notification).isOngoing()).isTrue();
  }

  @Config(sdk = {
      Build.VERSION_CODES.JELLY_BEAN_MR1,
      Build.VERSION_CODES.JELLY_BEAN_MR2,
      Build.VERSION_CODES.KITKAT,
      Build.VERSION_CODES.LOLLIPOP,
      Build.VERSION_CODES.LOLLIPOP_MR1,
      Build.VERSION_CODES.M})
  @Test
  public void build_whenShowWhenNotSet_setsShowWhenOnNotificationToTrue() {
    Notification notification = builder.setWhen(100).setShowWhen(true).build();

    assertThat(shadowOf(notification).isWhenShown()).isTrue();
  }

  @Config(sdk = {
      Build.VERSION_CODES.JELLY_BEAN_MR1,
      Build.VERSION_CODES.JELLY_BEAN_MR2,
      Build.VERSION_CODES.KITKAT,
      Build.VERSION_CODES.LOLLIPOP,
      Build.VERSION_CODES.LOLLIPOP_MR1,
      Build.VERSION_CODES.M})
  @Test
  public void build_setShowWhenOnNotification() {
    Notification notification = builder.setShowWhen(false).build();

    assertThat(shadowOf(notification).isWhenShown()).isFalse();
  }

  @Test
  public void build_setsContentTextOnNotification() throws Exception {
    Notification notification = builder.setContentText("Hello Text").build();

    assertThat(shadowOf(notification).getContentText().toString()).isEqualTo("Hello Text");
  }

  @Test
  public void build_setsTickerOnNotification() throws Exception {
    Notification notification = builder.setTicker("My ticker").build();

    assertThat(notification.tickerText).isEqualTo("My ticker");
  }

  @Test
  public void build_setsContentInfoOnNotification() throws Exception {
    builder.setContentInfo("11");
    Notification notification = builder.build();
    assertThat(shadowOf(notification).getContentInfo().toString()).isEqualTo("11");
  }

  @Config(sdk = Build.VERSION_CODES.M)
  @Test
  public void build_setsIconOnNotification() throws Exception {
    Notification notification = builder.setSmallIcon(R.drawable.an_image).build();

    assertThat(notification.getSmallIcon().getResId()).isEqualTo(R.drawable.an_image);
  }

  @Test
  public void build_setsWhenOnNotification() throws Exception {
    Notification notification = builder.setWhen(11L).build();

    assertThat(notification.when).isEqualTo(11L);
  }

  @Test
  public void build_setsProgressOnNotification_true() throws Exception {
    Notification notification = builder.setProgress(36, 57, true).build();

    ProgressBar progressBar = shadowOf(notification).getProgressBar();
    // If indeterminate then max and progress values are ignored.
    assertThat(progressBar.isIndeterminate()).isTrue();
  }

  @Test
  public void build_setsProgressOnNotification_false() throws Exception {
    Notification notification = builder.setProgress(50, 10, false).build();

    ProgressBar progressBar = shadowOf(notification).getProgressBar();
    assertThat(progressBar.getMax()).isEqualTo(50);
    assertThat(progressBar.getProgress()).isEqualTo(10);
    assertThat(progressBar.isIndeterminate()).isFalse();
  }

  @Config(sdk = {
      Build.VERSION_CODES.JELLY_BEAN_MR1,
      Build.VERSION_CODES.JELLY_BEAN_MR2,
      Build.VERSION_CODES.KITKAT,
      Build.VERSION_CODES.LOLLIPOP,
      Build.VERSION_CODES.LOLLIPOP_MR1,
      Build.VERSION_CODES.M})
  @Test
  public void build_setsUsesChronometerOnNotification_true() throws Exception {
    Notification notification = builder.setUsesChronometer(true).setWhen(10).setShowWhen(true).build();

    assertThat(shadowOf(notification).usesChronometer()).isTrue();
  }

  @Config(sdk = {
      Build.VERSION_CODES.JELLY_BEAN_MR1,
      Build.VERSION_CODES.JELLY_BEAN_MR2,
      Build.VERSION_CODES.KITKAT,
      Build.VERSION_CODES.LOLLIPOP,
      Build.VERSION_CODES.LOLLIPOP_MR1,
      Build.VERSION_CODES.M})
  @Test
  public void build_setsUsesChronometerOnNotification_false() throws Exception {
    Notification notification = builder.setUsesChronometer(false).setWhen(10).setShowWhen(true).build();

    assertThat(shadowOf(notification).usesChronometer()).isFalse();
  }

  @Test
  public void build_handlesNullContentTitle() {
    Notification notification = builder.setContentTitle(null).build();

    assertThat(shadowOf(notification).getContentTitle()).isEmpty();
  }

  @Test
  public void build_handlesNullContentText() {
    Notification notification = builder.setContentText(null).build();

    assertThat(shadowOf(notification).getContentText()).isEmpty();
  }

  @Test
  public void build_handlesNullTicker() {
    Notification notification = builder.setTicker(null).build();

    assertThat(notification.tickerText).isNull();
  }

  @Test
  public void build_handlesNullContentInfo() {
    Notification notification = builder.setContentInfo(null).build();

    assertThat(shadowOf(notification).getContentInfo()).isEmpty();
  }

  @Test
  @Config(sdk = {
      Build.VERSION_CODES.JELLY_BEAN_MR2,
      Build.VERSION_CODES.KITKAT,
      Build.VERSION_CODES.LOLLIPOP,
      Build.VERSION_CODES.LOLLIPOP_MR1,
      Build.VERSION_CODES.M})
  public void build_addsActionToNotification() throws Exception {
    PendingIntent action = PendingIntent.getBroadcast(RuntimeEnvironment.application, 0, null, 0);
    Notification notification = builder.addAction(0, "Action", action).build();

    assertThat(notification.actions[0].actionIntent).isEqualToComparingFieldByField(action);
  }

  @Test
  public void withBigTextStyle() {
    Notification notification = builder.setStyle(new Notification.BigTextStyle(builder)
        .bigText("BigText")
        .setBigContentTitle("Title")
        .setSummaryText("Summary"))
        .build();

    assertThat(shadowOf(notification).getBigText()).isEqualTo("BigText");
    assertThat(shadowOf(notification).getBigContentTitle()).isEqualTo("Title");
    assertThat(shadowOf(notification).getBigContentText()).isEqualTo("Summary");
  }
}

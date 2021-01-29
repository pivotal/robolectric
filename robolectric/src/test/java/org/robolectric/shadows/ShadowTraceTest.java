package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.Q;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.os.Trace;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowTrace.AsyncTraceSection;

/** Test for {@link ShadowTrace}. */
@RunWith(AndroidJUnit4.class)
@Config(minSdk = JELLY_BEAN_MR2)
public class ShadowTraceTest {
  private static final String VERY_LONG_TAG_NAME = String.format(String.format("%%%ds", 128), "A");

  // Arbitrary value
  private static final Integer COOKIE = 353882576;

  @Test
  public void beginSection_calledOnce_addsSection() throws Exception {
    Trace.beginSection("section1");

    assertThat(ShadowTrace.getCurrentSections()).containsExactly("section1");
    assertThat(ShadowTrace.getPreviousSections()).isEmpty();
  }

  @Test
  public void beginSection_calledTwice_addsBothSections() throws Exception {
    Trace.beginSection("section1");
    Trace.beginSection("section2");

    assertThat(ShadowTrace.getCurrentSections()).containsExactly("section1", "section2");
    assertThat(ShadowTrace.getPreviousSections()).isEmpty();
  }

  @Test
  public void beginSection_tagIsNull_throwsNullPointerException() throws Exception {
    try {
      Trace.beginSection(null);
      fail("Must throw");
    } catch (NullPointerException e) {
      // Must throw.
    }
  }

  @Test
  public void beginSection_tagIsNullAndCrashDisabled_doesNotThrow() throws Exception {
    ShadowTrace.doNotUseSetCrashOnIncorrectUsage(false);
    Trace.beginSection(null);
    // Should not crash.
  }

  @Test
  public void beginSection_tagIsTooLong_throwsIllegalArgumentException() throws Exception {
    try {
      Trace.beginSection(VERY_LONG_TAG_NAME);
      fail("Must throw");
    } catch (IllegalArgumentException e) {
      // Must throw.
    }
  }

  @Test
  public void beginSection_tagIsTooLongAndCrashDisabled_doesNotThrow() throws Exception {
    ShadowTrace.doNotUseSetCrashOnIncorrectUsage(false);
    Trace.beginSection(VERY_LONG_TAG_NAME);
    // Should not crash.
  }

  @Test
  public void endSection_oneSection_closesSection() throws Exception {
    Trace.beginSection("section1");

    Trace.endSection();

    assertThat(ShadowTrace.getCurrentSections()).isEmpty();
    assertThat(ShadowTrace.getPreviousSections()).containsExactly("section1");
  }

  @Test
  public void endSection_twoSections_closesLastSection() throws Exception {
    Trace.beginSection("section1");
    Trace.beginSection("section2");

    Trace.endSection();

    assertThat(ShadowTrace.getCurrentSections()).containsExactly("section1");
    assertThat(ShadowTrace.getPreviousSections()).containsExactly("section2");
  }

  @Test
  public void endSection_twoRecursiveSectionsAndCalledTwice_closesAllSections() throws Exception {
    Trace.beginSection("section1");
    Trace.beginSection("section2");

    Trace.endSection();
    Trace.endSection();

    assertThat(ShadowTrace.getCurrentSections()).isEmpty();
    assertThat(ShadowTrace.getPreviousSections()).containsExactly("section2", "section1");
  }

  @Test
  public void endSection_twoSequentialSections_closesAllSections() throws Exception {
    Trace.beginSection("section1");
    Trace.endSection();
    Trace.beginSection("section2");
    Trace.endSection();

    assertThat(ShadowTrace.getCurrentSections()).isEmpty();
    assertThat(ShadowTrace.getPreviousSections()).containsExactly("section1", "section2");
  }

  @Test
  public void endSection_calledBeforeBeginning_doesNotThrow() throws Exception {
    Trace.endSection();
    // Should not crash.
  }

  @Test
  public void endSection_oneSectionButCalledTwice_doesNotThrow() throws Exception {
    Trace.beginSection("section1");

    Trace.endSection();
    Trace.endSection();
    // Should not crash.
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_calledOnce_addsSection() throws Exception {
    Trace.beginAsyncSection("section1", COOKIE);

    assertThat(ShadowTrace.getCurrentAsyncSections())
        .containsExactly(
            AsyncTraceSection.newBuilder().setSectionName("section1").setCookie(COOKIE).build());
    assertThat(ShadowTrace.getPreviousAsyncSections()).isEmpty();
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_calledTwice_addsBothSections() throws Exception {
    Trace.beginAsyncSection("section1", COOKIE);
    Trace.beginAsyncSection("section2", COOKIE);

    assertThat(ShadowTrace.getCurrentAsyncSections())
        .containsExactly(
            AsyncTraceSection.newBuilder().setSectionName("section1").setCookie(COOKIE).build(),
            AsyncTraceSection.newBuilder().setSectionName("section2").setCookie(COOKIE).build());
    assertThat(ShadowTrace.getPreviousAsyncSections()).isEmpty();
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_tagIsNull_throwsNullPointerException() throws Exception {
    try {
      Trace.beginAsyncSection(null, COOKIE);
      fail("Must throw");
    } catch (NullPointerException e) {
      // Must throw.
    }
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_tagIsNullAndCrashDisabled_doesNotThrow() throws Exception {
    ShadowTrace.doNotUseSetCrashOnIncorrectUsage(false);
    Trace.beginAsyncSection(null, COOKIE);
    // Should not crash.
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_tagIsTooLong_throwsIllegalArgumentException() throws Exception {
    try {
      Trace.beginAsyncSection(VERY_LONG_TAG_NAME, COOKIE);
      fail("Must throw");
    } catch (IllegalArgumentException e) {
      // Must throw.
    }
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_tagIsTooLongAndCrashDisabled_doesNotThrow() throws Exception {
    ShadowTrace.doNotUseSetCrashOnIncorrectUsage(false);
    Trace.beginAsyncSection(VERY_LONG_TAG_NAME, COOKIE);
    // Should not crash.
  }

  @Test
  @Config(minSdk = Q)
  public void endAsyncSection_oneSection_closesSection() throws Exception {
    Trace.beginAsyncSection("section1", COOKIE);

    Trace.endAsyncSection("section1", COOKIE);

    assertThat(ShadowTrace.getCurrentAsyncSections()).isEmpty();
    assertThat(ShadowTrace.getPreviousAsyncSections())
        .containsExactly(
            AsyncTraceSection.newBuilder().setSectionName("section1").setCookie(COOKIE).build());
  }

  @Test
  @Config(minSdk = Q)
  public void async_sameSectionTwoCookies_separateTraces() throws Exception {
    Trace.beginAsyncSection("section1", COOKIE);
    Trace.beginAsyncSection("section1", COOKIE + 1);

    AsyncTraceSection sectionWithCookie =
        AsyncTraceSection.newBuilder().setSectionName("section1").setCookie(COOKIE).build();
    AsyncTraceSection sectionWithCookiePlusOne =
        AsyncTraceSection.newBuilder().setSectionName("section1").setCookie(COOKIE + 1).build();

    assertThat(ShadowTrace.getCurrentAsyncSections())
        .containsExactly(sectionWithCookie, sectionWithCookiePlusOne);
    assertThat(ShadowTrace.getPreviousAsyncSections()).isEmpty();

    Trace.endAsyncSection("section1", COOKIE);

    assertThat(ShadowTrace.getCurrentAsyncSections()).containsExactly(sectionWithCookiePlusOne);
    assertThat(ShadowTrace.getPreviousAsyncSections()).containsExactly(sectionWithCookie);

    Trace.endAsyncSection("section1", COOKIE + 1);
    assertThat(ShadowTrace.getCurrentAsyncSections()).isEmpty();
    assertThat(ShadowTrace.getPreviousAsyncSections())
        .containsExactly(sectionWithCookie, sectionWithCookiePlusOne);
  }

  @Test
  public void reset_resetsInternalState() throws Exception {
    Trace.beginSection("section1");
    Trace.endSection();
    Trace.beginSection("section2");

    ShadowTrace.reset();

    assertThat(ShadowTrace.getCurrentSections()).isEmpty();
    assertThat(ShadowTrace.getPreviousSections()).isEmpty();
  }

  @Test
  public void toggleEnabledTest() throws Exception {
    Trace.beginSection("section1");
    assertThat(ShadowTrace.isEnabled()).isTrue();
    ShadowTrace.setEnabled(false);
    assertThat(ShadowTrace.isEnabled()).isFalse();
    ShadowTrace.setEnabled(true);
    assertThat(ShadowTrace.isEnabled()).isTrue();
    Trace.endSection();

  }

  @Test
  public void traceFromIndependentThreads() throws ExecutionException, InterruptedException {
    ShadowTrace.doNotUseSetCrashOnIncorrectUsage(true);
    ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    try {
      Trace.beginSection("main_looper_trace");
      Future<?> f = backgroundExecutor.submit(() -> Trace.beginSection("bg_trace"));
      f.get();
      Trace.endSection();

      assertThat(ShadowTrace.getPreviousSections()).containsExactly("main_looper_trace");
      assertThat(ShadowTrace.getCurrentSections()).isEmpty();

      f =
          backgroundExecutor.submit(
              () -> {
                assertThat(ShadowTrace.getCurrentSections()).containsExactly("bg_trace");
                assertThat(ShadowTrace.getPreviousSections()).isEmpty();

                Trace.endSection();

                assertThat(ShadowTrace.getPreviousSections()).containsExactly("bg_trace");
                assertThat(ShadowTrace.getCurrentSections()).isEmpty();
              });
      f.get();
    } finally {
      backgroundExecutor.shutdown();
    }
  }

  @Test
  @Config(minSdk = Q)
  public void beginAsyncSection_multipleThreads_stateIsShared()
      throws ExecutionException, InterruptedException {
    ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    AsyncTraceSection mainLooperSection =
        AsyncTraceSection.newBuilder()
            .setSectionName("main_looper_trace")
            .setCookie(COOKIE)
            .build();
    AsyncTraceSection bgLooperSection =
        AsyncTraceSection.newBuilder().setSectionName("bg_trace").setCookie(COOKIE).build();

    try {
      Trace.beginAsyncSection("main_looper_trace", COOKIE);
      Future<?> f = backgroundExecutor.submit(() -> Trace.beginAsyncSection("bg_trace", COOKIE));
      f.get();

      assertThat(ShadowTrace.getCurrentAsyncSections())
          .containsExactly(mainLooperSection, bgLooperSection);
      assertThat(ShadowTrace.getPreviousAsyncSections()).isEmpty();

      Trace.endAsyncSection("main_looper_trace", COOKIE);
      assertThat(ShadowTrace.getPreviousAsyncSections()).containsExactly(mainLooperSection);
      assertThat(ShadowTrace.getCurrentAsyncSections()).containsExactly(bgLooperSection);

      f =
          backgroundExecutor.submit(
              () -> {
                Trace.endAsyncSection("bg_trace", COOKIE);
                assertThat(ShadowTrace.getPreviousAsyncSections())
                    .containsExactly(mainLooperSection, bgLooperSection);
                assertThat(ShadowTrace.getCurrentAsyncSections()).isEmpty();
              });
      f.get();

    } finally {
      backgroundExecutor.shutdown();
    }
  }
}

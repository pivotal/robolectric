package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.Q;
import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.os.Trace;
import android.util.Log;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Shadow implementation for {@link Trace}, which stores the traces locally in arrays (unlike the
 * real implementation) and allows reading them.
 */
@Implements(Trace.class)
public class ShadowTrace {
  private static final String TAG = "ShadowTrace";

  private static final ThreadLocal<Deque<String>> currentSections =
      ThreadLocal.withInitial(() -> new ArrayDeque<>());

  private static final ThreadLocal<Queue<String>> previousSections =
      ThreadLocal.withInitial((Supplier<Deque<String>>) () -> new ArrayDeque<>());

  private static final Set<AsyncTraceSection> currentAsyncSections = new HashSet<>();

  private static final Set<AsyncTraceSection> previousAsyncSections = new HashSet<>();

  private static final boolean CRASH_ON_INCORRECT_USAGE_DEFAULT = true;
  private static boolean crashOnIncorrectUsage = CRASH_ON_INCORRECT_USAGE_DEFAULT;
  private static boolean appTracingAllowed = true;
  private static boolean isEnabled = true;

  private static final long TRACE_TAG_APP = 1L << 12;
  private static final int MAX_SECTION_NAME_LEN = 127;

  /** Starts a new trace section with given name. */
  @Implementation(minSdk = JELLY_BEAN_MR2)
  protected static void beginSection(String sectionName) {
    if (!Trace.isTagEnabled(TRACE_TAG_APP)) {
      return;
    }
    if (!checkValidSectionName(sectionName)) {
      return;
    }
    currentSections.get().addFirst(sectionName);
  }

  /** Ends the most recent active trace section. */
  @Implementation(minSdk = JELLY_BEAN_MR2)
  protected static void endSection() {
    if (!Trace.isTagEnabled(TRACE_TAG_APP)) {
      return;
    }
    if (currentSections.get().isEmpty()) {
      Log.e(TAG, "Trying to end a trace section that was never started");
      return;
    }
    previousSections.get().offer(currentSections.get().removeFirst());
  }

  /** Starts a new async trace section with given name. */
  @Implementation(minSdk = Q)
  protected static synchronized void beginAsyncSection(String sectionName, int cookie) {
    if (!Trace.isTagEnabled(TRACE_TAG_APP)) {
      return;
    }
    if (!checkValidSectionName(sectionName)) {
      return;
    }
    AsyncTraceSection newSection =
        AsyncTraceSection.newBuilder().setSectionName(sectionName).setCookie(cookie).build();
    if (currentAsyncSections.contains(newSection)) {
      if (crashOnIncorrectUsage) {
        throw new IllegalStateException("Section is already running");
      }
      Log.w(TAG, "Section is already running");
      return;
    }
    currentAsyncSections.add(newSection);
  }

  /** Ends async trace trace section. */
  @Implementation(minSdk = Q)
  protected static synchronized void endAsyncSection(String sectionName, int cookie) {
    if (!Trace.isTagEnabled(TRACE_TAG_APP)) {
      return;
    }
    AsyncTraceSection section =
        AsyncTraceSection.newBuilder().setSectionName(sectionName).setCookie(cookie).build();
    if (!currentAsyncSections.contains(section)) {
      Log.e(TAG, "Trying to end a trace section that was never started");
      return;
    }
    currentAsyncSections.remove(section);
    previousAsyncSections.add(section);
  }

  @Implementation(minSdk = JELLY_BEAN_MR2)
  protected static boolean isTagEnabled(long traceTag) {
    if (traceTag == TRACE_TAG_APP) {
      return appTracingAllowed;
    }

    return directlyOn(Trace.class, "isTagEnabled", ClassParameter.from(long.class, traceTag));
  }

  @Implementation(minSdk = JELLY_BEAN_MR2)
  protected static void setAppTracingAllowed(boolean appTracingAllowed) {
    ShadowTrace.appTracingAllowed = appTracingAllowed;
  }

  /** Returns whether systrace is enabled. */
  @Implementation(minSdk = Q)
  protected static boolean isEnabled() {
    return isEnabled;
  }

  /** Sets the systrace to enabled or disabled. */
  public static void setEnabled(boolean enabled) {
    ShadowTrace.isEnabled = enabled;
  }

  /** Returns a stack of the currently active trace sections for the current thread. */
  public static Deque<String> getCurrentSections() {
    return new ArrayDeque<>(currentSections.get());
  }

  /** Returns a queue of all the previously active trace sections for the current thread. */
  public static Queue<String> getPreviousSections() {
    return new ArrayDeque<>(previousSections.get());
  }

  /** Returns a set of all the current active async trace sections. */
  public static ImmutableSet<AsyncTraceSection> getCurrentAsyncSections() {
    return ImmutableSet.copyOf(currentAsyncSections);
  }

  /** Returns a set of all the previously active async trace sections. */
  public static ImmutableSet<AsyncTraceSection> getPreviousAsyncSections() {
    return ImmutableSet.copyOf(previousAsyncSections);
  }

  /**
   * Do not use this method unless absolutely necessary. Prefer fixing the tests instead.
   *
   * <p>Sets whether to crash on incorrect usage (e.g., calling {@link #endSection()} before {@link
   * beginSection(String)}. Default value - {@code true}.
   */
  public static void doNotUseSetCrashOnIncorrectUsage(boolean crashOnIncorrectUsage) {
    ShadowTrace.crashOnIncorrectUsage = crashOnIncorrectUsage;
  }

  private static boolean checkValidSectionName(String sectionName) {
    if (sectionName == null) {
      if (crashOnIncorrectUsage) {
        throw new NullPointerException("sectionName cannot be null");
      }
      Log.w(TAG, "Section name cannot be null");
      return false;
    } else if (sectionName.length() > MAX_SECTION_NAME_LEN) {
      if (crashOnIncorrectUsage) {
        throw new IllegalArgumentException("sectionName is too long");
      }
      Log.w(TAG, "Section name is too long");
      return false;
    }
    return true;
  }

  /** Resets internal lists of active trace sections. */
  @Resetter
  public static void reset() {
    // TODO: clear sections from other threads
    currentSections.get().clear();
    previousSections.get().clear();
    currentAsyncSections.clear();
    previousAsyncSections.clear();
    ShadowTrace.isEnabled = true;
    crashOnIncorrectUsage = CRASH_ON_INCORRECT_USAGE_DEFAULT;
  }

  /** AutoValue representation of a trace triggered by one of the async apis */
  @AutoValue
  public abstract static class AsyncTraceSection {

    abstract String getSectionName();

    abstract Integer getCookie();

    static Builder newBuilder() {
      return new AutoValue_ShadowTrace_AsyncTraceSection.Builder();
    }

    /** Builder for traces triggered by one of the async apis */
    @AutoValue.Builder()
    public abstract static class Builder {
      abstract Builder setSectionName(String sectionName);

      abstract Builder setCookie(Integer cookie);

      abstract AsyncTraceSection build();
    }
  }
}

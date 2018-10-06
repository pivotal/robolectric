package org.robolectric.shadows;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import static org.robolectric.Shadows.shadowOf;

/**
 * Robolectric places posted {@link Runnable}s into a queue instead of sending them to be handled on a
 * separate thread. {@link Runnable}s that are scheduled to be executed immediately can be triggered by calling
 * {@link #idleMainLooper()}.
 *
 * todo: add utility method to advance time and trigger execution of Runnables scheduled for a time in the future
 * 
 * @deprecated There is no special shadow implementation for the {@link android.os.Handler} class. The special
 * handling is all done by {@link ShadowLooper} and {@link ShadowMessageQueue}. This class has been retained
 * for backward compatibility with the various static method implementations.
 */
// <b>Note</b>: If this shadow is ever completely removed it will still probably make sense to keep
// the associated tests - if necessary we can copy them into ShadowLooperTest or ShadowMessageQueueTest.
@Deprecated
// Even though it doesn't implement anything, some parts of the system will fail if we don't have the
// @Implements tag (ShadowWrangler).
@Implements(Handler.class)
public class ShadowHandler {

  @RealObject
  private Handler realObject;

  /**
   * @deprecated use {@link ShadowLooper#idleMainLooper()} instead
   */
  @Deprecated
  public static void flush() {
    idleMainLooper();
  }

  /**
   * @deprecated
   * @see org.robolectric.shadows.ShadowLooper#idleMainLooper()
   */
  @Deprecated
  public static void idleMainLooper() {
    ShadowLooper.idleMainLooper();
  }

  /**
   * @deprecated
   * @see ShadowLooper#runUiThreadTasksIncludingDelayedTasks()
   */
  @Deprecated
  public static void runMainLooperToEndOfTasks() {
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
  }

  /**
   * @deprecated
   * @see ShadowLooper#runMainLooperOneTask() ()
   */
  @Deprecated
  public static void runMainLooperOneTask() {
    ShadowLooper shadowLooper = Shadow.extract(Looper.myLooper());
    shadowLooper.runOneTask();
  }

  /**
   * @deprecated
   * @see ShadowLooper#runMainLooperToNextTask() ()
   */
  @Deprecated
  public static void runMainLooperToNextTask() {
    ShadowLooper shadowLooper = Shadow.extract(Looper.myLooper());
    shadowLooper.runToNextTask();
  }

  @Implementation
  protected final boolean sendMessageDelayed(Message msg, long delayMillis) {
    if(delayMillis < 0L) {
      delayMillis = 0L;
    }

    return realObject.sendMessageAtTime(msg, shadowOf(realObject.getLooper().getQueue()).getScheduler().getCurrentTime() + delayMillis);
  }
}

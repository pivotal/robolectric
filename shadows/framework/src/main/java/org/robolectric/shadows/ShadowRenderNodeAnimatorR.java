package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.R;
import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.graphics.animation.RenderNodeAnimator;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;

/**
 * Copy of ShadowRenderNodeAnimator that reflects move of RenderNodeAnimator to android.graphics in
 * R
 */
@Implements(value = RenderNodeAnimator.class, minSdk = R, isInAndroidSdk = false)
public class ShadowRenderNodeAnimatorR {
  private static final int STATE_FINISHED = 3;

  @RealObject RenderNodeAnimator realObject;
  private boolean scheduled = false;
  private long startTime = -1;
  private boolean isEnding = false;

  @Resetter
  public static void reset() {
    // sAnimationHelper is a static field used for processing delayed animations. Since it registers
    // callbacks on the Choreographer, this is a problem if not reset between tests (as once the
    // test is complete, its scheduled callbacks would be removed, but the static object would still
    // believe it was registered and not re-register for the next test).
    ReflectionHelpers.setStaticField(
        RenderNodeAnimator.class, "sAnimationHelper", new ThreadLocal<>());
  }

  public void moveToRunningState() {
    directlyOn(realObject, RenderNodeAnimator.class, "moveToRunningState");
    if (!isEnding) {
      // Only schedule if this wasn't called during an end() call, as Robolectric will run any
      // Choreographer callbacks synchronously when unpaused (and thus end up running the full
      // animation even though RenderNodeAnimator just wanted to kick it into STATE_STARTED).
      schedule();
    }
  }

  @Implementation
  public void doStart() {
    directlyOn(realObject, RenderNodeAnimator.class, "doStart");
    schedule();
  }

  @Implementation
  public void cancel() {
    directlyOn(realObject, RenderNodeAnimator.class).cancel();

    int state = ReflectionHelpers.getField(realObject, "mState");
    if (state != STATE_FINISHED) {
      // In 21, RenderNodeAnimator only calls nEnd, it doesn't call the Java end method. Thus, it
      // expects the native code will end up calling onFinished, so we do that here.
      directlyOn(realObject, RenderNodeAnimator.class, "onFinished");
    }
  }

  @Implementation
  public void end() {
    // Set this to true to prevent us from scheduling and running the full animation on the end()
    // call. This can happen if the animation had not been started yet.
    isEnding = true;
    directlyOn(realObject, RenderNodeAnimator.class).end();
    isEnding = false;
    unschedule();

    int state = ReflectionHelpers.getField(realObject, "mState");
    if (state != STATE_FINISHED) {
      // This means that the RenderNodeAnimator called out to native code to finish the animation,
      // expecting that it would end up calling onFinished. Since that won't happen in Robolectric,
      // we call onFinished ourselves.
      directlyOn(realObject, RenderNodeAnimator.class, "onFinished");
    }
  }

  private void schedule() {
    if (!scheduled) {
      scheduled = true;
      Choreographer.getInstance().postFrameCallback(frameCallback);
    }
  }

  private void unschedule() {
    if (scheduled) {
      Choreographer.getInstance().removeFrameCallback(frameCallback);
      scheduled = false;
    }
  }

  private final FrameCallback frameCallback =
      new FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
          scheduled = false;
          if (startTime == -1) {
            startTime = frameTimeNanos;
          }

          long duration = realObject.getDuration();
          long curTime = frameTimeNanos - startTime;
          if (curTime >= duration) {
            directlyOn(realObject, RenderNodeAnimator.class, "onFinished");
          } else {
            schedule();
          }
        }
      };
}

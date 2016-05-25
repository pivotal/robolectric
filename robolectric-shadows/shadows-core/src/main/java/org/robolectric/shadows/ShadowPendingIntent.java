package org.robolectric.shadows;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.fakes.RoboIntentSender;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Shadow for {@code android.app.PendingIntent}.
 */
@Implements(PendingIntent.class)
public class ShadowPendingIntent {
  private static final List<PendingIntent> createdIntents = new ArrayList<>();

  @RealObject
  PendingIntent realPendingIntent;

  private Intent[] savedIntents;
  private Context savedContext;
  private boolean isActivityIntent;
  private boolean isBroadcastIntent;
  private boolean isServiceIntent;
  private int requestCode;
  private int flags;

  @Implementation
  public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
    return create(context, intent == null ? null : new Intent[] {intent}, true, false, false, requestCode, flags);
  }

  @Implementation
  public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags, Bundle options) {
    return create(context, intent == null ? null : new Intent[] {intent}, true, false, false, requestCode, flags);
  }

  @Implementation
  public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags) {
    return create(context, intents, true, false, false, requestCode, flags);
  }

  @Implementation
  public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags, Bundle options) {
    return create(context, intents, true, false, false, requestCode, flags);
  }

  @Implementation
  public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
    return create(context, intent == null ? null : new Intent[] {intent}, false, true, false, requestCode, flags);
  }

  @Implementation
  public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
    return create(context, intent == null ? null : new Intent[] {intent}, false, false, true, requestCode, flags);
  }

  @Implementation
  public void cancel() {
    Iterator<PendingIntent> iterator = createdIntents.iterator();
    while (iterator.hasNext()) {
      ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(iterator.next());
      if (shadowPendingIntent == this) {
        iterator.remove();
        break;
      }
    }
  }

  @Implementation
  public void send() throws CanceledException {
    send(savedContext, 0, null);
  }

  @Implementation
  public void send(Context context, int code, Intent intent) throws CanceledException {
    if (intent != null) {
      for (Intent savedIntent : savedIntents) {
        savedIntent.fillIn(intent, 0);
      }
    }

    if (isActivityIntent) {
      for (Intent savedIntent : savedIntents) {
        context.startActivity(savedIntent);
      }
    } else if (isBroadcastIntent) {
      for (Intent savedIntent : savedIntents) {
        context.sendBroadcast(savedIntent);
      }
    } else if (isServiceIntent) {
      for (Intent savedIntent : savedIntents) {
        context.startService(savedIntent);
      }
    }
  }

  @Implementation
  public IntentSender getIntentSender() {
    return new RoboIntentSender(realPendingIntent);
  }

  public boolean isActivityIntent() {
    return isActivityIntent;
  }

  public boolean isBroadcastIntent() {
    return isBroadcastIntent;
  }

  public boolean isServiceIntent() {
    return isServiceIntent;
  }

  public Context getSavedContext() {
    return savedContext;
  }

  public Intent getSavedIntent() {
    return savedIntents[0];
  }

  public Intent[] getSavedIntents() {
    return savedIntents;
  }

  public int getRequestCode() {
    return requestCode;
  }

  public int getFlags() {
    return flags;
  }

  @Override
  @Implementation
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ShadowPendingIntent that = (ShadowPendingIntent) o;

    if (savedContext != null) {
      String packageName = savedContext.getPackageName();
      String thatPackageName = that.savedContext.getPackageName();
      if (packageName != null ? !packageName.equals(thatPackageName) : thatPackageName != null) return false;
    } else {
      if (that.savedContext != null) return false;
    }
    if (savedIntents != null) {
      if (!Arrays.equals(this.savedIntents, that.savedIntents)) return false;
    }
    return true;
  }

  @Override
  @Implementation
  public int hashCode() {
    int result = savedIntents != null ? Arrays.hashCode(savedIntents) : 0;
    if (savedContext != null) {
      String packageName = savedContext.getPackageName();
      result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
    }
    return result;
  }

  private static PendingIntent create(Context context, Intent[] intents, boolean isActivity, boolean isBroadcast, boolean isService, int requestCode, int flags) {
    if ((flags & PendingIntent.FLAG_UPDATE_CURRENT) != 0) {
      PendingIntent createdIntent = getCreatedIntentFor(isActivity, isBroadcast, isService, requestCode);
      if (createdIntent != null) {
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(createdIntent);
        shadowPendingIntent.savedIntents = intents;
        return createdIntent;
      }
    }

    PendingIntent createdIntent = getCreatedIntentFor(intents, isActivity, isBroadcast, isService, requestCode);
    if ((flags & PendingIntent.FLAG_NO_CREATE) != 0) {
      return createdIntent;
    }
    if (createdIntent != null) {
      if ((flags & PendingIntent.FLAG_CANCEL_CURRENT) != 0) {
        createdIntent.cancel();
      } else {
        return createdIntent;
      }
    }

    PendingIntent pendingIntent = ReflectionHelpers.callConstructor(PendingIntent.class);
    ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(pendingIntent);
    shadowPendingIntent.savedIntents = intents;
    shadowPendingIntent.isActivityIntent = isActivity;
    shadowPendingIntent.isBroadcastIntent = isBroadcast;
    shadowPendingIntent.isServiceIntent = isService;
    shadowPendingIntent.savedContext = context;
    shadowPendingIntent.requestCode = requestCode;
    shadowPendingIntent.flags = flags;

    createdIntents.add(pendingIntent);
    return pendingIntent;
  }

  private static PendingIntent getCreatedIntentFor(Intent[] intents, boolean isActivity, boolean isBroadcast, boolean isService, int requestCode) {
    for (PendingIntent createdIntent : createdIntents) {
      ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(createdIntent);
      boolean bothNull = (shadowPendingIntent.savedIntents == null) && (intents == null);
      boolean sameLength = (shadowPendingIntent.savedIntents != null) && (intents != null) &&
          (shadowPendingIntent.savedIntents.length == intents.length);
      if (!bothNull && !sameLength) {
        continue;
      }

      // Order matters in the framework. If I call getActivities(Activity1, Activity2), that will
      // give me a different PendingIntent than if I call getActivities(Activity2, Activity1).
      boolean equalIntents = true;
      if (intents != null) {
        for (int i = 0; i < intents.length; i++) {
          if (!shadowPendingIntent.savedIntents[i].filterEquals(intents[i])) {
            equalIntents = false;
            break;
          }
        }
      }

      if (equalIntents && (shadowPendingIntent.getRequestCode() == requestCode) &&
          ((isActivity && shadowPendingIntent.isActivityIntent()) ||
            (isBroadcast && shadowPendingIntent.isBroadcastIntent()) ||
            (isService && shadowPendingIntent.isServiceIntent()))) {
        return createdIntent;
      }
    }
    return null;
  }

  private static PendingIntent getCreatedIntentFor(boolean isActivity, boolean isBroadcast, boolean isService, int requestCode) {
    for (PendingIntent createdIntent : createdIntents) {
      ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(createdIntent);
      if ((shadowPendingIntent.getRequestCode() == requestCode) &&
          ((isActivity && shadowPendingIntent.isActivityIntent()) ||
            (isBroadcast && shadowPendingIntent.isBroadcastIntent()) ||
            (isService && shadowPendingIntent.isServiceIntent()))) {
        return createdIntent;
      }
    }
    return null;
  }

  @Resetter
  public static void reset() {
    createdIntents.clear();
  }
}

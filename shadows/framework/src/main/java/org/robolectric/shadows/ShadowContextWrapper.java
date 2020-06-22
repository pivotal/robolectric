package org.robolectric.shadows;

import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.UserHandle;
import java.util.List;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity.IntentForResult;
import org.robolectric.shadows.ShadowInstrumentation.StartActivityListener;

@Implements(ContextWrapper.class)
public class ShadowContextWrapper {

  @RealObject
  private ContextWrapper realContextWrapper;

  /** Returns the broadcast intents sent during the tests (for all users). */
  public List<Intent> getBroadcastIntents() {
    return getShadowInstrumentation().getBroadcastIntents();
  }

  /** Returns the broadcast intents sent to the given user. */
  public List<Intent> getBroadcastIntentsForUser(UserHandle userHandle) {
    return getShadowInstrumentation().getBroadcastIntentsForUser(userHandle);
  }

  /** Clears the broadcast intents sent during the tests (for all users). */
  public void clearBroadcastIntents() {
    getShadowInstrumentation().clearBroadcastIntents();
  }

  /**
   * Registers a {@link StartActivityListener} that gets notified when the code under test requests
   * to start an Activity.
   */
  public void addStartActivityListener(StartActivityListener listener) {
    getShadowInstrumentation().addStartActivityListener(listener);
  }

  /**
   * Unregisters a {@link StartActivityListener} previously registered via method {@link
   * #addStartActivityListener(StartActivityListener)}.
   */
  public void removeStartActivityListener(StartActivityListener listener) {
    getShadowInstrumentation().removeStartActivityListener(listener);
  }

  /**
   * Consumes the most recent {@code Intent} started by {@link
   * ContextWrapper#startActivity(android.content.Intent)} and returns it.
   *
   * @return the most recently started {@code Intent}
   */
  public Intent getNextStartedActivity() {
    return getShadowInstrumentation().getNextStartedActivity();
  }

  /**
   * Returns the most recent {@code Intent} started by {@link
   * ContextWrapper#startActivity(android.content.Intent)} without consuming it.
   *
   * @return the most recently started {@code Intent}
   */
  public Intent peekNextStartedActivity() {
    return getShadowInstrumentation().peekNextStartedActivity();
  }

  /**
   * Clears all {@code Intent}s started by {@link
   * ContextWrapper#startActivity(android.content.Intent)}.
   */
  public void clearNextStartedActivities() {
    getShadowInstrumentation().clearNextStartedActivities();
  }

  /**
   * Consumes the most recent {@code IntentForResult} started by {@link *
   * ContextWrapper#startActivity(android.content.Intent, android.os.Bundle)} and returns it.
   *
   * @return the most recently started {@code IntentForResult}
   */
  public IntentForResult getNextStartedActivityForResult() {
    return getShadowInstrumentation().getNextStartedActivityForResult();
  }

  /**
   * Returns the most recent {@code IntentForResult} started by {@link
   * ContextWrapper#startActivity(android.content.Intent, android.os.Bundle)} without consuming it.
   *
   * @return the most recently started {@code IntentForResult}
   */
  public IntentForResult peekNextStartedActivityForResult() {
    return getShadowInstrumentation().peekNextStartedActivityForResult();
  }

  /**
   * Consumes the most recent {@code Intent} started by {@link
   * android.content.Context#startService(android.content.Intent)} and returns it.
   *
   * @return the most recently started {@code Intent}
   */
  public Intent getNextStartedService() {
    return getShadowInstrumentation().getNextStartedService();
  }

  /**
   * Returns the most recent {@code Intent} started by
   * {@link android.content.Context#startService(android.content.Intent)} without consuming it.
   *
   * @return the most recently started {@code Intent}
   */
  public Intent peekNextStartedService() {
    return getShadowInstrumentation().peekNextStartedService();
  }

  /**
   * Clears all {@code Intent} started by
   * {@link android.content.Context#startService(android.content.Intent)}.
   */
  public void clearStartedServices() {
    getShadowInstrumentation().clearStartedServices();
  }

  /**
   * Consumes the {@code Intent} requested to stop a service by
   * {@link android.content.Context#stopService(android.content.Intent)}
   * from the bottom of the stack of stop requests.
   */
  public Intent getNextStoppedService() {
    return getShadowInstrumentation().getNextStoppedService();
  }

  /** Grant the given permissions for the current process and user. */
  public void grantPermissions(String... permissionNames) {
    getShadowInstrumentation().grantPermissions(permissionNames);
  }

  /** Grant the given permissions for the given process and user. */
  public void grantPermissions(int pid, int uid, String... permissions) {
    getShadowInstrumentation().grantPermissions(pid, uid, permissions);
  }

  /**
   * Revoke the given permissions for the current process and user.
   *
   * Has no effect if permissions were not previously granted.
   */
  public void denyPermissions(String... permissionNames) {
    getShadowInstrumentation().denyPermissions(permissionNames);
  }

  /** Revoke the given permissions for the given process and user. */
  public void denyPermissions(int pid, int uid, String... permissions) {
    getShadowInstrumentation().denyPermissions(pid, uid, permissions);
  }

  ShadowInstrumentation getShadowInstrumentation() {
    ActivityThread activityThread = (ActivityThread) RuntimeEnvironment.getActivityThread();
    return Shadow.extract(activityThread.getInstrumentation());
  }

  /**
   * Makes {@link Context#getSystemService(String)} return {@code null} for the given system service
   * name, mimicking a device that doesn't have that system service.
   */
  public void removeSystemService(String name) {
    ((ShadowContextImpl) Shadow.extract(realContextWrapper.getBaseContext()))
        .removeSystemService(name);
  }
}

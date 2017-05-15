package org.robolectric.shadows;

import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION_CODES;
import static android.os.Build.VERSION_CODES.*;

@Implements(value = UserManager.class, minSdk = JELLY_BEAN_MR1)
public class ShadowUserManager {

  private boolean userUnlocked = true;
  private Map<UserHandle, Bundle> userRestrictions = new HashMap<>();

  @Implementation(minSdk = JELLY_BEAN_MR2)
  public Bundle getApplicationRestrictions(String packageName) {
    return null;
  }

  @Implementation(minSdk = LOLLIPOP)
  public List<UserHandle> getUserProfiles(){
    return Collections.emptyList();
  }

  @Implementation(minSdk = N)
  public boolean isUserUnlocked() {
    return userUnlocked;
  }

  /**
   * Setter for {@link UserManager#isUserUnlocked()}
   */
  public void setUserUnlocked(boolean userUnlocked) {
    this.userUnlocked = userUnlocked;
  }


  @Implementation(minSdk = LOLLIPOP)
  public boolean hasUserRestriction(String restrictionKey, UserHandle userHandle) {
    Bundle bundle = userRestrictions.get(userHandle);
    return bundle != null && bundle.getBoolean(restrictionKey);
  }

  public void setUserRestriction(UserHandle userHandle, String restrictionKey, boolean value) {
    Bundle bundle = getUserRestrictionsForUser(userHandle);
    bundle.putBoolean(restrictionKey, value);
  }

  @Implementation(minSdk = JELLY_BEAN_MR2)
  public Bundle getUserRestrictions(UserHandle userHandle) {
    return getUserRestrictionsForUser(userHandle);
  }

  private Bundle getUserRestrictionsForUser(UserHandle userHandle) {
    Bundle bundle = userRestrictions.get(userHandle);
    if (bundle == null) {
      bundle = new Bundle();
      userRestrictions.put(userHandle, bundle);
    }
    return bundle;
  }
}
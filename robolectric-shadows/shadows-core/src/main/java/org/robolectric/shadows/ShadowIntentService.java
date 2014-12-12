package org.robolectric.shadows;

import android.app.IntentService;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import static org.robolectric.internal.Shadow.directlyOn;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.*;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(IntentService.class)
public class ShadowIntentService extends ShadowService {
  @RealObject
  IntentService realIntentService;
  private boolean mRedelivery;

  public boolean getIntentRedelivery() {
    return mRedelivery;
  }

  @Implementation
  public void setIntentRedelivery(boolean enabled) {
    mRedelivery = enabled;
    directlyOn(realIntentService, IntentService.class, "setIntentRedelivery", from(enabled));
  }
}

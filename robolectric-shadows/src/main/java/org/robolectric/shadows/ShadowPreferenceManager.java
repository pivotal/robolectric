package org.robolectric.shadows;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.robolectric.Shadows.shadowOf;

/**
 * Shadow for {@code PreferenceManager} that returns instances of the
 * {@link org.robolectric.tester.android.content.TestSharedPreferences} utility class
 */
@Implements(PreferenceManager.class)
public class ShadowPreferenceManager {

  @Implementation
  public static SharedPreferences getDefaultSharedPreferences(Context context) {
    ShadowApplication shadowApplication = Shadows.shadowOf((Application) context.getApplicationContext());
    return shadowApplication.getSharedPreferences("__default__", Context.MODE_PRIVATE);
  }
}

package org.robolectric.shadows;

import android.os.Build;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;

@Implements(className = "android.app.SystemServiceRegistry", isInAndroidSdk = false, minSdk = Build.VERSION_CODES.M)
public class ShadowSystemServiceRegistry {

    @Resetter
    public static void reset() {
        HashMap<String, Object> fetchers = ReflectionHelpers.getStaticField(classForName(
                "android.app.SystemServiceRegistry"),
                "SYSTEM_SERVICE_FETCHERS");


        Class staticApplicationServiceFetcherClass = null;
        if (RuntimeEnvironment.getApiLevel() >= Build.VERSION_CODES.N) {
            staticApplicationServiceFetcherClass = classForName("android.app.SystemServiceRegistry$StaticApplicationContextServiceFetcher");
        } else if (RuntimeEnvironment.getApiLevel() == Build.VERSION_CODES.M) {
            staticApplicationServiceFetcherClass = classForName("android.app.SystemServiceRegistry$StaticOuterContextServiceFetcher");
        }

        Class staticServiceFetcherClass = classForName("android.app.SystemServiceRegistry$StaticServiceFetcher");

        for (Object o : fetchers.values()) {
          if (staticApplicationServiceFetcherClass.isInstance(o)) {
            ReflectionHelpers.setField(staticApplicationServiceFetcherClass, o, "mCachedInstance", null);
          } else if (staticServiceFetcherClass.isInstance(o)) {
            ReflectionHelpers.setField(staticServiceFetcherClass, o, "mCachedInstance", null);
          }
        }
    }

    private static Class classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

package org.robolectric.shadows;

import android.content.Context;
import android.os.Environment;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.res.ResName;
import org.robolectric.res.ResourceLoader;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.io.File;

import static org.robolectric.Shadows.shadowOf;

/**
 * Shadow for {@link android.content.Context}.
 */
@SuppressWarnings({"UnusedDeclaration"})
@Implements(Context.class)
abstract public class ShadowContext {
  @RealObject private Context realContext;

  @Implementation
  public File getExternalCacheDir() {
    return Environment.getExternalStorageDirectory();
  }

  @Implementation
  public File getExternalFilesDir(String type) {
    return Environment.getExternalStoragePublicDirectory(type);
  }

  /**
   * Non-Android accessor.
   * Deprecated. Instead call through {@link ShadowAssetManager#getResourceLoader()};
   *
   * @return the {@code ResourceLoader} associated with this {@code Context}
   */
  @Deprecated
  public ResourceLoader getResourceLoader() {
    return shadowOf(realContext.getAssets()).getResourceLoader();
  }

  public ResName getResName(int resourceId) {
    return shadowOf(realContext.getAssets()).getResourceLoader().getResourceIndex().getResName(resourceId);
  }

  public void callAttachBaseContext(Context context) {
    ReflectionHelpers.callInstanceMethod(realContext, "attachBaseContext", ClassParameter.from(Context.class, context));
  }
}

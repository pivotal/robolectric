package org.robolectric.shadows;

import android.content.Intent;

import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/**
 * Shadow for {@link android.content.Intent}.
 */
@SuppressWarnings({"UnusedDeclaration"})
@Implements(Intent.class)
public class ShadowIntent {
  @RealObject private Intent realIntent;

  /**
   * Non-Android accessor that returns the {@code Class} object set by
   * {@link Intent#setClass(android.content.Context, Class)}
   *
   * @return the {@code Class} object set by
   *         {@link Intent#setClass(android.content.Context, Class)}
   */
  public Class<?> getIntentClass() {
    try {
      return Class.forName(realIntent.getComponent().getClassName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}

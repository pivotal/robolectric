package org.robolectric.shadow.api;

public interface ShadowPicker<T> {

  /**
   * Determines the shadow class to be used depending on the configuration of the
   * {@link org.robolectric.internal.Environment}. Must be deterministic.
   *
   * @return the shadow class to be used
   */
  Class<? extends T> pickShadowClass();
}

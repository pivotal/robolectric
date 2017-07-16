package org.robolectric;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;

import org.robolectric.manifest.AndroidManifest;
import org.robolectric.util.Scheduler;

/**
 * Interface between robolectric and shadows-core modules.
 */
public interface ShadowsAdapter {
  Scheduler getBackgroundScheduler();

  ShadowLooperAdapter getMainLooper();

  String getShadowActivityThreadClassName();

  void setupLogging();

  String getShadowContextImplClassName();

  void bind(Application application, AndroidManifest appManifest);

  interface ShadowLooperAdapter {
    void runPaused(Runnable runnable);
  }
}

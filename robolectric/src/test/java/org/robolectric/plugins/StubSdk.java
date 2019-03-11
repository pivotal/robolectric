package org.robolectric.plugins;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.robolectric.pluginapi.Sdk;

/** Stub SDK */
public class StubSdk extends Sdk {

  private final boolean isSupported;

  public StubSdk(int apiLevel, boolean isSupported) {
    super(apiLevel);
    this.isSupported = isSupported;
  }

  @Override
  public String getAndroidVersion() {
    return null;
  }

  @Override
  public String getAndroidCodeName() {
    return null;
  }

  @Override
  public Path getJarPath() {
    return Paths.get("fake/path-" + getApiLevel() + ".jar");
  }

  @Override
  public boolean isSupported() {
    return isSupported;
  }

  @Override
  public String getUnsupportedMessage() {
    return "unsupported";
  }

  @Override
  public boolean isKnown() {
    return true;
  }
}

package org.robolectric.internal;

import org.robolectric.ApkLoader;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;
import org.robolectric.internal.dependency.DependencyResolver;

public interface SandboxFactory {
  AndroidSandbox getSandbox(
      InstrumentationConfiguration instrumentationConfig, SdkConfig sdkConfig,
      boolean useLegacyResources, DependencyResolver dependencyResolver,
      ApkLoader apkLoader);
}

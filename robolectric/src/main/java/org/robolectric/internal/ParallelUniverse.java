package org.robolectric.internal;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.ResBunch;
import org.robolectric.res.ResourceLoader;
import org.robolectric.res.builder.DefaultRobolectricPackageManager;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.ShadowsAdapter;

import java.lang.reflect.Method;

import static org.robolectric.util.ReflectionHelpers.ClassParameter.*;

public class ParallelUniverse implements ParallelUniverseInterface {
  private static final String DEFAULT_PACKAGE_NAME = "org.robolectric.default";
  private final RobolectricTestRunner robolectricTestRunner;
  private final ShadowsAdapter shadowsAdapter = Robolectric.getShadowsAdapter();

  private boolean loggingInitialized = false;
  private SdkConfig sdkConfig;

  public ParallelUniverse(RobolectricTestRunner robolectricTestRunner) {
    this.robolectricTestRunner = robolectricTestRunner;
  }

  @Override
  public void resetStaticState(Config config) {
    Robolectric.reset();

    if (!loggingInitialized) {
      shadowsAdapter.setupLogging();
      loggingInitialized = true;
    }
  }

  /*
   * If the Config already has a version qualifier, do nothing. Otherwise, add a version
   * qualifier for the target api level (which comes from the manifest or Config.emulateSdk()).
   */
  private String addVersionQualifierToQualifiers(String qualifiers) {
    int versionQualifierApiLevel = ResBunch.getVersionQualifierApiLevel(qualifiers);
    if (versionQualifierApiLevel == -1) {
      if (qualifiers.length() > 0) {
        qualifiers += "-";
      }
      qualifiers += "v" + sdkConfig.getApiLevel();
    }
    return qualifiers;
  }

  @Override
  public void setUpApplicationState(Method method, TestLifecycle testLifecycle, ResourceLoader systemResourceLoader, AndroidManifest appManifest, Config config) {
    RuntimeEnvironment.application = null;
    RuntimeEnvironment.setRobolectricPackageManager(new DefaultRobolectricPackageManager(shadowsAdapter));
    RuntimeEnvironment.getRobolectricPackageManager().addPackage(DEFAULT_PACKAGE_NAME);
    ResourceLoader resourceLoader;
    if (appManifest != null) {
      resourceLoader = robolectricTestRunner.getAppResourceLoader(sdkConfig, systemResourceLoader, appManifest);
      RuntimeEnvironment.getRobolectricPackageManager().addManifest(appManifest, resourceLoader);
    } else {
      resourceLoader = systemResourceLoader;
    }

    shadowsAdapter.setSystemResources(systemResourceLoader);
    String qualifiers = addVersionQualifierToQualifiers(config.qualifiers());
    Resources systemResources = Resources.getSystem();
    Configuration configuration = systemResources.getConfiguration();
    shadowsAdapter.overrideQualifiers(configuration, qualifiers);
    systemResources.updateConfiguration(configuration, systemResources.getDisplayMetrics());
    RuntimeEnvironment.setQualifiers(qualifiers);

    Class<?> contextImplClass = ReflectionHelpers.loadClassReflectively(getClass().getClassLoader(), shadowsAdapter.getShadowContextImplClassName());

    Class<?> activityThreadClass = ReflectionHelpers.loadClassReflectively(getClass().getClassLoader(), shadowsAdapter.getShadowActivityThreadClassName());
    Object activityThread = ReflectionHelpers.callConstructorReflectively(activityThreadClass);
    RuntimeEnvironment.setActivityThread(activityThread);

    ReflectionHelpers.setFieldReflectively(activityThread, "mInstrumentation", new RoboInstrumentation());
    ReflectionHelpers.setFieldReflectively(activityThread, "mCompatConfiguration", configuration);

    Context systemContextImpl = ReflectionHelpers.callStaticMethodReflectively(contextImplClass, "createSystemContext", from(activityThreadClass, activityThread));

    final Application application = (Application) testLifecycle.createApplication(method, appManifest, config);
    if (application != null) {
      String packageName = appManifest != null ? appManifest.getPackageName() : null;
      if (packageName == null) packageName = DEFAULT_PACKAGE_NAME;

      ApplicationInfo applicationInfo;
      try {
        applicationInfo = RuntimeEnvironment.getPackageManager().getApplicationInfo(packageName, 0);
      } catch (PackageManager.NameNotFoundException e) {
        throw new RuntimeException(e);
      }

      Class<?> compatibilityInfoClass = ReflectionHelpers.loadClassReflectively(getClass().getClassLoader(), "android.content.res.CompatibilityInfo");

      Object loadedApk = ReflectionHelpers.callInstanceMethodReflectively(activityThread, "getPackageInfo",
          from(ApplicationInfo.class, applicationInfo),
          fromNull(compatibilityInfoClass),
          from(ClassLoader.class, getClass().getClassLoader()),
          from(false),
          from(true));

      shadowsAdapter.bind(application, appManifest, resourceLoader);
      if (appManifest == null) {
        // todo: make this cleaner...
        shadowsAdapter.setPackageName(application, applicationInfo.packageName);
      }
      Resources appResources = application.getResources();
      ReflectionHelpers.setFieldReflectively(loadedApk, "mResources", appResources);
      Context contextImpl = ReflectionHelpers.callInstanceMethodReflectively(systemContextImpl, "createPackageContext", from(applicationInfo.packageName), from(Context.CONTEXT_INCLUDE_CODE));
      ReflectionHelpers.setFieldReflectively(activityThread, "mInitialApplication", application);
      ReflectionHelpers.callInstanceMethodReflectively(application, "attach", from(Context.class, contextImpl));

      appResources.updateConfiguration(configuration, appResources.getDisplayMetrics());
      shadowsAdapter.setAssetsQualifiers(appResources.getAssets(), qualifiers);

      RuntimeEnvironment.application = application;
      application.onCreate();
    }
  }

  @Override
  public void tearDownApplication() {
    if (RuntimeEnvironment.application != null) {
      RuntimeEnvironment.application.onTerminate();
    }
  }

  @Override
  public Object getCurrentApplication() {
    return RuntimeEnvironment.application;
  }

  @Override
  public void setSdkConfig(SdkConfig sdkConfig) {
    this.sdkConfig = sdkConfig;
  }
}

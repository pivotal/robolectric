package org.robolectric.android.internal;

import static org.robolectric.Shadows.shadowOf;

import android.app.ActivityThread;
import android.app.Application;
import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.Locale;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.robolectric.internal.DeprecatedMethodMarkerException;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestLifecycle;
import org.robolectric.android.Bootstrap;
import org.robolectric.android.fakes.RoboInstrumentation;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ParallelUniverseInterface;
import org.robolectric.internal.SdkConfig;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.manifest.RoboNotFoundException;
import org.robolectric.res.ResourceTable;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.PerfStatsCollector;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import org.robolectric.util.Scheduler;
import org.robolectric.util.TempDirectory;

public class ParallelUniverse implements ParallelUniverseInterface {

  private boolean loggingInitialized = false;
  private SdkConfig sdkConfig;

  @Override
  public void resetStaticState(Config config) {
    RuntimeEnvironment.setMainThread(Thread.currentThread());
    Robolectric.reset();

    if (!loggingInitialized) {
      ShadowLog.setupLogging();
      loggingInitialized = true;
    }
  }

  @Override
  public void setUpApplicationState(Method method, TestLifecycle testLifecycle, AndroidManifest appManifest,
                                    Config config, ResourceTable compileTimeResourceTable,
                                    ResourceTable appResourceTable,
                                    ResourceTable systemResourceTable) {
    ReflectionHelpers.setStaticField(RuntimeEnvironment.class, "apiLevel", sdkConfig.getApiLevel());

    RuntimeEnvironment.application = null;
    RuntimeEnvironment.setTempDirectory(new TempDirectory(createTestDataDirRootPath(method)));
    RuntimeEnvironment.setMasterScheduler(new Scheduler());
    RuntimeEnvironment.setMainThread(Thread.currentThread());

    RuntimeEnvironment.setCompileTimeResourceTable(compileTimeResourceTable);
    RuntimeEnvironment.setAppResourceTable(appResourceTable);
    RuntimeEnvironment.setSystemResourceTable(systemResourceTable);

    try {
      appManifest.initMetaData(appResourceTable);
    } catch (RoboNotFoundException e1) {
      throw new Resources.NotFoundException(e1.getMessage());
    }
    RuntimeEnvironment.setApplicationManifest(appManifest);

    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    Configuration configuration = new Configuration();
    DisplayMetrics displayMetrics = new DisplayMetrics();

    Bootstrap.applyQualifiers(config.qualifiers(), sdkConfig.getApiLevel(), configuration,
        displayMetrics);

    Locale locale = sdkConfig.getApiLevel() >= VERSION_CODES.N
        ? configuration.getLocales().get(0)
        : configuration.locale;
    Locale.setDefault(locale);

    Class<?> contextImplClass = ReflectionHelpers.loadClass(getClass().getClassLoader(), ShadowContextImpl.CLASS_NAME);

    // Looper needs to be prepared before the activity thread is created
    if (Looper.myLooper() == null) {
      Looper.prepareMainLooper();
    }
    ShadowLooper.getShadowMainLooper().resetScheduler();
    ActivityThread activityThread = ReflectionHelpers.newInstance(ActivityThread.class);
    RuntimeEnvironment.setActivityThread(activityThread);

    RoboInstrumentation androidInstrumentation = new RoboInstrumentation();
    ReflectionHelpers.setField(activityThread, "mInstrumentation", androidInstrumentation);
    ReflectionHelpers.setField(activityThread, "mCompatConfiguration", configuration);
    ReflectionHelpers.setStaticField(ActivityThread.class, "sMainThreadHandler", new Handler(Looper.myLooper()));

    Bootstrap.setUpDisplay(configuration, displayMetrics);

    Resources systemResources = Resources.getSystem();
    systemResources.updateConfiguration(configuration, displayMetrics);

    Context systemContextImpl = ReflectionHelpers.callStaticMethod(contextImplClass, "createSystemContext", ClassParameter.from(ActivityThread.class, activityThread));
    RuntimeEnvironment.systemContext = systemContextImpl;

    final ApplicationInfo applicationInfo;
    try {
      applicationInfo = systemContextImpl.getPackageManager().getApplicationInfo(appManifest.getPackageName(), 0);
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }

    Application application;
    try {
      application = (Application) testLifecycle.createApplication(method, appManifest, config);
      System.err.println("TestLifecycle.createApplication(Method, AndroidManifest, Config) is deprecated, please reimplement!");
    } catch (DeprecatedMethodMarkerException e) {
      application = (Application) testLifecycle.createApplication(method, applicationInfo, config);
    }
    RuntimeEnvironment.application = application;

    if (application != null) {
      shadowOf(application).bind(appManifest);

      final Class<?> appBindDataClass;
      try {
        appBindDataClass = Class.forName("android.app.ActivityThread$AppBindData");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      Object data = ReflectionHelpers.newInstance(appBindDataClass);
      ReflectionHelpers.setField(data, "processName", "org.robolectric");
      ReflectionHelpers.setField(data, "appInfo", applicationInfo);
      ReflectionHelpers.setField(activityThread, "mBoundApplication", data);

      LoadedApk loadedApk = activityThread.getPackageInfo(applicationInfo, null, Context.CONTEXT_INCLUDE_CODE);

      try {
        Context contextImpl = systemContextImpl.createPackageContext(applicationInfo.packageName, Context.CONTEXT_INCLUDE_CODE);
        ReflectionHelpers.setField(ActivityThread.class, activityThread, "mInitialApplication", application);
        shadowOf(application).callAttach(contextImpl);
      } catch (PackageManager.NameNotFoundException e) {
        throw new RuntimeException(e);
      }

      Resources appResources = application.getResources();
      ReflectionHelpers.setField(loadedApk, "mResources", appResources);
      ReflectionHelpers.setField(loadedApk, "mApplication", application);

      appResources.updateConfiguration(configuration, displayMetrics);

      initInstrumentation(activityThread, androidInstrumentation, applicationInfo);

      PerfStatsCollector.getInstance().measure("application onCreate()", () -> {
        RuntimeEnvironment.application.onCreate();
      });
    }
  }

  private void initInstrumentation(
      ActivityThread activityThread,
      RoboInstrumentation androidInstrumentation,
      ApplicationInfo applicationInfo) {
    final ComponentName component =
        new ComponentName(
            applicationInfo.packageName, androidInstrumentation.getClass().getSimpleName());
    androidInstrumentation.init(
        ActivityThread.class, activityThread, RuntimeEnvironment.application, component);
    androidInstrumentation.onCreate(new Bundle());
  }

  /**
   * Create a file system safe directory path name for the current test.
   */
  private String createTestDataDirRootPath(Method method) {
    return method.getClass().getSimpleName() + "_" + method.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
  }

  @Override
  public Thread getMainThread() {
    return RuntimeEnvironment.getMainThread();
  }

  @Override
  public void setMainThread(Thread newMainThread) {
    RuntimeEnvironment.setMainThread(newMainThread);
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
    ReflectionHelpers.setStaticField(RuntimeEnvironment.class, "apiLevel", sdkConfig.getApiLevel());
  }
}

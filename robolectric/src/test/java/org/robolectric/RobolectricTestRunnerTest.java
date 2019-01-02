package org.robolectric;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import android.app.Application;
import android.os.Build;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner.ResourcesMode;
import org.robolectric.RobolectricTestRunner.RobolectricFrameworkMethod;
import org.robolectric.RobolectricTestRunnerTest.TestWithBrokenAppCreate.MyTestApplication;
import org.robolectric.annotation.Config;
import org.robolectric.internal.AndroidSandbox;
import org.robolectric.internal.Bridge;
import org.robolectric.internal.DefaultSandboxFactory;
import org.robolectric.internal.DefaultSdkProvider;
import org.robolectric.internal.SandboxFactory;
import org.robolectric.internal.SdkConfig;
import org.robolectric.internal.dependency.DependencyResolver;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.pluginapi.perf.Metric;
import org.robolectric.pluginapi.perf.PerfStatsReporter;
import org.robolectric.util.TempDirectory;
import org.robolectric.util.TestUtil;
import org.robolectric.util.inject.Injector;

@RunWith(JUnit4.class)
public class RobolectricTestRunnerTest {

  private RunNotifier notifier;
  private List<String> events;
  private String priorEnabledSdks;
  private String priorAlwaysInclude;
  private SdkProvider sdkProvider;

  @Before
  public void setUp() throws Exception {
    notifier = new RunNotifier();
    events = new ArrayList<>();
    notifier.addListener(new RunListener() {
      @Override
      public void testIgnored(Description description) throws Exception {
        events.add("ignored: " + description.getDisplayName());
      }

      @Override
      public void testFailure(Failure failure) throws Exception {
        events.add("failure: " + failure.getMessage());
      }
    });

    priorEnabledSdks = System.getProperty("robolectric.enabledSdks");
    System.clearProperty("robolectric.enabledSdks");

    priorAlwaysInclude = System.getProperty("robolectric.alwaysIncludeVariantMarkersInTestName");
    System.clearProperty("robolectric.alwaysIncludeVariantMarkersInTestName");

    sdkProvider = new DefaultSdkProvider();
  }

  @After
  public void tearDown() throws Exception {
    TestUtil.resetSystemProperty(
        "robolectric.alwaysIncludeVariantMarkersInTestName", priorAlwaysInclude);
    TestUtil.resetSystemProperty("robolectric.enabledSdks", priorEnabledSdks);
  }

  @Test
  public void ignoredTestCanSpecifyUnsupportedSdkWithoutExploding() throws Exception {
    RobolectricTestRunner runner = new SingleSdkRobolectricTestRunner(TestWithOldSdk.class);
    runner.run(notifier);
    assertThat(events).containsExactly(
        "failure: Robolectric does not support API level 11.",
        "ignored: ignoredOldSdkMethod(org.robolectric.RobolectricTestRunnerTest$TestWithOldSdk)"
    );
  }

  @Test
  public void failureInResetterDoesntBreakAllTests() throws Exception {
    Injector injector = SingleSdkRobolectricTestRunner.defaultInjector()
        .register(AndroidSandbox.class, MyAndroidSandbox.class);

    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(TestWithTwoMethods.class, injector);

    runner.run(notifier);
    assertThat(events).containsExactly(
        "failure: fake error in setUpApplicationState",
        "failure: fake error in setUpApplicationState"
    );
  }

  @Test
  public void failureInAppOnCreateDoesntBreakAllTests() throws Exception {
    RobolectricTestRunner runner = new SingleSdkRobolectricTestRunner(TestWithBrokenAppCreate.class);
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "failure: fake error in application.onCreate",
            "failure: fake error in application.onCreate");
  }

  @Test
  public void equalityOfRobolectricFrameworkMethod() throws Exception {
    Method method = TestWithTwoMethods.class.getMethod("first");
    RobolectricFrameworkMethod rfm16 =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkProvider.getSdkConfig(16),
            mock(Config.class),
            ResourcesMode.legacy,
            ResourcesMode.legacy,
            false);
    RobolectricFrameworkMethod rfm17 =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkProvider.getSdkConfig(17),
            mock(Config.class),
            ResourcesMode.legacy,
            ResourcesMode.legacy,
            false);
    RobolectricFrameworkMethod rfm16b =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkProvider.getSdkConfig(16),
            mock(Config.class),
            ResourcesMode.legacy,
            ResourcesMode.legacy,
            false);
    RobolectricFrameworkMethod rfm16c =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkProvider.getSdkConfig(16),
            mock(Config.class),
            ResourcesMode.binary,
            ResourcesMode.legacy,
            false);

    assertThat(rfm16).isNotEqualTo(rfm17);
    assertThat(rfm16).isEqualTo(rfm16b);
    assertThat(rfm16).isNotEqualTo(rfm16c);

    assertThat(rfm16.hashCode()).isEqualTo((rfm16b.hashCode()));
  }

  @Test
  public void shouldReportPerfStats() throws Exception {
    List<Metric> metrics = new ArrayList<>();
    PerfStatsReporter reporter = (metadata, metrics1) -> metrics.addAll(metrics1);

    RobolectricTestRunner runner = new SingleSdkRobolectricTestRunner(TestWithTwoMethods.class) {
      @Nonnull
      @Override
      protected Iterable<PerfStatsReporter> getPerfStatsReporters() {
        return singletonList(reporter);
      }
    };

    runner.run(notifier);

    Set<String> metricNames = metrics.stream().map(Metric::getName).collect(toSet());
    assertThat(metricNames).contains("initialization");
  }

  @Test
  public void shouldResetThreadInterrupted() throws Exception {
    RobolectricTestRunner runner = new SingleSdkRobolectricTestRunner(TestWithInterrupt.class);
    runner.run(notifier);
    assertThat(events).containsExactly("failure: failed for the right reason");
  }

  /////////////////////////////

  @Ignore
  public static class TestWithOldSdk {
    @Config(sdk = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void oldSdkMethod() throws Exception {
      fail("I should not be run!");
    }

    @Ignore("This test shouldn't run, and shouldn't cause the test runner to fail")
    @Config(sdk = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void ignoredOldSdkMethod() throws Exception {
      fail("I should not be run!");
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class TestWithTwoMethods {
    @Test
    public void first() throws Exception {
    }

    @Test
    public void second() throws Exception {
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  @Config(application = MyTestApplication.class)
  public static class TestWithBrokenAppCreate {
    @Test
    public void first() throws Exception {}

    @Test
    public void second() throws Exception {}

    public static class MyTestApplication extends Application {
      @Override
      public void onCreate() {
        throw new RuntimeException("fake error in application.onCreate");
      }
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  @Config(application = MyTestApplication.class)
  public static class TestWithBrokenAppTerminate {
    @Test
    public void first() throws Exception {}

    @Test
    public void second() throws Exception {}

    public static class MyTestApplication extends Application {
      @Override
      public void onTerminate() {
        throw new RuntimeException("fake error in application.onTerminate");
      }
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class TestWithInterrupt {
    @Test
    public void first() throws Exception {
      Thread.currentThread().interrupt();
    }

    @Test
    public void second() throws Exception {
      TempDirectory tempDirectory = new TempDirectory("test");

      try {
        Path jarPath = tempDirectory.create("some-jar").resolve("some.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
          out.putNextEntry(new JarEntry("README.txt"));
          out.write("hi!".getBytes());
        }

        FileSystemProvider jarFSP = FileSystemProvider.installedProviders().stream()
            .filter(p -> p.getScheme().equals("jar")).findFirst().get();
        Path fakeJarFile = Paths.get(jarPath.toUri());

        // if Thread.interrupted() was true, this would fail in AbstractInterruptibleChannel:
        jarFSP.newFileSystem(fakeJarFile, new HashMap<>());
      } finally {
        tempDirectory.destroy();
      }

      fail("failed for the right reason");
    }
  }

  private static class SingleSdkRobolectricTestRunner extends RobolectricTestRunner {

    private static final DefaultSdkPicker DEFAULT_SDK_PICKER =
            new DefaultSdkPicker(new DefaultSdkProvider(), singletonList(DefaultSdkProvider.MAX_SDK_CONFIG), null);
    private static final Injector INJECTOR = defaultInjector();

    protected static Injector defaultInjector() {
      return RobolectricTestRunner.defaultInjector()
          .register(SdkPicker.class, DEFAULT_SDK_PICKER);
    }

    SingleSdkRobolectricTestRunner(Class<?> testClass) throws InitializationError {
      super(testClass, INJECTOR);
    }

    public SingleSdkRobolectricTestRunner(Class<?> testClass, Injector injector) throws InitializationError {
      super(testClass, injector);
    }

    @Override
    ResourcesMode getResourcesMode() {
      return ResourcesMode.legacy;
    }
  }

  private static class MyAndroidSandbox extends AndroidSandbox {

    @Inject
    public MyAndroidSandbox(SdkConfig sdkConfig, boolean useLegacyResources,
        ClassLoader robolectricClassLoader, ApkLoader apkLoader) {
      super(sdkConfig, useLegacyResources, robolectricClassLoader, apkLoader);
    }

    @Override
    protected Bridge getBridge() {
      Bridge mockBridge = mock(Bridge.class);
      doThrow(new RuntimeException("fake error in setUpApplicationState"))
          .when(mockBridge).setUpApplicationState(any(), any(), any(), any());
      return mockBridge;
    }
  }
}

package org.robolectric.internal;

import static java.util.Arrays.asList;

import com.google.common.collect.Lists;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import javax.annotation.Nonnull;
import javax.inject.Named;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.robolectric.internal.bytecode.ClassHandler;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;
import org.robolectric.internal.bytecode.Interceptor;
import org.robolectric.internal.bytecode.Interceptors;
import org.robolectric.internal.bytecode.Sandbox;
import org.robolectric.internal.bytecode.SandboxClassLoader;
import org.robolectric.internal.bytecode.SandboxConfig;
import org.robolectric.internal.bytecode.ShadowInfo;
import org.robolectric.internal.bytecode.ShadowMap;
import org.robolectric.internal.bytecode.ShadowWrangler;
import org.robolectric.pluginapi.perf.Metadata;
import org.robolectric.pluginapi.perf.Metric;
import org.robolectric.pluginapi.perf.PerfStatsReporter;
import org.robolectric.util.PerfStatsCollector;
import org.robolectric.util.PerfStatsCollector.Event;
import org.robolectric.util.inject.AutoFactory;
import org.robolectric.util.inject.Injector;

@SuppressWarnings("NewApi")
public class SandboxTestRunner extends BlockJUnit4ClassRunner {

  private static final ShadowMap BASE_SHADOW_MAP;
  private static final Injector DEFAULT_INJECTOR = defaultInjector().build();

  static {
    ServiceLoader<ShadowProvider> shadowProviders = ServiceLoader.load(ShadowProvider.class);
    BASE_SHADOW_MAP = ShadowMap.createFromShadowProviders(shadowProviders);
  }

  protected static Injector.Builder defaultInjector() {
    return new Injector.Builder();
  }

  private final List<PerfStatsReporter> perfStatsReporters;
  private final SandboxFactory sandboxFactory;
  private final ClassHandlerFactory classHandlerFactory;
  private final HashSet<Class<?>> loadedTestClasses = new HashSet<>();

  public SandboxTestRunner(Class<?> klass) throws InitializationError {
    this(klass, DEFAULT_INJECTOR);
  }

  public SandboxTestRunner(Class<?> klass, Injector injector) throws InitializationError {
    super(klass);

    perfStatsReporters = Arrays.asList(injector.getInstance(PerfStatsReporter[].class));
    sandboxFactory = injector.getInstance(SandboxFactory.class);
    classHandlerFactory = injector.getInstance(ClassHandlerFactory.class);
  }

  @Override
  protected Statement classBlock(RunNotifier notifier) {
    final Statement statement = childrenInvoker(notifier);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
          for (Class<?> testClass : loadedTestClasses) {
            invokeAfterClass(testClass);
          }
        } finally {
          afterClass();
          loadedTestClasses.clear();
        }
      }
    };
  }

  private void invokeBeforeClass(final Class clazz) throws Throwable {
    if (!loadedTestClasses.contains(clazz)) {
      loadedTestClasses.add(clazz);

      final TestClass testClass = new TestClass(clazz);
      final List<FrameworkMethod> befores = testClass.getAnnotatedMethods(BeforeClass.class);
      for (FrameworkMethod before : befores) {
        before.invokeExplosively(null);
      }
    }
  }

  private static void invokeAfterClass(final Class<?> clazz) throws Throwable {
    final TestClass testClass = new TestClass(clazz);
    final List<FrameworkMethod> afters = testClass.getAnnotatedMethods(AfterClass.class);
    for (FrameworkMethod after : afters) {
      after.invokeExplosively(null);
    }
  }

  protected void afterClass() {
  }

  @Nonnull
  protected Sandbox getSandbox(FrameworkMethod method) {
    InstrumentationConfiguration instrumentationConfiguration = createClassLoaderConfig(method);
    ClassLoader sandboxClassLoader = new SandboxClassLoader(ClassLoader.getSystemClassLoader(), instrumentationConfiguration);
    return sandboxFactory.createSandbox(sandboxClassLoader);
  }

  @AutoFactory
  private interface SandboxFactory {

    Sandbox createSandbox(@Named("sandboxClassLoader") ClassLoader classLoader);
  }

  /**
   * Create an {@link InstrumentationConfiguration} suitable for the provided {@link FrameworkMethod}.
   *
   * Custom TestRunner subclasses may wish to override this method to provide alternate configuration.
   *
   * @param method the test method that's about to run
   * @return an {@link InstrumentationConfiguration}
   */
  @Nonnull
  protected InstrumentationConfiguration createClassLoaderConfig(FrameworkMethod method) {
    InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder()
        .doNotAcquirePackage("java.")
        .doNotAcquirePackage("sun.")
        .doNotAcquirePackage("org.robolectric.annotation.")
        .doNotAcquirePackage("org.robolectric.internal.")
        .doNotAcquirePackage("org.robolectric.pluginapi.")
        .doNotAcquirePackage("org.robolectric.util.")
        .doNotAcquirePackage("org.junit.");

    String customPackages = System.getProperty("org.robolectric.packagesToNotAcquire", "");
    for (String pkg : customPackages.split(",")) {
      if (!pkg.isEmpty()) {
        builder.doNotAcquirePackage(pkg);
      }
    }

    for (Class<?> shadowClass : getExtraShadows(method)) {
      ShadowInfo shadowInfo = ShadowMap.obtainShadowInfo(shadowClass);
      builder.addInstrumentedClass(shadowInfo.shadowedClassName);
    }

    addInstrumentedPackages(method, builder);

    return builder.build();
  }

  private void addInstrumentedPackages(FrameworkMethod method, InstrumentationConfiguration.Builder builder) {
    SandboxConfig classConfig = getTestClass().getJavaClass().getAnnotation(SandboxConfig.class);
    if (classConfig != null) {
      for (String pkgName : classConfig.instrumentedPackages()) {
        builder.addInstrumentedPackage(pkgName);
      }
    }

    SandboxConfig methodConfig = method.getAnnotation(SandboxConfig.class);
    if (methodConfig != null) {
      for (String pkgName : methodConfig.instrumentedPackages()) {
        builder.addInstrumentedPackage(pkgName);
      }
    }
  }

  protected void configureSandbox(Sandbox sandbox, FrameworkMethod method) {
    ShadowMap.Builder builder = createShadowMap().newBuilder();

    // Configure shadows *BEFORE* setting the ClassLoader. This is necessary because
    // creating the ShadowMap loads all ShadowProviders via ServiceLoader and this is
    // not available once we install the Robolectric class loader.
    Class<?>[] shadows = getExtraShadows(method);
    if (shadows.length > 0) {
      builder.addShadowClasses(shadows);
    }
    ShadowMap shadowMap = builder.build();
    sandbox.replaceShadowMap(shadowMap);

    sandbox.configure(createClassHandler(shadowMap, sandbox));
  }

  @Override protected Statement methodBlock(final FrameworkMethod method) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        PerfStatsCollector perfStatsCollector = PerfStatsCollector.getInstance();
        perfStatsCollector.reset();
        perfStatsCollector.setEnabled(!perfStatsReporters.isEmpty());

        Event initialization = perfStatsCollector.startEvent("initialization");

        Sandbox sandbox = getSandbox(method);

        // Configure sandbox *BEFORE* setting the ClassLoader. This is necessary because
        // creating the ShadowMap loads all ShadowProviders via ServiceLoader and this is
        // not available once we install the Robolectric class loader.
        configureSandbox(sandbox, method);

        final ClassLoader priorContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(sandbox.getRobolectricClassLoader());

        Class bootstrappedTestClass = sandbox.bootstrappedClass(getTestClass().getJavaClass());
        HelperTestRunner helperTestRunner = getHelperTestRunner(bootstrappedTestClass);
        helperTestRunner.frameworkMethod = method;

        final Method bootstrappedMethod;
        try {
          //noinspection unchecked
          bootstrappedMethod = bootstrappedTestClass.getMethod(method.getMethod().getName());
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        }

        try {
          // Only invoke @BeforeClass once per class
          invokeBeforeClass(bootstrappedTestClass);

          beforeTest(sandbox, method, bootstrappedMethod);

          initialization.finished();

          final Statement statement = helperTestRunner.methodBlock(new FrameworkMethod(bootstrappedMethod));

          // todo: this try/finally probably isn't right -- should mimic RunAfters? [xw]
          try {
            statement.evaluate();
          } finally {
            afterTest(method, bootstrappedMethod);
          }
        } finally {
          Thread.currentThread().setContextClassLoader(priorContextClassLoader);

          try {
            finallyAfterTest(method);
          } catch (Exception e) {
            e.printStackTrace();
          }

          reportPerfStats(perfStatsCollector);
          perfStatsCollector.reset();
        }
      }
    };
  }

  private void reportPerfStats(PerfStatsCollector perfStatsCollector) {
    if (perfStatsReporters.isEmpty()) {
      return;
    }

    Metadata metadata = perfStatsCollector.getMetadata();
    Collection<Metric> metrics = perfStatsCollector.getMetrics();

    for (PerfStatsReporter perfStatsReporter : perfStatsReporters) {
      try {
        perfStatsReporter.report(metadata, metrics);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  protected void beforeTest(Sandbox sandbox, FrameworkMethod method, Method bootstrappedMethod) throws Throwable {
  }

  protected void afterTest(FrameworkMethod method, Method bootstrappedMethod) {
  }

  protected void finallyAfterTest(FrameworkMethod method) {
  }

  protected HelperTestRunner getHelperTestRunner(Class bootstrappedTestClass) {
    try {
      return new HelperTestRunner(bootstrappedTestClass);
    } catch (InitializationError initializationError) {
      throw new RuntimeException(initializationError);
    }
  }

  protected static class HelperTestRunner extends BlockJUnit4ClassRunner {
    public FrameworkMethod frameworkMethod;

    public HelperTestRunner(Class<?> klass) throws InitializationError {
      super(klass);
    }

    // cuz accessibility
    @Override
    protected Statement methodBlock(FrameworkMethod method) {
      return super.methodBlock(method);
    }
  }

  @Nonnull
  protected Class<?>[] getExtraShadows(FrameworkMethod method) {
    List<Class<?>> shadowClasses = new ArrayList<>();
    addShadows(shadowClasses, getTestClass().getJavaClass().getAnnotation(SandboxConfig.class));
    addShadows(shadowClasses, method.getAnnotation(SandboxConfig.class));
    return shadowClasses.toArray(new Class[shadowClasses.size()]);
  }

  private void addShadows(List<Class<?>> shadowClasses, SandboxConfig annotation) {
    if (annotation != null) {
      shadowClasses.addAll(asList(annotation.shadows()));
    }
  }

  protected ShadowMap createShadowMap() {
    return BASE_SHADOW_MAP;
  }

  @Nonnull
  protected ClassHandler createClassHandler(ShadowMap shadowMap, Sandbox sandbox) {
    return classHandlerFactory.create(shadowMap);
  }

  @AutoFactory
  private interface ClassHandlerFactory {

    ClassHandler create(ShadowMap shadowMap);
  }

  protected boolean shouldIgnore(FrameworkMethod method) {
    return method.getAnnotation(Ignore.class) != null;
  }
}
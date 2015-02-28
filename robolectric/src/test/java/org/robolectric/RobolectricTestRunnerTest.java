package org.robolectric;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.model.InitializationError;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.PackageResourceLoader;
import org.robolectric.res.ResourceLoader;
import org.robolectric.res.ResourcePath;
import org.robolectric.shadows.ShadowView;
import org.robolectric.shadows.ShadowViewGroup;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

public class RobolectricTestRunnerTest {
  @Test
  public void whenClassHasConfigAnnotation_getConfig_shouldMergeClassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test1.class, "withoutAnnotation"),
        1, "foo", "from-test", "test/res", "test/assets", 2, new Class[]{Test1.class});

    assertConfig(configFor(Test1.class, "withDefaultsAnnotation"),
        1, "foo", "from-test", "test/res", "test/assets", 2, new Class[]{Test1.class});

    assertConfig(configFor(Test1.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", "method/assets", 8, new Class[]{Test1.class, Test2.class});
  }

  @Test
  public void whenClassDoesntHaveConfigAnnotation_getConfig_shouldUseMethodConfig() throws Exception {
    assertConfig(configFor(Test2.class, "withoutAnnotation"),
        -1, "--default", "", "res", "assets", -1, new Class[]{});

    assertConfig(configFor(Test2.class, "withDefaultsAnnotation"),
        -1, "--default", "", "res", "assets", -1, new Class[]{});

    assertConfig(configFor(Test2.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", "method/assets", 8, new Class[]{Test1.class});
  }

  @Test
  public void whenClassDoesntHaveConfigAnnotation_getConfig_shouldMergeParentClassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test5.class, "withoutAnnotation"),
        1, "foo", "from-test", "test/res", "test/assets", 2, new Class[]{Test1.class});

    assertConfig(configFor(Test5.class, "withDefaultsAnnotation"),
        1, "foo", "from-test", "test/res", "test/assets", 2, new Class[]{Test1.class});

    assertConfig(configFor(Test5.class, "withOverrideAnnotation"),
        9, "foo", "from-method5", "test/res", "method5/assets", 8, new Class[]{Test1.class, Test5.class});
  }

  @Test
  public void whenClassAndParentClassHaveConfigAnnotation_getConfig_shouldMergeParentClassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test6.class, "withoutAnnotation"),
            1, "foo", "from-class6", "class6/res", "test/assets", 2, new Class[]{Test1.class, Test6.class});

    assertConfig(configFor(Test6.class, "withDefaultsAnnotation"),
            1, "foo", "from-class6", "class6/res", "test/assets", 2, new Class[]{Test1.class, Test6.class});

    assertConfig(configFor(Test6.class, "withOverrideAnnotation"),
            9, "foo", "from-method5", "class6/res", "method5/assets", 8, new Class[]{Test1.class, Test5.class, Test6.class});
  }

  @Test
  public void whenClassAndSubclassHaveConfigAnnotation_getConfig_shouldMergeClassSubclassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test3.class, "withoutAnnotation"),
        1, "foo", "from-subclass", "test/res", "test/assets", 2, new Class[]{Test1.class});

    assertConfig(configFor(Test3.class, "withDefaultsAnnotation"),
        1, "foo", "from-subclass", "test/res", "test/assets", 2, new Class[]{Test1.class});

    assertConfig(configFor(Test3.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", "method/assets", 8, new Class[]{Test1.class, Test2.class});
  }

  @Test
  public void whenClassDoesntHaveConfigAnnotationButSubclassDoes_getConfig_shouldMergeSubclassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test4.class, "withoutAnnotation"),
        -1, "--default", "from-subclass", "res", "assets", -1, new Class[]{});

    assertConfig(configFor(Test4.class, "withDefaultsAnnotation"),
        -1, "--default", "from-subclass", "res", "assets", -1, new Class[]{});

    assertConfig(configFor(Test4.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", "method/assets", 8, new Class[]{Test1.class});
  }

  @Test
  public void shouldLoadDefaultsFromPropertiesFile() throws Exception {
    Properties properties = properties(
        "emulateSdk: 432\n" +
            "manifest: --none\n" +
            "qualifiers: from-properties-file\n" +
            "resourceDir: from/properties/file/res\n" +
            "assetDir: from/properties/file/assets\n" +
            "reportSdk: 234\n" +
            "shadows: org.robolectric.shadows.ShadowView, org.robolectric.shadows.ShadowViewGroup\n" +
            "application: org.robolectric.TestFakeApp");
    assertConfig(configFor(Test2.class, "withoutAnnotation", properties),
        432, "--none", "from-properties-file", "from/properties/file/res", "from/properties/file/assets", 234, new Class[] {ShadowView.class, ShadowViewGroup.class});
  }

  @Test
  public void withEmptyShadowList_shouldLoadDefaultsFromPropertiesFile() throws Exception {
    Properties properties = properties("shadows:");
    assertConfig(configFor(Test2.class, "withoutAnnotation", properties), -1, "--default", "", "res", "assets", -1, new Class[] {});
  }

  @Test
  public void rememberThatSomeTestRunnerMethodsShouldBeOverridable() throws Exception {
    @SuppressWarnings("unused")
    class CustomTestRunner extends RobolectricTestRunner {
      public CustomTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
      }

      @Override public PackageResourceLoader createResourceLoader(ResourcePath resourcePath) {
        return super.createResourceLoader(resourcePath);
      }

      @Override
      protected ResourceLoader createAppResourceLoader(ResourceLoader systemResourceLoader,
          AndroidManifest appManifest) {
        return super.createAppResourceLoader(systemResourceLoader, appManifest);
      }
    }
  }

  private Config configFor(Class<?> testClass, String methodName, final Properties configProperties) throws InitializationError {
    Method info;
    try {
      info = testClass.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    return new RobolectricTestRunner(testClass) {
      @Override protected Properties getConfigProperties() {
        return configProperties;
      }
    }.getConfig(info);
  }

  private Config configFor(Class<?> testClass, String methodName) throws InitializationError {
    Method info;
    try {
      info = testClass.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    return new RobolectricTestRunner(testClass).getConfig(info);
  }

  private void assertConfig(Config config, int emulateSdk, String manifest, String qualifiers, String resourceDir, String assetsDir, int reportSdk, Class[] shadows) {
    assertThat(stringify(config)).isEqualTo(stringify(emulateSdk, manifest, qualifiers, resourceDir, assetsDir, reportSdk, shadows));
  }

  @Ignore @Config(emulateSdk = 1, manifest = "foo", reportSdk = 2, shadows = Test1.class, qualifiers = "from-test", resourceDir = "test/res", assetDir = "test/assets")
  public static class Test1 {
    @Test
    public void withoutAnnotation() throws Exception {
    }

    @Test @Config
    public void withDefaultsAnnotation() throws Exception {
    }

    @Test @Config(emulateSdk = 9, manifest = "furf", reportSdk = 8, shadows = Test2.class, qualifiers = "from-method", resourceDir = "method/res", assetDir = "method/assets")
    public void withOverrideAnnotation() throws Exception {
    }
  }

  @Ignore
  public static class Test2 {
    @Test
    public void withoutAnnotation() throws Exception {
    }

    @Test @Config
    public void withDefaultsAnnotation() throws Exception {
    }

    @Test @Config(emulateSdk = 9, manifest = "furf", reportSdk = 8, shadows = Test1.class, qualifiers = "from-method", resourceDir = "method/res", assetDir = "method/assets")
    public void withOverrideAnnotation() throws Exception {
    }
  }

  @Ignore
  @Config(qualifiers = "from-subclass")
  public static class Test3 extends Test1 {
  }

  @Ignore
  @Config(qualifiers = "from-subclass")
  public static class Test4 extends Test2 {
  }

  @Ignore
  public static class Test5 extends Test1 {
    @Test
    public void withoutAnnotation() throws Exception {
    }

    @Test @Config
    public void withDefaultsAnnotation() throws Exception {
    }

    @Test @Config(emulateSdk = 9, reportSdk = 8, shadows = Test5.class, qualifiers = "from-method5", assetDir = "method5/assets")
    public void withOverrideAnnotation() throws Exception {
    }
  }

  @Ignore
  @Config(qualifiers = "from-class6", shadows = Test6.class, resourceDir = "class6/res")
  public static class Test6 extends Test5 {
  }

  private String stringify(Config config) {
    int emulateSdk = config.emulateSdk();
    String manifest = config.manifest();
    String qualifiers = config.qualifiers();
    String resourceDir = config.resourceDir();
    String assetsDir = config.assetDir();
    int reportSdk = config.reportSdk();
    Class<?>[] shadows = config.shadows();
    return stringify(emulateSdk, manifest, qualifiers, resourceDir, assetsDir, reportSdk, shadows);
  }

  private String stringify(int emulateSdk, String manifest, String qualifiers, String resourceDir, String assetsDir, int reportSdk, Class<?>[] shadows) {
      String[] stringClasses = new String[shadows.length];
      for (int i = 0; i < stringClasses.length; i++) {
          stringClasses[i] = shadows[i].toString();
      }

      Arrays.sort(stringClasses);

      return "emulateSdk=" + emulateSdk + "\n" +
        "manifest=" + manifest + "\n" +
        "qualifiers=" + qualifiers + "\n" +
        "resourceDir=" + resourceDir + "\n" +
        "assetDir=" + assetsDir + "\n" +
        "reportSdk=" + reportSdk + "\n" +
        "shadows=" +  Arrays.toString(stringClasses);
  }

  private Properties properties(String s) throws IOException {
    StringReader reader = new StringReader(s);
    Properties properties = new Properties();
    properties.load(reader);
    return properties;
  }
}

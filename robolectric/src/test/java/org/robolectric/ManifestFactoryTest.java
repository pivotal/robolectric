package org.robolectric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.robolectric.util.TestUtil.resourceFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.annotation.Config;
import org.robolectric.internal.DefaultManifestFactory;
import org.robolectric.internal.ManifestFactory;
import org.robolectric.internal.ManifestIdentifier;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

@RunWith(JUnit4.class)
public class ManifestFactoryTest {
  @Test
  public void shouldLoadLibraryManifests() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("manifest", resourceFile("TestAndroidManifest.xml").toString());
    properties.setProperty("libraries", "lib1");
    Config config = Config.Implementation.fromProperties(properties);
    ManifestFactory manifestFactory = new RobolectricTestRunner(ManifestFactoryTest.class).getManifestFactory(config);
    AndroidManifest manifest = manifestFactory.create(manifestFactory.identify(config));

    List<AndroidManifest> libraryManifests = manifest.getLibraryManifests();
    assertEquals(1, libraryManifests.size());
    assertEquals("org.robolectric.lib1", libraryManifests.get(0).getPackageName());
  }

  @Test
  public void shouldLoadAllResourcesForExistingLibraries() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("manifest", resourceFile("TestAndroidManifest.xml").toString());
    properties.setProperty("resourceDir", "res");
    properties.setProperty("assetDir", "assets");
    Config config = Config.Implementation.fromProperties(properties);
    ManifestFactory manifestFactory = new RobolectricTestRunner(ManifestFactoryTest.class).getManifestFactory(config);
    AndroidManifest appManifest = manifestFactory.create(manifestFactory.identify(config));

    // This intentionally loads from the non standard resources/project.properties
    assertThat(appManifest.getIncludedResourcePaths().get(0).getResourceBase().toString()).isEqualTo("./src/test/resources/res");
    assertThat(appManifest.getIncludedResourcePaths().get(0).getResourceBase().toString()).isEqualTo("./src/test/resources/lib1/res");
    assertThat(appManifest.getIncludedResourcePaths().get(0).getResourceBase().toString()).isEqualTo("./src/test/resources/lib1/../lib3/res");
    assertThat(appManifest.getIncludedResourcePaths().get(0).getResourceBase().toString()).isEqualTo("./src/test/resources/lib1/../lib2/res");
  }

  @Test
  public void whenBuildSystemApiPropertiesFileIsPresent_shouldUseDefaultManifestFactory() throws Exception {
    final Properties properties = new Properties();
    properties.setProperty("android_sdk_home", "");
    properties.setProperty("android_merged_manifest", "/path/to/MergedManifest.xml");
    properties.setProperty("android_merged_resources", "/path/to/merged-resources");
    properties.setProperty("android_merged_assets", "/path/to/merged-assets");

    RobolectricTestRunner testRunner = new RobolectricTestRunner(ManifestFactoryTest.class) {
      Properties getBuildSystemApiProperties() {
        return properties;
      }
    };

    Config.Implementation config = Config.Builder.defaults().build();
    ManifestFactory manifestFactory = testRunner.getManifestFactory(config);
    assertThat(manifestFactory).isInstanceOf(DefaultManifestFactory.class);
    ManifestIdentifier manifestIdentifier = manifestFactory.identify(config);
    assertThat(manifestIdentifier.getManifestFile()).isEqualTo(Fs.fileFromPath("/path/to/MergedManifest.xml"));
    assertThat(manifestIdentifier.getResDir()).isEqualTo(Fs.fileFromPath("/path/to/merged-resources"));
    assertThat(manifestIdentifier.getAssetDir()).isEqualTo(Fs.fileFromPath("/path/to/merged-assets"));
    assertThat(manifestIdentifier.getLibraryDirs()).isEmpty();
    assertThat(manifestIdentifier.getPackageName()).isNull();

    AndroidManifest androidManifest = manifestFactory.create(manifestIdentifier);
    assertThat(androidManifest.getAndroidManifestFile()).isEqualTo(Fs.fileFromPath("/path/to/MergedManifest.xml"));
    assertThat(androidManifest.getResDirectory()).isEqualTo(Fs.fileFromPath("/path/to/merged-resources"));
    assertThat(androidManifest.getAssetsDirectory()).isEqualTo(Fs.fileFromPath("/path/to/merged-assets"));
  }

  @Test
  public void whenConfigSpecified_overridesValuesFromFile() throws Exception {
    final Properties properties = new Properties();
    properties.setProperty("android_sdk_home", "");
    properties.setProperty("android_merged_manifest", "/path/to/MergedManifest.xml");
    properties.setProperty("android_merged_resources", "/path/to/merged-resources");
    properties.setProperty("android_merged_assets", "/path/to/merged-assets");

    RobolectricTestRunner testRunner = new RobolectricTestRunner(ManifestFactoryTest.class) {
      Properties getBuildSystemApiProperties() {
        return properties;
      }
    };

    Config.Implementation config = Config.Builder.defaults()
        .setManifest("/TestAndroidManifest.xml")
        .setPackageName("another.package")
        .build();
    ManifestFactory manifestFactory = testRunner.getManifestFactory(config);
    assertThat(manifestFactory).isInstanceOf(DefaultManifestFactory.class);
    ManifestIdentifier manifestIdentifier = manifestFactory.identify(config);
    assertThat(manifestIdentifier.getManifestFile()).isEqualTo(Fs.fileFromPath(getClass().getResource("/TestAndroidManifest.xml").getPath()));
    assertThat(manifestIdentifier.getResDir()).isEqualTo(Fs.fileFromPath("/path/to/merged-resources"));
    assertThat(manifestIdentifier.getAssetDir()).isEqualTo(Fs.fileFromPath("/path/to/merged-assets"));
    assertThat(manifestIdentifier.getLibraryDirs()).isEmpty();
    assertThat(manifestIdentifier.getPackageName()).isEqualTo("another.package");
  }
}

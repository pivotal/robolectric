package org.robolectric.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.robolectric.annotation.Config;
import org.robolectric.gradleapp.BuildConfig;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.FileFsFile;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.util.TestUtil.joinPath;
import static org.robolectric.util.TestUtil.newFile;

public class GradleManifestFactoryTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private GradleManifestFactory factory;

  @Before
  public void setup() {
    FileFsFile.from("build", "intermediates", "res").getFile().mkdirs();
    FileFsFile.from("build", "intermediates", "assets").getFile().mkdirs();
    FileFsFile.from("build", "intermediates", "manifests").getFile().mkdirs();

    FileFsFile.from("custom_build", "intermediates", "res").getFile().mkdirs();
    FileFsFile.from("custom_build", "intermediates", "assets").getFile().mkdirs();
    FileFsFile.from("custom_build", "intermediates", "manifests").getFile().mkdirs();
    factory = new GradleManifestFactory();
  }

  @After
  public void teardown() throws IOException {
    delete(FileFsFile.from("build", "intermediates", "res").getFile());
    delete(FileFsFile.from("build", "intermediates", "assets").getFile());
    delete(FileFsFile.from("build", "intermediates", "manifests").getFile());
    delete(FileFsFile.from("build", "intermediates", "res", "merged").getFile());

    delete(FileFsFile.from("custom_build", "intermediates", "res").getFile());
    delete(FileFsFile.from("custom_build", "intermediates", "assets").getFile());
    delete(FileFsFile.from("custom_build", "intermediates", "manifests").getFile());
  }
  
  @Test
  public void getAppManifest_forApplications_shouldCreateManifest() throws Exception {
    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).build());

    assertThat(manifest.getPackageName()).isEqualTo("org.robolectric.gradleapp");
    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("build/intermediates/res/flavor1/type1"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("build/intermediates/assets/flavor1/type1"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("build/intermediates/manifests/full/flavor1/type1/AndroidManifest.xml"));
  }

  @Test
  public void getAppManifest_forLibraries_shouldCreateManifest() throws Exception {
    delete(FileFsFile.from("build", "intermediates", "res").getFile());
    delete(FileFsFile.from("build", "intermediates", "assets").getFile());
    delete(FileFsFile.from("build", "intermediates", "manifests").getFile());

    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).build());

    assertThat(manifest.getPackageName()).isEqualTo("org.robolectric.gradleapp");
    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("build/intermediates/bundles/flavor1/type1/res"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("build/intermediates/bundles/flavor1/type1/assets"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("build/intermediates/bundles/flavor1/type1/AndroidManifest.xml"));
  }

  @Test
  public void getAppManifest_shouldCreateManifestWithMethodOverrides() throws Exception {
    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfigOverride.class).build());

    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("build/intermediates/res/flavor2/type2"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("build/intermediates/assets/flavor2/type2"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("build/intermediates/manifests/full/flavor2/type2/AndroidManifest.xml"));
  }

  @Test
  public void getAppManifest_withBuildDirOverride_shouldCreateManifest() throws Exception {
    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).setBuildDir("custom_build").build());

    assertThat(manifest.getPackageName()).isEqualTo("org.robolectric.gradleapp");
    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("custom_build/intermediates/res/flavor1/type1"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("custom_build/intermediates/assets/flavor1/type1"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("custom_build/intermediates/manifests/full/flavor1/type1/AndroidManifest.xml"));
  }

  @Test
  public void getAppManifest_withPackageNameOverride_shouldCreateManifest() throws Exception {
    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).setPackageName("fake.package.name").build());

    assertThat(manifest.getPackageName()).isEqualTo("fake.package.name");
    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("build/intermediates/res/flavor1/type1"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("build/intermediates/assets/flavor1/type1"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("build/intermediates/manifests/full/flavor1/type1/AndroidManifest.xml"));
  }

  @Test
  public void getAppManifest_withAbiSplitOverride_shouldCreateManifest() throws Exception {
    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).setAbiSplit("armeabi").build());

    assertThat(manifest.getPackageName()).isEqualTo("org.robolectric.gradleapp");
    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("build/intermediates/res/flavor1/type1"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("build/intermediates/assets/flavor1/type1"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("build/intermediates/manifests/full/flavor1/armeabi/type1/AndroidManifest.xml"));
  }

  @Test
  public void getAppManifest_withMergedResources_shouldHaveMergedResPath() throws Exception {
    FileFsFile.from("build", "intermediates", "res", "merged").getFile().mkdirs();

    final AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).setPackageName("fake.package.name").build());

    assertThat(manifest.getPackageName()).isEqualTo("fake.package.name");
    assertThat(manifest.getResDirectory().getPath()).isEqualTo(convertPath("build/intermediates/res/merged/flavor1/type1"));
    assertThat(manifest.getAssetsDirectory().getPath()).isEqualTo(convertPath("build/intermediates/assets/flavor1/type1"));
    assertThat(manifest.getAndroidManifestFile().getPath()).isEqualTo(convertPath("build/intermediates/manifests/full/flavor1/type1/AndroidManifest.xml"));
  }

  @Test
  public void rClassShouldBeInTheSamePackageAsBuildConfig() throws Exception {
    File manifestFile = new File(
        joinPath("build", "intermediates", "manifests", "full",
            org.robolectric.gradleapp.BuildConfig.FLAVOR,
            org.robolectric.gradleapp.BuildConfig.BUILD_TYPE),
        "AndroidManifest.xml");
    manifestFile.getParentFile().mkdirs();
    newFile(manifestFile, "<manifest package=\"something\"/>");

    AndroidManifest manifest = createManifest(
        new Config.Builder().setConstants(BuildConfig.class).build());
    assertThat(manifest.getRClass().getPackage().getName()).isEqualTo("org.robolectric.gradleapp");
  }

  ////////////////////////////////

  private AndroidManifest createManifest(Config config) {
    return factory.create(factory.identify(config));
  }

  private static String convertPath(String path) {
    return path.replace('/', File.separatorChar);
  }

  private void delete(File file) {
    final File[] files = file.listFiles();
    if (files != null) {
      for (File each : files) {
        delete(each);
      }
    }
    file.delete();
  }

  public static class BuildConfigOverride {
    public static final String APPLICATION_ID = "org.sandwich.bar";
    public static final String BUILD_TYPE = "type2";
    public static final String FLAVOR = "flavor2";
  }
}

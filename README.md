<a name="README">[<img src="https://rawgithub.com/robolectric/robolectric/master/images/robolectric-horizontal.png"/>](http://robolectric.org)</a>

[![Build Status](https://travis-ci.org/robolectric/robolectric.svg?branch=master)](https://travis-ci.org/robolectric/robolectric)
[![GitHub release](https://img.shields.io/github/release/robolectric/robolectric.svg?maxAge=60)](https://github.com/robolectric/robolectric/releases)

Robolectric is a testing framework that de-fangs the Android SDK so you can test-drive the development of your Android app.

## Usage

Here's an example of a simple test written using Robolectric:

```java
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MyActivityTest {

  @Test
  public void clickingButton_shouldChangeResultsViewText() throws Exception {
    Activity activity = Robolectric.setupActivity(MyActivity.class);

    Button button = (Button) activity.findViewById(R.id.press_me_button);
    TextView results = (TextView) activity.findViewById(R.id.results_text_view);

    button.performClick();
    assertThat(results.getText().toString(), equalTo("Testing Android Rocks!"));
  }
}
```

For more information about how to install and use Robolectric on your project, extend its functionality, and join the community of contributors, please visit [http://robolectric.org](http://robolectric.org).

## Install

### Starting a New Project

If you'd like to start a new project with Robolectric tests you can refer to `deckard` (for either [maven](http://github.com/robolectric/deckard-maven) or [gradle](http://github.com/robolectric/deckard-gradle)) as a guide to setting up both Android and Robolectric on your machine.

### Gradle

```groovy
testCompile "org.robolectric:robolectric:3.3.2"
```

### Maven

```xml
<dependency>
   <groupId>org.robolectric</groupId>
   <artifactId>robolectric</artifactId>
   <version>3.3.2</version>
   <scope>test</scope>
</dependency>
```

## Building And Contributing

Robolectric is built using Gradle. Both IntelliJ and Android Studio can import the top-level `build.gradle` file and will automatically generate their project files from it.

You will need to have portions of the Android SDK available in your local Maven artifact repository in order to build Robolectric. Copy all required Android dependencies to your local Maven repo by running:

    ./scripts/install-dependencies.rb

Robolectric supports running tests against multiple Android API levels. The work it must do to support each API level is slightly different, so its shadows are built separately for each. To build shadows for every API version, run:

    ./gradlew clean assemble install compileTest

### Using Snapshots

If you would like to live on the bleeding edge, you can try running against a snapshot build. Keep in mind that snapshots represent the most recent changes on master and may contain bugs.

### Gradle

```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
}

dependencies {
    testCompile "org.robolectric:robolectric:3.4-SNAPSHOT"
}
```

### Maven

```xml
<repository>
  <id>sonatype-snapshpots</id>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
</repository>

<dependency>
   <groupId>org.robolectric</groupId>
   <artifactId>robolectric</artifactId>
   <version>3.4-SNAPSHOT</version>
   <scope>test</scope>
</dependency>
```

package org.robolectric.shadows;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;
import org.robolectric.util.Transcript;
import android.support.v4.content.AsyncTaskLoader;

@RunWith(TestRunners.WithDefaults.class)
public class AsyncTaskLoaderTest {
  private Transcript transcript;

  @Before public void setUp() {
    transcript = new Transcript();
    ShadowLooper.getUiThreadScheduler().pause();
    ShadowApplication.getInstance().getBackgroundScheduler().pause();
  }

  @Test public void forceLoad_shouldEnqueueWorkOnSchedulers() {
    new TestLoader().forceLoad();
    transcript.assertNoEventsSoFar();

    ShadowApplication.getInstance().getBackgroundScheduler().runOneTask();
    transcript.assertEventsSoFar("loadInBackground");

    ShadowLooper.getUiThreadScheduler().runOneTask();
    transcript.assertEventsSoFar("deliverResult");
  }

  public class TestLoader extends AsyncTaskLoader<Void> {
    public TestLoader() {
      super(RuntimeEnvironment.application);
    }

    @Override
    public Void loadInBackground() {
      transcript.add("loadInBackground");
      return null;
    }

    @Override
    public void deliverResult(Void data) {
      transcript.add("deliverResult");
    }
  }
}

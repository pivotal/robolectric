package org.robolectric.util.concurrent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.TestRunners;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.Scheduler;
import org.robolectric.util.Transcript;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunners.WithDefaults.class)
public class RoboExecutorServiceTest {
  private final Transcript transcript = new Transcript();
  private final RoboExecutorService executorService = new RoboExecutorService();
  private final Scheduler backgroundScheduler = Robolectric.getBackgroundThreadScheduler();
  private Runnable runnable;

  @Before
  public void setUp() throws Exception {
    backgroundScheduler.pause();
    runnable = new Runnable() {
      @Override
      public void run() {
        transcript.add("background event ran");
      }
    };
  }

  @Test
  public void execute_shouldRunStuffOnBackgroundThread() throws Exception {
    executorService.execute(runnable);

    transcript.assertNoEventsSoFar();

    ShadowApplication.runBackgroundTasks();
    transcript.assertEventsSoFar("background event ran");
  }

  @Test
  public void submitRunnable_shouldRunStuffOnBackgroundThread() throws Exception {
    Future<String> future = executorService.submit(runnable, "foo");

    transcript.assertNoEventsSoFar();
    assertThat(future.isDone()).isFalse();

    ShadowApplication.runBackgroundTasks();
    transcript.assertEventsSoFar("background event ran");
    assertThat(future.isDone()).isTrue();

    assertThat(future.get()).isEqualTo("foo");
  }

  @Test
  public void submitCallable_shouldRunStuffOnBackgroundThread() throws Exception {
    Future<String> future = executorService.submit(new Callable<String>() {
      @Override
      public String call() throws Exception {
        runnable.run();
        return "foo";
      }
    });

    transcript.assertNoEventsSoFar();
    assertThat(future.isDone()).isFalse();

    ShadowApplication.runBackgroundTasks();
    transcript.assertEventsSoFar("background event ran");
    assertThat(future.isDone()).isTrue();

    assertThat(future.get()).isEqualTo("foo");
  }

  @Test
  public void byDefault_IsNotShutdown() {
    assertThat(executorService.isShutdown()).isFalse();
  }

  @Test
  public void byDefault_IsNotTerminated() {
    assertThat(executorService.isTerminated()).isFalse();
  }

  @Test
  public void whenShutdownBeforeSubmittedTasksAreExecuted_TaskIsNotInTranscript() {
    executorService.execute(runnable);

    executorService.shutdown();
    ShadowApplication.runBackgroundTasks();

    transcript.assertNoEventsSoFar();
  }

  @Test
  public void whenShutdownNow_ReturnedListContainsOneRunnable() {
    executorService.execute(runnable);

    List<Runnable> notExecutedRunnables = executorService.shutdownNow();
    ShadowApplication.runBackgroundTasks();

    transcript.assertNoEventsSoFar();
    assertThat(notExecutedRunnables).hasSize(1);
  }

  @Test(timeout = 500)
  public void whenGettingFutureValue_FutureRunnableIsExecuted() throws Exception {
    Future<String> future = executorService.submit(runnable, "foo");

    assertThat(future.get()).isEqualTo("foo");
    assertThat(future.isDone()).isTrue();
  }

  @Test
  public void whenAwaitingTerminationAfterShutdown_TrueIsReturned() throws InterruptedException {
    executorService.shutdown();

    assertThat(executorService.awaitTermination(0, TimeUnit.MILLISECONDS)).isTrue();
  }

  @Test
  public void whenAwaitingTermination_AllTasksAreRunByDefault() throws Exception {
    executorService.execute(runnable);

    assertThat(executorService.awaitTermination(500, TimeUnit.MILLISECONDS)).isFalse();
    transcript.assertNoEventsSoFar();
  }
}
package org.robolectric.shadows;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;
import org.robolectric.annotation.Config;

import static android.os.Build.VERSION_CODES.N;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.MultiApiSelfTest.class)
@Config(minSdk = N)
public class ShadowJobSchedulerTest {

  private JobScheduler jobScheduler;

  @Before
  public void setUp() {
    jobScheduler = (JobScheduler) RuntimeEnvironment.application.getSystemService(Context.JOB_SCHEDULER_SERVICE);
  }

  @Test
  public void getAllPendingJobs() {
    JobInfo jobInfo = new JobInfo.Builder(99,
        new ComponentName(RuntimeEnvironment.application, "component_class_name"))
        .setPeriodic(1000)
        .build();
    jobScheduler.schedule(jobInfo);

    assertThat(jobScheduler.getAllPendingJobs()).contains(jobInfo);
  }

  @Test
  public void getPendingJob() {
    JobInfo jobInfo = new JobInfo.Builder(99,
        new ComponentName(RuntimeEnvironment.application, "component_class_name"))
        .setRequiresDeviceIdle(true)
        .build();
    jobScheduler.schedule(jobInfo);

    assertThat(jobScheduler.getPendingJob(99)).isEqualTo(jobInfo);
    assertThat(jobScheduler.getPendingJob(100)).isNull();
  }

  @Test
  public void cancelAll() {
    jobScheduler.schedule(new JobInfo.Builder(99,
        new ComponentName(RuntimeEnvironment.application, "component_class_name"))
        .setPeriodic(1000)
        .build());
    jobScheduler.schedule(new JobInfo.Builder(33,
        new ComponentName(RuntimeEnvironment.application, "component_class_name"))
        .setPeriodic(1000)
        .build());

    assertThat(jobScheduler.getAllPendingJobs()).hasSize(2);

    jobScheduler.cancelAll();

    assertThat(jobScheduler.getAllPendingJobs()).isEmpty();
  }

  @Test
  public void cancelSingleJob() {
    jobScheduler.schedule(new JobInfo.Builder(99,
          new ComponentName(RuntimeEnvironment.application, "component_class_name"))
          .setPeriodic(1000)
          .build());

    assertThat(jobScheduler.getAllPendingJobs()).isNotEmpty();

    jobScheduler.cancel(99);

    assertThat(jobScheduler.getAllPendingJobs()).isEmpty();
  }

  @Test
  public void cancelNonExistentJob() {
    jobScheduler.schedule(new JobInfo.Builder(99,
          new ComponentName(RuntimeEnvironment.application, "component_class_name"))
          .setPeriodic(1000)
          .build());

    assertThat(jobScheduler.getAllPendingJobs()).isNotEmpty();

    jobScheduler.cancel(33);

    assertThat(jobScheduler.getAllPendingJobs()).isNotEmpty();
  }

  @Test
  public void schedule_success() {
    int result = jobScheduler.schedule(new JobInfo.Builder(99,
        new ComponentName(RuntimeEnvironment.application, "component_class_name"))
        .setPeriodic(1000)
        .build());
    assertThat(result).isEqualTo(JobScheduler.RESULT_SUCCESS);
  }

  @Test
  public void schedule_fail() {
    shadowOf(jobScheduler).failOnJob(99);

    int result = jobScheduler.schedule(new JobInfo.Builder(99,
        new ComponentName(RuntimeEnvironment.application, "component_class_name"))
        .setPeriodic(1000)
        .build());

    assertThat(result).isEqualTo(JobScheduler.RESULT_FAILURE);
  }

  @Test
  @Config(minSdk = N)
  public void getPendingJob_withValidId() {
    int jobId = 99;
    JobInfo originalJobInfo = new JobInfo.Builder(jobId,
          new ComponentName(RuntimeEnvironment.application, "component_class_name"))
          .setPeriodic(1000)
          .build();

    jobScheduler.schedule(originalJobInfo);

    JobInfo retrievedJobInfo = jobScheduler.getPendingJob(jobId);

    assertThat(retrievedJobInfo).isEqualTo(originalJobInfo);
  }

  @Test
  @Config(minSdk = N)
  public void getPendingJob_withInvalidId() {
    int jobId = 99;
    int invalidJobId = 100;
    JobInfo originalJobInfo = new JobInfo.Builder(jobId,
          new ComponentName(RuntimeEnvironment.application, "component_class_name"))
          .setPeriodic(1000)
          .build();

    jobScheduler.schedule(originalJobInfo);

    JobInfo retrievedJobInfo = jobScheduler.getPendingJob(invalidJobId);

    assertThat(retrievedJobInfo).isNull();
  }
}

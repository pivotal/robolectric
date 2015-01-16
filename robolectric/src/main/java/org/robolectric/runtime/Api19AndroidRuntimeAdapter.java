package org.robolectric.runtime;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.IBinder;
import org.robolectric.RoboInstrumentation;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

public class Api19AndroidRuntimeAdapter implements AndroidRuntimeAdapter{

  @Override
  public void callActivityAttach(Object component, Context baseContext, Class<?> activityThreadClass, Application application, Intent intent, ActivityInfo activityInfo, String activityTitle, Class<?> nonConfigurationInstancesClass) {
    ReflectionHelpers.callInstanceMethod(component, "attach",
        ClassParameter.from(Context.class, baseContext),
        ClassParameter.from(activityThreadClass, null),
        ClassParameter.from(Instrumentation.class, new RoboInstrumentation()),
        ClassParameter.from(IBinder.class, null),
        ClassParameter.from(int.class, 0),
        ClassParameter.from(Application.class, application),
        ClassParameter.from(Intent.class, intent),
        ClassParameter.from(ActivityInfo.class, activityInfo),
        ClassParameter.from(CharSequence.class, activityTitle),
        ClassParameter.from(Activity.class, null),
        ClassParameter.from(String.class, "id"),
        ClassParameter.from(nonConfigurationInstancesClass, null),
        ClassParameter.from(Configuration.class, application.getResources().getConfiguration()));
  }
}

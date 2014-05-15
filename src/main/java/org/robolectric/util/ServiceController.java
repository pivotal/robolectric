package org.robolectric.util;

import android.app.Service;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import org.robolectric.AndroidManifest;
import org.robolectric.RoboInstrumentation;
import org.robolectric.Robolectric;
import org.robolectric.res.ResName;
import org.robolectric.shadows.ShadowService;
import org.robolectric.shadows.ShadowActivityThread;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import static org.fest.reflect.core.Reflection.constructor;
import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;
import static org.fest.reflect.core.Reflection.type;
import static org.robolectric.Robolectric.shadowOf_;

public class ServiceController<T extends Service> extends ComponentController<ServiceController<T>, T, ShadowService>{

  public static <T extends Service> ServiceController<T> of(Class<T> serviceClass) {
    return new ServiceController<T>(serviceClass);
  }

  public static <T extends Service> ServiceController<T> of(T service) {
    return new ServiceController<T>(service);
  }

  public ServiceController(Class<T> serviceClass) {
    this(constructor().in(serviceClass).newInstance());
  }

  public ServiceController(T service) {
    super(service);
  }

  public ServiceController<T> attach() {
    Application application = this.application == null ? Robolectric.application : this.application;
    Context baseContext = this.baseContext == null ? application : this.baseContext;

    ClassLoader cl = baseContext.getClassLoader();
    Class<?> activityThreadClass = type(ShadowActivityThread.CLASS_NAME).withClassLoader(cl).load();
    
    method("attach").withParameterTypes(
        Context.class /* context */,
        activityThreadClass /* aThread */,
        String.class /* className */,
        IBinder.class /* token */,
        Application.class /* application */,
        Object.class /* activityManager */
        
    ).in(component).invoke(
        baseContext,
        null /* aThread */,
        component.getClass().getSimpleName(), /* className */
        null /* token */,
        application,
        null /* activityManager */);

    attached = true;
    return this;
  }

  public ServiceController<T> bind() {
    invokeWhilePaused("onBind", getIntent());
    return this;
  }

  public ServiceController<T> create() {
    invokeWhilePaused("onCreate");
    return this;
  }

  public ServiceController<T> destroy() {
    invokeWhilePaused("onDestroy");
    return this;
  }
  
  public ServiceController<T> rebind() {
    invokeWhilePaused("onRebind", getIntent());
    return this;
  }
  
  public ServiceController<T> startCommand(int flags, int startId) {
    invokeWhilePaused("onStartCommand", getIntent(), flags, startId);
    return this;
  }

  public ServiceController<T> unbind() {
    invokeWhilePaused("onUnbind", getIntent());
    return this;
  }
}

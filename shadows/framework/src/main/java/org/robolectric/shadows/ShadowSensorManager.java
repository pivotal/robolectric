package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.O;
import static com.google.common.base.Preconditions.checkArgument;

import android.hardware.Sensor;
import android.hardware.SensorDirectChannel;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.MemoryFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@Implements(value = SensorManager.class, looseSignatures = true)
public class ShadowSensorManager {
  public boolean forceListenersToFail = false;
  private final Map<Integer, Sensor> sensorMap = new HashMap<>();
  private final ArrayList<SensorEventListener> listeners = new ArrayList<>();

  @RealObject private SensorManager realObject;

  /**
   * Provide a Sensor for the indicated sensor type.
   * @param sensorType from Sensor constants
   * @param sensor Sensor instance
   */
  public void addSensor(int sensorType, Sensor sensor) {
    sensorMap.put(sensorType, sensor);
  }

  @Implementation
  public Sensor getDefaultSensor(int type) {
    return sensorMap.get(type);
  }

  /** @param handler is ignored. */
  @Implementation
  protected boolean registerListener(
      SensorEventListener listener, Sensor sensor, int rate, Handler handler) {
    return registerListener(listener, sensor, rate);
  }

  @Implementation
  public boolean registerListener(SensorEventListener listener, Sensor sensor, int rate) {
    if (forceListenersToFail) {
      return false;
    }
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
    return true;
  }

  @Implementation
  public void unregisterListener(SensorEventListener listener, Sensor sensor) {
    listeners.remove(listener);
  }

  @Implementation
  public void unregisterListener(SensorEventListener listener) {
    listeners.remove(listener);
  }

  public boolean hasListener(SensorEventListener listener) {
    return listeners.contains(listener);
  }

  /** Propagates the {@code event} to all registered listeners. */
  public void sendSensorEventToListeners(SensorEvent event) {
    for (SensorEventListener listener : listeners) {
      listener.onSensorChanged(event);
    }
  }

  public SensorEvent createSensorEvent() {
    return ReflectionHelpers.callConstructor(SensorEvent.class);
  }

  /**
   * Creates a {@link SensorEvent} with the given value array size, which the caller should set
   * based on the type of {@link Sensor} which is being emulated.
   *
   * <p>Callers can then specify individual values for the event. For example, for a proximity event
   * a caller may wish to specify the distance value:
   *
   * <pre>{@code
   * event.values[0] = distance;
   * }</pre>
   *
   * <p>See {@link SensorEvent#values} for more information about values.
   */
  public static SensorEvent createSensorEvent(int valueArraySize) {
    checkArgument(valueArraySize > 0);
    ClassParameter<Integer> valueArraySizeParam = new ClassParameter<>(int.class, valueArraySize);
    return ReflectionHelpers.callConstructor(SensorEvent.class, valueArraySizeParam);
  }

  @Implementation(minSdk = O)
  public Object createDirectChannel(MemoryFile mem) {
    return ReflectionHelpers.callConstructor(SensorDirectChannel.class,
        ClassParameter.from(SensorManager.class, realObject),
        ClassParameter.from(int.class, 0),
        ClassParameter.from(int.class, SensorDirectChannel.TYPE_MEMORY_FILE),
        ClassParameter.from(long.class, mem.length()));
  }
}

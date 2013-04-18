package org.robolectric.shadows;

import static org.fest.reflect.core.Reflection.field;

import android.os.Bundle;
import org.robolectric.internal.Implementation;
import org.robolectric.internal.Implements;
import org.robolectric.internal.RealObject;

import java.util.Map;

@Implements(value = Bundle.class, callThroughByDefault = true)
public class ShadowBundle {

    @RealObject
    private Bundle realObject;

    @Override
    @Implementation
    public int hashCode() {
        // force unparcelling
        realObject.keySet();
        Map underlyingMap = getUnderlyingMap(realObject);
        return underlyingMap != null ? underlyingMap.hashCode() : 0;
    }

    @Override
    @Implementation
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Bundle)) {
            return false;
        }
        Bundle bundle = (Bundle) other;
        // Force unparcelling of both sets of data and compare internal maps.
        realObject.keySet();
        bundle.keySet();
        Map realObjectMap = getUnderlyingMap(realObject);
        Map otherMap = getUnderlyingMap(bundle);
        return realObjectMap.equals(otherMap);
    }

    private static Map getUnderlyingMap(Bundle bundle) {
        return field("mMap").ofType(Map.class).in(bundle).get();
    }
}

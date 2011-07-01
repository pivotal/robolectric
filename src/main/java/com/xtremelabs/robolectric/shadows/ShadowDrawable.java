package com.xtremelabs.robolectric.shadows;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.internal.RealObject;

import java.io.InputStream;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(Drawable.class)
public class ShadowDrawable {
    private static int defaultIntrinsicWidth = -1;
    private static int defaultIntrinsicHeight = -1;

    @RealObject Drawable realObject;

    private Rect bounds = new Rect(0, 0, 0, 0);
    private int intrinsicWidth = defaultIntrinsicWidth;
    private int intrinsicHeight = defaultIntrinsicHeight;
    private int alpha;

    @Implementation
    public static Drawable createFromStream(InputStream is, String srcName) {
        BitmapDrawable drawable = new BitmapDrawable(Robolectric.newInstanceOf(Bitmap.class));
        shadowOf(drawable).setSource(srcName);
        return drawable;
    }

    @Implementation
    public final Rect getBounds() {
        return bounds;
    }

    @Implementation
    public void setBounds(Rect rect) {
        this.bounds = rect;
    }

    @Implementation
    public void setBounds(int left, int top, int right, int bottom) {
        bounds = new Rect(left, top, right, bottom);
    }

    @Implementation
    public int getIntrinsicWidth() {
        return intrinsicWidth;
    }

    @Implementation
    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    public static void setDefaultIntrinsicWidth(int defaultIntrinsicWidth) {
        ShadowDrawable.defaultIntrinsicWidth = defaultIntrinsicWidth;
    }

    public static void setDefaultIntrinsicHeight(int defaultIntrinsicHeight) {
        ShadowDrawable.defaultIntrinsicHeight = defaultIntrinsicHeight;
    }

    public void setIntrinsicWidth(int intrinsicWidth) {
        this.intrinsicWidth = intrinsicWidth;
    }

    public void setIntrinsicHeight(int intrinsicHeight) {
        this.intrinsicHeight = intrinsicHeight;
    }

    @Override @Implementation
    public boolean equals(Object o) {
        if (realObject == o) return true;
        if (o == null || realObject.getClass() != o.getClass()) return false;

        ShadowDrawable that = shadowOf((Drawable) o);

        if (intrinsicHeight != that.intrinsicHeight) return false;
        if (intrinsicWidth != that.intrinsicWidth) return false;
        if (bounds != null ? !bounds.equals(that.bounds) : that.bounds != null) return false;

        return true;
    }

    @Override @Implementation
    public int hashCode() {
        int result = bounds != null ? bounds.hashCode() : 0;
        result = 31 * result + intrinsicWidth;
        result = 31 * result + intrinsicHeight;
        return result;
    }

    @Implementation
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public int getAlpha() {
        return alpha;
    }
}

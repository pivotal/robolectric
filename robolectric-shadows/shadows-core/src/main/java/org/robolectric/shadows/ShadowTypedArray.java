package org.robolectric.shadows;

import android.content.res.Resources;
import android.content.res.TypedArray;
import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.HiddenApi;
import org.robolectric.util.ReflectionHelpers;

import static org.robolectric.util.ReflectionHelpers.ClassParameter.*;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(TypedArray.class)
public class ShadowTypedArray {
  @RealObject private TypedArray realTypedArray;
  private CharSequence[] stringData;
  public String positionDescription;

  public static TypedArray create(Resources realResources, int[] attrs, int[] data, int[] indices, int len, CharSequence[] stringData) {
    TypedArray typedArray = ReflectionHelpers.callConstructorReflectively(TypedArray.class,
        from(Resources.class, realResources),
        from(data), from(indices), from(len));
    Shadows.shadowOf(typedArray).stringData = stringData;
    return typedArray;
  }

  @HiddenApi @Implementation
  public CharSequence loadStringValueAt(int index) {
    return stringData[index / ShadowAssetManager.STYLE_NUM_ENTRIES];
  }

  @Implementation
  public String getPositionDescription() {
    return positionDescription;
  }
}

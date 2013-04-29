package org.robolectric.shadows;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.TestRunners;

import static org.junit.Assert.*;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(TestRunners.WithDefaults.class)
public class BitmapTest {
    @Test
    public void shouldCreateScaledBitmap() throws Exception {
        Bitmap originalBitmap = create("Original bitmap");
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 200, false);
        assertEquals("Original bitmap scaled to 100 x 200", shadowOf(scaledBitmap).getDescription());
        assertEquals(100, scaledBitmap.getWidth());
        assertEquals(200, scaledBitmap.getHeight());
    }

    @Test
    public void shouldCreateActiveBitmap() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
        assertFalse(bitmap.isRecycled());
    }

    @Test
    public void shouldCreateBitmapWithCorrectConfig() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
        assertEquals(bitmap.getConfig(), Config.ARGB_8888);
    }

    @Test
    public void shouldCreateBitmapFromAnotherBitmap() {
    	Bitmap originalBitmap = create("Original bitmap");
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap);
        assertEquals("Original bitmap created from Bitmap object", shadowOf(newBitmap).getDescription());
    }

    @Test
    public void shouldRecycleBitmap() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
        bitmap.recycle();
        assertTrue(bitmap.isRecycled());
    }

    @Test
    public void equals_shouldCompareDescriptions() throws Exception {
        assertFalse(create("bitmap A").equals(create("bitmap B")));
        assertTrue(create("bitmap A").equals(create("bitmap A")));
    }

    @Test
    public void equals_shouldCompareWidthAndHeight() throws Exception {
        Bitmap bitmapA1 = create("bitmap A");
        shadowOf(bitmapA1).setWidth(100);
        shadowOf(bitmapA1).setHeight(100);

        Bitmap bitmapA2 = create("bitmap A");
        shadowOf(bitmapA2).setWidth(101);
        shadowOf(bitmapA2).setHeight(101);

        assertFalse(bitmapA1.equals(bitmapA2));
    }

    @Test
    public void shouldReceiveDescriptionWhenDrawingToCanvas() throws Exception {
        Bitmap bitmap1 = create("Bitmap One");
        Bitmap bitmap2 = create("Bitmap Two");

        Canvas canvas = new Canvas(bitmap1);
        canvas.drawBitmap(bitmap2, 0, 0, null);

        assertEquals("Bitmap One\nBitmap Two", shadowOf(bitmap1).getDescription());
    }

    @Test
    public void shouldReceiveDescriptionWhenDrawingToCanvasWithBitmapAndMatrixAndPaint() throws Exception {
        Bitmap bitmap1 = create("Bitmap One");
        Bitmap bitmap2 = create("Bitmap Two");

        Canvas canvas = new Canvas(bitmap1);
        canvas.drawBitmap(bitmap2, new Matrix(), null);

        assertEquals("Bitmap One\nBitmap Two transformed by matrix", shadowOf(bitmap1).getDescription());
    }

    @Test
    public void shouldReceiveDescriptionWhenDrawABitmapToCanvasWithAPaintEffect() throws Exception {
        Bitmap bitmap1 = create("Bitmap One");
        Bitmap bitmap2 = create("Bitmap Two");

        Canvas canvas = new Canvas(bitmap1);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix()));
        canvas.drawBitmap(bitmap2, new Matrix(), paint);

        assertEquals("Bitmap One\nBitmap Two with ColorMatrixColorFilter<1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0> transformed by matrix", shadowOf(bitmap1).getDescription());
    }

    @Test
    public void visualize_shouldReturnDescription() throws Exception {
        Bitmap bitmap = create("Bitmap One");
        assertEquals("Bitmap One", Robolectric.visualize(bitmap));
    }

    @Test
    public void shouldCopyBitmap() {
        Bitmap bitmap = Robolectric.newInstanceOf(Bitmap.class);
        Bitmap bitmapCopy = bitmap.copy(Config.ARGB_8888, true);
        assertEquals(shadowOf(bitmapCopy).getConfig(), Config.ARGB_8888);
        assertTrue(shadowOf(bitmapCopy).isMutable());
    }

    private static Bitmap create(String name) {
      Bitmap bitmap = Robolectric.newInstanceOf(Bitmap.class);
      shadowOf(bitmap).appendDescription(name);
      return bitmap;
    }
}

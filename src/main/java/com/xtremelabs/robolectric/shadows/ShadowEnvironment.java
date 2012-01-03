package com.xtremelabs.robolectric.shadows;

import java.io.File;

import android.os.Environment;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

@Implements(Environment.class)
public class ShadowEnvironment {

    private static final String MEDIA_REMOVED = "removed";

    private static String externalStorageState = MEDIA_REMOVED;
    private static File externalStorageDirectory;

    @Implementation
    public static String getExternalStorageState() {
        return externalStorageState;
    }

    public static void setExternalStorageState(String externalStorageState) {
        ShadowEnvironment.externalStorageState = externalStorageState;
    }
    
    @Implementation
    public static File getExternalStorageDirectory() {
    	return externalStorageDirectory;
    }
    
    public static void setExternalStorageDirectory(File externalStorageDirectory) {
    	ShadowEnvironment.externalStorageDirectory = externalStorageDirectory;
    }
}

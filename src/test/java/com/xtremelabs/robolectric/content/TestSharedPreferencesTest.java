package com.xtremelabs.robolectric.content;

import android.content.SharedPreferences;
import com.xtremelabs.robolectric.tester.android.content.TestSharedPreferences;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestSharedPreferencesTest {
    private HashMap<String, Map<String, Object>> content;
    private SharedPreferences.Editor editor;
    TestSharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        content = new HashMap<String, Map<String, Object>>();

        sharedPreferences = new TestSharedPreferences(content, "prefsName", 3);
        editor = sharedPreferences.edit();
        editor.putBoolean("boolean", true);
        editor.putFloat("float", 1.1f);
        editor.putInt("int", 2);
        editor.putLong("long", 3l);
        editor.putString("string", "foobar");
    }

    @Test
    public void commit_shouldStoreValues() throws Exception {
        editor.commit();

        TestSharedPreferences anotherSharedPreferences = new TestSharedPreferences(content, "prefsName", 3);
        assertTrue(anotherSharedPreferences.getBoolean("boolean", false));
        assertThat(anotherSharedPreferences.getFloat("float", 666f), equalTo(1.1f));
        assertThat(anotherSharedPreferences.getInt("int", 666), equalTo(2));
        assertThat(anotherSharedPreferences.getLong("long", 666l), equalTo(3l));
        assertThat(anotherSharedPreferences.getString("string", "wacka wa"), equalTo("foobar"));
    }

    @Test
    public void getAll_shouldReturnAllValues() throws Exception {
        editor.commit();
        Map<String, ?> all = sharedPreferences.getAll();
        assertThat(all.size(), equalTo(5));
        assertThat((Integer) all.get("int"), equalTo(2));
    }

    @Test
    public void commit_shouldRemoveValues() throws Exception {
        editor.putString("deleteMe", "foobar");
        editor.remove("deleteMe");

        editor.putString("dontDeleteMe", "quux");
        editor.remove("dontDeleteMe");
        editor.putString("dontDeleteMe", "baz");

        editor.commit();

        TestSharedPreferences anotherSharedPreferences = new TestSharedPreferences(content, "prefsName", 3);
        assertTrue(anotherSharedPreferences.getBoolean("boolean", false));
        assertThat(anotherSharedPreferences.getFloat("float", 666f), equalTo(1.1f));
        assertThat(anotherSharedPreferences.getInt("int", 666), equalTo(2));
        assertThat(anotherSharedPreferences.getLong("long", 666l), equalTo(3l));
        assertThat(anotherSharedPreferences.getString("string", "wacka wa"), equalTo("foobar"));

        assertThat(anotherSharedPreferences.contains("deleteMe"), equalTo(false));
        assertThat(anotherSharedPreferences.getString("dontDeleteMe", "oops"), equalTo("baz"));
    }

    @Test
    public void apply_shouldStoreValues() throws Exception {
        editor.apply();

        TestSharedPreferences anotherSharedPreferences = new TestSharedPreferences(content, "prefsName", 3);
        assertThat(anotherSharedPreferences.getString("string", "wacka wa"), equalTo("foobar"));
    }

    @Test
    public void shouldReturnDefaultValues() throws Exception {
        TestSharedPreferences anotherSharedPreferences = new TestSharedPreferences(content, "bazBang", 3);

        assertFalse(anotherSharedPreferences.getBoolean("boolean", false));
        assertThat(anotherSharedPreferences.getFloat("float", 666f), equalTo(666f));
        assertThat(anotherSharedPreferences.getInt("int", 666), equalTo(666));
        assertThat(anotherSharedPreferences.getLong("long", 666l), equalTo(666l));
        assertThat(anotherSharedPreferences.getString("string", "wacka wa"), equalTo("wacka wa"));
    }
}

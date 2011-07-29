package com.xtremelabs.robolectric.shadows;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.view.View;
import com.xtremelabs.robolectric.R;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.WithTestDefaultsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(WithTestDefaultsRunner.class)
public class AlertDialogTest {

    @Test
    public void testBuilder() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextWrapper(null));
        builder.setTitle("title").setMessage("message");
        builder.setCancelable(true);
        AlertDialog alert = builder.create();
        alert.show();

        assertThat(alert.isShowing(), equalTo(true));

        ShadowAlertDialog shadowAlertDialog = shadowOf(alert);
        assertEquals("title", shadowAlertDialog.getTitle());
        assertThat(shadowAlertDialog.getMessage(), equalTo("message"));
        assertThat(shadowAlertDialog.isCancelable(), equalTo(true));
        assertThat(shadowOf(ShadowAlertDialog.getLatestAlertDialog()), sameInstance(shadowAlertDialog));
        assertThat(ShadowAlertDialog.getLatestAlertDialog(), sameInstance(alert));
    }

    @Test
    public void getLatestAlertDialog_shouldReturnARealAlertDialog() throws Exception {
        assertThat(ShadowAlertDialog.getLatestAlertDialog(), nullValue());

        AlertDialog dialog = new AlertDialog.Builder(new ContextWrapper(null)).show();
        assertThat(ShadowAlertDialog.getLatestAlertDialog(), sameInstance(dialog));
    }

    @Test
    public void shouldAllowNullButtonListeners() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextWrapper(null));
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        Robolectric.clickOn(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
    }

    @Test
    public void testSetMessageAfterCreation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextWrapper(null));
        builder.setTitle("title").setMessage("message");
        AlertDialog alert = builder.create();

        ShadowAlertDialog shadowAlertDialog = shadowOf(alert);
        assertThat(shadowAlertDialog.getMessage(), equalTo("message"));

        shadowAlertDialog.setMessage("new message");
        assertThat(shadowAlertDialog.getMessage(), equalTo("new message"));

        shadowAlertDialog.setMessage(null);
        assertThat(shadowAlertDialog.getMessage(), nullValue());
    }

    @Test
    public void shouldSetMessageFromResourceId() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(new Activity());
        builder.setTitle("title").setMessage(R.string.hello);

        AlertDialog alert = builder.create();
        ShadowAlertDialog shadowAlertDialog = shadowOf(alert);
        assertThat(shadowAlertDialog.getMessage(), equalTo("Hello"));
    }

    @Test
    public void testBuilderWithItemArrayViaResourceId() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextWrapper(Robolectric.application));

        builder.setTitle("title");
        builder.setItems(R.array.alertDialogTestItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {

                } else if (item == 1) {

                }
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

        assertThat(alert.isShowing(), equalTo(true));

        ShadowAlertDialog shadowAlertDialog = shadowOf(alert);
        assertThat(shadowAlertDialog.getTitle().toString(), equalTo("title"));
        assertThat(shadowAlertDialog.getItems().length, equalTo(2));
        assertEquals(shadowAlertDialog.getItems()[0], "Aloha");
        assertThat(shadowOf(ShadowAlertDialog.getLatestAlertDialog()), sameInstance(shadowAlertDialog));
        assertThat(ShadowAlertDialog.getLatestAlertDialog(), sameInstance(alert));
    }

    @Test
    public void show_setsLatestAlertDialogAndLatestDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(Robolectric.application).create();
        assertNull(ShadowDialog.getLatestDialog());
        assertNull(ShadowAlertDialog.getLatestAlertDialog());

        alertDialog.show();

        assertEquals(alertDialog, ShadowDialog.getLatestDialog());
        assertEquals(alertDialog, ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void shouldReturnTheIndexOfTheCheckedItemInASingleChoiceDialog() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextWrapper(Robolectric.application));

        builder.setSingleChoiceItems(new String[]{"foo", "bar"}, 1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {

            }
        });
        AlertDialog alert = builder.create();
        alert.show();

        assertThat(alert.isShowing(), equalTo(true));

        ShadowAlertDialog shadowAlertDialog = shadowOf(alert);
        assertEquals(shadowAlertDialog.getCheckedItemIndex(), 1);
        assertEquals(shadowAlertDialog.getItems()[0], "foo");
        assertThat(shadowAlertDialog.getItems().length, equalTo(2));
        assertThat(ShadowAlertDialog.getLatestAlertDialog(), sameInstance(alert));
    }

    @Test
    public void shouldCallTheClickListenerOfTheCheckedItemInASingleChoiceDialog() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextWrapper(Robolectric.application));

        TestDialogOnClickListener listener = new TestDialogOnClickListener();
        builder.setSingleChoiceItems(new String[]{"foo", "bar"}, 1, listener);

        AlertDialog alert = builder.create();
        alert.show();

        ShadowAlertDialog shadowAlertDialog = shadowOf(alert);
        shadowAlertDialog.clickOnItem(0);
        assertThat(listener.clickedItem, equalTo(0));
        assertThat(shadowAlertDialog.getCheckedItemIndex(), equalTo(0));
    }

    @Test
    public void shouldFindViewsByIdIfAViewIsSet() throws Exception {
        ContextWrapper context = new ContextWrapper(null);
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        
        assertThat(dialog.findViewById(99), nullValue());

        View view = new View(context);
        view.setId(99);
        dialog.setView(view);
        assertThat(dialog.findViewById(99), sameInstance(view));
        
        assertThat(dialog.findViewById(66), nullValue());
    }
    
    private static class TestDialogOnClickListener implements DialogInterface.OnClickListener {
        private DialogInterface dialog;
        private int clickedItem;

        public void onClick(DialogInterface dialog, int item) {
            this.dialog = dialog;
            this.clickedItem = item;
        }
    }
}

package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShadowMessengerTest {

  @Test
  public void testMessengerSend() throws Exception {
    Handler handler = new Handler();
    Messenger messenger = new Messenger(handler);

    ShadowLooper.pauseMainLooper();
    Message msg = Message.obtain(null, 123);
    messenger.send(msg);

    assertThat(handler.hasMessages(123)).isTrue();
    ShadowHandler.runMainLooperOneTask();
    assertThat(handler.hasMessages(123)).isFalse();
  }

  @Test
  public void getLastMessageSentShouldWork() throws Exception {
    Handler handler = new Handler();
    Messenger messenger = new Messenger(handler);
    Message msg = Message.obtain(null, 123);
    messenger.send(msg);

    assertThat(ShadowMessenger.getLastMessageSent()).isEqualTo(msg);
  }

  @Test
  public void clearLastMessageSentShouldWork() throws Exception {
    Handler handler = new Handler();
    Messenger messenger = new Messenger(handler);
    Message msg = Message.obtain(null, 123);
    messenger.send(msg);
    ShadowMessenger.clearLastMessageSent();

    assertThat(ShadowMessenger.getLastMessageSent()).isNull();
  }

  @Test
  public void createMessengerWithBinder_getLastMessageSentShouldWork() throws Exception {
    Handler handler = new Handler();
    Messenger messenger = new Messenger(new Messenger(handler).getBinder());

    Message msg = Message.obtain(null, 123);
    messenger.send(msg);

    assertThat(ShadowMessenger.getLastMessageSent()).isEqualTo(msg);
  }
}

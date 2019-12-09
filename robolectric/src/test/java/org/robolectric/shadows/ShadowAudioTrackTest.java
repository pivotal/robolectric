package org.robolectric.shadows;

import static android.media.AudioTrack.ERROR_BAD_VALUE;
import static android.media.AudioTrack.WRITE_BLOCKING;
import static android.media.AudioTrack.WRITE_NON_BLOCKING;
import static android.os.Build.VERSION_CODES.Q;
import static com.google.common.truth.Truth.assertThat;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Tests for {@link ShadowAudioTrack}. */
@RunWith(AndroidJUnit4.class)
@Config(minSdk = Q)
public class ShadowAudioTrackTest {

  private static final int SAMPLE_RATE_IN_HZ = 44100;
  private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
  private static final int AUDIO_ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

  @Test
  public void setMinBufferSize() {
    int originalMinBufferSize =
        AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_ENCODING_FORMAT);
    ShadowAudioTrack.setMinBufferSize(512);
    int newMinBufferSize =
        AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_ENCODING_FORMAT);

    assertThat(originalMinBufferSize).isEqualTo(ShadowAudioTrack.DEFAULT_MIN_BUFFER_SIZE);
    assertThat(newMinBufferSize).isEqualTo(512);
  }

  @Test
  public void writeByteArray_blocking() {
    AudioTrack audioTrack = getSampleAudioTrack();

    int written = audioTrack.write(new byte[] {0, 0, 0, 0}, 0, 2);

    assertThat(written).isEqualTo(2);
  }

  @Test
  public void writeByteArray_nonBlocking() {
    AudioTrack audioTrack = getSampleAudioTrack();

    int written = audioTrack.write(new byte[] {0, 0, 0, 0}, 0, 2, WRITE_NON_BLOCKING);

    assertThat(written).isEqualTo(2);
  }

  @Test
  public void writeByteBuffer_blocking() {
    AudioTrack audioTrack = getSampleAudioTrack();
    ByteBuffer byteBuffer = ByteBuffer.allocate(4);

    int written = audioTrack.write(byteBuffer, 2, WRITE_BLOCKING);

    assertThat(written).isEqualTo(2);
  }

  @Test
  public void writeByteBuffer_nonBlocking() {
    AudioTrack audioTrack = getSampleAudioTrack();
    ByteBuffer byteBuffer = ByteBuffer.allocate(4);

    int written = audioTrack.write(byteBuffer, 2, WRITE_NON_BLOCKING);

    assertThat(written).isEqualTo(2);
  }

  @Test
  public void writeDirectByteBuffer_blocking() {
    AudioTrack audioTrack = getSampleAudioTrack();
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);

    int written = audioTrack.write(byteBuffer, 2, WRITE_BLOCKING);

    assertThat(written).isEqualTo(2);
  }

  @Test
  public void writeDirectByteBuffer_nonBlocking() {
    AudioTrack audioTrack = getSampleAudioTrack();
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);

    int written = audioTrack.write(byteBuffer, 2, WRITE_NON_BLOCKING);

    assertThat(written).isEqualTo(2);
  }

  @Test
  public void writeDirectByteBuffer_invalidWriteMode() {
    AudioTrack audioTrack = getSampleAudioTrack();
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);

    int written = audioTrack.write(byteBuffer, 2, 5);

    assertThat(written).isEqualTo(ERROR_BAD_VALUE);
  }

  @Test
  public void writeDirectByteBuffer_invalidSize() {
    AudioTrack audioTrack = getSampleAudioTrack();
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);

    int written = audioTrack.write(byteBuffer, 10, WRITE_NON_BLOCKING);

    assertThat(written).isEqualTo(ERROR_BAD_VALUE);
  }

  private static AudioTrack getSampleAudioTrack() {
    return new AudioTrack.Builder()
        .setAudioAttributes(
            new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
        .setAudioFormat(
            new AudioFormat.Builder()
                .setEncoding(AUDIO_ENCODING_FORMAT)
                .setSampleRate(SAMPLE_RATE_IN_HZ)
                .setChannelMask(CHANNEL_CONFIG)
                .build())
        .build();
  }
}

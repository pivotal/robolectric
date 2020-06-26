package org.robolectric.shadows;

import static android.media.AudioRecord.ERROR_BAD_VALUE;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioSystem;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

/**
 * Shadow {@link AudioRecord} which by default will fulfil any requests for audio data by completely
 * filling any requested buffers.
 *
 * <p>It is also possible to provide the underlying data by implementing {@link AudioRecordSource}
 * and setting this via {@link #setSource(AudioRecordSource)}.
 */
@Implements(value = AudioRecord.class, minSdk = LOLLIPOP)
public final class ShadowAudioRecord {

  private static final AudioRecordSource DEFAULT_SOURCE = new AudioRecordSource() {};

  private static final AtomicReference<AudioRecordSource> source =
      new AtomicReference<>(DEFAULT_SOURCE);

  public static void setSource(AudioRecordSource source) {
    ShadowAudioRecord.source.set(source);
  }

  @Resetter
  public static void clearSource() {
    source.set(DEFAULT_SOURCE);
  }

  @Implementation
  protected static int native_get_min_buff_size(
      int sampleRateInHz, int channelCount, int audioFormat) {
    int frameSize;
    switch (audioFormat) {
      case AudioFormat.ENCODING_PCM_16BIT:
        frameSize = 2;
        break;
      case AudioFormat.ENCODING_PCM_FLOAT:
        frameSize = 2 * channelCount;
        break;
      default:
        return ERROR_BAD_VALUE;
    }
    return frameSize * (sampleRateInHz / 4); // Approx quarter of a second sample per buffer
  }

  @Implementation
  protected int native_start(int syncEvent, int sessionId) {
    return AudioSystem.SUCCESS;
  }

  protected int native_read_in_byte_array(byte[] audioData, int offsetInBytes, int sizeInBytes) {
    return native_read_in_byte_array(audioData, offsetInBytes, sizeInBytes, true);
  }

  @Implementation(minSdk = M)
  protected int native_read_in_byte_array(
      byte[] audioData, int offsetInBytes, int sizeInBytes, boolean isBlocking) {
    return source.get().readInByteArray(audioData, offsetInBytes, sizeInBytes, isBlocking);
  }

  protected int native_read_in_short_array(
      short[] audioData, int offsetInShorts, int sizeInShorts) {
    return native_read_in_short_array(audioData, offsetInShorts, sizeInShorts, true);
  }

  @Implementation(minSdk = M)
  protected int native_read_in_short_array(
      short[] audioData, int offsetInShorts, int sizeInShorts, boolean isBlocking) {
    return source.get().readInShortArray(audioData, offsetInShorts, sizeInShorts, isBlocking);
  }

  @Implementation(minSdk = M)
  protected int native_read_in_float_array(
      float[] audioData, int offsetInFloats, int sizeInFloats, boolean isBlocking) {
    return source.get().readInFloatArray(audioData, offsetInFloats, sizeInFloats, isBlocking);
  }

  protected int native_read_in_direct_buffer(Object jBuffer, int sizeInBytes) {
    return native_read_in_direct_buffer(jBuffer, sizeInBytes, true);
  }

  @Implementation(minSdk = M)
  protected int native_read_in_direct_buffer(Object jBuffer, int sizeInBytes, boolean isBlocking) {
    // Note, in the real implementation the buffers position is not adjusted during the
    // read, so use duplicate to ensure the real implementation is matched.
    return source
        .get()
        .readInDirectBuffer(((ByteBuffer) jBuffer).duplicate(), sizeInBytes, isBlocking);
  }

  /** Provides underlying data for the {@link ShadowAudioRecord}. */
  public interface AudioRecordSource {

    /**
     * Provides backing data for {@link AudioRecord#read(byte[], int, int)} and {@link
     * AudioRecord#read(byte[], int, int, int)}.
     *
     * @return Either a non-negative value representing number of bytes that have been written from
     *     the offset or a negative error code.
     */
    default int readInByteArray(
        byte[] audioData, int offsetInBytes, int sizeInBytes, boolean isBlocking) {
      return sizeInBytes;
    }

    /**
     * Provides backing data for {@link AudioRecord#read(short[], int, int)} and {@link
     * AudioRecord#read(short[], int, int, int)}.
     *
     * @return Either a non-negative value representing number of bytes that have been written from
     *     the offset or a negative error code.
     */
    default int readInShortArray(
        short[] audioData, int offsetInShorts, int sizeInShorts, boolean isBlocking) {
      return sizeInShorts;
    }

    /**
     * Provides backing data for {@link AudioRecord#read(float[], int, int, int)}.
     *
     * @return Either a non-negative value representing number of bytes that have been written from
     *     the offset or a negative error code.
     */
    default int readInFloatArray(
        float[] audioData, int offsetInFloats, int sizeInFloats, boolean isBlocking) {
      return sizeInFloats;
    }

    /**
     * Provides backing data for {@link AudioRecord#read(byte[], int, int)} and {@link
     * AudioRecord#read(byte[], int, int, int)}.
     *
     * @return Either a non-negative value representing number of bytes that have been written from
     *     the offset or a negative error code. Note any position/limit changes to the buffer will
     *     not be visible to the caller of the AudioRecord methods.
     */
    default int readInDirectBuffer(ByteBuffer buffer, int sizeInBytes, boolean isBlocking) {
      int maxBytes = Math.min(buffer.remaining(), sizeInBytes);
      ((Buffer) buffer).position(buffer.position() + maxBytes);
      return maxBytes;
    }
  }
}

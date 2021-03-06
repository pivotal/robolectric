package org.robolectric.shadows;

import android.os.Build;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.TempDirectory;

/**
 * A {@link SharedMemory} fake that uses a private temporary disk file for storage and Java's {@link
 * MappedByteBuffer} for the memory mappings.
 */
@Implements(
    value = SharedMemory.class,
    minSdk = Build.VERSION_CODES.O_MR1,
    /* not quite true, but this prevents a useless `shadowOf()` accessor showing
     * up, which would break people compiling against API 26 and earlier.
     */
    isInAndroidSdk = false)
public class ShadowSharedMemory {
  private static final Map<Integer, File> filesByFd = new ConcurrentHashMap<>();

  private static final AtomicReference<ErrnoException> fakeCreateException =
      new AtomicReference<>();

  @RealObject private SharedMemory realObject;

  @Resetter
  public static void reset() {
    filesByFd.clear();
  }

  /**
   * Only works on {@link SharedMemory} instances from {@link SharedMemory#create}.
   *
   * <p>"prot" is ignored -- all mappings are read/write.
   */
  @Implementation
  protected ByteBuffer map(int prot, int offset, int length) throws ErrnoException {
    ReflectionHelpers.callInstanceMethod(realObject, "checkOpen");
    FileDescriptor fileDescriptor = getRealFileDescriptor();
    int fd = ReflectionHelpers.getField(fileDescriptor, "fd");
    File file = filesByFd.get(fd);
    if (file == null) {
      throw new IllegalStateException("Cannot find the backing file from fd");
    }

    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
      // It would be easy to support a MapMode.READ_ONLY type mapping as well except none of the
      // OsConstants fields are even initialized by robolectric and so "prot" is always zero!
      return randomAccessFile.getChannel().map(MapMode.READ_WRITE, offset, length);
    } catch (IOException e) {
      throw new ErrnoException(e.getMessage(), OsConstants.EIO, e);
    }
  }

  @Implementation
  protected static void unmap(ByteBuffer mappedBuf) throws ErrnoException {
    if (mappedBuf instanceof MappedByteBuffer) {
      // There's no API to clean up a MappedByteBuffer other than GC so we've got nothing to do!
    } else {
      throw new IllegalArgumentException(
          "ByteBuffer wasn't created by #map(int, int, int); can't unmap");
    }
  }

  @Implementation
  protected static FileDescriptor nCreate(String name, int size) throws ErrnoException {
    maybeThrow(fakeCreateException);

    TempDirectory tempDirectory = RuntimeEnvironment.getTempDirectory();

    try {
      // Give each instance its own private file.
      File sharedMemoryFile =
          Files.createTempFile(
                  tempDirectory.createIfNotExists("SharedMemory"), "shmem-" + name, ".tmp")
              .toFile();
      RandomAccessFile randomAccessFile = new RandomAccessFile(sharedMemoryFile, "rw");
      randomAccessFile.setLength(0);
      randomAccessFile.setLength(size);

      FileDescriptor fileDescriptor = randomAccessFile.getFD();
      int fd = ReflectionHelpers.getField(fileDescriptor, "fd");
      filesByFd.put(fd, sharedMemoryFile);
      return fileDescriptor;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file descriptior", e);
    }
  }

  @Implementation
  protected static int nGetSize(FileDescriptor fd) {
    int internalFd = ReflectionHelpers.getField(fd, "fd");
    return (int) filesByFd.get(internalFd).length();
  }

  private FileDescriptor getRealFileDescriptor() {
    return ReflectionHelpers.getField(realObject, "mFileDescriptor");
  }

  /**
   * Causes subsequent calls to {@link SharedMemory#create)} to throw the specified exception, if
   * non-null. Pass null to restore create to normal operation.
   */
  public static void setCreateShouldThrow(ErrnoException e) {
    fakeCreateException.set(e);
  }

  private static void maybeThrow(AtomicReference<ErrnoException> exceptionRef)
      throws ErrnoException {
    ErrnoException exception = exceptionRef.get();
    if (exception != null) {
      throw exception;
    }
  }
}

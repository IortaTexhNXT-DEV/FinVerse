package com.iortatechnxt.brokerverse.common.office;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Decompression limits of every Office Open XML and OpenDocument file BIBS opens from outside
 * (uploads, attachments, edited templates): a file is a ZIP archive, and a small archive can expand
 * to gigabytes (a "zip bomb"). Every such file is opened through this class.
 *
 * <ul>
 *   <li>at most {@value #MAX_ENTRIES} parts in one package;
 *   <li>at most {@value #MAX_ENTRY_BYTES} bytes for one uncompressed part (a sheet, {@code
 *       content.xml});
 *   <li>a part may expand at most 100 times its compressed size ({@link #MIN_INFLATE_RATIO}; Apache
 *       POI checks it per part above a grace size of 100 KB, the OpenDocument reader against the
 *       size of the whole file).
 * </ul>
 *
 * <p>Apache POI keeps its limits in static settings ({@link ZipSecureFile}); they are applied when
 * this class is loaded, so they hold before the first file is opened through it.
 */
public final class OfficeFileLimits {

  /** Largest uncompressed part of a package, in bytes (64 MiB). */
  public static final long MAX_ENTRY_BYTES = 64L * 1024 * 1024;

  /** Most parts (entries) in one package. */
  public static final int MAX_ENTRIES = 1000;

  /** Smallest compressed / uncompressed ratio of a part (1 : 100). */
  public static final double MIN_INFLATE_RATIO = 0.01;

  /** Code of the refusal. */
  public static final String LIMIT_CODE = "FILE_ARCHIVE_LIMIT";

  private static final long RATIO = Math.round(1 / MIN_INFLATE_RATIO);
  private static final long GRACE_BYTES = 100L * 1024;

  static {
    ZipSecureFile.setMinInflateRatio(MIN_INFLATE_RATIO);
    ZipSecureFile.setMaxEntrySize(MAX_ENTRY_BYTES);
    ZipSecureFile.setMaxFileCount(MAX_ENTRIES);
  }

  private OfficeFileLimits() {}

  /**
   * Opens an Excel workbook within the limits.
   *
   * @param content xlsx bytes
   * @return workbook (to be closed by the caller)
   * @throws IOException when the file is not a workbook or breaks a limit
   */
  public static XSSFWorkbook workbook(byte[] content) throws IOException {
    return new XSSFWorkbook(new ByteArrayInputStream(content));
  }

  /**
   * Opens a Word document within the limits.
   *
   * @param content docx bytes
   * @return document (to be closed by the caller)
   * @throws IOException when the file is not a document or breaks a limit
   */
  public static XWPFDocument wordDocument(byte[] content) throws IOException {
    return new XWPFDocument(new ByteArrayInputStream(content));
  }

  /**
   * Opens an Office Open XML package within the limits.
   *
   * @param content package bytes
   * @return package (to be closed by the caller)
   * @throws IOException when the file breaks a limit
   * @throws InvalidFormatException when the file is not an Office Open XML package
   */
  public static OPCPackage officePackage(byte[] content)
      throws IOException, InvalidFormatException {
    return OPCPackage.open(new ByteArrayInputStream(content));
  }

  /**
   * Refuses a package with too many parts.
   *
   * @param entriesSeen parts read so far
   */
  public static void requireEntryCount(int entriesSeen) {
    if (entriesSeen > MAX_ENTRIES) {
      throw refusal("The file holds more than " + MAX_ENTRIES + " parts");
    }
  }

  /**
   * Wraps the stream of one uncompressed part so that reading stops with {@code FILE_ARCHIVE_LIMIT}
   * beyond {@link #MAX_ENTRY_BYTES} or beyond 100 times the size of the whole file (at least 100
   * KB).
   *
   * @param entry stream of the part (not closed by the wrapper)
   * @param archiveBytes size of the whole compressed file
   * @return bounded stream
   */
  public static InputStream boundedEntry(InputStream entry, long archiveBytes) {
    long limit = Math.min(MAX_ENTRY_BYTES, Math.max(GRACE_BYTES, archiveBytes * RATIO));
    return new BoundedStream(entry, limit);
  }

  private static BusinessRuleException refusal(String detail) {
    return new BusinessRuleException(
        LIMIT_CODE, detail + "; the file is refused as a possible compressed-file bomb");
  }

  /** Counts the bytes read and refuses to go beyond the limit. */
  private static final class BoundedStream extends FilterInputStream {

    private final long limit;
    private long read;

    BoundedStream(InputStream in, long limit) {
      super(in);
      this.limit = limit;
    }

    @Override
    public int read() throws IOException {
      int b = super.read();
      if (b >= 0) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = super.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    @Override
    public long skip(long n) throws IOException {
      long skipped = super.skip(n);
      count(skipped);
      return skipped;
    }

    @Override
    public void close() {
      // The part belongs to the archive stream, which the caller closes.
    }

    private void count(long n) {
      read += n;
      if (read > limit) {
        throw refusal("A part of the file expands beyond " + limit + " bytes");
      }
    }
  }
}

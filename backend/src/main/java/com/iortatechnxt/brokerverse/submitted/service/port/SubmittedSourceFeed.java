package com.iortatechnxt.brokerverse.submitted.service.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Port: scheduled pull of a source file of submitted policies from a bank system (LFS, HLS, CIU,
 * SPI, LAMD, Loan Booking Report, IA masterlist; BRIDSP-01, 13; SUBMITTED_POLICIES_DESIGN section
 * 2.2). The transports are parked (SP SQ01): the default adapter delivers nothing and the sources
 * are uploaded. An adapter returns the files of a source, which the intake loads like an upload.
 */
public interface SubmittedSourceFeed {

  /**
   * Files of a source available for a business date.
   *
   * @param companyId company
   * @param sourceCode source register code
   * @param businessDate business date
   * @return files, empty when none
   */
  List<FeedFile> pull(Long companyId, String sourceCode, LocalDate businessDate);

  /**
   * A file of a source.
   *
   * @param fileName file name
   * @param content file bytes
   */
  record FeedFile(String fileName, byte[] content) {

    /** Defensive copy. */
    public FeedFile {
      content = content == null ? new byte[0] : content.clone();
    }

    /**
     * The file bytes.
     *
     * @return a copy
     */
    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof FeedFile f
          && java.util.Objects.equals(fileName, f.fileName)
          && java.util.Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return 31 * java.util.Objects.hashCode(fileName) + java.util.Arrays.hashCode(content);
    }

    @Override
    public String toString() {
      return "FeedFile[" + fileName + ", " + content.length + " bytes]";
    }
  }
}

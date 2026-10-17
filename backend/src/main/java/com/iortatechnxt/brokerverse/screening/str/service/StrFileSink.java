package com.iortatechnxt.brokerverse.screening.str.service;

import java.util.List;

/**
 * Port: where the AMLC extraction file is saved (SNSRP-706; FR-SS-071; design section 10). The
 * "designated folder path" is BDOI's (SQ16); the default adapter keeps the file in the BIBS report
 * archive, downloaded from the STR screen. A shared-folder or SFTP adapter replaces it without a
 * change to the extraction.
 */
public interface StrFileSink {

  /**
   * Saves an extraction file.
   *
   * @param file the file
   * @return where it was saved
   */
  Delivered deliver(StrFile file);

  /**
   * An extraction file.
   *
   * @param fileName the file name
   * @param contentType the media type
   * @param content the bytes
   * @param echo the extraction facts (batch, period, count)
   * @param rows the number of STRs
   */
  record StrFile(String fileName, String contentType, byte[] content, List<String> echo, int rows) {

    /** Defensive copies. */
    public StrFile {
      content = content.clone();
      echo = List.copyOf(echo);
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof StrFile f && fileName.equals(f.fileName);
    }

    @Override
    public int hashCode() {
      return fileName.hashCode();
    }

    @Override
    public String toString() {
      return fileName;
    }
  }

  /**
   * Where a file was saved.
   *
   * @param reportRunId the report archive run holding the file, null for another destination
   * @param location a description of the destination
   */
  record Delivered(Long reportRunId, String location) {}
}

package com.iortatechnxt.finverse.attachment.service;

/**
 * Hook for malware scanning of uploaded files. Every bean implementing this interface is consulted
 * before a file is stored; a single "infected" verdict rejects the upload. The shipped {@link
 * NoOpVirusScanner} accepts everything; add a bean (e.g. a ClamAV client) to enable scanning.
 */
public interface VirusScanner {

  /**
   * Scans a file.
   *
   * @param fileName file name
   * @param content file bytes
   * @return verdict
   */
  ScanVerdict scan(String fileName, byte[] content);

  /**
   * Scan verdict.
   *
   * @param clean true when no threat was found
   * @param detail threat name or scanner remark
   */
  record ScanVerdict(boolean clean, String detail) {

    /**
     * Clean verdict.
     *
     * @return verdict
     */
    public static ScanVerdict ok() {
      return new ScanVerdict(true, null);
    }
  }
}

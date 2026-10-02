package com.iortatechnxt.brokerverse.storage.domain;

/**
 * Where the content of a stored file comes from, which decides its malware scan.
 *
 * <ul>
 *   <li>{@link #UPLOADED}: a file a user or another system supplied; it waits for the malware scan
 *       and cannot be downloaded before a clean result.
 *   <li>{@link #GENERATED}: a file the application produced itself (report output, slip, invoice,
 *       receipt print, bank file, e-mail attachment as sent). It is recorded clean with the result
 *       {@value #GENERATED_RESULT}: the malware scan covers the inbound bucket and uploads only,
 *       and protected e-mail attachments cannot be scanned.
 * </ul>
 */
public enum FileOrigin {
  UPLOADED,
  GENERATED;

  /** Scan result recorded for files the application generated. */
  public static final String GENERATED_RESULT = "APPLICATION_GENERATED";
}

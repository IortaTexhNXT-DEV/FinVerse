package com.iortatechnxt.brokerverse.storage.service;

/**
 * Port (SPI) told about every quarantined file, inside the transaction that records the quarantine.
 * The integration module implements it: an in-app notification to the uploader and to the holders
 * of {@code FILE_QUARANTINE_VIEW} (security role) and the alert {@code FILE_QUARANTINED}.
 */
public interface FileQuarantineListener {

  /**
   * A file was quarantined.
   *
   * @param file the facts
   */
  void quarantined(QuarantinedFile file);

  /**
   * Facts of a quarantined file (no content).
   *
   * @param fileId stored file id
   * @param companyId company
   * @param ownerEntityType owner entity type
   * @param ownerEntityId owner key
   * @param fileName file name
   * @param uploadedBy user who stored the file
   * @param scanResult scan result value, e.g. {@code THREATS_FOUND}
   */
  record QuarantinedFile(
      Long fileId,
      Long companyId,
      String ownerEntityType,
      String ownerEntityId,
      String fileName,
      String uploadedBy,
      String scanResult) {}
}

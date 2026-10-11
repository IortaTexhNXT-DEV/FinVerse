package com.iortatechnxt.brokerverse.migration.archive.service.port;

import java.util.Optional;

/**
 * Port of the place where the legacy documents of the archive arrive (DATA_MIGRATION_DESIGN
 * sections 3.2, 16 and 23): the document index (object H02) names each file of the transfer folder
 * with its size and SHA-256. The default reads the files staged in the console; the transfer-folder
 * adapter replaces it once BDOI IT names the folder.
 */
public interface LegacyDocumentSource {

  /**
   * The content of a legacy document.
   *
   * @param companyId company
   * @param sourceSystem legacy system
   * @param fileName file name in the transfer folder
   * @return the bytes, empty when the file is not there
   */
  Optional<byte[]> read(Long companyId, String sourceSystem, String fileName);
}

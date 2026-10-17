package com.iortatechnxt.brokerverse.opsledger.service.port;

import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import java.util.Arrays;

/**
 * Port to the shared-drive folders of Operations extracts (RMTID.001 one folder per remittance
 * type, PRCID extracts, CMRID.001 DP lists; OQ17). The default adapter is the in-system extract
 * repository ({@code ops_extract_file}, screen Operations - Interfaces - Extracts): a file with the
 * same folder and name replaces nothing and is refused.
 */
public interface FileDropPort {

  /**
   * Stores a file in a folder.
   *
   * @param companyId company
   * @param location folder and file name
   * @param file content type and bytes (the checksum is computed)
   * @param origin module and reference that produced it
   * @return the stored file's id and path
   */
  DroppedFile drop(
      Long companyId, ExtractFile.Location location, DropContent file, ExtractFile.Origin origin);

  /**
   * Content of a file to drop.
   *
   * @param contentType MIME type
   * @param bytes content
   */
  record DropContent(String contentType, byte[] bytes) {

    /** Defensive copy. */
    public DropContent {
      bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
      return bytes.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof DropContent d
          && contentType.equals(d.contentType)
          && Arrays.equals(bytes, d.bytes);
    }

    @Override
    public int hashCode() {
      return 31 * contentType.hashCode() + Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
      return "DropContent[" + contentType + ", " + bytes.length + " bytes]";
    }
  }

  /**
   * A stored file.
   *
   * @param id repository id
   * @param path folder/file name
   * @param sha256 checksum
   */
  record DroppedFile(Long id, String path, String sha256) {}
}

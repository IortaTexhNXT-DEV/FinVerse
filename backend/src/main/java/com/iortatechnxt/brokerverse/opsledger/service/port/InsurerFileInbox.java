package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Port to the insurers' files (OQ22, OQ29, OQ38): remittance schedules with ORs (RMTID.012),
 * production reports and feedback (PRCID.009), DP billing responses (CMRID.009). Insurer channels
 * (SFTP / API / mailbox) are parked; the default adapter has no inbox - users upload the files on
 * the module screens - and returns nothing.
 */
public interface InsurerFileInbox {

  /**
   * Transport of the adapter (MANUAL_UPLOAD by default).
   *
   * @return transport name
   */
  String transport();

  /**
   * Files received from an insurer and not yet processed.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param fileType REMIT_OR, PRODUCTION or DP_RESPONSE
   * @return files; always empty with the manual transport
   */
  List<InboxFile> pending(Long companyId, String insurerCode, String fileType);

  /**
   * A file received from an insurer.
   *
   * @param insurerCode insurer
   * @param fileType file type
   * @param fileName file name
   * @param receivedAt time received
   * @param content bytes
   */
  record InboxFile(
      String insurerCode, String fileType, String fileName, Instant receivedAt, byte[] content) {

    /** Defensive copy. */
    public InboxFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof InboxFile f
          && fileName.equals(f.fileName)
          && insurerCode.equals(f.insurerCode)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return 31 * fileName.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
      return "InboxFile[" + insurerCode + ", " + fileName + "]";
    }
  }
}

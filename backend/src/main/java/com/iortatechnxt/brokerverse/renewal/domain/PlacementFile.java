package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The consolidated placement file of an insurer (FRRN.029.02.02): one workbook of the packaged
 * renewal accounts placed with the insurer, with how it was sent.
 */
@Entity
@Table(name = "rnw_placement_file")
public class PlacementFile extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "line_code", length = 30, updatable = false)
  private String lineCode;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(nullable = false, updatable = false)
  private int accounts;

  @Column(length = 10)
  private String channel;

  @Column(name = "message_no", length = 30)
  private String messageNo;

  @Column(name = "transmission_status", length = 30)
  private String transmissionStatus;

  /** For JPA. */
  protected PlacementFile() {}

  /**
   * A generated placement file.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param lineCode product line
   * @param fileName file name
   * @param accounts number of renewal accounts
   */
  public PlacementFile(
      Long companyId, String insurerCode, String lineCode, String fileName, int accounts) {
    this.companyId = companyId;
    this.insurerCode = insurerCode;
    this.lineCode = lineCode;
    this.fileName = fileName;
    this.accounts = accounts;
  }

  /**
   * Records the stored file.
   *
   * @param id attachment
   */
  public void stored(Long id) {
    this.attachmentId = id;
  }

  /**
   * Records the sending.
   *
   * @param newChannel channel
   * @param message message
   * @param status transmission status
   */
  public void sent(String newChannel, String message, String status) {
    this.channel = newChannel;
    this.messageNo = message;
    this.transmissionStatus = status;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public int getAccounts() {
    return accounts;
  }

  public String getChannel() {
    return channel;
  }

  public String getMessageNo() {
    return messageNo;
  }

  public String getTransmissionStatus() {
    return transmissionStatus;
  }
}

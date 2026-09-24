package com.iortatechnxt.brokerverse.placement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A rendered placement slip file (PDF or XLSX) with its checksum. */
@Entity
@Table(name = "plc_slip_file")
public class SlipFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "slip_id", nullable = false, updatable = false)
  private Long slipId;

  @Column(nullable = false, length = 10, updatable = false)
  private String format;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(nullable = false, updatable = false)
  private byte[] content;

  protected SlipFile() {}

  /**
   * Stores a rendered file.
   *
   * @param slipId slip
   * @param format PDF or XLSX
   * @param fileName file name
   * @param sha256 checksum
   * @param content bytes
   */
  public SlipFile(Long slipId, String format, String fileName, String sha256, byte[] content) {
    this.slipId = slipId;
    this.format = format;
    this.fileName = fileName;
    this.sha256 = sha256;
    this.content = content.clone();
  }

  public Long getId() {
    return id;
  }

  public Long getSlipId() {
    return slipId;
  }

  public String getFormat() {
    return format;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public byte[] getContent() {
    return content.clone();
  }
}

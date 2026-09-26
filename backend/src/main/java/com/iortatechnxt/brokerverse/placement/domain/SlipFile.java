package com.iortatechnxt.brokerverse.placement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A rendered placement slip file (PDF or XLSX) with its checksum. The content is in the file store
 * ({@code stored_file}, build step ST1); files rendered before ST1 keep their bytes in {@code
 * content} until {@code FILE_BYTEA_MIGRATION} copies them.
 */
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

  @Column(name = "content", updatable = false)
  private byte[] content;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  protected SlipFile() {}

  /**
   * Records a rendered file whose content is in the file store.
   *
   * @param slipId slip
   * @param format PDF or XLSX
   * @param fileName file name
   * @param sha256 checksum
   * @param storedFileId stored file of the content
   */
  public SlipFile(Long slipId, String format, String fileName, String sha256, Long storedFileId) {
    this.slipId = slipId;
    this.format = format;
    this.fileName = fileName;
    this.sha256 = sha256;
    this.storedFileId = storedFileId;
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

  /**
   * The bytes of a file rendered before ST1 and not yet copied to the file store.
   *
   * @return bytes, null when the content is in the file store
   */
  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  /**
   * The stored file of the content; null for a file rendered before ST1 and not yet copied.
   *
   * @return stored file id
   */
  public Long getStoredFileId() {
    return storedFileId;
  }
}

package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * The layout of an STR_LAYOUT version (SNSRP-105, 706). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_str_layout")
public class StrLayoutRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, length = 10, updatable = false)
  private StrFormat format;

  @Column(name = "delimiter", length = 5, updatable = false)
  private String delimiter;

  @Column(name = "encoding", nullable = false, length = 20, updatable = false)
  private String encoding;

  /** For JPA. */
  protected StrLayoutRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param format file format
   * @param delimiter delimiter
   * @param encoding encoding
   */
  @SuppressWarnings("java:S107")
  public StrLayoutRow(Long versionId, StrFormat format, String delimiter, String encoding) {
    this.versionId = versionId;
    this.format = format;
    this.delimiter = delimiter;
    this.encoding = encoding;
  }

  public Long getVersionId() {
    return versionId;
  }

  public StrFormat getFormat() {
    return format;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public String getEncoding() {
    return encoding;
  }
}

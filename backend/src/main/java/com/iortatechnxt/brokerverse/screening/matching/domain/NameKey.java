package com.iortatechnxt.brokerverse.screening.matching.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A blocking key of a client or watchlist entry name (SNSRP-301; design 4.2 {@code scr_name_key}).
 * Keys are replaced as a whole when the subject's names change; the engine scores only pairs that
 * share a key.
 */
@Entity
@Table(name = "scr_name_key")
public class NameKey extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_kind", nullable = false, length = 10, updatable = false)
  private NameSubjectKind subjectKind;

  @Column(name = "subject_id", nullable = false, updatable = false)
  private Long subjectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "key_type", nullable = false, length = 10, updatable = false)
  private NameKeyType keyType;

  @Column(name = "key_value", nullable = false, length = 200, updatable = false)
  private String keyValue;

  /** For JPA. */
  protected NameKey() {}

  /**
   * Creates a key.
   *
   * @param subjectKind whose name
   * @param subjectId client or entry id
   * @param keyType kind of key
   * @param keyValue key value
   */
  public NameKey(
      NameSubjectKind subjectKind, Long subjectId, NameKeyType keyType, String keyValue) {
    this.subjectKind = subjectKind;
    this.subjectId = subjectId;
    this.keyType = keyType;
    this.keyValue = keyValue;
  }

  public NameSubjectKind getSubjectKind() {
    return subjectKind;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public NameKeyType getKeyType() {
    return keyType;
  }

  public String getKeyValue() {
    return keyValue;
  }
}

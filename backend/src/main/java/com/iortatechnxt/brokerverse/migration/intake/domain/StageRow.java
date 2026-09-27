package com.iortatechnxt.brokerverse.migration.intake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A staged row of an extract (DATA_MIGRATION_DESIGN section 6): the raw values as received (masked
 * outside production), the values after the code maps, the SHA-256 of the raw values, the status
 * and, once loaded, the target record. Rows are inserted in pages by the intake and purged with the
 * retention of the batch.
 */
@Entity
@Table(name = "mig_stage_row")
public class StageRow {

  private static final int MAX_MESSAGE = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "extract_id", nullable = false, updatable = false)
  private Long extractId;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "layout_code", nullable = false, length = 10, updatable = false)
  private String layoutCode;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(name = "legacy_key", nullable = false, length = 200, updatable = false)
  private String legacyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", columnDefinition = "jsonb")
  private Map<String, String> rawPayload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "mapped_payload", columnDefinition = "jsonb")
  private Map<String, String> mappedPayload;

  @Column(name = "row_hash", nullable = false, length = 64, updatable = false)
  private String rowHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private RowStatus status = RowStatus.STAGED;

  @Column(name = "batch_id")
  private Long batchId;

  @Column(name = "target_entity", length = 60)
  private String targetEntity;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "target_code", length = 80)
  private String targetCode;

  @Column(length = 1000)
  private String message;

  @Column(name = "loaded_at")
  private Instant loadedAt;

  protected StageRow() {}

  /**
   * The validated result of a row.
   *
   * @param newStatus VALID, WARNING or INVALID
   * @param mapped mapped values
   * @param batch batch
   */
  public void validated(RowStatus newStatus, Map<String, String> mapped, Long batch) {
    this.status = newStatus;
    this.mappedPayload = mapped == null ? null : new LinkedHashMap<>(mapped);
    this.batchId = batch;
    this.message = null;
  }

  /**
   * The row was loaded.
   *
   * @param entity target entity
   * @param id target id
   * @param code target code
   * @param when time
   */
  public void loaded(String entity, Long id, String code, Instant when) {
    this.status = RowStatus.LOADED;
    this.targetEntity = entity;
    this.targetId = id;
    this.targetCode = code;
    this.loadedAt = when;
    this.message = null;
  }

  /**
   * The row was skipped (loaded before, unchanged) or rejected, with the reason.
   *
   * @param newStatus SKIPPED or REJECTED
   * @param reason message
   */
  public void finish(RowStatus newStatus, String reason) {
    this.status = newStatus;
    this.message =
        reason == null ? null : reason.substring(0, Math.min(reason.length(), MAX_MESSAGE));
  }

  /**
   * Sets the status (exclusion, rollback, rerun).
   *
   * @param newStatus status
   */
  public void mark(RowStatus newStatus) {
    this.status = newStatus;
  }

  public Long getId() {
    return id;
  }

  public Long getExtractId() {
    return extractId;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public String getLayoutCode() {
    return layoutCode;
  }

  public int getRowNo() {
    return rowNo;
  }

  public String getLegacyKey() {
    return legacyKey;
  }

  public Map<String, String> getRawPayload() {
    return rawPayload == null ? Map.of() : rawPayload;
  }

  public Map<String, String> getMappedPayload() {
    return mappedPayload == null ? Map.of() : mappedPayload;
  }

  public String getRowHash() {
    return rowHash;
  }

  public RowStatus getStatus() {
    return status;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getTargetEntity() {
    return targetEntity;
  }

  public Long getTargetId() {
    return targetId;
  }

  public String getTargetCode() {
    return targetCode;
  }

  public String getMessage() {
    return message;
  }

  public Instant getLoadedAt() {
    return loadedAt;
  }
}

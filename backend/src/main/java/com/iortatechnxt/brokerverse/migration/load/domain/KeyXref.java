package com.iortatechnxt.brokerverse.migration.load.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * The cross-reference of a loaded record from its legacy key (DATA_MIGRATION_DESIGN section 11):
 * source system, object and legacy key to the BIBS entity, id and code, the batch that loaded it
 * and the hash of the loaded row. It makes loads rerunnable (same key and hash: skipped) and lets
 * users find a migrated record by its legacy key. A rolled-back entry keeps its row with the time.
 */
@Entity
@Table(name = "mig_key_xref")
public class KeyXref {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "source_system", nullable = false, length = 10, updatable = false)
  private String sourceSystem;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "legacy_key", nullable = false, length = 200, updatable = false)
  private String legacyKey;

  @Column(name = "target_entity", nullable = false, length = 60)
  private String targetEntity;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "target_code", length = 80)
  private String targetCode;

  @Column(name = "target_version")
  private Long targetVersion;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "row_hash", nullable = false, length = 64)
  private String rowHash;

  @Column(name = "loaded_at", nullable = false)
  private Instant loadedAt;

  @Column(name = "rolled_back_at")
  private Instant rolledBackAt;

  protected KeyXref() {}

  /**
   * A new cross-reference.
   *
   * @param companyId company
   * @param sourceSystem source system
   * @param objectCode object
   * @param legacyKey legacy key
   */
  public KeyXref(Long companyId, String sourceSystem, String objectCode, String legacyKey) {
    this.companyId = companyId;
    this.sourceSystem = sourceSystem;
    this.objectCode = objectCode;
    this.legacyKey = legacyKey;
  }

  /**
   * Points the key at a loaded record (a first load, an update or a load after a rollback).
   *
   * @param target the target record
   * @param batch batch
   * @param hash row hash
   * @param when time
   */
  public void loaded(Target target, Long batch, String hash, Instant when) {
    this.targetEntity = target.entity();
    this.targetId = target.id();
    this.targetCode = target.code();
    this.targetVersion = target.version();
    this.batchId = batch;
    this.rowHash = hash;
    this.loadedAt = when;
    this.rolledBackAt = null;
  }

  /**
   * The record was undone by a rollback.
   *
   * @param when time
   */
  public void rolledBack(Instant when) {
    this.rolledBackAt = when;
  }

  public boolean isLive() {
    return rolledBackAt == null;
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public String getLegacyKey() {
    return legacyKey;
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

  public Long getTargetVersion() {
    return targetVersion;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getRowHash() {
    return rowHash;
  }

  public Instant getLoadedAt() {
    return loadedAt;
  }

  public Instant getRolledBackAt() {
    return rolledBackAt;
  }

  /**
   * A loaded BIBS record.
   *
   * @param entity entity type
   * @param id id
   * @param code business code
   * @param version optimistic-lock version at load (rollback checks it is unchanged)
   */
  public record Target(String entity, Long id, String code, Long version) {}
}

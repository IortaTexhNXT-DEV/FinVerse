package com.iortatechnxt.brokerverse.migration.archive.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A legacy archive record (objects C04, P02, G02, H01, H02; DATA_MIGRATION_DESIGN section 16;
 * FR-DM-110): the searchable keys of a closed legacy transaction or history record with its
 * labelled legacy columns. Documents are attachments of type LEGACY_DOCUMENT linked to the record.
 * Read-only for users; the access is logged.
 */
@Entity
@Table(name = "mig_archive_record")
public class ArchiveRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "source_system", nullable = false, length = 10, updatable = false)
  private String sourceSystem;

  @Column(name = "record_type", nullable = false, length = 20, updatable = false)
  private String recordType;

  @Column(name = "legacy_key", nullable = false, length = 80, updatable = false)
  private String legacyKey;

  @Column(name = "client_key", length = 30, updatable = false)
  private String clientKey;

  @Column(name = "client_name", length = 250, updatable = false)
  private String clientName;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "receipt_no", length = 40, updatable = false)
  private String receiptNo;

  @Column(name = "claim_no", length = 40, updatable = false)
  private String claimNo;

  @Column(name = "document_date", nullable = false, updatable = false)
  private LocalDate documentDate;

  @Column(name = "period_from", updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", updatable = false)
  private LocalDate periodTo;

  @Column(length = 3, updatable = false)
  private String currency;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 40, updatable = false)
  private String status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", updatable = false)
  private Map<String, String> summary;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "row_hash", nullable = false, length = 64, updatable = false)
  private String rowHash;

  @Column(name = "document_count", nullable = false)
  private int documentCount;

  @Column(name = "rolled_back", nullable = false)
  private boolean rolledBack;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50, updatable = false)
  private String createdBy;

  protected ArchiveRecord() {}

  /**
   * A new archive record.
   *
   * @param companyId company
   * @param keys identifying keys
   * @param facts dates, amount, status and labelled columns
   * @param load batch, row hash, user and time
   */
  public ArchiveRecord(Long companyId, Keys keys, Facts facts, Load load) {
    this.companyId = companyId;
    this.sourceSystem = keys.sourceSystem();
    this.recordType = keys.recordType();
    this.legacyKey = keys.legacyKey();
    this.clientKey = keys.clientKey();
    this.clientName = keys.clientName();
    this.policyNo = keys.policyNo();
    this.invoiceNo = keys.invoiceNo();
    this.receiptNo = keys.receiptNo();
    this.claimNo = keys.claimNo();
    this.documentDate = facts.documentDate();
    this.periodFrom = facts.periodFrom();
    this.periodTo = facts.periodTo();
    this.currency = facts.currency();
    this.amount = facts.amount();
    this.status = facts.status();
    this.summary = new LinkedHashMap<>(facts.summary());
    this.batchId = load.batchId();
    this.rowHash = load.rowHash();
    this.createdBy = load.user();
    this.createdAt = load.when();
  }

  /** A document was linked to the record. */
  public void documentAdded() {
    this.documentCount++;
  }

  /** Undone by a rollback of its batch. */
  public void rollBack() {
    this.rolledBack = true;
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

  public String getRecordType() {
    return recordType;
  }

  public String getLegacyKey() {
    return legacyKey;
  }

  public String getClientKey() {
    return clientKey;
  }

  public String getClientName() {
    return clientName;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getClaimNo() {
    return claimNo;
  }

  public LocalDate getDocumentDate() {
    return documentDate;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getStatus() {
    return status;
  }

  public Map<String, String> getSummary() {
    return summary == null ? Map.of() : summary;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getRowHash() {
    return rowHash;
  }

  public int getDocumentCount() {
    return documentCount;
  }

  public boolean isRolledBack() {
    return rolledBack;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Identifying keys.
   *
   * @param sourceSystem source system
   * @param recordType record type
   * @param legacyKey legacy key
   * @param clientKey legacy client number
   * @param clientName client name
   * @param policyNo policy or cover number
   * @param invoiceNo invoice number
   * @param receiptNo receipt number
   * @param claimNo claim number
   */
  public record Keys(
      String sourceSystem,
      String recordType,
      String legacyKey,
      String clientKey,
      String clientName,
      String policyNo,
      String invoiceNo,
      String receiptNo,
      String claimNo) {}

  /**
   * Facts of a record.
   *
   * @param documentDate document date
   * @param periodFrom period start
   * @param periodTo period end
   * @param currency currency
   * @param amount amount
   * @param status legacy status
   * @param summary labelled legacy columns
   */
  public record Facts(
      LocalDate documentDate,
      LocalDate periodFrom,
      LocalDate periodTo,
      String currency,
      BigDecimal amount,
      String status,
      Map<String, String> summary) {}

  /**
   * Load facts.
   *
   * @param batchId batch
   * @param rowHash row hash
   * @param user loader
   * @param when time
   */
  public record Load(Long batchId, String rowHash, String user, Instant when) {}
}

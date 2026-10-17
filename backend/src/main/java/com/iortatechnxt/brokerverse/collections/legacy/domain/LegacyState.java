package com.iortatechnxt.brokerverse.collections.legacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A row of the open collection follow-up carried from legacy for a legacy invoice (Data Migration
 * object F03, the designed CLX_LEGACY_ITEMS): the latest disposition, a promise to pay, an
 * installment of a plan or the collector assignment.
 */
@Entity
@Table(name = "clx_legacy_state")
public class LegacyState {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "legacy_invoice_no", nullable = false, length = 40, updatable = false)
  private String legacyInvoiceNo;

  @Column(name = "source_system", nullable = false, length = 10, updatable = false)
  private String sourceSystem;

  @Column(name = "record_type", nullable = false, length = 12, updatable = false)
  private String recordType;

  @Column(name = "seq_no", nullable = false, updatable = false)
  private int seqNo;

  @Column(name = "disposition_code", length = 40, updatable = false)
  private String dispositionCode;

  @Column(name = "disposition_date", updatable = false)
  private LocalDate dispositionDate;

  @Column(name = "promise_date", updatable = false)
  private LocalDate promiseDate;

  @Column(name = "promise_amount", precision = 19, scale = 2, updatable = false)
  private BigDecimal promiseAmount;

  @Column(name = "installment_no", updatable = false)
  private Integer installmentNo;

  @Column(name = "installment_due", updatable = false)
  private LocalDate installmentDue;

  @Column(name = "installment_amount", precision = 19, scale = 2, updatable = false)
  private BigDecimal installmentAmount;

  @Column(length = 50, updatable = false)
  private String collector;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "migration_batch", nullable = false, length = 20, updatable = false)
  private String migrationBatch;

  @Column(name = "rolled_back_at")
  private Instant rolledBackAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50, updatable = false)
  private String createdBy;

  protected LegacyState() {}

  /**
   * A carried row.
   *
   * @param key invoice, legacy invoice, source system and batch
   * @param row the follow-up
   * @param user loading user
   * @param at time
   */
  public LegacyState(Key key, Row row, String user, Instant at) {
    this.companyId = key.companyId();
    this.invoiceNo = key.invoiceNo();
    this.legacyInvoiceNo = key.legacyInvoiceNo();
    this.sourceSystem = key.sourceSystem();
    this.migrationBatch = key.migrationBatch();
    this.recordType = row.recordType();
    this.seqNo = row.seqNo();
    this.dispositionCode = row.dispositionCode();
    this.dispositionDate = row.dispositionDate();
    this.promiseDate = row.promiseDate();
    this.promiseAmount = row.promiseAmount();
    this.installmentNo = row.installmentNo();
    this.installmentDue = row.installmentDue();
    this.installmentAmount = row.installmentAmount();
    this.collector = row.collector();
    this.remarks = row.remarks();
    this.createdBy = user;
    this.createdAt = at;
  }

  /**
   * Marks the row of a rolled-back batch.
   *
   * @param at time
   */
  public void rollBack(Instant at) {
    this.rolledBackAt = at;
  }

  /**
   * Which invoice and batch a row belongs to.
   *
   * @param companyId company
   * @param invoiceNo ledger invoice number
   * @param legacyInvoiceNo legacy invoice number
   * @param sourceSystem source system
   * @param migrationBatch loading batch
   */
  public record Key(
      Long companyId,
      String invoiceNo,
      String legacyInvoiceNo,
      String sourceSystem,
      String migrationBatch) {}

  /**
   * The follow-up of a row.
   *
   * @param recordType DISPOSITION, PROMISE, INSTALLMENT or ASSIGNMENT
   * @param seqNo sequence within the invoice and type
   * @param dispositionCode latest disposition (BIBS code)
   * @param dispositionDate its date
   * @param promiseDate promised payment date
   * @param promiseAmount promised amount
   * @param installmentNo installment number
   * @param installmentDue installment due date
   * @param installmentAmount installment amount
   * @param collector collector (BIBS user)
   * @param remarks remarks
   */
  public record Row(
      String recordType,
      int seqNo,
      String dispositionCode,
      LocalDate dispositionDate,
      LocalDate promiseDate,
      BigDecimal promiseAmount,
      Integer installmentNo,
      LocalDate installmentDue,
      BigDecimal installmentAmount,
      String collector,
      String remarks) {}

  public Long getId() {
    return id;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getLegacyInvoiceNo() {
    return legacyInvoiceNo;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getMigrationBatch() {
    return migrationBatch;
  }

  public Instant getRolledBackAt() {
    return rolledBackAt;
  }

  /**
   * The follow-up of the row.
   *
   * @return row
   */
  public Row row() {
    return new Row(
        recordType,
        seqNo,
        dispositionCode,
        dispositionDate,
        promiseDate,
        promiseAmount,
        installmentNo,
        installmentDue,
        installmentAmount,
        collector,
        remarks);
  }
}

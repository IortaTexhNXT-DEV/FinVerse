package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One movement of one invoice component, immutable (RMTID.038 payment and remittance history,
 * ADJID.024). Idempotent on source module, source reference, invoice, component and type.
 */
@Entity
@Table(name = "ops_invoice_movement")
public class OpsInvoiceMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "invoice_id", nullable = false, updatable = false)
  private Long invoiceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 20, updatable = false)
  private MovementType movementType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private LedgerComponent component;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "ar_no", length = 40, updatable = false)
  private String arNo;

  @Column(name = "or_no", length = 40, updatable = false)
  private String orNo;

  @Column(name = "batch_no", length = 40, updatable = false)
  private String batchNo;

  @Column(name = "value_date", nullable = false, updatable = false)
  private LocalDate valueDate;

  @Column(name = "journal_batch_no", length = 40, updatable = false)
  private String journalBatchNo;

  @Column(length = 250, updatable = false)
  private String remarks;

  @Column(name = "posted_at", nullable = false, updatable = false)
  private Instant postedAt;

  @Column(name = "posted_by", nullable = false, length = 50, updatable = false)
  private String postedBy;

  protected OpsInvoiceMovement() {}

  /**
   * Creates a movement.
   *
   * @param invoiceId invoice
   * @param entry type, component and amount
   * @param source module, reference, dates and document numbers
   */
  public OpsInvoiceMovement(Long invoiceId, Entry entry, Source source) {
    this.invoiceId = invoiceId;
    this.movementType = entry.type();
    this.component = entry.component();
    this.amount = entry.amount();
    this.sourceModule = source.module();
    this.sourceRef = source.reference();
    this.arNo = source.arNo();
    this.orNo = source.orNo();
    this.batchNo = source.batchNo();
    this.valueDate = source.valueDate();
    this.journalBatchNo = source.journalBatchNo();
    this.remarks = source.remarks();
    this.postedAt = source.postedAt();
    this.postedBy = source.postedBy();
  }

  public Long getId() {
    return id;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public MovementType getMovementType() {
    return movementType;
  }

  public LedgerComponent getComponent() {
    return component;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getArNo() {
    return arNo;
  }

  public String getOrNo() {
    return orNo;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public String getPostedBy() {
    return postedBy;
  }

  /**
   * What moved.
   *
   * @param type movement type
   * @param component component
   * @param amount signed amount
   */
  public record Entry(MovementType type, LedgerComponent component, BigDecimal amount) {}

  /**
   * Where the movement comes from.
   *
   * @param module source module (e.g. CASHIERING)
   * @param reference source reference, unique per business transaction
   * @param valueDate value date
   * @param arNo acknowledgement receipt number
   * @param orNo official receipt number
   * @param batchNo remittance, adjustment or other batch number
   * @param journalBatchNo GL journal batch
   * @param remarks remarks
   * @param postedAt time
   * @param postedBy user
   */
  public record Source(
      String module,
      String reference,
      LocalDate valueDate,
      String arNo,
      String orNo,
      String batchNo,
      String journalBatchNo,
      String remarks,
      Instant postedAt,
      String postedBy) {}
}

package com.iortatechnxt.brokerverse.cashiering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A line of a Cashiering legacy batch: an unapplied item taken to income, or a legacy invoice whose
 * PR 2307 is reversed against the insurer; posted in its own transaction.
 */
@Entity
@Table(name = "csh_legacy_batch_line")
public class LegacyBatchLine {

  /** Not yet posted. */
  public static final String PENDING = "PENDING";

  /** Posted. */
  public static final String POSTED = "POSTED";

  /** Refused at posting. */
  public static final String FAILED = "FAILED";

  private static final int MAX_MESSAGE = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "unapplied_id", updatable = false)
  private Long unappliedId;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 40, updatable = false)
  private String reference;

  @Column(name = "ledger_context", nullable = false, length = 10, updatable = false)
  private String ledgerContext;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "age_days", updatable = false)
  private Integer ageDays;

  @Column(length = 250, updatable = false)
  private String reason;

  @Column(nullable = false, length = 10)
  private String status = PENDING;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(length = 500)
  private String message;

  protected LegacyBatchLine() {}

  /**
   * A new line.
   *
   * @param batchId batch
   * @param lineNo line number
   * @param subject item or invoice
   * @param amount amount
   * @param reason reason
   */
  public LegacyBatchLine(
      Long batchId, int lineNo, Subject subject, BigDecimal amount, String reason) {
    this.batchId = batchId;
    this.lineNo = lineNo;
    this.unappliedId = subject.unappliedId();
    this.invoiceNo = subject.invoiceNo();
    this.reference = subject.reference();
    this.ledgerContext = subject.ledgerContext();
    this.ageDays = subject.ageDays();
    this.amount = amount;
    this.reason = reason;
  }

  /**
   * Records the posting.
   *
   * @param journal journal batch
   */
  public void posted(String journal) {
    status = POSTED;
    journalBatchNo = journal;
    message = null;
  }

  /**
   * Records a refusal.
   *
   * @param why message
   */
  public void failed(String why) {
    status = FAILED;
    message = why == null || why.length() <= MAX_MESSAGE ? why : why.substring(0, MAX_MESSAGE);
  }

  public Long getId() {
    return id;
  }

  public Long getBatchId() {
    return batchId;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Long getUnappliedId() {
    return unappliedId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getReference() {
    return reference;
  }

  public String getLedgerContext() {
    return ledgerContext;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Integer getAgeDays() {
    return ageDays;
  }

  public String getReason() {
    return reason;
  }

  public String getStatus() {
    return status;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getMessage() {
    return message;
  }

  /**
   * What a line is about.
   *
   * @param unappliedId unapplied item, null for an invoice line
   * @param invoiceNo invoice, null for an unapplied line
   * @param reference unapplied reference or invoice number
   * @param ledgerContext NEW or LEGACY
   * @param ageDays age of the item in days, null for an invoice line
   */
  public record Subject(
      Long unappliedId,
      String invoiceNo,
      String reference,
      String ledgerContext,
      Integer ageDays) {}
}

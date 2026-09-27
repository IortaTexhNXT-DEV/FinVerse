package com.iortatechnxt.brokerverse.commission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A legacy invoice of a direct payment PR reversal batch, posted in its own transaction. */
@Entity
@Table(name = "cmr_dppr_batch_line")
public class DpprBatchLine {

  private static final int MAX_MESSAGE = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 250, updatable = false)
  private String reason;

  @Column(nullable = false, length = 10)
  private String status = "PENDING";

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(length = 500)
  private String message;

  protected DpprBatchLine() {}

  /**
   * A new line.
   *
   * @param batchId batch
   * @param lineNo number
   * @param invoiceNo legacy invoice
   * @param amount open premium receivable to reverse
   * @param reason reason
   */
  public DpprBatchLine(
      Long batchId, int lineNo, String invoiceNo, BigDecimal amount, String reason) {
    this.batchId = batchId;
    this.lineNo = lineNo;
    this.invoiceNo = invoiceNo;
    this.amount = amount;
    this.reason = reason;
  }

  /**
   * Records the posting.
   *
   * @param journal journal batch
   */
  public void posted(String journal) {
    status = "POSTED";
    journalBatchNo = journal;
  }

  /**
   * Records a refusal.
   *
   * @param why message
   */
  public void failed(String why) {
    status = "FAILED";
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

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public BigDecimal getAmount() {
    return amount;
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
}

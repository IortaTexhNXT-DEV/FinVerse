package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A No Touch billing batch of one insurer and month (Report List #164; OQ39): exported with the
 * premium and fee columns blank, returned by the insurer with the validated values, then billed
 * with a service invoice.
 */
@Entity
@Table(name = "sbm_no_touch_batch")
public class SbmNoTouchBatch extends BaseEntity {

  /** Exported to the insurer. */
  public static final String EXPORTED = "EXPORTED";

  /** Returned by the insurer. */
  public static final String RETURNED = "RETURNED";

  /** Billed. */
  public static final String BILLED = "BILLED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, updatable = false, length = 30)
  private String batchNo;

  @Column(name = "insurer_code", nullable = false, updatable = false, length = 30)
  private String insurerCode;

  @Column(nullable = false, updatable = false, length = 7)
  private String period;

  @Column(nullable = false, length = 20)
  private String status = EXPORTED;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Column(name = "gross_fee", precision = 19, scale = 2)
  private BigDecimal grossFee;

  @Column(precision = 19, scale = 2)
  private BigDecimal vat;

  @Column(precision = 19, scale = 2)
  private BigDecimal wtax;

  @Column(name = "exported_file_id")
  private Long exportedFileId;

  @Column(name = "statement_file_id")
  private Long statementFileId;

  @Column(name = "si_no", length = 40)
  private String siNo;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @Column(name = "billed_at")
  private Instant billedAt;

  protected SbmNoTouchBatch() {}

  /**
   * A batch.
   *
   * @param companyId company
   * @param batchNo number
   * @param insurerCode insurer
   * @param period month (yyyy-MM)
   */
  public SbmNoTouchBatch(Long companyId, String batchNo, String insurerCode, String period) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.insurerCode = insurerCode;
    this.period = period;
  }

  /**
   * The export.
   *
   * @param lines lines exported
   * @param fileId exported file
   */
  public void exported(int lines, Long fileId) {
    this.lineCount = lines;
    this.exportedFileId = fileId;
  }

  /**
   * The insurer's return with the totals.
   *
   * @param totals gross fee, VAT and withholding tax
   * @param statementFile billing statement
   * @param at time
   */
  public void returned(Totals totals, Long statementFile, Instant at) {
    if (BILLED.equals(status)) {
      throw new BusinessRuleException("SBM_NO_TOUCH_BILLED", batchNo + " is already billed");
    }
    this.grossFee = totals.grossFee();
    this.vat = totals.vat();
    this.wtax = totals.wtax();
    this.statementFileId = statementFile;
    this.returnedAt = at;
    this.status = RETURNED;
  }

  /**
   * Billed with a service invoice.
   *
   * @param serviceInvoiceNo service invoice
   * @param journal journal of the fee
   * @param at time
   */
  public void billed(String serviceInvoiceNo, String journal, Instant at) {
    if (!RETURNED.equals(status)) {
      throw new BusinessRuleException(
          "SBM_NO_TOUCH_NOT_RETURNED", "Upload the insurer's return of " + batchNo + " first");
    }
    this.siNo = serviceInvoiceNo;
    this.journalBatchNo = journal;
    this.billedAt = at;
    this.status = BILLED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getPeriod() {
    return period;
  }

  public String getStatus() {
    return status;
  }

  public int getLineCount() {
    return lineCount;
  }

  public BigDecimal getGrossFee() {
    return grossFee;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public Long getExportedFileId() {
    return exportedFileId;
  }

  public Long getStatementFileId() {
    return statementFileId;
  }

  public String getSiNo() {
    return siNo;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }

  public Instant getBilledAt() {
    return billedAt;
  }

  /**
   * Totals of a returned batch.
   *
   * @param grossFee gross service fee
   * @param vat VAT
   * @param wtax withholding tax
   */
  public record Totals(BigDecimal grossFee, BigDecimal vat, BigDecimal wtax) {}
}

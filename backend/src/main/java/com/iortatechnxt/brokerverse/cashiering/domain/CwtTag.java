package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.CwtPath;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A BIR 2307 reversal tagged by Marketing Collection with its {@code CWT-} reference (MKTID.013,
 * CSHID.026): the 2% creditable withholding tax of an invoice settled in cash or by the client's
 * certificate. Cashiering validates it against the CWT copy, posts the reclass and routes it to
 * Disbursement (CSHID.027, DBMID.001). The stage mirrors the workflow {@code OPS_CWT_2307}.
 */
@Entity
@Table(name = "csh_cwt_tag")
public class CwtTag extends BaseEntity {

  /** Initial stage. */
  public static final String TAGGED = "TAGGED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30, updatable = false)
  private String reference;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private CwtPath path;

  @Column(name = "certificate_no", length = 60, updatable = false)
  private String certificateNo;

  @Column(name = "period_from", updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", updatable = false)
  private LocalDate periodTo;

  @Column(name = "cwt_copy_received", nullable = false)
  private boolean cwtCopyReceived;

  @Column(nullable = false, updatable = false)
  private boolean remitted;

  @Column(nullable = false, length = 20)
  private String stage = TAGGED;

  @Column(name = "batch_id")
  private Long batchId;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  @Column(name = "reclass_journal_no", length = 40)
  private String reclassJournalNo;

  @Column(name = "offset_journal_no", length = 40)
  private String offsetJournalNo;

  @Column(length = 250)
  private String remarks;

  protected CwtTag() {}

  /**
   * Tags a 2307 reversal.
   *
   * @param reference CWT- reference
   * @param invoice invoice facts
   * @param details amount, path and certificate
   * @param remitted whether the invoice was already remitted (MKTID.010)
   */
  public CwtTag(String reference, TaggedInvoice invoice, CwtDetails details, boolean remitted) {
    this.companyId = invoice.companyId();
    this.reference = reference;
    this.invoiceNo = invoice.invoiceNo();
    this.arn = invoice.arn();
    this.clientCode = invoice.clientCode();
    this.insurerCode = invoice.insurerCode();
    this.amount = details.amount();
    this.path = details.path();
    this.certificateNo = details.certificateNo();
    this.periodFrom = details.periodFrom();
    this.periodTo = details.periodTo();
    this.remarks = details.remarks();
    this.remitted = remitted;
    if (path == CwtPath.CERTIFICATE && (certificateNo == null || certificateNo.isBlank())) {
      throw new BusinessRuleException(
          "CWT_CERTIFICATE_REQUIRED", "A certificate tag needs the BIR 2307 certificate number");
    }
  }

  /**
   * Records whether the copy of the certificate was received (validation checklist).
   *
   * @param received flag
   */
  public void copyReceived(boolean received) {
    this.cwtCopyReceived = received;
  }

  /**
   * Records the reclass posting and the batch (CSHID.027).
   *
   * @param batch batch id
   * @param journal reclass journal
   */
  public void validated(Long batch, String journal) {
    this.batchId = batch;
    this.reclassJournalNo = journal;
  }

  /**
   * Records the AR of the cash path (CSHID.026).
   *
   * @param receipt AR number
   */
  public void settledInCash(String receipt) {
    this.receiptNo = receipt;
  }

  /**
   * Records the DTIP offset journal (DBMID.001).
   *
   * @param journal journal
   */
  public void offset(String journal) {
    this.offsetJournalNo = journal;
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param stageCode stage
   */
  public void markStage(String stageCode) {
    this.stage = stageCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReference() {
    return reference;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public CwtPath getPath() {
    return path;
  }

  public String getCertificateNo() {
    return certificateNo;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public boolean isCwtCopyReceived() {
    return cwtCopyReceived;
  }

  public boolean isRemitted() {
    return remitted;
  }

  public String getStage() {
    return stage;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getReclassJournalNo() {
    return reclassJournalNo;
  }

  public String getOffsetJournalNo() {
    return offsetJournalNo;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * The invoice a tag is for.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param arn account
   * @param clientCode client
   * @param insurerCode lead insurer
   */
  public record TaggedInvoice(
      Long companyId, String invoiceNo, String arn, String clientCode, String insurerCode) {}

  /**
   * What Marketing tags.
   *
   * @param amount 2% amount
   * @param path cash or certificate
   * @param certificateNo certificate number (certificate path)
   * @param periodFrom certificate period from, may be null
   * @param periodTo certificate period to, may be null
   * @param remarks remarks
   */
  public record CwtDetails(
      BigDecimal amount,
      CwtPath path,
      String certificateNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      String remarks) {}
}

package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * The extraction tag an invoice received (RMTID.003): EXTRACTED with its batch, UNEXTRACTED_DUE
 * with the exclusion reasons (RMTID.014/017/020/022/031), UNEXTRACTED_NOT_DUE, or RETURNED when a
 * user returned it from a batch (RMTID.029). The latest tag is the invoice's current tag.
 */
@Entity
@Table(name = "rem_extraction_tag")
public class InvoiceTag extends BaseEntity {

  @Column(name = "run_id", updatable = false)
  private Long runId;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "remittance_type", length = 20, updatable = false)
  private RemittanceType remittanceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private ExtractionTag tag;

  @Column(length = 200, updatable = false)
  private String reasons;

  @Column(length = 500, updatable = false)
  private String remarks;

  @Column(name = "paid_ar", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal paidAr;

  @Column(name = "dtip_balance", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal dtipBalance;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal remittable;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  protected InvoiceTag() {}

  /**
   * A tag.
   *
   * @param runId extraction run, null for a return
   * @param facts company, invoice, insurer and type
   * @param outcome tag, reasons and remarks
   * @param money paid AR, DTIP balance and remittable amount
   */
  public InvoiceTag(Long runId, Facts facts, Outcome outcome, Money money) {
    this.runId = runId;
    this.companyId = facts.companyId();
    this.invoiceNo = facts.invoiceNo();
    this.insurerCode = facts.insurerCode();
    this.remittanceType = facts.type();
    this.tag = outcome.tag();
    this.reasons = outcome.reasons();
    this.remarks = outcome.remarks();
    this.paidAr = money.paidAr();
    this.dtipBalance = money.dtipBalance();
    this.remittable = money.remittable();
  }

  /**
   * Links the tag to the batch the invoice was extracted into.
   *
   * @param number batch number
   */
  public void inBatch(String number) {
    this.batchNo = number;
  }

  public Long getRunId() {
    return runId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public RemittanceType getRemittanceType() {
    return remittanceType;
  }

  public ExtractionTag getTag() {
    return tag;
  }

  public String getReasons() {
    return reasons;
  }

  public String getRemarks() {
    return remarks;
  }

  public BigDecimal getPaidAr() {
    return paidAr;
  }

  public BigDecimal getDtipBalance() {
    return dtipBalance;
  }

  public BigDecimal getRemittable() {
    return remittable;
  }

  public String getBatchNo() {
    return batchNo;
  }

  /**
   * Whom a tag concerns.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param insurerCode insurer
   * @param type remittance type, may be null
   */
  public record Facts(Long companyId, String invoiceNo, String insurerCode, RemittanceType type) {}

  /**
   * The decision.
   *
   * @param tag tag
   * @param reasons exclusion reason codes (LOV REMIT_EXCLUSION_REASON), comma separated
   * @param remarks details
   */
  public record Outcome(ExtractionTag tag, String reasons, String remarks) {}

  /**
   * The money examined.
   *
   * @param paidAr net paid AR not yet remitted
   * @param dtipBalance outstanding DTIP
   * @param remittable amount that may be remitted now
   */
  public record Money(BigDecimal paidAr, BigDecimal dtipBalance, BigDecimal remittable) {}
}

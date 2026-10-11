package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A commission payment detail uploaded from Collection (CSHID.007): staged until the official
 * receipts are issued, one OR per insurer, payee, certificate and payment (OQ49).
 */
@Entity
@Table(name = "csh_commission_line")
public class CommissionLine extends BaseEntity {

  /** Waiting for its OR. */
  public static final String STAGED = "STAGED";

  /** OR issued. */
  public static final String ISSUED = "ISSUED";

  /** Refused by the consolidation check. */
  public static final String REJECTED = "REJECTED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "job_no", nullable = false, length = 40, updatable = false)
  private String jobNo;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "payee_name", nullable = false, length = 250, updatable = false)
  private String payeeName;

  @Column(name = "certificate_ref", length = 60, updatable = false)
  private String certificateRef;

  @Column(name = "payment_ref", nullable = false, length = 60, updatable = false)
  private String paymentRef;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal gross;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal vat;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal wtax;

  @Column(name = "payment_date", nullable = false, updatable = false)
  private LocalDate paymentDate;

  @Column(nullable = false, length = 20)
  private String status = STAGED;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  @Column(length = 250)
  private String message;

  protected CommissionLine() {}

  /**
   * Stages a line.
   *
   * @param companyId company
   * @param jobNo upload job
   * @param rowNo file row
   * @param detail payment detail
   */
  public CommissionLine(Long companyId, String jobNo, int rowNo, CommissionDetail detail) {
    this.companyId = companyId;
    this.jobNo = jobNo;
    this.rowNo = rowNo;
    this.insurerCode = detail.insurerCode();
    this.payeeName = detail.payeeName();
    this.certificateRef = detail.certificateRef();
    this.paymentRef = detail.paymentRef();
    this.invoiceNo = detail.invoiceNo();
    this.gross = detail.amounts().gross();
    this.vat = detail.amounts().vat();
    this.wtax = detail.amounts().wtax();
    this.paymentDate = detail.paymentDate();
  }

  /**
   * Records the outcome.
   *
   * @param outcome ISSUED or REJECTED
   * @param receipt OR number, may be null
   * @param text message
   */
  public void resolve(String outcome, String receipt, String text) {
    this.status = outcome;
    this.receiptNo = receipt;
    this.message = text;
  }

  /**
   * Gross, VAT and withholding tax.
   *
   * @return amounts
   */
  public OrAmounts amounts() {
    return new OrAmounts(gross, vat, wtax);
  }

  /**
   * Consolidation key: insurer, certificate and payment (CSHID.007).
   *
   * @return key
   */
  public String groupKey() {
    return insurerCode + "|" + (certificateRef == null ? "" : certificateRef) + "|" + paymentRef;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getJobNo() {
    return jobNo;
  }

  public int getRowNo() {
    return rowNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getCertificateRef() {
    return certificateRef;
  }

  public String getPaymentRef() {
    return paymentRef;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public BigDecimal getGross() {
    return gross;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  public String getStatus() {
    return status;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getMessage() {
    return message;
  }

  /**
   * One commission payment detail.
   *
   * @param insurerCode insurer paying the commission
   * @param payeeName payee on the OR
   * @param certificateRef BIR certificate (EWT) reference, may be null
   * @param paymentRef payment (check or credit) reference
   * @param invoiceNo invoice, may be null
   * @param amounts basic commission, VAT and withholding tax
   * @param paymentDate payment date
   */
  public record CommissionDetail(
      String insurerCode,
      String payeeName,
      String certificateRef,
      String paymentRef,
      String invoiceNo,
      OrAmounts amounts,
      LocalDate paymentDate) {}
}

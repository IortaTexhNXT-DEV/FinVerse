package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of an insurer statement of account (ACSL 2.4.0): loaded with the insurer's invoice,
 * policy, assured, dates, gross premium, balance and payments, or failed with the reason (the
 * upload log).
 */
@Entity
@Table(name = "acsl_soa_line")
public class SoaLine {

  /** Status of a loaded row. */
  public static final String LOADED = "LOADED";

  /** Status of a failed row. */
  public static final String FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "upload_id", nullable = false, updatable = false)
  private Long uploadId;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(nullable = false, length = 10, updatable = false)
  private String status;

  @Column(length = 500, updatable = false)
  private String error;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(name = "assured_name", length = 250, updatable = false)
  private String assuredName;

  @Column(name = "inception_date", updatable = false)
  private LocalDate inceptionDate;

  @Column(name = "expiry_date", updatable = false)
  private LocalDate expiryDate;

  @Column(name = "gross_premium", precision = 19, scale = 2, updatable = false)
  private BigDecimal grossPremium;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal balance;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal paid;

  protected SoaLine() {}

  /**
   * A loaded row.
   *
   * @param uploadId upload
   * @param rowNo row in the file
   * @param values the insurer's values
   * @return line
   */
  public static SoaLine loaded(Long uploadId, int rowNo, SoaValues values) {
    SoaLine line = new SoaLine();
    line.uploadId = uploadId;
    line.rowNo = rowNo;
    line.status = LOADED;
    line.invoiceNo = values.invoiceNo();
    line.policyNo = values.policyNo();
    line.assuredName = values.assuredName();
    line.inceptionDate = values.inceptionDate();
    line.expiryDate = values.expiryDate();
    line.grossPremium = values.grossPremium();
    line.balance = values.balance();
    line.paid = values.paid();
    return line;
  }

  /**
   * A failed row.
   *
   * @param uploadId upload
   * @param rowNo row in the file
   * @param invoiceNo invoice as read, may be null
   * @param error reason
   * @return line
   */
  public static SoaLine failed(Long uploadId, int rowNo, String invoiceNo, String error) {
    SoaLine line = new SoaLine();
    line.uploadId = uploadId;
    line.rowNo = rowNo;
    line.status = FAILED;
    line.invoiceNo = invoiceNo;
    line.error = error;
    return line;
  }

  public Long getId() {
    return id;
  }

  public Long getUploadId() {
    return uploadId;
  }

  public int getRowNo() {
    return rowNo;
  }

  public String getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public LocalDate getInceptionDate() {
    return inceptionDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public BigDecimal getPaid() {
    return paid;
  }

  /**
   * The insurer's values of a row.
   *
   * @param invoiceNo invoice (BDOI invoice number quoted by the insurer)
   * @param policyNo policy
   * @param assuredName assured
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param grossPremium gross premium
   * @param balance balance per the insurer
   * @param paid payments per the insurer
   */
  public record SoaValues(
      String invoiceNo,
      String policyNo,
      String assuredName,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      BigDecimal grossPremium,
      BigDecimal balance,
      BigDecimal paid) {}
}

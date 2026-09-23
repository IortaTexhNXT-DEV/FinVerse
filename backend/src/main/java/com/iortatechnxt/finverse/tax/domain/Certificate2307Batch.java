package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A batch run that issued BIR Form 2307 certificates for one quarter. */
@Entity
@Table(name = "tax_2307_batch")
public class Certificate2307Batch extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 40)
  private String batchNo;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "certificate_count", nullable = false)
  private int certificateCount;

  @Column(name = "total_income", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalIncome = BigDecimal.ZERO;

  @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalTax = BigDecimal.ZERO;

  protected Certificate2307Batch() {}

  /**
   * Opens a batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @param quarter quarter covered
   */
  public Certificate2307Batch(Long companyId, String batchNo, TaxPeriod quarter) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.periodStart = quarter.from();
    this.periodEnd = quarter.to();
  }

  /**
   * Adds an issued certificate to the batch totals.
   *
   * @param certificate certificate
   */
  public void count(Certificate2307 certificate) {
    certificateCount++;
    totalIncome = totalIncome.add(certificate.getTotalIncome());
    totalTax = totalTax.add(certificate.getTotalTax());
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public int getCertificateCount() {
    return certificateCount;
  }

  public BigDecimal getTotalIncome() {
    return totalIncome;
  }

  public BigDecimal getTotalTax() {
    return totalTax;
  }
}

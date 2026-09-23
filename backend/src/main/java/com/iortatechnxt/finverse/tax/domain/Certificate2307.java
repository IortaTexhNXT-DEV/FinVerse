package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BIR Form 2307 — Certificate of Creditable Tax Withheld at Source, issued to one payee for one
 * quarter. Payee and payor identities are copied when the certificate is issued, so a reprint shows
 * the facts as certified even if the masters change later. An ISSUED certificate can be CANCELLED
 * (e.g. after a correction) and the payee re-certified by a new batch.
 */
@Entity
@Table(name = "tax_2307_certificate")
public class Certificate2307 extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private Certificate2307Batch batch;

  @Column(name = "certificate_no", nullable = false, length = 40)
  private String certificateNo;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Column(name = "payee_tin", nullable = false, length = 9)
  private String payeeTin;

  @Column(name = "payee_branch_code", nullable = false, length = 5)
  private String payeeBranchCode;

  @Column(name = "payee_name", nullable = false, length = 200)
  private String payeeName;

  @Column(name = "payee_address", length = 300)
  private String payeeAddress;

  @Column(name = "payee_zip", length = 10)
  private String payeeZip;

  @Column(name = "payor_tin", nullable = false, length = 9)
  private String payorTin;

  @Column(name = "payor_branch_code", nullable = false, length = 5)
  private String payorBranchCode;

  @Column(name = "payor_name", nullable = false, length = 200)
  private String payorName;

  @Column(name = "payor_address", length = 300)
  private String payorAddress;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "total_income", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalIncome = BigDecimal.ZERO;

  @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalTax = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private CertificateStatus status = CertificateStatus.ISSUED;

  @Column(name = "status_reason", length = 200)
  private String statusReason;

  @OneToMany(mappedBy = "certificate", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<Certificate2307Line> lines = new ArrayList<>();

  protected Certificate2307() {}

  /**
   * Issues a certificate.
   *
   * @param batch issuing batch
   * @param certificateNo certificate number
   * @param partyCode payee party code
   * @param payee payee identity
   * @param payor withholding agent identity
   */
  public Certificate2307(
      Certificate2307Batch batch,
      String certificateNo,
      String partyCode,
      Taxpayer payee,
      Taxpayer payor) {
    this.companyId = batch.getCompanyId();
    this.batch = batch;
    this.certificateNo = certificateNo;
    this.partyCode = partyCode;
    this.payeeTin = payee.tin();
    this.payeeBranchCode = payee.branchCode();
    this.payeeName = payee.name();
    this.payeeAddress = payee.address();
    this.payeeZip = payee.zipCode();
    this.payorTin = payor.tin();
    this.payorBranchCode = payor.branchCode();
    this.payorName = payor.name();
    this.payorAddress = payor.address();
    this.periodStart = batch.getPeriodStart();
    this.periodEnd = batch.getPeriodEnd();
  }

  /**
   * Sets the Part II lines and totals of a new certificate.
   *
   * @param amounts lines (at least one)
   */
  public void certify(List<AtcQuarterAmounts> amounts) {
    if (amounts.isEmpty() || !lines.isEmpty()) {
      throw new BusinessRuleException(
          "INVALID_2307_LINES", "A new certificate needs at least one ATC line, set once");
    }
    int number = 1;
    for (AtcQuarterAmounts a : amounts) {
      lines.add(new Certificate2307Line(this, number++, a));
      totalIncome = totalIncome.add(a.total());
      totalTax = totalTax.add(a.tax());
    }
  }

  /**
   * Cancels an issued certificate.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    if (status != CertificateStatus.ISSUED) {
      throw new BusinessRuleException(
          "CERTIFICATE_NOT_ISSUED", "Certificate " + certificateNo + " is not issued");
    }
    this.status = CertificateStatus.CANCELLED;
    this.statusReason = reason;
  }

  /**
   * Payee identity.
   *
   * @return payee
   */
  public Taxpayer payee() {
    return new Taxpayer(payeeTin, payeeBranchCode, payeeName, payeeAddress, payeeZip);
  }

  /**
   * Payor (withholding agent) identity.
   *
   * @return payor
   */
  public Taxpayer payor() {
    return new Taxpayer(payorTin, payorBranchCode, payorName, payorAddress, null);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Certificate2307Batch getBatch() {
    return batch;
  }

  public String getCertificateNo() {
    return certificateNo;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public BigDecimal getTotalIncome() {
    return totalIncome;
  }

  public BigDecimal getTotalTax() {
    return totalTax;
  }

  public CertificateStatus getStatus() {
    return status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public List<Certificate2307Line> getLines() {
    return Collections.unmodifiableList(lines);
  }
}

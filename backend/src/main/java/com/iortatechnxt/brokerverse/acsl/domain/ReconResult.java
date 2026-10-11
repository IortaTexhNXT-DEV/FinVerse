package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * The reconciliation of one SOA line (ACSL 2.13.1, 2.14.1; Appendix C "SOA reconciliation"): the
 * bucket, the book figures of the matched invoice (premium, outstanding, for remittance, remitted
 * with batch and date, 2307 with batch and date, cancellation reference, direct billed), the
 * insurer's figures and the variances.
 */
@Entity
@Table(name = "acsl_recon_result")
public class ReconResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(name = "assured_name", length = 250, updatable = false)
  private String assuredName;

  @Column(name = "root_invoice_no", length = 40, updatable = false)
  private String rootInvoiceNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ReconBucket bucket;

  @Embedded private BookFigures book;

  @Column(name = "soa_premium", precision = 19, scale = 2, updatable = false)
  private BigDecimal soaPremium;

  @Column(name = "soa_balance", precision = 19, scale = 2, updatable = false)
  private BigDecimal soaBalance;

  @Column(name = "premium_variance", precision = 19, scale = 2, updatable = false)
  private BigDecimal premiumVariance;

  @Column(name = "outstanding_variance", precision = 19, scale = 2, updatable = false)
  private BigDecimal outstandingVariance;

  protected ReconResult() {}

  /**
   * Records the reconciliation of a line.
   *
   * @param runId run
   * @param line SOA line
   * @param bucket bucket
   * @param rootInvoiceNo matched invoice's family root, may be null
   * @param book book figures (empty when not found)
   */
  public ReconResult(
      Long runId, SoaLine line, ReconBucket bucket, String rootInvoiceNo, BookFigures book) {
    this.runId = runId;
    this.rowNo = line.getRowNo();
    this.invoiceNo = line.getInvoiceNo();
    this.policyNo = line.getPolicyNo();
    this.assuredName = line.getAssuredName();
    this.rootInvoiceNo = rootInvoiceNo;
    this.bucket = bucket;
    this.book = book;
    this.soaPremium = line.getGrossPremium();
    this.soaBalance = line.getBalance();
    this.premiumVariance = difference(line.getGrossPremium(), book.premium());
    this.outstandingVariance = difference(line.getBalance(), book.outstanding());
  }

  private static BigDecimal difference(BigDecimal insurer, BigDecimal books) {
    return insurer == null || books == null ? null : insurer.subtract(books);
  }

  /**
   * Whether the insurer's figures differ from the books.
   *
   * @return true when a variance is not zero
   */
  public boolean hasVariance() {
    return nonZero(premiumVariance) || nonZero(outstandingVariance);
  }

  private static boolean nonZero(BigDecimal value) {
    return value != null && value.signum() != 0;
  }

  /**
   * The book figures, never null.
   *
   * @return figures
   */
  public BookFigures bookOrNone() {
    return book == null ? BookFigures.NONE : book;
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }

  public int getRowNo() {
    return rowNo;
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

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public ReconBucket getBucket() {
    return bucket;
  }

  public BigDecimal getSoaPremium() {
    return soaPremium;
  }

  public BigDecimal getSoaBalance() {
    return soaBalance;
  }

  public BigDecimal getPremiumVariance() {
    return premiumVariance;
  }

  public BigDecimal getOutstandingVariance() {
    return outstandingVariance;
  }
}

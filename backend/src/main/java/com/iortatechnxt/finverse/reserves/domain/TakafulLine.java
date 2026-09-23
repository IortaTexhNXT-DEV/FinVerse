package com.iortatechnxt.finverse.reserves.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Takaful surplus of one policy stored with a valuation run (PGIBR074 posted view). */
@Entity
@Table(name = "rsv_takaful_line")
public class TakafulLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "policy_id", nullable = false)
  private Long policyId;

  @Column(name = "policy_no", nullable = false, length = 40)
  private String policyNo;

  @Column(name = "insured_name", nullable = false, length = 200)
  private String insuredName;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "product_code", nullable = false, length = 20)
  private String productCode;

  @Column(name = "source_type", nullable = false, length = 10)
  private String sourceType;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossPremium;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal discount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal loading;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal claims;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal applicable;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal retakaful;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal tax;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal payable;

  protected TakafulLine() {}

  /**
   * Stores one item of a run.
   *
   * @param runId run
   * @param item calculated surplus
   */
  public TakafulLine(Long runId, TakafulItem item) {
    this.runId = runId;
    this.policyId = item.policyId();
    this.policyNo = item.policyNo();
    this.insuredName = item.insuredName();
    this.branchId = item.key().branchId();
    this.businessLine = item.key().businessLine();
    this.productCode = item.key().productCode();
    this.sourceType = item.key().sourceType();
    this.expiryDate = item.expiryDate();
    this.grossPremium = item.gross();
    this.discount = item.discount();
    this.loading = item.loading();
    this.commission = item.commission();
    this.claims = item.claims();
    this.applicable = item.applicable();
    this.retakaful = item.retakaful();
    this.tax = item.tax();
    this.payable = item.payable();
  }

  /**
   * The stored item.
   *
   * @return item
   */
  public TakafulItem toItem() {
    return new TakafulItem(
        policyId,
        policyNo,
        insuredName,
        new ReserveKey(branchId, businessLine, productCode, sourceType),
        expiryDate,
        grossPremium,
        discount,
        loading,
        commission,
        claims,
        applicable,
        retakaful,
        tax,
        payable);
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }
}

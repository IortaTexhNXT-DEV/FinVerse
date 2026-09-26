package com.iortatechnxt.brokerverse.reserves.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Policy-level UPR stored with a valuation run (drill-down from the run summary): one row per
 * premium transaction that is still unearned at the valuation date or was approved in its month.
 */
@Entity
@Table(name = "rsv_upr_detail")
public class UprDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "policy_id", nullable = false)
  private Long policyId;

  @Column(name = "endorsement_no", nullable = false)
  private int endorsementNo;

  @Column(name = "document_no", nullable = false, length = 50)
  private String documentNo;

  @Column(nullable = false, length = 20)
  private String kind;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "product_code", nullable = false, length = 20)
  private String productCode;

  @Column(name = "source_type", nullable = false, length = 10)
  private String sourceType;

  @Column(name = "upr_basis", nullable = false, length = 20)
  private String uprBasis;

  @Column(name = "cover_from", nullable = false)
  private LocalDate coverFrom;

  @Column(name = "cover_to", nullable = false)
  private LocalDate coverTo;

  @Column(name = "approval_date", nullable = false)
  private LocalDate approvalDate;

  @Column(name = "total_units", nullable = false)
  private int totalUnits;

  @Column(name = "earned_units", nullable = false)
  private int earnedUnits;

  @Column(name = "unearned_units", nullable = false)
  private int unearnedUnits;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission;

  @Column(name = "treaty_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal treatyPremium;

  @Column(name = "fac_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal facPremium;

  @Column(name = "ri_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal riCommission;

  @Column(name = "unearned_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal unearnedPremium;

  @Column(name = "unearned_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal unearnedCommission;

  @Column(name = "treaty_upr", nullable = false, precision = 19, scale = 2)
  private BigDecimal treatyUpr;

  @Column(name = "fac_upr", nullable = false, precision = 19, scale = 2)
  private BigDecimal facUpr;

  @Column(name = "unearned_ri_comm", nullable = false, precision = 19, scale = 2)
  private BigDecimal unearnedRiCommission;

  protected UprDetail() {}

  /**
   * Stores one item of a run.
   *
   * @param runId run
   * @param item calculated UPR
   */
  public UprDetail(Long runId, UprItem item) {
    this.runId = runId;
    this.policyId = item.policyId();
    this.endorsementNo = item.endorsementNo();
    this.documentNo = item.documentNo();
    this.kind = item.kind();
    this.branchId = item.key().branchId();
    this.businessLine = item.key().businessLine();
    this.productCode = item.key().productCode();
    this.sourceType = item.key().sourceType();
    this.uprBasis = item.basis();
    this.coverFrom = item.coverFrom();
    this.coverTo = item.coverTo();
    this.approvalDate = item.approvalDate();
    this.totalUnits = item.units().total();
    this.earnedUnits = item.units().earned();
    this.unearnedUnits = item.units().unearned();
    PremiumAmounts w = item.written();
    this.premium = w.premium();
    this.commission = w.commission();
    this.treatyPremium = w.treatyPremium();
    this.facPremium = w.facPremium();
    this.riCommission = w.riCommission();
    PremiumAmounts u = item.unearned();
    this.unearnedPremium = u.premium();
    this.unearnedCommission = u.commission();
    this.treatyUpr = u.treatyPremium();
    this.facUpr = u.facPremium();
    this.unearnedRiCommission = u.riCommission();
  }

  /**
   * The stored item.
   *
   * @return item
   */
  public UprItem toItem() {
    return new UprItem(
        policyId,
        endorsementNo,
        documentNo,
        kind,
        new ReserveKey(branchId, businessLine, productCode, sourceType),
        uprBasis,
        coverFrom,
        coverTo,
        approvalDate,
        new EarningUnits(totalUnits, earnedUnits, unearnedUnits),
        new PremiumAmounts(premium, commission, treatyPremium, facPremium, riCommission),
        new PremiumAmounts(
            unearnedPremium, unearnedCommission, treatyUpr, facUpr, unearnedRiCommission));
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }
}

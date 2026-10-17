package com.iortatechnxt.brokerverse.commission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One invoice of an incentive run (CMRID.003/005/006): its production, whether an exclusion rule
 * removed it (negative amounts, erroneous bookings) and the incentive it earns.
 */
@Entity
@Table(name = "cmr_incentive_run_line")
public class IncentiveRunLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "sales_unit", length = 20, updatable = false)
  private String salesUnit;

  @Column(length = 40, updatable = false)
  private String segment;

  @Column(name = "product_line", length = 30, updatable = false)
  private String productLine;

  @Column(name = "basic_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal basicPremium;

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal grossPremium;

  @Column(nullable = false, updatable = false)
  private boolean excluded;

  @Column(name = "exclusion_reason", length = 40, updatable = false)
  private String exclusionReason;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal incentive;

  protected IncentiveRunLine() {}

  /**
   * A computed line.
   *
   * @param runId run
   * @param production the invoice's production
   * @param exclusionReason exclusion rule, null when eligible
   * @param incentive incentive earned
   */
  public IncentiveRunLine(
      Long runId, Production production, String exclusionReason, BigDecimal incentive) {
    this.runId = runId;
    this.invoiceNo = production.invoiceNo();
    this.insurerCode = production.insurerCode();
    this.salesUnit = production.salesUnit();
    this.segment = production.segment();
    this.productLine = production.productLine();
    this.basicPremium = production.basicPremium();
    this.grossPremium = production.grossPremium();
    this.excluded = exclusionReason != null;
    this.exclusionReason = exclusionReason;
    this.incentive = incentive;
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public String getSegment() {
    return segment;
  }

  public String getProductLine() {
    return productLine;
  }

  public BigDecimal getBasicPremium() {
    return basicPremium;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public boolean isExcluded() {
    return excluded;
  }

  public String getExclusionReason() {
    return exclusionReason;
  }

  public BigDecimal getIncentive() {
    return incentive;
  }

  /**
   * An invoice's production.
   *
   * @param invoiceNo invoice
   * @param insurerCode insurer
   * @param salesUnit sales unit (branch pass-on)
   * @param segment segment
   * @param productLine product line
   * @param basicPremium basic premium
   * @param grossPremium gross premium
   */
  public record Production(
      String invoiceNo,
      String insurerCode,
      String salesUnit,
      String segment,
      String productLine,
      BigDecimal basicPremium,
      BigDecimal grossPremium) {}
}

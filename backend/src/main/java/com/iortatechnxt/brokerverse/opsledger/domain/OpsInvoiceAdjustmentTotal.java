package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Cumulative adjustments of an original invoice against its original premium, DTIP and commission
 * (ADJID.028 over-adjustment control): every endorsement or cancellation invoice booked against the
 * original adds its amounts.
 */
@Entity
@Table(name = "ops_invoice_adjustment_total")
public class OpsInvoiceAdjustmentTotal extends BaseEntity {

  @Column(name = "original_invoice_no", nullable = false, length = 40, updatable = false)
  private String originalInvoiceNo;

  @Column(name = "original_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal originalPremium;

  @Column(name = "original_dtip", nullable = false, precision = 19, scale = 2)
  private BigDecimal originalDtip;

  @Column(name = "original_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal originalCommission;

  @Column(name = "adjusted_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal adjustedPremium = BigDecimal.ZERO;

  @Column(name = "adjusted_dtip", nullable = false, precision = 19, scale = 2)
  private BigDecimal adjustedDtip = BigDecimal.ZERO;

  @Column(name = "adjusted_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal adjustedCommission = BigDecimal.ZERO;

  @Column(name = "adjustment_count", nullable = false)
  private int adjustmentCount;

  protected OpsInvoiceAdjustmentTotal() {}

  /**
   * Starts the totals of an original invoice.
   *
   * @param original the original invoice
   */
  public OpsInvoiceAdjustmentTotal(OpsInvoice original) {
    this.originalInvoiceNo = original.getInvoiceNo();
    this.originalPremium = original.getGrossPremium();
    this.originalDtip = original.component(LedgerComponent.DTIP).due();
    this.originalCommission = original.getCommission();
  }

  /**
   * Adds an endorsement or cancellation invoice.
   *
   * @param adjustment the adjusting invoice (amounts signed)
   */
  public void add(OpsInvoice adjustment) {
    adjustedPremium = adjustedPremium.add(adjustment.getGrossPremium());
    adjustedDtip = adjustedDtip.add(adjustment.component(LedgerComponent.DTIP).due());
    adjustedCommission = adjustedCommission.add(adjustment.getCommission());
    adjustmentCount++;
  }

  /**
   * Whether the adjustments take the premium or DTIP below zero (ADJID.028).
   *
   * @return true when over-adjusted
   */
  public boolean isOverAdjusted() {
    return originalPremium.add(adjustedPremium).signum() < 0
        || originalDtip.add(adjustedDtip).signum() < 0;
  }

  public String getOriginalInvoiceNo() {
    return originalInvoiceNo;
  }

  public BigDecimal getOriginalPremium() {
    return originalPremium;
  }

  public BigDecimal getOriginalDtip() {
    return originalDtip;
  }

  public BigDecimal getOriginalCommission() {
    return originalCommission;
  }

  public BigDecimal getAdjustedPremium() {
    return adjustedPremium;
  }

  public BigDecimal getAdjustedDtip() {
    return adjustedDtip;
  }

  public BigDecimal getAdjustedCommission() {
    return adjustedCommission;
  }

  public int getAdjustmentCount() {
    return adjustmentCount;
  }
}

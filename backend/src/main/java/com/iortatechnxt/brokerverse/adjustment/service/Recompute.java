package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.ShareChange;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import java.math.BigDecimal;
import java.util.List;

/**
 * Result of the recompute of a request (ADJID.014/027/028): before / after per component, the
 * change per insurer, the premium and commission to post, the effect on payments and remittance,
 * the service invoice impact and the over-adjustment baseline.
 *
 * @param components before and change per invoice component
 * @param shares change per insurer share
 * @param premium premium change by component (signed)
 * @param commission commission change (signed)
 * @param settlement payments and remittance of the invoice
 * @param serviceInvoice service invoice to issue or credit
 * @param baseline cumulative adjustments against the original premium
 * @param quotationRequired TSI increase above the package limit (ADJID.008)
 */
public record Recompute(
    List<ComponentChange> components,
    List<ShareChange> shares,
    PremiumComponents premium,
    CommissionTerms commission,
    Settlement settlement,
    ServiceInvoiceImpact serviceInvoice,
    Baseline baseline,
    boolean quotationRequired) {

  /** Defensive copies. */
  public Recompute {
    components = List.copyOf(components);
    shares = List.copyOf(shares);
  }

  /**
   * Whether the request reduces the invoice (premium or, without premium change, commission).
   *
   * @return true for returns
   */
  public boolean reduces() {
    int sign = premium.total().signum();
    return sign < 0 || sign == 0 && commission.commission().signum() < 0;
  }

  /**
   * Payments and remittance of the invoice and what the request does to them (ADJID.009/012/013,
   * OPERATIONS_DESIGN section 5 rows 17 and 18).
   *
   * @param netApplied payments applied to the invoice
   * @param remitted DTIP already remitted to the insurer
   * @param reapplication whether the payments must be re-applied (decrease of a paid invoice)
   * @param arInsurer return premium to set up as AR Insurer (decrease already remitted)
   */
  public record Settlement(
      BigDecimal netApplied, BigDecimal remitted, boolean reapplication, BigDecimal arInsurer) {}

  /**
   * Service invoice impact of a commission change (ADJID.014 addendum, OQ34).
   *
   * @param action ISSUE, CREDIT or NONE
   * @param commission commission to invoice or credit (positive)
   * @param vat VAT to invoice or credit (positive)
   */
  public record ServiceInvoiceImpact(String action, BigDecimal commission, BigDecimal vat) {}

  /**
   * Over-adjustment control (ADJID.028, OQ37).
   *
   * @param originalPremium original gross premium
   * @param adjustedBefore cumulative adjustments already booked
   * @param adjustedAfter cumulative adjustments with this request
   * @param limitPercent baseline in percent of the original premium
   * @param exceeded whether the request takes the cumulative adjustments over the baseline or below
   *     zero
   */
  public record Baseline(
      BigDecimal originalPremium,
      BigDecimal adjustedBefore,
      BigDecimal adjustedAfter,
      BigDecimal limitPercent,
      boolean exceeded) {}
}

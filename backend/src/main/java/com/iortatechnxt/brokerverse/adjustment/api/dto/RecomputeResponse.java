package com.iortatechnxt.brokerverse.adjustment.api.dto;

import com.iortatechnxt.brokerverse.adjustment.api.dto.RequestResponse.ChangeView;
import com.iortatechnxt.brokerverse.adjustment.api.dto.RequestResponse.ShareView;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute.Baseline;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute.ServiceInvoiceImpact;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute.Settlement;
import java.math.BigDecimal;
import java.util.List;

/**
 * Recompute preview of a request (ADJID.008/009/012-014/023/027/028): before / after per component,
 * change per insurer, the service invoice impact, what happens to payments and remittance, the
 * baseline and the possible duplicates.
 *
 * @param requestClass class derived from the endorsement type
 * @param computation computation derived from the request type
 * @param negative whether the request reduces the invoice
 * @param needsApproval whether the team leader approves it
 * @param changes before / after per component
 * @param shares change per insurer
 * @param premiumChange gross premium change
 * @param commissionChange commission change
 * @param vatChange VAT on commission change
 * @param settlement payments and remittance
 * @param serviceInvoice service invoice impact
 * @param baseline over-adjustment control
 * @param quotationRequired TSI increase above the package limit
 * @param duplicates possible duplicates (request numbers)
 */
public record RecomputeResponse(
    String requestClass,
    String computation,
    boolean negative,
    boolean needsApproval,
    List<ChangeView> changes,
    List<ShareView> shares,
    BigDecimal premiumChange,
    BigDecimal commissionChange,
    BigDecimal vatChange,
    Settlement settlement,
    ServiceInvoiceImpact serviceInvoice,
    Baseline baseline,
    boolean quotationRequired,
    List<String> duplicates) {

  /** Defensive copies. */
  public RecomputeResponse {
    changes = List.copyOf(changes);
    shares = List.copyOf(shares);
    duplicates = List.copyOf(duplicates);
  }

  /**
   * Maps a recompute.
   *
   * @param kind class, computation, sign and approval of the request
   * @param r recompute
   * @param duplicates possible duplicates
   * @return response
   */
  public static RecomputeResponse from(Kind kind, Recompute r, List<String> duplicates) {
    return new RecomputeResponse(
        kind.requestClass(),
        kind.computation(),
        kind.negative(),
        kind.needsApproval(),
        r.components().stream()
            .map(c -> new ChangeView(c.component().name(), c.before(), c.delta(), c.after()))
            .toList(),
        r.shares().stream()
            .map(
                x ->
                    new ShareView(
                        x.insurerCode(),
                        x.sharePct(),
                        x.lead(),
                        x.premiumDelta(),
                        x.commissionDelta(),
                        x.vatDelta()))
            .toList(),
        r.premium().total(),
        r.commission().commission(),
        r.commission().vatOnCommission(),
        r.settlement(),
        r.serviceInvoice(),
        r.baseline(),
        r.quotationRequired(),
        duplicates);
  }

  /**
   * What kind of request is recomputed.
   *
   * @param requestClass class
   * @param computation computation
   * @param negative reduces the invoice
   * @param needsApproval approval needed
   */
  public record Kind(
      String requestClass, String computation, boolean negative, boolean needsApproval) {}
}

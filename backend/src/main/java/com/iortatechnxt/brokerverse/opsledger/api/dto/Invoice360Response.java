package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotal;
import com.iortatechnxt.brokerverse.opsledger.service.Invoice360Service.BookingRefs;
import com.iortatechnxt.brokerverse.opsledger.service.Invoice360Service.Invoice360;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.RelatedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.Section;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The invoice 360 view (RMTID.026/038, ADJID.024).
 *
 * @param invoice invoice with components and shares
 * @param booking booked invoice id, service invoice and GL journals
 * @param movements movements in posting order
 * @param history status, flag and lock history
 * @param adjustments cumulative adjustments of the original invoice, null when none
 * @param related records of the Operations modules by section
 */
public record Invoice360Response(
    OpsInvoiceResponse invoice,
    BookingRefs booking,
    List<MovementResponse> movements,
    List<StatusChangeResponse> history,
    Adjustments adjustments,
    Map<Section, List<RelatedItem>> related) {

  /**
   * Maps the view.
   *
   * @param v view
   * @return response
   */
  public static Invoice360Response from(Invoice360 v) {
    return new Invoice360Response(
        OpsInvoiceResponse.from(v.invoice()),
        v.booking(),
        v.movements().stream().map(MovementResponse::from).toList(),
        v.history().stream().map(StatusChangeResponse::from).toList(),
        v.adjustments() == null ? null : Adjustments.from(v.adjustments()),
        v.related());
  }

  /**
   * Cumulative adjustments of the original invoice (ADJID.028).
   *
   * @param originalInvoiceNo original invoice
   * @param originalPremium original premium
   * @param originalDtip original DTIP
   * @param originalCommission original commission
   * @param adjustedPremium adjustments of the premium
   * @param adjustedDtip adjustments of the DTIP
   * @param adjustedCommission adjustments of the commission
   * @param adjustmentCount number of adjusting invoices
   * @param overAdjusted premium or DTIP below zero
   */
  public record Adjustments(
      String originalInvoiceNo,
      BigDecimal originalPremium,
      BigDecimal originalDtip,
      BigDecimal originalCommission,
      BigDecimal adjustedPremium,
      BigDecimal adjustedDtip,
      BigDecimal adjustedCommission,
      int adjustmentCount,
      boolean overAdjusted) {

    static Adjustments from(OpsInvoiceAdjustmentTotal t) {
      return new Adjustments(
          t.getOriginalInvoiceNo(),
          t.getOriginalPremium(),
          t.getOriginalDtip(),
          t.getOriginalCommission(),
          t.getAdjustedPremium(),
          t.getAdjustedDtip(),
          t.getAdjustedCommission(),
          t.getAdjustmentCount(),
          t.isOverAdjusted());
    }
  }
}

package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Adjustments tab of the invoice 360 view (ADJID.020/024): endorsement requests raised on the
 * invoice, or whose posting booked it, with their premium change and stage.
 */
@Component
@Transactional(readOnly = true)
public class AdjustmentRelatedItems implements InvoiceRelatedItems {

  private final AdjustmentQueryService queries;

  /**
   * Creates the source.
   *
   * @param queries requests of an invoice
   */
  public AdjustmentRelatedItems(AdjustmentQueryService queries) {
    this.queries = queries;
  }

  @Override
  public Section section() {
    return Section.ADJUSTMENTS;
  }

  @Override
  public List<RelatedItem> itemsFor(String invoiceNo) {
    return queries.forInvoice(invoiceNo).stream().map(AdjustmentRelatedItems::item).toList();
  }

  private static RelatedItem item(EndorsementRequest r) {
    BigDecimal premium =
        r.getChanges().stream()
            .filter(c -> c.component() == LedgerComponent.DTIP)
            .map(ComponentChange::delta)
            .findFirst()
            .orElse(null);
    return new RelatedItem(
        "ENDORSEMENT_REQUEST",
        r.getRequestNo(),
        r.getTerms().effectiveDate(),
        premium,
        r.getStage().name(),
        r.getTerms().endorsementType() + ": " + r.getTerms().description(),
        Adjustments.link(r.getId()));
  }
}

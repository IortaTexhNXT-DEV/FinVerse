package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production reconciliation on the Operations home and the invoice 360 view (BRQID.003, RMTID.026):
 * cycles to send, awaiting the insurer, reconciling, and open unbooked production; the
 * reconciliation items of an invoice in the Reconciliation section.
 */
@Component
@Transactional(readOnly = true)
public class ProdReconWorkCounts implements OpsWorkCountSource, InvoiceRelatedItems {

  private static final String CYCLES = "/prodrecon/cycles?stage=";

  private final ReconCycleRepository cycles;
  private final ReconItemRepository items;

  /**
   * Creates the source.
   *
   * @param cycles cycles
   * @param items items
   */
  public ProdReconWorkCounts(ReconCycleRepository cycles, ReconItemRepository items) {
    this.cycles = cycles;
    this.items = items;
  }

  @Override
  public List<WorkCount> counts(Long companyId) {
    return List.of(
        tile("RECON_TO_SEND", "Registers to send", companyId, ReconCycle.EXTRACTED, Severity.INFO),
        tile(
            "RECON_AWAITING",
            "Awaiting insurer feedback",
            companyId,
            ReconCycle.SENT,
            Severity.INFO),
        tile(
            "RECON_RECONCILING",
            "Cycles reconciling",
            companyId,
            ReconCycle.RECONCILING,
            Severity.WARNING),
        new WorkCount(
            OpsWorkCountSource.Section.PRODRECON,
            "RECON_UNBOOKED",
            "Unbooked insurer production",
            items.countUnbooked(companyId, List.of(UnbookedStatus.OPEN, UnbookedStatus.PREBOOKED)),
            Severity.WARNING,
            "/prodrecon/unbooked"));
  }

  private WorkCount tile(
      String key, String label, Long companyId, String stage, Severity severity) {
    return new WorkCount(
        OpsWorkCountSource.Section.PRODRECON,
        key,
        label,
        cycles.countByCompanyIdAndStageAndClosedFalse(companyId, stage),
        severity,
        CYCLES + stage);
  }

  @Override
  public InvoiceRelatedItems.Section section() {
    return InvoiceRelatedItems.Section.RECONCILIATION;
  }

  @Override
  public List<RelatedItem> itemsFor(String invoiceNo) {
    List<ReconItem> found = items.findByInvoiceNoOrderByIdDesc(invoiceNo);
    Map<Long, ReconCycle> byId =
        cycles.findAllById(found.stream().map(ReconItem::getCycleId).distinct().toList()).stream()
            .collect(Collectors.toMap(ReconCycle::getId, Function.identity()));
    List<RelatedItem> out = new ArrayList<>();
    for (ReconItem item : found) {
      ReconCycle cycle = byId.get(item.getCycleId());
      out.add(
          new RelatedItem(
              "RECON_ITEM",
              cycle == null ? String.valueOf(item.getCycleId()) : cycle.getCycleNo(),
              cycle == null ? item.getBookingDate() : cycle.getProductionMonth(),
              item.getBdoi() == null ? null : item.getBdoi().grossPremium(),
              item.getStatus().name(),
              item.getDiscrepancies() == null
                  ? "Production reconciliation"
                  : "Differs: " + item.getDiscrepancies(),
              "/prodrecon/cycles/" + item.getCycleId()));
    }
    return out;
  }
}

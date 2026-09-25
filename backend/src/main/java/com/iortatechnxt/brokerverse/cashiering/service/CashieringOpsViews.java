package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PickupStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTagRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItemRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequestRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.domain.PrebookedRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering on the Operations screens: the work tiles of the Operations home ({@code
 * OpsWorkCountSource}, BRQID.003) and the Receipts tab of Invoice 360 ({@code InvoiceRelatedItems},
 * RMTID.026 / ADJID.024).
 */
@Component
@Transactional(readOnly = true)
public class CashieringOpsViews implements OpsWorkCountSource, InvoiceRelatedItems {

  private static final String UNAPPLIED_LINK = "/cashiering/unapplied";

  private final UnappliedRepository unapplied;
  private final PrebookedRepository prebooked;
  private final ReceiptActionRepository actions;
  private final PdcItemRepository pdcs;
  private final PickupRequestRepository pickups;
  private final CwtTagRepository tags;
  private final ApplicationRepository applications;
  private final CashReceiptRepository receipts;
  private final Clock clock;

  /**
   * Creates the views.
   *
   * @param unapplied unapplied items
   * @param prebooked pre-booked items
   * @param actions receipt actions
   * @param pdcs PDC warehouse
   * @param pickups pick-up requests
   * @param tags 2307 tags
   * @param applications applications
   * @param receipts receipts
   * @param clock clock
   */
  public CashieringOpsViews(
      UnappliedRepository unapplied,
      PrebookedRepository prebooked,
      ReceiptActionRepository actions,
      PdcItemRepository pdcs,
      PickupRequestRepository pickups,
      CwtTagRepository tags,
      ApplicationRepository applications,
      CashReceiptRepository receipts,
      Clock clock) {
    this.unapplied = unapplied;
    this.prebooked = prebooked;
    this.actions = actions;
    this.pdcs = pdcs;
    this.pickups = pickups;
    this.tags = tags;
    this.applications = applications;
    this.receipts = receipts;
    this.clock = clock;
  }

  @Override
  public List<WorkCount> counts(Long companyId) {
    LocalDate today = LocalDate.now(clock);
    return List.of(
        tile(
            "UNAPPLIED",
            "Unapplied payments",
            unapplied.countByCompanyIdAndStage(companyId, Unapplied.STAGE_INITIAL),
            Severity.WARNING,
            UNAPPLIED_LINK),
        tile(
            "DISPOSITION_APPROVAL",
            "Dispositions for approval",
            unapplied.countByCompanyIdAndStage(companyId, "FOR_APPROVAL"),
            Severity.WARNING,
            UNAPPLIED_LINK),
        tile(
            "PREBOOKED",
            "Pre-booked payments",
            prebooked.countByCompanyIdAndStatus(companyId, Prebooked.OPEN),
            Severity.INFO,
            "/cashiering/prebooked"),
        tile(
            "RECEIPT_APPROVAL",
            "Cancellations and reinstatements for approval",
            actions.countByCompanyIdAndStage(companyId, "FOR_APPROVAL"),
            Severity.WARNING,
            "/cashiering/receipts"),
        tile(
            "PDC_WAREHOUSED",
            "Post-dated checks in the warehouse",
            pdcs.countByCompanyIdAndStatus(companyId, PdcStatus.WAREHOUSED),
            Severity.INFO,
            "/cashiering/pdc"),
        tile(
            "PICKUP_DUE",
            "Check pick-ups due",
            pickups.countByCompanyIdAndStatusAndPickupDateLessThanEqual(
                companyId, PickupStatus.FOR_PICKUP, today),
            Severity.WARNING,
            "/cashiering/pickups"),
        tile(
            "CWT_VALIDATING",
            "BIR 2307 to validate",
            tags.countByCompanyIdAndStage(companyId, "VALIDATING")
                + tags.countByCompanyIdAndStage(companyId, "TAGGED"),
            Severity.INFO,
            "/cashiering/cwt"));
  }

  private static WorkCount tile(
      String key, String label, long count, Severity severity, String link) {
    return new WorkCount(
        OpsWorkCountSource.Section.CASHIERING,
        "CSH_" + key,
        label,
        count,
        count > 0 ? severity : Severity.INFO,
        link);
  }

  @Override
  public InvoiceRelatedItems.Section section() {
    return InvoiceRelatedItems.Section.RECEIPTS;
  }

  @Override
  public List<RelatedItem> itemsFor(String invoiceNo) {
    List<RelatedItem> items = new ArrayList<>();
    for (Application a : applications.findByInvoiceNoOrderByIdAsc(invoiceNo)) {
      Receipt r =
          a.getReceiptId() == null ? null : receipts.findById(a.getReceiptId()).orElse(null);
      items.add(
          new RelatedItem(
              r == null ? "Application" : "Application of " + r.getKind(),
              r == null ? a.reference() : r.getReceiptNo(),
              a.getValueDate(),
              a.getAmount(),
              a.getStatus(),
              a.getSource() + " " + a.reference(),
              r == null ? UNAPPLIED_LINK : "/cashiering/receipts/" + r.getId()));
    }
    return items;
  }
}

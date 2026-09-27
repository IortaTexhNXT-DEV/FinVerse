package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsement;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@code ENDORSEMENT_PENDING} (BRRN.032; FR-RN-027): endorsements update the expiring (mother)
 * policy before the renewal is approved. While an endorsement request on the expiring invoice
 * family is neither posted nor cancelled the check fails, and posting, the Renewal Advice and the
 * acceptance are refused. Each endorsement of the family - booked endorsement invoices and
 * Adjustment requests - is linked to the renewal with its status (BRRN.032 AC 3).
 */
@Component
public class EndorsementPendingCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "ENDORSEMENT_PENDING";

  private final AdjustmentQueryService adjustments;
  private final CandidateEndorsementRepository links;

  /**
   * Creates the check.
   *
   * @param adjustments endorsement requests
   * @param links endorsements linked to renewals
   */
  public EndorsementPendingCheck(
      AdjustmentQueryService adjustments, CandidateEndorsementRepository links) {
    this.adjustments = adjustments;
    this.links = links;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    if (!context.bibs()) {
      return Verdict.notApplicable("Endorsements are read from the BIBS ledger only");
    }
    RenewalCandidate c = context.candidate();
    Map<String, EndorsementRequest> requests = new LinkedHashMap<>();
    for (OpsInvoice invoice : context.family()) {
      if (!invoice.getInvoiceNo().equals(c.getExpiringInvoiceNo())) {
        link(c, invoice.getInvoiceNo(), CandidateEndorsement.BOOKING, "POSTED");
      }
      adjustments
          .forInvoice(invoice.getInvoiceNo())
          .forEach(r -> requests.put(r.getRequestNo(), r));
    }
    List<String> open = new ArrayList<>();
    for (EndorsementRequest r : requests.values()) {
      link(c, r.getRequestNo(), CandidateEndorsement.ADJUSTMENT, r.getStage().name());
      if (r.getStage().isOpen()) {
        open.add(r.getRequestNo());
      }
    }
    if (!open.isEmpty()) {
      return Verdict.fail(
          "Endorsement in progress (" + String.join(", ", open) + ")", String.join(",", open));
    }
    return Verdict.pass(
        requests.isEmpty() ? "No endorsement in progress" : "Endorsements posted or cancelled");
  }

  private void link(RenewalCandidate c, String reference, String source, String status) {
    links
        .findByCandidateIdAndReference(c.getId(), reference)
        .ifPresentOrElse(
            e -> e.seen(status),
            () -> links.save(new CandidateEndorsement(c.getId(), reference, source, status, null)));
  }
}

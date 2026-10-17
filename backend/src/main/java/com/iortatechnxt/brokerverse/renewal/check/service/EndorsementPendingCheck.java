package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsement;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
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

  /**
   * Parameter: ANY (BDOI: every endorsement of the term routes the renewal to Review) or
   * IN_PROGRESS (only an endorsement in progress).
   */
  public static final String ROUTE = "RNW_ENDORSEMENT_ROUTE";

  private final AdjustmentQueryService adjustments;
  private final CandidateEndorsementRepository links;
  private final SystemParameterService parameters;

  /**
   * Creates the check.
   *
   * @param adjustments endorsement requests
   * @param links endorsements linked to renewals
   * @param parameters system parameters (route of the endorsements of the term)
   */
  public EndorsementPendingCheck(
      AdjustmentQueryService adjustments,
      CandidateEndorsementRepository links,
      SystemParameterService parameters) {
    this.adjustments = adjustments;
    this.links = links;
    this.parameters = parameters;
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
    return termVerdict(context, requests);
  }

  private Verdict termVerdict(CheckContext context, Map<String, EndorsementRequest> requests) {
    String expiring = context.candidate().getExpiringInvoiceNo();
    List<String> term = new ArrayList<>(requests.keySet());
    context.family().stream()
        .map(OpsInvoice::getInvoiceNo)
        .filter(n -> !n.equals(expiring))
        .forEach(term::add);
    if (!term.isEmpty() && "ANY".equals(parameters.text(ROUTE, "ANY").strip())) {
      return new Verdict(
          CheckOutcome.WARN,
          "Endorsements of the expiring term (" + String.join(", ", term) + "): review",
          String.join(",", term));
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

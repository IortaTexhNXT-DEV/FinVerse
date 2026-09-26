package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Checks of a request against its invoice and the other requests (ADJID.001/023):
 *
 * <ul>
 *   <li>the invoice is booked, not a return invoice, not cancelled or written off, and not locked
 *       by another module (remittance queue);
 *   <li>no cancellation together with another open financial request on the same invoice (e.g. no
 *       TSI change with a partial cancellation);
 *   <li>duplicates: same invoice, request type, reason and endorsement reference.
 * </ul>
 */
@Component
public class RequestConflicts {

  private final EndorsementRequestRepository requests;

  /**
   * Creates the checks.
   *
   * @param requests requests of an invoice
   */
  public RequestConflicts(EndorsementRequestRepository requests) {
    this.requests = requests;
  }

  /**
   * Refuses an invoice that cannot be adjusted.
   *
   * @param invoice invoice
   */
  public void requireEligible(OpsInvoice invoice) {
    if (invoice.getKind().isNegative()) {
      throw new BusinessRuleException(
          "ADJ_RETURN_INVOICE",
          invoice.getInvoiceNo() + " is a return invoice: raise the request on the original");
    }
    if (invoice.isCancelled() || invoice.isWrittenOff()) {
      throw new BusinessRuleException(
          "ADJ_INVOICE_CLOSED", invoice.getInvoiceNo() + " is cancelled or written off");
    }
    invoice.requireNotLockedByOther(Adjustments.MODULE);
  }

  /**
   * Refuses a cancellation together with another open financial request on the invoice.
   *
   * @param self the request itself, null for a new one
   * @param invoiceNo invoice
   * @param computation computation of the request
   */
  public void requireCompatible(
      EndorsementRequest self, String invoiceNo, Computation computation) {
    if (!computation.isFinancial()) {
      return;
    }
    for (EndorsementRequest other : requests.findBySubjectInvoiceNoOrderByIdDesc(invoiceNo)) {
      boolean same = self != null && other.getId().equals(self.getId());
      boolean conflict =
          other.getComputation().isFinancial()
              && (other.getComputation().isCancellation() || computation.isCancellation());
      if (!same && other.getStage().isOpen() && conflict) {
        throw new BusinessRuleException(
            "ADJ_INCOMPATIBLE_REQUEST",
            "Request "
                + other.getRequestNo()
                + " ("
                + other.getComputation()
                + ") is still open on "
                + invoiceNo
                + ": a cancellation cannot be combined with another financial change");
      }
    }
  }

  /**
   * Requests that duplicate a draft (ADJID.023): same invoice, request type, reason and endorsement
   * reference, not cancelled.
   *
   * @param draft draft
   * @param selfId the request itself, null for a new one
   * @return duplicates, newest first
   */
  public List<EndorsementRequest> duplicates(RequestDraft draft, Long selfId) {
    RequestTerms t = draft.terms();
    return requests.findBySubjectInvoiceNoOrderByIdDesc(draft.invoiceNo()).stream()
        .filter(r -> !r.getId().equals(selfId))
        .filter(r -> r.getStage() != RequestStage.CANCELLED)
        .filter(r -> Objects.equals(r.getTerms().requestType(), t.requestType()))
        .filter(r -> Objects.equals(r.getTerms().reasonCode(), t.reasonCode()))
        .filter(r -> Objects.equals(r.getTerms().endorsementRef(), t.endorsementRef()))
        .toList();
  }
}

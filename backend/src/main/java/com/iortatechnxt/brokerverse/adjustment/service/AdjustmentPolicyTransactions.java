package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestClass;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.service.port.PolicyTransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The endorsement and cancellation requests of a policy for its transaction history (ADJID.022/
 * 024): each request with its business type label, stage, recorded change per component and the GL
 * journals its posting kept; a request whose posting moved the client's excess payments to an
 * unapplied item also gives the refund, with the journals of the re-application.
 */
@Component
@Transactional(readOnly = true)
public class AdjustmentPolicyTransactions implements PolicyTransactionSource {

  private static final Map<RequestStage, String> STAGE_LABELS = new EnumMap<>(RequestStage.class);

  static {
    STAGE_LABELS.put(RequestStage.DRAFT, "Draft");
    STAGE_LABELS.put(RequestStage.FOR_VALIDATION, "For Validation");
    STAGE_LABELS.put(RequestStage.FOR_APPROVAL, "For Approval");
    STAGE_LABELS.put(RequestStage.FOR_POSTING, "For Posting");
    STAGE_LABELS.put(RequestStage.AWAITING_REAPPLICATION, "Payments to Re-apply");
    STAGE_LABELS.put(RequestStage.POSTED, "Posted");
    STAGE_LABELS.put(RequestStage.RETURNED, "Returned");
    STAGE_LABELS.put(RequestStage.CANCELLED, "Cancelled");
  }

  private final EndorsementRequestRepository requests;
  private final LedgerEffects effects;
  private final LovService lovs;

  /**
   * Creates the source.
   *
   * @param requests requests
   * @param effects journals of the re-application of payments
   * @param lovs type labels
   */
  public AdjustmentPolicyTransactions(
      EndorsementRequestRepository requests, LedgerEffects effects, LovService lovs) {
    this.requests = requests;
    this.effects = effects;
    this.lovs = lovs;
  }

  /**
   * Stage of a request as users read it.
   *
   * @param stage stage
   * @return label
   */
  public static String stageLabel(RequestStage stage) {
    return STAGE_LABELS.get(stage);
  }

  @Override
  public List<SourcedTransaction> transactionsFor(Collection<String> invoiceNos) {
    if (invoiceNos.isEmpty()) {
      return List.of();
    }
    List<SourcedTransaction> found = new ArrayList<>();
    for (EndorsementRequest r : requests.findBySubjectInvoiceNoInOrderByIdAsc(invoiceNos)) {
      if (r.getStage() == RequestStage.CANCELLED) {
        continue;
      }
      r.loadCollections();
      found.addAll(transactions(r));
    }
    return found;
  }

  private List<SourcedTransaction> transactions(EndorsementRequest r) {
    boolean posted = r.getStage().isPosted();
    PostingOutcome outcome = r.outcome();
    boolean refund =
        posted && outcome.excessAmount() != null && outcome.excessAmount().signum() > 0;
    List<String> refundJournals = refund ? effects.reapplicationJournals(r) : List.of();
    List<String> journals = new ArrayList<>(r.getJournals());
    journals.removeAll(refundJournals);
    LocalDate date = dateOf(r);
    List<SourcedTransaction> rows = new ArrayList<>();
    rows.add(
        new SourcedTransaction(
            kindOf(r),
            r.getRequestNo(),
            r.getId(),
            r.getSubject().invoiceNo(),
            outcome.newInvoiceNo(),
            date,
            r.getTerms().effectiveDate(),
            lovs.label(RequestRules.TYPE_LOV, r.getTerms().endorsementType()),
            r.getTerms().requestType() == null
                ? null
                : lovs.label(RequestRules.REQUEST_TYPE_LOV, r.getTerms().requestType()),
            changesOf(r),
            r.getStage().name(),
            stageLabel(r.getStage()),
            posted,
            journals));
    if (refund) {
      rows.add(
          new SourcedTransaction(
              Kind.REFUND,
              r.getRequestNo(),
              r.getId(),
              r.getSubject().invoiceNo(),
              null,
              date,
              r.getTerms().effectiveDate(),
              "Refund to Client – Excess Payment",
              outcome.unappliedRef(),
              Map.of(),
              r.getStage().name(),
              stageLabel(r.getStage()),
              true,
              refundJournals));
    }
    return rows;
  }

  private static LocalDate dateOf(EndorsementRequest r) {
    if (r.trail().postedAt() != null) {
      return BusinessClock.dateOf(r.trail().postedAt());
    }
    return r.getCreatedAt() == null ? null : BusinessClock.dateOf(r.getCreatedAt());
  }

  private static Kind kindOf(EndorsementRequest r) {
    Computation computation = r.getComputation();
    if (computation.isCancellation()) {
      return Kind.CANCELLATION;
    }
    if (r.getRequestClass() == RequestClass.INTERNAL || computation == Computation.WRITE_OFF) {
      return Kind.ADJUSTMENT;
    }
    return Kind.ENDORSEMENT;
  }

  /** The recorded change per component; a write-off leaves the premium unchanged. */
  private static Map<LedgerComponent, BigDecimal> changesOf(EndorsementRequest r) {
    Map<LedgerComponent, BigDecimal> changes = new EnumMap<>(LedgerComponent.class);
    if (r.getComputation() == Computation.WRITE_OFF) {
      return changes;
    }
    for (ComponentChange c : r.getChanges()) {
      changes.put(c.component(), c.delta());
    }
    return changes;
  }
}

package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionAction;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequestRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRule;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.Action;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The cashiers' side of the collector requests queued by {@link CashieringDispositionRequests}
 * (BRCLXN.030-033): the queue, and accepting a request (a disposition of the workflow {@code
 * OPS_DISPOSITION} is assigned from it, so the approval rules of the disposition type stay) or
 * rejecting it. The outcome goes back as {@code UnappliedDispositionChanged} (see {@link
 * CollectorRequestTracker}).
 */
@Service
@Transactional
public class CollectorRequestService {

  /** Entity of the audit trail. */
  public static final String ENTITY = "CollectorRequest";

  /** Default cashiering disposition type of each collector action (LOV DISPOSITION_TYPE). */
  static final Map<Action, String> DEFAULT_TYPES =
      Map.of(
          Action.APPLY_TO_INVOICE, "APPLY_OTHER_INVOICE",
          Action.REFUND, "REFUND",
          Action.RECLASS, "RECLASS",
          Action.TRANSFER, "TRANSFER_UNIT");

  private static final Map<Action, Set<DispositionAction>> ALLOWED =
      Map.of(
          Action.APPLY_TO_INVOICE, Set.of(DispositionAction.APPLY, DispositionAction.DST_APPLY),
          Action.REFUND, Set.of(DispositionAction.REFUND),
          Action.RECLASS, Set.of(DispositionAction.RECLASS),
          Action.TRANSFER, Set.of(DispositionAction.TRANSFER));

  private final CollectorRequestRepository requests;
  private final UnappliedRepository items;
  private final DispositionService dispositions;
  private final CollectorRequestTracker tracker;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests collector requests
   * @param items unapplied items
   * @param dispositions unapplied workbench
   * @param tracker request status and events
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CollectorRequestService(
      CollectorRequestRepository requests,
      UnappliedRepository items,
      DispositionService dispositions,
      CollectorRequestTracker tracker,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.items = items;
    this.dispositions = dispositions;
    this.tracker = tracker;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Requests in some statuses (the Cashiering queue).
   *
   * @param companyId company
   * @param statuses statuses
   * @param text request, invoice or requester fragment, may be null
   * @param pageable page
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public Page<CollectorRequest> list(
      Long companyId,
      Collection<CollectorRequest.Status> statuses,
      String text,
      Pageable pageable) {
    String like =
        text == null || text.isBlank() ? null : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    return requests.search(companyId, statuses, like, pageable);
  }

  /**
   * Requests of an unapplied item.
   *
   * @param unappliedId item
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public List<CollectorRequest> forItem(Long unappliedId) {
    return requests.findByUnappliedIdOrderByIdDesc(unappliedId);
  }

  /**
   * The unapplied items of some requests.
   *
   * @param list requests
   * @return items by id
   */
  @Transactional(readOnly = true)
  public Map<Long, Unapplied> itemsOf(Collection<CollectorRequest> list) {
    return items
        .findAllById(list.stream().map(CollectorRequest::getUnappliedId).distinct().toList())
        .stream()
        .collect(Collectors.toMap(Unapplied::getId, u -> u));
  }

  /**
   * Requests waiting for a cashier.
   *
   * @param companyId company
   * @return count
   */
  @Transactional(readOnly = true)
  public long queuedCount(Long companyId) {
    return requests.countByCompanyIdAndStatus(companyId, CollectorRequest.Status.QUEUED);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @Transactional(readOnly = true)
  public CollectorRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Accepts a queued request: assigns the disposition to the item and, when asked, submits it at
   * once (an application is then executed; a refund, reclass or transfer goes for approval).
   *
   * @param id request
   * @param input disposition type and the fields the collector could not give
   * @return the disposition
   */
  public Disposition accept(Long id, Acceptance input) {
    CollectorRequest r = queued(id);
    Unapplied item = item(r);
    Action action = Action.valueOf(r.getAction());
    String type =
        input.dispositionType() == null ? DEFAULT_TYPES.get(action) : input.dispositionType();
    requireMatchingType(action, type);
    BigDecimal amount = firstOf(input.amount(), r.getAmount(), item.getBalance());
    Disposition d =
        dispositions.assign(
            item.getId(),
            type,
            new DispositionDetails(
                amount,
                r.getInvoiceNo(),
                blankToNull(input.targetClientCode()),
                blankToNull(input.targetUnit()),
                blankToNull(input.payeeName()),
                text(blankToNull(input.remarks()), r.getRemarks())));
    r.accept(d.getId(), currentUser.username(), clock.instant());
    tracker.publish(item, r, CollectorRequestTracker.ACCEPTED, "Disposition " + type + " assigned");
    audit.record(ENTITY, r.getRequestNo(), AuditAction.UPDATE, "Accepted as " + type);
    return input.submit() ? dispositions.submit(item.getId()) : d;
  }

  /**
   * Rejects a queued request.
   *
   * @param id request
   * @param reason reason
   * @return the request
   */
  public CollectorRequest reject(Long id, String reason) {
    if (blankToNull(reason) == null) {
      throw new BusinessRuleException("REJECT_REASON_REQUIRED", "Enter the reason for rejecting");
    }
    CollectorRequest r = queued(id);
    r.reject(currentUser.username(), clock.instant(), reason.strip());
    tracker.publish(item(r), r, CollectorRequestTracker.REJECTED, reason.strip());
    audit.record(ENTITY, r.getRequestNo(), AuditAction.REJECT, reason.strip());
    return r;
  }

  private void requireMatchingType(Action action, String type) {
    DispositionAction kind =
        dispositions.types().stream()
            .filter(t -> t.getTypeCode().equals(type))
            .map(DispositionTypeRule::getAction)
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "DISPOSITION_TYPE_NOT_CONFIGURED", "Unknown disposition type " + type));
    if (!ALLOWED.get(action).contains(kind)) {
      throw new BusinessRuleException(
          "COLLECTOR_REQUEST_TYPE_MISMATCH",
          "Disposition type " + type + " does not carry out a " + action + " request");
    }
  }

  private CollectorRequest queued(Long id) {
    CollectorRequest r = get(id);
    if (!r.isQueued()) {
      throw new BusinessRuleException(
          "COLLECTOR_REQUEST_DECIDED", r.getRequestNo() + " is already " + r.getStatus());
    }
    return r;
  }

  private Unapplied item(CollectorRequest r) {
    return items
        .findById(r.getUnappliedId())
        .orElseThrow(
            () -> new ResourceNotFoundException(UnappliedService.ENTITY, r.getUnappliedId()));
  }

  private static BigDecimal firstOf(BigDecimal a, BigDecimal b, BigDecimal c) {
    if (a != null) {
      return a;
    }
    return b != null ? b : c;
  }

  private static String text(String value, String fallback) {
    return value == null ? fallback : value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * What a cashier adds when accepting a request.
   *
   * @param dispositionType disposition type (LOV DISPOSITION_TYPE), null for the default of the
   *     action
   * @param amount amount, null for the requested amount or the whole balance
   * @param targetClientCode client to reclass to
   * @param targetUnit marketing unit to transfer to
   * @param payeeName refund payee, null for the payor
   * @param remarks remarks, null for the collector's
   * @param submit submit the disposition at once
   */
  public record Acceptance(
      String dispositionType,
      BigDecimal amount,
      String targetClientCode,
      String targetUnit,
      String payeeName,
      String remarks,
      boolean submit) {}
}

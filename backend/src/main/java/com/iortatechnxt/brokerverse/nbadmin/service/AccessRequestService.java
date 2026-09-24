package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User access requests (BRNB.085, BRD 3.3.5 / 3.4.2): the Business Administrator submits, the
 * Approver approves (not the requester) and the approval applies the change through {@link
 * AccessChangeApplier}; or rejects with a comment. Both steps are audited and notified.
 */
@Service
@Transactional
public class AccessRequestService {

  /** Audit entity type. */
  public static final String ENTITY = "AccessRequest";

  /** Frontend route of the access requests screen. */
  public static final String SCREEN = "/broking-setup/access-requests";

  private final AccessRequestRepository requests;
  private final AccessRequestValidator validator;
  private final AccessChangeApplier applier;
  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param validator request validation
   * @param applier applies approved changes
   * @param numbers document numbers
   * @param notifications in-app notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccessRequestService(
      AccessRequestRepository requests,
      AccessRequestValidator validator,
      AccessChangeApplier applier,
      DocumentNumberService numbers,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.validator = validator;
    this.applier = applier;
    this.numbers = numbers;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits a request for approval.
   *
   * @param content what is requested
   * @return the pending request
   */
  public AccessRequest submit(AccessRequestContent content) {
    AccessRequestContent clean = validator.validate(content);
    if (requests.existsByUsernameIgnoreCaseAndStatus(
        clean.username(), AccessRequestStatus.PENDING)) {
      throw new BusinessRuleException(
          "ACCESS_REQUEST_PENDING",
          "A request for user " + clean.username() + " is already waiting for approval");
    }
    String no = numbers.next("AR-" + LocalDate.now(clock).getYear());
    AccessRequest saved = requests.save(new AccessRequest(no, clean));
    audit.record(
        ENTITY, no, AuditAction.SUBMIT, describe(saved) + " - " + saved.getJustification());
    notifications.notifyPermission(
        "ACCESS_APPROVE",
        new Notice(
            "Access request " + no + " to approve",
            describe(saved),
            link(saved),
            ENTITY,
            String.valueOf(saved.getId())));
    return saved;
  }

  /**
   * Approves a request and applies the change (four eyes: not the requester).
   *
   * @param id request
   * @param comment optional comment
   * @return the decision, with the temporary password of a created user (shown once)
   */
  public Decision approve(Long id, String comment) {
    AccessRequest request = decide(id, true, comment);
    String temporaryPassword = applier.apply(request);
    audit.record(
        ENTITY,
        request.getRequestNo(),
        AuditAction.AUTHORIZE,
        "Approved and applied: " + describe(request) + note(comment));
    notifyRequester(request, "approved");
    return new Decision(request, temporaryPassword);
  }

  /**
   * Rejects a request with a comment.
   *
   * @param id request
   * @param comment reason (mandatory)
   * @return the decision
   */
  public Decision reject(Long id, String comment) {
    if (comment == null || comment.isBlank()) {
      throw new BusinessRuleException("ACCESS_REJECT_REASON", "Enter the reason of the rejection");
    }
    AccessRequest request = decide(id, false, comment);
    audit.record(
        ENTITY,
        request.getRequestNo(),
        AuditAction.REJECT,
        "Rejected: " + describe(request) + note(comment));
    notifyRequester(request, "rejected");
    return new Decision(request, null);
  }

  /**
   * One request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public AccessRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Requests matching the filter, newest first by the pageable sort.
   *
   * @param status status, null for all
   * @param type request type, null for all
   * @param text user name or request number fragment, null for all
   * @param pageable page
   * @return requests
   */
  @Transactional(readOnly = true)
  public Page<AccessRequest> search(
      AccessRequestStatus status, AccessRequestType type, String text, Pageable pageable) {
    Specification<AccessRequest> spec =
        (root, query, cb) -> {
          List<Predicate> p = new ArrayList<>();
          if (status != null) {
            p.add(cb.equal(root.get("status"), status));
          }
          if (type != null) {
            p.add(cb.equal(root.get("requestType"), type));
          }
          if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            p.add(
                cb.or(
                    cb.like(cb.lower(root.get("username")), like),
                    cb.like(cb.lower(root.get("requestNo")), like)));
          }
          return cb.and(p.toArray(Predicate[]::new));
        };
    return requests.findAll(spec, pageable);
  }

  /**
   * Pending requests (approval inbox).
   *
   * @return pending requests, oldest first
   */
  @Transactional(readOnly = true)
  public List<AccessRequest> pending() {
    return requests.findByStatusOrderByIdAsc(AccessRequestStatus.PENDING);
  }

  private AccessRequest decide(Long id, boolean approved, String comment) {
    AccessRequest request = get(id);
    String approver = currentUser.username();
    if (CurrentUser.sameUser(approver, request.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_FOUR_EYES", "A request cannot be decided by the user who submitted it");
    }
    request.decide(approved, approver, clock.instant(), blankToNull(comment));
    return request;
  }

  private void notifyRequester(AccessRequest request, String outcome) {
    notifications.notifyUser(
        request.getCreatedBy(),
        new Notice(
            "Access request " + request.getRequestNo() + " " + outcome,
            describe(request) + note(request.getDecisionComment()),
            link(request),
            ENTITY,
            String.valueOf(request.getId())));
  }

  /**
   * One-line description of a request.
   *
   * @param r request
   * @return description
   */
  public static String describe(AccessRequest r) {
    return switch (r.getRequestType()) {
      case CREATE_USER -> "Create user " + r.getUsername() + " with roles " + r.roles();
      case MODIFY_ROLES -> "Change roles of " + r.getUsername() + " to " + r.roles();
      case DISABLE_USER -> "Disable user " + r.getUsername();
      case ENABLE_USER -> "Enable user " + r.getUsername();
    };
  }

  private static String link(AccessRequest r) {
    return SCREEN + "?id=" + r.getId();
  }

  private static String note(String comment) {
    return comment == null || comment.isBlank() ? "" : " (" + comment + ")";
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * The outcome of a decision.
   *
   * @param request the decided request
   * @param temporaryPassword temporary password of a created user (never stored), else null
   */
  public record Decision(AccessRequest request, String temporaryPassword) {

    @Override
    public String toString() {
      return "Decision[" + request.getRequestNo() + ", ***]";
    }
  }
}

package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestEvent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User access requests (BRNB.085; BRD 1.002-1.009, 3.002) and role-permission change requests
 * (PMADD05): the requester saves a draft, edits it and submits it to the approver(s) of their
 * choice; the decisions are taken by {@link AccessDecisionService}, the return, correction and
 * cancellation by {@link AccessRequestReturnService}. Each step is kept in the request history and
 * the audit trail, and notified (USER_ACCESS_DESIGN section 4.3).
 */
@Service
@Transactional
public class AccessRequestService {

  /** Audit entity type. */
  public static final String ENTITY = "AccessRequest";

  /** Frontend route of the access requests screen. */
  public static final String SCREEN = "/user-access/requests";

  private final AccessRequestRepository requests;
  private final AccessRequestValidator validator;
  private final AccessApprovers approvers;
  private final AccessRiskRules risks;
  private final AccessRequestHistory history;
  private final AccessRequestNotifier notifier;
  private final AccessRequestVisibility visibility;
  private final AccessDecisionService decisions;
  private final AccessRequestPermissions permissions;
  private final DocumentNumberService numbers;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param validator request checks
   * @param approvers approver checks
   * @param risks risk rules (second approval)
   * @param history request history and audit trail
   * @param notifier notifications
   * @param visibility who sees which request
   * @param decisions approval and rejection
   * @param permissions request functions of the current user
   * @param numbers document numbers
   * @param currentUser current user
   * @param clock clock
   */
  public AccessRequestService(
      AccessRequestRepository requests,
      AccessRequestValidator validator,
      AccessApprovers approvers,
      AccessRiskRules risks,
      AccessRequestHistory history,
      AccessRequestNotifier notifier,
      AccessRequestVisibility visibility,
      AccessDecisionService decisions,
      AccessRequestPermissions permissions,
      DocumentNumberService numbers,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.validator = validator;
    this.approvers = approvers;
    this.risks = risks;
    this.history = history;
    this.notifier = notifier;
    this.visibility = visibility;
    this.decisions = decisions;
    this.permissions = permissions;
    this.numbers = numbers;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates and submits a request in one step (the compatible submission of BRNB.085 and PMADD05):
   * any holder of the approval right may decide it.
   *
   * @param content what is requested
   * @return the pending request
   */
  public AccessRequest submit(AccessRequestContent content) {
    return create(content, false, null);
  }

  /**
   * Creates a request: saved as a draft (light checks), or submitted at once to the chosen
   * approvers (full checks; FR-UA-010).
   *
   * @param content what is requested
   * @param draft true to save a draft
   * @param chosenApprovers approvers in order; null for the compatible one-step submission
   * @return the draft or pending request
   */
  public AccessRequest create(
      AccessRequestContent content, boolean draft, List<String> chosenApprovers) {
    permissions.requireRequestPermission(content);
    AccessRequestContent clean =
        draft ? validator.validateDraft(content) : validator.validate(content);
    return create(clean, draft, chosenApprovers, null);
  }

  /**
   * Creates a validated request, as a line of a bulk batch when a batch is given.
   *
   * @param clean validated content
   * @param draft true to save a draft
   * @param chosenApprovers approvers in order; null for the compatible one-step submission
   * @param batchId bulk batch, null for none
   * @return the request
   */
  AccessRequest create(
      AccessRequestContent clean, boolean draft, List<String> chosenApprovers, Long batchId) {
    String no = numbers.next("AR-" + LocalDate.now(clock).getYear());
    AccessRequest saved = requests.save(new AccessRequest(no, clean, batchId));
    history.record(saved, AccessRequestAction.SAVE, null, clean.justification());
    if (!draft) {
      submitSaved(saved, chosenApprovers, AccessRequestAction.SUBMIT, null);
    }
    return saved;
  }

  /**
   * Edits a draft, or corrects a returned request (BRD 1.002.1.2, 1.006.1; creator only).
   *
   * @param id request
   * @param content new content
   * @return the request
   */
  public AccessRequest edit(Long id, AccessRequestContent content) {
    AccessRequest r = requireCreator(get(id), "edit");
    permissions.requireCorrectionRight(r);
    AccessRequestContent clean = validator.validateDraft(content);
    AccessRequestStatus from = r.getStatus();
    r.edit(clean);
    history.record(r, AccessRequestAction.EDIT, from, null);
    return r;
  }

  /**
   * Submits a draft, or resubmits a corrected request, to the chosen approvers after the full
   * checks (BRD 1.002.1.4, 1.006.1.5; FR-UA-010, FR-UA-016).
   *
   * @param id request
   * @param chosenApprovers approvers in order; null lets any approver decide (compatibility of the
   *     one-step submission and of its correction)
   * @param remarks remarks; the correction remark is mandatory on a resubmission
   * @return the pending request
   */
  public AccessRequest submit(Long id, List<String> chosenApprovers, String remarks) {
    AccessRequest r = requireCreator(get(id), "submit");
    boolean resubmission = r.getStatus() == AccessRequestStatus.RETURNED;
    if (resubmission) {
      permissions.requireCorrectionRight(r);
      if (remarks == null || remarks.isBlank()) {
        throw new BusinessRuleException(
            "ACCESS_CORRECTION_REMARKS", "Enter the correction remarks");
      }
    }
    permissions.requireRequestPermission(r.content());
    r.edit(validator.validateSubmission(r.content(), r.getId()));
    submitSaved(
        r,
        chosenApprovers,
        resubmission ? AccessRequestAction.RESUBMIT : AccessRequestAction.SUBMIT,
        remarks == null || remarks.isBlank() ? null : remarks.trim());
    return r;
  }

  /**
   * Submits a saved request (also the lines of a bulk batch).
   *
   * @param r request (draft or returned)
   * @param chosenApprovers approvers in order; null lets any approver decide (compatibility)
   * @param action SUBMIT or RESUBMIT
   * @param remarks remarks for the history
   */
  void submitSaved(
      AccessRequest r, List<String> chosenApprovers, AccessRequestAction action, String remarks) {
    List<String> names =
        chosenApprovers == null ? List.of() : approvers.validate(chosenApprovers, r.content());
    AccessRequestStatus from = r.getStatus();
    r.submit(names, risks.evaluate(r.content()), currentUser.username(), clock.instant());
    history.record(r, action, from, remarks == null ? r.getJustification() : remarks);
    notifier.toApprove(r, AccessApprovers.approvalPermission(r.getUserType()));
  }

  /**
   * Approves a request (see {@link AccessDecisionService#approve}).
   *
   * @param id request
   * @param comment optional comment
   * @return the decision
   */
  public Decision approve(Long id, String comment) {
    return decisions.approve(id, comment);
  }

  /**
   * Rejects a request (see {@link AccessDecisionService#reject}).
   *
   * @param id request
   * @param comment reason (mandatory)
   * @return the decision
   */
  public Decision reject(Long id, String comment) {
    return decisions.reject(id, comment);
  }

  /**
   * One request by number (bulk approval from the inbox).
   *
   * @param requestNo request number
   * @return request
   */
  @Transactional(readOnly = true)
  public AccessRequest byNumber(String requestNo) {
    return requests
        .findByRequestNo(requestNo)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, requestNo));
  }

  /**
   * One request (no visibility check: internal use).
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public AccessRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * One request the current user may see (FR-UA-018; a draft only its creator).
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public AccessRequest view(Long id) {
    AccessRequest r = get(id);
    if (!visibility.canSee(r)) {
      throw new AccessDeniedException("You are not permitted to open request " + r.getRequestNo());
    }
    return r;
  }

  /**
   * History of a request the current user may see (BRD 1.008; FR-UA-018).
   *
   * @param id request
   * @return events, oldest first
   */
  @Transactional(readOnly = true)
  public List<AccessRequestEvent> history(Long id) {
    return history.of(view(id).getId());
  }

  /**
   * Work list of the current user (FR-UA-017, FR-UA-018).
   *
   * @param search tab and filters
   * @param pageable page
   * @return requests
   */
  @Transactional(readOnly = true)
  public Page<AccessRequest> search(AccessRequestSearch search, Pageable pageable) {
    return requests.findAll(visibility.specification(search), pageable);
  }

  /**
   * Requests matching the filter, for internal use (no visibility check).
   *
   * @param status status, null for all
   * @param type request type, null for all
   * @param text user name, role code or request number fragment, null for all
   * @param pageable page
   * @return requests
   */
  @Transactional(readOnly = true)
  public Page<AccessRequest> search(
      AccessRequestStatus status, AccessRequestType type, String text, Pageable pageable) {
    return requests.findAll(
        visibility.specification(
            new AccessRequestSearch(
                AccessRequestSearch.Scope.ALL, status, type, text, null, null, null, null, null)),
        pageable);
  }

  /**
   * Requests waiting for a decision or an implementation (approval inbox).
   *
   * @return requests, oldest first
   */
  @Transactional(readOnly = true)
  public List<AccessRequest> pending() {
    return requests.findByStatusInOrderByIdAsc(
        List.of(
            AccessRequestStatus.PENDING,
            AccessRequestStatus.PENDING_SECOND,
            AccessRequestStatus.FOR_IMPLEMENTATION));
  }

  private AccessRequest requireCreator(AccessRequest r, String what) {
    if (!CurrentUser.sameUser(currentUser.username(), r.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_NOT_REQUESTER", "Only the creator can " + what + " request " + r.getRequestNo());
    }
    return r;
  }

  /**
   * One-line description of a request.
   *
   * @param r request
   * @return description
   */
  public static String describe(AccessRequest r) {
    return AccessRequestDescriptions.describe(r);
  }

  /**
   * Link to the request detail.
   *
   * @param r request
   * @return frontend route
   */
  static String link(AccessRequest r) {
    return SCREEN + "/" + r.getId();
  }

  static String note(String comment) {
    return comment == null || comment.isBlank() ? "" : " (" + comment + ")";
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

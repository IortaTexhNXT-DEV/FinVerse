package com.iortatechnxt.brokerverse.brokerclaims.home.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandler;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandlerRepository;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimEventRecorder;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimLookup;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimWorkflow;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.StatusMatrix;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reassignment of claims between handlers (NFR p.37, FR-CM-055): a TL, TH or UH ({@code
 * WORK_ASSIGN}) hands one claim or a selection to another user who may record claims; the claim
 * takes the new handler's unit from the register, the workflow case follows, the change is kept in
 * the claim timeline, and the new handler is notified once ({@code BCL_CLAIM_ASSIGNED}).
 */
@Service
@Transactional
public class ClaimAssignmentService {

  /** Notification event of an assignment. */
  static final String ASSIGNED_EVENT = "BCL_CLAIM_ASSIGNED";

  private final ClaimLookup lookup;
  private final ClaimWorkflow workflow;
  private final ClaimEventRecorder recorder;
  private final StatusMatrix matrix;
  private final ClaimHandlerRepository handlers;
  private final UserDirectory directory;
  private final NotificationService notifications;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param lookup claims of the company
   * @param workflow workflow wiring
   * @param recorder claim timeline
   * @param matrix handler units
   * @param handlers claims handler register
   * @param directory users
   * @param notifications in-app notifications
   * @param currentUser current user
   */
  public ClaimAssignmentService(
      ClaimLookup lookup,
      ClaimWorkflow workflow,
      ClaimEventRecorder recorder,
      StatusMatrix matrix,
      ClaimHandlerRepository handlers,
      UserDirectory directory,
      NotificationService notifications,
      CurrentUser currentUser) {
    this.lookup = lookup;
    this.workflow = workflow;
    this.recorder = recorder;
    this.matrix = matrix;
    this.handlers = handlers;
    this.directory = directory;
    this.notifications = notifications;
    this.currentUser = currentUser;
  }

  /**
   * The users a claim may be assigned to (holders of BCL_RECORD) with their unit and team.
   *
   * @return handlers in user order
   */
  @Transactional(readOnly = true)
  public List<Assignee> assignees() {
    Map<String, ClaimHandler> register =
        handlers.findAllByOrderByUsernameAsc().stream()
            .collect(
                Collectors.toMap(
                    h -> h.getUsername().toLowerCase(Locale.ROOT),
                    Function.identity(),
                    (a, b) -> a));
    return directory.usersWithPermission(Permission.BCL_RECORD.name()).stream()
        .sorted()
        .map(
            u -> {
              ClaimHandler h = register.get(u.toLowerCase(Locale.ROOT));
              return new Assignee(
                  u, h == null ? null : h.getUnitCode(), h == null ? null : h.getTeam());
            })
        .toList();
  }

  /**
   * Reassigns claims to a handler.
   *
   * @param companyId company
   * @param claimIds claims
   * @param handler new handler
   * @param comment comment, may be null
   * @return the claims moved (already with that handler are skipped)
   */
  public int reassign(Long companyId, List<Long> claimIds, String handler, String comment) {
    if (handler == null || handler.isBlank()) {
      throw new BusinessRuleException("BCL_HANDLER_REQUIRED", "Select the new handler");
    }
    if (claimIds == null || claimIds.isEmpty()) {
      throw new BusinessRuleException("BCL_CLAIMS_REQUIRED", "Select the claims to reassign");
    }
    String target =
        directory.usersWithPermission(Permission.BCL_RECORD.name()).stream()
            .filter(u -> CurrentUser.sameUser(u, handler.strip()))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "BCL_HANDLER_NOT_ELIGIBLE", handler + " cannot handle claims"));
    String unit = matrix.unitOf(target).orElse(null);
    List<String> moved = new ArrayList<>();
    for (Long id : claimIds) {
      Claim claim = lookup.require(companyId, id);
      String previous = claim.getHandler();
      if (!CurrentUser.sameUser(previous, target)) {
        claim.assignTo(target, unit);
        workflow.assign(claim);
        recorder.record(
            claim, ClaimField.HANDLER, new ClaimEvent.Values(previous, target), comment);
        moved.add(claim.getClaimNo());
      }
    }
    notifyHandler(target, moved);
    return moved.size();
  }

  private void notifyHandler(String target, List<String> claimNos) {
    if (claimNos.isEmpty() || CurrentUser.sameUser(target, currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        target,
        new Notice(
            claimNos.size() + " claim(s) assigned to you",
            String.join(", ", claimNos),
            "/claims-handling/worklist?tab=MINE",
            ClaimCodes.ENTITY_TYPE,
            null),
        ASSIGNED_EVENT);
  }

  /**
   * A user who may handle claims.
   *
   * @param username user
   * @param unitCode unit in the register, null when not registered
   * @param team team in the register
   */
  public record Assignee(String username, String unitCode, String team) {}
}

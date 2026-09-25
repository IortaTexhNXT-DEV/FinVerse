package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowDefinitions;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Who receives an escalation and how they hear of it (BRCLXN.049/050): the designated user must
 * handle escalations ({@code CLX_ESCALATION_HANDLE}); without one the case waits in the stage's
 * queue and every handler is notified. The account officers of the invoices are notified too
 * ({@code CLX_ESCALATED}). Resolving the team lead or unit head of an account from the sales
 * organisation is parked until BDOI gives the escalation matrix (CQ14).
 */
@Component
public class EscalationNotices {

  /** Permission of the escalation handlers. */
  public static final String HANDLER = "CLX_ESCALATION_HANDLE";

  /** Escalation reasons. */
  public static final String REASON_LOV = "CLX_ESCALATION_REASON";

  private static final String EVENT = "CLX_ESCALATED";
  private static final int DEFAULT_SLA_HOURS = 24;

  private final UserDirectory users;
  private final LovService lovs;
  private final WorkflowDefinitions definitions;
  private final NotificationService notifications;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param users user directory
   * @param lovs reasons
   * @param definitions stage SLAs
   * @param notifications notifications
   * @param currentUser current user
   * @param clock clock
   */
  public EscalationNotices(
      UserDirectory users,
      LovService lovs,
      WorkflowDefinitions definitions,
      NotificationService notifications,
      CurrentUser currentUser,
      Clock clock) {
    this.users = users;
    this.lovs = lovs;
    this.definitions = definitions;
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Checks the receiving level and user.
   *
   * @param level level
   * @param username designated user, may be null (required for USER)
   */
  public void requireTarget(TargetLevel level, String username) {
    boolean named = username != null && !username.isBlank();
    if (level == null || level == TargetLevel.USER && !named) {
      throw new BusinessRuleException(
          "CLX_ESCALATION_TARGET", "Choose the level, and the user for a designated authority");
    }
    if (named && !users.usersWithPermission(HANDLER).contains(username.strip())) {
      throw new BusinessRuleException(
          "CLX_ESCALATION_TARGET", username + " does not handle escalations (" + HANDLER + ")");
    }
  }

  /**
   * Checks an escalation reason.
   *
   * @param reasonCode reason
   */
  public void requireReason(String reasonCode) {
    lovs.requireValid(REASON_LOV, reasonCode, LocalDate.now(clock));
  }

  /**
   * The SLA of the stage a level receives escalations in.
   *
   * @param level level
   * @return hours
   */
  public int defaultSlaHours(TargetLevel level) {
    WorkflowStage stage =
        definitions.stage(EscalationService.WORKFLOW, level.isHead() ? "WITH_UH" : "WITH_TL");
    return stage.getSlaHours() == null ? DEFAULT_SLA_HOURS : stage.getSlaHours();
  }

  /**
   * Notifies the target (or the handlers) and the account officers of a new escalation.
   *
   * @param escalation escalation
   * @param invoices escalated accounts
   * @param manual whether a user raised it
   */
  public void escalated(Escalation escalation, List<Candidate> invoices, boolean manual) {
    Notice notice =
        new Notice(
            escalation.getEscalationNo() + ": " + escalation.getAssuredName() + " escalated",
            (manual ? "Escalated by " + currentUser.username() : "Rule " + escalation.getRuleCode())
                + " - "
                + escalation.getCurrency()
                + " "
                + escalation.getTotalBalance()
                + " outstanding on "
                + invoices.size()
                + " invoice(s)",
            "/collections/escalations/" + escalation.getId(),
            EscalationService.ENTITY,
            String.valueOf(escalation.getId()));
    String actor = currentUser.optionalUsername().orElse(null);
    if (escalation.getTargetUsername() == null) {
      notifications.notifyPermission(HANDLER, notice, EVENT);
    } else {
      notifications.notifyUser(escalation.getTargetUsername(), notice, EVENT);
    }
    Set<String> officers = new LinkedHashSet<>();
    invoices.stream().map(Candidate::aoUsername).filter(Objects::nonNull).forEach(officers::add);
    officers.stream()
        .filter(u -> !u.equals(actor) && !u.equals(escalation.getTargetUsername()))
        .forEach(u -> notifications.notifyUser(u, notice, EVENT));
  }
}

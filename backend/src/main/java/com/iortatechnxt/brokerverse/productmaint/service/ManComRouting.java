package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.productmaint.domain.ManComApproval;
import com.iortatechnxt.brokerverse.productmaint.domain.ManComApprovalRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ManCom approver selection and approval routing (BDOI FRS FRPM.014.01, setting {@value #SETTING}):
 * TSU selects one or more approvers of the ManCom list; every selected approver except the
 * President approves in parallel; the President is asked only after all of them approved; the last
 * approval signs the request off. Approvers approve, reject or return for revision, with remarks
 * mandatory for a rejection or a return; a return or rejection ends the round and sends the request
 * back to TSU. Each step is told in the system and by e-mail as preferred.
 */
@Service
@Transactional
public class ManComRouting {

  /** Setting of the routing. */
  public static final String SETTING = "PM_MANCOM_ROUTING";

  /** BDOI's routing. */
  public static final String SELECTED = "SELECTED_PARALLEL_THEN_PRESIDENT";

  /** Setting: the President. */
  public static final String PRESIDENT = "PM_MANCOM_PRESIDENT";

  /** Permission of the ManCom members. */
  public static final String MEMBER = "PKG_MANCOM_SIGNOFF";

  private static final String EVENT = "PM_MANCOM_APPROVAL";
  private static final String APPROVE = "APPROVE";
  private static final String OTHERS = "OTHERS";

  private final ManComApprovalRepository tasks;
  private final PackageRequests requests;
  private final RequirementsService requirements;
  private final WorkflowService workflow;
  private final UserDirectory directory;
  private final NoticeDelivery delivery;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the routing.
   *
   * @param tasks approval tasks
   * @param requests package requests
   * @param requirements the sign-off of the request (last approval)
   * @param workflow workflow (returns)
   * @param directory ManCom members and names
   * @param delivery notices
   * @param audit audit trail
   * @param currentUser current user
   * @param parameters business parameters (routing, President)
   * @param clock clock
   */
  public ManComRouting(
      ManComApprovalRepository tasks,
      PackageRequests requests,
      RequirementsService requirements,
      WorkflowService workflow,
      UserDirectory directory,
      NoticeDelivery delivery,
      AuditTrailService audit,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.tasks = tasks;
    this.requests = requests;
    this.requirements = requirements;
    this.workflow = workflow;
    this.directory = directory;
    this.delivery = delivery;
    this.audit = audit;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Whether BDOI's routing applies.
   *
   * @return true for SELECTED_PARALLEL_THEN_PRESIDENT
   */
  @Transactional(readOnly = true)
  public boolean selectedRouting() {
    return SELECTED.equals(parameters.text(SETTING, SELECTED).strip());
  }

  /**
   * The ManCom approver list.
   *
   * @return usernames, sorted
   */
  @Transactional(readOnly = true)
  public List<String> members() {
    return directory.usersWithPermission(MEMBER).stream().sorted().toList();
  }

  /**
   * The President (approves last), blank when none is set.
   *
   * @return username
   */
  @Transactional(readOnly = true)
  public String president() {
    return parameters.text(PRESIDENT, "").strip();
  }

  /**
   * The tasks of a request, newest round first.
   *
   * @param id request
   * @return tasks
   */
  @Transactional(readOnly = true)
  public List<ManComApproval> progress(Long id) {
    return tasks.findByRequestIdOrderByRoundNoDescIdAsc(id);
  }

  /**
   * Selects the ManCom approvers of a request waiting for ManCom and creates their tasks.
   *
   * @param id request
   * @param approvers selected approvers
   * @return the tasks of the new round
   */
  public List<ManComApproval> select(Long id, List<String> approvers) {
    PackageRequest p = requests.get(id);
    if (p.getStatus() != RequestStage.FOR_MANCOM) {
      throw new BusinessRuleException(
          "PKG_MANCOM_STAGE", "ManCom approvers are selected once the request is for ManCom");
    }
    Set<String> chosen = new LinkedHashSet<>(approvers == null ? List.of() : approvers);
    if (chosen.isEmpty() || !members().containsAll(chosen)) {
      throw new BusinessRuleException(
          "PKG_MANCOM_APPROVERS", "Select one or more approvers of the ManCom approver list");
    }
    List<ManComApproval> previous = progress(id);
    previous.stream().filter(t -> !isDecided(t)).forEach(ManComApproval::close);
    int round = previous.isEmpty() ? 1 : previous.get(0).getRoundNo() + 1;
    String president = president();
    boolean others = chosen.stream().anyMatch(u -> !CurrentUser.sameUser(u, president));
    List<ManComApproval> created =
        chosen.stream()
            .map(
                u -> {
                  boolean isPresident = CurrentUser.sameUser(u, president);
                  return tasks.save(
                      new ManComApproval(id, round, u, isPresident, isPresident && others));
                })
            .toList();
    created.stream()
        .filter(t -> ManComApproval.PENDING.equals(t.getStatus()))
        .forEach(t -> tell(p, t.getApprover()));
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.SUBMIT,
        "ManCom approvers selected: "
            + String.join(", ", chosen.stream().map(directory::displayName).toList()));
    return created;
  }

  /**
   * The decision of the current user on a request.
   *
   * @param id request
   * @param decision APPROVE, RETURN or REJECT
   * @param remarks remarks (mandatory for RETURN and REJECT)
   * @return the task
   */
  public ManComApproval decide(Long id, String decision, String remarks) {
    PackageRequest p = requests.get(id);
    List<ManComApproval> round = currentRound(id);
    ManComApproval mine =
        round.stream()
            .filter(t -> ManComApproval.PENDING.equals(t.getStatus()))
            .filter(t -> CurrentUser.sameUser(t.getApprover(), currentUser.username()))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PKG_MANCOM_NOT_YOURS",
                        "No ManCom approval of this request waits for you"));
    if (APPROVE.equals(decision)) {
      mine.decide(ManComApproval.APPROVED, remarks, clock.instant());
      afterApproval(p, round, remarks);
      return mine;
    }
    if (remarks == null || remarks.isBlank()) {
      throw new BusinessRuleException(
          "PKG_MANCOM_REMARKS", "Enter the remarks of the rejection or the return");
    }
    boolean reject = "REJECT".equals(decision);
    mine.decide(reject ? "REJECTED" : "RETURNED", remarks, clock.instant());
    round.stream().filter(t -> !isDecided(t)).forEach(ManComApproval::close);
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "return",
        new TransitionNote(OTHERS, (reject ? "Rejected by ManCom: " : "") + remarks.strip()));
    return mine;
  }

  private void afterApproval(PackageRequest p, List<ManComApproval> round, String remarks) {
    boolean othersDone =
        round.stream()
            .filter(t -> !t.isPresident())
            .allMatch(t -> ManComApproval.APPROVED.equals(t.getStatus()));
    if (!othersDone) {
      return;
    }
    round.stream()
        .filter(t -> t.isPresident() && ManComApproval.WAITING.equals(t.getStatus()))
        .forEach(
            t -> {
              t.open();
              tell(p, t.getApprover());
            });
    if (round.stream().allMatch(t -> ManComApproval.APPROVED.equals(t.getStatus()))) {
      requirements.signoff(p.getId(), remarks);
    }
  }

  private List<ManComApproval> currentRound(Long id) {
    List<ManComApproval> all = progress(id);
    if (all.isEmpty()) {
      return all;
    }
    int round = all.get(0).getRoundNo();
    return all.stream().filter(t -> t.getRoundNo() == round).toList();
  }

  private static boolean isDecided(ManComApproval t) {
    return !ManComApproval.PENDING.equals(t.getStatus())
        && !ManComApproval.WAITING.equals(t.getStatus());
  }

  private void tell(PackageRequest p, String approver) {
    delivery.toUser(
        approver,
        new Notice(
            p.getRequestNo() + " for your ManCom approval",
            p.getTitle(),
            PackageRequests.link(p),
            PackageRequests.ENTITY,
            String.valueOf(p.getId())),
        EVENT,
        true);
  }
}

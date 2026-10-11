package com.iortatechnxt.brokerverse.eb.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountClassification;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the accounts of a confirmed cycle (BRID-017, 019, 022.01; FR-EB-046; design 1 and 11):
 * one draft account per benefit line through {@code AccountService.createDraft}, with the cycle's
 * business type (shared work item BT0): a RENEWAL cycle creates RENEWAL accounts that refer to the
 * line's expiring ARN (or current policy), a NEW_BUSINESS cycle creates new-business accounts, both
 * of origin EMPLOYEE_BENEFITS. The ARNs are kept on the cycle, the contract of each account becomes
 * a tracked item, the cycle moves to IN_PLACEMENT and Processing is notified. From there the BRD-1
 * placement, issuance and booking run unchanged; {@link PlacementProgressListener} closes the cycle
 * as PLACED once every account is booked.
 *
 * <p>Contract for the client confirmation step, which builds the {@link LinePlacement}s from the
 * chosen proposals.
 */
@Service
@Transactional
public class EbPlacementService {

  private static final String PROCESS_PERMISSION = "EB_PROCESS";

  private final EbRecords records;
  private final EbCycleRepository cycles;
  private final AccountService accounts;
  private final AccountRepository accountRepository;
  private final TrackedItemService trackedItems;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final EbActivityLog activity;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records cycle look-up
   * @param cycles cycles of a programme
   * @param accounts account creation
   * @param accountRepository accounts of a cycle
   * @param trackedItems contract items
   * @param workflow workflow engine
   * @param notifications in-app notices
   * @param activity TAT stamps
   * @param audit audit trail
   */
  @SuppressWarnings("java:S107") // constructor injection
  public EbPlacementService(
      EbRecords records,
      EbCycleRepository cycles,
      AccountService accounts,
      AccountRepository accountRepository,
      TrackedItemService trackedItems,
      WorkflowService workflow,
      NotificationService notifications,
      EbActivityLog activity,
      AuditTrailService audit) {
    this.records = records;
    this.cycles = cycles;
    this.accounts = accounts;
    this.accountRepository = accountRepository;
    this.trackedItems = trackedItems;
    this.workflow = workflow;
    this.notifications = notifications;
    this.activity = activity;
    this.audit = audit;
  }

  /**
   * Triggers the placement of a confirmed cycle: one account per line.
   *
   * @param companyId company
   * @param cycleId cycle in stage CONFIRMED
   * @param lines the account of each benefit line
   * @return the accounts created, in line order
   */
  public List<Account> trigger(Long companyId, Long cycleId, List<LinePlacement> lines) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (cycle.getStage() != EbCycleStage.CONFIRMED) {
      throw new BusinessRuleException(
          "EB_CYCLE_NOT_CONFIRMED",
          "Cycle " + cycle.getCycleNo() + " is not confirmed by the client");
    }
    if (lines == null || lines.isEmpty()) {
      throw new BusinessRuleException("EB_LINE_REQUIRED", "Add at least one benefit line");
    }
    EbProgramme programme = records.programmeOf(cycle);
    Set<Integer> seen = new HashSet<>();
    List<Account> created = new ArrayList<>();
    for (LinePlacement placement : lines) {
      if (!seen.add(placement.lineNo())) {
        throw new BusinessRuleException(
            "EB_LINE_TWICE", "Line " + placement.lineNo() + " is placed twice");
      }
      EbProgrammeLine line = programme.line(placement.lineNo());
      Account account = accounts.createDraft(request(programme, cycle, line, placement));
      cycle.recordAccount(account.getArn());
      keepProduct(line, account.getProductCode());
      trackedItems.openContract(
          cycle, account.getArn(), account.getInsurerCode(), line.getBenefitLine());
      created.add(account);
    }
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE,
        cycle.getId().toString(),
        "trigger_placement",
        TransitionNote.comment(created.size() + " account(s) created"));
    List<String> arns = created.stream().map(Account::getArn).toList();
    activity.done(
        cycle,
        TatActivity.PLACEMENT_REQUEST,
        cycle.getCycleNo(),
        programme.getAccountOfficer(),
        String.join(", ", arns));
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.SUBMIT,
        "Placement triggered: " + cycle.getBusinessType() + " accounts " + String.join(", ", arns));
    notifications.notifyPermission(
        PROCESS_PERMISSION,
        new Notice(
            cycle.getCycleNo() + ": accounts for placement",
            programme.getClientName() + " - " + String.join(", ", arns),
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=accounts",
            EbCodes.ENTITY_CYCLE,
            cycle.getId().toString()),
        EbCodes.EVENT_PLACEMENT_TRIGGERED);
    return created;
  }

  /** The line keeps the product of its account, so the booked account is matched to it. */
  private static void keepProduct(EbProgrammeLine line, String productCode) {
    if (line.getProductCode() != null) {
      return;
    }
    line.update(
        new EbProgrammeLine.Data(
            line.getBenefitLine(),
            productCode,
            line.getIncumbentInsurer(),
            line.getCurrentPolicyNo(),
            line.getCurrentArn(),
            line.getPeriodFrom(),
            line.getPeriodTo(),
            line.getHeadcount()));
  }

  private static NewAccount request(
      EbProgramme programme, EbCycle cycle, EbProgrammeLine line, LinePlacement placement) {
    AccountDraft given = placement.draft();
    AccountDraft draft =
        new AccountDraft(
            programme.getClientId(),
            given.productCode() == null ? line.getProductCode() : given.productCode(),
            given.marketSegment(),
            given.sourceChannel(),
            given.insurerCode() == null ? line.getIncumbentInsurer() : given.insurerCode(),
            given.insurerBranch(),
            given.periodFrom(),
            given.periodTo(),
            given.multiYear(),
            given.termYears(),
            given.currency(),
            given.paymentArrangement(),
            given.mortgage(),
            given.contact(),
            given.items(),
            given.ratingBasis(),
            given.commissionRate(),
            given.ffyStart());
    AccountClassification classification =
        cycle.getBusinessType() == BusinessType.RENEWAL
            ? new AccountClassification(
                BusinessType.RENEWAL, renewalOf(programme, line), AccountOrigin.EMPLOYEE_BENEFITS)
            : AccountClassification.newBusiness(AccountOrigin.EMPLOYEE_BENEFITS);
    return new NewAccount(
        programme.getCompanyId(),
        null,
        null,
        draft,
        placement.premium(),
        programme.getAccountOfficer(),
        null,
        null,
        classification);
  }

  private static String renewalOf(EbProgramme programme, EbProgrammeLine line) {
    if (line.getCurrentArn() != null) {
      return line.getCurrentArn();
    }
    return line.getCurrentPolicyNo() != null
        ? line.getCurrentPolicyNo()
        : programme.getProgrammeNo() + "/" + line.getLineNo();
  }

  /**
   * The accounts created for a cycle.
   *
   * @param cycle cycle
   * @return accounts, in creation order
   */
  @Transactional(readOnly = true)
  public List<Account> accountsOf(EbCycle cycle) {
    return cycle.getAccountArns().stream()
        .map(accountRepository::findByArn)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * The accounts created for the cycles of a programme (Accounts tab), latest cycle first.
   *
   * @param companyId company
   * @param programmeId programme
   * @return accounts with their cycle
   */
  @Transactional(readOnly = true)
  public List<CycleAccount> accountsOfProgramme(Long companyId, Long programmeId) {
    EbProgramme programme = records.programme(companyId, programmeId);
    List<CycleAccount> result = new ArrayList<>();
    for (EbCycle cycle : cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId())) {
      accountsOf(cycle)
          .forEach(a -> result.add(new CycleAccount(a, cycle.getId(), cycle.getCycleNo())));
    }
    return result;
  }

  /**
   * An account of a cycle.
   *
   * @param account account
   * @param cycleId cycle
   * @param cycleNo cycle number
   */
  public record CycleAccount(Account account, Long cycleId, String cycleNo) {}
}

package com.iortatechnxt.brokerverse.eb.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleOutcome;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Follows the accounts of a cycle in placement (FR-EB-046: "the cycle becomes PLACED when all its
 * accounts are booked"): when the last account of an IN_PLACEMENT cycle is booked, the cycle moves
 * to PLACED with its outcome (NEW_PLACED for a new business; RENEWED_INCUMBENT or MOVED for a
 * renewal, by the insurers of the accounts), the programme becomes ACTIVE and each line takes the
 * new account as its current policy (ARN, insurer, period) for the next renewal.
 */
@Component
public class PlacementProgressListener {

  private final EbCycleRepository cycles;
  private final EbRecords records;
  private final EbPlacementService placement;
  private final WorkflowService workflow;
  private final AuditTrailService audit;

  /**
   * Creates the listener.
   *
   * @param cycles cycles
   * @param records programme look-up
   * @param placement accounts of a cycle
   * @param workflow workflow engine
   * @param audit audit trail
   */
  public PlacementProgressListener(
      EbCycleRepository cycles,
      EbRecords records,
      EbPlacementService placement,
      WorkflowService workflow,
      AuditTrailService audit) {
    this.cycles = cycles;
    this.records = records;
    this.placement = placement;
    this.workflow = workflow;
    this.audit = audit;
  }

  /**
   * Closes the cycle when the booking of its last account is recorded.
   *
   * @param event account status change
   */
  @EventListener
  public void on(AccountStatusChanged event) {
    Optional<EbCycle> found =
        event.to() == AccountStatus.BOOKED
            ? cycles
                .findByAccountArn(event.arn())
                .filter(c -> c.getStage() == EbCycleStage.IN_PLACEMENT)
            : Optional.empty();
    found.ifPresent(cycle -> closeWhenAllBooked(cycle, event.arn()));
  }

  private void closeWhenAllBooked(EbCycle cycle, String bookedArn) {
    List<Account> accounts = placement.accountsOf(cycle);
    boolean allBooked =
        accounts.size() == cycle.getAccountArns().size()
            && accounts.stream()
                .allMatch(
                    a -> a.getArn().equals(bookedArn) || a.getStatus() == AccountStatus.BOOKED);
    if (allBooked) {
      close(cycle, accounts);
    }
  }

  private void close(EbCycle cycle, List<Account> accounts) {
    EbProgramme programme = records.programmeOf(cycle);
    cycle.recordOutcome(outcome(cycle, programme, accounts), null, null);
    accounts.forEach(a -> takeOver(programme, a));
    programme.markStatus(EbProgrammeStatus.ACTIVE);
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE,
        cycle.getId().toString(),
        "placed",
        TransitionNote.comment("Every account booked"));
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.CLOSE,
        "Placed: " + String.join(", ", cycle.getAccountArns()) + " booked");
  }

  private static EbCycleOutcome outcome(
      EbCycle cycle, EbProgramme programme, List<Account> accounts) {
    if (cycle.getBusinessType() != BusinessType.RENEWAL) {
      return EbCycleOutcome.NEW_PLACED;
    }
    boolean incumbent =
        accounts.stream()
            .allMatch(
                a ->
                    programme.getLines().stream()
                        .anyMatch(
                            l ->
                                Objects.equals(l.getProductCode(), a.getProductCode())
                                    && Objects.equals(
                                        l.getIncumbentInsurer(), a.getInsurerCode())));
    return incumbent ? EbCycleOutcome.RENEWED_INCUMBENT : EbCycleOutcome.MOVED;
  }

  /** The line of the account's product takes the account as its current policy. */
  private static void takeOver(EbProgramme programme, Account account) {
    programme.getLines().stream()
        .filter(EbProgrammeLine::isActive)
        .filter(l -> Objects.equals(l.getProductCode(), account.getProductCode()))
        .findFirst()
        .ifPresent(
            l ->
                l.update(
                    new EbProgrammeLine.Data(
                        l.getBenefitLine(),
                        l.getProductCode(),
                        account.getInsurerCode(),
                        account.getPolicyNumbers().isEmpty()
                            ? l.getCurrentPolicyNo()
                            : account.getPolicyNumbers().get(0),
                        account.getArn(),
                        account.getPeriodFrom(),
                        account.getPeriodTo(),
                        l.getHeadcount())));
  }
}

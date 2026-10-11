package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.domain.SlipStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Actions on several accounts at once from the Placement Workbench: cancel placement (BRNB.062),
 * reactivate (BRD 2.1.16) and send the generated slips (BRNB.071). Each account (or slip) runs in
 * its own transaction and reports its outcome, so one refusal does not undo the others.
 */
@Service
public class PlacementBatchService {

  private final AccountLifecycleService lifecycle;
  private final PlacementSlipService slips;
  private final PlacementQueryService queries;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the service.
   *
   * @param lifecycle account lifecycle
   * @param slips placement slips
   * @param queries placement reads
   * @param transactionManager transaction manager
   */
  public PlacementBatchService(
      AccountLifecycleService lifecycle,
      PlacementSlipService slips,
      PlacementQueryService queries,
      PlatformTransactionManager transactionManager) {
    this.lifecycle = lifecycle;
    this.slips = slips;
    this.queries = queries;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Cancels the placement of several accounts (BRNB.062), each moving to Placement cancelled.
   *
   * @param arns accounts
   * @param reasonCode reason (list CANCELLATION_REASON)
   * @param comment comment
   * @return outcome per account
   */
  public List<ItemResult> cancel(List<String> arns, String reasonCode, String comment) {
    return each(
        arns, arn -> lifecycle.cancelPlacement(arn, reasonCode, comment), "Placement cancelled");
  }

  /**
   * Reactivates cancelled placements (BRD 2.1.16), back to Ready for placement.
   *
   * @param arns accounts
   * @param comment comment
   * @return outcome per account
   */
  public List<ItemResult> reactivate(List<String> arns, String comment) {
    return each(arns, arn -> lifecycle.reactivate(arn, comment), "Reactivated");
  }

  /**
   * Sends the generated (not yet sent) slips of the accounts with the proposed e-mail, password
   * protected.
   *
   * @param arns accounts
   * @return outcome per account
   */
  public List<ItemResult> sendSlips(List<String> arns) {
    Set<Long> slipIds = new LinkedHashSet<>();
    List<ItemResult> results = new ArrayList<>();
    for (String arn : distinct(arns)) {
      queries.slipsFor(arn).stream()
          .filter(s -> s.getStatus() == SlipStatus.GENERATED)
          .findFirst()
          .map(PlacementSlip::getId)
          .ifPresentOrElse(
              slipIds::add,
              () ->
                  results.add(new ItemResult(arn, false, "No generated slip waiting to be sent")));
    }
    for (Long slipId : slipIds) {
      PlacementSlip slip = slips.get(slipId);
      ItemResult sent =
          run(slip.displayNo(), () -> slips.send(slipId, slips.draft(slipId)), "Sent");
      slip.getAccounts()
          .forEach(a -> results.add(new ItemResult(a.arn(), sent.ok(), sent.message())));
    }
    return results;
  }

  private List<ItemResult> each(List<String> arns, Consumer<String> action, String done) {
    return distinct(arns).stream().map(arn -> run(arn, () -> action.accept(arn), done)).toList();
  }

  private static List<String> distinct(List<String> arns) {
    if (arns == null || arns.isEmpty()) {
      throw new BusinessRuleException("PLACEMENT_NO_ACCOUNTS", "Select at least one account");
    }
    return arns.stream().map(String::strip).distinct().toList();
  }

  private ItemResult run(String reference, Runnable action, String done) {
    try {
      newTransaction.executeWithoutResult(status -> action.run());
      return new ItemResult(reference, true, done);
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return new ItemResult(reference, false, e.getMessage());
    }
  }

  /**
   * Outcome of an action on one account.
   *
   * @param arn account (or slip) reference
   * @param ok done
   * @param message outcome or reason of the refusal
   */
  public record ItemResult(String arn, boolean ok, String message) {}
}

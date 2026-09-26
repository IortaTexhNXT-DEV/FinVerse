package com.iortatechnxt.brokerverse.approval.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Approves several inbox items at once (FRBS 2.5.6, BASAU 2.5.3): each item goes to the {@link
 * BulkApprovalAction} of its module and is approved on its own, so one refusal never blocks the
 * others; the outcome of every item is reported (partial success).
 */
@Service
public class BulkApprovalService {

  private final List<BulkApprovalAction> actions;

  /**
   * Creates the service.
   *
   * @param actions bulk approval actions of the modules
   */
  public BulkApprovalService(List<BulkApprovalAction> actions) {
    this.actions = List.copyOf(actions);
  }

  /**
   * Approves items.
   *
   * @param items items as listed in the inbox
   * @return one outcome per item, in order
   */
  public List<Outcome> approve(List<Item> items) {
    return items.stream().map(this::approveOne).toList();
  }

  /**
   * Whether items of a module and type can be approved in bulk.
   *
   * @param module module
   * @param type type
   * @return true when a module action exists
   */
  public boolean supports(String module, String type) {
    return actions.stream().anyMatch(a -> a.supports(module, type));
  }

  private Outcome approveOne(Item item) {
    BulkApprovalAction action =
        actions.stream()
            .filter(a -> a.supports(item.module(), item.type()))
            .findFirst()
            .orElse(null);
    if (action == null) {
      return new Outcome(item, false, "Bulk approval is not available for " + item.type());
    }
    try {
      return new Outcome(item, true, action.approve(item.companyId(), item.reference()));
    } catch (BusinessRuleException | ResourceNotFoundException | AccessDeniedException ex) {
      return new Outcome(item, false, ex.getMessage());
    }
  }

  /**
   * An inbox item to approve.
   *
   * @param module module
   * @param type type
   * @param reference reference
   * @param companyId company, may be null
   */
  public record Item(String module, String type, String reference, Long companyId) {}

  /**
   * Outcome of one item.
   *
   * @param item item
   * @param approved whether it was approved
   * @param message outcome or refusal reason
   */
  public record Outcome(Item item, boolean approved, String message) {}
}

package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.receivables.api.dto.AllocationRequest;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Plans how money is applied to a payer's open debit items: validates manual allocations and
 * computes first-in-first-out allocations (oldest due date first).
 */
@Component
public class AllocationPlanner {

  private static final Set<OpenItemStatus> UNSETTLED =
      EnumSet.of(OpenItemStatus.OPEN, OpenItemStatus.PARTIALLY_SETTLED);

  private final OpenItemService openItems;

  /**
   * Creates the planner.
   *
   * @param openItems open item service
   */
  public AllocationPlanner(OpenItemService openItems) {
    this.openItems = openItems;
  }

  /**
   * Lists the payer's unsettled debit items in a currency, oldest due first.
   *
   * @param companyId company
   * @param partyId party
   * @param currency currency (null = all)
   * @return open debit items
   */
  public List<OpenItem> openDebits(Long companyId, Long partyId, String currency) {
    return openItems.partyItems(companyId, partyId).stream()
        .filter(i -> i.getDirection() == ItemDirection.DEBIT)
        .filter(i -> UNSETTLED.contains(i.getStatus()))
        .filter(i -> currency == null || i.getCurrency().equals(currency))
        .sorted(
            Comparator.comparing(OpenItem::getDueDate)
                .thenComparing(OpenItem::getDocumentDate)
                .thenComparing(OpenItem::getId))
        .toList();
  }

  /**
   * Validates manual allocations.
   *
   * @param partyId payer party
   * @param currency receipt currency
   * @param available money available for allocation
   * @param requests allocations
   * @return planned allocations
   */
  public List<Planned> manual(
      Long partyId, String currency, BigDecimal available, List<AllocationRequest> requests) {
    if (requests.isEmpty()) {
      throw new BusinessRuleException(
          "NO_ALLOCATIONS", "Select at least one debit note for a manual allocation");
    }
    List<Planned> planned = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    for (AllocationRequest a : requests) {
      if (!seen.add(a.debitItemId())) {
        throw new BusinessRuleException(
            "DUPLICATE_ALLOCATION", "A debit note may be allocated only once per receipt");
      }
      OpenItem item = openItems.get(a.debitItemId());
      requireAllocatable(item, partyId, currency, a.amount());
      planned.add(new Planned(item, a.amount()));
      total = total.add(a.amount());
    }
    if (total.compareTo(available) > 0) {
      throw new BusinessRuleException(
          "OVER_ALLOCATION", "Allocations " + total + " exceed the available amount " + available);
    }
    return planned;
  }

  /**
   * Allocates an amount to the oldest open debit items.
   *
   * @param companyId company
   * @param partyId party
   * @param currency currency
   * @param available amount to allocate
   * @return planned allocations (may allocate less when not enough is outstanding)
   */
  public List<Planned> fifo(Long companyId, Long partyId, String currency, BigDecimal available) {
    List<Planned> planned = new ArrayList<>();
    BigDecimal remaining = available;
    for (OpenItem item : openDebits(companyId, partyId, currency)) {
      if (remaining.signum() == 0) {
        break;
      }
      BigDecimal value = remaining.min(item.outstanding());
      planned.add(new Planned(item, value));
      remaining = remaining.subtract(value);
    }
    return planned;
  }

  /**
   * Total of planned allocations.
   *
   * @param planned allocations
   * @return total
   */
  public static BigDecimal total(List<Planned> planned) {
    return planned.stream().map(Planned::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static void requireAllocatable(
      OpenItem item, Long partyId, String currency, BigDecimal value) {
    if (item.getDirection() != ItemDirection.DEBIT
        || !Objects.equals(item.getPartyId(), partyId)
        || !item.getCurrency().equals(currency)) {
      throw new BusinessRuleException(
          "INVALID_ALLOCATION",
          "Item " + item.getDocumentNo() + " is not a " + currency + " debit item of the payer");
    }
    if (!UNSETTLED.contains(item.getStatus()) || value.compareTo(item.outstanding()) > 0) {
      throw new BusinessRuleException(
          "INVALID_ALLOCATION",
          "Allocation "
              + value
              + " exceeds the outstanding "
              + item.outstanding()
              + " of "
              + item.getDocumentNo());
    }
  }

  /**
   * Planned allocation.
   *
   * @param item debit item
   * @param amount amount to apply
   */
  public record Planned(OpenItem item, BigDecimal amount) {}
}

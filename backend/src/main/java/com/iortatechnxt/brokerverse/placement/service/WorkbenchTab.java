package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import java.util.List;

/** Tabs of the Placement Workbench (BDOI Placement & Booking design) and the statuses they list. */
public enum WorkbenchTab {
  /** Accounts to place, placed and awaiting the policy, or issued and waiting for booking. */
  FOR_PLACEMENT(
      List.of(
          AccountStatus.READY_FOR_PLACEMENT, AccountStatus.PLACED, AccountStatus.POLICY_ISSUED)),
  /** Accounts waiting for payment or client confirmation. */
  AWAITING_PAYMENT(List.of(AccountStatus.AWAITING_PAYMENT)),
  /** Placements returned by the insurer (return queue, BRNB.034). */
  RETURNED(List.of(AccountStatus.RETURNED_BY_INSURER)),
  /** Cancelled placements that may be reactivated (BRD 2.1.16). */
  CANCELLED(List.of(AccountStatus.PLACEMENT_CANCELLED)),
  /** Open hold covers expiring within the alert lead time (BRNB.103). */
  HOLD_COVER_EXPIRING(List.of()),
  /** Booked accounts. */
  BOOKED(List.of(AccountStatus.BOOKED));

  private final List<AccountStatus> statuses;

  WorkbenchTab(List<AccountStatus> statuses) {
    this.statuses = statuses;
  }

  /**
   * Account statuses listed on the tab (empty for the hold cover tab).
   *
   * @return statuses
   */
  public List<AccountStatus> statuses() {
    return statuses;
  }
}

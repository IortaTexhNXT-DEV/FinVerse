package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Sales unit and cost center of an account (production statistics BRNB.075, cost center BRNB.108),
 * defaulted from the account officer's team in the sales organisation.
 *
 * @param region region code
 * @param department department code
 * @param team team code
 * @param accountOfficer account officer (user name)
 * @param costCenter cost center (dimension COST_CENTER)
 */
@Embeddable
public record SalesStamp(
    @Column(name = "sales_region", length = 20) String region,
    @Column(name = "sales_department", length = 20) String department,
    @Column(name = "sales_team", length = 20) String team,
    @Column(name = "account_officer", length = 50) String accountOfficer,
    @Column(name = "cost_center", length = 20) String costCenter) {

  /**
   * The same stamp with another cost center.
   *
   * @param newCostCenter cost center
   * @return stamp
   */
  public SalesStamp withCostCenter(String newCostCenter) {
    return new SalesStamp(region, department, team, accountOfficer, newCostCenter);
  }
}

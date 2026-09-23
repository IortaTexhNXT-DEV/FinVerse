package com.iortatechnxt.finverse.dashboard.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps dashboard KPIs to the chart of accounts, so KPI definitions follow the client's chart
 * without code changes. Statement lines are {@code report_group} values; account prefixes select an
 * account and every account whose code starts with it.
 *
 * @param cashGroups statement lines counted as cash position
 * @param receivableGroups statement lines counted as insurance receivables
 * @param reserveGroups statement lines counted as technical reserves
 * @param premiumGroups statement lines of gross written premium (income, credit natural)
 * @param claimsPaidAccounts account prefixes of gross claims paid (expense, debit natural)
 * @param outstandingClaimsAccounts account prefixes of the outstanding claims reserve (liability)
 */
@ConfigurationProperties(prefix = "finverse.dashboard")
public record DashboardProperties(
    List<String> cashGroups,
    List<String> receivableGroups,
    List<String> reserveGroups,
    List<String> premiumGroups,
    List<String> claimsPaidAccounts,
    List<String> outstandingClaimsAccounts) {

  /** Canonical constructor applying defaults matching the standard insurance COA. */
  public DashboardProperties {
    cashGroups = orDefault(cashGroups, "Cash and Cash Equivalents");
    receivableGroups = orDefault(receivableGroups, "Insurance Receivables");
    reserveGroups = orDefault(reserveGroups, "Insurance Contract Liabilities");
    premiumGroups = orDefault(premiumGroups, "Gross Premiums Written");
    claimsPaidAccounts = orDefault(claimsPaidAccounts, "5100");
    outstandingClaimsAccounts = orDefault(outstandingClaimsAccounts, "2102");
  }

  private static List<String> orDefault(List<String> configured, String fallback) {
    return configured == null ? List.of(fallback) : List.copyOf(configured);
  }
}

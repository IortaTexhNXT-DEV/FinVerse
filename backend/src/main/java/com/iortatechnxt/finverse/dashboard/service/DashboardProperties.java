package com.iortatechnxt.finverse.dashboard.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps dashboard KPIs to chart-of-accounts statement lines ({@code report_group}), so KPI
 * definitions follow the client's chart of accounts without code changes.
 *
 * @param cashGroups statement lines counted as cash position
 * @param receivableGroups statement lines counted as insurance receivables
 * @param reserveGroups statement lines counted as technical reserves
 */
@ConfigurationProperties(prefix = "finverse.dashboard")
public record DashboardProperties(
    List<String> cashGroups, List<String> receivableGroups, List<String> reserveGroups) {

  /** Canonical constructor applying defaults matching the standard insurance COA. */
  public DashboardProperties {
    cashGroups =
        cashGroups == null ? List.of("Cash and Cash Equivalents") : List.copyOf(cashGroups);
    receivableGroups =
        receivableGroups == null ? List.of("Insurance Receivables") : List.copyOf(receivableGroups);
    reserveGroups =
        reserveGroups == null
            ? List.of("Insurance Contract Liabilities")
            : List.copyOf(reserveGroups);
  }
}

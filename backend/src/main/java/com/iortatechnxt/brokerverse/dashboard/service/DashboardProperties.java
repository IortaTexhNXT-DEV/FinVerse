package com.iortatechnxt.brokerverse.dashboard.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps dashboard KPIs to the chart of accounts, so KPI definitions follow the client's chart
 * without code changes. Statement lines are {@code report_group} values.
 *
 * @param cashGroups statement lines counted as cash position
 * @param receivableGroups statement lines counted as insurance receivables
 */
@ConfigurationProperties(prefix = "brokerverse.dashboard")
public record DashboardProperties(List<String> cashGroups, List<String> receivableGroups) {

  /** Canonical constructor applying defaults matching the standard insurance COA. */
  public DashboardProperties {
    cashGroups = orDefault(cashGroups, "Cash and Cash Equivalents");
    receivableGroups = orDefault(receivableGroups, "Insurance Receivables");
  }

  private static List<String> orDefault(List<String> configured, String fallback) {
    return configured == null ? List.of(fallback) : List.copyOf(configured);
  }
}

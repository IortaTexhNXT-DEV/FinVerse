package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The presentation of the claims reports of the FRS BRD-07 Claims v1.0 (FRCLM.011 to 017, delivery
 * work DW-CLM-11): the report heading of the Claims unit (company and unit above the title and
 * filters), and the detailed or summary presentation grouped by status, handler, insurer, claim
 * type, Marketing team or Account Officer. The summary keeps one row per group with the number of
 * claims and the totals of the amount columns.
 */
@Component
public class ClaimReportPresentation {

  /** Parameter of the presentation. */
  public static final String PRESENTATION = "presentation";

  /** Parameter of the grouping. */
  public static final String GROUP_BY = "groupBy";

  /** Parameter of a title entered by the user (FRCLM.015.01). */
  public static final String TITLE = "reportTitle";

  /** One row per claim. */
  public static final String DETAILED = "DETAILED";

  /** One row per group. */
  public static final String SUMMARY = "SUMMARY";

  /** No grouping. */
  public static final String NONE = "NONE";

  /** Groupings offered. */
  public static final List<String> GROUPS =
      List.of(
          NONE, "STATUS", "HANDLER", "INSURER", "CLAIM_TYPE", "MARKETING_TEAM", "ACCOUNT_OFFICER");

  private static final Map<String, String[]> KEYS =
      Map.of(
          "STATUS", new String[] {"status", "Claim Status"},
          "HANDLER", new String[] {"handler", "Claim Handler"},
          "INSURER", new String[] {"insurers", "Insurance Company"},
          "CLAIM_TYPE", new String[] {"claim_type", "Claim Type"},
          "MARKETING_TEAM", new String[] {"sales_team", "Marketing Team"},
          "ACCOUNT_OFFICER", new String[] {"account_officer", "Account Officer"});

  private static final String CLAIMS = "claims";

  private final OrganizationService organization;

  /**
   * Creates the presentation.
   *
   * @param organization company name of the heading
   */
  public ClaimReportPresentation(OrganizationService organization) {
    this.organization = organization;
  }

  /**
   * Applies the heading and the chosen presentation to a claims report.
   *
   * @param p parameters of the run
   * @param result report as defined
   * @return report as presented
   */
  public ReportResult present(ReportParameters p, ReportResult result) {
    String group = p.optionalText(GROUP_BY).map(String::strip).orElse(NONE);
    boolean summary = p.optionalText(PRESENTATION).map(SUMMARY::equals).orElse(false);
    String[] key = KEYS.get(group);
    boolean groupable =
        key != null && result.columns().stream().anyMatch(c -> c.key().equals(key[0]))
            || key != null && hasCell(result, key[0]);
    ReportResult laid = result;
    if (groupable && summary) {
      laid = summary(result, key[0], key[1]);
    } else if (groupable) {
      laid = TabularReportBuilder.regroup(result).groupBy(key[0], key[1]).build();
    }
    String title =
        p.optionalText(TITLE).map(String::strip).filter(t -> !t.isEmpty()).orElse(laid.title());
    return new ReportResult(
        laid.code(), title, heading(p, laid), laid.columns(), laid.rows(), laid.notes());
  }

  private List<String> heading(ReportParameters p, ReportResult result) {
    List<String> lines = new ArrayList<>();
    p.optionalLong(BclReportSql.COMPANY)
        .ifPresent(id -> lines.add(organization.getCompany(id).getName() + " - Claims Unit"));
    lines.addAll(result.parameterEcho());
    return lines;
  }

  private static boolean hasCell(ReportResult result, String key) {
    return result.rows().stream()
        .anyMatch(r -> r.kind() == RowKind.DETAIL && r.cells().containsKey(key));
  }

  private static ReportResult summary(ReportResult result, String key, String label) {
    List<ReportColumn> amounts = result.columns().stream().filter(ReportColumn::summed).toList();
    Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
    for (ReportRow r : result.rows()) {
      if (r.kind() != RowKind.DETAIL) {
        continue;
      }
      Object value = r.cells().get(key);
      String name = value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value);
      Map<String, Object> g =
          groups.computeIfAbsent(
              name,
              n -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put(key, n);
                row.put(CLAIMS, 0L);
                amounts.forEach(c -> row.put(c.key(), BigDecimal.ZERO));
                return row;
              });
      g.put(CLAIMS, (Long) g.get(CLAIMS) + 1);
      for (ReportColumn c : amounts) {
        if (r.cells().get(c.key()) instanceof BigDecimal amount) {
          g.put(c.key(), ((BigDecimal) g.get(c.key())).add(amount));
        }
      }
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text(key, label));
    columns.add(ReportColumn.count(CLAIMS, "Number of Claims"));
    columns.addAll(amounts);
    List<ReportRow> rows = new ArrayList<>();
    Map<String, Object> total = new LinkedHashMap<>();
    total.put(CLAIMS, 0L);
    amounts.forEach(c -> total.put(c.key(), BigDecimal.ZERO));
    for (Map<String, Object> g : groups.values()) {
      rows.add(ReportRow.detail(g));
      total.put(CLAIMS, (Long) total.get(CLAIMS) + (Long) g.get(CLAIMS));
      amounts.forEach(
          c ->
              total.put(
                  c.key(), ((BigDecimal) total.get(c.key())).add((BigDecimal) g.get(c.key()))));
    }
    if (!groups.isEmpty()) {
      rows.add(new ReportRow(RowKind.TOTAL, 0, "Grand Total", total));
    }
    List<String> notes = new ArrayList<>(result.notes());
    notes.add("Summary: one row per " + label + ".");
    return new ReportResult(
        result.code(), result.title(), result.parameterEcho(), columns, rows, notes);
  }
}

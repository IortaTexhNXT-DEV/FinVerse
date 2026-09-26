package com.iortatechnxt.brokerverse.screening.cases.api;

import com.iortatechnxt.brokerverse.screening.report.ClientReports;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * High-risk Clients (FR-SS-045; capability 4 "view / extract the list of high-risk clients"): the
 * clients with a high rating, the PEP or the Watchlist Review tag, with their category, open case,
 * active policy and marketing unit. The export is the report {@code SCR-HIGH-RISK-CLIENTS}.
 */
@RestController
@RequestMapping("/api/v1/screening/high-risk-clients")
public class HighRiskController {

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final ClientReports.HighRisk report;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param report the high-risk report (its query)
   * @param clock clock
   */
  public HighRiskController(ClientReports.HighRisk report, Clock clock) {
    this.report = report;
    this.clock = clock;
  }

  /**
   * The high-risk clients of a company.
   *
   * @param companyId company
   * @param asOf as-of date, today by default
   * @param riskCategory risk category filter
   * @param marketingUnit marketing unit filter
   * @param clientType client type filter
   * @return rows, highest tier first
   */
  @GetMapping
  @PreAuthorize(CaseController.HAS_VIEW)
  public List<HighRiskRow> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(required = false) String riskCategory,
      @RequestParam(required = false) String marketingUnit,
      @RequestParam(required = false) String clientType) {
    return report
        .rows(
            new ClientReports.Filter(
                companyId,
                asOf == null ? LocalDate.now(clock.withZone(MANILA)) : asOf,
                blankToNull(riskCategory),
                blankToNull(marketingUnit),
                null,
                blankToNull(clientType)))
        .stream()
        .map(HighRiskRow::from)
        .toList();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A high-risk client.
   *
   * @param clientCode client code
   * @param clientName client name
   * @param clientType client type
   * @param riskCategory risk category of the last tagging
   * @param riskRating risk rating
   * @param tags PEP / WATCHLIST_REVIEW
   * @param taggedOn date of the last change
   * @param source RULE or MANUAL
   * @param openCase open case number and stage, or None
   * @param activePolicy Yes or No
   * @param unit marketing unit / unit head
   */
  public record HighRiskRow(
      String clientCode,
      String clientName,
      String clientType,
      String riskCategory,
      String riskRating,
      String tags,
      String taggedOn,
      String source,
      String openCase,
      String activePolicy,
      String unit) {

    static HighRiskRow from(Map<String, Object> row) {
      return new HighRiskRow(
          text(row, "client_code"),
          text(row, "display_name"),
          text(row, "client_type"),
          text(row, "risk_category"),
          text(row, "risk_rating"),
          text(row, "tags"),
          text(row, "tagged_on"),
          text(row, "source"),
          text(row, "open_case"),
          text(row, "active_policy"),
          text(row, "unit"));
    }

    private static String text(Map<String, Object> row, String key) {
      return Objects.toString(row.get(key), null);
    }
  }
}

package com.iortatechnxt.brokerverse.screening.cases.api;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.screening.report.ClientReports;
import java.time.Clock;
import java.time.LocalDate;
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
                asOf == null ? BusinessClock.today(clock) : asOf,
                blankToNull(riskCategory),
                blankToNull(marketingUnit),
                null,
                blankToNull(clientType)))
        .stream()
        .map(HighRiskRow::from)
        .toList();
  }

  /**
   * The values of the screen's filters: the risk categories and marketing units, by name.
   *
   * @param companyId company
   * @return filter values
   */
  @GetMapping("/filters")
  @PreAuthorize(CaseController.HAS_VIEW)
  public HighRiskFilters filters(@RequestParam Long companyId) {
    return new HighRiskFilters(
        report.riskCategories(companyId).stream().map(FilterOption::from).toList(),
        report.marketingUnits(companyId).stream().map(FilterOption::from).toList());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * The values of the High-risk Clients filters.
   *
   * @param riskCategories risk categories given to clients
   * @param marketingUnits marketing units of the screening cases
   */
  public record HighRiskFilters(
      List<FilterOption> riskCategories, List<FilterOption> marketingUnits) {}

  /**
   * One value of a filter.
   *
   * @param code stored code
   * @param label name shown
   */
  public record FilterOption(String code, String label) {

    static FilterOption from(Map<String, Object> row) {
      return new FilterOption(
          Objects.toString(row.get("code"), ""), Objects.toString(row.get("label"), ""));
    }
  }

  /**
   * A high-risk client.
   *
   * @param clientCode client code
   * @param clientName client name
   * @param clientType client type
   * @param riskCategory risk category of the last tagging
   * @param riskCategoryName name of the risk category
   * @param riskRating risk rating
   * @param tags PEP / WATCHLIST_REVIEW
   * @param taggedOn date of the last change
   * @param source RULE or MANUAL
   * @param openCase open case number and stage, or None
   * @param activePolicy Yes or No
   * @param unit marketing unit / unit head
   * @param openCaseNo number of the open case
   * @param openCaseStage stage of the open case
   * @param marketingUnit marketing unit code
   * @param marketingUnitName marketing unit name
   * @param unitHead unit head (user name)
   */
  public record HighRiskRow(
      String clientCode,
      String clientName,
      String clientType,
      String riskCategory,
      String riskCategoryName,
      String riskRating,
      String tags,
      String taggedOn,
      String source,
      String openCase,
      String activePolicy,
      String unit,
      String openCaseNo,
      String openCaseStage,
      String marketingUnit,
      String marketingUnitName,
      String unitHead) {

    static HighRiskRow from(Map<String, Object> row) {
      return new HighRiskRow(
          text(row, "client_code"),
          text(row, "display_name"),
          text(row, "client_type"),
          text(row, "risk_category"),
          text(row, "risk_category_name"),
          text(row, "risk_rating"),
          text(row, "tags"),
          text(row, "tagged_on"),
          text(row, "source"),
          text(row, "open_case"),
          text(row, "active_policy"),
          text(row, "unit"),
          text(row, "open_case_no"),
          text(row, "open_case_stage"),
          text(row, "marketing_unit"),
          text(row, "marketing_unit_name"),
          text(row, "unit_head"));
    }

    private static String text(Map<String, Object> row, String key) {
      return Objects.toString(row.get(key), null);
    }
  }
}

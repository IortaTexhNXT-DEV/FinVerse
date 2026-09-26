package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import com.iortatechnxt.brokerverse.nbreport.service.ProductionService;
import com.iortatechnxt.brokerverse.nbreport.service.UnitProduction;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Production Statistics (NB-PRODUCTION, BRNB.075): bookings, premium and commission per region,
 * department, team or account officer (the sales organisation stamped on the account) for a date
 * range, against the units' targets pro rata to the period. Targets are seed values until BDOI
 * gives the hierarchy and target values (Q41). Filter Business Type (BRID-022.01, shared work item
 * BT0): targets are shown unchanged whatever the filter.
 */
@Component
public class ProductionReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-PRODUCTION";

  private static final String LEVEL = "level";

  private final ProductionService production;

  /**
   * Creates the report.
   *
   * @param production production statistics
   */
  public ProductionReport(ProductionService production) {
    this.production = production;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Production Statistics",
        "Production per region, department, team or officer against target (BRNB.075)",
        Permission.WORK_ASSIGN,
        true,
        ParameterSpec.select(
            LEVEL,
            "Sales Unit Level",
            Arrays.stream(UnitLevel.values()).map(Enum::name).toList(),
            UnitLevel.TEAM.name()),
        NbReportSupport.businessTypeFilter());
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        production
            .production(
                p.longValue(NbReportSupport.COMPANY),
                UnitLevel.valueOf(p.text(LEVEL)),
                p.date(NbReportSupport.FROM),
                p.date(NbReportSupport.TO),
                NbReportSupport.selected(p, NbReportSupport.BUSINESS_TYPE))
            .stream()
            .map(ProductionReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Unit"),
            ReportColumn.text("name", "Name"),
            ReportColumn.count("bookings", "Bookings"),
            ReportColumn.count("targetCount", "Target Bookings"),
            ReportColumn.amount("premium", "Premium"),
            ReportColumn.amount("targetPremium", "Target Premium"),
            ReportColumn.percent("achievement", "Achievement"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.amount("targetCommission", "Target Commission"))
        .rows(rows)
        .presorted()
        .note(
            "Premium is the basic premium of the booked invoices, net of endorsements and"
                + " cancellations. Targets are pro rata to the days of the period (PHP).")
        .build();
  }

  private static Map<String, Object> row(UnitProduction u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("code", u.code());
    m.put("name", u.name());
    m.put("bookings", u.bookings());
    m.put("targetCount", u.targetCount());
    m.put("premium", u.premium());
    m.put("targetPremium", u.targetPremium());
    m.put("achievement", u.achievement());
    m.put("commission", u.commission());
    m.put("targetCommission", u.targetCommission());
    return m;
  }
}

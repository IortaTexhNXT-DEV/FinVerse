package com.iortatechnxt.brokerverse.underwriting.report;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.RiskExposure;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR085 Risk Accumulation Report: sums insured of the risks in force on a date, per accumulation
 * zone (Σ SI per zone), for catastrophe exposure control.
 */
@Component
public class RiskAccumulationReport implements ReportDefinition {

  private static final String ZONE = "zone";
  private static final String AS_OF = "asOfDate";
  private static final String UNASSIGNED = "(no zone)";
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final PolicyQueryService policies;

  /**
   * Creates the report.
   *
   * @param policies policy read service
   */
  public RiskAccumulationReport(PolicyQueryService policies) {
    this.policies = policies;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "PGIBR085",
        "Risk Accumulation Report",
        ReportCategory.UNDERWRITING,
        "Sums insured in force per accumulation zone",
        List.of(
            ParameterSpec.required(UwReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.optional(ZONE, "Accumulation Group (Zone)", ParameterType.TEXT),
            ParameterSpec.required(AS_OF, "As on Date", ParameterType.DATE).withDefault("TODAY")),
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate asOf = p.date(AS_OF);
    String zoneFilter = p.optionalText(ZONE).orElse(null);
    List<RiskExposure> exposures =
        policies.risksInForce(p.longValue(UwReportSupport.COMPANY), asOf).stream()
            .filter(e -> zoneFilter == null || zoneFilter.equals(e.risk().accumulationZone()))
            .sorted(Comparator.comparing(RiskAccumulationReport::zoneOf))
            .toList();
    List<Map<String, Object>> rows = new ArrayList<>();
    int serial = 1;
    for (RiskExposure e : exposures) {
      PolicySnapshot policy = e.policy();
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("serial", String.valueOf(serial++));
      m.put(ZONE, zoneOf(e));
      m.put("occupation", e.risk().occupation());
      m.put("policyNo", policy.policyNo());
      m.put("endt", e.endorsements());
      m.put(
          "days",
          String.valueOf(ChronoUnit.DAYS.between(policy.periodFrom(), policy.periodTo()) + 1));
      m.put("si", e.risk().sumInsured());
      m.put(
          "ourSi",
          Money.round(
              e.risk()
                  .sumInsured()
                  .multiply(policy.sharePct())
                  .divide(HUNDRED, Money.RATE_SCALE, RoundingMode.HALF_EVEN)));
      m.put("currency", policy.currency());
      m.put("product", policy.productCode());
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("serial", "S.No"),
            ReportColumn.text(ZONE, "Accumulation Zone"),
            ReportColumn.text("occupation", "Occupation"),
            ReportColumn.text("policyNo", "Policy"),
            ReportColumn.count("endt", "Endts"),
            ReportColumn.text("days", "Period (days)"),
            ReportColumn.amount("si", "SI (100%)"),
            ReportColumn.amount("ourSi", "Our SI"),
            ReportColumn.text("currency", "Ccy"),
            ReportColumn.text("product", "Product"))
        .groupBy(ZONE, "Accumulation Zone")
        .presorted()
        .rows(rows)
        .note("Risks of approved policies in force on the as-on date; SI in policy currency.")
        .build();
  }

  private static String zoneOf(RiskExposure e) {
    String zone = e.risk().accumulationZone();
    return zone == null || zone.isBlank() ? UNASSIGNED : zone;
  }
}

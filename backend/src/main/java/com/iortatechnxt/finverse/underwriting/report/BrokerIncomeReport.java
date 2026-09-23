package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_SHARE;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.underwriting.domain.DateBasis;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR025 Broker Income Statement: brokerage earned by each broker on approved business (source
 * type = broker). Branch &gt; Broker.
 */
@Component
public class BrokerIncomeReport implements ReportDefinition {

  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared report support
   */
  public BrokerIncomeReport(UwReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>(UwReportSupport.rangeParams());
    params.removeIf(s -> UwReportSupport.CLASS.equals(s.name()));
    params.addAll(UwReportSupport.dateRange("Approval Date From", "Approval Date To"));
    return new ReportMetadata(
        "PGIBR025",
        "Broker Income Statement",
        ReportCategory.UNDERWRITING,
        "Brokerage per broker on approved policies and endorsements",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<Long, String> branches = support.branchCodes(p.longValue(UwReportSupport.COMPANY));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PremiumTransaction t :
        support.transactions(p, DateBasis.APPROVAL, UwReportSupport.APPROVED)) {
      if (t.policy().sourceType() != SourceType.BROKER) {
        continue;
      }
      PremiumBreakdown b = t.premium();
      Map<String, Object> m = new LinkedHashMap<>();
      UwReportSupport.putGroups(m, t.policy(), branches);
      UwReportSupport.putDocument(m, t);
      m.put("broker", t.policy().intermediaryCode() + " " + t.policy().intermediaryName());
      m.put(K_SHARE, t.policy().sharePct());
      m.put("gross100", t.toBase(b.getGrossPremium()));
      m.put("ourGross", t.toBase(b.getOurGrossPremium()));
      m.put("brokerPct", b.getCommissionRate());
      m.put("brokerage", t.toBase(b.getCommission()));
      rows.add(m);
    }
    List<ReportColumn> columns = new ArrayList<>(UwReportSupport.documentColumns());
    columns.addAll(
        List.of(
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("gross100", "100% Gross"),
            ReportColumn.amount("ourGross", "Our Gross"),
            ReportColumn.percent("brokerPct", "Broker %"),
            ReportColumn.amount("brokerage", "Broker Commission")));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(K_BRANCH, "Branch")
        .groupBy("broker", "Broker")
        .rows(rows)
        .note("Amounts in base currency.")
        .build();
  }
}

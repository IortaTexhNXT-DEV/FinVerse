package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_PRODUCT;

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
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR003 Daily Production Report: number of policies and endorsements and net premium (100 % and
 * our share) per day, based on issue or approval date. Branch &gt; Class &gt; Product.
 */
@Component
public class DailyProductionReport implements ReportDefinition {

  private static final String DATE = "date";
  private static final String POLICIES = "policies";
  private static final String ENDORSEMENTS = "endorsements";
  private static final String NET_100 = "net100";
  private static final String OUR_NET = "ourNet";

  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared report support
   */
  public DailyProductionReport(UwReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(
        ParameterSpec.select(
            UwReportSupport.BASIS, "Based on", List.of("ISSUE", "APPROVAL"), "APPROVAL"));
    params.addAll(UwReportSupport.dateRange("Date From", "Date To"));
    return new ReportMetadata(
        "PGIBR003",
        "Daily Production Report",
        ReportCategory.UNDERWRITING,
        "Policies, endorsements and net premium written per day",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    DateBasis basis = UwReportSupport.basis(p);
    Map<Long, String> branches = support.branchCodes(p.longValue(UwReportSupport.COMPANY));
    Map<String, Map<String, Object>> days = new LinkedHashMap<>();
    for (PremiumTransaction t : support.transactions(p, basis, UwReportSupport.APPROVED)) {
      LocalDate date = basis == DateBasis.ISSUE ? t.issueDate() : t.approvalDate();
      Map<String, Object> line = new LinkedHashMap<>();
      UwReportSupport.putGroups(line, t, branches);
      String key = line.get(K_BRANCH) + "|" + line.get(K_PRODUCT) + "|" + date;
      Map<String, Object> day = days.computeIfAbsent(key, k -> line);
      day.put(DATE, date);
      boolean original = t.endorsementNo() == 0;
      day.merge(POLICIES, original ? 1 : 0, (a, b) -> (Integer) a + (Integer) b);
      day.merge(ENDORSEMENTS, original ? 0 : 1, (a, b) -> (Integer) a + (Integer) b);
      day.merge(NET_100, t.toBase(t.premium().getNetPremium()), UwReportSupport::add);
      day.merge(OUR_NET, t.toBase(t.premium().getOurNetPremium()), UwReportSupport::add);
    }
    List<Map<String, Object>> rows = new ArrayList<>(days.values());
    rows.sort((a, b) -> ((LocalDate) a.get(DATE)).compareTo((LocalDate) b.get(DATE)));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date(DATE, "Date"),
            ReportColumn.count(POLICIES, "No of Policies"),
            ReportColumn.count(ENDORSEMENTS, "No of Endorsements"),
            ReportColumn.amount(NET_100, "100% Net"),
            ReportColumn.amount(OUR_NET, "Our Net"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .rows(rows)
        .note("Amounts in base currency.")
        .build();
  }
}

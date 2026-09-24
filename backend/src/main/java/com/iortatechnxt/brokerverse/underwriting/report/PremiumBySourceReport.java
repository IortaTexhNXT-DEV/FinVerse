package com.iortatechnxt.brokerverse.underwriting.report;

import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_PRODUCT;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_SHARE;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.domain.DateBasis;
import com.iortatechnxt.brokerverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR043 Premium Income by Direct / Agent / Broker: approved premium by distribution channel with
 * commission in foreign and local currency. Source type &gt; Branch &gt; Class &gt; Product.
 */
@Component
public class PremiumBySourceReport implements ReportDefinition {

  private static final String SOURCE = "sourceType";
  private static final String ALL = "ALL";

  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared report support
   */
  public PremiumBySourceReport(UwReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(
        ParameterSpec.select(
            SOURCE, "Source Type", List.of(ALL, "DIRECT", "AGENT", "BROKER"), ALL));
    params.addAll(UwReportSupport.dateRange("Approval Date From", "Approval Date To"));
    return new ReportMetadata(
        "PGIBR043",
        "Premium Income by Direct/Agent/Broker",
        ReportCategory.UNDERWRITING,
        "Approved premium and commission by distribution channel",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String source = p.text(SOURCE);
    Map<Long, String> branches = support.branchCodes(p.longValue(UwReportSupport.COMPANY));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PremiumTransaction t :
        support.transactions(p, DateBasis.APPROVAL, UwReportSupport.APPROVED)) {
      if (!ALL.equals(source) && !source.equals(t.policy().sourceType().name())) {
        continue;
      }
      PremiumBreakdown b = t.premium();
      Map<String, Object> m = new LinkedHashMap<>();
      UwReportSupport.putGroups(m, t, branches);
      UwReportSupport.putDocument(m, t);
      m.put(SOURCE, t.policy().sourceType().name());
      m.put(K_SHARE, t.policy().sharePct());
      m.put("si", t.toBase(b.getOurSumInsured()));
      m.put("gross", t.toBase(b.getOurGrossPremium()));
      m.put("charges", t.toBase(b.taxesAndCharges()));
      m.put("commissionPct", b.getCommissionRate());
      m.put("currency", t.policy().currency());
      m.put("commissionFc", b.getCommission());
      m.put("commissionLc", t.toBase(b.getCommission()));
      rows.add(m);
    }
    List<ReportColumn> columns = new ArrayList<>(UwReportSupport.documentColumns());
    columns.addAll(
        List.of(
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("si", "SI"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("charges", "Charges"),
            ReportColumn.percent("commissionPct", "Commission %"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amountNoTotal("commissionFc", "Commission FC"),
            ReportColumn.amount("commissionLc", "Commission LC")));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(SOURCE, "Source Type")
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .rows(rows)
        .note("SI, gross and charges in base currency (LC); FC = policy currency.")
        .build();
  }
}

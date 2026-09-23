package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_PRODUCT;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_SHARE;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.underwriting.domain.DateBasis;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR040 Transactions Pending Approval: policies and endorsements in draft or awaiting approval,
 * in the policy currency (no rate is fixed before approval). Branch &gt; Class &gt; Product.
 */
@Component
public class PendingTransactionsReport implements ReportDefinition {

  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared report support
   */
  public PendingTransactionsReport(UwReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(ParameterSpec.optional(UwReportSupport.FROM, "Issue Date From", ParameterType.DATE));
    params.add(ParameterSpec.optional(UwReportSupport.TO, "Issue Date To", ParameterType.DATE));
    return new ReportMetadata(
        "PGIBR040",
        "Transactions Pending Approval",
        ReportCategory.UNDERWRITING,
        "Draft and pending policies and endorsements with their maker",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<Long, String> branches = support.branchCodes(p.longValue(UwReportSupport.COMPANY));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PremiumTransaction t :
        support.transactions(
            p, DateBasis.ISSUE, EnumSet.of(PolicyStatus.DRAFT, PolicyStatus.PENDING_APPROVAL))) {
      PremiumBreakdown b = t.premium();
      Map<String, Object> m = new LinkedHashMap<>();
      UwReportSupport.putGroups(m, t.policy(), branches);
      UwReportSupport.putDocument(m, t);
      m.put("status", t.status().name());
      m.put("createdBy", t.createdBy());
      m.put("currency", t.policy().currency());
      m.put("period", t.policy().periodFrom() + " - " + t.policy().periodTo());
      m.put(K_SHARE, t.policy().sharePct());
      m.put("si", b.getOurSumInsured());
      m.put("gross", b.getOurGrossPremium());
      m.put("discount", b.getOurDiscount());
      m.put("loading", b.getOurLoading());
      m.put("net", b.getOurNetPremium());
      m.put("charges", b.taxesAndCharges());
      m.put("commission", b.getCommission());
      rows.add(m);
    }
    List<ReportColumn> columns = new ArrayList<>(UwReportSupport.documentColumns());
    columns.addAll(
        List.of(
            ReportColumn.text("status", "Status"),
            ReportColumn.text("createdBy", "Created By"),
            ReportColumn.text("currency", "Ccy"),
            ReportColumn.text("period", "Period"),
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("si", "SI"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("discount", "Disc"),
            ReportColumn.amount("loading", "Loading"),
            ReportColumn.amount("net", "Net"),
            ReportColumn.amount("charges", "Charges"),
            ReportColumn.amount("commission", "Commission")));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .rows(rows)
        .note("Endt No blank = new policy awaiting approval; amounts in policy currency.")
        .build();
  }
}

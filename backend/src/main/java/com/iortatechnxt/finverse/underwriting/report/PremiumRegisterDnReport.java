package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_PRODUCT;
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
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR005 Premium Register with DN Number: approved policies and endorsements with the client
 * debit note and the broker credit note, in base currency. Branch &gt; Class &gt; Product.
 */
@Component
public class PremiumRegisterDnReport implements ReportDefinition {

  private static final String TXN = "transactions";
  private static final String POLICIES = "POLICIES";
  private static final String ENDORSEMENTS = "ENDORSEMENTS";

  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared report support
   */
  public PremiumRegisterDnReport(UwReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(UwReportSupport.basisParam());
    params.addAll(UwReportSupport.dateRange("Date From", "Date To"));
    params.add(
        ParameterSpec.select(TXN, "Account Option", List.of("ALL", POLICIES, ENDORSEMENTS), "ALL"));
    return new ReportMetadata(
        "PGIBR005",
        "Premium Register with DN Number",
        ReportCategory.UNDERWRITING,
        "Approved policies and endorsements with debit and credit note numbers",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String option = p.text(TXN);
    Map<Long, String> branches = support.branchCodes(p.longValue(UwReportSupport.COMPANY));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PremiumTransaction t :
        support.transactions(p, UwReportSupport.basis(p), UwReportSupport.APPROVED)) {
      boolean original = t.endorsementNo() == 0;
      if (POLICIES.equals(option) && !original || ENDORSEMENTS.equals(option) && original) {
        continue;
      }
      rows.add(row(t, branches));
    }
    List<ReportColumn> columns = new ArrayList<>(UwReportSupport.documentColumns());
    columns.addAll(
        List.of(
            ReportColumn.text("customer", "Customer"),
            ReportColumn.date("issueDate", "Issue Date"),
            ReportColumn.date("endtDate", "Endt Date"),
            ReportColumn.text("period", "Period"),
            ReportColumn.date("approvalDate", "Approval Date"),
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("si100", "100% SI"),
            ReportColumn.amount("ourSi", "Our SI"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("discount", "Discount"),
            ReportColumn.amount("loading", "Loading"),
            ReportColumn.amount("net", "Net"),
            ReportColumn.amount("policyFee", "Policy Fee"),
            ReportColumn.amount("otherCharges", "Other Charges"),
            ReportColumn.text("broker", "Broker"),
            ReportColumn.amount("commission", "Broker Commission"),
            ReportColumn.text("brokerCn", "Broker CN No"),
            ReportColumn.text("customerDn", "Customer DN No")));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .rows(rows)
        .note("Amounts at our share in base currency (policy currency x rate at approval).")
        .build();
  }

  private static Map<String, Object> row(PremiumTransaction t, Map<Long, String> branches) {
    PremiumBreakdown b = t.premium();
    Map<String, Object> m = new LinkedHashMap<>();
    UwReportSupport.putGroups(m, t.policy(), branches);
    UwReportSupport.putDocument(m, t);
    m.put("customer", t.policy().customerName());
    m.put("issueDate", t.policy().issueDate());
    m.put("endtDate", t.endorsementNo() == 0 ? null : t.effectiveDate());
    m.put("period", t.policy().periodFrom() + " - " + t.policy().periodTo());
    m.put("approvalDate", t.approvalDate());
    m.put(K_SHARE, t.policy().sharePct());
    m.put("si100", t.toBase(b.getSumInsured()));
    m.put("ourSi", t.toBase(b.getOurSumInsured()));
    m.put("gross", t.toBase(b.getOurGrossPremium()));
    m.put("discount", t.toBase(b.getOurDiscount()));
    m.put("loading", t.toBase(b.getOurLoading()));
    m.put("net", t.toBase(b.getOurNetPremium()));
    m.put("policyFee", t.toBase(b.getPolicyFee()));
    m.put("otherCharges", t.toBase(b.taxesAndCharges().subtract(b.getPolicyFee())));
    m.put("broker", t.policy().intermediaryName());
    m.put("commission", t.toBase(b.getCommission()));
    m.put("brokerCn", t.creditNoteNo());
    m.put("customerDn", t.debitNoteNo());
    return m;
  }
}

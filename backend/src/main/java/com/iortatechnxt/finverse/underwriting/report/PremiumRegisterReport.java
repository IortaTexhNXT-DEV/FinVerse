package com.iortatechnxt.finverse.underwriting.report;

import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_PRODUCT;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_SHARE;
import static com.iortatechnxt.finverse.underwriting.report.UwReportSupport.K_UW_YEAR;

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
import com.iortatechnxt.finverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.finverse.underwriting.service.TransactionRef;
import com.iortatechnxt.finverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR015 Premium Register (General): every approved premium transaction with coinsurance split,
 * taxes, commission and the reinsurance split (treaty = quota share + surplus ceded, net retention,
 * FAC) supplied by the reinsurance module. Branch &gt; Class &gt; Product &gt; UW Year.
 */
@Component
public class PremiumRegisterReport implements ReportDefinition {

  private final UwReportSupport support;
  private final UnderwritingPorts ports;

  /**
   * Creates the report.
   *
   * @param support shared report support
   * @param ports reinsurance figures
   */
  public PremiumRegisterReport(UwReportSupport support, UnderwritingPorts ports) {
    this.support = support;
    this.ports = ports;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(UwReportSupport.basisParam());
    params.addAll(UwReportSupport.dateRange("Date From", "Date To"));
    return new ReportMetadata(
        "PGIBR015",
        "Premium Register (General)",
        ReportCategory.UNDERWRITING,
        "Premium, coinsurance, taxes, commission and reinsurance split per transaction",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<PremiumTransaction> txns =
        support.transactions(p, UwReportSupport.basis(p), UwReportSupport.APPROVED);
    Map<TransactionRef, ReinsuranceFigures> ri =
        ports.reinsurance(txns.stream().map(PremiumTransaction::ref).toList());
    Map<Long, String> branches = support.branchCodes(p.longValue(UwReportSupport.COMPANY));
    List<Map<String, Object>> rows = new ArrayList<>();
    int serial = 1;
    for (PremiumTransaction t : txns) {
      ReinsuranceFigures r =
          ri.getOrDefault(t.ref(), ReinsuranceFigures.noCession(t.premium().getOurNetPremium()));
      rows.add(row(serial++, t, r, branches));
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("serial", "S.No"));
    columns.add(ReportColumn.text("invoiceNo", "Invoice No"));
    columns.add(ReportColumn.date("txnDate", "Txn Date"));
    columns.addAll(UwReportSupport.documentColumns());
    columns.addAll(
        List.of(
            ReportColumn.date("periodFrom", "Period From"),
            ReportColumn.date("periodTo", "Period To"),
            ReportColumn.text("agent", "Agent / Broker"),
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("si", "Our SI"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("discount", "Discount"),
            ReportColumn.amount("loading", "Loading"),
            ReportColumn.amount("net", "Net (100%)"),
            ReportColumn.percent("coinsPct", "Coins %"),
            ReportColumn.amount("coinsShare", "Coins Share of Net"),
            ReportColumn.amount("netOfCoins", "Net of Coins"),
            ReportColumn.amount("taxes", "Taxes & Charges"),
            ReportColumn.amount("commission", "Broker Commission"),
            ReportColumn.amount("treaty", "Treaty RI"),
            ReportColumn.amount("retention", "Net Retention"),
            ReportColumn.amount("fac", "FAC Premium")));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .groupBy(K_UW_YEAR, "UW Year")
        .rows(rows)
        .note("Treaty RI = quota share + surplus ceded; net retention = premium retained.")
        .note("Amounts in base currency.")
        .build();
  }

  private static Map<String, Object> row(
      int serial, PremiumTransaction t, ReinsuranceFigures r, Map<Long, String> branches) {
    PremiumBreakdown b = t.premium();
    Map<String, Object> m = new LinkedHashMap<>();
    UwReportSupport.putGroups(m, t, branches);
    UwReportSupport.putDocument(m, t);
    m.put("serial", String.valueOf(serial));
    m.put("invoiceNo", t.debitNoteNo());
    m.put("txnDate", t.approvalDate());
    m.put("periodFrom", t.policy().periodFrom());
    m.put("periodTo", t.policy().periodTo());
    m.put("agent", t.policy().intermediaryName());
    m.put(K_SHARE, t.policy().sharePct());
    m.put("si", t.toBase(b.getOurSumInsured()));
    m.put("gross", t.toBase(b.getGrossPremium()));
    m.put("discount", t.toBase(b.getDiscountAmount()));
    m.put("loading", t.toBase(b.getLoadingAmount()));
    m.put("net", t.toBase(b.getNetPremium()));
    m.put("coinsPct", BigDecimal.valueOf(100).subtract(t.policy().sharePct()));
    m.put("coinsShare", t.toBase(b.getNetPremium().subtract(b.getOurNetPremium())));
    m.put("netOfCoins", t.toBase(b.getOurNetPremium()));
    m.put("taxes", t.toBase(b.taxesAndCharges()));
    m.put("commission", t.toBase(b.getCommission()));
    m.put("treaty", t.toBase(r.treatyPremium()));
    m.put("retention", t.toBase(r.netRetention()));
    m.put("fac", t.toBase(r.facPremium()));
    return m;
  }
}

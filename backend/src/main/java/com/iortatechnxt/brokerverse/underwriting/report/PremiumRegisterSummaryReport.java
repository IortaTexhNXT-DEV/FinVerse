package com.iortatechnxt.brokerverse.underwriting.report;

import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_PRODUCT;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_UW_YEAR;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.brokerverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.brokerverse.underwriting.service.TransactionRef;
import com.iortatechnxt.brokerverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR016 Premium Register Summary: premium register totals per product, same reinsurance rules as
 * PGIBR015. Branch &gt; Class &gt; UW Year, one line per product.
 */
@Component
public class PremiumRegisterSummaryReport implements ReportDefinition {

  private static final String[] AMOUNTS = {
    "si", "gross", "discount", "loading", "net", "taxes", "commission", "fac", "treaty", "retention"
  };

  private final UwReportSupport support;
  private final UnderwritingPorts ports;

  /**
   * Creates the report.
   *
   * @param support shared report support
   * @param ports reinsurance figures
   */
  public PremiumRegisterSummaryReport(UwReportSupport support, UnderwritingPorts ports) {
    this.support = support;
    this.ports = ports;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(UwReportSupport.basisParam());
    params.addAll(UwReportSupport.dateRange("Date From", "Date To"));
    return new ReportMetadata(
        "PGIBR016",
        "Premium Register Summary",
        ReportCategory.UNDERWRITING,
        "Premium, taxes, commission and reinsurance totals per product",
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
    Map<String, Map<String, Object>> byProduct = new LinkedHashMap<>();
    for (PremiumTransaction t : txns) {
      Map<String, Object> line = new LinkedHashMap<>();
      UwReportSupport.putGroups(line, t, branches);
      String key =
          line.get(K_BRANCH)
              + "|"
              + line.get(K_CLASS)
              + "|"
              + line.get(K_UW_YEAR)
              + "|"
              + line.get(K_PRODUCT);
      Map<String, Object> total = byProduct.computeIfAbsent(key, k -> line);
      ReinsuranceFigures r =
          ri.getOrDefault(t.ref(), ReinsuranceFigures.noCession(t.premium().getOurNetPremium()));
      BigDecimal[] values = values(t, r);
      for (int i = 0; i < AMOUNTS.length; i++) {
        total.merge(AMOUNTS[i], values[i], UwReportSupport::add);
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_PRODUCT, "Product"),
            ReportColumn.amount("si", "Our SI"),
            ReportColumn.amount("gross", "Gross"),
            ReportColumn.amount("discount", "Discount"),
            ReportColumn.amount("loading", "Loading"),
            ReportColumn.amount("net", "Net"),
            ReportColumn.amount("taxes", "Taxes & Charges"),
            ReportColumn.amount("commission", "Broker Commission"),
            ReportColumn.amount("fac", "FAC"),
            ReportColumn.amount("treaty", "Treaty RI"),
            ReportColumn.amount("retention", "Net Retention"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_UW_YEAR, "UW Year")
        .rows(new ArrayList<>(byProduct.values()))
        .note("Our share, base currency. Treaty RI = quota share + surplus ceded.")
        .build();
  }

  private static BigDecimal[] values(PremiumTransaction t, ReinsuranceFigures r) {
    PremiumBreakdown b = t.premium();
    return new BigDecimal[] {
      t.toBase(b.getOurSumInsured()),
      t.toBase(b.getOurGrossPremium()),
      t.toBase(b.getOurDiscount()),
      t.toBase(b.getOurLoading()),
      t.toBase(b.getOurNetPremium()),
      t.toBase(b.taxesAndCharges()),
      t.toBase(b.getCommission()),
      t.toBase(r.facPremium()),
      t.toBase(r.treatyPremium()),
      t.toBase(r.netRetention())
    };
  }
}

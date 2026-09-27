package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLAIM;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_INSURED;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_LOSS_DATE;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_POLICY;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_PRODUCT;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.service.ClaimFigures;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR002 Claims Settled Statement: claims with amounts paid or recovered in a period. Estimate
 * payment = types 1 + 3 and estimate recovery = types 2 + 4 as at the "paid to" date; paid figures
 * are those of the period. Totals are payment − recovery. Branch &gt; Class &gt; Product.
 */
@Component
public class ClaimsSettledReport implements ReportDefinition {

  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param support claims report support
   */
  public ClaimsSettledReport(ClaimReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Paid Date From", "Paid Date To"));
    params.add(ClaimReportSupport.includeExpenseParam());
    return new ReportMetadata(
        "PGIBR002",
        "Claims Settled Statement",
        ReportCategory.CLAIMS,
        "Claims paid or recovered in a period with their estimates",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate from = p.date(ClaimReportSupport.FROM);
    LocalDate to = p.date(ClaimReportSupport.TO);
    boolean expense = p.flag(ClaimReportSupport.INCLUDE_EXPENSE);
    Map<Long, ClaimFigures> period = support.between(companyId, from, to);
    Map<Long, ClaimFigures> asOf = support.asOf(companyId, to);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Claim c : support.claims(p)) {
      ClaimFigures paid = period.getOrDefault(c.getId(), ClaimFigures.none());
      if (paid.paid(expense).signum() == 0 && paid.recovered().signum() == 0) {
        continue;
      }
      ClaimFigures estimate = asOf.getOrDefault(c.getId(), ClaimFigures.none());
      Map<String, Object> m = new LinkedHashMap<>();
      ClaimReportSupport.putClaim(m, c, branches);
      m.put("nature", c.getLoss().getNatureOfLoss());
      BigDecimal estPayment = estimate.paymentEstimate(expense);
      m.put("estPayment", estPayment);
      m.put("estRecovery", estimate.estimateRecovery());
      m.put("estTotal", estPayment.subtract(estimate.estimateRecovery()));
      m.put("paidPayment", paid.paid(expense));
      m.put("paidRecovery", paid.recovered());
      m.put("paidTotal", paid.paid(expense).subtract(paid.recovered()));
      m.put("closed", ClaimReportSupport.finishedBy(c, to) ? "Y" : "N");
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_CLAIM, "Claim No"),
            ReportColumn.text(K_POLICY, "Policy No"),
            ReportColumn.date(K_LOSS_DATE, "Loss Date"),
            ReportColumn.text(K_INSURED, "Assured"),
            ReportColumn.text("nature", "Nature of Loss"),
            ReportColumn.amount("estPayment", "Estimate Payment"),
            ReportColumn.amount("estRecovery", "Estimate Recovery"),
            ReportColumn.amount("estTotal", "Total Estimate"),
            ReportColumn.amount("paidPayment", "Paid Payment"),
            ReportColumn.amount("paidRecovery", "Paid Recovery"),
            ReportColumn.amount("paidTotal", "Total Paid"),
            ReportColumn.text("closed", "Closed"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CLASS, ClaimReportSupport.CLASS_LABEL)
        .groupBy(K_PRODUCT, ClaimReportSupport.PRODUCT_LABEL)
        .rows(rows)
        .note(
            "Estimate payment = types 1 + 3, estimate recovery = types 2 + 4, as at the paid-to date.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }
}

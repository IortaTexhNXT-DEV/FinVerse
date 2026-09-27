package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLAIM;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_INSURED;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_LOSS_DATE;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_POLICY;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_PRODUCT;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_UW_YEAR;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR018 Claims Outstanding as on a date: payment O/S = payment estimate − paid, recovery O/S =
 * recovery estimate − recovered, net O/S = payment O/S − recovery O/S. Claims closed, repudiated or
 * withdrawn by the as-on date, and claims reported after it, are excluded. Branch &gt; Class &gt;
 * Product &gt; UW Year.
 */
@Component
public class ClaimsOutstandingReport implements ReportDefinition {

  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param support claims report support
   */
  public ClaimsOutstandingReport(ClaimReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.add(ClaimReportSupport.asOnParam());
    params.add(ClaimReportSupport.includeExpenseParam());
    return new ReportMetadata(
        "PGIBR018",
        "Claims Outstanding",
        ReportCategory.CLAIMS,
        "Outstanding payment and recovery estimates of open claims as on a date",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate asOn = p.date(ClaimReportSupport.AS_ON);
    boolean expense = p.flag(ClaimReportSupport.INCLUDE_EXPENSE);
    Map<Long, ClaimFigures> figures = support.asOf(companyId, asOn);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Claim c : support.claims(p)) {
      ClaimFigures f = figures.getOrDefault(c.getId(), ClaimFigures.none());
      boolean open =
          !c.getLoss().getReportedDate().isAfter(asOn) && !ClaimReportSupport.finishedBy(c, asOn);
      BigDecimal paymentOs = f.paymentOutstanding(expense);
      if (!open || paymentOs.signum() == 0 && f.recoveryOutstanding().signum() == 0) {
        continue;
      }
      Map<String, Object> m = new LinkedHashMap<>();
      ClaimReportSupport.putClaim(m, c, branches);
      m.put("estPayment", f.paymentEstimate(expense));
      m.put("estRecovery", f.estimateRecovery());
      m.put("paid", f.paid(expense));
      m.put("recovered", f.recovered());
      m.put("paymentOs", paymentOs);
      m.put("recoveryOs", f.recoveryOutstanding());
      m.put("netOs", paymentOs.subtract(f.recoveryOutstanding()));
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_CLAIM, "Claim No"),
            ReportColumn.text(K_POLICY, "Policy No"),
            ReportColumn.text(K_INSURED, "Assured"),
            ReportColumn.date(K_LOSS_DATE, "Loss Date"),
            ReportColumn.amount("estPayment", "Payment Estimate"),
            ReportColumn.amount("estRecovery", "Recovery Estimate"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("recovered", "Recovered"),
            ReportColumn.amount("paymentOs", "Payment O/S"),
            ReportColumn.amount("recoveryOs", "Recovery O/S"),
            ReportColumn.amount("netOs", "Net O/S"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CLASS, ClaimReportSupport.CLASS_LABEL)
        .groupBy(K_PRODUCT, ClaimReportSupport.PRODUCT_LABEL)
        .groupBy(K_UW_YEAR, "UW Year")
        .rows(rows)
        .note(
            "O/S = estimate - paid; Net O/S = payment O/S - recovery O/S; closed claims excluded.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }
}

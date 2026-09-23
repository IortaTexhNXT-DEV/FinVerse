package com.iortatechnxt.finverse.claims.report;

import static com.iortatechnxt.finverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.claims.report.ClaimReportSupport.K_CLAIM;
import static com.iortatechnxt.finverse.claims.report.ClaimReportSupport.K_CUSTOMER;
import static com.iortatechnxt.finverse.claims.report.ClaimReportSupport.K_INSURED;
import static com.iortatechnxt.finverse.claims.report.ClaimReportSupport.K_LOSS_DATE;
import static com.iortatechnxt.finverse.claims.report.ClaimReportSupport.K_POLICY;

import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.service.ClaimFigures;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR036 Client-wise Outstanding Claims as on a date: open claims with a payment outstanding
 * (estimate − paid, loss and expense), grouped Branch &gt; Customer.
 */
@Component
public class ClientOutstandingReport implements ReportDefinition {

  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param support claims report support
   */
  public ClientOutstandingReport(ClaimReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.add(ClaimReportSupport.asOnParam());
    return new ReportMetadata(
        "PGIBR036",
        "Client-wise Outstanding Claims",
        ReportCategory.CLAIMS,
        "Outstanding claims by client as on a date",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate asOn = p.date(ClaimReportSupport.AS_ON);
    Map<Long, ClaimFigures> figures = support.asOf(companyId, asOn);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Claim c : support.claims(p)) {
      ClaimFigures f = figures.getOrDefault(c.getId(), ClaimFigures.none());
      if (f.paymentOutstanding(true).signum() == 0 || ClaimReportSupport.finishedBy(c, asOn)) {
        continue;
      }
      Map<String, Object> m = new LinkedHashMap<>();
      ClaimReportSupport.putClaim(m, c, branches);
      m.put("status", c.getStatus().name());
      m.put("estimate", f.paymentEstimate(true));
      m.put("paid", f.paid(true));
      m.put("os", f.paymentOutstanding(true));
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_CLAIM, "Claim No"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text(K_POLICY, "Policy No"),
            ReportColumn.date(K_LOSS_DATE, "Loss Date"),
            ReportColumn.text(K_INSURED, "Assured"),
            ReportColumn.amount("estimate", "Estimate"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("os", "O/S"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CUSTOMER, "Customer")
        .rows(rows)
        .note("O/S = estimate - paid (loss and expense). Status is the current status.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }
}

package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLAIM;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_INSURED;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_LOSS_DATE;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_POLICY;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * CLM-REGISTER Claims Register (bordereaux): every claim reported in a period with its loss
 * details, status and estimate, paid, recovered and outstanding as at the period end. Branch &gt;
 * Class.
 */
@Component
public class ClaimsRegisterReport implements ReportDefinition {

  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param support claims report support
   */
  public ClaimsRegisterReport(ClaimReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ClaimReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Reported Date From", "Reported Date To"));
    return new ReportMetadata(
        "CLM-REGISTER",
        "Claims Register (Bordereaux)",
        ReportCategory.CLAIMS,
        "Claims reported in a period with loss details and financial position",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate from = p.date(ClaimReportSupport.FROM);
    LocalDate to = p.date(ClaimReportSupport.TO);
    Map<Long, ClaimFigures> figures = support.asOf(companyId, to);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Claim c : support.claims(p)) {
      LocalDate reported = c.getLoss().getReportedDate();
      if (reported.isBefore(from) || reported.isAfter(to)) {
        continue;
      }
      ClaimFigures f = figures.getOrDefault(c.getId(), ClaimFigures.none());
      Map<String, Object> m = new LinkedHashMap<>();
      ClaimReportSupport.putClaim(m, c, branches);
      m.put("productCode", c.getPolicy().getProductCode());
      m.put("reported", reported);
      m.put("nature", c.getLoss().getNatureOfLoss());
      m.put("cause", c.getLoss().getCauseOfLoss());
      m.put("location", c.getLoss().getLossLocation());
      m.put("status", c.getStatus().name());
      m.put("currency", c.getCurrency());
      m.put("estimate", f.paymentEstimate(true));
      m.put("paid", f.paid(true));
      m.put("recovered", f.recovered());
      m.put("os", f.paymentOutstanding(true));
      rows.add(m);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_CLAIM, "Claim No"),
            ReportColumn.text(K_POLICY, "Policy No"),
            ReportColumn.text(K_INSURED, "Insured"),
            ReportColumn.text("productCode", "Product"),
            ReportColumn.date(K_LOSS_DATE, "Loss Date"),
            ReportColumn.date("reported", "Reported"),
            ReportColumn.text("nature", "Nature of Loss"),
            ReportColumn.text("cause", "Cause"),
            ReportColumn.text("location", "Location"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("currency", "Ccy"),
            ReportColumn.amount("estimate", "Estimate"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("recovered", "Recovered"),
            ReportColumn.amount("os", "Outstanding"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_CLASS, ClaimReportSupport.CLASS_LABEL)
        .rows(rows)
        .note("Figures as at the reported-to date; status is the current status.")
        .note(ClaimReportSupport.AMOUNTS_NOTE)
        .build();
  }
}

package com.iortatechnxt.brokerverse.underwriting.report;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR084 Marine Open Cover Certificate Report: certificates issued in a period with sum insured
 * and premium in foreign and local currency and their claims (claims module), totalled per open
 * cover.
 */
@Component
public class OpenCoverCertificateReport implements ReportDefinition {

  private static final String OPEN_COVER = "openCover";

  private final UwReportSupport support;
  private final UnderwritingPorts ports;

  /**
   * Creates the report.
   *
   * @param support shared report support
   * @param ports claims figures
   */
  public OpenCoverCertificateReport(UwReportSupport support, UnderwritingPorts ports) {
    this.support = support;
    this.ports = ports;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Certificate Date From", "Certificate Date To"));
    return new ReportMetadata(
        "PGIBR084",
        "Marine Open Cover Certificate Report",
        ReportCategory.UNDERWRITING,
        "Certificates with sum insured, premium and claims per open cover",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    UwFilters f = UwReportSupport.filters(p);
    LocalDate from = p.date(UwReportSupport.FROM);
    LocalDate to = p.date(UwReportSupport.TO);
    List<Policy> certs =
        support.approvedCertificates(p.longValue(UwReportSupport.COMPANY), null).stream()
            .filter(c -> !c.getIssueDate().isBefore(from) && !c.getIssueDate().isAfter(to))
            .filter(c -> f.test(PolicySnapshot.of(c)))
            .toList();
    Map<Long, ClaimsFigures> claims = ports.claims(certs.stream().map(Policy::getId).toList());
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Policy c : certs) {
      rows.add(row(c, claims.getOrDefault(c.getId(), ClaimsFigures.none())));
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("certificate", "Certificate"),
            ReportColumn.text("period", "Period"),
            ReportColumn.text("insured", "Insured"),
            ReportColumn.text("currency", "Ccy"),
            ReportColumn.amountNoTotal("siFc", "SI FC"),
            ReportColumn.amount("siLc", "SI LC"),
            ReportColumn.amountNoTotal("premiumFc", "Premium FC"),
            ReportColumn.amount("premiumLc", "Premium LC"),
            ReportColumn.text("claim", "Claim"),
            ReportColumn.date("lossDate", "Loss Date"),
            ReportColumn.amount("reserve", "Reserve"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("outstanding", "O/S"))
        .groupBy(OPEN_COVER, "Open Cover")
        .rows(rows)
        .note("LC = base currency at the rate of approval; claims in base currency.")
        .build();
  }

  private static Map<String, Object> row(Policy c, ClaimsFigures claim) {
    BigDecimal rate =
        c.getRefs().getExchangeRate() == null ? BigDecimal.ONE : c.getRefs().getExchangeRate();
    BigDecimal si = c.getPremium().getOurSumInsured();
    BigDecimal premium = c.getPremium().getOurNetPremium();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(OPEN_COVER, c.getOpenCover().getOpenCoverNo() + " " + c.getOpenCover().getInsuredName());
    m.put("certificate", c.getPolicyNo());
    m.put("period", c.getPeriodFrom() + " - " + c.getPeriodTo());
    m.put("insured", c.getInsuredName());
    m.put("currency", c.getCurrency());
    m.put("siFc", si);
    m.put("siLc", Money.convert(si, rate));
    m.put("premiumFc", premium);
    m.put("premiumLc", Money.convert(premium, rate));
    m.put("claim", claim.latestClaimNo());
    m.put("lossDate", claim.latestLossDate());
    m.put("reserve", Money.convert(claim.reserve(), rate));
    m.put("paid", Money.convert(claim.paid(), rate));
    m.put("outstanding", Money.convert(claim.outstanding(), rate));
    return m;
  }
}
